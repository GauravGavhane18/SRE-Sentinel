#!/usr/bin/env bash
# ==============================================================================
# Sentinel System Clean & Reset Script
# ==============================================================================

GREEN='\033[0;32m'
NC='\033[0m'

echo "[*] Stopping docker containers..."
docker-compose down 2>/dev/null || true

echo "[*] Terminating running Sentinel daemon processes..."
pkill -f sentinel.jar 2>/dev/null || true
rm -f /tmp/sentinel-daemon.pid

echo "[*] Stopping chaos CPU stress workers..."
if [ -f /tmp/chaos-cpu.pids ]; then
    while read -r pid; do
        kill "$pid" 2>/dev/null || true
    done < /tmp/chaos-cpu.pids
    rm -f /tmp/chaos-cpu.pids
fi

echo "[*] Cleaning temporary chaos disk dumps..."
rm -f /tmp/sentinel-chaos.dump

echo "[*] Cleaning logs..."
rm -rf logs/*

echo "[*] Removing Cron watchdog job..."
crontab -l 2>/dev/null | grep -v -F "monitor.sh --check-daemon" | crontab - || true

echo -e "${GREEN}[✔] Sentinel environment reset successfully.${NC}"
