#!/usr/bin/env bash
# ==============================================================================
# Mock Backup Daemon Process (SRE Process Monitor Target)
# ==============================================================================

echo "Backup daemon initialized. PID: $$"
# Loop forever performing low-overhead sleeping to simulate a background daemon
while true; do
    sleep 30
done
