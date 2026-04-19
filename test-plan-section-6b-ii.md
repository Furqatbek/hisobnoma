# Section 6b-ii: Frontend — POS Views — Test Plan

**Stack:** Vitest + @vue/test-utils + MSW (Mock Service Worker)
**Coverage target:** 100% component test coverage for all POS-related Vue.js views
**Conventions used in this section:**
- "mount" = `mount()` via `@vue/test-utils` with a Pinia store and Vue Router stub
- MSW handlers intercept all API calls at the network layer; each test seeds its own handler overrides via `server.use(...)`
- "trigger" = `wrapper.find(...).trigger('...')` + `await nextTick()` unless stated otherwise
- Debounce tests use `vi.useFakeTimers()` + `vi.advanceTimersByTime(n)` to settle the timer synchronously; `vi.useRealTimers()` restored in `afterEach`
- Dialog/confirm tests stub `window.confirm` via `vi.spyOn(window, 'confirm')` or interact with a custom modal's `[data-testid="confirm-btn"]`
- Each table row represents one `it()` block

---

## POSView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `mount with no open shift shows undismissable Open Shift modal` | `onMounted` / `getCurrentShift` | MSW returns 404 or empty on `GET /shifts/current`; no open shift exists | Open Shift modal element is present and visible in DOM; backdrop has no close handler; POS interface controls (product search, cart, payment panel) are absent or disabled |
| `Open Shift modal cannot be dismissed without submitting` | `keydown Escape` + click outside modal | Open Shift modal is displayed | Pressing Escape and clicking the modal backdrop do not close the modal; modal remains visible |
| `Open Shift modal submit calls openShift and enables POS interface` | `click` submit button in Open Shift modal | User fills required fields and submits; MSW returns new shift on `POST /shifts` | `openShift` called once with correct payload; modal element removed from DOM; product search input, cart area, and Complete Sale button are now enabled/visible |
| `product search is inactive before shift is open` | Render | MSW returns no open shift on mount | Product search input is absent or has `disabled` attribute while Open Shift modal is displayed |
| `product search with 3+ chars triggers debounced searchProducts` | `input` event on product search field | User types "mil" and debounce settles; MSW returns matching products | `GET /products/search?q=mil` called exactly once after debounce; results dropdown is visible containing matched product entries |
| `product search with fewer than 3 chars does not call API` | `input` event on product search field | User types "mi" (2 chars) | No API call is made; results dropdown is absent |
| `rapid typing only triggers one debounced search call` | Multiple `input` events in quick succession | User types "m", "mi", "mil" within 100 ms each | Only one API call fires after debounce settles; intermediate queries not dispatched |
| `product search dropdown shows name, price, and stock for each result` | Render after search | MSW returns products with name, unitPrice, and stockQty | Each dropdown item contains the product name, formatted unit price, and stock quantity |
| `selecting product from dropdown calls addLine and shows cart row` | `click` on a dropdown item | Dropdown is visible with one result; user clicks it | `addLine` called with the selected product; a new cart row appears showing the product name, unit price, quantity 1, and calculated line total |
| `selecting same product again increments qty instead of duplicating row` | `click` same dropdown item a second time | Cart already contains a row for the product | Row count does not increase; the existing row's quantity becomes 2 and line total updates accordingly |
| `qty input change in cart row calls updateLine and recalculates line total` | `input` event on qty field in cart row | Cart has one row with unitPrice=15000; user changes qty from 1 to 3 | `updateLine` called with new qty; line total cell immediately shows 45000 |
| `qty set to 0 or blank is rejected with validation feedback` | `input` event on qty field | User clears qty input or enters 0 | Qty field shows validation error or reverts to 1; `updateLine` not called with invalid qty |
| `trash icon on cart row calls removeLine and removes row` | `click` trash icon on a cart row | Cart has two rows; user clicks the trash icon on the first row | `removeLine` called with the correct line id; that row is removed; remaining row is still present |
| `coupon input + Apply with valid code calls validateCoupon and shows discount row` | `click` Apply coupon button | User types "SAVE10" in coupon input; MSW returns valid coupon with 10% discount | `validateCoupon` called with "SAVE10"; a discount row appears in the totals section showing the discount label and computed discount amount |
| `applying valid coupon does not close or disrupt cart rows` | `click` Apply coupon button | Cart has two items; coupon applied successfully | Cart row count unchanged; only totals section updates to include discount row |
| `expired coupon code shows "Coupon expired" error` | `click` Apply coupon button | MSW returns 422 with `{ code: "COUPON_EXPIRED" }` for the coupon code | Error message "Coupon expired" is visible in or near the coupon input area; no discount row added to totals |
| `invalid coupon code shows "Invalid coupon" error` | `click` Apply coupon button | MSW returns 404 or 422 with `{ code: "COUPON_INVALID" }` | Error message "Invalid coupon" is visible; no discount row added to totals |
| `error message clears when user modifies coupon input` | `input` event on coupon field after error | Error "Invalid coupon" shown; user starts typing a new code | Error message element is removed from DOM |
| `CASH tab selected + amount entered + Complete Sale calls addPayment then completeTransaction` | `click` CASH tab, `input` cash amount, `click` Complete Sale | Cart has items totalling 50000; user enters 60000 in cash amount field; MSW returns success | `addPayment` called with `{ method: "CASH", amount: 60000 }`; `completeTransaction` called; success screen element becomes visible |
| `success screen shows correct change amount` | Render after completeTransaction | Transaction total=50000; cash tendered=60000 | Change due label displays 10000 (or formatted equivalent) |
| `success screen shows Print Receipt button` | Render after completeTransaction | Successful sale completed | Element with text "Print Receipt" is present and enabled in DOM |
| `success screen shows New Sale button` | Render after completeTransaction | Successful sale completed | Element with text "New Sale" is present and enabled in DOM |
| `underpayment shows "Insufficient payment amount" error` | `click` Complete Sale | Cart total=50000; cash amount entered=30000 | Error message "Insufficient payment amount" is visible; `addPayment` / `completeTransaction` not called; cart and payment form remain active |
| `New Sale button calls createTransaction and resets all state` | `click` "New Sale" on success screen | Success screen displayed | `createTransaction` called; cart rows cleared (empty cart); coupon input cleared; product search field cleared; success screen hidden; POS interface in initial ready state |
| `Complete Sale button is disabled when cart is empty` | Render | Cart has no line items | Complete Sale button has `disabled` attribute; clicking it triggers no API call |
| `Complete Sale button becomes enabled when cart has items` | Render after adding item | Cart gains at least one line | Complete Sale button `disabled` attribute removed; button is interactive |
| `Void Transaction button shows confirmation dialog` | `click` Void Transaction | Cart has items; transaction is in progress | A confirmation dialog or modal becomes visible; cart rows not yet cleared |
| `confirming void dialog calls voidTransaction and clears cart` | `click` confirm in void dialog | Confirmation dialog visible; user confirms | `voidTransaction` called; cart rows cleared; coupon cleared; product search reset |
| `cancelling void dialog leaves cart unchanged` | `click` cancel in void dialog | Confirmation dialog visible; user clicks cancel | Dialog closes; cart rows unchanged; `voidTransaction` not called |
| `Close Shift button opens modal with running totals and actual cash input` | `click` Close Shift button | Shift is open with some completed sales | Close Shift modal becomes visible; modal shows total sales count and sales amount computed from shift; actual cash input field is present and empty |
| `Close Shift modal confirm calls closeShift with actual cash amount` | `click` confirm in Close Shift modal | User enters actual cash amount in field and confirms | `closeShift` called with payload containing actual cash amount; modal closes; UI reflects shift closed state (POS interface disabled or Open Shift prompt shown) |
| `Close Shift modal cancel does not call closeShift` | `click` cancel in Close Shift modal | Close Shift modal is open; user clicks cancel | Modal closes; `closeShift` not called; POS interface remains active |

