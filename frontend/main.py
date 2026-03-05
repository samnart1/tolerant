from fastapi import FastAPI, Request, Form, HTTPException
from fastapi.responses import HTMLResponse, RedirectResponse, JSONResponse
import httpx
import uuid
import os
import time
import logging

app = FastAPI(title="Frontend Service")
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

PRODUCT_CATALOG_URL = os.getenv("PRODUCT_CATALOG_URL", "http://productcatalog:8081")
CART_URL = os.getenv("CART_URL", "http://cart:8082")
CURRENCY_URL = os.getenv("CURRENCY_URL", "http://currency:8083")
SHIPPING_URL = os.getenv("SHIPPING_URL", "http://shipping:8084")
CHECKOUT_URL = os.getenv("CHECKOUT_URL", "http://checkout:8087")
RECOMMENDATION_URL = os.getenv("RECOMMENDATION_URL", "http://recommendation:8088")
AD_URL = os.getenv("AD_URL", "http://ad:8089")

CIRCUIT_BREAKER_ENABLED = os.getenv("CIRCUIT_BREAKER_ENABLED", "false").lower() == "true"
CB_FAIL_MAX = int(os.getenv("CB_FAIL_MAX", "5"))
CB_RESET_TIMEOUT = int(os.getenv("CB_RESET_TIMEOUT", "30"))

logger.info(f"Circuit Breaker Enabled: {CIRCUIT_BREAKER_ENABLED}")
logger.info(f"CB Config: fail_max={CB_FAIL_MAX}, reset_timeout={CB_RESET_TIMEOUT}")

class CircuitBreaker:
    
    CLOSED = "closed"
    OPEN = "open"
    HALF_OPEN = "half_open"
    
    def __init__(self, name, fail_max = 5, reset_timeout = 30):
        self.name = name
        self.fail_max = fail_max
        self.reset_timeout = reset_timeout
        self.state = self.CLOSED
        self.failure_count = 0
        self.last_failure_time = None
        self.success_count = 0
        
        self.state_changes = []
        self.total_calls = 0
        self.total_failures = 0
        self.total_successes = 0
        self.total_rejected = 0
    
    def _change_state(self, new_state):
        if self.state != new_state:
            old_state = self.state
            self.state = new_state
            event = {
                "timestamp": time.time(),
                "breaker": self.name,
                "from": old_state,
                "to": new_state,
                "failure_count": self.failure_count
            }
            self.state_changes.append(event)
            logger.info(f"[CB:{self.name}] State change: {old_state} -> {new_state}")
    
    def can_execute(self):
        self.total_calls += 1
        
        if self.state == self.CLOSED:
            return True
        
        if self.state == self.OPEN:
            if time.time() - self.last_failure_time >= self.reset_timeout:
                self._change_state(self.HALF_OPEN)
                return True
            self.total_rejected += 1
            return False
        
        if self.state == self.HALF_OPEN:
            return True
        
        return False
    
    def record_success(self):
        self.total_successes += 1
        
        if self.state == self.HALF_OPEN:
            self.success_count += 1
            if self.success_count >= 2:  # 2 successes to close
                self.failure_count = 0
                self.success_count = 0
                self._change_state(self.CLOSED)
        elif self.state == self.CLOSED:
            self.failure_count = 0
    
    def record_failure(self):
        self.total_failures += 1
        self.failure_count += 1
        self.last_failure_time = time.time()
        
        if self.state == self.HALF_OPEN:
            self._change_state(self.OPEN)
        elif self.state == self.CLOSED and self.failure_count >= self.fail_max:
            self._change_state(self.OPEN)
    
    def get_metrics(self):
        return {
            "name": self.name,
            "state": self.state,
            "failure_count": self.failure_count,
            "total_calls": self.total_calls,
            "total_successes": self.total_successes,
            "total_failures": self.total_failures,
            "total_rejected": self.total_rejected,
            "state_changes": self.state_changes
        }


class CircuitBreakerOpen(Exception):
    pass


breakers = {
    "productcatalog": CircuitBreaker("productcatalog", CB_FAIL_MAX, CB_RESET_TIMEOUT),
    "cart": CircuitBreaker("cart", CB_FAIL_MAX, CB_RESET_TIMEOUT),
    "currency": CircuitBreaker("currency", CB_FAIL_MAX, CB_RESET_TIMEOUT),
    "shipping": CircuitBreaker("shipping", CB_FAIL_MAX, CB_RESET_TIMEOUT),
    "checkout": CircuitBreaker("checkout", CB_FAIL_MAX, CB_RESET_TIMEOUT),
    "recommendation": CircuitBreaker("recommendation", CB_FAIL_MAX, CB_RESET_TIMEOUT),
    "ad": CircuitBreaker("ad", CB_FAIL_MAX, CB_RESET_TIMEOUT),
}

