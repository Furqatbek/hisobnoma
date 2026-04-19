# Section 6b-iii: Frontend — Customer & Purchase Views — Test Plan

**Stack:** Vitest + @vue/test-utils + MSW (Mock Service Worker)
**Coverage target:** 100% component test coverage for all customer- and purchase-related views
**Conventions used in this section:**
- "mount" = `mountAsync` via `@vue/test-utils` with a Pinia store and Vue Router stub
- MSW handlers intercept all API calls; each test seeds its own handler overrides
- "trigger" = `wrapper.find(...).trigger('...')` + `await nextTick()` unless stated otherwise
- "emitted" = assertion on `wrapper.emitted()`
- DOM assertions use `wrapper.text()`, `wrapper.find()`, or `wrapper.html()` as appropriate

---

## 1. CustomersView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Loads paginated customer list on mount | `onMounted` lifecycle | MSW returns 2 customers on `GET /customers?page=1` | `getCustomers` is called once; table renders exactly 2 rows with code, name, phone, email, balance, and status columns populated |
| Renders all required table columns | Render / DOM inspection | Mount with 1 seeded customer record | Table headers contain "Code", "Name", "Phone", "Email", "Balance", "Status"; row cells contain matching data values |
| Search input triggers debounced searchCustomers | User input | Type "Ahmad" into search input; advance fake timers past debounce delay | `searchCustomers` called with `{ q: 'Ahmad' }`; table re-renders with only matched rows; previous rows removed |
| Search results replace table content | User input + MSW | MSW returns 1 result for search query; 3 rows shown before search | After debounce resolves, table contains exactly 1 row matching search result |
| "Create Customer" button navigates to create route | Click | Click "Create Customer" button | `router.push` called with `'/customers/create'` |
| Edit button navigates to edit route for correct id | Click | Click edit button on row with id=7 | `router.push` called with `'/customers/7/edit'` |
| "View History" button navigates to history route | Click | Click "View History" on row with id=7 | `router.push` called with `'/customers/7/history'` |
| Delete button shows confirmation dialog | Click | Click delete button on any row | Confirmation dialog/modal becomes visible in DOM; row is not yet removed |
| Confirm delete calls deleteCustomer and removes row | Click confirm in dialog | Confirm deletion of customer id=7; MSW returns 200 | `deleteCustomer(7)` called; row for customer id=7 removed from table; success toast/notification shown |
| Delete customer with invoices shows 422 error | Click confirm in dialog | MSW returns 422 `{ message: "Cannot delete customer with invoices" }` | Error message "Cannot delete customer with invoices" visible in DOM; row remains in table |
| Cancel delete closes dialog without removing row | Click cancel in dialog | Click delete, then click cancel | Dialog closes; row count unchanged; `deleteCustomer` not called |
| Pagination next button calls API with page 2 | Click | Click "Next" page button; total pages > 1 | `getCustomers` called with `{ page: 2 }`; table updates to page 2 data |
| Pagination prev button calls API with page 1 | Click | On page 2, click "Previous" button | `getCustomers` called with `{ page: 1 }` |
| Pagination prev disabled on first page | Render | Mount on page 1 | "Previous" button is disabled or absent |
| Empty customer list shows empty state | Render | MSW returns empty array | Table body has no data rows; empty-state message or illustration visible |
| Status badge renders correct style per status | Render | Seed customers with ACTIVE and INACTIVE statuses | ACTIVE rows show green/active badge; INACTIVE rows show grey/inactive badge |

---

