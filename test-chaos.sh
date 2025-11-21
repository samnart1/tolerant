#!/bin/bash

# Test chaos engineering - inject failures into running services
# Usage: ./test-chaos.sh <service> [failure_rate] [delay_ms]

SERVICE=$1
FAILURE_RATE=${2:-0.3}
DELAY_MS=${3:-500}

if [ -z "$SERVICE" ]; then
    echo "Usage: ./test-chaos.sh <service> [failure_rate] [delay_ms]"
    echo ""
    echo "Services:"
    echo "  payment      - Payment service"
    echo "  inventory    - Inventory service"
    echo "  notification - Notification service"
    echo ""
    echo "Example:"
    echo "  ./test-chaos.sh payment 0.3 500"
    echo "  This will make payment service fail 30% of requests with 500ms delay"
    exit 1
fi

echo "========================================"
echo "Injecting Chaos"
echo "========================================"
echo "Service: $SERVICE"
echo "Failure rate: $FAILURE_RATE (${FAILURE_RATE}% of requests)"
echo "Delay: $DELAY_MS ms"
echo "========================================"
echo ""

./benchmarks/scenarios/failure-injection.sh $SERVICE $FAILURE_RATE $DELAY_MS

echo ""
echo "Chaos injection complete!"
echo ""
echo "To disable chaos:"
echo "  ./test-chaos.sh $SERVICE 0 0"
echo ""
echo "Monitor the impact:"
echo "  - Grafana: http://localhost:3000"
echo "  - Prometheus: http://localhost:9090"
echo "  - Service health: http://localhost:8081/actuator/health"
