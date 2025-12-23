from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List
import os
import random
import time

app = FastAPI(title="Product Catalog Service")

# Failure injection config
FAILURE_RATE = float(os.getenv("FAILURE_RATE", "0"))
LATENCY_MS = int(os.getenv("LATENCY_MS", "0"))

# Product data matching the IDs from locustfile
PRODUCTS = {
    "0PUK6V6EV0": {
        "id": "0PUK6V6EV0",
        "name": "Vintage Typewriter",
        "description": "A classic typewriter for the modern writer.",
        "picture": "/static/img/products/typewriter.jpg",
        "price_usd": {"currency_code": "USD", "units": 67, "nanos": 990000000},
        "categories": ["vintage", "office"]
    },
    "1YMWWN1N4O": {
        "id": "1YMWWN1N4O",
        "name": "Home Barista Kit",
        "description": "Complete coffee brewing set for home.",
        "picture": "/static/img/products/barista-kit.jpg",
        "price_usd": {"currency_code": "USD", "units": 124, "nanos": 0},
        "categories": ["kitchen", "coffee"]
    },
    "2ZYFJ3GM2N": {
        "id": "2ZYFJ3GM2N",
        "name": "Film Camera",
        "description": "Retro film camera for photography enthusiasts.",
        "picture": "/static/img/products/camera.jpg",
        "price_usd": {"currency_code": "USD", "units": 79, "nanos": 990000000},
        "categories": ["photography", "vintage"]
    },
    "66VCHSJNUP": {
        "id": "66VCHSJNUP",
        "name": "Camp Mug",
        "description": "Durable enamel mug for outdoor adventures.",
        "picture": "/static/img/products/camp-mug.jpg",
        "price_usd": {"currency_code": "USD", "units": 9, "nanos": 990000000},
        "categories": ["outdoor", "kitchen"]
    },
    "6E92ZMYYFZ": {
        "id": "6E92ZMYYFZ",
        "name": "Air Plant",
        "description": "Low maintenance air plant for your desk.",
        "picture": "/static/img/products/air-plant.jpg",
        "price_usd": {"currency_code": "USD", "units": 12, "nanos": 300000000},
        "categories": ["home", "plants"]
    },
    "9SIQT8TOJO": {
        "id": "9SIQT8TOJO",
        "name": "City Bike",
        "description": "Stylish city bike for urban commuting.",
        "picture": "/static/img/products/city-bike.jpg",
        "price_usd": {"currency_code": "USD", "units": 789, "nanos": 500000000},
        "categories": ["cycling", "outdoor"]
    },
    "L9ECAV7KIM": {
        "id": "L9ECAV7KIM",
        "name": "Terrarium",
        "description": "Glass terrarium with succulents.",
        "picture": "/static/img/products/terrarium.jpg",
        "price_usd": {"currency_code": "USD", "units": 36, "nanos": 450000000},
        "categories": ["home", "plants"]
    },
    "LS4PSXUNUM": {
        "id": "LS4PSXUNUM",
        "name": "Metal Camping Lamp",
        "description": "Vintage-style metal lamp for camping.",
        "picture": "/static/img/products/camp-lamp.jpg",
        "price_usd": {"currency_code": "USD", "units": 24, "nanos": 990000000},
        "categories": ["outdoor", "vintage"]
    },
    "OLJCESPC7Z": {
        "id": "OLJCESPC7Z",
        "name": "Sunglasses",
        "description": "Retro sunglasses with UV protection.",
        "picture": "/static/img/products/sunglasses.jpg",
        "price_usd": {"currency_code": "USD", "units": 19, "nanos": 990000000},
        "categories": ["accessories", "vintage"]
    }
}

class Product(BaseModel):
    id: str
    name: str
    description: str
    picture: str
    price_usd: dict
    categories: List[str]

def maybe_fail():
    if FAILURE_RATE > 0 and random.random() * 100 < FAILURE_RATE:
        raise HTTPException(status_code=500, detail="Simulated failure")

def maybe_delay():
    if LATENCY_MS > 0:
        time.sleep(LATENCY_MS / 1000.0)

@app.get("/health")
def health():
    return {
        "status": "ok",
        "failure_rate": FAILURE_RATE,
        "latency_ms": LATENCY_MS
    }

@app.get("/products", response_model=List[Product])
def list_products():
    maybe_delay()
    maybe_fail()
    return list(PRODUCTS.values())

@app.get("/product/{product_id}", response_model=Product)
def get_product(product_id: str):
    maybe_delay()
    maybe_fail()
    if product_id not in PRODUCTS:
        raise HTTPException(status_code=404, detail="Product not found")
    return PRODUCTS[product_id]

@app.post("/search")
def search_products(query: str = ""):
    maybe_delay()
    maybe_fail()
    results = []
    for product in PRODUCTS.values():
        if query.lower() in product["name"].lower() or query.lower() in product["description"].lower():
            results.append(product)
    return {"results": results}

if __name__ == "__main__":
    import uvicorn
    print(f"ProductCatalog Service starting with FAILURE_RATE={FAILURE_RATE}%, LATENCY_MS={LATENCY_MS}")
    uvicorn.run(app, host="0.0.0.0", port=8081)
