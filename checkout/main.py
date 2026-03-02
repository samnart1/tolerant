from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List
import httpx
import uuid
import os

app = FastAPI(title="Checkout Service")


CART_URL = os.getenv("CART_URL", "http://cart:8082")
PRODUCT_CATALOG_URL = os.getenv("PRODUCT_CATALOG_URL", "http://productcatalog:8081")
CURRENCY_URL = os.getenv("CURRENCY_URL", "http://currency:8083")
SHIPPING_URL = os.getenv("SHIPPING_URL", "http://shipping:8084")
PAYMENT_URL = os.getenv("PAYMENT_URL", "http://payment:8085")
EMAIL_URL = os.getenv("EMAIL_URL", "http://email:8086")

class Address(BaseModel):
    street_address: str
    city: str
    state: str
    country: str
    zip_code: str

class CreditCardInfo(BaseModel):
    credit_card_number: str
    credit_card_cvv: int
    credit_card_expiration_year: int
    credit_card_expiration_month: int

class PlaceOrderRequest(BaseModel):
    user_id: str
    user_currency: str
    address: Address
    email: str
    credit_card: CreditCardInfo

class OrderItem(BaseModel):
    product_id: str
    quantity: int
    cost: dict

class OrderResult(BaseModel):
    order_id: str
    shipping_tracking_id: str
    shipping_cost: dict
    shipping_address: Address
    items: List[OrderItem]

class PlaceOrderResponse(BaseModel):
    order: OrderResult

@app.get("/health")
def health():
    return {"status": "ok"}

@app.post("/checkout", response_model=PlaceOrderResponse)
async def place_order(req: PlaceOrderRequest):
    async with httpx.AsyncClient(timeout=10.0) as client:
        
        cart_resp = await client.post(f"{CART_URL}/cart/get", json={"user_id": req.user_id})
        if cart_resp.status_code != 200:
            raise HTTPException(status_code=500, detail="Failed to get cart")
        cart = cart_resp.json()
        
        if not cart.get("items"):
            raise HTTPException(status_code=400, detail="Cart is empty")
        
        
        order_items = []
        total_usd_cents = 0
        
        for item in cart["items"]:
            prod_resp = await client.get(f"{PRODUCT_CATALOG_URL}/product/{item['product_id']}")
            if prod_resp.status_code == 200:
                product = prod_resp.json()
                price = product["price_usd"]
                item_cost_cents = (price["units"] * 100 + price.get("nanos", 0) // 10_000_000) * item["quantity"]
                total_usd_cents += item_cost_cents
                
                order_items.append(OrderItem(
                    product_id=item["product_id"],
                    quantity=item["quantity"],
                    cost=price
                ))
        
        
        ship_quote_resp = await client.post(f"{SHIPPING_URL}/quote", json={
            "address": req.address.model_dump(),
            "items": [{"product_id": i.product_id, "quantity": i.quantity} for i in order_items]
        })
        shipping_cost = {"currency_code": "USD", "units": 5, "nanos": 0}
        if ship_quote_resp.status_code == 200:
            shipping_cost = ship_quote_resp.json()["cost_usd"]
        
        
        total_usd_cents += shipping_cost["units"] * 100 + shipping_cost.get("nanos", 0) // 10_000_000
        
        
        total_amount = {
            "currency_code": req.user_currency,
            "units": total_usd_cents // 100,
            "nanos": (total_usd_cents % 100) * 10_000_000
        }
        
        if req.user_currency != "USD":
            conv_resp = await client.post(f"{CURRENCY_URL}/convert", json={
                "from_money": {"currency_code": "USD", "units": total_usd_cents // 100, "nanos": (total_usd_cents % 100) * 10_000_000},
                "to_code": req.user_currency
            })
            if conv_resp.status_code == 200:
                total_amount = conv_resp.json()
        
        
        pay_resp = await client.post(f"{PAYMENT_URL}/charge", json={
            "amount": total_amount,
            "credit_card": req.credit_card.model_dump()
        })
        if pay_resp.status_code != 200:
            raise HTTPException(status_code=500, detail="Payment failed")
        
        transaction_id = pay_resp.json()["transaction_id"]
        
        
        ship_resp = await client.post(f"{SHIPPING_URL}/ship", json={
            "address": req.address.model_dump(),
            "items": [{"product_id": i.product_id, "quantity": i.quantity} for i in order_items]
        })
        tracking_id = "TRACK-DEFAULT"
        if ship_resp.status_code == 200:
            tracking_id = ship_resp.json()["tracking_id"]
        
        
        order_id = str(uuid.uuid4())
        order_result = OrderResult(
            order_id=order_id,
            shipping_tracking_id=tracking_id,
            shipping_cost=shipping_cost,
            shipping_address=req.address,
            items=order_items
        )
        
        
        try:
            await client.post(f"{EMAIL_URL}/send/confirmation", json={
                "email": req.email,
                "order": order_result.model_dump()
            })
        except Exception:
            pass  # order won't fail if email fails
        
        
        await client.post(f"{CART_URL}/cart/empty", json={"user_id": req.user_id})
        
        return PlaceOrderResponse(order=order_result)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8087)
