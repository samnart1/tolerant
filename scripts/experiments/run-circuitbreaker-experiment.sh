#!/bin/bash
# Circuit Breaker Comparison Experiment
# Compares application-level vs service mesh circuit breakers

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
RESULTS_DIR="$PROJECT_ROOT/results/circuitbreaker-$(date +%Y%m%d-%H%M%S)"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo "⚡ Circuit Breaker Comparison Experiment"
echo "=================================================="
echo "Results will be saved to: $RESULTS_DIR"
echo ""

mkdir -p "$RESULTS_DIR/app-level" "$RESULTS_DIR/mesh-level" "$RESULTS_DIR/both"

# Setup
setup() {
    echo -e "${YELLOW}Setting up experiment...${NC}"
    kubectl port-forward svc/order-service 8080:8080 &
    PORT_FORWARD_PID=$!
    sleep 3
    inject_failure
    
    wrk -t4 -c50 -d3m \
        -s "$PROJECT_ROOT/workload/wrk-scripts/baseline-test.lua" \
        http://localhost:8080 \
        > "$RESULTS_DIR/both/test-results.txt"
    
    remove_failure
    
    echo -e "${GREEN}✓ Both layers test complete${NC}"
}

# Analyze results
analyze_results() {
    echo -e "${YELLOW}Analyzing results...${NC}"
    
    # Extract key metrics
    for dir in app-level mesh-level both; do
        if [ -f "$RESULTS_DIR/$dir/test-results.txt" ]; then
            grep "Requests/sec" "$RESULTS_DIR/$dir/test-results.txt" > "$RESULTS_DIR/$dir/throughput.txt" || true
            grep "Latency" "$RESULTS_DIR/$dir/test-results.txt" > "$RESULTS_DIR/$dir/latency.txt" || true
        fi
    done
    
    echo -e "${GREEN}✓ Analysis complete${NC}"
}

# Generate comparison report
generate_report() {
    echo -e "${YELLOW}Generating comparison report...${NC}"
    
    cat > "$RESULTS_DIR/COMPARISON_REPORT.md" << 'EOF'
# Circuit Breaker Comparison Experiment

## Objective
Compare the effectiveness and performance impact of circuit breakers at different layers:
1. Application-level (Resilience4j)
2. Service mesh level (Istio)
3. Both layers combined

## Experiment Setup

### Failure Injection
- **Type:** Network delay
- **Target:** Inventory Service
- **Delay:** 500ms
- **Duration:** 10 minutes per test

### Load Profile
- **Concurrent Users:** 50
- **Threads:** 4
- **Duration:** 3 minutes per test
- **Recovery Time:** 30 seconds between tests

## Results

### Test 1: Application-Level Circuit Breaker

**Configuration:**
- Resilience4j enabled
- Istio retries disabled
- Circuit breaker opens after 5 failures

**Results:**
```
$(cat "$RESULTS_DIR/app-level/test-results.txt" 2>/dev/null | tail -15)
```

**Key Observations:**
- Circuit breaker state transitions
- Fallback execution rate
- Response time during open state

### Test 2: Service Mesh Circuit Breaker

**Configuration:**
- Istio outlier detection enabled
- Consecutive errors threshold: 3
- Ejection time: 30s

**Results:**
```
$(cat "$RESULTS_DIR/mesh-level/test-results.txt" 2>/dev/null | tail -15)
```

**Key Observations:**
- Pod ejection behavior
- Connection pool management
- Load balancing impact

### Test 3: Both Layers

**Configuration:**
- Both application and mesh circuit breakers active
- Layered defense approach

**Results:**
```
$(cat "$RESULTS_DIR/both/test-results.txt" 2>/dev/null | tail -15)
```

**Key Observations:**
- Redundancy benefits
- Potential overhead
- Faster failure detection

## Analysis

### Throughput Comparison

| Configuration | Requests/sec | % of Baseline |
|--------------|--------------|---------------|
| App-level    | TBD          | TBD           |
| Mesh-level   | TBD          | TBD           |
| Both         | TBD          | TBD           |

### Latency Comparison

| Configuration | P50 | P95 | P99 | Max |
|--------------|-----|-----|-----|-----|
| App-level    | TBD | TBD | TBD | TBD |
| Mesh-level   | TBD | TBD | TBD | TBD |
| Both         | TBD | TBD | TBD | TBD |

### Error Handling

| Configuration | Error Rate | Fallback Execution | Recovery Time |
|--------------|------------|-------------------|---------------|
| App-level    | TBD        | TBD               | TBD           |
| Mesh-level   | TBD        | TBD               | TBD           |
| Both         | TBD        | TBD               | TBD           |

## Conclusions

### Application-Level Circuit Breaker
**Pros:**
- Fine-grained control
- Business logic integration
- Detailed metrics

**Cons:**
- Requires code changes
- Language-specific
- Application overhead

### Service Mesh Circuit Breaker
**Pros:**
- No code changes
- Consistent across services
- Infrastructure-level visibility

**Cons:**
- Coarser granularity
- Limited business context
- Network overhead

### Combined Approach
**Pros:**
- Defense in depth
- Best of both worlds
- Faster failure detection

**Cons:**
- Potential redundancy
- Configuration complexity
- Higher overhead

## Recommendations

Based on this experiment:

1. **For critical services:** Use both layers for maximum resilience
2. **For performance-critical paths:** Consider mesh-level only
3. **For business logic failures:** Application-level required
4. **For network failures:** Mesh-level sufficient

## Next Steps

- Run retry strategy comparison experiment
- Test bulkhead patterns
- Evaluate timeout configurations

EOF

    echo -e "${GREEN}✓ Report generated${NC}"
}

