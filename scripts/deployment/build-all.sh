#!/bin/bash
# Build all microservices and load into kind cluster

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "🔨 Building all microservices"
echo "=================================================="

# Build Order Service (Spring Boot)
build_order_service() {
    echo -e "${YELLOW}Building Order Service...${NC}"
    cd "$PROJECT_ROOT/services/order-service"
    
    # Build with Maven
    ./mvnw clean package -DskipTests
    
    # Build Docker image
    docker build -t order-service:latest .
    
    # Load into kind
    kind load docker-image order-service:latest --name thesis-cluster
    
    echo -e "${GREEN}✓ Order Service built${NC}"
}

# Build Inventory Service (Go)
build_inventory_service() {
    echo -e "${YELLOW}Building Inventory Service...${NC}"
    cd "$PROJECT_ROOT/services/inventory-service"
    
    # Build Docker image
    docker build -t inventory-service:latest .
    
    # Load into kind
    kind load docker-image inventory-service:latest --name thesis-cluster
    
    echo -e "${GREEN}✓ Inventory Service built${NC}"
}

# Build Payment Service (Go)
build_payment_service() {
    echo -e "${YELLOW}Building Payment Service...${NC}"
    cd "$PROJECT_ROOT/services/payment-service"
    
    # Build Docker image
    docker build -t payment-service:latest .
    
    # Load into kind
    kind load docker-image payment-service:latest --name thesis-cluster
    
    echo -e "${GREEN}✓ Payment Service built${NC}"
}

# Main execution
main() {
    build_order_service
    build_inventory_service
    build_payment_service
    
    echo ""
    echo "=================================================="
    echo -e "${GREEN}All services built successfully!${NC}"
    echo "=================================================="
    echo ""
    echo "Next: Deploy services with 'make deploy-all'"
}

main