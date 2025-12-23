from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import uuid
import os
import random
import time

app = FastAPI(title="Payment Service")

# Failure injection config
FAILURE_RATE = float(os.getenv("FAILURE_RATE", "0"))  # 0-100
LATENCY_MS = int(os.getenv("LATENCY_MS", "0"))  # Additional latency in ms

class CreditCardInfo(BaseModel):
    credit_card_number: str
    credit_card_cvv: int
    credit_card_expiration_year: int
    credit_card_expiration_month: int

class Money(BaseModel):
    currency_code: str
    units: int
    nanos: int = 0

class ChargeRequest(BaseModel):
    amount: Money
    credit_card: CreditCardInfo

class ChargeResponse(BaseModel):
    transaction_id: str

def maybe_fail():
    """Inject failure based on FAILURE_RATE"""
    if FAILURE_RATE > 0 and random.random() * 100 < FAILURE_RATE:
        raise HTTPException(status_code=500, detail="Simulated failure")

def maybe_delay():
    """Inject latency based on LATENCY_MS"""
    if LATENCY_MS > 0:
        time.sleep(LATENCY_MS / 1000.0)

@app.get("/health")
def health():
    return {
        "status": "ok",
        "failure_rate": FAILURE_RATE,
        "latency_ms": LATENCY_MS
    }

@app.get("/config")
def get_config():
    """Get current failure injection config"""
    return {
        "failure_rate": FAILURE_RATE,
        "latency_ms": LATENCY_MS
    }

@app.post("/charge", response_model=ChargeResponse)
def charge(req: ChargeRequest):
    # Inject failures/latency
    maybe_delay()
    maybe_fail()
    
    # Basic validation
    card_num = req.credit_card.credit_card_number.replace(" ", "").replace("-", "")
    
    if len(card_num) < 12:
        raise HTTPException(status_code=400, detail="Invalid card number")
    
    if req.credit_card.credit_card_cvv < 100 or req.credit_card.credit_card_cvv > 9999:
        raise HTTPException(status_code=400, detail="Invalid CVV")
    
    if req.amount.units <= 0 and req.amount.nanos <= 0:
        raise HTTPException(status_code=400, detail="Invalid amount")
    
    # Mock successful charge
    transaction_id = str(uuid.uuid4())
    return ChargeResponse(transaction_id=transaction_id)

if __name__ == "__main__":
    import uvicorn
    print(f"Payment Service starting with FAILURE_RATE={FAILURE_RATE}%, LATENCY_MS={LATENCY_MS}")
    uvicorn.run(app, host="0.0.0.0", port=8085)
