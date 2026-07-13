# Sentinel: Self-Healing Linux Infrastructure Monitoring System

A lightweight, production-like SRE daemon designed to monitor Linux host health, systemd services, Docker containers, and network connectivity, with automated self-healing recovery and root-cause analysis (RCA) log collection. 

This project is structured for a final-year Computer Science student applying for a **Systems Reliability Engineer (SRE) Internship** at Nutanix, emphasizing Linux administration, networking, process management, and infrastructure automation.

---

## 🏗️ Architecture & Component Flow

```
                      +---------------------------------------+
                      |         Linux Ubuntu Host             |
                      +---------------------------------------+
                                          |
                      +-------------------+-------------------+
                      |                   |                   |
            [Host Resources]      [Linux Services]     [Docker Containers]
             CPU, Mem, Disk           systemd           nginx, pg, redis
             /proc/loadavg         pgrep, systemctl     docker inspect
                      |                   |                   |
                      +-------------------+-------------------+
                                          |
                                          ▼
                      +---------------------------------------+
                      |       Java SRE Daemon Orchestrator    |
                      |   - Schedules Check Loops             |
                      |   - Parses Exit Codes & Stdout        |
                      |   - Implements Exponential Backoff    |
                      +---------------------------------------+
                                          |
                       (On Failure: Collect Diagnostics)
                                          |
                                          ▼
                      +---------------------------------------+
                      |         Root Cause Diagnostics        |
                      |     df -h, free -m, docker logs,      |
                      |     journalctl -u, ping, ss -tulpn    |
                      +---------------------------------------+
                                          |
                       (Execute Auto-Recovery Command)
                                          |
                                          ▼
                      +---------------------------------------+
                      |           ProcessBuilder              |
                      |   docker restart / systemctl restart  |
                      +---------------------------------------+
                      |                   |                   |
                      ▼                   ▼                   ▼
            [logs/incidents.log]   [Console UI Dashboard]  [Cron Watchdog]
            Structured logs & RCA   Real-time ANSI Panel   Heals Sentinel
```

---

## 📁 Folder Structure

```
Sentinel/
├── config/
│   └── sentinel-config.json     # Declarative service check & recovery config
├── logs/
│   ├── incidents.log            # SRE centralized incident & RCA log file
│   └── sentinel-watchdog.log    # Daemon watchdog logs
├── scripts/
│   ├── install.sh               # Dependency checks, folder prep, watchdog setup
│   ├── start.sh                 # Boots Docker Compose and starts monitoring Panel
│   ├── stop.sh                  # Graceful teardown of containers and daemon
│   ├── monitor.sh               # Watchdog monitor called by cron to self-heal Sentinel
│   ├── chaos.sh                 # Interactive chaos injector (kills services/consumes CPU)
│   ├── cleanup.sh               # System reset (removes logs, cron, stops stress tests)
│   ├── backup-daemon.sh         # Mock background service for OS process checks
│   └── start-services.sh        # Process recovery orchestrator for mock services
├── src/
│   ├── main/java/com/sentinel/  # Java Orchestration & Log collection source
│   └── test/java/com/sentinel/  # JUnit 5 unit verification tests
├── Dockerfile                   # Runner container for the Sentinel daemon
├── docker-compose.yml           # Runs target services (Nginx, Redis, Postgres)
├── sentinel.jar                 # Compiled executable Java Jar
├── pom.xml                      # Maven project configuration
└── README.md                    # Project documentation
```

---

## 🛠️ Technologies Used

* **OS**: Linux (Ubuntu 20.04/22.04 LTS target)
* **Containerization**: Docker, Docker Compose
* **Utilities**: `systemctl`, `journalctl`, `pgrep`, `docker inspect`, `curl`, `nc` (netcat), `df`, `free`, `uptime`, `dd`
* **Automation**: Bash (SRE scripts, Chaos scenarios)
* **Scheduler & Logging**: Java 17 (ProcessBuilder, ScheduledExecutorService, Jackson JSON, JUnit 5)

---

## 🚀 Installation & Running

### 1. Prerequisite Checks
Ensure you have Docker and Java 17 installed on your Linux machine.

### 2. Setup and Compile
Run the automated installation script. It verifies system utilities, sets up folders, compiles the Java Jar, and registers a cron watchdog task:
```bash
chmod +x scripts/*.sh
./scripts/install.sh
```

### 3. Start the Stack
Boot the Docker services, mock background processes, and open the interactive console dashboard:
```bash
./scripts/start.sh
```

This will open the live terminal interface displaying real-time host resource metrics and service statuses.

---

## 🖥️ Live Console Dashboard Interface

Sentinel clears the terminal and renders an active ANSI dashboard refreshed every 1.5 seconds:

```
========================================================================================
       SENTINEL : SELF-HEALING INFRASTRUCTURE MONITORING DAEMON (SRE PANEL)             
========================================================================================
 [HOST RESOURCES]
  CPU Load: 0.15, 0.22, 0.18           | Host Uptime: up 2 hours, 14 minutes
  Memory:   Used: 2314MB / Total: 7980MB | Disk Space:  Used: 42% (23GB free of 40GB)
  Processes: 148                         | Zombie Proc: 0
----------------------------------------------------------------------------------------
 [DAEMON METRICS]
  Uptime: 00h 04m 12s          | Total Checks: 184      | Successful: 181     
  Failed Checks: 3             | Recovery Success: 1     | Avg Recovery Time: 2150ms
========================================================================================
 SERVICE NAME            | STATUS       | LATENCY      | TYPE     | DETAILS / MESSAGE   
----------------------------------------------------------------------------------------
 Nginx-Web-HTTP          | ● UP         | 4 ms         | HTTP     | HTTP 200 - OK (via curl)
 Redis-Cache-TCP         | ● UP         | 2 ms         | TCP      | TCP Connection established
 Postgres-DB-Docker      | ● UP         | 8 ms         | DOCKER   | Container running   
 Backup-Daemon-Process   | ● UP         | 5 ms         | PROCESS  | Process active (PIDs: 4109)
========================================================================================
 RECENT AUTOMATED INCIDENT RECOVERIES (Tail - 4 logs/incidents.log)
----------------------------------------------------------------------------------------
 [2026-07-12 15:30:15] Nginx-Web-HTTP (HTTP) -> Recovery: SUCCESS (Duration: 2150ms) | RCA: HTTP status code: 502
========================================================================================
```

---

## 🔄 How Monitoring & Self-Healing Works

### 1. Decoupled SRE Checks (80% Linux Utilities)
Sentinel delegates the health verification to Linux system tools using Java `ProcessBuilder` instead of running them natively in JVM. This guarantees that health checks represent real-world network and OS behavior:
* **HTTP Checker**: Runs `curl` and checks status codes.
* **TCP Checker**: Scans port reachability using netcat (`nc`).
* **Docker Checker**: Runs `docker inspect` to verify container runtime status.
* **Process Checker**: Runs `pgrep` to check process table records.

### 2. Failure Detection & Exponential Backoff
When a check fails, the service enters the `DEGRADED` state. Sentinel attempts immediate retries, applying an exponential backoff policy (e.g., waiting 1s, then 2s, then 4s) to prevent a failing system from being overwhelmed by constant check connections (thundering herd problem).

### 3. Automated RCA Diagnostics (Module 7)
If all retry attempts are exhausted, the service is marked `UNHEALTHY`. Before running the recovery command, Sentinel executes diagnostics commands and saves a root-cause report to `logs/incidents.log`:
* **Docker containers**: Extracts `docker logs --tail 25`, `docker inspect`, and `docker stats`.
* **OS Processes**: Grabs `systemctl status` and `journalctl -u <service> -n 25`.
* **Network Nodes**: Performs `ping`, `getent hosts` (DNS resolution), and `ss` port audits.

### 4. Self-Healing Action
Sentinel invokes the configured `recoveryCommand` (e.g., `docker restart <container>` or `systemctl restart <service>`) via `ProcessBuilder`. After a 2-second stabilization delay, a verification check is run to confirm recovery.

---

## 💥 Chaos Engineering Verification

Sentinel provides an interactive chaos engineering console to verify self-healing behavior under failures. 

To execute the chaos injector:
```bash
./scripts/chaos.sh
```

### Chaos Scenarios

1. **Nginx Web Server Down**:
   * *Injection*: Select `1` in chaos console. (Executes `docker stop sentinel-nginx`).
   * *Observation*: Sentinel dashboard shows `Nginx-Web-HTTP` status transition to `RETRYING` (yellow), then `DOWN` (red). Sentinel executes `docker restart sentinel-nginx` and heals the service back to `UP` (green).
2. **Process Termination**:
   * *Injection*: Select `4` in chaos console. (Executes `pkill -f backup-daemon`).
   * *Observation*: Sentinel identifies the missing PID via `pgrep` check, runs `scripts/start-services.sh backup-daemon`, and restores process active status.
3. **Resource Stress (CPU & Disk)**:
   * *Injection*: Select `5` and `6` in chaos console. (Spins up background workers and creates a huge temp file).
   * *Observation*: The dashboard shows high load averages and critical disk space percentages, allowing SREs to monitor stress levels.

---

## 📝 Incident Log Format (`logs/incidents.log`)

Every incident triggers a structured, human-readable report. This log combines quick-parse status summaries with detailed host snapshots and stdout records:

