from fastapi import FastAPI
from pydantic import BaseModel
from typing import List
import random

app = FastAPI(title="Ad Service")

ADS = [
    {
        "redirect_url": "/product/0PUK6V6EV0",
        "text": "Vintage Typewriter - Write like Hemingway!"
    },
    {
        "redirect_url": "/product/1YMWWN1N4O", 
        "text": "Home Barista Kit - Brew like a pro!"
    },
    {
        "redirect_url": "/product/2ZYFJ3GM2N",
        "text": "Film Camera - Capture timeless moments!"
    },
    {
        "redirect_url": "/product/9SIQT8TOJO",
        "text": "City Bike - Ride in style!"
    },
    {
        "redirect_url": "/product/L9ECAV7KIM",
        "text": "Terrarium - Bring nature indoors!"
    }
]

class AdRequest(BaseModel):
    context_keys: List[str] = []

class Ad(BaseModel):
    redirect_url: str
    text: str

class AdResponse(BaseModel):
    ads: List[Ad]

@app.get("/health")
def health():
    return {"status": "ok"}

@app.post("/ads", response_model=AdResponse)
def get_ads(req: AdRequest):
    # Return 1-2 random ads
    num_ads = random.randint(1, 2)
    selected = random.sample(ADS, min(num_ads, len(ADS)))
    return AdResponse(ads=selected)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8089)
