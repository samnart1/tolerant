#!/bin/bash
# Run baseline experiment without fault injection

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
RESULTS_DIR="$PROJECT_ROOT/results/baseline-$(date +%Y%m%d-%H%M%S)"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo "📊 Running Baseline Experiment"
echo "=================================================="
echo "Results will be saved to: $RESULTS_DIR"
echo ""

mkdir -p "$RESULTS_DIR"

# Port forward Order Service
setup_port_forward() {
    echo -e "${YELLOW}Setting up port forwarding...${NC}"
    kubectl port-forward svc/order-service 8080:8080 &
    PORT_FORWARD_PID=$!
    sleep 3
    echo -e "${GREEN}✓ Port forwarding established${NC}"
}

# Run warmup
run_warmup() {
    echo -e "${YELLOW}Running warmup (30 seconds)...${NC}"
    wrk -t2 -c10 -d30s \
        -s "$PROJECT_ROOT/workload/wrk-scripts/baseline-test.lua" \
        http://localhost:8080 \
        > /dev/null 2>&1
    echo -e "${GREEN}✓ Warmup complete${NC}"
}

# Run steady state test
run_steady_state() {
    echo -e "${YELLOW}Running steady state test (5 minutes, 50 concurrent users)...${NC}"
    wrk -t4 -c50 -d5m \
        -s "$PROJECT_ROOT/workload/wrk-scripts/baseline-test.lua" \
        http://localhost:8080 \
        > "$RESULTS_DIR/steady-state.txt"
    echo -e "${GREEN}✓ Steady state test complete${NC}"
}

# Run load test
run_load_test() {
    echo -e "${YELLOW}Running load test (3 minutes, 100 concurrent users)...${NC}"
    wrk -t8 -c100 -d3m \
        -s "$PROJECT_ROOT/workload/wrk-scripts/baseline-test.lua" \
        http://localhost:8080 \
        > "$RESULTS_DIR/load-test.txt"
    echo -e "${GREEN}✓ Load test complete${NC}"
}

# Run stress test
run_stress_test() {
    echo -e "${YELLOW}Running stress test (2 minutes, 200 concurrent users)...${NC}"
    wrk -t12 -c200 -d2m \
        -s "$PROJECT_ROOT/workload/wrk-scripts/baseline-test.lua" \
        http://localhost:8080 \
        > "$RESULTS_DIR/stress-test.txt"
    echo -e "${GREEN}✓ Stress test complete${NC}"
}

# Run spike test with k6
run_spike_test() {
    echo -e "${YELLOW}Running spike test with k6...${NC}"
    k6 run --out json="$RESULTS_DIR/spike-test.json" \
        -e BASE_URL=http://localhost:8080 \
        "$PROJECT_ROOT/workload/k6-scripts/spike-test.js" \
        > "$RESULTS_DIR/spike-test.txt"
    echo -e "${GREEN}✓ Spike test complete${NC}"
}

# Collect metrics from Prometheus
collect_metrics() {
    echo -e "${YELLOW}Collecting metrics from Prometheus...${NC}"
    
    # Port forward Prometheus
    kubectl port-forward -n monitoring svc/prometheus-server 9090:80 &
    PROM_PID=$!
    sleep 3
    
    # Query metrics
    END_TIME=$(date +%s)
    START_TIME=$((END_TIME - 1800))  # Last 30 minutes
    
    # Request rate
    curl -s "http://localhost:9090/api/query_range?query=rate(http_server_requests_seconds_count\[1m\])&start=${START_TIME}&end=${END_TIME}&step=15s" \
        > "$RESULTS_DIR/metrics-request-rate.json"
    
    # Error rate
    curl -s "http://localhost:9090/api/query_range?query=rate(http_server_requests_seconds_count{status=~\"5..\"}[1m])&start=${START_TIME}&end=${END_TIME}&step=15s" \
        > "$RESULTS_DIR/metrics-error-rate.json"
    
    # Latency
    curl -s "http://localhost:9090/api/query_range?query=histogram_quantile(0.95,rate(http_server_requests_seconds_bucket\[1m\]))&start=${START_TIME}&end=${END_TIME}&step=15s" \
        > "$RESULTS_DIR/metrics-p95-latency.json"
    
    # Circuit breaker state
    curl -s "http://localhost:9090/api/query?query=resilience4j_circuitbreaker_state" \
        > "$RESULTS_DIR/metrics-circuit-breaker.json"
    
    kill $PROM_PID 2>/dev/null || true
    
    echo -e "${GREEN}✓ Metrics collected${NC}"
}

# Generate summary report
generate_report() {
    echo -e "${YELLOW}Generating summary report...${NC}"
    
    cat > "$RESULTS_DIR/REPORT.md" << EOF
# Baseline Experiment Report

**Date:** $(date)
**Duration:** ~15 minutes
**Configuration:** No fault injection

## Test Scenarios

### 1. Steady State Test
- Duration: 5 minutes
- Concurrent Users: 50
- Threads: 4

### 2. Load Test
- Duration: 3 minutes
- Concurrent Users: 100
- Threads: 8

### 3. Stress Test
- Duration: 2 minutes
- Concurrent Users: 200
- Threads: 12

### 4. Spike Test (k6)
- Variable load profile
- Spike to 300 users

## Results

### Steady State
\`\`\`
$(tail -20 "$RESULTS_DIR/steady-state.txt")
\`\`\`

### Load Test
\`\`\`
$(tail -20 "$RESULTS_DIR/load-test.txt")
\`\`\`

### Stress Test
\`\`\`
$(tail -20 "$RESULTS_DIR/stress-test.txt")
\`\`\`

## Files Generated

- \`steady-state.txt\` - Steady state test results
- \`load-test.txt\` - Load test results
- \`stress-test.txt\` - Stress test results
- \`spike-test.txt\` - Spike test results  
- \`spike-test.json\` - Spike test JSON data
- \`metrics-*.json\` - Prometheus metrics

## Analysis

Use these baseline results to compare against fault-injection experiments.

Key metrics to analyze:
1. Request throughput (req/s)
2. Error rate (%)
3. P50, P95, P99 latencies
4. Resource utilization

EOF

    echo -e "${GREEN}✓ Report generated${NC}"
}

# Cleanup
cleanup() {
    echo -e "${YELLOW}Cleaning up...${NC}"
    kill $PORT_FORWARD_PID 2>/dev/null || true
    echo -e "${GREEN}✓ Cleanup complete${NC}"
}

# Main execution
main() {
    trap cleanup EXIT
    
    setup_port_forward
    run_warmup
    run_steady_state
    run_load_test
    run_stress_test
    run_spike_test
    collect_metrics
    generate_report
    
    echo ""
    echo "=================================================="
    echo -e "${GREEN}Baseline experiment complete!${NC}"
    echo "=================================================="
    echo ""
    echo "Results saved to: $RESULTS_DIR"
    echo ""
    echo "View report:"
    echo "  cat $RESULTS_DIR/REPORT.md"
    echo ""
    echo "Next experiments:"
    echo "  ./scripts/experiments/run-circuitbreaker-experiment.sh"
    echo "  ./scripts/experiments/run-retry-experiment.sh"
    echo ""
}

main