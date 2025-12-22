from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import uuid

app = FastAPI(title="Payment Service")

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

def luhn_check(card_number: str) -> bool:
    """Basic Luhn algorithm check for card validation"""
    digits = [int(d) for d in card_number if d.isdigit()]
    if len(digits) < 13:
        return False
    
    checksum = 0
    for i, digit in enumerate(reversed(digits)):
        if i % 2 == 1:
            digit *= 2
            if digit > 9:
                digit -= 9
        checksum += digit
    return checksum % 10 == 0

@app.get("/health")
def health():
    return {"status": "ok"}

@app.post("/charge", response_model=ChargeResponse)
def charge(req: ChargeRequest):
    # Basic validation
    card_num = req.credit_card.credit_card_number.replace(" ", "").replace("-", "")
    
    # Skip strict validation for testing - accept most card formats
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
    uvicorn.run(app, host="0.0.0.0", port=8085)
