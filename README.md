# Thesis: Circuit Breaker Performance Analysis in Microservices

Python recreation of Google's Online Boutique for analyzing fault tolerance patterns.

## Quick Start

```bash
# 1. Start all services (baseline mode)
docker-compose up -d

# 2. Verify everything is running
docker-compose ps
curl http://localhost:8080/health

# 3. Open frontend in browser
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

### Option 1: Run All Experiments Automatically (Recommended)

```bash
# Run full experiment suite (4 experiments x 30 min each = ~2 hours)
chmod +x run_experiments.sh
./run_experiments.sh 30 100 10

# Parameters: duration(min) users spawn_rate
# For quick test: ./run_experiments.sh 5 50 10
```

### Option 2: Run Individual Experiments

```bash
# Experiment 1: Baseline (no failures, no circuit breaker)
docker-compose --env-file env/baseline.env up -d

# Experiment 2: Baseline + Failures (shows cascading failure problem)
docker-compose --env-file env/baseline_failures.env up -d

# Experiment 3: Circuit Breaker (measures overhead)
docker-compose --env-file env/circuit_breaker.env up -d

# Experiment 4: Circuit Breaker + Failures (shows protection)
docker-compose --env-file env/circuit_breaker_failures.env up -d
```

### Running Locust Manually

```bash
# Start Locust with web UI
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
# Install analysis dependencies
cd analysis
pip install -r requirements.txt

# Start Jupyter
jupyter notebook benchmark_analysis.ipynb
```

The notebook generates:
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
PAYMENT_FAILURE_RATE=30    # Percentage of requests to fail
PAYMENT_LATENCY_MS=100     # Additional latency in ms
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
├── frontend/           # Entry point, has circuit breaker
├── productcatalog/     # Product listing
├── cart/               # Shopping cart (Redis)
├── checkout/           # Order orchestration
├── payment/            # Payment processing (failure injection)
├── shipping/           # Shipping quotes
├── email/              # Email notifications
├── currency/           # Currency conversion
├── recommendation/     # Product recommendations
├── ad/                 # Contextual ads
├── loadgenerator/      # Locust test file
├── env/                # Environment configs per experiment
├── analysis/           # Jupyter notebook for analysis
├── results/            # Benchmark output (CSV, JSON)
├── docker-compose.yml
├── run_experiments.sh  # Automated experiment runner
└── README.md
```

## Troubleshooting

```bash
# Check service logs
docker-compose logs frontend
docker-compose logs payment

# Check circuit breaker state
curl http://localhost:8080/metrics | jq

# Restart a single service
docker-compose restart frontend

# Full reset
docker-compose down -v
docker-compose up -d --build
```

## Thesis Focus

**Topic**: Analyzing the performance consequences of fault tolerance patterns in microservices

**Key Questions**:
1. What is the overhead of circuit breaker pattern under normal conditions?
2. How does circuit breaker improve resilience under failure scenarios?
3. What are optimal circuit breaker configuration parameters?
