# Section 6a-i: Frontend — Stores, Services, Router & Auth Views — Test Plan

**Platform:** Hisobnoma SaaS  
**Scope:** Pinia stores (`auth.js`, `receipt.js`), services (`tokenStorage.js`, `api.js`), Vue Router guards, and Auth Views  
**Goal:** 100% test coverage  
**Stack:** Vitest + @vue/test-utils + MSW (Mock Service Worker) + Pinia test helpers

---

## 1. Pinia Store: `auth.js`

**State shape:** `user`, `accessToken`, `refreshToken`, `permissions`, `roles`

### 1.1 Action: `login(credentials)`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| login — success sets tokens in state | `login({ username, password })` | Valid credentials, API returns 200 with `accessToken` and `refreshToken` | `state.accessToken` and `state.refreshToken` equal the returned token values; `state.user` populated |
| login — success writes tokens to localStorage | `login({ username, password })` | Valid credentials, API returns 200 | `localStorage.getItem('accessToken')` and `localStorage.getItem('refreshToken')` equal the returned token values |
| login — invalid password sets error | `login({ username, password })` | API returns 401 | `state.accessToken` is `null`; `state.refreshToken` is `null`; `state.error` contains a non-empty error message |
| login — locked account sets error | `login({ username, password })` | API returns 403 | `state.error` equals `"Account locked"`; tokens remain `null` |
| login — network error sets error | `login({ username, password })` | Network request fails (no response) | `state.error` equals `"Network error"`; tokens remain `null` |

### 1.2 Action: `logout()`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| logout — clears user from state | `logout()` | Called after a successful login | `state.user` is `null` |
| logout — clears accessToken from state | `logout()` | Called after a successful login | `state.accessToken` is `null` |
| logout — clears refreshToken from state | `logout()` | Called after a successful login | `state.refreshToken` is `null` |
| logout — removes accessToken from localStorage | `logout()` | Tokens previously written to localStorage | `localStorage.getItem('accessToken')` is `null` |
| logout — removes refreshToken from localStorage | `logout()` | Tokens previously written to localStorage | `localStorage.getItem('refreshToken')` is `null` |
| logout — resets permissions to empty array | `logout()` | State had populated permissions array | `state.permissions` deep-equals `[]` |

### 1.3 Action: `refresh()`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| refresh — valid token updates accessToken in state | `refresh()` | API returns 200 with new `accessToken` | `state.accessToken` equals the new token value |
| refresh — valid token writes new accessToken to localStorage | `refresh()` | API returns 200 with new `accessToken` | `localStorage.getItem('accessToken')` equals the new token value |
| refresh — expired token calls logout and clears state | `refresh()` | API returns 401 | `logout()` action invoked; `state.accessToken` is `null`; `state.refreshToken` is `null` |
| refresh — network error calls logout | `refresh()` | Network request fails | `logout()` action invoked; state cleared |

### 1.4 Action: `changePassword(data)`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| changePassword — correct password resolves without state change | `changePassword({ current, newPassword })` | API returns 200 | Promise resolves; no change to `accessToken`, `user`, or `permissions` in state |
| changePassword — wrong password propagates error | `changePassword({ current, newPassword })` | API returns 400 | Promise rejects (or throws); error propagated to caller; state unchanged |

### 1.5 Action: `forgotPassword(email)`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| forgotPassword — existing email returns success flag | `forgotPassword('user@example.com')` | API returns 200 | Resolves with a truthy success indicator; no state mutation |
| forgotPassword — non-existent email also returns success flag | `forgotPassword('nobody@example.com')` | API returns 200 (security: no user enumeration) | Resolves with a truthy success indicator; no state mutation; no error set in state |

### 1.6 Action: `resetPassword(data)`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| resetPassword — valid token and matching passwords succeeds | `resetPassword({ token, password, confirm })` | `password === confirm`, API returns 200 | Resolves with success flag; no error in state |
| resetPassword — expired token propagates error | `resetPassword({ token, password, confirm })` | API returns 400 | Rejects or sets `state.error`; error communicated to caller |
| resetPassword — password mismatch skips API call | `resetPassword({ token, password, confirm })` | `password !== confirm` | Validation error thrown/returned before any HTTP request is made; API endpoint never called |

