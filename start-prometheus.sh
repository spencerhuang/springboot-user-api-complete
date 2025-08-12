#!/bin/bash

# Start Prometheus script for Spring Boot User Management API
# This script helps you start Prometheus with the correct configuration

echo "🚀 Starting Prometheus for Spring Boot User Management API..."

# Check if prometheus.yml exists
if [ ! -f "prometheus.yml" ]; then
    echo "❌ Error: prometheus.yml not found in current directory"
    echo "Please make sure you're running this script from the project root directory"
    exit 1
fi

# Check if Prometheus is installed
PROMETHEUS_CMD=""
if command -v prometheus &> /dev/null; then
    PROMETHEUS_CMD="prometheus"
elif [ -f "./prometheus" ]; then
    PROMETHEUS_CMD="./prometheus"
elif [ -f "./prometheus.exe" ]; then
    PROMETHEUS_CMD="./prometheus.exe"
else
    echo "❌ Error: Prometheus not found"
    echo ""
    echo "Please install Prometheus first:"
    echo "1. Download from: https://prometheus.io/download/"
    echo "2. Extract to this directory"
    echo "3. Run this script again"
    echo ""
    echo "Or install via package manager:"
    echo "  macOS: brew install prometheus"
    echo "  Ubuntu/Debian: sudo apt-get install prometheus"
    echo "  CentOS/RHEL: sudo yum install prometheus"
    exit 1
fi

# Check if port 9090 is available
if lsof -Pi :9090 -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo "⚠️  Warning: Port 9090 is already in use"
    echo "This might mean Prometheus is already running"
    echo "You can access it at: http://localhost:9090"
    echo ""
    read -p "Do you want to continue anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Aborted."
        exit 1
    fi
fi

echo "✅ Prometheus executable found: $PROMETHEUS_CMD"
echo "✅ Configuration file found: prometheus.yml"
echo ""

# Start Prometheus
echo "Starting Prometheus..."
echo "Configuration: prometheus.yml"
echo "Web UI: http://localhost:9090"
echo "Target: http://localhost:8080/actuator/prometheus"
echo ""
echo "Press Ctrl+C to stop Prometheus"
echo ""

# Start Prometheus with configuration
$PROMETHEUS_CMD --config.file=prometheus.yml --web.enable-lifecycle

echo ""
echo "Prometheus stopped."
