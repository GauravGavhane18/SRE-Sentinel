#!/usr/bin/env bash
# ==============================================================================
# Sentinel Chaos Engineering Test Script
# ==============================================================================

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

echo "===================================================================="
echo "          Sentinel Chaos Engineering Dashboard"
echo "===================================================================="
echo "Select a failure scenario to simulate:"
echo " 1) Stop Nginx Docker Container"
echo " 2) Stop PostgreSQL Database"
echo " 3) Expose Network Failure (Close Redis TCP Port/Container)"
echo " 4) Kill 'backup-daemon' OS Process"
echo " 5) Inject High CPU Stress (Simulate Resource Exhaustion)"
echo " 6) Inject Disk Space Exhaustion (Write huge file to disk)"
echo " 7) Crash Sentinel Monitoring Daemon itself"
echo " 8) Restore / Cleanup all chaos injectors"
echo "===================================================================="
read -p "Enter choice [1-8]: " choice

case $choice in
    1)
        echo "[*] Injecting failure: Stopping Nginx container..."
        docker stop sentinel-nginx
        echo -e "${GREEN}[+] Injection successful. Sentinel should detect HTTP check failure and run 'docker restart sentinel-nginx'.${NC}"
        ;;
    2)
        echo "[*] Injecting failure: Stopping Postgres container..."
        docker stop sentinel-postgres
        echo -e "${GREEN}[+] Injection successful. Sentinel should detect Docker container failure and trigger recovery.${NC}"
        ;;
    3)
        echo "[*] Injecting failure: Stopping Redis container (closes port 6379)..."
        docker stop sentinel-redis
        echo -e "${GREEN}[+] Injection successful. Sentinel should detect TCP 6379 failure and trigger recovery.${NC}"
        ;;
    4)
        echo "[*] Injecting failure: Killing backup-daemon processes..."
        pkill -f backup-daemon || echo "No backup-daemon running."
        echo -e "${GREEN}[+] Injection successful. Sentinel should detect OS process failure and run start-services.sh.${NC}"
        ;;
    5)
        echo "[*] Injecting CPU Stress..."
        # Spin up 4 background workers consuming CPU
        for i in {1..4}; do
            dd if=/dev/zero of=/dev/null &
            echo $! >> /tmp/chaos-cpu.pids
        done
        echo -e "${GREEN}[+] CPU stress injectors running. Observe system load increase on Sentinel dashboard.${NC}"
        ;;
    6)
        echo "[*] Injecting Disk Exhaustion..."
        # Fallocate or dd a large file
        df_avail=$(df -m / | tail -1 | awk '{print $4}')
        # Write a file filling 95% of remaining space, or at least 2GB
        if [ "$df_avail" -gt 5000 ]; then
            write_mb=5000
        else
            write_mb=$((df_avail - 100)) # Leave 100MB free
        fi
        echo "Writing ${write_mb}MB mock dump file to /tmp/sentinel-chaos.dump..."
        dd if=/dev/zero of=/tmp/sentinel-chaos.dump bs=1M count=$write_mb status=progress
        echo -e "${GREEN}[+] Disk space filled. Check Sentinel dashboard disk usage metrics.${NC}"
        ;;
    7)
        echo "[*] Crashing Sentinel daemon..."
        pid=$(pgrep -f "sentinel.jar")
        if [ -n "$pid" ]; then
            kill -9 "$pid"
            echo -e "${RED}[+] Sentinel daemon killed (PID $pid).${NC}"
            echo "Watchdog cron job or manual monitor.sh will be needed to revive it."
        else
            echo "Sentinel daemon is not running."
        fi
        ;;
    8)
        echo "[*] Reverting all chaos injections..."
        # Kill CPU stress
        if [ -f /tmp/chaos-cpu.pids ]; then
            while read -r pid; do
                kill "$pid" 2>/dev/null || true
            done < /tmp/chaos-cpu.pids
            rm -f /tmp/chaos-cpu.pids
        fi
        # Remove disk file
        rm -f /tmp/sentinel-chaos.dump
        # Restart containers if stopped
        docker start sentinel-nginx 2>/dev/null || true
        docker start sentinel-postgres 2>/dev/null || true
        docker start sentinel-redis 2>/dev/null || true
        # Restart process
        /bin/bash scripts/start-services.sh backup-daemon 2>/dev/null || true
        echo -e "${GREEN}[+] System cleanup completed. Monitoring should stabilize.${NC}"
        ;;
    *)
        echo -e "${RED}[!] Invalid option.${NC}"
        exit 1
        ;;
esac
