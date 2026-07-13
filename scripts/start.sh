#!/usr/bin/env bash
# ==============================================================================
# Sentinel Startup Runner
# ==============================================================================

# Start services
docker-compose up -d

# Start the mock backup daemon process
/bin/bash scripts/start-services.sh backup-daemon

# Launch Sentinel daemon in background with wrapper
nohup /bin/bash scripts/monitor.sh --check-daemon > /dev/null 2>&1 &

echo "===================================================================="
echo " Sentinel stack running. Launching console monitor panel..."
echo "===================================================================="
sleep 2
# Run interactive console dashboard
java -jar sentinel.jar
