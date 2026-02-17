# Thesis: Analyzing Performance Consequence of Fault Tolerance in Microservices (Circuit Breaker)

Python recreation of Google's Online Boutique for analyzing fault tolerance patterns.

## Quick Start

```bash
docker-compose up -d

docker-compose ps
curl http://localhost:8080/health

open http://localhost:8080
```

## Architecture

```
                    ┌─────────────────┐
                    │  loadgenerator  │
                    │    (Locust)     │
                    └────────┬────────┘
                             │ HTTP
                             ▼
                    ┌─────────────────┐
                    │    frontend     │ ◄── Circuit Breaker Here
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
        │                   │
        │   ┌───────────────┼───────────────┐
        │   │       │       │       │       │
        │   ▼       ▼       ▼       ▼       ▼
        │ cart  payment shipping email  currency
        │ (8082) (8085)  (8084)  (8086)  (8083)
        │   │
        │   ▼
        │ Redis
        │ (6379)
        │
        └───► recommendation (8088)
```

## Running Experiments

### 1: Running All Experiments

```bash
chmod +x run_experiments.sh
./run_experiments.sh 30 100 10

# For quick test: ./run_experiments.sh 5 50 10
```

### 2: Running Individual Experiments

```bash
docker-compose --env-file env/baseline.env up -d

docker-compose --env-file env/baseline_failures.env up -d

docker-compose --env-file env/circuit_breaker.env up -d

docker-compose --env-file env/circuit_breaker_failures.env up -d
```

### Running Locust Manually

```bash
docker run --rm -p 8089:8089 \
    --network=online-boutique \
    -v $(pwd)/loadgenerator:/mnt/locust \
    locustio/locust:latest \
    -f /mnt/locust/locustfile.py \
    --host=http://frontend:8080

# Open http://localhost:8089 and configure:
# - Number of users: 100
# - Spawn rate: 10
# - Run time: 30m
```

## Analyzing Results

```bash
cd analysis
pip install -r requirements.txt

jupyter notebook benchmark_analysis.ipynb
```

The notebook will generate the ff:
- Summary statistics table
- Failure rate comparison chart
- Response time percentiles chart
- Throughput comparison
- Time-series analysis
- Circuit breaker state metrics

## Experiment Configurations

| Experiment | Circuit Breaker | Failure Rate | Purpose |
|------------|-----------------|--------------|---------|
| baseline | OFF | 0% | Control measurement |
| baseline_failures | OFF | 30% on payment | Shows cascading failure problem |
| circuit_breaker | ON | 0% | Measures CB overhead |
| circuit_breaker_failures | ON | 30% on payment | Shows CB protection |

## Environment Variables

### Circuit Breaker Config (frontend)
```bash
CIRCUIT_BREAKER_ENABLED=true/false
CB_FAIL_MAX=5              # Failures before opening
CB_RESET_TIMEOUT=30        # Seconds before half-open
```

### Failure Injection (per service)
```bash
PAYMENT_FAILURE_RATE=30    
PAYMENT_LATENCY_MS=100     
```

## Key Metrics to Analyze

1. **Failure Rate** - % of failed requests
2. **Response Time** - p50, p95, p99 latencies
3. **Throughput** - Requests per second
4. **Circuit Breaker Events** - State transitions (closed → open → half-open)
5. **Rejected Requests** - Requests fast-failed by open circuit

## Project Structure

```
thesis-microservices/
├── frontend/           
├── productcatalog/     
├── cart/               
├── checkout/           
├── payment/            
├── shipping/           
├── email/              
├── currency/           
├── recommendation/     
├── ad/                 
├── loadgenerator/      
├── env/                
├── analysis/           
├── results/            
├── docker-compose.yml
├── run_experiments.sh  
└── README.md
```

## Some Troubleshooting

```bash
docker-compose logs frontend
docker-compose logs payment

curl http://localhost:8080/metrics | jq

docker-compose restart frontend

docker-compose down -v
docker-compose up -d --build
```
