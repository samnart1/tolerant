#!/bin/bash

PROFILES=("baseline" "retry-timeout" "circuit-breaker" "bulkhead" "async-queue" "production-combined")

echo "=========================================="
echo "Running benchmarks for all profiles"
echo "=========================================="

for PROFILE in "${PROFILES[@]}"; do
    echo ""
    echo "=========================================="
    echo "Testing Profile: $PROFILE"
    echo "=========================================="
    
    # Start system with profile
    ./infrastructure/scripts/start-with-profile.sh $PROFILE
    
    # Wait for warm-up
    echo "Warming up (30 seconds)..."
    sleep 30
    
    # Run benchmark
    ./benchmarks/scripts/run-benchmark.sh $PROFILE 10 300 10
    
    # Collect metrics
    ./infrastructure/scripts/collect-metrics.sh $PROFILE 300 &
    
    # Wait for completion
    wait
    
    # Stop system
    docker compose down
    
    echo "Profile $PROFILE complete!"
    sleep 10
done

echo ""
echo "=========================================="
echo "All benchmarks complete!"
echo "=========================================="
echo "Results saved in: results/"