#!/bin/bash

# Start the system with baseline profile (no resilience patterns)
# This is used for baseline performance measurements

echo "Starting system with BASELINE profile..."
echo "This profile has NO resilience patterns enabled."
echo ""

./start-with-profile.sh baseline