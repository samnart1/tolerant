from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Optional
import redis
import json
import os

app = FastAPI(title="Cart Service")

REDIS_HOST = os.getenv("REDIS_HOST", "redis")
REDIS_PORT = int(os.getenv("REDIS_PORT", 6379))

def get_redis():
    return redis.Redis(host=REDIS_HOST, port=REDIS_PORT, decode_responses=True)

class CartItem(BaseModel):
    product_id: str
    quantity: int

class Cart(BaseModel):
    user_id: str
    items: List[CartItem]

class AddItemRequest(BaseModel):
    user_id: str
    item: CartItem

class EmptyCartRequest(BaseModel):
    user_id: str

class GetCartRequest(BaseModel):
    user_id: str

@app.get("/health")
def health():
    try:
        r = get_redis()
        r.ping()
        return {"status": "ok", "redis": "connected"}
    except Exception as e:
        return {"status": "degraded", "redis": str(e)}

@app.post("/cart/get", response_model=Cart)
def get_cart(req: GetCartRequest):
    r = get_redis()
    cart_key = f"cart:{req.user_id}"
    cart_data = r.get(cart_key)
    
    if cart_data:
        items = json.loads(cart_data)
        return Cart(user_id=req.user_id, items=[CartItem(**i) for i in items])
    return Cart(user_id=req.user_id, items=[])

@app.post("/cart/add")
def add_item(req: AddItemRequest):
    r = get_redis()
    cart_key = f"cart:{req.user_id}"
    
    
    cart_data = r.get(cart_key)
    items = json.loads(cart_data) if cart_data else []
    
    
    found = False
    for item in items:
        if item["product_id"] == req.item.product_id:
            item["quantity"] += req.item.quantity
            found = True
            break
    
    if not found:
        items.append({"product_id": req.item.product_id, "quantity": req.item.quantity})
    
    r.set(cart_key, json.dumps(items))
    return {"success": True}

@app.post("/cart/empty")
def empty_cart(req: EmptyCartRequest):
    r = get_redis()
    cart_key = f"cart:{req.user_id}"
    r.delete(cart_key)
    return {"success": True}


@app.get("/cart/{user_id}", response_model=Cart)
def get_cart_rest(user_id: str):
    return get_cart(GetCartRequest(user_id=user_id))

@app.delete("/cart/{user_id}")
def empty_cart_rest(user_id: str):
    return empty_cart(EmptyCartRequest(user_id=user_id))

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8082)
