# Launch Guide — Production

A step-by-step first production launch on a single server with Docker Compose. Local setup:
[`LAUNCH_LOCAL.md`](LAUNCH_LOCAL.md). Deep reference (scaling, monitoring, recovery):
[`DEPLOYMENT.md`](DEPLOYMENT.md).

## 0. What you need

- A Linux server (Ubuntu 22.04+, 4+ GB RAM) with Docker + Compose v2, ports 80/443 open
- A domain pointing at the server (the bundled nginx/TLS config serves `temurmchj.uz` — for a
  different domain, edit `docker/nginx/conf.d/default.conf` `server_name` + certificate paths)
- (For CI/CD) GitHub repo access to set secrets/variables

## 1. Server preparation

```bash
sudo mkdir -p /opt/hisobnoma && sudo chown $USER /opt/hisobnoma
git clone <repo-url> /opt/hisobnoma && cd /opt/hisobnoma
```

## 2. TLS certificates (once)

```bash
sudo apt install certbot
sudo certbot certonly --standalone -d <your-domain>
```

The prod compose mounts `/etc/letsencrypt` into nginx read-only; the `certbot` sidecar renews
every 12h. Verify `docker/nginx/conf.d/default.conf` points at
`/etc/letsencrypt/live/<your-domain>/`.

## 3. Configure `.env` (the security-critical step)

```bash
cp .env.example .env && nano .env
```

**Required — the stack will not run correctly without these:**

| Var | Note |
|---|---|
| `DB_USERNAME`, `DB_PASSWORD` | Postgres credentials (compose creates the DB with them) |
| `REDIS_PASSWORD` | Redis auth |
| `JWT_SECRET` | `openssl rand -base64 64` — **the app refuses to boot in prod with a short or placeholder secret** |
| `GRAFANA_ADMIN_PASSWORD` | Grafana login |

**Strongly recommended for a real launch:**

| Var | Why |
|---|---|
| `CORS_ALLOWED_ORIGINS` | Pin to `https://<your-domain>` (compose defaults to the bundled domain, never `*`) |
| `SMS_ENABLED=true` + `SMS_API_TOKEN` | Customer OTP login sends nothing without it |
| `SMTP_*`, `ALERT_EMAIL`, `CRITICAL_ALERT_EMAIL`, `SLACK_WEBHOOK_URL` | Otherwise Alertmanager runs with inert placeholders |
| `TELEGRAM_BOT_*`, `APNS_*` | Staff notifications / admin-app push, when ready |

## 4. First boot

```bash
docker compose -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.prod.yml ps          # wait for app: healthy
```

Flyway migrates the schema automatically (`ddl-auto: validate` guards drift). Services started:
app, postgres, redis, frontend, nginx, certbot, prometheus, grafana, alertmanager, loki,
promtail, postgres-backup.

## 5. Verify (do not skip)

```bash
# 1. App healthy (Docker healthcheck — the app publishes no host port)
docker ps --filter label=com.docker.compose.service=app --filter health=healthy

# 2. Site + API through nginx/TLS
curl -sf https://<your-domain>/actuator/health
curl -sf https://<your-domain>/ -o /dev/null && echo storefront OK

# 3. Log in at https://<your-domain> with the seeded admin/admin123 — THEN CHANGE THE PASSWORD
#    (create real staff users/roles; the default admin is a bootstrap account)

# 4. First backup exists (sidecar dumps daily; pre-deploy dumps come from the pipeline)
docker compose -f docker-compose.prod.yml exec postgres-backup ls -lh /backups

# 5. Monitoring: Grafana via SSH tunnel (localhost-bound passthrough)
ssh -N -L 3000:localhost:3000 user@server   # → http://localhost:3000
```

## 6. Wire up CI/CD (optional but recommended)

GitHub → Settings:
- **Secrets:** `STAGING_HOST/USER/SSH_KEY`, `PRODUCTION_HOST/USER/SSH_KEY`, Slack webhook
- **Variables:** `PRODUCTION_URL`, `STAGING_URL` (defaults: the bundled domain)

Then: pushes to `main` deploy to staging; version tags (`v*`) deploy to production with a
pre-deploy DB backup, blue-green scale-up (app=2 → health-checked → app=1), smoke tests
against `PRODUCTION_URL`, and rollback + Slack notify on failure.

## 7. Updating manually (no CI)

```bash
cd /opt/hisobnoma && git pull
./scripts/backup.sh                                    # belt and suspenders
docker compose -f docker-compose.prod.yml up -d --build app frontend
```

## 8. Disaster recovery

- Backups: daily gzip dumps in the `hisobnoma_postgres_backups` volume (retention
  `BACKUP_RETENTION_DAYS`, default 30); off-site S3 via `scripts/backup.sh` + `BACKUP_S3_*`
- Restore: `scripts/restore.sh` — **drill it against a scratch database before you need it**

## Launch-day checklist

- [ ] `JWT_SECRET` is 64+ random chars; DB/Redis/Grafana passwords set
- [ ] Domain matches nginx config; HTTPS works; HTTP redirects
- [ ] `CORS_ALLOWED_ORIGINS` pinned to your domain
- [ ] Default `admin` password changed; real staff accounts created
- [ ] `SMS_ENABLED=true` with a working token (test the OTP login end-to-end)
- [ ] Alert email/Slack configured and a test alert received
- [ ] A backup file exists in `/backups`; restore drill done
- [ ] CI green on the release commit (runs the Postgres migration test)