## 2. CustomerFormView.vue — Create Mode

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Calls getNextCustomerCode on mount | `onMounted` | MSW returns `{ nextCode: "CUST-000042" }` on `GET /customers/next-code` | `getNextCustomerCode` called once; code input placeholder reads "Auto-generated: CUST-000042" |
| Code field is editable in create mode | Render | Mount in create mode (no route id param) | Code input is not disabled; user can type into it |
| User can override auto-generated code | User input | Clear code input and type "CUST-TEST" | Input value reflects "CUST-TEST" |
| Blank code field submits without code | Submit | Leave code field empty, fill all other fields; MSW returns 201 | `createCustomer` called with payload that does not include `code` key, or includes `code: ""` |
| Custom code submitted when provided | Submit | Enter "CUST-TEST" in code field, fill required fields; MSW returns 201 | `createCustomer` called with `{ code: "CUST-TEST", ... }` |
| Duplicate code API 409 shows error | Submit | MSW returns 409 `{ message: "Customer code already exists" }` | Error message "Customer code already exists" rendered in form; no navigation occurs |
| Empty name shows validation error | Submit | Leave Name field blank, click Submit | Validation error "Name is required" shown near name field; `createCustomer` not called |
| Invalid phone format shows validation error | Submit | Enter "abc" in phone field, click Submit | Validation error message for phone format shown; `createCustomer` not called |
| All required form fields render | Render | Mount in create mode | Form contains inputs labelled Code, Name, Phone, Email, Address, Credit Limit |
| Submit valid form navigates to /customers | Submit | All fields valid; MSW returns 201 | `createCustomer` called with correct payload; `router.push('/customers')` called |
| Credit Limit field accepts numeric input only | User input | Type alphabetic characters into Credit Limit | Input rejects non-numeric characters or shows validation error |
| Form shows loading state while submitting | Submit | Introduce artificial API delay via MSW | Submit button disabled or shows spinner while request is in-flight |

---

## 3. CustomerFormView.vue — Edit Mode

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| getCustomer called on mount with route id | `onMounted` | Mount with route param `id=5`; MSW returns customer data | `getCustomer(5)` called once on mount |
| All fields pre-filled with existing data | Render | MSW returns customer `{ id:5, code:"CUST-0005", name:"Ali", phone:"0911...", email:"ali@x.uz", address:"Tashkent", creditLimit:5000000 }` | Each form input value matches the corresponding customer property |
| Code field is disabled in edit mode | Render | Mount with route param `id=5` | Code input has `disabled` attribute; user cannot type in it |
| Code field shows existing code as value, not placeholder | Render | Mount with route param `id=5`; code = "CUST-0005" | Code input `.value` = "CUST-0005"; placeholder not used to display the code |
| Name field is editable | User input | Clear name field and type "Hassan" | Name input accepts the new value |
| Phone field is editable | User input | Change phone to new number | Phone input reflects new value |
| Email field is editable | User input | Change email to new address | Email input reflects new value |
| Address field is editable | User input | Change address text | Address input reflects new value |
| Credit Limit field is editable | User input | Update credit limit to 10000000 | Credit limit input reflects new numeric value |
| Submit calls updateCustomer with correct id | Submit | Modify name and submit; MSW returns 200 | `updateCustomer(5, { name: "Hassan", ... })` called; code field value not included in editable payload changes |
| Successful update navigates to /customers | Submit | MSW returns 200 on PUT/PATCH | `router.push('/customers')` called after successful update |
| API 404 on load shows error and redirects | `onMounted` | MSW returns 404 for `GET /customers/999` | Error message rendered; `router.push('/customers')` called |
| getNextCustomerCode is NOT called in edit mode | `onMounted` | Mount with route param id present | `getNextCustomerCode` endpoint not called |

---

## 4. CustomerHistoryView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| getCustomer called on mount | `onMounted` | Mount with route param `id=3`; MSW returns customer data | `getCustomer(3)` called; customer name, code, and balance displayed in header area |
| Customer header shows name, code, balance | Render | Customer `{ name:"Dilnoza", code:"CUST-003", balance:1500000 }` | Header/summary area contains "Dilnoza", "CUST-003", and formatted balance |
| AR Invoices tab active by default on mount | Render | Mount component | AR Invoices tab is the active/selected tab; invoice table visible |
| AR Invoices tab loads getARInvoices on mount | `onMounted` | MSW returns 2 invoices filtered by `customerId=3` | `getARInvoices({ customerId: 3 })` called; table shows 2 invoice rows |
| Invoices table shows correct columns | Render | 1 seeded invoice | Table shows number, date, due date, amount, balance due, and status columns |
| UNPAID status badge renders red | Render | Invoice with status=UNPAID | Status cell has CSS class or style indicating red/danger colour |
| PARTIAL status badge renders yellow | Render | Invoice with status=PARTIAL | Status cell has CSS class or style indicating yellow/warning colour |
| PAID status badge renders green | Render | Invoice with status=PAID | Status cell has CSS class or style indicating green/success colour |
| Overdue invoice row highlighted red | Render | Invoice with due date in the past and status != PAID | Row has overdue CSS class or red highlight; non-overdue rows do not |
| Payments tab click loads getARPayments | Click | Click "Payments" tab | `getARPayments({ customerId: 3 })` called; payments table rendered with number, date, method, amount columns |
| Credit Notes tab click loads getCreditNotes | Click | Click "Credit Notes" tab | `getCreditNotes({ customerId: 3 })` called; credit notes table rendered with number, date, amount, status columns |
| Credit note status OPEN and APPLIED shown | Render | Seed one OPEN and one APPLIED credit note | Both status values rendered correctly in status column |
| Balance summary shows correct totals | Render | MSW returns invoices totalling 3000000, payments 1500000 | Summary area shows Total Invoiced, Total Paid, Outstanding Balance with correct computed values |
| "Create Invoice" button navigates with customerId | Click | Click "Create Invoice" | `router.push('/finance/debtors/create?customerId=3')` called |
| "Record Payment" button opens payment modal | Click | Click "Record Payment" | Payment modal becomes visible in DOM |
| Switching tabs does not repeat customer API call | Click | Click through all 3 tabs | `getCustomer` called exactly once; tab data APIs called per tab activation |

