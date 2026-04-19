# Section 5a: Mobile Module — Test Plan

---

## 1. MobileAuthService Unit Tests

Framework: JUnit 5 + Mockito.

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `mobileLogin_validCredentials_returnsTokenPair` | `mobileLogin(request)` | Valid username + password | Returns `MobileAuthResponse` with accessToken + refreshToken |
| `mobileLogin_invalidPassword_throwsAuthException` | `mobileLogin(request)` | Wrong password | Throws `AuthenticationException` |
| `mobileLogin_userNotFound_throwsAuthException` | `mobileLogin(request)` | Username missing | Throws `AuthenticationException` |
| `mobileLogin_inactiveUser_throwsAuthException` | `mobileLogin(request)` | User INACTIVE | Throws `AuthenticationException` "Account disabled" |
| `mobileRefreshToken_valid_returnsNewAccessToken` | `mobileRefreshToken(refreshToken)` | Valid non-expired refresh token | Returns new accessToken |
| `mobileRefreshToken_expired_throwsAuthException` | `mobileRefreshToken(refreshToken)` | Expired refresh token | Throws `AuthenticationException` "Token expired" |
| `mobileRefreshToken_invalid_throwsAuthException` | `mobileRefreshToken(refreshToken)` | Tampered token | Throws `AuthenticationException` |
| `mobileLogout_success_invalidatesToken` | `mobileLogout(refreshToken)` | Valid token | Token blacklisted; future refresh fails |

---

## 2. MobileProductService Unit Tests

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getProducts_returnsPaged` | `getProducts(tenantId, pageable)` | Multiple active products | Returns paged `MobileProductDto` |
| `getProduct_found_returnsDto` | `getProduct(tenantId, id)` | Product exists | Returns dto with price, stock info |
| `getProduct_notFound_throwsNotFoundException` | `getProduct(tenantId, id)` | Missing | Throws `NotFoundException` |
| `searchProducts_byName_returnsMatches` | `searchProducts(tenantId, query, pageable)` | 3 products match | Returns 3 |
| `searchProducts_byBarcode_returnsMatch` | `searchProducts(tenantId, barcode, pageable)` | Exact barcode | Returns 1 |
| `getProductsByCategory_returnsFiltered` | `getProductsByCategory(tenantId, catId, pageable)` | 5 in category | Returns 5 |
| `getFeaturedProducts_returnsMarkedProducts` | `getFeaturedProducts(tenantId)` | 3 marked featured | Returns 3 |
| `getProductStock_returnsAvailableQty` | `getProductStock(tenantId, productId, locationId)` | Stock exists | Returns available quantity |
| `scanBarcode_returnsProductDto` | `scanBarcode(tenantId, barcode)` | Valid barcode | Returns `MobileProductDto` |
| `scanBarcode_notFound_throwsNotFoundException` | `scanBarcode(tenantId, barcode)` | Unknown barcode | Throws `NotFoundException` |

---

## 3. MobileCartService Unit Tests

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getCart_returnsCurrentCart` | `getCart(tenantId, userId)` | Cart exists in session/cache | Returns `MobileCartDto` with lines |
| `getCart_empty_returnsEmptyCart` | `getCart(tenantId, userId)` | No cart | Returns empty cart dto |
| `addItem_newProduct_lineCreated` | `addItem(tenantId, userId, request)` | Product not yet in cart | New line added; qty = requested |
| `addItem_existingProduct_qtyIncremented` | `addItem(tenantId, userId, request)` | Product already in cart | Qty incremented; no duplicate line |
| `addItem_insufficientStock_throwsBusinessException` | `addItem(tenantId, userId, request)` | Available qty < requested | Throws `BusinessException` "Insufficient stock" |
| `addItem_productNotFound_throwsNotFoundException` | `addItem(tenantId, userId, request)` | Product missing | Throws `NotFoundException` |
| `updateItemQty_success` | `updateItemQty(tenantId, userId, lineId, qty)` | Valid qty | Line updated; totals recalculated |
| `updateItemQty_zero_removesLine` | `updateItemQty(tenantId, userId, lineId, 0)` | qty = 0 | Line removed from cart |
| `removeItem_success` | `removeItem(tenantId, userId, lineId)` | Line exists | Line removed |
| `removeItem_notFound_throwsNotFoundException` | `removeItem(tenantId, userId, lineId)` | Line missing | Throws `NotFoundException` |
| `clearCart_success` | `clearCart(tenantId, userId)` | Cart has items | All lines removed |
| `applyCartCoupon_valid_discountApplied` | `applyCartCoupon(tenantId, userId, code)` | Valid coupon | Discount reflected in cart total |
| `applyCartCoupon_invalid_throwsBusinessException` | `applyCartCoupon(tenantId, userId, code)` | Expired coupon | Throws `BusinessException` |
| `checkoutCart_success_createsTransaction` | `checkoutCart(tenantId, userId, request)` | Sufficient payment | Creates `POSTransaction`; cart cleared |
| `checkoutCart_underpayment_throwsBusinessException` | `checkoutCart(tenantId, userId, request)` | Payment < total | Throws `BusinessException` |

