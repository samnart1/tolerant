#!/bin/bash

# Run benchmarks for all profiles
# This script automates testing all resilience patterns

PROFILES=("baseline" "retry-timeout" "circuit-breaker" "bulkhead" "async-queue" "production-combined")
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

echo "=========================================="
echo "Running benchmarks for all profiles"
echo "=========================================="
echo "Project root: $PROJECT_ROOT"
echo ""

cd "$PROJECT_ROOT"

for PROFILE in "${PROFILES[@]}"; do
    echo ""
    echo "=========================================="
    echo "Testing Profile: $PROFILE"
    echo "=========================================="

    # Start system with profile
    ./infrastructure/scripts/start-with-profile.sh $PROFILE

    if [ $? -ne 0 ]; then
        echo "Failed to start system with profile: $PROFILE"
        continue
    fi

    # Wait for warm-up
    echo "Warming up (30 seconds)..."
    sleep 30

    # Run benchmark
    ./benchmarks/scripts/run-benchmark.sh $PROFILE 10 300 10 &
    BENCH_PID=$!

    # Collect metrics in parallel
    ./infrastructure/scripts/collect-metrics.sh $PROFILE 300 &
    METRICS_PID=$!

    # Wait for both to complete
    wait $BENCH_PID
    wait $METRICS_PID

    # Stop system
    cd infrastructure
    docker compose down
    cd ..

    echo "Profile $PROFILE complete!"
    echo "Cooling down (10 seconds)..."
    sleep 10
done

echo ""
echo "=========================================="
echo "All benchmarks complete!"
echo "=========================================="
echo "Results saved in: results/"
echo ""
echo "To analyze results, run:"
echo "  cd benchmarks/scripts"
echo "  ./analyze-results.sh"
echo "========================================"