### 1.7 Getters

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| isAuthenticated — true when accessToken present | `isAuthenticated` getter | `state.accessToken` is a non-null string | Returns `true` |
| isAuthenticated — false when accessToken is null | `isAuthenticated` getter | `state.accessToken` is `null` | Returns `false` |
| currentUser — returns user object | `currentUser` getter | `state.user` is populated with a user object | Returns the exact `state.user` object |
| permissions — returns permissions array | `permissions` getter | `state.permissions` is `['INVENTORY_READ', 'POS_READ']` | Returns `['INVENTORY_READ', 'POS_READ']` |
| roles — returns roles array | `roles` getter | `state.roles` is `['cashier']` | Returns `['cashier']` |
| hasPermission — true when permission in list | `hasPermission('INVENTORY_READ')` | `state.permissions` includes `'INVENTORY_READ'` | Returns `true` |
| hasPermission — false when permission not in list | `hasPermission('ADMIN')` | `state.permissions` does not include `'ADMIN'` | Returns `false` |

### 1.8 Token Persistence

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| token persistence — both tokens written after login | `login(credentials)` | Successful login | `localStorage.getItem('accessToken')` and `localStorage.getItem('refreshToken')` are both set |
| token persistence — loadTokens restores state from localStorage | `loadTokens()` (or app-init hook) | Tokens present in localStorage before store initialises | `state.accessToken` and `state.refreshToken` equal the localStorage values |
| token persistence — localStorage keys cleared after logout | `logout()` | Tokens exist in localStorage and state | Both localStorage keys return `null` after logout |

---

## 2. Pinia Store: `receipt.js`

**State shape:** `currentReceipt`, `items` (array), `totals` (`subtotal`, `discount`, `tax`, `total`)

### 2.1 Action: `addItem(product, qty)`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| addItem — new product appended to items | `addItem(product, 2)` | Product not already in `state.items` | `state.items` has length 1; item contains `productId`, `qty: 2`, `lineTotal: price × 2` |
| addItem — lineTotal calculated correctly for new item | `addItem({ id: 1, price: 10.00 }, 3)` | New product with qty 3 | `state.items[0].lineTotal === 30.00` |
| addItem — existing product increments qty | `addItem(product, 2)` called twice | Same product added twice | `state.items` still has length 1; `qty === 4` |
| addItem — existing product recalculates lineTotal | `addItem({ id: 1, price: 5.00 }, 2)` then `addItem({ id: 1, price: 5.00 }, 3)` | Same product added twice | `state.items[0].lineTotal === 25.00` |

### 2.2 Action: `removeItem(productId)`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| removeItem — existing product removed from array | `removeItem(productId)` | Product with `productId` is in `state.items` | `state.items` no longer contains an item with that `productId`; array length decremented |
| removeItem — non-existent productId is a no-op | `removeItem(9999)` | No item with `productId 9999` in `state.items` | `state.items` unchanged; no error thrown |

### 2.3 Action: `calculateTotals()`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| calculateTotals — subtotal equals sum of all lineTotals | `calculateTotals()` | Items with lineTotals `[100, 50, 25]` | `state.totals.subtotal === 175.00` |
| calculateTotals — discount applied at 10% | `calculateTotals()` | Subtotal = 100, discount rate = 10% | `state.totals.discount === 10.00` |
| calculateTotals — tax applied at 15% on discounted subtotal | `calculateTotals()` | Subtotal = 100, discount = 10 (net = 90), tax rate = 15% | `state.totals.tax === 13.50` |
| calculateTotals — total equals subtotal minus discount plus tax | `calculateTotals()` | Subtotal = 100, discount = 10, tax = 13.50 | `state.totals.total === 103.50` |
| calculateTotals — zero items yields all-zero totals | `calculateTotals()` | `state.items` is empty | All four totals equal `0` |

### 2.4 Action: `clearReceipt()`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| clearReceipt — items reset to empty array | `clearReceipt()` | Items array had entries | `state.items` deep-equals `[]` |
| clearReceipt — subtotal reset to zero | `clearReceipt()` | `state.totals.subtotal` was non-zero | `state.totals.subtotal === 0` |
| clearReceipt — discount reset to zero | `clearReceipt()` | `state.totals.discount` was non-zero | `state.totals.discount === 0` |
| clearReceipt — tax reset to zero | `clearReceipt()` | `state.totals.tax` was non-zero | `state.totals.tax === 0` |
| clearReceipt — total reset to zero | `clearReceipt()` | `state.totals.total` was non-zero | `state.totals.total === 0` |

---

