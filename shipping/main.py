from fastapi import FastAPI
from pydantic import BaseModel
from typing import List
import random
import uuid

app = FastAPI(title="Shipping Service")

class Address(BaseModel):
    street_address: str = ""
    city: str = ""
    state: str = ""
    country: str = ""
    zip_code: str = ""

class CartItem(BaseModel):
    product_id: str
    quantity: int

class ShippingRequest(BaseModel):
    address: Address
    items: List[CartItem]

class Money(BaseModel):
    currency_code: str
    units: int
    nanos: int = 0

class QuoteResponse(BaseModel):
    cost_usd: Money

class ShipOrderRequest(BaseModel):
    address: Address
    items: List[CartItem]

class ShipOrderResponse(BaseModel):
    tracking_id: str

@app.get("/health")
def health():
    return {"status": "ok"}

@app.post("/quote", response_model=QuoteResponse)
def get_quote(req: ShippingRequest):
    total_items = sum(item.quantity for item in req.items)
    cost = 5.0 + (total_items * 1.0)
    
    return QuoteResponse(
        cost_usd=Money(
            currency_code="USD",
            units=int(cost),
            nanos=int((cost % 1) * 1_000_000_000)
        )
    )

@app.post("/ship", response_model=ShipOrderResponse)
def ship_order(req: ShipOrderRequest):
    tracking_id = f"TR{uuid.uuid4().hex[:12].upper()}"
    return ShipOrderResponse(tracking_id=tracking_id)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8084)
