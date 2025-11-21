#!/bin/bash

#metrics from prometheus
PROFILE=$1
DURATION=${2:-300}
OUTPUT_DIR="results/$PROFILE"

if [ -z "$PROFILE" ]; then
    echo "usage: ./collect-metrics.sh [duration_seconds]"
    exit 1
fi

mkdir -p $OUTPUT_DIR

echo "collecting metrics for profile: $PROFILE"
echo "duration: $DURATION seconds"
echo "output directory: $OUTPUT_DIR"

#metrics at intervals
for i in $(seq 1 $((DURATION/10))); do
    TIMESTAMP=$(date +%s)

    #gateway metrics
    curl -s http://localhost:8080/actuator/prometheus > "$OUTPUT_DIR/gateway-$TIMESTAMP.txt"

    #order service metrics
    curl -s http://localhost:8081/actuator/prometheus > "$OUTPUT_DIR/order-$TIMESTAMP.txt"

    #payment
    curl -s http://localhost:8082/actuator/prometheus > "$OUTPUT_DIR/payment-$TIMESTAMP.txt"

    #inventory
    curl -s http://localhost:8083/actuator/prometheus > "$OUTPUT_DIR/inventory-$TIMESTAMP.txt"

    #notification
    curl -s http://localhost:8084/actuator/prometheus > "$OUTPUT_DIR/notification-$TIMESTAMP.txt"

    echo "collected metrics at $TIMESTAMP"
    sleep 10
done

echo "Metrics collection complete!"
echo "Results saved to: $OUTPUT_DIR"