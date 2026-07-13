#!/usr/bin/env bash
# ==============================================================================
# Sentinel SRE Stack Installation & Setup Script
# ==============================================================================
set -e

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

echo "===================================================================="
echo "          Installing Sentinel Self-Healing Daemon"
echo "===================================================================="

# 1. Directory Setup
echo "[*] Initializing folder structure..."
mkdir -p config logs scripts screenshots
echo -e "${GREEN}[+] Directories initialized successfully.${NC}"

# 2. Dependency Checking
echo "[*] Checking system dependencies..."
dependencies=("java" "docker" "docker-compose" "curl" "nc" "uptime" "df" "free" "ps")
missing=0

for dep in "${dependencies[@]}"; do
    if command -v "$dep" >/dev/null 2>&1; then
        echo -e "  [✔] $dep is installed"
    else
        echo -e "  [✘] $dep is NOT installed"
        missing=$((missing + 1))
    fi
done

if [ $missing -gt 0 ]; then
    echo -e "${RED}[!] Warning: $missing dependencies are missing. Please install them to ensure full features.${NC}"
else
    echo -e "${GREEN}[+] All core system utilities are available!${NC}"
fi

# 3. Compiling Java Orchestrator
if command -v mvn >/dev/null 2>&1; then
    echo "[*] Compiling Sentinel Java orchestrator..."
    mvn clean package -DskipTests
    cp target/sre-sentinel-1.0-SNAPSHOT.jar sentinel.jar
    echo -e "${GREEN}[+] Compilation completed: sentinel.jar is ready.${NC}"
else
    echo -e "${RED}[!] Maven (mvn) not found. Checking for pre-compiled sentinel.jar...${NC}"
    if [ -f "sentinel.jar" ]; then
        echo -e "${GREEN}[+] Pre-compiled sentinel.jar found.${NC}"
    else
        echo -e "${RED}[!] No pre-compiled jar found and Maven is missing. Please build the jar manually.${NC}"
    fi
fi

# 4. Cron Configuration (Module 9: Automation requirement)
echo "[*] Configuring watchdog Cron job for Sentinel daemon auto-recovery..."
CRON_JOB="*/5 * * * * $(pwd)/scripts/monitor.sh --check-daemon >> $(pwd)/logs/sentinel-watchdog.log 2>&1"
(crontab -l 2>/dev/null | grep -F "monitor.sh --check-daemon") && echo -e "  [✔] Cron watchdog is already configured" || {
    (crontab -l 2>/dev/null; echo "$CRON_JOB") | crontab -
    echo -e "${GREEN}[+] Cron watchdog job appended to crontab successfully.${NC}"
}

echo "===================================================================="
echo -e "${GREEN}[✔] Sentinel Installation completed successfully!${NC}"
echo "Run './scripts/start.sh' to boot the stack."
echo "===================================================================="
