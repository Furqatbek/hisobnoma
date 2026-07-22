# Hisobnoma Platform

Multi-tenant ERP for retail/wholesale businesses in Uzbekistan: inventory, POS, finance
(AR/AP/GL/banking/tax), HR, delivery, distribution (routing, van sales, agent KPIs), an online
customer shop (Flutter app + public API), a B2B bulk-order API, and staff mobile apps.

**Stack:** Java 21 · Spring Boot 3.3 · PostgreSQL 16 (Flyway) · Redis · Vue 3 (staff admin) ·
Flutter (customer shop, `mobile-shop/`) · Docker Compose (prod: nginx/TLS, Prometheus, Grafana,
Loki, Alertmanager, scheduled backups).

## Quick start (development)

```bash
cp .env.example .env                      # fill in local values (see the file's comments)
docker compose up -d postgres redis
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
# staff frontend: cd frontend && npm install && npm run dev
```

Tests: `./mvnw test` (H2; the Postgres migration test runs in CI/Docker). Swagger (dev):
`http://localhost:8080/swagger-ui.html`.

## Documentation

| Topic | Doc |
|---|---|
| Platform API contract | [`docs/API.md`](docs/API.md) |
| Module APIs (AP, AR, Banking/Tax, Delivery, POS, Reports) | [`docs/api/`](docs/api/) |
| Customer shop API (+ demo walkthrough) | [`docs/api/MOBILE_SHOP_API.md`](docs/api/MOBILE_SHOP_API.md) |
| Staff mobile app API | [`docs/api/MOBILE_MODULE_API.md`](docs/api/MOBILE_MODULE_API.md) |
| APNs push notifications | [`docs/api/MOBILE_PUSH_API.md`](docs/api/MOBILE_PUSH_API.md) |
| Multi-tenancy & data isolation (+ SaaS roadmap) | [`docs/MULTI_TENANCY.md`](docs/MULTI_TENANCY.md) |
| Deployment & operations (incl. launch checklist) | [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md) |
| Production-readiness audit trail | [`docs/PRODUCTION_READINESS.md`](docs/PRODUCTION_READINESS.md) |
| Online-shop plan / roadmap | [`docs/WEB_SHOP_PLAN.md`](docs/WEB_SHOP_PLAN.md) |
| Customer Flutter app | [`mobile-shop/README.md`](mobile-shop/README.md) |

## Repository layout

```
src/main/java/com/hisobnoma/platform/   backend (module-per-domain packages)
src/main/resources/db/migration/        Flyway migrations (V1…)
frontend/                               Vue 3 staff admin SPA
mobile-shop/                            Flutter customer shop app
docker/                                 nginx, prometheus, grafana, loki, alertmanager configs
scripts/                                backup.sh / restore.sh
.github/workflows/                      CI (tests vs real Postgres) + deploy pipeline
```

## Key conventions

- **Multi-tenancy:** every tenant-owned row carries `tenant_id`; queries are tenant-scoped and
  requests without a resolvable tenant fail closed (`400 TENANT_REQUIRED`). Details + developer
  rules: `docs/MULTI_TENANCY.md`.
- **RBAC:** every staff endpoint is permission-guarded (enforced by an ArchUnit test); permissions
  are seeded in migrations and mirrored by frontend route guards.
- **Migrations:** production runs `ddl-auto: validate` — schema changes only via Flyway.
- **Payments:** the customer shop is cash-on-delivery; online card providers are disabled until
  merchant credentials exist and never fake success.
