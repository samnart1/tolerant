#!/bin/bash

# Benchmark runner
# Usage: ./run_benchmark.sh <experiment_name> <duration_minutes> <users> <spawn_rate>

EXPERIMENT_NAME=${1:-"baseline"}
DURATION=${2:-30}
USERS=${3:-100}
SPAWN_RATE=${4:-10}

RESULTS_DIR="./results/${EXPERIMENT_NAME}_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$RESULTS_DIR"

echo "=============================================="
echo "Running experiment: $EXPERIMENT_NAME"
echo "Duration: ${DURATION}m | Users: $USERS | Spawn rate: $SPAWN_RATE/s"
echo "Results: $RESULTS_DIR"
echo "=============================================="

# Save experiment config
cat > "$RESULTS_DIR/config.json" << EOF
{
    "experiment": "$EXPERIMENT_NAME",
    "duration_minutes": $DURATION,
    "users": $USERS,
    "spawn_rate": $SPAWN_RATE,
    "timestamp": "$(date -Iseconds)"
}
EOF

# Run Locust headless with CSV output
docker run --rm \
    --network=online-boutique \
    -v "$(pwd)/loadgenerator:/mnt/locust" \
    -v "$(pwd)/$RESULTS_DIR:/mnt/results" \
    thesis-microservices-loadgenerator:latest \
    -f /mnt/locust/locustfile.py \
    --host=http://frontend:8080 \
    --headless \
    -u "$USERS" \
    -r "$SPAWN_RATE" \
    -t "${DURATION}m" \
    --csv=/mnt/results/locust \
    --csv-full-history \
    --print-stats

echo ""
echo "=============================================="
echo "Experiment complete!"
echo "Results saved to: $RESULTS_DIR"
echo "Files:"
ls -la "$RESULTS_DIR"
echo "=============================================="
