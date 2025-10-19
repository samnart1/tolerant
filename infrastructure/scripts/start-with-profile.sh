#!/bin/bash

PROFILE=$1

if [ -z "$PROFILE" ]; then
    echo "Usage: ./start-with-profile.sh"
    echo "Profiles: baseline, retry-timeout, circuit-breaker, bulkhead, async-queue, production-combined"
    exit 1
fi

echo "--------------------"
echo "starting system with profile: $PROFILE"
echo "--------------------"

# docker compose down

# docker compose build

# PROFILE=$PROFILE docker compose up -d

# echo "waiting for services to start (60s)...."
# sleep 60

#checking health
echo ""
echo "------------------------------------"
echo "Service Health Status:"
echo "-----------------------------"
curl -s http://localhost:8761/actuator/health | jq '.status'
curl -s http://localhost:8080/actuator/health | jq '.status'
curl -s http://localhost:8081/actuator/health | jq '.status'
curl -s http://localhost:8082/actuator/health | jq '.status'
curl -s http://localhost:8083/actuator/health | jq '.status'
curl -s http://localhost:8084/actuator/health | jq '.status'

echo ""
echo "--------------------------------"
echo "system ready for testing"
echo "-------------------------------"
echo "eureka: http://localhost:8761"
echo "API Gateway underway: http://localhost:8080"
echo "Prometheus: http://localhost:9090"
echo "grafana: http://localhost:3000 (admin/admin)"
echo "--------------------------------"