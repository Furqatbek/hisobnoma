# Hisobnoma Shop — Flutter Mobile App Design Brief

## Goal

Redesign the customer-facing mobile shop app with a **minimalist aesthetic** following **Apple Human Interface Guidelines (HIG)**. The app is built in **Flutter** (Material 3 + Cupertino where appropriate). Language is **Uzbek (Cyrillic script)**.

The app lets customers browse a product catalog, build a cart, place orders with delivery, check order status, and optionally log in via SMS OTP to see order history.

---

## Design Principles

1. **Clarity** — Content is the focus. No visual noise, decorative elements, or gratuitous color.
2. **Deference** — UI stays out of the way. Fluid motion, crisp typography, translucent materials.
3. **Depth** — Subtle layering via elevation, blur, and transitions communicates hierarchy.
4. **Minimalism** — Every element must earn its place. Generous whitespace. No redundant labels.
5. **Consistency** — Uniform spacing (8pt grid), consistent iconography (SF Symbols style), predictable navigation patterns.

---

## Color & Typography

- **Primary accent**: Teal (current seed). Consider a refined palette — one accent, one surface, neutral text.
- **Background**: Pure white (`#FFFFFF`) or near-white with subtle warm/cool tint.
- **Text**: System default (San Francisco on iOS, Roboto on Android). Three weights max: regular, medium, bold.
- **Cards**: Minimal border-radius (12–16px), no heavy shadows — use subtle 1px borders or very faint elevation.
- **Status colors**: Green (available/completed), Orange (new/pending), Blue (confirmed), Red (cancelled/out-of-stock), Amber (delivering).

---

## Navigation Architecture

```
Tab Bar (bottom, 3 tabs)
  [1] Catalog (home)
  [2] Cart (with badge count)
  [3] Profile / Account

Each tab has its own navigation stack.
```

**Current (flat)**: All screens push onto a single stack from the catalog. Redesign with a **bottom tab bar** for the 3 primary destinations.

---

## Screens

### 1. Catalog Screen (Tab 1 — Home)

**Purpose**: Browse and search products.

**Layout**:
- **Top**: Large title "Каталог" (collapsing on scroll, iOS-style)
- **Search bar**: Rounded, embedded below title. Placeholder: "Қидириш..."
- **Category filter**: Horizontal scroll of pill-shaped chips. First chip: "Барчаси" (All). Selected chip is filled accent, others are outlined/ghost.
- **Product grid**: 2 columns, edge-to-edge cards with 12px gap. Each card:
  - Product image (aspect ratio ~1:1, rounded corners, fills card width)
  - Product name (2 lines max, medium weight)
  - Price in bold (e.g., "12 000 сўм")
  - Small "Тугаган" badge overlay on image if out of stock (semi-transparent red)
  - Tap → Product Detail
- **Infinite scroll**: Loading spinner at bottom when fetching more
- **Pull-to-refresh**: Standard iOS-style refresh indicator
- **Empty state**: Centered icon + "Ҳеч нарса топилмади" (nothing found) or "Ҳозирча маҳсулотлар йўқ" (no products yet)
- **Error state**: Centered Wi-Fi-off icon + "Юклашда хатолик юз берди" + "Қайта уриниш" button

**API**: `GET /api/v1/web/catalog/products?page=0&size=20&search=...&categoryId=...`
**API**: `GET /api/v1/web/catalog/categories` (for chips)

---

### 2. Product Detail Screen

**Purpose**: View full product info, add to cart or contact seller.

**Layout**:
- **Hero image area**: Full-width, ~40% of screen height. If multiple images → horizontal PageView with dot indicator at bottom. Pinch-to-zoom optional.
- **Content area** (scrollable, overlaps image slightly with rounded top corners — sheet-style):
  - Product name (title, bold)
  - Price (large, accent color) + unit name if present (e.g., "кг")
  - Stock badge: green "Мавжуд" or red "Тугаган" — small pill
  - Category + Brand as subtle grey text or small chips
  - Divider
  - Description section (if present): "Тавсиф" label + body text
- **Bottom fixed bar** (safe-area aware):
  - "Саватга қўшиш" (Add to cart) — full-width filled button, disabled if out of stock
  - If already in cart: show "Саватда ✓" with quantity stepper instead
- **Secondary actions** (above the main button, as text buttons or subtle icons):
  - "Қўнғироқ қилиш" (Call) — only if SHOP_PHONE configured
  - "Telegram орқали буюртма" (Telegram) — only if SHOP_TELEGRAM configured

**API**: `GET /api/v1/web/catalog/products/{id}`

**Transitions**: Hero animation on the product image from grid card to detail.