## 3. Service: `tokenStorage.js`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| getAccessToken — returns stored value | `getAccessToken()` | `localStorage` contains the access token key with value `'tok-abc'` | Returns `'tok-abc'` |
| getAccessToken — returns null when not set | `getAccessToken()` | `localStorage` does not contain the access token key | Returns `null` |
| getRefreshToken — returns stored value | `getRefreshToken()` | `localStorage` contains the refresh token key with value `'ref-xyz'` | Returns `'ref-xyz'` |
| getRefreshToken — returns null when not set | `getRefreshToken()` | `localStorage` does not contain the refresh token key | Returns `null` |
| setTokens — writes access token to localStorage | `setTokens('abc', 'xyz')` | Clean localStorage | `localStorage.getItem(ACCESS_KEY) === 'abc'` |
| setTokens — writes refresh token to localStorage | `setTokens('abc', 'xyz')` | Clean localStorage | `localStorage.getItem(REFRESH_KEY) === 'xyz'` |
| clearTokens — removes access token key | `clearTokens()` | Both keys previously set | `localStorage.getItem(ACCESS_KEY)` is `null` |
| clearTokens — removes refresh token key | `clearTokens()` | Both keys previously set | `localStorage.getItem(REFRESH_KEY)` is `null` |
| round trip — getAccessToken returns value set by setTokens | `setTokens('abc', 'xyz')` then `getAccessToken()` | Clean localStorage | Returns `'abc'` |
| round trip — getRefreshToken returns value set by setTokens | `setTokens('abc', 'xyz')` then `getRefreshToken()` | Clean localStorage | Returns `'xyz'` |

---

## 4. Service: `api.js` — Auth API Methods (via MSW)

> Each method is tested with three MSW handler scenarios unless noted: **success**, **401 response**, and **500 response**.

### 4.1 `login(credentials)` — POST `/api/v1/auth/login`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| login API — success returns token and user payload | `login({ username, password })` | MSW returns 200 `{ accessToken, refreshToken, user }` | Resolved value matches `{ accessToken, refreshToken, user }` |
| login API — 401 causes axios to throw | `login({ username, password })` | MSW returns 401 | Promise rejects; error has response status `401` |
| login API — 500 causes axios to throw | `login({ username, password })` | MSW returns 500 | Promise rejects; error has response status `500` |

### 4.2 `pinLogin(request)` — POST `/api/v1/auth/pin-login`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| pinLogin API — success returns token and user payload | `pinLogin({ phone, pin })` | MSW returns 200 `{ accessToken, refreshToken, user }` | Resolved value matches `{ accessToken, refreshToken, user }` |
| pinLogin API — 401 causes axios to throw | `pinLogin({ phone, pin })` | MSW returns 401 | Promise rejects; error has response status `401` |
| pinLogin API — 500 causes axios to throw | `pinLogin({ phone, pin })` | MSW returns 500 | Promise rejects; error has response status `500` |

### 4.3 `getUsersList()` — GET `/api/v1/auth/users/list`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| getUsersList API — success returns array of users | `getUsersList()` | MSW returns 200 with array of `{ id, username, displayName }` | Resolved value is an array matching the mocked user objects |
| getUsersList API — 401 causes axios to throw | `getUsersList()` | MSW returns 401 | Promise rejects; error has response status `401` |
| getUsersList API — 500 causes axios to throw | `getUsersList()` | MSW returns 500 | Promise rejects; error has response status `500` |

### 4.4 `setPin(data)` — PUT `/api/v1/auth/set-pin`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| setPin API — success resolves with 200 | `setPin({ pin })` | MSW returns 200 OK | Promise resolves; response status is `200` |
| setPin API — 401 causes axios to throw | `setPin({ pin })` | MSW returns 401 | Promise rejects; error has response status `401` |
| setPin API — 500 causes axios to throw | `setPin({ pin })` | MSW returns 500 | Promise rejects; error has response status `500` |

### 4.5 `logout()` — POST `/api/v1/auth/logout`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| logout API — success resolves with 200 | `logout()` | MSW returns 200 OK | Promise resolves; response status is `200` |
| logout API — 401 causes axios to throw | `logout()` | MSW returns 401 | Promise rejects; error has response status `401` |
| logout API — 500 causes axios to throw | `logout()` | MSW returns 500 | Promise rejects; error has response status `500` |

