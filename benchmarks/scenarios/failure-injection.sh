#!/bin/bash

SERVICE=$1
FAILURE_RATE=${2:-0.3}
DELAY_MS=${3:-500}

if [ -z "$SERVICE" ]; then
    echo "Usage: ./failure-injection.sh  [failure_rate] [delay_ms]"
    echo "Services: payment, inventory, notification"
    exit 1
fi

PORT=8082
case $SERVICE in
    payment) PORT=8082 ;;
    inventory) PORT=8083 ;;
    notification) PORT=8084 ;;
    *)
        echo "Unknown service: $SERVICE"
        exit 1
        ;;
esac

echo "Injecting chaos into $SERVICE"
echo "Failure rate: $FAILURE_RATE"
echo "Delay: $DELAY_MS ms"

curl -X POST "http://localhost:$PORT/api/chaos/config?failureRate=$FAILURE_RATE&delayMs=$DELAY_MS"

echo ""
echo "Chaos enabled for $SERVICE"