---

## 5. SuppliersView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Loads vendors on mount with pagination | `onMounted` | MSW returns paginated vendor list | `getVendors` called once with `{ page: 1 }`; table renders returned rows |
| Vendor table shows required columns | Render | 1 seeded vendor record | Table contains Code, Name, Phone, Email, Status columns with correct values |
| Search input calls searchVendors | User input | Type "Toshmat" and advance past debounce | `searchVendors({ q: "Toshmat" })` called; table updates with search results |
| "Create Supplier" button navigates to create route | Click | Click "Create Supplier" | Navigation to supplier form or modal triggered |
| Edit button navigates to edit route | Click | Click edit on vendor id=4 | Navigation to vendor edit view for id=4 triggered |
| "View History" button navigates correctly | Click | Click "View History" on vendor id=4 | `router.push('/purchases/supplier-history/4')` called |
| Delete button shows confirmation dialog | Click | Click delete on any vendor row | Confirmation dialog visible; vendor row not yet removed |
| Confirm delete calls deleteVendor and removes row | Click confirm | Confirm deletion; MSW returns 200 | `deleteVendor(id)` called; row removed from table |
| Delete with POs returns 422 error | Click confirm | MSW returns 422 with error message | Error message visible; row not removed |
| Pagination next loads page 2 | Click | Click "Next" when total pages > 1 | `getVendors({ page: 2 })` called |
| Pagination prev disabled on first page | Render | Mount on page 1 | Previous button disabled or absent |
| Empty vendor list shows empty state | Render | MSW returns empty array | Empty-state message or illustration shown; no table rows |
| Status badge renders per vendor status | Render | Mix of ACTIVE and INACTIVE vendors | ACTIVE and INACTIVE badges styled distinctly |

---

## 6. PurchaseOrdersView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Loads purchase orders on mount | `onMounted` | MSW returns list of POs on `GET /purchase-orders?page=1` | `getPurchaseOrders` called once; PO rows rendered in table |
| Table shows required columns | Render | 1 seeded PO | Table shows PO number, vendor, date, status badge, total amount |
| Status badge renders per PO status | Render | Seed POs with DRAFT, RELEASED, RECEIVED, CANCELLED | Each status renders a visually distinct badge |
| Status filter dropdown triggers filtered API call | Change | Select "RELEASED" in status filter dropdown | `getPurchaseOrders({ status: "RELEASED" })` called; table updates |
| Date range filter triggers filtered API call | Change | Set from-date and to-date | `getPurchaseOrders({ dateFrom: "...", dateTo: "..." })` called |
| "Create PO" button navigates to create form | Click | Click "Create PO" | `router.push('/purchases/purchase-orders/create')` called |
| Click PO row navigates to detail view | Click | Click row for PO id=12 | `router.push('/purchases/purchase-orders/12')` called |
| Empty list renders empty state message | Render | MSW returns empty array | "No purchase orders found" message visible; no table rows |
| Pagination next triggers page 2 call | Click | Click "Next"; total pages > 1 | `getPurchaseOrders({ page: 2 })` called |
| Pagination prev triggers page 1 call | Click | On page 2, click "Previous" | `getPurchaseOrders({ page: 1 })` called |
| Clearing status filter resets to all POs | Change | Select "RELEASED", then select blank/all option | `getPurchaseOrders` called without status filter; all POs shown |
| Combined filters pass all params | Change | Set status=DRAFT and date range | API called with both `status` and date params simultaneously |