---

### 3. Cart Screen (Tab 2)

**Purpose**: Review items, adjust quantities, proceed to checkout.

**Layout**:
- **Title**: "Сават" (large title style)
- **Empty state**: Centered cart icon + "Сават бўш" + "Каталогга қайтиш" button
- **Item list**: Each row:
  - Small product image (56x56, rounded)
  - Product name (medium, 2 lines max)
  - Unit price (grey, small)
  - Quantity stepper: `−  [qty]  +` (compact, inline)
  - Line total on the right (bold)
  - Swipe-to-delete (iOS-style red "Ўчириш" action)
- **Summary section** (pinned at bottom):
  - Subtotal row: "Жами" + amount
  - Checkout button: "Буюртма бериш" — full-width filled
- **Badge**: Tab icon shows item count badge

**No API calls** — cart is local (SharedPreferences).

---

### 4. Checkout Screen

**Purpose**: Collect delivery info and submit order.

**Layout**:
- **Title**: "Буюртма"
- **Form** (grouped sections with subtle headers):
  - **Section: Контакт** (Contact)
    - Name field — "Исмингиз" (pre-filled if logged in)
    - Phone field — "Телефон рақам", starts with "+998" (pre-filled if logged in)
  - **Section: Етказиб бериш** (Delivery)
    - Region dropdown — "Туман" (optional, only shown if regions exist)
    - Village dropdown — "Қишлоқ / маҳалла" (cascading, appears after region selected)
  - **Section: Қўшимча** (Additional)
    - Note field — "Изоҳ (ихтиёрий)", multiline, max 500 chars
- **Order summary** (card at bottom before button):
  - Item count summary (e.g., "3 та маҳсулот")
  - Subtotal
  - Delivery fee row (only if region has fee > 0) — "Етказиб бериш: 5 000 сўм"
  - Divider
  - Grand total: "Жами тўлов" — bold, large
- **Submit button**: "Буюртмани юбориш" — full-width, shows loading spinner while submitting
- **Validation errors**: Inline under each field, red text
- **Error toast**: "Буюртма юборилмади" or "Жуда кўп уриниш" (rate limited)

**API**: `POST /api/v1/web/orders` with body:
```json
{
  "customerName": "Ali",
  "phone": "+998901234567",
  "regionId": 1,
  "villageId": 11,
  "note": "...",
  "lines": [{ "catalogItemId": 100, "quantity": 2.5 }]
}
```
**API**: `GET /api/v1/web/delivery/regions`
**API**: `GET /api/v1/web/delivery/villages?regionId=1`

---

### 5. Order Success Screen

**Purpose**: Confirm order was placed, show order number.

**Layout** (replaces checkout, no back navigation):
- **Centered content**:
  - Large green checkmark (animated — scale + fade in)
  - "Буюртмангиз қабул қилинди!" (bold, title)
  - Order number in a copyable pill/card (e.g., `WO-000012`) — tap to copy with haptic feedback
  - Total amount
  - Subtle hint: "Тез орада сиз билан боғланамиз..."
- **Actions**:
  - "Каталогга қайтиш" — primary filled button
  - "Буюртма ҳолатини текшириш" — text button below

---

### 6. Order Status Screen

**Purpose**: Look up any order by number + phone (no login needed).

**Access**: From Profile tab or Order Success screen.

**Layout**:
- **Title**: "Буюртма ҳолати"
- **Search form**:
  - Order number field — "Буюртма рақами"
  - Phone field — "Телефон рақам"
  - "Излаш" button
- **Result card** (appears below form on success):
  - Order number + status badge (colored pill)
  - Divider
  - Line items: product name, qty, unit price, line total
  - Delivery fee row (if > 0)
  - Total (bold)
- **Not found state**: Red text "Буюртма топилмади. Рақам ва телефонни текширинг."

**Status badge colors**:
| Status | Label | Color |
|--------|-------|-------|
| NEW | Янги | Orange |
| CONFIRMED | Тасдиқланган | Blue |
| DELIVERING | Етказилмоқда | Amber |
| COMPLETED | Бажарилган | Green |
| CANCELLED | Бекор қилинган | Red |

**API**: `GET /api/v1/web/orders/{orderNumber}?phone=...`

---

### 7. Profile / Account Screen (Tab 3)

**Purpose**: Login, view order history, check single order status.

**Layout (not logged in)**:
- Person circle icon (large, grey)
- "Кириш" button (primary)
- Divider
- "Буюртма ҳолатини текшириш" link → Order Status Screen

