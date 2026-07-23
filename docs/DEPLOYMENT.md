# Hisobnoma Platform - Deployment Guide

> Looking for a step-by-step launch? Use the focused guides: [`LAUNCH_LOCAL.md`](LAUNCH_LOCAL.md) (development machine) and [`LAUNCH_PRODUCTION.md`](LAUNCH_PRODUCTION.md) (first production launch). This document is the deeper operations reference.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Environment Setup](#environment-setup)
3. [Local Development](#local-development)
4. [Production Deployment](#production-deployment)
5. [Docker Deployment](#docker-deployment)
6. [Monitoring Setup](#monitoring-setup)
7. [Backup & Recovery](#backup--recovery)
8. [Scaling](#scaling)
9. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### System Requirements

- **OS**: Linux (Ubuntu 22.04+ recommended), macOS, or Windows with WSL2
- **CPU**: 2+ cores (4+ recommended for production)
- **RAM**: 4GB minimum (8GB+ recommended for production)
- **Disk**: 20GB+ available space

### Software Requirements

| Software | Version | Purpose |
|----------|---------|---------|
| Java | 21+ | Application runtime |
| Maven | 3.9+ | Build tool |
| Docker | 24+ | Container runtime |
| Docker Compose | 2.20+ | Multi-container orchestration |
| PostgreSQL | 16+ | Primary database |
| Redis | 7+ | Caching and sessions |

---

## Environment Setup

### 1. Clone Repository

```bash
git clone https://github.com/your-org/hisobnoma.git
cd hisobnoma
```

### 2. Configure Environment Variables

Copy the example environment file:

```bash
cp .env.example .env
```

Edit `.env` with your configuration:

```bash
# Required Settings
DB_USERNAME=hisobnoma
DB_PASSWORD=<secure-password>
JWT_SECRET=<512-bit-secret, at least 64 characters>
REDIS_PASSWORD=<secure-password>

# Optional Settings
APP_VERSION=1.0.0
DB_POOL_SIZE=20
JWT_ACCESS_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000
```

### 3. Generate Secrets

Generate secure secrets for production:

```bash
# Generate JWT secret (512-bit — the app refuses to boot outside dev profiles with
# a shorter/placeholder secret)
openssl rand -base64 64

# Generate database password
openssl rand -base64 24

# Generate Redis password
openssl rand -base64 24
```

---

## Local Development

### Quick Start

```bash
# Start dependencies
docker compose up -d postgres redis

# Run application
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Full Stack

```bash
# Start all services (base compose: postgres, redis, backend, frontend)
docker compose up -d

# View backend logs (the service is named `backend` in docker-compose.yml;
# it is only called `app` in docker-compose.prod.yml)
docker compose logs -f backend
```

> The monitoring stack (Prometheus, Grafana, Loki, Promtail, Alertmanager) is **prod-only** —
> it lives in `docker-compose.prod.yml`, not the base `docker-compose.yml`. See
> [Production deployment](#production-deployment) to run it.

### Development URLs

| Service | URL |
|---------|-----|
| Application (backend) | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Actuator | http://localhost:8080/actuator |
| Frontend | http://localhost:3000 |

---

## Production Deployment

### Server Preparation

1. **Update system packages:**

```bash
sudo apt update && sudo apt upgrade -y
```

2. **Install Docker:**

```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
```

3. **Install Docker Compose:**

```bash
sudo apt install docker-compose-plugin
```

4. **Create application directory:**

```bash
sudo mkdir -p /opt/hisobnoma
sudo chown $USER:$USER /opt/hisobnoma
```

### Deployment Steps

1. **Copy files to server:**

```bash
scp -r . user@server:/opt/hisobnoma/
```

2. **Configure environment:**

```bash
ssh user@server
cd /opt/hisobnoma
cp .env.example .env
nano .env  # Configure production values
```

3. **Start services:**

```bash
docker compose -f docker-compose.prod.yml up -d
```

4. **Verify deployment:**

```bash
# Check services status
docker compose -f docker-compose.prod.yml ps

# Check health
curl http://localhost:8080/actuator/health
```

### SSL/TLS Configuration

1. **Obtain SSL certificates (Let's Encrypt):**

```bash
sudo apt install certbot
sudo certbot certonly --standalone -d yourdomain.com
```

2. **Copy certificates:**

```bash
# No copying needed: docker-compose.prod.yml mounts /etc/letsencrypt read-only into the
# nginx container, and the certbot sidecar renews certificates every 12h. Just make sure
# docker/nginx/conf.d/default.conf's ssl_certificate paths match your domain, e.g.:
ls /etc/letsencrypt/live/<your-domain>/fullchain.pem
```

3. **Update nginx configuration in `docker/nginx/conf.d/default.conf`**

4. **Restart nginx:**

```bash
docker compose -f docker-compose.prod.yml restart nginx
```

---

## Docker Deployment

### Build Production Image

```bash
docker build -f Dockerfile.prod -t hisobnoma/platform:latest .
```

### Multi-Architecture Build

```bash
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -f Dockerfile.prod \
  -t hisobnoma/platform:latest \
  --push .
```

### Container Resource Limits

The production compose file includes resource limits:

| Service | CPU Limit | Memory Limit |
|---------|-----------|--------------|
| app | 2.0 | 2GB |
| postgres | 1.0 | 1GB |
| redis | 0.5 | 512MB |
| prometheus | 0.5 | 512MB |
| grafana | 0.5 | 256MB |
| alertmanager | 0.25 | 128MB |
| loki | 0.5 | 512MB |
| promtail | 0.25 | 128MB |
| postgres-backup | 0.25 | 128MB |

(nginx runs without an explicit limit.)

### Docker Commands Reference

```bash
# Start all services
docker compose -f docker-compose.prod.yml up -d

# Stop all services
docker compose -f docker-compose.prod.yml down

# View logs
docker compose -f docker-compose.prod.yml logs -f [service]

# Restart service
docker compose -f docker-compose.prod.yml restart [service]

# Scale application
docker compose -f docker-compose.prod.yml up -d --scale app=3

# Update images
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
```

---

## Monitoring Setup

### Prometheus

Prometheus is **not published to the host** — query it through Grafana (the datasource is
pre-provisioned) or from inside the compose network (`docker compose -f docker-compose.prod.yml
exec app curl http://prometheus:9090/-/healthy`).

**Key Metrics:**

| Metric | Description |
|--------|-------------|
| `http_server_requests_seconds_count` | Total HTTP requests |
| `http_server_requests_seconds_sum` | Total request duration |
| `jvm_memory_used_bytes` | JVM memory usage |
| `hikaricp_connections_active` | Active DB connections |

### Grafana

Grafana is reachable via the nginx passthrough bound to localhost on the server — open an SSH
tunnel and browse locally:

```bash
ssh -N -L 3000:localhost:3000 user@server   # then open http://localhost:3000
```

**Default credentials:**
- Username: `admin`
- Password: Set via `GRAFANA_ADMIN_PASSWORD`

**Pre-configured dashboards:**
- Hisobnoma Platform Overview
- JVM Metrics
- Database Performance
- HTTP Request Analysis

### Alertmanager

Alertmanager is internal-only (`alertmanager:9093` on the compose network); its state is
visible in Grafana (datasource pre-provisioned). Alerts route per the table below.

**Alert Routes:**
- Critical alerts → Email + Slack (immediate)
- Warning alerts → Email + Slack (5 min delay)
- Info alerts → Email only (aggregated)

**Configure alerts in `.env`:**

```bash
ALERT_EMAIL=alerts@example.com
CRITICAL_ALERT_EMAIL=oncall@example.com
SLACK_WEBHOOK_URL=https://hooks.slack.com/...
```

### Log Aggregation (Loki)

View logs in Grafana using Loki datasource.

**Log labels:**
- `job`: Service name (hisobnoma-app, nginx, postgres)
- `level`: Log level (INFO, WARN, ERROR)
- `container`: Docker container name

**Example queries:**

```logql
# Error logs
{job="hisobnoma-app"} |= "ERROR"

# Slow requests
{job="nginx"} | json | status >= 500

# Authentication failures
{job="hisobnoma-app"} |~ "authentication failed"
```

---

## Backup & Recovery

### Automated Backups

1. **Configure backup script:**

```bash
# Edit environment variables
export DB_PASSWORD=<your-password>
export BACKUP_DIR=/backups
export RETENTION_DAYS=30
```

2. **Run backup manually:**

```bash
./scripts/backup.sh
```

3. **Schedule automated backups (cron):**

```bash
# Daily at 2 AM
0 2 * * * /opt/hisobnoma/scripts/backup.sh >> /var/log/hisobnoma-backup.log 2>&1
```

### S3 Backup (Optional)

Configure S3 upload in environment:

```bash
export S3_BUCKET=hisobnoma-backups
export AWS_ACCESS_KEY_ID=<key>
export AWS_SECRET_ACCESS_KEY=<secret>
```

### Database Restore

```bash
# Restore from latest backup
./scripts/restore.sh --latest

# Restore from specific backup
./scripts/restore.sh -f /backups/hisobnoma_20240115_020000.sql.gz

# Restore with database recreation
./scripts/restore.sh --latest --drop-existing

# Dry run (verify only)
./scripts/restore.sh --latest --dry-run
```

### Point-in-Time Recovery

1. **Enable WAL archiving in PostgreSQL:**

```yaml
# postgres runs on the image defaults; set WAL parameters via ALTER SYSTEM
# (or mount a custom postgresql.conf into the postgres service):
archive_mode = on
archive_command = 'cp %p /backups/wal/%f'
```

2. **Restore to specific point:**

```bash
# Set recovery target
recovery_target_time = '2024-01-15 14:30:00'
```

---

## Scaling

### Horizontal Scaling

Scale application instances:

```bash
docker compose -f docker-compose.prod.yml up -d --scale app=3
```

### Load Balancing

Nginx automatically load-balances across app instances.

Configure in `docker/nginx/conf.d/default.conf`:

```nginx
upstream app_servers {
    least_conn;  # or ip_hash for session affinity
    server app:8080;
}
```

### Database Scaling

For high-traffic scenarios:

1. **Read replicas:**
   - Configure PostgreSQL streaming replication
   - Use separate connection pools for reads/writes

2. **Connection pooling:**
   - Deploy PgBouncer between app and database
   - Configure pool mode (transaction/session)

### Redis Scaling

1. **Redis Cluster:**
   - Minimum 6 nodes (3 masters, 3 replicas)
   - Update Spring configuration for cluster mode

2. **Redis Sentinel:**
   - High availability with automatic failover
   - Minimum 3 sentinel instances

---

## Troubleshooting

### Common Issues

#### Application Won't Start

```bash
# Check logs
docker compose -f docker-compose.prod.yml logs app

# Common causes:
# - Database not ready: Check postgres health
# - Invalid configuration: Verify .env file
# - Port conflict: Check if 8080 is available
```

#### Database Connection Issues

```bash
# Test database connectivity
docker exec -it hisobnoma-postgres psql -U hisobnoma -d hisobnoma

# Check connection pool
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active

# Increase pool size if needed
DB_POOL_SIZE=30
```

#### High Memory Usage

```bash
# Check JVM heap
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# Adjust JVM settings in Dockerfile.prod
JAVA_OPTS="-XX:MaxRAMPercentage=70.0"
```

#### Slow Performance

```bash
# Check P95 latency
curl http://localhost:8080/actuator/metrics/http.server.requests

# Enable query logging
logging.level.org.hibernate.SQL=DEBUG

# Check for N+1 queries in logs
```

### Health Check Endpoints

| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | Overall health status |
| `/actuator/health/db` | Database health |
| `/actuator/health/redis` | Redis health |
| `/actuator/health/diskSpace` | Disk space check |
| `/actuator/info` | Application info |
| `/actuator/prometheus` | Prometheus metrics |

### Log Locations

| Service | Log Path |
|---------|----------|
| Application | `/app/logs/*.log` (in container) |
| Nginx | `/var/log/nginx/*.log` |
| PostgreSQL | Docker logs |
| All services | `docker compose logs [service]` |

### Support

For issues and support:

1. Check application logs: `docker compose logs -f app`
2. Review Grafana dashboards for metrics
3. Search existing issues on GitHub
4. Create new issue with:
   - Environment details
   - Error messages
   - Steps to reproduce

---

## Production launch checklist (ops)

Everything below is wired in `docker-compose.prod.yml` / `.github/workflows/deploy.yml`; these are
the switches an operator must still set per environment:

1. **GitHub repository variables** — `PRODUCTION_URL` and `STAGING_URL` (Settings → Variables).
   The deploy workflow's environment links and smoke tests use them; defaults are
   `https://temurmchj.uz` / `https://staging.temurmchj.uz` (the domain the bundled nginx config and
   TLS certs serve). If you serve a different domain, also update
   `docker/nginx/conf.d/default.conf` (`server_name` + cert paths) and re-issue certs.
2. **Server `.env`** (in `/opt/hisobnoma`, next to the compose file) — required:
   `DB_USERNAME`, `DB_PASSWORD`, `REDIS_PASSWORD`, `JWT_SECRET` (≥64 chars — the app refuses to
   boot on a weak secret outside dev profiles), `GRAFANA_ADMIN_PASSWORD`.
   Recommended: `CORS_ALLOWED_ORIGINS` (compose defaults it to the served domain — never `*`),
   `SMS_ENABLED=true` + `SMS_API_TOKEN` (OTP login needs it), `TELEGRAM_*`, `APNS_*` when ready.
3. **Alerting** — the `alertmanager` service substitutes `SMTP_HOST/PORT/FROM/USERNAME/PASSWORD`,
   `ALERT_EMAIL`, `CRITICAL_ALERT_EMAIL`, `SLACK_WEBHOOK_URL` from `.env` at container start. It
   boots with inert placeholders when unset — set them or alerts go nowhere.
4. **Backups** — the `postgres-backup` sidecar dumps daily (`BACKUP_INTERVAL_SECONDS`, default
   86400) into the `hisobnoma_postgres_backups` volume with `BACKUP_RETENTION_DAYS` (default 30)
   retention. Verify a dump exists after first deploy: `docker compose -f docker-compose.prod.yml
   exec postgres-backup ls -lh /backups`. Off-site copy (S3) still requires running
   `scripts/backup.sh` with the `S3_BUCKET`/AWS vars (e.g. from host cron).
5. **Logs** — Loki + promtail ship app/nginx/container logs; browse them in Grafana (the Loki
   datasource is pre-provisioned).
6. **Restore drill** — test `scripts/restore.sh` against a staging copy before you need it.

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2024-01 | Initial release |
| 1.1.0 | 2026-07 | Alertmanager/Loki/promtail deployed; scheduled backup sidecar; scalable app service; domain-parameterized deploy verification; ops launch checklist |

---

*Last updated: July 2026*
