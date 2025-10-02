# Microservice Resilience Thesis Project
## Analyzing the Performance Consequences of Fault-Tolerance Patterns in Microservices
This repository contains the complete implementation for the thesis project analyzing resilience patterns in microservice architectures.
## Project Strucuture
- `services/` - Microservice implementations
	- `order-service/` - Java/Spring Boot order management service
	- `inventory-service/` - Go-based inventory service
	- `payment-service/` - Payment processing service with RabbitMQ
- `infrastructure/` - Kubernetes, Istio and infrastructure configurations
- `monitoring/` - Prometheus, Grafana, Jaeger configurations
- `experiments/` - Experiment scenarios, workloads and results
- `scripts/` - Automation scripts for deloyment and testing
- `docs/` - Architecture documentation and runbooks

## Quick Start

1. Prerequisites: Docker, Kubernetes (minikube/kind), kubectl, helm
2. Run setup: `./scripts/setup/install-prerequisites.sh`
3. Deply infrastructure: `./scripts/deploy/deploy-infrastructure.sh`
4. Deploy services: `./scripts/deploy/deploy-services.sh`
5. Run experiments: `./scripts/test/run-experiment.sh`

##
## Architecture

See `docs/architecture/` for architecture diagrams and design.
