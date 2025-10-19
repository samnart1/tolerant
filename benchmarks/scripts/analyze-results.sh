#!/bin/bash

PROFILE=$1

if [ -z "$PROFILE" ]; then
    echo "Usage: ./analyze-results.sh "
    exit 1
fi

RESULTS_FILE="results/$PROFILE/results.jtl"

if [ ! -f "$RESULTS_FILE" ]; then
    echo "Results file not found: $RESULTS_FILE"
    exit 1
fi

echo "=========================================="
echo "Analyzing results for profile: $PROFILE"
echo "=========================================="

# Extract key metrics using awk
echo ""
echo "Response Time Statistics:"
awk -F',' 'NR>1 {sum+=$2; sumsq+=$2*$2; if($2>max)max=$2; if(min=="")min=$2; if($2<min)min=$2} 
    END {print "Min:", min "ms"; 
         print "Max:", max "ms"; 
         print "Mean:", sum/NR "ms"; 
         print "StdDev:", sqrt(sumsq/NR - (sum/NR)^2) "ms"}' $RESULTS_FILE

echo ""
echo "Success Rate:"
awk -F',' 'NR>1 {total++; if($8=="true" || $8=="200")success++} 
    END {print "Total Requests:", total; 
         print "Successful:", success; 
         print "Failed:", total-success;
         print "Success Rate:", (success/total)*100 "%"}' $RESULTS_FILE

echo ""
echo "Throughput:"
awk -F',' 'NR>1 {if(NR==2)start=$1; end=$1} 
    END {duration=(end-start)/1000; 
         print "Duration:", duration "s"; 
         print "Requests/sec:", NR/duration}' $RESULTS_FILE