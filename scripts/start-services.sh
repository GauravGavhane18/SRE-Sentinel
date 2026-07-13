#!/usr/bin/env bash
# ==============================================================================
# Startup Services script for Sentinel Process recovery
# ==============================================================================

TARGET=$1

if [ "$TARGET" == "backup-daemon" ]; then
    # Kill any existing daemon instances
    pkill -f backup-daemon.sh || true
    
    # Run mock backup-daemon in background
    nohup /bin/bash scripts/backup-daemon.sh > /dev/null 2>&1 &
    echo "[+] backup-daemon started in background."
else
    echo "Unknown service: $TARGET"
    exit 1
fi