### 4.6 `refresh(token)` — POST `/api/v1/auth/refresh`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| refresh API — success returns new tokens | `refresh('ref-token')` | MSW returns 200 `{ accessToken, refreshToken }` | Resolved value contains new `accessToken` and `refreshToken` |
| refresh API — 401 causes axios to throw | `refresh('expired-token')` | MSW returns 401 | Promise rejects; error has response status `401` |
| refresh API — 500 causes axios to throw | `refresh('ref-token')` | MSW returns 500 | Promise rejects; error has response status `500` |

### 4.7 `me()` — GET `/api/v1/auth/me`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| me API — success returns user object | `me()` | MSW returns 200 with user object | Resolved value matches the mocked user object |
| me API — 401 causes axios to throw | `me()` | MSW returns 401 | Promise rejects; error has response status `401` |
| me API — 500 causes axios to throw | `me()` | MSW returns 500 | Promise rejects; error has response status `500` |

### 4.8 `changePassword(data)` — PUT `/api/v1/auth/change-password`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| changePassword API — success resolves with 200 | `changePassword({ current, newPassword })` | MSW returns 200 OK | Promise resolves; response status is `200` |
| changePassword API — 401 causes axios to throw | `changePassword({ current, newPassword })` | MSW returns 401 | Promise rejects; error has response status `401` |
| changePassword API — 500 causes axios to throw | `changePassword({ current, newPassword })` | MSW returns 500 | Promise rejects; error has response status `500` |

### 4.9 `forgotPassword(data)` — POST `/api/v1/auth/forgot-password`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| forgotPassword API — success resolves with 200 | `forgotPassword({ email })` | MSW returns 200 OK | Promise resolves; response status is `200` |
| forgotPassword API — 401 causes axios to throw | `forgotPassword({ email })` | MSW returns 401 | Promise rejects; error has response status `401` |
| forgotPassword API — 500 causes axios to throw | `forgotPassword({ email })` | MSW returns 500 | Promise rejects; error has response status `500` |

### 4.10 `resetPassword(data)` — POST `/api/v1/auth/reset-password`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| resetPassword API — success resolves with 200 | `resetPassword({ token, password })` | MSW returns 200 OK | Promise resolves; response status is `200` |
| resetPassword API — 401 causes axios to throw | `resetPassword({ token, password })` | MSW returns 401 | Promise rejects; error has response status `401` |
| resetPassword API — 500 causes axios to throw | `resetPassword({ token, password })` | MSW returns 500 | Promise rejects; error has response status `500` |

---

## 5. Router Tests

| Test Name | Route | Scenario | Expected |
|---|---|---|---|
| guard — unauthenticated redirected from /dashboard | `/dashboard` | `auth.isAuthenticated === false` | Navigation redirected to `/login` |
| guard — unauthenticated redirected from /pos | `/pos` | `auth.isAuthenticated === false` | Navigation redirected to `/login` |
| guard — authenticated user redirected from /login | `/login` | `auth.isAuthenticated === true` | Navigation redirected to `/dashboard` |
| guard — INVENTORY_READ grants access to /inventory/products | `/inventory/products` | User authenticated, `permissions` includes `'INVENTORY_READ'` | Navigation resolves; component rendered (no redirect) |
| guard — missing INVENTORY_READ blocks /inventory/products | `/inventory/products` | User authenticated, `permissions` does NOT include `'INVENTORY_READ'` | Navigation redirected to `/no-access` |
| guard — POS_READ grants access to /pos | `/pos` | User authenticated, `permissions` includes `'POS_READ'` | Navigation resolves; component rendered (no redirect) |
| guard — missing POS_READ blocks /pos | `/pos` | User authenticated, `permissions` does NOT include `'POS_READ'` | Navigation redirected to `/no-access` |
| guard — ADMIN grants access to /admin/users | `/admin/users` | User authenticated, `roles` or `permissions` includes `'ADMIN'` | Navigation resolves; component rendered (no redirect) |
| guard — missing ADMIN blocks /admin/users | `/admin/users` | User authenticated, no `'ADMIN'` role/permission | Navigation redirected to `/no-access` |
| router — unknown path renders NotFoundView | `/completely-unknown-path` | No matching route defined | `NotFoundView` component is rendered |
| router — route change updates document.title | Any route with `meta.title` set | Navigation between two routes with different `meta.title` values | `document.title` updated to match the new route's `meta.title` after navigation |

---

## 6. Auth Views