async def call_service(breaker_name, method, url, **kwargs):
    
    breaker = breakers.get(breaker_name)
    timeout = kwargs.pop("timeout", 5.0)
    
    if not CIRCUIT_BREAKER_ENABLED:
        async with httpx.AsyncClient(timeout=timeout) as client:
            if method == "GET":
                return await client.get(url, **kwargs)
            else:
                return await client.post(url, **kwargs)
    
    if not breaker.can_execute():
        raise CircuitBreakerOpen(f"Circuit breaker {breaker_name} is OPEN")
    
    try:
        async with httpx.AsyncClient(timeout=timeout) as client:
            if method == "GET":
                resp = await client.get(url, **kwargs)
            else:
                resp = await client.post(url, **kwargs)
            
            if resp.status_code >= 500:
                breaker.record_failure()
            else:
                breaker.record_success()
            return resp
            
    except Exception as e:
        breaker.record_failure()
        raise


sessions = {}

def get_session_id(request: Request) -> str:
    session_id = request.cookies.get("session_id")
    if not session_id:
        session_id = str(uuid.uuid4())
    return session_id

def get_currency(request: Request) -> str:
    session_id = get_session_id(request)
    return sessions.get(session_id, {}).get("currency", "USD")



async def get_products():
    try:
        resp = await call_service("productcatalog", "GET", f"{PRODUCT_CATALOG_URL}/products")
        return resp.json() if resp.status_code == 200 else []
    except CircuitBreakerOpen:
        logger.warning("ProductCatalog circuit open - returning empty")
        return []
    except Exception as e:
        logger.error(f"ProductCatalog error: {e}")
        return []

async def get_product(product_id):
    try:
        resp = await call_service("productcatalog", "GET", f"{PRODUCT_CATALOG_URL}/product/{product_id}")
        return resp.json() if resp.status_code == 200 else None
    except CircuitBreakerOpen:
        logger.warning(f"ProductCatalog circuit open - no product {product_id}")
        return None
    except Exception:
        return None

async def get_cart(user_id):
    try:
        resp = await call_service("cart", "POST", f"{CART_URL}/cart/get", json={"user_id": user_id})
        return resp.json() if resp.status_code == 200 else {"items": []}
    except CircuitBreakerOpen:
        logger.warning("Cart circuit open - returning empty cart")
        return {"items": []}
    except Exception:
        return {"items": []}

async def add_to_cart(user_id, product_id, quantity):
    try:
        resp = await call_service("cart", "POST", f"{CART_URL}/cart/add", json={
            "user_id": user_id,
            "item": {"product_id": product_id, "quantity": quantity}
        })
        return resp.status_code == 200
    except CircuitBreakerOpen:
        logger.warning("Cart circuit open - cannot add item")
        return False
    except Exception:
        return False

async def empty_cart_service(user_id):
    try:
        resp = await call_service("cart", "POST", f"{CART_URL}/cart/empty", json={"user_id": user_id})
        return resp.status_code == 200
    except CircuitBreakerOpen:
        return False
    except Exception:
        return False

async def get_recommendations(product_ids):
    try:
        resp = await call_service("recommendation", "POST", f"{RECOMMENDATION_URL}/recommend", 
                                json={"product_ids": product_ids})
        return resp.json().get("product_ids", []) if resp.status_code == 200 else []
    except CircuitBreakerOpen:
        return []
    except Exception:
        return []

async def get_ads(context_keys):
    try:
        resp = await call_service("ad", "POST", f"{AD_URL}/ads", json={"context_keys": context_keys})
        return resp.json().get("ads", []) if resp.status_code == 200 else []
    except CircuitBreakerOpen:
        return []
    except Exception:
        return []

async def get_shipping_quote(items):
    try:
        resp = await call_service("shipping", "POST", f"{SHIPPING_URL}/quote", json={
            "address": {"street_address": "", "city": "", "state": "", "country": "", "zip_code": ""},
            "items": items
        })
        return resp.json().get("cost_usd", {}) if resp.status_code == 200 else {}
    except CircuitBreakerOpen:
        return {"currency_code": "USD", "units": 0, "nanos": 0}
    except Exception:
        return {"currency_code": "USD", "units": 0, "nanos": 0}

async def do_checkout_service(session_id, currency, address, email, credit_card):
    try:
        resp = await call_service("checkout", "POST", f"{CHECKOUT_URL}/checkout", 
                                timeout=30.0,
                                json={
                                    "user_id": session_id,
                                    "user_currency": currency,
                                    "address": address,
                                    "email": email,
                                    "credit_card": credit_card
                                })
        return resp
    except CircuitBreakerOpen as e:
        raise e
    except Exception as e:
        raise e



@app.get("/health")
def health():
    return {"status": "ok", "circuit_breaker_enabled": CIRCUIT_BREAKER_ENABLED}