**Layout (logged in)**:
- User info header: name (if set) + phone
- **Order history list** (scrollable):
  - Each order as a card:
    - Order number + status badge
    - Line items summary (product names, quantities)
    - Total amount
    - Tap → expanded detail or Order Status Screen
  - Pull-to-refresh
  - Empty: "Ҳозирча буюртмалар йўқ"
- "Чиқиш" (Logout) — text button at bottom, destructive red

**API**: `GET /api/v1/web/me/orders?page=0&size=20` (requires Bearer token)

---

### 8. Login Screen (pushed from Profile tab)

**Purpose**: SMS OTP authentication.

**Layout — Stage 1 (Phone entry)**:
- Title: "Кириш"
- Subtitle: "Телефон рақамингизга SMS код юборамиз"
- Phone field with "+998" prefix, numeric keyboard
- "Код юбориш" button (full width)

**Layout — Stage 2 (Code entry)**:
- Phone shown as static text (with "Рақамни ўзгартириш" text button)
- 6-digit code field — large, spaced, auto-focus (consider individual digit boxes like iOS verification)
- Name field (optional): "Исмингиз (ихтиёрий)"
- "Тасдиқлаш" (Confirm) button
- "Қайта юбориш" (Resend) text button with cooldown timer

**Error states**: Inline — "Телефон рақам нотўғри", "Код нотўғри ёки муддати ўтган", "Жуда кўп уриниш"

**API**: `POST /api/v1/web/auth/request-otp` body: `{ "phone": "..." }`
**API**: `POST /api/v1/web/auth/verify` body: `{ "phone": "...", "code": "...", "name": "..." }`

**Response**: `{ "token": "...", "phone": "...", "name": "..." }`

---

## Data Models (for mockup content)

### Product
```
id: 1
name: "Coca-Cola 1.5л"
shortDescription: "Газли ичимлик"
description: "Coca-Cola классик таъми, 1.5 литрлик шиша"
price: 12000
currency: "UZS"
categoryName: "Ичимликлар"
brandName: "Coca-Cola"
unitName: "дона"
inStock: true
imageUrl: "/uploads/products/1/main.jpg"
images: ["/uploads/products/1/main.jpg", "/uploads/products/1/side.jpg"]
```

### Category
```
id: 1, name: "Ичимликлар"
id: 2, name: "Озиқ-овқат"
id: 3, name: "Маиший кимё"
```

### Delivery Region
```
id: 1, name: "Тошкент шаҳар", deliveryFee: 0
id: 2, name: "Тошкент вилояти", deliveryFee: 15000
```

### Delivery Village
```
id: 11, name: "Чилонзор", regionId: 1
id: 12, name: "Юнусобод", regionId: 1
```

### Order
```
orderNumber: "WO-000012"
status: "CONFIRMED"
deliveryFee: 15000
totalAmount: 51000
currency: "UZS"
lines: [
  { productName: "Coca-Cola 1.5л", quantity: 3, unitPrice: 12000, lineTotal: 36000 }
]
```

---

## Currency Formatting

All prices formatted as: `12 000 сўм` (space-separated thousands + " сўм" suffix).
Null/missing amounts show em-dash: `—`

---

## Key Interactions & Animations

1. **Product card → detail**: Hero animation on product image
2. **Add to cart**: Brief scale animation on cart badge + haptic
3. **Order success checkmark**: Scale-up + fade-in animation
4. **Category chip selection**: Smooth color transition
5. **Swipe to delete** (cart): Standard iOS slide-to-reveal
6. **Pull-to-refresh**: Native platform feel
7. **Bottom sheet** transitions for modals (if any)
8. **Skeleton loading**: Show content placeholders (shimmer) instead of spinner for initial catalog load

---

## Spacing & Layout Rules

- **Grid**: 8pt base grid
- **Screen padding**: 16px horizontal
- **Card gap**: 12px
- **Section spacing**: 24px between form sections
- **Button height**: 48–52px
- **Icon size**: 24px (navigation), 20px (inline)
- **Border radius**: 12px (cards), 24px (buttons/chips), 8px (inputs)
- **Bottom safe area**: Always respected for buttons and tab bar

---

## Accessibility

- Minimum tap target: 44x44px
- Text contrast: WCAG AA minimum (4.5:1 body, 3:1 large text)
- All images: meaningful alt text
- Form fields: proper labels and error associations
- Status badges: don't rely on color alone — include text label

---

## What NOT to Include

- No onboarding / splash walkthrough
- No social login (only SMS OTP)
- No payment integration (cash on delivery only)
- No push notifications (not yet)
- No dark mode (defer to later)
- No animations that delay user actions
- No bottom sheets for simple navigation (use full screens)
