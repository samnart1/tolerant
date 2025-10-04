.PHONY: help setup build deploy test clean experiments

# Colors for output
GREEN  := \033[0;32m
YELLOW := \033[1;33m
NC     := \033[0m

help:
	@echo "$(GREEN)Microservices Fault-Tolerance Thesis - Commands$(NC)"
	@echo ""
	@echo "$(YELLOW)Setup:$(NC)"
	@echo "  make setup-infra      Setup K8s cluster with all infrastructure"
	@echo "  make setup-tools      Install required CLI tools"
	@echo "  make setup-local      Start local development environment"
	@echo ""
	@echo "$(YELLOW)Build:$(NC)"
	@echo "  make build-all        Build all services"
	@echo "  make build-order      Build order service"
	@echo "  make build-inventory  Build inventory service"
	@echo "  make build-payment    Build payment service"
	@echo "  make build-analytics  Build analytics service"
	@echo ""
	@echo "$(YELLOW)Deploy:$(NC)"
	@echo "  make deploy-all       Deploy all services"
	@echo "  make deploy-infra     Deploy infrastructure only"
	@echo "  make redeploy         Rebuild and redeploy all"
	@echo ""
	@echo "$(YELLOW)Experiments:$(NC)"
	@echo "  make experiment-baseline         Run baseline tests"
	@echo "  make experiment-circuitbreaker   Test circuit breakers"
	@echo "  make experiment-retry            Test retry strategies"
	@echo "  make experiment-bulkhead         Test bulkhead patterns"
	@echo "  make experiment-protocol         Compare REST vs gRPC"
	@echo "  make experiment-all              Run all experiments"
	@echo ""
	@echo "$(YELLOW)Monitoring:$(NC)"
	@echo "  make dashboard        Open Grafana dashboard"
	@echo "  make prometheus       Open Prometheus"
	@echo "  make chaos-dashboard  Open Chaos Mesh dashboard"
	@echo "  make jaeger           Open Jaeger tracing"
	@echo "  make rabbitmq         Open RabbitMQ management"
	@echo "  make logs             Tail service logs"
	@echo ""
	@echo "$(YELLOW)Chaos:$(NC)"
	@echo "  make chaos-network    Apply network chaos"
	@echo "  make chaos-pod        Apply pod chaos"
	@echo "  make chaos-stop       Stop all chaos experiments"
	@echo ""
	@echo "$(YELLOW)Testing:$(NC)"
	@echo "  make test-unit        Run unit tests"
	@echo "  make test-integration Run integration tests"
	@echo "  make load-test        Run load test"
	@echo ""
	@echo "$(YELLOW)Utilities:$(NC)"
	@echo "  make status           Show cluster status"
	@echo "  make clean            Clean all resources"
	@echo "  make reset            Complete reset (destructive!)"
	@echo "  make port-forward     Setup all port forwards"

# Setup commands
setup-infra:
	@echo "$(GREEN)Setting up infrastructure...$(NC)"
	./scripts/deployment/setup-cluster.sh

setup-tools:
	@echo "$(GREEN)Installing required tools...$(NC)"
	@command -v kubectl >/dev/null 2>&1 || { echo "Please install kubectl"; exit 1; }
	@command -v docker >/dev/null 2>&1 || { echo "Please install docker"; exit 1; }
	@command -v kind >/dev/null 2>&1 || { echo "Please install kind"; exit 1; }
	@command -v helm >/dev/null 2>&1 || { echo "Please install helm"; exit 1; }
	@echo "$(GREEN)All tools installed!$(NC)"

setup-local:
	@echo "$(GREEN)Starting local development environment...$(NC)"
	docker-compose up -d
	@echo "$(GREEN)Local environment ready!$(NC)"

# Build commands
build-all: build-order build-inventory build-payment build-analytics

build-order:
	@echo "$(GREEN)Building Order Service...$(NC)"
	./scripts/deployment/build-all.sh order

build-inventory:
	@echo "$(GREEN)Building Inventory Service...$(NC)"
	cd services/inventory-service && docker build -t inventory-service:latest .
	kind load docker-image inventory-service:latest --name thesis-cluster

build-payment:
	@echo "$(GREEN)Building Payment Service...$(NC)"
	cd services/payment-service && docker build -t payment-service:latest .
	kind load docker-image payment-service:latest --name thesis-cluster

build-analytics:
	@echo "$(GREEN)Building Analytics Service...$(NC)"
	cd services/analytics-service && docker build -t analytics-service:latest .
	kind load docker-image analytics-service:latest --name thesis-cluster

# Deploy commands
deploy-all:
	@echo "$(GREEN)Deploying all services...$(NC)"
	./scripts/deployment/deploy-all.sh

deploy-infra:
	@echo "$(GREEN)Deploying infrastructure...$(NC)"
	kubectl apply -f infrastructure/kubernetes/base/rabbitmq.yaml
	kubectl apply -f infrastructure/istio/
	kubectl apply -f infrastructure/monitoring/prometheus/

redeploy: build-all
	@echo "$(GREEN)Redeploying services...$(NC)"
	kubectl rollout restart deployment order-service
	kubectl rollout restart deployment inventory-service
	kubectl rollout restart deployment payment-service
	kubectl rollout restart deployment analytics-service

# Experiment commands
experiment-baseline:
	@echo "$(GREEN)Running baseline experiment...$(NC)"
	./scripts/experiments/run-baseline.sh

experiment-circuitbreaker:
	@echo "$(GREEN)Running circuit breaker experiment...$(NC)"
	./scripts/experiments/run-circuitbreaker-experiment.sh

experiment-retry:
	@echo "$(GREEN)Running retry experiment...$(NC)"
	./scripts/experiments/run-retry-experiment.sh

experiment-bulkhead:
	@echo "$(GREEN)Running bulkhead experiment...$(NC)"
	./scripts/experiments/run-bulkhead-experiment.sh

experiment-protocol:
	@echo "$(GREEN)Running protocol comparison...$(NC)"
	./scripts/experiments/run-protocol-experiment.sh

experiment-all: experiment-baseline experiment-circuitbreaker experiment-retry
	@echo "$(GREEN)All experiments complete!$(NC)"

# Monitoring commands
dashboard:
	@echo "$(GREEN)Opening Grafana dashboard...$(NC)"
	@echo "Access at http://localhost:3000 (admin/admin)"
	kubectl port-forward -n monitoring svc/grafana 3000:80

prometheus:
	@echo "$(GREEN)Opening Prometheus...$(NC)"
	@echo "Access at http://localhost:9090"
	kubectl port-forward -n monitoring svc/prometheus-server 9090:80

chaos-dashboard:
	@echo "$(GREEN)Opening Chaos Mesh dashboard...$(NC)"
	@echo "Access at http://localhost:2333"
	kubectl port-forward -n chaos-mesh svc/chaos-dashboard 2333:2333

jaeger:
	@echo "$(GREEN)Opening Jaeger...$(NC)"
	@echo "Access at http://localhost:16686"
	kubectl port-forward -n istio-system svc/jaeger-query 16686:16686

rabbitmq:
	@echo "$(GREEN)Opening RabbitMQ management...$(NC)"
	@echo "Access at http://localhost:15672 (admin/admin)"
	kubectl port-forward svc/rabbitmq 15672:15672

logs:
	@echo "$(GREEN)Tailing service logs...$(NC)"
	kubectl logs -f -l app=order-service --all-containers=true

# Chaos commands
chaos-network:
	@echo "$(GREEN)Applying network chaos...$(NC)"
	kubectl apply -f infrastructure/chaos/network-chaos.yaml

chaos-pod:
	@echo "$(GREEN)Applying pod chaos...$(NC)"
	kubectl apply -f infrastructure/chaos/pod-chaos.yaml

chaos-stop:
	@echo "$(GREEN)Stopping all chaos experiments...$(NC)"
	kubectl delete networkchaos --all
	kubectl delete podchaos --all
	kubectl delete stresschaos --all

# Testing commands
test-unit:
	@echo "$(GREEN)Running unit tests...$(NC)"
	cd services/order-service && ./mvnw test
	cd services/inventory-service && go test ./...
	cd services/payment-service && go test ./...

test-integration:
	@echo "$(GREEN)Running integration tests...$(NC)"
	./scripts/test/run-integration-tests.sh

load-test:
	@echo "$(GREEN)Running load test...$(NC)"
	kubectl port-forward svc/order-service 8080:8080 &
	sleep 3
	wrk -t4 -c50 -d30s -s workload/wrk-scripts/baseline-test.lua http://localhost:8080
	pkill -f "port-forward"

# Utility commands
status:
	@echo "$(GREEN)Cluster Status:$(NC)"
	@echo ""
	@echo "Nodes:"
	@kubectl get nodes
	@echo ""
	@echo "Pods:"
	@kubectl get pods
	@echo ""
	@echo "Services:"
	@kubectl get svc
	@echo ""
	@echo "Chaos Experiments:"
	@kubectl get networkchaos,podchaos,stresschaos 2>/dev/null || echo "No chaos experiments running"

clean:
	@echo "$(YELLOW)Cleaning resources...$(NC)"
	kubectl delete -f infrastructure/kubernetes/overlays/dev/ --ignore-not-found=true
	kubectl delete networkchaos --all --ignore-not-found=true
	kubectl delete podchaos --all --ignore-not-found=true
	docker system prune -f
	@echo "$(GREEN)Cleanup complete!$(NC)"

reset:
	@echo "$(YELLOW)WARNING: This will delete EVERYTHING!$(NC)"
	@echo "Press Ctrl+C to cancel, or wait 5 seconds..."
	@sleep 5
	kind delete cluster --name thesis-cluster
	docker system prune -af
	rm -rf results/*
	@echo "$(GREEN)Reset complete!$(NC)"

port-forward:
	@echo "$(GREEN)Setting up port forwards...$(NC)"
	kubectl port-forward svc/order-service 8080:8080 &
	kubectl port-forward svc/inventory-service 8081:8081 &
	kubectl port-forward svc/payment-service 8082:8082 &
	kubectl port-forward svc/analytics-service 8084:8084 &
	kubectl port-forward -n monitoring svc/grafana 3000:80 &
	kubectl port-forward -n monitoring svc/prometheus-server 9090:80 &
	kubectl port-forward svc/rabbitmq 15672:15672 &
	@echo "$(GREEN)All services are port-forwarded!$(NC)"
	@echo ""
	@echo "Access URLs:"
	@echo "  Order Service:     http://localhost:8080"
	@echo "  Inventory Service: http://localhost:8081"
	@echo "  Payment Service:   http://localhost:8082"
	@echo "  Analytics Service: http://localhost:8084"
	@echo "  Grafana:          http://localhost:3000"
	@echo "  Prometheus:       http://localhost:9090"
	@echo "  RabbitMQ:         http://localhost:15672"