---

## TransactionsView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `getTransactions called on mount` | `onMounted` | Component mounts; MSW returns a page of transactions | `getTransactions` called exactly once on mount; table renders returned rows |
| `transactions table renders all required columns` | Render after mount | MSW returns one transaction with all fields | Table headers and corresponding row cells contain transaction number, date, cashier, total, and status |
| `status badge renders correctly for COMPLETED` | Render after mount | MSW returns a transaction with `status: "COMPLETED"` | Status cell for that row contains a badge element with text "COMPLETED" and a green/success visual style |
| `status badge renders correctly for VOIDED` | Render after mount | MSW returns a transaction with `status: "VOIDED"` | Status cell for that row contains a badge element with text "VOIDED" and a red/danger visual style |
| `date range filter change calls getTransactionsByDateRange` | `change` on date range picker | User sets a from-date and to-date | `getTransactionsByDateRange` called with the selected date range params; table updates to show filtered results |
| `status filter COMPLETED calls getTransactionsByStatus` | `change` on status filter dropdown | User selects "COMPLETED" | `getTransactionsByStatus({ status: "COMPLETED" })` called; table updates |
| `status filter VOIDED calls getTransactionsByStatus` | `change` on status filter dropdown | User selects "VOIDED" | `getTransactionsByStatus({ status: "VOIDED" })` called; table updates |
| `status filter cleared resets to all transactions` | `change` on status filter dropdown | User selects blank/all option after filtering | `getTransactions` called without status filter; all transactions shown |
| `terminal filter dropdown calls getTransactionsByTerminal` | `change` on terminal filter dropdown | User selects a specific terminal from the dropdown | `getTransactionsByTerminal({ terminalId: id })` called; table updates to show only that terminal's transactions |
| `row click opens receipt detail modal with line items` | `click` on a transaction row | Transaction has 2 line items; user clicks the row | Receipt detail modal becomes visible; modal body contains 2 line item rows showing product name and amounts |
| `receipt detail modal shows payment info` | `click` on a transaction row | Transaction paid with CASH; change amount recorded | Modal contains payment method "CASH" and change given amount |
| `receipt detail modal close button hides modal` | `click` close in modal | Receipt detail modal is open | Modal element is removed or hidden; no API call made |
| `Void button present on COMPLETED row` | Render after mount | MSW returns a COMPLETED transaction | A Void action button or icon is present in that row |
| `Void button absent on VOIDED row` | Render after mount | MSW returns a VOIDED transaction | No Void button or action element is present in that row |
| `Void button on COMPLETED row calls voidTransaction and changes badge to VOIDED` | `click` Void button | Void button clicked on COMPLETED transaction id=55; MSW returns success | `voidTransaction(55)` called; status badge in that row changes to "VOIDED"; Void button disappears from that row |
| `pagination next calls API with incremented page param` | `click` next-page control | Current page is 1; total pages > 1 | A new API call is made with `page=2`; table updates to page 2 data |
| `pagination prev calls API with decremented page param` | `click` prev-page control | Current page is 2 | A new API call is made with `page=1`; table updates to page 1 data |
| `pagination prev disabled on first page` | Render | Mount on page 1 | Previous/back page button has `disabled` attribute or is absent |
| `empty result shows "No transactions found" empty state` | Render after mount | MSW returns `{ data: [], meta: { totalPages: 0 } }` | Element containing text "No transactions found" is visible; table body has no data rows |

