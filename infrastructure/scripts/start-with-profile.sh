#!/bin/bash

PROFILE=$1

if [ -z "$PROFILE" ]; then
    echo "Usage: ./start-with-profile.sh <profile>"
    echo "Profiles: baseline, retry-timeout, circuit-breaker, bulkhead, async-queue, production-combined"
    exit 1
fi

echo "========================================"
echo "Starting system with profile: $PROFILE"
echo "========================================"

cd "$(dirname "$0")/.."

# Stop existing containers
echo "Stopping existing containers..."
docker compose down

# Build services
echo "Building services..."
docker compose build

# Start services with the specified profile
echo "Starting services with profile: $PROFILE"
PROFILE=$PROFILE docker compose up -d

echo ""
echo "Waiting for services to start (90s)..."
sleep 90

# Check health
echo ""
echo "========================================"
echo "Service Health Status:"
echo "========================================"

check_health() {
    SERVICE=$1
    PORT=$2
    echo -n "$SERVICE ($PORT): "
    STATUS=$(curl -s http://localhost:$PORT/actuator/health 2>/dev/null | jq -r '.status' 2>/dev/null)
    if [ "$STATUS" == "UP" ]; then
        echo "✓ UP"
    else
        echo "✗ DOWN or UNAVAILABLE"
    fi
}

check_health "Eureka Server    " "8761"
check_health "API Gateway      " "8080"
check_health "Order Service    " "8081"
check_health "Payment Service  " "8082"
check_health "Inventory Service" "8083"
check_health "Notification Svc " "8084"

echo ""
echo "========================================"
echo "System Ready!"
echo "========================================"
echo "Profile: $PROFILE"
echo ""
echo "Services:"
echo "  Eureka Dashboard: http://localhost:8761"
echo "  API Gateway:      http://localhost:8080"
echo "  Order Service:    http://localhost:8081"
echo "  Payment Service:  http://localhost:8082"
echo "  Inventory Service: http://localhost:8083"
echo "  Notification Svc: http://localhost:8084"
echo "  RabbitMQ Mgmt:    http://localhost:15672 (guest/guest)"
echo ""
echo "To inject failures:"
echo "  cd ../benchmarks/scenarios"
echo "  ./failure-injection.sh <service> [failure_rate] [delay_ms]"
echo "========================================"