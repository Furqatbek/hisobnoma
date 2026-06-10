# Hisobnoma Shop — customer mobile app

Flutter app for the store's customers: browse the curated **live item list** published from the
Hisobnoma admin (“Веб-каталог” page) and order by phone/Telegram. In-app checkout arrives in
Phase 3 (see `../docs/WEB_SHOP_PLAN.md`).

The app talks only to the public catalog API (`/api/v1/web/catalog/**`, anonymous,
`X-Tenant-ID` header) documented in `../docs/API.md`. No login is required.

## Project layout

```
lib/
  main.dart                      app entry, theme
  config/app_config.dart         build-time config (--dart-define)
  api/catalog_api.dart           CatalogApi (abstract) + HttpCatalogApi
  models/                        PublicProduct, PublicCategory, PageResult
  screens/catalog_screen.dart    grid, search, category chips, pagination
  screens/product_detail_screen.dart  images, price, order buttons
  widgets/                       ProductCard, error/empty states
  util/format.dart               UZS price formatting
  l10n/strings.dart              uz-Cyrl strings
test/                            unit + widget tests (`flutter test`)
```

## Build-time configuration

All settings are injected with `--dart-define` (no secrets in the repo):

| Define | Meaning | Default |
|--------|---------|---------|
| `SHOP_API_BASE_URL` | Backend base URL, no trailing slash | `http://10.0.2.2:8080` in debug |
| `SHOP_TENANT_ID` | Tenant sent as `X-Tenant-ID` | `1` |
| `SHOP_PHONE` | "Order by phone" number (`tel:` link) | empty → button hidden |
| `SHOP_TELEGRAM` | "Order via Telegram" link | empty → button hidden |

## Run in development

```bash
flutter pub get
flutter run \
  --dart-define=SHOP_API_BASE_URL=http://10.0.2.2:8080 \
  --dart-define=SHOP_PHONE=+998901234567 \
  --dart-define=SHOP_TELEGRAM=https://t.me/yourshop
```

`10.0.2.2` reaches the host machine's `localhost` from the Android emulator. For a physical
phone on the same Wi-Fi, use the computer's LAN IP; for production, the VPS URL (HTTPS).

> Plain HTTP (development only) requires cleartext traffic to be allowed; Android release
> builds should always point to an HTTPS URL.

## Tests

```bash
flutter analyze
flutter test
```

Covered: UZS formatting, JSON mapping of all API payloads, HTTP client behaviour
(tenant header, query params, error handling, UTF-8), and widget tests for the catalog
screen (grid render, stock badges, empty state, error + retry, search, category filter).

## Build a debug APK (for the owner's phone)

```bash
flutter build apk --debug \
  --dart-define=SHOP_API_BASE_URL=https://your-vps-domain \
  --dart-define=SHOP_PHONE=+998901234567 \
  --dart-define=SHOP_TELEGRAM=https://t.me/yourshop
# → build/app/outputs/flutter-apk/app-debug.apk  (share via link/QR/Telegram)
```

## Release build checklist

1. Generate a keystore (once, keep it safe — losing it means losing the app identity):
   ```bash
   keytool -genkey -v -keystore ~/hisobnoma-shop.jks -keyalg RSA \
     -keysize 2048 -validity 10000 -alias shop
   ```
2. Create `android/key.properties` (gitignored):
   ```properties
   storePassword=...
   keyPassword=...
   keyAlias=shop
   storeFile=/absolute/path/hisobnoma-shop.jks
   ```
3. Wire signing in `android/app/build.gradle.kts` (standard Flutter signing config).
4. Build: `flutter build apk --release --dart-define=...` (same defines as above, HTTPS URL).
5. Install on a clean device and run the manual checklist below.
6. Store submission is deferred (Phase 5); distribute the APK directly via link/QR until then.

## Manual test checklist (per release)

- [ ] Catalog opens and shows exactly the LIVE items from the admin "Веб-каталог" page
- [ ] Draft items are not visible
- [ ] Product images load (uploads are served from the backend `/uploads/**`)
- [ ] Prices match admin values, including per-item price overrides
- [ ] Out-of-stock product shows "Тугаган" badge
- [ ] Search and category chips filter correctly
- [ ] Pull-to-refresh picks up newly published items
- [ ] Airplane mode → friendly error with a working "Қайта уриниш" retry
- [ ] Product detail shows description and image gallery (swipe between images)
- [ ] "Қўнғироқ қилиш" opens the dialer with the configured number
- [ ] "Telegram орқали буюртма" opens the configured Telegram chat
- [ ] Looks correct on a small phone (360 px wide) and a tablet
