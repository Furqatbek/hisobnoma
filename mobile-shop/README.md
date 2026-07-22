# Hisobnoma Shop — customer mobile app

> **Ownership note (2026-06-10):** the customer mobile app is now developed by a separate
> mobile team. This Flutter project is kept as a **working reference implementation** of
> the public shop API (catalog with sale badges, cart price preview, checkout, order
> status, SMS-OTP auth, order history) — every public endpoint is exercised here with
> tests. The full customer API — every endpoint with request/response shapes and a runnable
> demo walkthrough — is documented in **[`../docs/api/MOBILE_SHOP_API.md`](../docs/api/MOBILE_SHOP_API.md)**
> (the broader platform contract is also in `../docs/API.md`); public `/api/v1/web/**`
> endpoints only change additively, so installed apps never break.

Flutter app for the store's customers: browse the curated **live item list** published from the
Hisobnoma admin (“Веб-каталог” page), add products to a persistent cart and **order in-app**
(name + phone + delivery region/village). Orders land in the admin "Онлайн буюртмалар" inbox
with a Telegram alert. See `../docs/WEB_SHOP_PLAN.md` for the roadmap.

The app talks to the public shop API (`/api/v1/web/**`, `X-Tenant-ID` header required):
catalog browsing, cart price preview, checkout and order-status lookup are anonymous; the
optional SMS-OTP login unlocks the `/me/**` account features (order history, loyalty,
wishlist). Full contract: `../docs/api/MOBILE_SHOP_API.md`.

## Project layout

```
lib/
  main.dart                      app entry, theme
  config/app_config.dart         build-time config (--dart-define)
  api/catalog_api.dart           CatalogApi (abstract) + HttpCatalogApi
  models/                        PublicProduct, PublicCategory, PageResult
  screens/catalog_screen.dart    grid, search, category chips, pagination
  screens/product_detail_screen.dart  images, price, add-to-cart
  screens/cart_screen.dart       cart with quantity steppers
  screens/checkout_screen.dart   name/phone/region/village form
  screens/order_success_screen.dart   order number confirmation
  screens/order_status_screen.dart    lookup by number + phone
  screens/login_screen.dart      phone + SMS code login
  screens/my_orders_screen.dart  order history for logged-in customers
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

Covered: UZS formatting, JSON mapping of all API payloads (catalog, orders, delivery),
HTTP client behaviour (tenant header, query params, checkout POST body, error handling,
UTF-8), cart store math + persistence, and widget tests for the catalog screen
(grid render, stock badges, empty state, error + retry, search, category filter) and the
checkout form (validation, payload building, cart clearing, 429 handling, cascading
region→village selects).

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
- [ ] Add to cart from card and detail; badge count updates; cart survives app restart
- [ ] Checkout with empty name/short phone shows validation errors
- [ ] Successful checkout shows order number; order appears in admin "Онлайн буюртмалар" with Telegram ping
- [ ] Order status lookup works with the order number + phone; wrong phone is rejected
- [ ] Login: real SMS code arrives; wrong code rejected; 6th code request in a day blocked
- [ ] "Буюртмаларим" lists only own orders; survives app restart; logout clears it
- [ ] Checkout pre-fills name/phone for a logged-in customer
- [ ] Staff link a web customer to a debtor; converting their order uses the linked customer
- [ ] Region with a delivery fee: fee row + grand total shown at checkout; server total matches
- [ ] Confirming an order reserves stock (inventory module shows reserved qty); cancel releases it
- [ ] Looks correct on a small phone (360 px wide) and a tablet