```
[2026-07-12 15:30:15] SERVICE=Nginx-Web-HTTP | TYPE=HTTP | ATTEMPTS=3 | DURATION=2150ms | STATUS=SUCCESS | RCA="HTTP status code: 502"

================================================================================
          AUTOMATED ROOT CAUSE DIAGNOSTICS & TROUBLESHOOTING REPORT
================================================================================
Service/Target:     Nginx-Web-HTTP (http://localhost:8080/)
Check Type:         HTTP
Error Reason:       HTTP status code: 502 (via curl)
Timestamp:          2026-07-12T15:30:13.125
--------------------------------------------------------------------------------

[HOST RESOURCE SUMMARY]
Disk space: Filesystem      Size  Used Avail Use% Mounted on
/dev/sda1        40G   23G   15G  62% /
Memory:     
              total        used        free      shared  buff/cache   available
Mem:           7980        5120        1230         150        1630        2450
Uptime/Load: 15:30:13 up 2:14,  1 user,  load average: 0.15, 0.22, 0.18
--------------------------------------------------------------------------------

[DOCKER DIAGNOSTICS: sentinel-nginx]
--- Last 25 Container Logs ---
2026/07/12 15:30:10 [error] 31#31: *5 connect() failed (111: Connection refused) while connecting to upstream...
127.0.0.1 - - [12/Jul/2026:15:30:10 +0000] "GET / HTTP/1.1" 502 157 "-" "curl/7.81.0"

--- Container State Inspect ---
{"Status":"running","Running":true,"Paused":false,"Restarting":false,"OOMKilled":false,"Dead":false,"Pid":28392,"ExitCode":0,"Error":""}

--- Container Resource Stats ---
CONTAINER ID   NAME             CPU %     MEM USAGE / LIMIT   MEM %     NET I/O          BLOCK I/O   PIDS
464d1ea0293d   sentinel-nginx   0.02%     2.34MiB / 7.8GB     0.03%     1.2kB / 580B     0B / 0B     2
================================================================================
```

---

## 💼 Resume Statements Justification

By building and running this project, you can confidently discuss and defend the following resume highlights in interviews:

* **"Built a self-healing infrastructure monitoring system for Linux services and Docker containers..."**:
  * *Justification*: You wrote a daemon that connects directly to the Docker daemon socket (`docker.sock`) and uses `systemctl`/`pgrep` to track target configurations.
* **"Automated service recovery by executing Linux and Docker commands through ProcessBuilder..."**:
  * *Justification*: The Java orchestrator handles multi-thread concurrency, coordinates checking strategies, and uses `ProcessBuilder` to safely restart containers and systemd services.
* **"Implemented a centralized incident logging module to record service failures, recovery attempts, and system events..."**:
  * *Justification*: You created `IncidentLogger` to write grep-ready key-value strings alongside multi-line resource profiles and service command logs in `logs/incidents.log`.
* **"Performed chaos testing by intentionally stopping services and containers..."**:
  * *Justification*: You wrote `scripts/chaos.sh` which stresses resources and stops containers, proving the effectiveness of the healing actions.

---

## 💬 SRE Interview Prep (Nutanix Focus)

Here are key questions you might receive about this project in a Nutanix systems engineering interview:

### Q1: Why did you choose to run `curl` and `nc` commands via `ProcessBuilder` instead of using native Java HTTP clients and sockets?
* **Answer**: In SRE and production engineering, monitoring tools should replicate standard client behavior. Command-line tools like `curl` and `nc` respect system-level DNS configurations, environment variables (like proxy settings), and kernel TCP timeout parameters exactly as an operator's command line would. Additionally, invoking native CLI tools limits dependencies inside the Java orchestrator, maintaining a lightweight footprints.

### Q2: What is "Load Average" and how does Sentinel collect it?
* **Answer**: Load average is the average number of runnable processes in the OS task run-queue over a given time interval (1, 5, and 15 minutes). It reflects CPU demand and disk I/O bottlenecks. Sentinel reads load averages directly from `/proc/loadavg` on Linux, which is a virtual file system representing kernel state. This approach is highly efficient because it avoids process fork overhead.

### Q3: How does Sentinel prevent infinite restart loops if a service has a permanent config failure?
* **Answer**: Sentinel limits the number of retries per failure event (`maxRetries` in configuration) and applies exponential backoff delays. If the service fails to stabilize after the recovery command, Sentinel marks the status as `UNHEALTHY` and logs the failure without repeating the recovery command, preventing CPU cycle wastage and log storage exhaustion.

### Q4: How is Sentinel itself kept running (watchdog design)?
* **Answer**: Sentinel is protected by a secondary system watchdog pattern. We register a watchdog script (`monitor.sh --check-daemon`) as a Linux cron job running every 5 minutes. The script checks for the daemon's PID or checks if `sentinel.jar` exists in the process list. If the process is dead, the script automatically restarts it. This represents a classic "Who monitors the monitor?" self-healing pattern.
