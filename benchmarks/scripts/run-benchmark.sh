#!/bin/bash

PROFILE=$1
THREADS=${2:-10}
DURATION=${3:-300}
RAMP_UP=${4:-10}

if [ -z "$PROFILE" ]; then
    echo "Usage: ./run-benchmark.sh  [threads] [duration] [ramp_up]"
    exit 1
fi

OUTPUT_DIR="results/$PROFILE"
mkdir -p $OUTPUT_DIR

echo "=========================================="
echo "Running benchmark for profile: $PROFILE"
echo "Threads: $THREADS"
echo "Duration: $DURATION seconds"
echo "Ramp-up: $RAMP_UP seconds"
echo "=========================================="

# Run JMeter test
jmeter -n -t benchmarks/jmeter/order-load-test.jmx \
    -Jthreads=$THREADS \
    -Jduration=$DURATION \
    -Jrampup=$RAMP_UP \
    -l $OUTPUT_DIR/results.jtl \
    -e -o $OUTPUT_DIR/html-report

echo "Benchmark complete!"
echo "Results: $OUTPUT_DIR/results.jtl"
echo "HTML Report: $OUTPUT_DIR/html-report/index.html"