@app.get("/metrics")
def get_metrics():
    return {
        "circuit_breaker_enabled": CIRCUIT_BREAKER_ENABLED,
        "breakers": {name: b.get_metrics() for name, b in breakers.items()}
    }

@app.get("/", response_class=HTMLResponse)
async def index(request: Request):
    session_id = get_session_id(request)
    currency = get_currency(request)
    
    products = await get_products()
    ads = await get_ads([])
    
    response = HTMLResponse(content=f"""
    <!DOCTYPE html>
    <html>
    <head><title>Online Boutique</title></head>
    <body>
        <h1>Online Boutique</h1>
        <p>Currency: {currency}</p>
        <p>Products: {len(products)}</p>
        <p>Session: {session_id[:8]}...</p>
        <ul>
        {"".join(f'<li><a href="/product/{p["id"]}">{p["name"]}</a> - ${p["price_usd"]["units"]}</li>' for p in products)}
        </ul>
    </body>
    </html>
    """)
    response.set_cookie("session_id", session_id)
    return response

@app.post("/setCurrency")
async def set_currency(request: Request, currency_code: str = Form(...)):
    session_id = get_session_id(request)
    
    if session_id not in sessions:
        sessions[session_id] = {}
    sessions[session_id]["currency"] = currency_code
    
    response = RedirectResponse(url="/", status_code=303)
    response.set_cookie("session_id", session_id)
    return response

@app.get("/product/{product_id}", response_class=HTMLResponse)
async def product_page(request: Request, product_id: str):
    session_id = get_session_id(request)
    currency = get_currency(request)
    
    product = await get_product(product_id)
    if not product:
        raise HTTPException(status_code=404, detail="Product not found")
    
    recommendations = await get_recommendations([product_id])
    ads = await get_ads([product.get("categories", [])])
    
    response = HTMLResponse(content=f"""
    <!DOCTYPE html>
    <html>
    <head><title>{product["name"]} - Online Boutique</title></head>
    <body>
        <h1>{product["name"]}</h1>
        <p>{product["description"]}</p>
        <p>Price: ${product["price_usd"]["units"]}.{product["price_usd"].get("nanos", 0) // 10_000_000:02d}</p>
        <form action="/cart" method="post">
            <input type="hidden" name="product_id" value="{product_id}">
            <label>Quantity: <input type="number" name="quantity" value="1" min="1"></label>
            <button type="submit">Add to Cart</button>
        </form>
        <h3>Recommendations</h3>
        <ul>
        {"".join(f'<li><a href="/product/{r}">{r}</a></li>' for r in recommendations[:4])}
        </ul>
        <a href="/">Back to Home</a>
    </body>
    </html>
    """)
    response.set_cookie("session_id", session_id)
    return response

@app.get("/cart", response_class=HTMLResponse)
async def view_cart(request: Request):
    session_id = get_session_id(request)
    currency = get_currency(request)
    
    cart = await get_cart(session_id)
    items = cart.get("items", [])
    
    cart_items_html = []
    cart_items_for_shipping = []
    total = 0
    
    for item in items:
        product = await get_product(item["product_id"])
        if product:
            price = product["price_usd"]["units"] + product["price_usd"].get("nanos", 0) / 1_000_000_000
            item_total = price * item["quantity"]
            total += item_total
            cart_items_html.append(f'<li>{product["name"]} x {item["quantity"]} = ${item_total:.2f}</li>')
            cart_items_for_shipping.append({"product_id": item["product_id"], "quantity": item["quantity"]})
    
    shipping = await get_shipping_quote(cart_items_for_shipping)
    shipping_cost = shipping.get("units", 0) + shipping.get("nanos", 0) / 1_000_000_000
    
    response = HTMLResponse(content=f"""
    <!DOCTYPE html>
    <html>
    <head><title>Cart - Online Boutique</title></head>
    <body>
        <h1>Shopping Cart</h1>
        <ul>{"".join(cart_items_html) if cart_items_html else "<li>Cart is empty</li>"}</ul>
        <p>Subtotal: ${total:.2f}</p>
        <p>Shipping: ${shipping_cost:.2f}</p>
        <p><strong>Total: ${total + shipping_cost:.2f}</strong></p>
        <form action="/cart/empty" method="post" style="display:inline;">
            <button type="submit">Empty Cart</button>
        </form>
        <a href="/cart/checkout">Checkout</a>
        <br><br>
        <a href="/">Continue Shopping</a>
    </body>
    </html>
    """)
    response.set_cookie("session_id", session_id)
    return response

@app.post("/cart")
async def add_to_cart_endpoint(request: Request, product_id: str = Form(...), quantity: int = Form(...)):
    session_id = get_session_id(request)
    await add_to_cart(session_id, product_id, quantity)
    
    response = RedirectResponse(url="/cart", status_code=303)
    response.set_cookie("session_id", session_id)
    return response

