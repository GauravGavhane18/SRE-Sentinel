#!/usr/bin/env bash
# ==============================================================================
# Sentinel Monitor & Watchdog Wrapper
# ==============================================================================

PID_FILE="/tmp/sentinel-daemon.pid"

check_daemon() {
    if [ -f "$PID_FILE" ]; then
        pid=$(cat "$PID_FILE")
        if ps -p "$pid" > /dev/null 2>&1; then
            echo "$(date) - Sentinel daemon is running under PID $pid."
            exit 0
        fi
    fi
    
    # Check if Java process is running anyway
    pid=$(pgrep -f "sentinel.jar")
    if [ -n "$pid" ]; then
        echo "$pid" > "$PID_FILE"
        echo "$(date) - Sentinel daemon was running under PID $pid, updated PID file."
        exit 0
    fi
    
    echo "$(date) - Sentinel daemon is OFFLINE! Triggering self-healing watchdog..."
    # Locate sentinel jar in workspace root
    DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")"/.. && pwd)"
    nohup java -jar "$DIR/sentinel.jar" > /dev/null 2>&1 &
    new_pid=$!
    echo "$new_pid" > "$PID_FILE"
    echo "$(date) - Sentinel daemon restarted with PID $new_pid."
}

if [ "$1" == "--check-daemon" ]; then
    check_daemon
else
    # Direct start mode (runs in foreground)
    echo "Starting Sentinel Monitor Daemon..."
    java -jar sentinel.jar
fi
