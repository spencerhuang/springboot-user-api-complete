@echo off
REM Start Prometheus script for Spring Boot User Management API
REM This script helps you start Prometheus with the correct configuration

echo 🚀 Starting Prometheus for Spring Boot User Management API...

REM Check if prometheus.yml exists
if not exist "prometheus.yml" (
    echo ❌ Error: prometheus.yml not found in current directory
    echo Please make sure you're running this script from the project root directory
    pause
    exit /b 1
)

REM Check if Prometheus is installed
set PROMETHEUS_CMD=
if exist "prometheus.exe" (
    set PROMETHEUS_CMD=prometheus.exe
) else if exist "prometheus" (
    set PROMETHEUS_CMD=prometheus
) else (
    echo ❌ Error: Prometheus not found
    echo.
    echo Please install Prometheus first:
    echo 1. Download from: https://prometheus.io/download/
    echo 2. Extract to this directory
    echo 3. Run this script again
    echo.
    pause
    exit /b 1
)

echo ✅ Prometheus executable found: %PROMETHEUS_CMD%
echo ✅ Configuration file found: prometheus.yml
echo.

REM Start Prometheus
echo Starting Prometheus...
echo Configuration: prometheus.yml
echo Web UI: http://localhost:9090
echo Target: http://localhost:8080/actuator/prometheus
echo.
echo Press Ctrl+C to stop Prometheus
echo.

REM Start Prometheus with configuration
%PROMETHEUS_CMD% --config.file=prometheus.yml --web.enable-lifecycle

echo.
echo Prometheus stopped.
pause