---

## 4. MobileSyncService Unit Tests

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `syncProducts_returnsChangedSince` | `syncProducts(tenantId, lastSync)` | 5 products changed after lastSync | Returns list of 5 |
| `syncProducts_nothingChanged_returnsEmpty` | `syncProducts(tenantId, lastSync)` | No changes since lastSync | Returns empty list |
| `syncCategories_returnsChangedSince` | `syncCategories(tenantId, lastSync)` | 2 categories updated | Returns 2 |
| `syncPriceLists_returnsActiveLists` | `syncPriceLists(tenantId, lastSync)` | Changed price lists | Returns updated lists |
| `getSyncManifest_returnsAllCounts` | `getSyncManifest(tenantId)` | Products, categories, prices | Returns counts per resource type |

---

## 5. MobileReceivingService Unit Tests

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getPendingReceivingOrders_returnsList` | `getPendingReceivingOrders(tenantId)` | 3 pending | Returns list of 3 |
| `scanReceivingItem_validBarcode_recordsQty` | `scanReceivingItem(tenantId, orderId, barcode, qty)` | Barcode matches a PO line | Qty recorded; line updated |
| `scanReceivingItem_barcodeNotInOrder_throwsBusinessException` | `scanReceivingItem(tenantId, orderId, barcode, qty)` | Barcode not on PO | Throws `BusinessException` |
| `completeReceiving_success` | `completeReceiving(tenantId, orderId)` | All lines scanned | Status COMPLETED; stock posted |

---

## 6. Repository Tests (`@DataJpaTest`)

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `MobileSessionRepository_findByRefreshToken` | `findByRefreshToken(token)` | Token exists | Returns Optional with session |
| `MobileSessionRepository_findByUserId` | `findByUserId(userId)` | Active session | Returns active sessions |
| `MobileCartRepository_findByUserIdAndTenantId` | `findByUserAndTenant(userId, tenantId)` | Cart exists | Returns Optional |

---

## 7. Integration Tests — Mobile API

Base path: `/api/v1/mobile`

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|---|---|---|---|
| `mobileLogin_returns200_withTokenPair` | `POST /api/v1/mobile/auth/login` | No auth; valid credentials | `200 OK`; accessToken + refreshToken |
| `mobileLogin_returns401_badCredentials` | `POST /api/v1/mobile/auth/login` | No auth; wrong password | `401 Unauthorized` |
| `mobileRefreshToken_returns200_newToken` | `POST /api/v1/mobile/auth/refresh` | Valid refresh token | `200 OK`; new accessToken |
| `mobileRefreshToken_returns401_expired` | `POST /api/v1/mobile/auth/refresh` | Expired token | `401 Unauthorized` |
| `mobileLogout_returns200` | `POST /api/v1/mobile/auth/logout` | Bearer mobile token | `200 OK` |
| `getProducts_returns200` | `GET /api/v1/mobile/products` | Bearer mobile token | `200 OK`; paged products |
| `getProducts_returns401_noToken` | `GET /api/v1/mobile/products` | No token | `401 Unauthorized` |
| `searchProducts_returns200` | `GET /api/v1/mobile/products/search?q=phone` | Bearer mobile token | `200 OK`; matching |
| `scanBarcode_returns200_productFound` | `GET /api/v1/mobile/products/barcode/{code}` | Bearer mobile token; valid | `200 OK`; product dto |
| `scanBarcode_returns404_notFound` | `GET /api/v1/mobile/products/barcode/{code}` | Bearer mobile token; unknown | `404 Not Found` |
| `getCart_returns200` | `GET /api/v1/mobile/cart` | Bearer mobile token | `200 OK`; cart dto |
| `addCartItem_returns200` | `POST /api/v1/mobile/cart/items` | Bearer mobile token; valid | `200 OK`; updated cart |
| `addCartItem_returns422_insufficientStock` | `POST /api/v1/mobile/cart/items` | Bearer mobile token; low stock | `422 Unprocessable Entity` |
| `updateCartItemQty_returns200` | `PUT /api/v1/mobile/cart/items/{lineId}` | Bearer mobile token | `200 OK`; updated line |
| `removeCartItem_returns200` | `DELETE /api/v1/mobile/cart/items/{lineId}` | Bearer mobile token | `200 OK` |
| `checkoutCart_returns201_transactionCreated` | `POST /api/v1/mobile/cart/checkout` | Bearer mobile token; valid | `201 Created`; transaction dto |
| `checkoutCart_returns422_underpayment` | `POST /api/v1/mobile/cart/checkout` | Bearer mobile token; low payment | `422 Unprocessable Entity` |
| `syncProducts_returns200` | `GET /api/v1/mobile/sync/products?lastSync=...` | Bearer mobile token | `200 OK`; changed products |
| `getPendingReceiving_returns200` | `GET /api/v1/mobile/receiving` | Bearer mobile token | `200 OK`; pending orders |
| `completeReceiving_returns200` | `POST /api/v1/mobile/receiving/{id}/complete` | Bearer mobile token | `200 OK` |