---

## ShiftsView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `getShifts and getCurrentShift called on mount` | `onMounted` | Component mounts; MSW returns both a current shift and a list of historical shifts | `getShifts` and `getCurrentShift` each called exactly once on mount |
| `open shift section shows green OPEN badge` | Render after mount | MSW returns an open shift | A badge element with text "OPEN" and a green/success CSS class is visible in the open-shift summary area |
| `open shift section shows terminal name` | Render after mount | Open shift has `terminalName: "Terminal 1"` | Terminal name "Terminal 1" is displayed in the open-shift section |
| `open shift section shows opened timestamp` | Render after mount | Open shift has `openedAt: "2026-04-18T08:00:00"` | The opened-at timestamp is displayed in a human-readable format in the open-shift section |
| `open shift section shows running sales total` | Render after mount | Open shift has `totalSales: 750000` | Formatted sales total (e.g. "750,000") is displayed in the open-shift section |
| `Close Shift button present on open shift section` | Render after mount | An open shift exists | "Close Shift" button is visible in the open-shift summary area |
| `Close Shift button click opens modal with total sales and cash input` | `click` Close Shift button | Open shift has total sales and a Close Shift button | Close Shift modal becomes visible; modal shows the computed total sales amount; actual cash input field is present and empty |
| `confirm close calls closeShift and marks shift CLOSED in list` | `click` confirm in Close Shift modal | User enters actual cash amount and confirms | `closeShift` called with actual cash payload; open-shift summary section is removed or shows no open shift; the closed shift appears with a "CLOSED" badge in the historical shifts table |
| `cancel close modal does not call closeShift` | `click` cancel in Close Shift modal | Close Shift modal is open; user clicks cancel | Modal closes; `closeShift` not called; open-shift section unchanged |
| `historical shifts table renders required columns` | Render after mount | MSW returns 2 historical shifts | Table headers and row cells contain shift number, terminal, cashier, opened time, closed time, and total sales |
| `historical shift CLOSED badge rendered` | Render after mount | Shift in list has `status: "CLOSED"` | Status cell for that row contains a "CLOSED" badge with a grey/neutral visual style |
| `clicking historical shift row expands transactions sub-table` | `click` on a historical shift row | Row is initially collapsed; shift has 3 transactions | A transactions sub-table expands below that row showing all 3 transactions; each sub-row shows relevant transaction info |
| `expanding sub-table calls getTransactionsByShift` | `click` on a historical shift row | Shift row clicked for shift id=8 | `getTransactionsByShift(8)` (or equivalent) called; returned transactions rendered in the expanded sub-table |
| `clicking expanded shift row again collapses sub-table` | `click` on an already-expanded shift row | Sub-table is visible | Sub-table collapses; no additional API call made |
| `expanding a different shift collapses the previously expanded one` | `click` on a second shift row | One shift row is already expanded | Previously expanded sub-table collapses; newly clicked shift's sub-table expands |
| `no open shift shows no open-shift summary section` | Render after mount | MSW returns 404 or empty for `GET /shifts/current` | Open-shift summary area (with OPEN badge and Close Shift button) is absent from DOM |
| `empty shifts list shows "No shifts yet" message` | Render after mount | MSW returns `{ data: [] }` for `GET /shifts` | Element containing text "No shifts yet" is visible; historical shifts table body has no data rows |

