# Analyzing the Performance Consequences of Fault-Tolerance Patterns in Microservices

## 🎓 Master's Thesis Implementation

A comprehensive experimental framework for evaluating fault-tolerance patterns in microservice architectures, examining the trade-offs between resilience and performance across different communication protocols.

## 🏗️ Architecture

```
┌─────────────┐      REST/gRPC      ┌──────────────┐
│   Order     │ ──────────────────> │  Inventory   │
│  Service    │                     │   Service    │
│ (Spring)    │                     │     (Go)     │
└─────────────┘                     └──────────────┘
      │
      │ RabbitMQ (Async)
      ▼
┌─────────────┐
│  Payment    │
│  Service    │
│    (Go)     │
└─────────────┘
```

## 🔧 Tech Stack

- **Languages**: Java (Spring Boot), Go, C++
- **Orchestration**: Kubernetes + Istio
- **Messaging**: RabbitMQ
- **Monitoring**: Prometheus, Grafana, Jaeger
- **Chaos**: Chaos Mesh, Toxiproxy
- **Load Testing**: wrk, k6

## 🚀 Quick Start

```bash
# 1. Setup infrastructure
make setup-infra

# 2. Deploy services
make deploy-all

# 3. Run baseline experiment
make experiment-baseline

# 4. View dashboards
make dashboard
```

## 📊 Experiments

1. **Circuit Breaker Comparison**: App-level vs Service Mesh
2. **Retry Strategy Impact**: Exponential backoff vs Fixed
3. **Bulkhead Patterns**: Thread pool vs Semaphore
4. **Protocol Performance**: REST vs gRPC under failures
5. **Async Resilience**: Message queue failure modes

## 📁 Project Structure

```
.
├── services/              # Microservice implementations
│   ├── order-service/    # Java/Spring Boot
│   ├── inventory-service/# Go
│   ├── payment-service/  # Go
│   └── analytics-service/# C++ (optional)
├── infrastructure/        # K8s, Istio, monitoring
├── scripts/              # Automation scripts
├── workload/             # Load generators
└── results/              # Experimental data
```

## 📖 Documentation

- [Architecture Guide](docs/architecture/README.md)
- [API Documentation](docs/api/README.md)
- [Experiment Protocols](docs/experiments/README.md)

## 🎯 Key Features

- ✅ Multi-language implementation
- ✅ Application & infrastructure-level resilience
- ✅ Automated fault injection
- ✅ Comprehensive metrics collection
- ✅ Reproducible experiments
- ✅ Real-time monitoring dashboards

## 📄 License

MIT License - See LICENSE file

## 👨‍🎓 Author

[Your Name] - Master's Thesis, [University Name], 2025