### 6.1 `LoginView.vue`

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| LoginView — renders username input | `mount()` | Default render | DOM contains an `<input>` with type `text` (or relevant selector) for username |
| LoginView — renders password input | `mount()` | Default render | DOM contains an `<input>` with `type="password"` |
| LoginView — renders Sign In button | `mount()` | Default render | DOM contains a button with text `"Sign In"` |
| LoginView — valid submit calls auth.login | `trigger('submit')` on form | Username and password fields populated with valid values | `auth.login` called once with `{ username, password }` |
| LoginView — valid submit navigates to /dashboard | `trigger('submit')` on form | `auth.login()` resolves successfully | `router.push` called with `'/dashboard'` |
| LoginView — empty username shows required error | `trigger('submit')` on form | Username field is empty | Error text `"Username is required"` appears in DOM; `auth.login` not called |
| LoginView — empty password shows required error | `trigger('submit')` on form | Password field is empty | Error text `"Password is required"` appears in DOM; `auth.login` not called |
| LoginView — failed login shows error under form | `trigger('submit')` on form | `auth.login()` throws (wrong credentials) | Error message from the thrown error rendered beneath the form; user remains on `/login` |
| LoginView — loading spinner visible during API call | `trigger('submit')` on form | `auth.login()` is pending (not yet resolved) | Loading spinner element present in DOM while promise is in flight |
| LoginView — loading spinner hidden after API resolves | `trigger('submit')` on form | `auth.login()` resolves or rejects | Loading spinner no longer visible in DOM |
| LoginView — PIN toggle button rendered | `mount()` | Default render | DOM contains a button with text matching `"Sign in with PIN"` (or equivalent) |
| LoginView — PIN toggle replaces username with phone + PIN | `trigger('click')` on PIN toggle | Default credential mode active | Username input replaced by phone input and PIN input; password input no longer present |
| LoginView — PIN login submit calls auth.pinLogin | `trigger('submit')` on form | PIN mode active; phone and PIN fields populated | `auth.pinLogin` called once with the phone and PIN values |

### 6.2 `ForgotPasswordView.vue`

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| ForgotPasswordView — renders email input | `mount()` | Default render | DOM contains an `<input>` of type `email` (or text) for email address |
| ForgotPasswordView — renders Send Reset Link button | `mount()` | Default render | DOM contains a button with text `"Send Reset Link"` |
| ForgotPasswordView — renders Back to Login link | `mount()` | Default render | DOM contains a link or router-link pointing to `/login` |
| ForgotPasswordView — valid email submit calls API | `trigger('submit')` on form | Email field contains a valid email address | `api.forgotPassword` (or equivalent store action) called once with the email value |
| ForgotPasswordView — valid email shows success message | `trigger('submit')` on form | API resolves with 200 | DOM contains text `"Check your email for reset instructions"` |
| ForgotPasswordView — invalid email format shows error | `trigger('submit')` on form | Email field contains `"not-an-email"` | Error text `"Invalid email format"` appears in DOM; API not called |
| ForgotPasswordView — empty email shows required error | `trigger('submit')` on form | Email field is empty | Error text `"Email is required"` appears in DOM; API not called |
| ForgotPasswordView — Back to Login link navigates to /login | `trigger('click')` on Back to Login link | Default render | Router navigates to `/login` |

### 6.3 `ResetPasswordView.vue`

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| ResetPasswordView — renders new password input | `mount()` | Default render | DOM contains a `type="password"` input for new password |
| ResetPasswordView — renders confirm password input | `mount()` | Default render | DOM contains a second `type="password"` input for password confirmation |
| ResetPasswordView — renders submit button | `mount()` | Default render | DOM contains a submit button |
| ResetPasswordView — token extracted from URL query params on mount | `mount()` with route query `?token=abc123` | Component mounted with query param present | Component internally holds `token === 'abc123'`; no error state shown on initial render |
| ResetPasswordView — matching passwords with valid token calls API | `trigger('submit')` on form | Both password fields match; token present in URL | `api.resetPassword` called once with the token and new password |
| ResetPasswordView — matching passwords success navigates to /login | `trigger('submit')` on form | API resolves with 200 | Router navigates to `/login` after successful reset |
| ResetPasswordView — mismatched passwords shows error, skips API | `trigger('submit')` on form | New password and confirm password fields contain different values | Error text `"Passwords do not match"` appears in DOM; API not called |
| ResetPasswordView — password shorter than 8 chars shows error | `trigger('submit')` on form | New password field contains fewer than 8 characters | Validation error rendered in DOM; API not called |
| ResetPasswordView — API 400 shows expired token error | `trigger('submit')` on form | Passwords match; API returns 400 | Error text `"Reset link has expired"` appears in DOM; user remains on current view |
