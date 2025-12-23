#!/bin/bash

# Master experiment runner for thesis benchmarks
# Usage: ./run_experiments.sh [duration_minutes] [users] [spawn_rate]

DURATION=${1:-30}
USERS=${2:-100}
SPAWN_RATE=${3:-10}

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
RESULTS_BASE="./results/experiment_${TIMESTAMP}"

echo "=============================================="
echo "THESIS EXPERIMENT SUITE"
echo "=============================================="
echo "Duration per experiment: ${DURATION} minutes"
echo "Users: ${USERS}"
echo "Spawn rate: ${SPAWN_RATE}/sec"
echo "Results directory: ${RESULTS_BASE}"
echo "=============================================="

mkdir -p "${RESULTS_BASE}"

run_experiment() {
    local name=$1
    local env_file=$2
    
    echo ""
    echo "=============================================="
    echo "Starting experiment: ${name}"
    echo "Environment: ${env_file}"
    echo "=============================================="
    
    local result_dir="${RESULTS_BASE}/${name}"
    mkdir -p "${result_dir}"
    
    # Stop any running containers
    docker compose down 2>/dev/null
    
    # Start services with specific environment
    echo "Starting services..."
    docker compose --env-file "${env_file}" up -d
    
    # Wait for services to be ready
    echo "Waiting for services to be ready..."
    sleep 15
    
    # Check frontend health
    if ! curl -s http://localhost:8080/health > /dev/null; then
        echo "ERROR: Frontend not healthy!"
        docker compose logs frontend
        return 1
    fi
    
    # Save config
    cp "${env_file}" "${result_dir}/config.env"
    curl -s http://localhost:8080/health > "${result_dir}/frontend_health.json"
    
    echo "Running Locust for ${DURATION} minutes..."
    
    # Run Locust headless
    docker run --rm \
        --network=online-boutique \
        -v "$(pwd)/loadgenerator:/mnt/locust" \
        -v "$(pwd)/${result_dir}:/mnt/results" \
        thesis-microservices-loadgenerator:latest \
        -f /mnt/locust/locustfile.py \
        --host=http://frontend:8080 \
        --headless \
        -u "${USERS}" \
        -r "${SPAWN_RATE}" \
        -t "${DURATION}m" \
        --csv=/mnt/results/locust \
        --csv-full-history \
        --print-stats \
        2>&1 | tee "${result_dir}/locust_output.log"
    
    # Save circuit breaker metrics (if available)
    curl -s http://localhost:8080/metrics > "${result_dir}/cb_metrics.json" 2>/dev/null || true
    
    # Save docker logs
    docker compose logs > "${result_dir}/docker_logs.txt" 2>&1
    
    echo "Experiment ${name} complete!"
    echo "Results saved to: ${result_dir}"
    
    # Cooldown
    docker compose down
    sleep 5
}

# Run all experiments
echo ""
echo "Starting experiment suite..."
echo ""

# 1. Baseline (no CB, no failures)
run_experiment "1_baseline" "./env/baseline.env"

# 2. Baseline + Failures (no CB, with failures) - shows the problem
run_experiment "2_baseline_failures" "./env/baseline_failures.env"

# 3. Circuit Breaker (with CB, no failures) - measures overhead
run_experiment "3_circuit_breaker" "./env/circuit_breaker.env"

# 4. Circuit Breaker + Failures (with CB, with failures) - shows solution
run_experiment "4_circuit_breaker_failures" "./env/circuit_breaker_failures.env"

echo ""
echo "=============================================="
echo "ALL EXPERIMENTS COMPLETE!"
echo "=============================================="
echo "Results directory: ${RESULTS_BASE}"
echo ""
echo "Files per experiment:"
echo "  - locust_stats.csv        : Summary statistics"
echo "  - locust_stats_history.csv: Time-series data"
echo "  - locust_failures.csv     : Failure details"
echo "  - cb_metrics.json         : Circuit breaker state"
echo "  - config.env              : Experiment configuration"
echo ""
echo "Next step: Run analysis notebook"
echo "  jupyter notebook analysis/benchmark_analysis.ipynb"
echo "=============================================="
