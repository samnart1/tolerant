from fastapi import FastAPI, Request, Form, HTTPException
from fastapi.responses import HTMLResponse, RedirectResponse
from pydantic import BaseModel
from typing import Optional
import httpx
import uuid
import os

app = FastAPI(title="Frontend Service")

# Service URLs
PRODUCT_CATALOG_URL = os.getenv("PRODUCT_CATALOG_URL", "http://productcatalog:8081")
CART_URL = os.getenv("CART_URL", "http://cart:8082")
CURRENCY_URL = os.getenv("CURRENCY_URL", "http://currency:8083")
SHIPPING_URL = os.getenv("SHIPPING_URL", "http://shipping:8084")
CHECKOUT_URL = os.getenv("CHECKOUT_URL", "http://checkout:8087")
RECOMMENDATION_URL = os.getenv("RECOMMENDATION_URL", "http://recommendation:8088")
AD_URL = os.getenv("AD_URL", "http://ad:8089")

# In-memory session store (for simplicity)
sessions = {}

def get_session_id(request: Request) -> str:
    """Get or create session ID from cookie"""
    session_id = request.cookies.get("session_id")
    if not session_id:
        session_id = str(uuid.uuid4())
    return session_id

def get_currency(request: Request) -> str:
    """Get currency from session"""
    session_id = get_session_id(request)
    return sessions.get(session_id, {}).get("currency", "USD")

async def get_products():
    """Fetch all products from catalog"""
    async with httpx.AsyncClient(timeout=5.0) as client:
        try:
            resp = await client.get(f"{PRODUCT_CATALOG_URL}/products")
            return resp.json() if resp.status_code == 200 else []
        except Exception:
            return []

async def get_product(product_id: str):
    """Fetch single product"""
    async with httpx.AsyncClient(timeout=5.0) as client:
        try:
            resp = await client.get(f"{PRODUCT_CATALOG_URL}/product/{product_id}")
            return resp.json() if resp.status_code == 200 else None
        except Exception:
            return None

async def get_cart(user_id: str):
    """Fetch user cart"""
    async with httpx.AsyncClient(timeout=5.0) as client:
        try:
            resp = await client.post(f"{CART_URL}/cart/get", json={"user_id": user_id})
            return resp.json() if resp.status_code == 200 else {"items": []}
        except Exception:
            return {"items": []}

async def get_recommendations(product_ids: list):
    """Get product recommendations"""
    async with httpx.AsyncClient(timeout=5.0) as client:
        try:
            resp = await client.post(f"{RECOMMENDATION_URL}/recommend", json={"product_ids": product_ids})
            return resp.json().get("product_ids", []) if resp.status_code == 200 else []
        except Exception:
            return []

async def get_ads(context_keys: list = []):
    """Get contextual ads"""
    async with httpx.AsyncClient(timeout=5.0) as client:
        try:
            resp = await client.post(f"{AD_URL}/ads", json={"context_keys": context_keys})
            return resp.json().get("ads", []) if resp.status_code == 200 else []
        except Exception:
            return []

async def get_shipping_quote(user_id: str, items: list):
    """Get shipping cost estimate"""
    async with httpx.AsyncClient(timeout=5.0) as client:
        try:
            resp = await client.post(f"{SHIPPING_URL}/quote", json={
                "address": {"street_address": "", "city": "", "state": "", "country": "", "zip_code": ""},
                "items": items
            })
            return resp.json().get("cost_usd", {}) if resp.status_code == 200 else {}
        except Exception:
            return {}

@app.get("/health")
def health():
    return {"status": "ok"}

@app.get("/", response_class=HTMLResponse)
async def index(request: Request):
    """Home page - shows products"""
    session_id = get_session_id(request)
    currency = get_currency(request)
    
    products = await get_products()
    ads = await get_ads()
    
    # Return minimal HTML that confirms the page works
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
    """Set user's currency preference"""
    session_id = get_session_id(request)
    
    if session_id not in sessions:
        sessions[session_id] = {}
    sessions[session_id]["currency"] = currency_code
    
    response = RedirectResponse(url="/", status_code=303)
    response.set_cookie("session_id", session_id)
    return response

@app.get("/product/{product_id}", response_class=HTMLResponse)
async def product_page(request: Request, product_id: str):
    """Product detail page"""
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
    """View shopping cart"""
    session_id = get_session_id(request)
    currency = get_currency(request)
    
    cart = await get_cart(session_id)
    items = cart.get("items", [])
    
    # Get product details for cart items
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
    
    # Get shipping quote
    shipping = await get_shipping_quote(session_id, cart_items_for_shipping)
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
async def add_to_cart(request: Request, product_id: str = Form(...), quantity: int = Form(...)):
    """Add item to cart"""
    session_id = get_session_id(request)
    
    async with httpx.AsyncClient(timeout=5.0) as client:
        await client.post(f"{CART_URL}/cart/add", json={
            "user_id": session_id,
            "item": {"product_id": product_id, "quantity": quantity}
        })
    
    response = RedirectResponse(url="/cart", status_code=303)
    response.set_cookie("session_id", session_id)
    return response

@app.post("/cart/empty")
async def empty_cart(request: Request):
    """Empty the cart"""
    session_id = get_session_id(request)
    
    async with httpx.AsyncClient(timeout=5.0) as client:
        await client.post(f"{CART_URL}/cart/empty", json={"user_id": session_id})
    
    response = RedirectResponse(url="/cart", status_code=303)
    response.set_cookie("session_id", session_id)
    return response

@app.get("/cart/checkout", response_class=HTMLResponse)
async def checkout_page(request: Request):
    """Checkout form page"""
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
    """Process checkout"""
    session_id = get_session_id(request)
    currency = get_currency(request)
    
    async with httpx.AsyncClient(timeout=30.0) as client:
        try:
            resp = await client.post(f"{CHECKOUT_URL}/checkout", json={
                "user_id": session_id,
                "user_currency": currency,
                "address": {
                    "street_address": street_address,
                    "city": city,
                    "state": state,
                    "country": country,
                    "zip_code": zip_code
                },
                "email": email,
                "credit_card": {
                    "credit_card_number": credit_card_number,
                    "credit_card_cvv": int(credit_card_cvv),
                    "credit_card_expiration_year": credit_card_expiration_year,
                    "credit_card_expiration_month": credit_card_expiration_month
                }
            })
            
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
    """Clear session"""
    session_id = get_session_id(request)
    if session_id in sessions:
        del sessions[session_id]
    
    response = RedirectResponse(url="/", status_code=303)
    response.delete_cookie("session_id")
    return response

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8080)