---

## 7. PurchaseOrderFormView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Vendor dropdown loaded on mount | `onMounted` | MSW returns 3 active vendors | `getActiveVendors` called once; dropdown contains 3 vendor options |
| Order date defaults to today | Render | Mount component | Date picker value equals today's date (2026-04-17) |
| "Add Line" button adds a new empty line row | Click | Click "Add Line" | New row appears in lines table with empty product search, qty, and unit cost fields |
| Product search in line triggers searchProducts | User input | Type "Cement" in product search of first line | `searchProducts({ q: "Cement" })` called; dropdown of matching products shown |
| Selecting product pre-fills name and unit cost | Click | Select product from dropdown | Product name field and unit cost field populated with product data |
| Entering qty calculates line total | User input | Enter qty=5 with unit cost=200000 | Line total cell shows 1000000 |
| Remove line button deletes the row | Click | Add two lines, click remove on first line | First row removed; one row remains |
| Cannot remove last remaining line | Click | Only one line present, click remove | Remove button absent or disabled on the sole remaining row |
| Submit with vendor + lines calls createPurchaseOrder | Submit | Select vendor, add one valid line, click Submit; MSW returns 201 | `createPurchaseOrder` called with correct payload; redirect to PO detail view |
| Submit with no vendor shows validation error | Submit | Leave vendor blank, add valid lines, click Submit | Validation error "Vendor is required" shown; `createPurchaseOrder` not called |
| Submit with no lines shows validation error | Submit | Select vendor, no lines added, click Submit | Validation error "At least one line is required" shown; `createPurchaseOrder` not called |
| Submit with qty=0 on a line shows validation error | Submit | Set qty=0 on a line, click Submit | Validation error for zero-quantity line shown; `createPurchaseOrder` not called |
| Line total updates when qty changes | User input | Change qty after initial entry | Line total recalculates reactively |
| Line total updates when unit cost changes | User input | Change unit cost after initial entry | Line total recalculates reactively |
| Redirect to PO detail on successful create | Submit | MSW returns 201 with new PO id=99 | `router.push('/purchases/purchase-orders/99')` called |

---

## 8. PurchaseOrderDetailView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| getOnePurchaseOrder called on mount with id | `onMounted` | Mount with route param `id=12` | `getOnePurchaseOrder(12)` called once |
| Header shows PO metadata | Render | PO `{ number:"PO-0012", vendor:"Toshmat LLC", createdAt:"2026-04-10", status:"DRAFT", total:3000000 }` | Header displays PO number, vendor name, created date, status badge, and total |
| Lines table shows all required columns | Render | PO with 2 line items | Table shows product, ordered qty, received qty (0), unit cost, and line total per row |
| Received qty defaults to 0 initially | Render | PO that has not been received | Received qty column shows 0 for all lines |
| "Release PO" visible only when DRAFT | Render | PO with status=DRAFT | "Release PO" button visible; not present for RELEASED/RECEIVED/CANCELLED |
| Click Release shows confirmation then calls API | Click | Click "Release PO", confirm dialog | Confirmation dialog appears; on confirm `releasePurchaseOrder(12)` called |
| Status badge updates to RELEASED after release | Click | Successful release API call | Status badge changes from DRAFT to RELEASED without full page reload |
| "Cancel PO" visible when DRAFT or RELEASED | Render | Mount with DRAFT and separately with RELEASED PO | "Cancel PO" button visible for both DRAFT and RELEASED statuses |
| "Cancel PO" not visible when RECEIVED | Render | PO with status=RECEIVED | "Cancel PO" button absent from DOM |
| Click Cancel shows confirmation then calls API | Click | Click "Cancel PO", confirm | `cancelPurchaseOrder(12)` called; status badge changes to CANCELLED |
| "Create Receiving" visible only when RELEASED | Render | RELEASED PO | "Create Receiving" button visible; absent for DRAFT and CANCELLED |
| Click "Create Receiving" navigates or opens modal | Click | Click "Create Receiving" on RELEASED PO | Navigate to receiving form for this PO, or receiving modal becomes visible |
| API 404 on load shows error and redirects | `onMounted` | MSW returns 404 for `GET /purchase-orders/999` | Error message rendered; `router.push('/purchases/purchase-orders')` called |
| Total in header matches sum of lines | Render | PO with 2 lines; total=5000000 | Header total equals sum of individual line totals |