---

## MSW Handler Reference (shared setup)

```typescript
// vitest.setup.ts (excerpt)
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'

export const server = setupServer(
  // Shifts
  http.get('/api/shifts/current',             () => HttpResponse.json({})),
  http.post('/api/shifts',                    () => HttpResponse.json({ id: 1 }, { status: 201 })),
  http.post('/api/shifts/:id/close',          () => HttpResponse.json({})),
  http.get('/api/shifts',                     () => HttpResponse.json({ data: [], meta: { page: 1, totalPages: 1 } })),
  http.get('/api/shifts/:id/transactions',    () => HttpResponse.json({ data: [] })),

  // Transactions
  http.get('/api/transactions',               () => HttpResponse.json({ data: [], meta: { page: 1, totalPages: 1 } })),
  http.post('/api/transactions',              () => HttpResponse.json({ id: 100 }, { status: 201 })),
  http.post('/api/transactions/:id/void',     () => HttpResponse.json({})),
  http.post('/api/transactions/:id/payments', () => HttpResponse.json({})),
  http.post('/api/transactions/:id/complete', () => HttpResponse.json({ change: 0 })),

  // Products / POS search
  http.get('/api/products/search',            () => HttpResponse.json({ data: [] })),

  // Coupons
  http.post('/api/coupons/validate',          () => HttpResponse.json({ discount: 0 })),
)

beforeAll(() => server.listen({ onUnhandledRequest: 'warn' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())
```

> Each test that requires a non-default response calls `server.use(http.get(..., handler))` before mounting the component.

**Debounce testing:** Use `vi.useFakeTimers()` before each debounce test and call `vi.advanceTimersByTime(300)` (or the configured debounce interval) to fire the timer synchronously. Restore with `vi.useRealTimers()` in `afterEach`.

**Dialog/confirmation testing:** If the app uses `window.confirm`, stub it with `vi.spyOn(window, 'confirm').mockReturnValue(true)` (or `false` for cancel tests). If a custom modal component is used, click its confirm control via `wrapper.find('[data-testid="confirm-btn"]').trigger('click')`.

**Coverage collection:** Run with `vitest --coverage` using the `@vitest/coverage-v8` provider. Set statement, branch, function, and line thresholds to `100` for all files under `src/views/pos/` in `vitest.config.ts`.
