#!/bin/bash

# Cleanup script - stops all containers and cleans up resources

echo "========================================"
echo "Cleanup"
echo "========================================"
echo ""
echo "This will:"
echo "  - Stop all Docker containers"
echo "  - Remove all containers"
echo "  - Clean up networks"
echo ""

read -p "Continue? (y/N): " -n 1 -r
echo

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Cancelled"
    exit 0
fi

cd infrastructure

echo "Stopping containers..."
docker compose down

echo "Removing orphaned containers..."
docker compose down --remove-orphans

echo ""
echo "Cleanup complete!"
echo ""
echo "To start fresh, run:"
echo "  ./quick-start.sh"
echo "  or"
echo "  ./run-experiment.sh <profile>"
