# Launch Guide — Local Development

Get the full platform running on your machine. For servers, see
[`LAUNCH_PRODUCTION.md`](LAUNCH_PRODUCTION.md).

## Prerequisites

Java 21, Maven 3.9+ (or the bundled `./mvnw`), Docker + Compose v2, Node 18+ (staff frontend),
Flutter (only for the customer app).

## 1. Environment

```bash
git clone <repo-url> hisobnoma && cd hisobnoma
cp .env.example .env        # optional locally — dev has safe defaults for everything
```

You do **not** need to fill `.env` to run locally: the `dev` profile defaults to
`jdbc:postgresql://localhost:5432/hisobnoma_dev` with `postgres/postgres`, Redis on
`localhost:6379` without a password, and a placeholder JWT secret (tolerated only under
dev/test profiles — production refuses to boot with it). If a `.env` file exists at the repo
root it is loaded at startup (real environment variables always win).

## 2. Start dependencies

```bash
docker compose up -d postgres redis     # Postgres 16 on 5432 (db hisobnoma_dev), Redis on 6379
```

## 3. Run the backend

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Flyway applies all migrations on first boot and seeds roles, permissions, and a default
super-admin: **`admin` / `admin123`** (change it immediately on any shared machine).

Useful URLs:
| What | URL |
|---|---|
| API | http://localhost:8080/api/v1 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/actuator/health |

## 4. Staff frontend (Vue)

```bash
cd frontend
npm install
npm run dev                 # http://localhost:5173, proxied to the backend
```

Log in with the seeded `admin` / `admin123`.

## 5. Customer shop app (Flutter, optional)

```bash
cd mobile-shop
flutter pub get
flutter run --dart-define=SHOP_API_BASE_URL=http://10.0.2.2:8080   # Android emulator → host
```

Anonymous storefront calls require the tenant header — the app sends `X-Tenant-ID`
(`SHOP_TENANT_ID`, default `1`). When calling the shop API by hand (curl/Postman), always add
`X-Tenant-ID: 1`, otherwise you get `400 TENANT_REQUIRED`. A full curl walkthrough lives in
[`api/MOBILE_SHOP_API.md`](api/MOBILE_SHOP_API.md).

## 6. Everything in containers (alternative)

```bash
docker compose up -d        # postgres, redis, backend (8080), frontend (3000)
docker compose logs -f backend
```

## 7. Tests

```bash
./mvnw test                 # unit + full-flow against H2 (no Docker needed)
./mvnw verify               # adds the Postgres migration test when Docker is available
cd frontend && npx eslint . && npx vite build
cd mobile-shop && flutter analyze && flutter test
```

## Local gotchas

- **`400 TENANT_REQUIRED`** on shop/B2B endpoints → you forgot `X-Tenant-ID` (there is no
  silent default tenant; this is intentional).
- **SMS OTP** is disabled by default (`SMS_ENABLED=false`) — the code is generated and stored,
  but no SMS is sent; watch the backend log or read `web_otp_codes` in dev to log in.
- **Push (APNs/FCM), payments, Telegram** are all off by default and log no-ops — nothing to
  configure for normal development.
- Rate limiting is in-memory in dev (`RATELIMIT_REDIS=false`), so no Redis dependency for it.
- Port clashes: 5432/6379/8080/5173 (and 3000 if using full compose).