@app.post("/cart/empty")
async def empty_cart(request: Request):
    session_id = get_session_id(request)
    await empty_cart_service(session_id)
    
    response = RedirectResponse(url="/cart", status_code=303)
    response.set_cookie("session_id", session_id)
    return response

@app.get("/cart/checkout", response_class=HTMLResponse)
async def checkout_page(request: Request):
    session_id = get_session_id(request)
    
    response = HTMLResponse(content="""
    <!DOCTYPE html>
    <html>
    <head><title>Checkout - Online Boutique</title></head>
    <body>
        <h1>Checkout</h1>
        <form action="/cart/checkout" method="post">
            <h3>Shipping Address</h3>
            <input type="email" name="email" placeholder="Email" required><br>
            <input type="text" name="street_address" placeholder="Street Address" required><br>
            <input type="text" name="city" placeholder="City" required><br>
            <input type="text" name="state" placeholder="State" required><br>
            <input type="text" name="zip_code" placeholder="ZIP Code" required><br>
            <input type="text" name="country" placeholder="Country" required><br>
            
            <h3>Payment</h3>
            <input type="text" name="credit_card_number" placeholder="Card Number" required><br>
            <input type="number" name="credit_card_expiration_month" placeholder="Exp Month" min="1" max="12" required><br>
            <input type="number" name="credit_card_expiration_year" placeholder="Exp Year" required><br>
            <input type="text" name="credit_card_cvv" placeholder="CVV" required><br>
            
            <button type="submit">Place Order</button>
        </form>
        <a href="/cart">Back to Cart</a>
    </body>
    </html>
    """)
    response.set_cookie("session_id", session_id)
    return response

@app.post("/cart/checkout", response_class=HTMLResponse)
async def do_checkout(
    request: Request,
    email: str = Form(...),
    street_address: str = Form(...),
    city: str = Form(...),
    state: str = Form(...),
    zip_code: str = Form(...),
    country: str = Form(...),
    credit_card_number: str = Form(...),
    credit_card_expiration_month: int = Form(...),
    credit_card_expiration_year: int = Form(...),
    credit_card_cvv: str = Form(...)
):
    session_id = get_session_id(request)
    currency = get_currency(request)
    
    try:
        resp = await do_checkout_service(
            session_id=session_id,
            currency=currency,
            address={
                "street_address": street_address,
                "city": city,
                "state": state,
                "country": country,
                "zip_code": zip_code
            },
            email=email,
            credit_card={
                "credit_card_number": credit_card_number,
                "credit_card_cvv": int(credit_card_cvv),
                "credit_card_expiration_year": credit_card_expiration_year,
                "credit_card_expiration_month": credit_card_expiration_month
            }
        )
        
        if resp.status_code == 200:
            order = resp.json()["order"]
            response = HTMLResponse(content=f"""
            <!DOCTYPE html>
            <html>
            <head><title>Order Confirmed - Online Boutique</title></head>
            <body>
                <h1>Order Confirmed!</h1>
                <p>Order ID: {order["order_id"]}</p>
                <p>Tracking: {order["shipping_tracking_id"]}</p>
                <p>Confirmation sent to: {email}</p>
                <a href="/">Continue Shopping</a>
            </body>
            </html>
            """)
        else:
            response = HTMLResponse(content=f"""
            <!DOCTYPE html>
            <html>
            <head><title>Checkout Failed - Online Boutique</title></head>
            <body>
                <h1>Checkout Failed</h1>
                <p>Error: {resp.text}</p>
                <a href="/cart">Back to Cart</a>
            </body>
            </html>
            """, status_code=500)
            
    except CircuitBreakerOpen:
        response = HTMLResponse(content="""
        <!DOCTYPE html>
        <html>
        <head><title>Service Unavailable - Online Boutique</title></head>
        <body>
            <h1>Service Temporarily Unavailable</h1>
            <p>Our checkout service is experiencing issues. Please try again in a moment.</p>
            <a href="/cart">Back to Cart</a>
        </body>
        </html>
        """, status_code=503)
        
    except Exception as e:
        response = HTMLResponse(content=f"""
        <!DOCTYPE html>
        <html>
        <head><title>Checkout Error - Online Boutique</title></head>
        <body>
            <h1>Checkout Error</h1>
            <p>Error: {str(e)}</p>
            <a href="/cart">Back to Cart</a>
        </body>
        </html>
        """, status_code=500)
    
    response.set_cookie("session_id", session_id)
    return response

@app.get("/logout")
async def logout(request: Request):
    session_id = get_session_id(request)
    if session_id in sessions:
        del sessions[session_id]
    
    response = RedirectResponse(url="/", status_code=303)
    response.delete_cookie("session_id")
    return response

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8080)
