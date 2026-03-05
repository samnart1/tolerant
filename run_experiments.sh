#!/bin/bash

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


PROMETHEUS_URL="http://localhost:9090"

dump_energy_snapshot() {
    local output_file=$1
    local timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

    echo "  Dumping energy snapshot..."

    local host_power
    host_power=$(curl -sf "${PROMETHEUS_URL}/api/v1/query" \
        --data-urlencode 'query=scaph_host_power_microwatts / 1000000' \
        2>/dev/null)

    local process_power
    process_power=$(curl -sf "${PROMETHEUS_URL}/api/v1/query" \
        --data-urlencode 'query=sort_desc(scaph_process_power_consumption_microwatts / 1000000)' \
        2>/dev/null)

    local rapl_energy
    rapl_energy=$(curl -sf "${PROMETHEUS_URL}/api/v1/query" \
        --data-urlencode 'query=scaph_host_energy_microjoules' \
        2>/dev/null)

    cat > "${output_file}" << SNAPSHOT
{
  "timestamp": "${timestamp}",
  "host_power_watts": ${host_power:-null},
  "process_power_watts": ${process_power:-null},
  "host_energy_microjoules": ${rapl_energy:-null}
}
SNAPSHOT

    echo "  Energy snapshot saved to: ${output_file}"
}

dump_energy_timeseries() {
    local start_ts=$1
    local end_ts=$2
    local output_file=$3

    echo "  Dumping energy time-series..."

    local host_power_range
    host_power_range=$(curl -sf "${PROMETHEUS_URL}/api/v1/query_range" \
        --data-urlencode 'query=scaph_host_power_microwatts / 1000000' \
        --data-urlencode "start=${start_ts}" \
        --data-urlencode "end=${end_ts}" \
        --data-urlencode "step=5" \
        2>/dev/null)

    local proc_power_range
    proc_power_range=$(curl -sf "${PROMETHEUS_URL}/api/v1/query_range" \
        --data-urlencode 'query=topk(5, scaph_process_power_consumption_microwatts / 1000000)' \
        --data-urlencode "start=${start_ts}" \
        --data-urlencode "end=${end_ts}" \
        --data-urlencode "step=5" \
        2>/dev/null)

    cat > "${output_file}" << TIMESERIES
{
  "start_unix": ${start_ts},
  "end_unix": ${end_ts},
  "step_seconds": 5,
  "host_power_watts": ${host_power_range:-null},
  "process_power_watts_top5": ${proc_power_range:-null}
}
TIMESERIES

    echo "  Time-series saved to: ${output_file}"
}


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

    docker compose down --remove-orphans 2>/dev/null

    echo "Starting services..."
    docker compose --env-file "${env_file}" up -d

    echo "Waiting for services to be ready..."
    sleep 15

    if ! curl -s http://localhost:8080/health > /dev/null; then
        echo "ERROR: Frontend not healthy!"
        docker compose logs frontend
        return 1
    fi

    echo "Waiting for Scaphandre metrics..."
    sleep 10

    cp "${env_file}" "${result_dir}/config.env"
    curl -s http://localhost:8080/health > "${result_dir}/frontend_health.json"

    local start_ts
    start_ts=$(date +%s)
    dump_energy_snapshot "${result_dir}/energy_start.json"

    echo "Running Locust for ${DURATION} minutes..."

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

    local end_ts
    end_ts=$(date +%s)
    dump_energy_snapshot "${result_dir}/energy_end.json"

    dump_energy_timeseries "${start_ts}" "${end_ts}" "${result_dir}/energy_timeseries.json"

    curl -s http://localhost:8080/metrics > "${result_dir}/cb_metrics.json" 2>/dev/null || true

    docker compose logs > "${result_dir}/docker_logs.txt" 2>&1

    echo "Experiment ${name} complete!"
    echo "Results saved to: ${result_dir}"

    docker compose down
    sleep 5
}


echo ""
echo "Starting experiment suite..."
echo ""

run_experiment "1_baseline" "./env/baseline.env"

run_experiment "2_baseline_failures" "./env/baseline_failures.env"

run_experiment "3_circuit_breaker" "./env/circuit_breaker.env"

run_experiment "4_circuit_breaker_failures" "./env/circuit_breaker_failures.env"

echo ""
echo "=============================================="
echo "ALL EXPERIMENTS COMPLETE!"
echo "=============================================="
echo "Results directory: ${RESULTS_BASE}"
echo ""
echo "Files per experiment:"
echo "  - locust_stats.csv         : Summary statistics"
echo "  - locust_stats_history.csv : Time-series throughput/latency"
echo "  - locust_failures.csv      : Failure details"
echo "  - cb_metrics.json          : Circuit breaker state"
echo "  - energy_start.json        : Power snapshot at experiment start"
echo "  - energy_end.json          : Power snapshot at experiment end"
echo "  - energy_timeseries.json   : Full power time-series (5s resolution)"
echo "  - config.env               : Experiment configuration"
echo ""
echo "Next step: Run analysis notebook"
echo "  jupyter notebook analysis/benchmark_analysis.ipynb"
echo "=============================================="
