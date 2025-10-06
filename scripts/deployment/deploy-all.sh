#!/bin/bash
# Deploy all services to Kubernetes cluster

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "🚀 Deploying all services"
echo "=================================================="

cd "$PROJECT_ROOT"

# Deploy services
deploy_services() {
    echo -e "${YELLOW}Deploying microservices...${NC}"
    
    # Deploy in order: RabbitMQ -> Services
    kubectl apply -f infrastructure/kubernetes/base/rabbitmq.yaml
    
    echo "Waiting for RabbitMQ..."
    kubectl wait --for=condition=ready pod -l app=rabbitmq --timeout=120s
    
    kubectl apply -f infrastructure/kubernetes/base/order-service.yaml
    kubectl apply -f infrastructure/kubernetes/base/inventory-service.yaml
    kubectl apply -f infrastructure/kubernetes/base/payment-service.yaml
    
    echo -e "${GREEN}✓ Services deployed${NC}"
}

# Wait for services to be ready
wait_for_services() {
    echo -e "${YELLOW}Waiting for services to be ready...${NC}"
    
    kubectl wait --for=condition=ready pod -l app=order-service --timeout=180s
    kubectl wait --for=condition=ready pod -l app=inventory-service --timeout=180s
    kubectl wait --for=condition=ready pod -l app=payment-service --timeout=180s
    
    echo -e "${GREEN}✓ All services are ready${NC}"
}

# Apply Istio configuration
apply_istio_config() {
    echo -e "${YELLOW}Applying Istio configuration...${NC}"
    
    kubectl apply -f infrastructure/istio/gateway.yaml
    kubectl apply -f infrastructure/istio/destination-rules.yaml
    
    echo -e "${GREEN}✓ Istio configuration applied${NC}"
}

# Print service information
print_info() {
    echo ""
    echo "=================================================="
    echo -e "${GREEN}Deployment complete!${NC}"
    echo "=================================================="
    echo ""
    echo "Services:"
    kubectl get pods -l 'app in (order-service,inventory-service,payment-service,rabbitmq)'
    echo ""
    echo "To access the Order Service:"
    echo "  kubectl port-forward svc/order-service 8080:8080"
    echo "  curl -X POST http://localhost:8080/api/v1/orders \\"
    echo "    -H 'Content-Type: application/json' \\"
    echo "    -d '{\"productId\":\"PROD-001\",\"quantity\":2,\"paymentMethod\":\"credit_card\"}'"
    echo ""
    echo "Run experiments:"
    echo "  make experiment-baseline"
    echo "  make experiment-circuitbreaker"
    echo ""
}

# Main execution
main() {
    deploy_services
    wait_for_services
    # apply_istio_config
    print_info
}

main