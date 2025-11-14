# Master's Thesis Implementation

## Analyzing the Performance Consequences of Fault-Tolerance Patterns in Microservices

A comprehensive experimental framework for evaluating fault-tolerance patterns in microservice architectures, examining the trade-offs between resilience and performance across different communication protocols and resilience strategies.

---

## Architecture

### Microservices

- **Eureka Server**: Service discovery and registration
- **API Gateway**: Entry point with routing and load balancing
- **Order Service**: Main orchestrator implementing resilience patterns
- **Payment Service**: Handles payment processing (chaos-enabled)
- **Inventory Service**: Manages inventory reservations (chaos-enabled)
- **Notification Service**: Sends notifications (sync/async)
- **RabbitMQ**: Message broker for async communication

### Resilience Patterns Implemented

1. **Retry**: Automatic retry on transient failures
2. **Timeout/TimeLimiter**: Prevent long-running operations
3. **Circuit Breaker**: Prevent cascading failures
4. **Bulkhead**: Resource isolation and thread pool management
5. **Async Messaging**: Decouple non-critical operations (RabbitMQ)

### Monitoring Stack

- **Prometheus**: Metrics collection
- **Grafana**: Visualization and dashboards
- **Spring Boot Actuator**: Application metrics and health checks

---

## Quick Start

### Prerequisites

- Docker & Docker Compose
- JMeter (for load testing)
- curl, jq (for scripts)

### Start the System

```bash
# Quick start with baseline profile
./quick-start.sh

# Or start with a specific profile
./infrastructure/scripts/start-with-profile.sh circuit-breaker
```

### Run an Experiment

```bash
./run-experiment.sh baseline 10 300
```

### Stop the System

```bash
./cleanup.sh
```

---

## Project Structure

```
tolerant/
├── services/                    # Microservices
│   ├── order-service/          # Main service with resilience patterns
│   ├── payment-service/        # Payment processing
│   ├── inventory-service/      # Inventory management
│   ├── notification-service/   # Notifications
│   ├── api-gateway/           # API Gateway
│   └── eureka-server/         # Service discovery
│
├── infrastructure/             # Infrastructure configuration
│   ├── docker-compose.yml     # Service orchestration
│   ├── scripts/               # Deployment scripts
│   ├── prometheus/            # Prometheus config
│   └── grafana/               # Grafana dashboards
│
├── benchmarks/                 # Load testing and benchmarks
│   ├── jmeter/                # JMeter test plans
│   ├── scripts/               # Benchmark automation
│   └── scenarios/             # Chaos engineering scenarios
│
├── quick-start.sh             # Quick start script
├── run-experiment.sh          # Run single experiment
├── test-chaos.sh              # Inject failures
├── cleanup.sh                 # Cleanup resources
└── USAGE.md                   # Detailed usage guide
```

---

## Resilience Profiles

| Profile | Retry | Timeout | Circuit Breaker | Bulkhead | Async Queue |
|---------|-------|---------|----------------|----------|-------------|
| baseline | ❌ | ❌ | ❌ | ❌ | ❌ |
| retry-timeout | ✅ | ✅ | ❌ | ❌ | ❌ |
| circuit-breaker | ✅ | ✅ | ✅ | ❌ | ❌ |
| bulkhead | ✅ | ✅ | ❌ | ✅ | ❌ |
| async-queue | ✅ | ✅ | ❌ | ❌ | ✅ |
| production-combined | ✅ | ✅ | ✅ | ✅ | ✅ |

---

## Documentation

- **[USAGE.md](USAGE.md)**: Comprehensive usage guide with examples
- **Configuration Files**: Each service has profile-specific `application-{profile}.yml`
- **Scripts**: All scripts include usage documentation

---

## For Thesis Research

### Experimental Methodology

1. **Baseline Measurement**: Measure performance without resilience patterns
2. **Individual Pattern Testing**: Test each pattern in isolation
3. **Combined Pattern Testing**: Test multiple patterns together
4. **Failure Injection**: Use chaos engineering to simulate failures
5. **Comparative Analysis**: Compare metrics across all configurations

### Key Research Questions

- What is the performance overhead of each resilience pattern?
- How do patterns affect latency, throughput, and resource usage?
- Which patterns are most effective under different failure scenarios?
- What are the trade-offs when combining multiple patterns?

### Metrics to Collect

- **Performance**: Throughput (req/s), Latency (p50, p95, p99)
- **Reliability**: Error rate, Success rate, Recovery time
- **Resources**: CPU usage, Memory usage, Thread pool utilization
- **Resilience**: Circuit breaker states, Retry attempts, Queue depth

---

## Running Experiments

See [USAGE.md](USAGE.md) for detailed instructions on:

- Running single experiments
- Running comprehensive benchmarks
- Injecting chaos/failures
- Collecting and analyzing metrics
- Accessing monitoring dashboards

---

## Technologies Used

- **Spring Boot 3.4**: Microservices framework
- **Spring Cloud**: Service discovery, configuration
- **Resilience4j**: Resilience patterns implementation
- **RabbitMQ**: Message broker
- **H2 Database**: In-memory database
- **Prometheus + Grafana**: Monitoring and visualization
- **Docker**: Containerization
- **JMeter**: Load testing

---

## License

This is a research project for academic purposes.

---

## Contact

For questions about this implementation, please refer to the usage documentation or contact the project maintainer.
