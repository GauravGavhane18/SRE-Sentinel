# Sentinel: Self-Healing Linux Infrastructure Monitoring System

Sentinel is a lightweight monitoring daemon that keeps track of host health (CPU, Memory, Disk), systemd/OS processes, network ports, and Docker containers. When a service fails, Sentinel runs automated recovery commands to restore it.

---

## 🚀 Quick Start

### 1. Prerequisites
Ensure you have Docker and Java 17 installed on your Linux machine.

### 2. Installation
Make scripts executable and run the installer:
```bash
chmod +x scripts/*.sh
./scripts/install.sh
```

### 3. Run the Monitoring System
Start the target Docker containers, mock processes, and the live dashboard:
```bash
./scripts/start.sh
```
This opens the live ANSI console dashboard in your terminal, showing real-time resource usage and service states.

### 4. Inject Chaos (Testing)
Run the interactive chaos testing script to manually stop services or stress CPU/Disk and watch Sentinel recover them:
```bash
./scripts/chaos.sh
```

### 5. Stop the System
To gracefully stop all containers, mock processes, and the monitoring daemon:
```bash
./scripts/stop.sh
```

---

## 📁 Key Directories & Files

- `config/sentinel-config.json`: Declarative JSON file to configure target service health checks and recovery commands.
- `logs/incidents.log`: Real-time audit log documenting service failures, diagnostics, and recovery status.
- `scripts/`: Helper scripts for installer, startup, chaos injection, and cleanup.
- `src/`: Java source code for the monitoring daemon.
- `docker-compose.yml`: Target services (Nginx, Postgres, Redis) monitored by Sentinel.

---

## ⚙️ Configuration Example

Services are defined in `config/sentinel-config.json`:
```json
{
  "name": "Nginx-Web-HTTP",
  "type": "HTTP",
  "target": "http://localhost:8080/",
  "checkIntervalMs": 5000,
  "timeoutMs": 2000,
  "recoveryCommand": "docker restart sentinel-nginx"
}
```
