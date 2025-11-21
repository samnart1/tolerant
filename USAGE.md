# Fault-Tolerance Patterns Performance Analysis - Usage Guide

## Quick Start

### 1. Start the System (Baseline)
```bash
./quick-start.sh
```

This starts all services with the baseline profile (no resilience patterns).

### 2. Test a Single Endpoint
```bash
curl -X POST http://localhost:8081/api/orders \
  -H 'Content-Type: application/json' \
  -d '{
    "customerId":"CUST001",
    "productId":"PROD001",
    "quantity":5,
    "amount":99.99,
    "customerEmail":"test@example.com",
    "customerPhone":"+1234567890"
  }'
```

### 3. Stop the System
```bash
./cleanup.sh
```

---

## Running Experiments

### Single Profile Experiment

Run an experiment with a specific resilience pattern:

```bash
./run-experiment.sh <profile> [threads] [duration] [chaos] [failure_rate] [delay_ms]
```

**Examples:**

```bash
# Baseline (no resilience) - 10 threads for 5 minutes
./run-experiment.sh baseline 10 300

# Circuit breaker pattern with chaos - 20% failure rate
./run-experiment.sh circuit-breaker 20 600 true 0.2 300

# Production-combined with heavy load
./run-experiment.sh production-combined 50 900
```

### All Profiles Benchmark

Run automated benchmarks for all profiles:

```bash
cd benchmarks/scripts
./run-all-profiles.sh
```

This will:
- Test each profile sequentially
- Run load tests
- Collect metrics
- Save results for comparison

---

## Available Resilience Profiles

| Profile | Description | Use Case |
|---------|-------------|----------|
| `baseline` | No resilience patterns | Baseline performance measurement |
| `retry-timeout` | Retry + Timeout mechanisms | Transient failure handling |
| `circuit-breaker` | Circuit breaker pattern | Prevent cascading failures |
| `bulkhead` | Resource isolation | Limit resource exhaustion |
| `async-queue` | Async messaging (RabbitMQ) | Decouple non-critical operations |
| `production-combined` | All patterns together | Production-ready configuration |

---

## Chaos Engineering

### Inject Failures

Inject failures into running services to test resilience:

```bash
./test-chaos.sh <service> [failure_rate] [delay_ms]
```

**Examples:**

```bash
# Make payment service fail 30% of requests with 500ms delay
./test-chaos.sh payment 0.3 500

# Make inventory service fail 50% with 1000ms delay
./test-chaos.sh inventory 0.5 1000

# Disable chaos
./test-chaos.sh payment 0 0
```

**Available services:** `payment`, `inventory`, `notification`

---

## Monitoring & Observability

### Access Dashboards

- **Eureka Dashboard**: http://localhost:8761
- **Grafana**: http://localhost:3000 (admin/admin)
- **Prometheus**: http://localhost:9090
- **RabbitMQ Management**: http://localhost:15672 (guest/guest)

### Service Health Checks

```bash
# Order Service
curl http://localhost:8081/actuator/health

# Payment Service
curl http://localhost:8082/actuator/health

# Inventory Service
curl http://localhost:8083/actuator/health

# Notification Service
curl http://localhost:8084/actuator/health
```

### Prometheus Metrics

```bash
# Order Service metrics
curl http://localhost:8081/actuator/prometheus

# Payment Service metrics
curl http://localhost:8082/actuator/prometheus
```

---

## Results & Analysis

### Result Locations

After running experiments, results are saved in:

- **JMeter Results**: `benchmarks/results/<profile>/`
- **Metrics Data**: `infrastructure/results/<profile>/`
- **HTML Reports**: `benchmarks/results/<profile>/html-report/`

### Analyze Results

```bash
cd benchmarks/scripts
./analyze-results.sh
```

---

## Manual Workflow

### 1. Start with Specific Profile

```bash
cd infrastructure/scripts
./start-with-profile.sh circuit-breaker
```

### 2. Wait for Services

Check health status:
```bash
curl http://localhost:8081/actuator/health
```

### 3. Run Load Test

```bash
cd benchmarks/scripts
./run-benchmark.sh circuit-breaker 10 300 10
```

Parameters:
- Profile: `circuit-breaker`
- Threads: `10`
- Duration: `300` seconds
- Ramp-up: `10` seconds

### 4. Collect Metrics

```bash
cd infrastructure/scripts
./collect-metrics.sh circuit-breaker 300
```

### 5. Stop System

```bash
cd infrastructure
docker compose down
```

---

## Troubleshooting

### Services Not Starting

Check Docker logs:
```bash
docker compose logs -f order-service
docker compose logs -f payment-service
```

### Health Checks Failing

Wait longer (services need 60-90 seconds to start):
```bash
watch -n 5 'curl -s http://localhost:8081/actuator/health | jq'
```

### Port Conflicts

Stop existing containers:
```bash
./cleanup.sh
docker ps -a
```

### Reset Everything

```bash
./cleanup.sh
docker system prune -f
./quick-start.sh
```

---

## For Your Thesis

### Recommended Experiments

1. **Baseline Measurement**
   ```bash
   ./run-experiment.sh baseline 20 600
   ```

2. **Each Pattern Under Normal Load**
   ```bash
   ./run-experiment.sh retry-timeout 20 600
   ./run-experiment.sh circuit-breaker 20 600
   ./run-experiment.sh bulkhead 20 600
   ./run-experiment.sh async-queue 20 600
   ./run-experiment.sh production-combined 20 600
   ```

3. **Each Pattern Under Failure Conditions**
   ```bash
   ./run-experiment.sh circuit-breaker 20 600 true 0.3 500
   # Repeat for other profiles
   ```

4. **Comparative Analysis**
   ```bash
   cd benchmarks/scripts
   ./run-all-profiles.sh
   ```

### Key Metrics to Analyze

- **Throughput**: Requests/second
- **Latency**: p50, p95, p99 percentiles
- **Error Rate**: Percentage of failed requests
- **Resource Usage**: CPU, Memory, Network
- **Recovery Time**: Time to recover from failures
- **Circuit Breaker Metrics**: Open/half-open/closed states

### Export Metrics for Analysis

Prometheus metrics are exported in the results directory. Use these for:
- Excel/CSV analysis
- Python data science (pandas, matplotlib)
- R statistical analysis
- Grafana visualization

---

## Tips

1. **Always warm up** - Let services run for 30-60 seconds before load testing
2. **Multiple runs** - Run each experiment 3-5 times for statistical significance
3. **Control variables** - Test one pattern at a time
4. **Document everything** - Keep notes on configurations and observations
5. **Monitor resources** - Watch CPU/memory during experiments
6. **Clean between runs** - Use `./cleanup.sh` between experiments

---

## Next Steps

1. Run baseline experiments
2. Test each resilience pattern individually
3. Inject chaos to test under failure conditions
4. Compare performance metrics
5. Analyze trade-offs for your thesis
