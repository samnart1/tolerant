from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI(title="Currency Service")

# Exchange rates relative to USD
RATES = {
    "USD": 1.0,
    "EUR": 0.92,
    "JPY": 149.50,
    "CAD": 1.36,
    "GBP": 0.79,
    "TRY": 32.15
}

class Money(BaseModel):
    currency_code: str
    units: int
    nanos: int = 0

class ConversionRequest(BaseModel):
    from_money: Money
    to_code: str

class ConversionResponse(BaseModel):
    currency_code: str
    units: int
    nanos: int

@app.get("/health")
def health():
    return {"status": "ok"}

@app.get("/currencies")
def get_supported_currencies():
    return {"currency_codes": list(RATES.keys())}

@app.post("/convert", response_model=ConversionResponse)
def convert(req: ConversionRequest):
    from_rate = RATES.get(req.from_money.currency_code, 1.0)
    to_rate = RATES.get(req.to_code, 1.0)
    
    # Convert to USD first, then to target
    total_cents = req.from_money.units * 100 + req.from_money.nanos // 10_000_000
    usd_cents = total_cents / from_rate
    target_cents = usd_cents * to_rate
    
    units = int(target_cents // 100)
    nanos = int((target_cents % 100) * 10_000_000)
    
    return ConversionResponse(
        currency_code=req.to_code,
        units=units,
        nanos=nanos
    )

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8083)
