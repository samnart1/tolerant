#!/bin/bash

# Quick Start Script
# Starts the system with baseline profile for quick testing

echo "========================================"
echo "Quick Start - Baseline Profile"
echo "========================================"
echo ""
echo "This will start all services with the baseline"
echo "profile (no resilience patterns) for quick testing."
echo ""

cd infrastructure/scripts
./start-with-profile.sh baseline

echo ""
echo "Quick test endpoint:"
echo "  curl -X POST http://localhost:8081/api/orders \\"
echo "    -H 'Content-Type: application/json' \\"
echo "    -d '{\"customerId\":\"CUST001\",\"productId\":\"PROD001\",\"quantity\":5,\"amount\":99.99,\"customerEmail\":\"test@example.com\",\"customerPhone\":\"+1234567890\"}'"
