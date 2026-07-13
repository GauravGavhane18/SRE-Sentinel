#!/usr/bin/env bash
# ==============================================================================
# Sentinel Stop Script
# ==============================================================================

echo "Stopping Sentinel stack..."
pkill -f sentinel.jar || true
rm -f /tmp/sentinel-daemon.pid

pkill -f backup-daemon || true

docker-compose stop

echo "Sentinel stack stopped."
