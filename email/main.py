from fastapi import FastAPI
from pydantic import BaseModel
from typing import List
import logging

app = FastAPI(title="Email Service")
logger = logging.getLogger(__name__)

class OrderItem(BaseModel):
    product_id: str
    quantity: int
    cost: dict  # Money object

class Address(BaseModel):
    street_address: str
    city: str
    state: str
    country: str
    zip_code: str

class OrderResult(BaseModel):
    order_id: str
    shipping_tracking_id: str
    shipping_cost: dict
    shipping_address: Address
    items: List[OrderItem]

class SendOrderConfirmationRequest(BaseModel):
    email: str
    order: OrderResult

class SendOrderConfirmationResponse(BaseModel):
    success: bool
    message: str

@app.get("/health")
def health():
    return {"status": "ok"}

@app.post("/send/confirmation", response_model=SendOrderConfirmationResponse)
def send_order_confirmation(req: SendOrderConfirmationRequest):
    # Mock email sending - just log it
    logger.info(f"Sending order confirmation to {req.email}")
    logger.info(f"Order ID: {req.order.order_id}")
    logger.info(f"Tracking: {req.order.shipping_tracking_id}")
    logger.info(f"Items: {len(req.order.items)}")
    
    # In a real service, this would send an actual email
    return SendOrderConfirmationResponse(
        success=True,
        message=f"Confirmation email sent to {req.email}"
    )

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8086)