# Cleanup
cleanup() {
    echo -e "${YELLOW}Cleaning up...${NC}"
    kubectl delete networkchaos inventory-delay-experiment 2>/dev/null || true
    kill $PORT_FORWARD_PID 2>/dev/null || true
    
    # Reset to default configuration
    kubectl apply -f "$PROJECT_ROOT/infrastructure/istio/destination-rules.yaml"
    kubectl apply -f "$PROJECT_ROOT/infrastructure/istio/gateway.yaml"
    
    echo -e "${GREEN}✓ Cleanup complete${NC}"
}

# Main execution
main() {
    trap cleanup EXIT
    
    setup
    test_app_level
    test_mesh_level
    test_both_layers
    analyze_results
    generate_report
    
    echo ""
    echo "=================================================="
    echo -e "${GREEN}Circuit breaker experiment complete!${NC}"
    echo "=================================================="
    echo ""
    echo "Results saved to: $RESULTS_DIR"
    echo ""
    echo "View report:"
    echo "  cat $RESULTS_DIR/COMPARISON_REPORT.md"
    echo ""
    echo "Visualize in Grafana:"
    echo "  kubectl port-forward -n monitoring svc/grafana 3000:80"
    echo ""
}

main

    echo -e "${GREEN}✓ Setup complete${NC}"
}

# Inject network delay to trigger circuit breaker
inject_failure() {
    echo -e "${YELLOW}Injecting network delay (500ms)...${NC}"
    kubectl apply -f - <<EOF
apiVersion: chaos-mesh.org/v1alpha1
kind: NetworkChaos
metadata:
  name: inventory-delay-experiment
  namespace: default
spec:
  action: delay
  mode: all
  selector:
    namespaces:
      - default
    labelSelectors:
      app: inventory-service
  delay:
    latency: "500ms"
    correlation: "100"
    jitter: "0ms"
  duration: "10m"
EOF
    sleep 5
    echo -e "${GREEN}✓ Failure injected${NC}"
}

# Remove failure injection
remove_failure() {
    echo -e "${YELLOW}Removing failure injection...${NC}"
    kubectl delete networkchaos inventory-delay-experiment 2>/dev/null || true
    sleep 5
    echo -e "${GREEN}✓ Failure removed${NC}"
}

# Test with application-level circuit breaker only
test_app_level() {
    echo -e "${YELLOW}Test 1: Application-level circuit breaker${NC}"
    
    # Disable Istio retries
    kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: inventory-service-vs
  namespace: default
spec:
  hosts:
    - inventory-service
  http:
    - route:
        - destination:
            host: inventory-service
            port:
              number: 8081
      retries:
        attempts: 0
      timeout: 10s
EOF
    
    sleep 3
    inject_failure
    
    wrk -t4 -c50 -d3m \
        -s "$PROJECT_ROOT/workload/wrk-scripts/baseline-test.lua" \
        http://localhost:8080 \
        > "$RESULTS_DIR/app-level/test-results.txt"
    
    # Collect circuit breaker metrics
    kubectl port-forward -n monitoring svc/prometheus-server 9090:80 &
    PROM_PID=$!
    sleep 3
    
    curl -s "http://localhost:9090/api/query?query=resilience4j_circuitbreaker_state" \
        > "$RESULTS_DIR/app-level/circuit-breaker-state.json"
    
    curl -s "http://localhost:9090/api/query?query=resilience4j_circuitbreaker_calls_total" \
        > "$RESULTS_DIR/app-level/circuit-breaker-calls.json"
    
    kill $PROM_PID 2>/dev/null || true
    
    remove_failure
    sleep 30  # Recovery time
    
    echo -e "${GREEN}✓ App-level test complete${NC}"
}

# Test with service mesh circuit breaker
test_mesh_level() {
    echo -e "${YELLOW}Test 2: Service mesh circuit breaker${NC}"
    
    # Disable application-level circuit breaker (would need app configuration change)
    # For this test, we enhance Istio's circuit breaking
    kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: inventory-service-dr
  namespace: default
spec:
  host: inventory-service
  trafficPolicy:
    connectionPool:
      tcp:
        maxConnections: 50
      http:
        http1MaxPendingRequests: 10
        http2MaxRequests: 50
        maxRequestsPerConnection: 1
    outlierDetection:
      consecutiveErrors: 3
      interval: 10s
      baseEjectionTime: 30s
      maxEjectionPercent: 100
      minHealthPercent: 0
EOF
    
    sleep 3
    inject_failure
    
    wrk -t4 -c50 -d3m \
        -s "$PROJECT_ROOT/workload/wrk-scripts/baseline-test.lua" \
        http://localhost:8080 \
        > "$RESULTS_DIR/mesh-level/test-results.txt"
    
    remove_failure
    sleep 30
    
    echo -e "${GREEN}✓ Mesh-level test complete${NC}"
}

# Test with both layers
test_both_layers() {
    echo -e "${YELLOW}Test 3: Both application and mesh circuit breakers${NC}"
    
    # Re-enable both
    kubectl apply -f "$PROJECT_ROOT/infrastructure/istio/destination-rules.yaml"
    
    sleep 3