from fastapi import FastAPI
from pydantic import BaseModel
from typing import List
import random
import httpx
import os

app = FastAPI(title="Recommendation Service")

PRODUCT_CATALOG_URL = os.getenv("PRODUCT_CATALOG_URL", "http://productcatalog:8081")

class RecommendationRequest(BaseModel):
    user_id: str = ""
    product_ids: List[str] = []  # already in cart/viewed

class RecommendationResponse(BaseModel):
    product_ids: List[str]

@app.get("/health")
def health():
    return {"status": "ok"}

@app.post("/recommend", response_model=RecommendationResponse)
async def get_recommendations(req: RecommendationRequest):
    try:
        async with httpx.AsyncClient() as client:
            resp = await client.get(f"{PRODUCT_CATALOG_URL}/products", timeout=5.0)
            products = resp.json()
    except Exception: # fallback
        products = [{"id": pid} for pid in [
            "0PUK6V6EV0", "1YMWWN1N4O", "2ZYFJ3GM2N", "66VCHSJNUP",
            "6E92ZMYYFZ", "9SIQT8TOJO", "L9ECAV7KIM", "LS4PSXUNUM", "OLJCESPC7Z"
        ]]
    
    
    available = [p["id"] for p in products if p["id"] not in req.product_ids]
    recommended = random.sample(available, min(5, len(available)))
    
    return RecommendationResponse(product_ids=recommended)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8088)
