#!/bin/bash

# Run a complete experiment with a specific profile
# Usage: ./run-experiment.sh <profile> [threads] [duration] [chaos_enabled] [failure_rate] [delay_ms]

PROFILE=$1
THREADS=${2:-10}
DURATION=${3:-300}
CHAOS_ENABLED=${4:-false}
CHAOS_FAILURE_RATE=${5:-0.0}
CHAOS_DELAY_MS=${6:-0}

if [ -z "$PROFILE" ]; then
    echo "Usage: ./run-experiment.sh <profile> [threads] [duration] [chaos_enabled] [failure_rate] [delay_ms]"
    echo ""
    echo "Profiles:"
    echo "  baseline            - No resilience patterns"
    echo "  retry-timeout       - Retry + Timeout"
    echo "  circuit-breaker     - Circuit Breaker"
    echo "  bulkhead            - Bulkhead isolation"
    echo "  async-queue         - Async messaging"
    echo "  production-combined - All patterns combined"
    echo ""
    echo "Example:"
    echo "  ./run-experiment.sh baseline 20 600"
    echo "  ./run-experiment.sh circuit-breaker 10 300 true 0.3 500"
    exit 1
fi

echo "========================================"
echo "Running Experiment"
echo "========================================"
echo "Profile: $PROFILE"
echo "Threads: $THREADS"
echo "Duration: $DURATION seconds"
echo "Chaos enabled: $CHAOS_ENABLED"
if [ "$CHAOS_ENABLED" == "true" ]; then
    echo "Failure rate: $CHAOS_FAILURE_RATE"
    echo "Delay: $CHAOS_DELAY_MS ms"
fi
echo "========================================"
echo ""

# Start system
echo "Step 1: Starting system..."
CHAOS_ENABLED=$CHAOS_ENABLED CHAOS_FAILURE_RATE=$CHAOS_FAILURE_RATE CHAOS_DELAY_MS=$CHAOS_DELAY_MS \
    ./infrastructure/scripts/start-with-profile.sh $PROFILE

if [ $? -ne 0 ]; then
    echo "Failed to start system"
    exit 1
fi

# Warm up
echo ""
echo "Step 2: Warming up (30 seconds)..."
sleep 30

# Run benchmark in background
echo ""
echo "Step 3: Starting load test..."
./benchmarks/scripts/run-benchmark.sh $PROFILE $THREADS $DURATION 10 &
BENCH_PID=$!

# Collect metrics in background
echo ""
echo "Step 4: Collecting metrics..."
./infrastructure/scripts/collect-metrics.sh $PROFILE $DURATION &
METRICS_PID=$!

# Wait for completion
echo ""
echo "Experiment running... (this will take $DURATION seconds)"
wait $BENCH_PID
wait $METRICS_PID

echo ""
echo "========================================"
echo "Experiment Complete!"
echo "========================================"
echo "Results:"
echo "  JMeter results: benchmarks/results/$PROFILE/"
echo "  Metrics:        infrastructure/results/$PROFILE/"
echo ""
echo "To stop the system:"
echo "  cd infrastructure && docker compose down"
echo "========================================"