---

## 9. SupplierHistoryView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| getVendor called on mount with route id | `onMounted` | Mount with route param `id=6` | `getVendor(6)` called once |
| Vendor info displayed at top | Render | Vendor `{ name:"Toshmat LLC", code:"VEND-006", phone:"0901..." }` | Header area shows vendor name and code |
| AP Invoices tab active by default | Render | Mount component | AP Invoices tab selected; AP invoice table visible |
| AP Invoices tab loads getAPInvoices on mount | `onMounted` | MSW returns 2 AP invoices filtered by `vendorId=6` | `getAPInvoices({ vendorId: 6 })` called; table shows 2 rows |
| AP Invoices table has required columns | Render | 1 seeded AP invoice | Columns include invoice number, date, amount, balance, status |
| Payments tab click loads getAPPayments | Click | Click "Payments" tab | `getAPPayments({ vendorId: 6 })` called; table shows payment number, date, method, amount |
| Purchase Orders tab click loads getPurchaseOrders | Click | Click "Purchase Orders" tab | `getPurchaseOrders({ vendorId: 6 })` called; table shows PO number, date, status, total |
| PO table in history has required columns | Render | 1 seeded PO in history | PO number, date, status badge, total all rendered |
| Balance summary shows correct totals | Render | AP invoices total=4000000, payments=2000000 | Summary shows Total Invoiced, Total Paid, Outstanding Payable with computed values |
| Outstanding Payable = Total Invoiced - Total Paid | Render | Invoiced=4000000, Paid=2000000 | Outstanding Payable displayed as 2000000 |
| "Create Invoice" button navigates to AP invoice create | Click | Click "Create Invoice" | Navigation to AP invoice create view or modal triggered with vendorId pre-filled |
| getVendor called only once across tab switches | Click | Click all 3 tabs | `getVendor` called exactly once; tab-specific API calls once per tab activation |
| AP invoice status badges render distinctly | Render | Mix of UNPAID, PARTIAL, PAID invoices | Each status renders with distinct visual styling |
| Empty AP invoices shows empty state | Render | MSW returns empty array for AP invoices | Empty-state message visible; no table rows in AP Invoices tab |

---

## MSW Handler Reference (shared setup)

```js
// vitest.setup.ts (excerpt)
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'

export const server = setupServer(
  http.get('/api/customers',         () => HttpResponse.json({ data: [], meta: { page: 1, totalPages: 1 } })),
  http.get('/api/customers/next-code', () => HttpResponse.json({ nextCode: 'CUST-000042' })),
  http.get('/api/customers/:id',     () => HttpResponse.json({})),
  http.post('/api/customers',        () => HttpResponse.json({}, { status: 201 })),
  http.put('/api/customers/:id',     () => HttpResponse.json({})),
  http.delete('/api/customers/:id',  () => new HttpResponse(null, { status: 204 })),
  http.get('/api/vendors',           () => HttpResponse.json({ data: [], meta: {} })),
  http.get('/api/vendors/:id',       () => HttpResponse.json({})),
  http.delete('/api/vendors/:id',    () => new HttpResponse(null, { status: 204 })),
  http.get('/api/purchase-orders',   () => HttpResponse.json({ data: [], meta: {} })),
  http.get('/api/purchase-orders/:id', () => HttpResponse.json({})),
  http.post('/api/purchase-orders',  () => HttpResponse.json({ id: 99 }, { status: 201 })),
  http.post('/api/purchase-orders/:id/release', () => HttpResponse.json({})),
  http.post('/api/purchase-orders/:id/cancel',  () => HttpResponse.json({})),
  http.get('/api/ar-invoices',       () => HttpResponse.json({ data: [] })),
  http.get('/api/ar-payments',       () => HttpResponse.json({ data: [] })),
  http.get('/api/credit-notes',      () => HttpResponse.json({ data: [] })),
  http.get('/api/ap-invoices',       () => HttpResponse.json({ data: [] })),
  http.get('/api/ap-payments',       () => HttpResponse.json({ data: [] })),
  http.get('/api/products/search',   () => HttpResponse.json({ data: [] })),
)

beforeAll(() => server.listen())
afterEach(() => server.resetHandlers())
afterAll(() => server.close())
```

> Each test that requires a non-default response calls `server.use(http.get(..., handler))` before mounting the component.
