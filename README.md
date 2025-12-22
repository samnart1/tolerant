# Thesis Microservices - Fault Tolerance Analysis

Python recreation of Google's Online Boutique microservices demo for analyzing circuit breaker patterns.

## Architecture

```
                    ┌─────────────────┐
                    │  loadgenerator  │
                    │    (Locust)     │
                    └────────┬────────┘
                             │ HTTP
                             ▼
                    ┌─────────────────┐
                    │    frontend     │◄── Circuit breakers here
                    │   (port 8080)   │
                    └────────┬────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
        ▼                    ▼                    ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│ productcatalog│   │    checkout   │   │      ad       │
│  (port 8081)  │   │  (port 8087)  │   │  (port 8089)  │
└───────────────┘   └───────┬───────┘   └───────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │         │         │         │         │
        ▼         ▼         ▼         ▼         ▼
┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐
│  cart   │ │ payment │ │shipping │ │  email  │ │currency │
│ (8082)  │ │ (8085)  │ │ (8084)  │ │ (8086)  │ │ (8083)  │
└────┬────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘
     │
     ▼
┌─────────┐
│  Redis  │
│ (6379)  │
└─────────┘
```

## Services

| Service | Port | Language | Description |
|---------|------|----------|-------------|
| frontend | 8080 | Python/FastAPI | HTTP endpoints for Locust |
| productcatalog | 8081 | Python/FastAPI | Product listing & details |
| cart | 8082 | Python/FastAPI | Cart storage (Redis) |
| currency | 8083 | Python/FastAPI | Currency conversion |
| shipping | 8084 | Python/FastAPI | Shipping quotes |
| payment | 8085 | Python/FastAPI | Mock payment processing |
| email | 8086 | Python/FastAPI | Mock email sending |
| checkout | 8087 | Python/FastAPI | Order orchestration |
| recommendation | 8088 | Python/FastAPI | Product recommendations |
| ad | 8089 | Python/FastAPI | Contextual ads |

## Quick Start

### 1. Start all services
```bash
docker-compose up -d
```

### 2. Verify services are running
```bash
docker-compose ps
```

### 3. Access the frontend
Open http://localhost:8080 in your browser

### 4. Run load tests with Locust
```bash
# Start Locust with the loadtest profile
docker-compose --profile loadtest up -d loadgenerator

# Open Locust UI
open http://localhost:8089
```

Or run Locust locally:
```bash
cd loadgenerator
pip install -r requirements.txt
locust -f locustfile.py --host=http://localhost:8080
```

## Development

### View logs for a specific service
```bash
docker-compose logs -f frontend
docker-compose logs -f checkout
```

### Restart a single service
```bash
docker-compose restart frontend
```

### Stop everything
```bash
docker-compose down
```

### Rebuild after code changes
```bash
docker-compose up -d --build
```

## Next Steps: Circuit Breaker Integration

The circuit breaker pattern will be added to the frontend service's outbound calls using `pybreaker`:

```python
from pybreaker import CircuitBreaker

# Create breaker for each downstream service
product_breaker = CircuitBreaker(
    fail_max=5,           # Open after 5 failures
    reset_timeout=30      # Try again after 30s
)

@product_breaker
async def get_products():
    # Call to productcatalog service
    ...
```

Key metrics to capture:
- Circuit state transitions (closed → open → half-open)
- Failure rates under load
- Recovery times
- Latency impact of circuit breaker overhead

## Thesis Focus

**Topic**: Analyzing the performance consequences of fault tolerance patterns in microservices

**This setup enables**:
1. Baseline measurements without circuit breakers
2. Injecting failures into downstream services
3. Measuring circuit breaker effectiveness
4. Comparing different circuit breaker configurations
