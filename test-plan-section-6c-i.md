# Section 6c-i: Frontend — Finance Views — Test Plan

**Testing Stack:** Vitest + @vue/test-utils + MSW  
**Coverage Goal:** 100% component test coverage for all finance-related Vue.js views  
**Platform:** Hisobnoma SaaS

---

## Overview

This section covers unit and integration-level component tests for every view under the Finance module. Each test is designed to be executed in a Vitest environment with `@vue/test-utils` for DOM mounting/interaction and MSW (Mock Service Worker) for intercepting HTTP calls at the network layer. All tests are isolated per component; shared MSW handlers are reset between tests via `afterEach`.

---

## ExpensesView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `mount calls getExpenses and renders table` | `onMounted` | Component mounts with no filter params | MSW handler for `GET /expenses?page=0` is called exactly once; table rows appear with date, category, description, and amount columns present in the header |
| `table renders required columns` | Render after mount | MSW returns an expense list with all fields populated | Table headers contain Date, Category, Description, Amount; each row cell maps to the correct field |
| `date range filter triggers filtered API call` | `change` event on date range inputs | User sets start date `2024-01-01` and end date `2024-01-31` | `GET /expenses?from=2024-01-01&to=2024-01-31` is called; table rows update to matching expenses only |
| `category filter dropdown triggers filtered API call` | `change` event on category dropdown | User selects category "Office Supplies" | `GET /expenses?category=Office+Supplies` (or equivalent id param) is called; table rows update to matching category only |
| `Create Expense button navigates to create form` | `click` on "Create Expense" button | User clicks the "Create Expense" button | `router.push` is called with `/finance/expenses/create` |
| `click expense row navigates to detail` | `click` on a table row | User clicks the row for expense with id `17` | `router.push` is called with `/finance/expenses/17` |
| `amount cell formatted as currency` | Render after mount | MSW returns an expense with amount `1234.5` | Amount cell text is formatted as a currency string (e.g., `$1,234.50` or locale equivalent); raw number `1234.5` is not displayed as-is |
| `footer shows total of displayed amounts` | Render after mount | MSW returns expenses with amounts `100.00`, `200.00`, and `50.00` | A footer row or summary element displays the summed total `350.00` formatted as currency |
| `pagination next calls getExpenses with page=1` | `click` on Next page control | Current page is 0; user clicks Next | `GET /expenses?page=1` is called; table rows update to page-2 data |
| `pagination prev calls getExpenses with page=0` | `click` on Previous page control | Current page is 1; user clicks Previous | `GET /expenses?page=0` is called; table rows update to page-1 data |
| `empty list shows empty state message` | Render after mount | MSW returns `{ data: [], total: 0 }` | Element with text "No expenses found" is present in the DOM; table body has no `<tr>` data rows |

---

## ExpenseFormView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `create mode renders all required fields` | Render | Component mounted without an `id` route param | Inputs/selects for amount, category, description, and date are all present in the DOM |
| `date field defaults to today in create mode` | Render | Component mounted without an `id` route param | Date input value equals today's date in ISO format (`2026-04-18` or locale-formatted equivalent) |
| `submit valid form calls createExpense and redirects` | `submit` form event | All required fields filled with valid values; amount is positive | `POST /expenses` is called with the correct payload; `router.push('/finance/expenses')` is invoked |
| `submit negative amount shows validation error` | `submit` form event | Amount field contains `-50` | No API call is made; element containing text "Amount must be positive" is visible in the DOM |
| `submit zero amount shows validation error` | `submit` form event | Amount field contains `0` | No API call is made; a validation error element is visible in the DOM (e.g., "Amount must be positive" or "Amount must be greater than zero") |
| `submit with no category shows validation error` | `submit` form event | Category field/select is left blank or unselected | No API call is made; element containing text "Category required" is visible in the DOM |
| `edit mode calls getExpense on mount` | `onMounted` | Component mounted with route param `id=42` | `GET /expenses/42` is called exactly once on mount |
| `edit mode pre-fills all fields from loaded expense` | Render after mount | MSW returns expense with amount `250.00`, category "Travel", description "Flight to Tashkent", date `2024-03-15` | Amount input value is `250.00`; category select shows "Travel"; description input contains "Flight to Tashkent"; date input contains `2024-03-15` |
| `edit mode submit calls updateExpense` | `submit` form event | Component in edit mode; user changes description and submits | `PUT /expenses/42` is called with the updated payload; `router.push('/finance/expenses')` is invoked |
| `404 on edit load shows error and redirects to list` | `onMounted` | MSW returns 404 for `GET /expenses/42` | An error message is rendered (or shown briefly); `router.push('/finance/expenses')` is called |

---

## ExpenseDetailView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `mount calls getExpense by ID and renders all fields` | `onMounted` | Component mounted with route param `id=42` | `GET /expenses/42` is called exactly once; elements for date, category, description, and amount are all present in the DOM and contain the MSW-returned values |
| `Edit button navigates to edit form` | `click` on "Edit" button | Expense detail loaded for id `42` | `router.push` is called with `/finance/expenses/42/edit` |
| `Delete button shows confirmation dialog` | `click` on "Delete" button | Expense detail loaded; user clicks Delete | A confirmation dialog (modal or native confirm) is rendered/visible; no API call has been made yet |
| `confirm delete calls deleteExpense and redirects to list` | `click` confirm in dialog | User confirms the deletion dialog | `DELETE /expenses/42` is called; `router.push('/finance/expenses')` is invoked |
| `cancel delete makes no API call` | `click` cancel in dialog | User dismisses the confirmation dialog | No `DELETE /expenses/.*` request is recorded by MSW; user remains on the detail view |

---

## DebtorsView.vue (AR Invoices)

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `mount calls getARInvoices and renders table` | `onMounted` | Component mounts with no filter params | MSW handler for `GET /ar-invoices?page=0` is called exactly once; table rows appear |
| `table renders required columns` | Render after mount | MSW returns an AR invoice with all fields populated | Table headers contain Invoice Number, Customer, Date, Due Date, Amount, Balance, and Status; each row cell maps to the correct field |
| `UNPAID badge renders with red styling` | Render after mount | MSW returns an invoice with `status: "UNPAID"` | The status cell contains a badge element with a red colour class/attribute (e.g., `badge-red`, `text-red-600`, or equivalent) |
| `PARTIAL badge renders with yellow styling` | Render after mount | MSW returns an invoice with `status: "PARTIAL"` | The status cell contains a badge element with a yellow colour class/attribute |
| `PAID badge renders with green styling` | Render after mount | MSW returns an invoice with `status: "PAID"` | The status cell contains a badge element with a green colour class/attribute |
| `overdue unpaid row is highlighted red` | Render after mount | MSW returns an invoice with a due date in the past and `status: "UNPAID"` | The table row element carries a red highlight class/attribute (e.g., `row-overdue`, `bg-red-50`); a PAID or future-due row does not carry this class |
| `overdue partial row is highlighted red` | Render after mount | MSW returns an invoice with a due date in the past and `status: "PARTIAL"` | The table row element carries the red overdue highlight class |
| `paid row past due date is NOT highlighted red` | Render after mount | MSW returns an invoice with a due date in the past and `status: "PAID"` | The table row does NOT carry the overdue highlight class |
| `customer search filter triggers debounced API call` | `input` event on customer search field | User types "Alisher" in the customer search input; wait for debounce | `GET /ar-invoices?customer=Alisher` (or equivalent query param) is called once after debounce settles; previous rows replaced by filtered results |
| `rapid customer search typing only triggers one debounced call` | Multiple `input` events in quick succession | User types "A", "Al", "Ali" within 100 ms each | Only one API call fires after debounce settles; intermediate queries are not dispatched |
| `status filter triggers filtered API call` | `change` event on status filter dropdown | User selects status "UNPAID" | `GET /ar-invoices?status=UNPAID` is called; table rows update to show only UNPAID invoices |
| `Create Invoice button navigates to create form` | `click` on "Create Invoice" button | User clicks "Create Invoice" | `router.push` is called with `/finance/debtors/create` |
| `click invoice row navigates to detail` | `click` on a table row | User clicks the row for invoice with id `55` | `router.push` is called with `/finance/debtors/55` |
| `pagination next calls getARInvoices with page=1` | `click` on Next page control | Current page is 0; user clicks Next | `GET /ar-invoices?page=1` is called; table rows update to page-2 data |
| `pagination prev calls getARInvoices with page=0` | `click` on Previous page control | Current page is 1; user clicks Previous | `GET /ar-invoices?page=0` is called; table rows update to page-1 data |

---

## DebtorFormView.vue (AR Invoice Create)

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `customer dropdown loads from getActiveCustomers on mount` | `onMounted` | Component mounts | `GET /customers/active` is called once; the customer dropdown options match the MSW-returned customer list |
| `tax code dropdown loads from getActiveTaxCodes on mount` | `onMounted` | Component mounts | `GET /tax-codes/active` is called once; tax code options in each line's tax code selector match the MSW-returned tax code list |
| `Add Line button appends a new line row` | `click` on "Add Line" button | Form rendered with one initial line | A new line row appears containing inputs for description, qty, unit price, and a tax code dropdown |
| `line total auto-calculated as qty × unit price` | `input` on qty or unit price field | User enters qty `3` and unit price `100.00` on a line row | That line's total cell displays `300.00` without any explicit save action |
| `line total updates when qty changes` | `input` on qty field | Line has unit price `50.00`; user changes qty from `2` to `5` | Line total cell updates from `100.00` to `250.00` |
| `line total updates when unit price changes` | `input` on unit price field | Line has qty `4`; user changes unit price from `10.00` to `20.00` | Line total cell updates from `40.00` to `80.00` |
| `Remove Line button removes the row` | `click` on "Remove Line" button on a row | Form has two line rows; user clicks Remove on the second row | The second row is removed from the DOM; only one line row remains |
| `Remove Line button disabled when only one line remains` | Render | Form has exactly one line row | The Remove Line button on that row has a `disabled` attribute or is absent from the DOM |
| `subtotal auto-updates when line totals change` | `input` on qty or unit price field | Two lines with totals `200.00` and `150.00`; user changes one | Subtotal element at the bottom reflects the updated sum |
| `tax amount auto-updates based on line tax codes` | `input` on qty or unit price, or `change` on tax code | Line has qty `1`, unit price `100.00`, tax code with rate `15%` | Tax total element reflects `15.00` |
| `grand total auto-updates as subtotal + tax` | `input` on any line field | Subtotal = `200.00`, tax = `30.00` | Grand total element displays `230.00` |
| `submit valid invoice calls createARInvoice and redirects` | `submit` form event | Customer selected; at least one valid line; all fields filled | `POST /ar-invoices` is called with the correct payload; `router.push('/finance/debtors')` is invoked |
| `submit with no customer shows validation error` | `submit` form event | Customer dropdown left unselected; lines otherwise valid | No API call is made; element containing text "Customer required" is visible in the DOM |
| `submit with no lines shows validation error` | `submit` form event | All line rows removed before submit | No API call is made; element containing text "At least one line required" is visible in the DOM |
| `submit with line qty=0 shows validation error` | `submit` form event | A line row has qty set to `0`; all other fields valid | No API call is made; a validation error is visible in the DOM on or near the offending line row |

---

## PaymentsView.vue (AR Payments)

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `mount calls getARPayments and renders table` | `onMounted` | Component mounts with no filter params | MSW handler for `GET /ar-payments?page=0` is called exactly once; table rows appear |
| `table renders required columns` | Render after mount | MSW returns an AR payment with all fields populated | Table headers contain Payment Number, Customer, Date, Method, and Amount; each row cell maps to the correct field |
| `customer filter triggers filtered API call` | `input` or `change` on customer filter | User selects or types customer "Nodir Toshmatov" | `GET /ar-payments?customer=…` (or equivalent query param) is called; table rows update to that customer's payments |
| `date range filter triggers filtered API call` | `change` on date range inputs | User sets start date `2024-02-01` and end date `2024-02-28` | `GET /ar-payments?from=2024-02-01&to=2024-02-28` is called; table rows update to the filtered date range |
| `combined customer and date range filters applied together` | `change` events on both filters | User selects a customer AND sets a date range | A single API call includes both query params; table rows satisfy both filter criteria |
| `Record Payment button opens modal` | `click` on "Record Payment" button | Modal is initially closed | A modal dialog becomes visible containing inputs for customer, amount, payment method, and date |
| `modal submit calls createARPayment, closes modal, and refreshes list` | `submit` inside modal | All modal fields filled with valid values; amount is positive | `POST /ar-payments` is called with the correct payload; modal is no longer visible; `GET /ar-payments` is called again to refresh the list |
| `modal negative amount shows validation error` | `submit` inside modal | Amount field contains `-100`; other fields valid | No API call is made; a validation error is visible inside the modal; modal remains open |
| `modal zero amount shows validation error` | `submit` inside modal | Amount field contains `0`; other fields valid | No API call is made; a validation error is visible inside the modal; modal remains open |
| `modal no customer shows validation error` | `submit` inside modal | Customer field left blank; other fields valid | No API call is made; element containing a customer-required error is visible inside the modal; modal remains open |

---

## Shared Test Setup Notes

The following setup applies to all views in this section and should be placed in a shared `vitest.setup.ts` or per-test `beforeAll`/`afterEach` hooks.

```typescript
// vitest.setup.ts (excerpt)
import { setupServer } from 'msw/node'
import { handlers } from './mocks/handlers'

const server = setupServer(...handlers)

beforeAll(() => server.listen({ onUnhandledRequest: 'warn' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())
```

**Router stub:** Use `createRouter({ history: createMemoryHistory(), routes })` and pass it to `mount` via the `global.plugins` option. Assert navigation via `router.currentRoute.value.path`.

**Debounce testing:** Use `vi.useFakeTimers()` before each debounce test and `vi.advanceTimersByTime(300)` (or the configured debounce value) to trigger the call synchronously. Restore with `vi.useRealTimers()` in `afterEach`.

**Currency formatting:** Assert formatted output with a flexible regex (e.g., `/1[,.]234[.,]50/`) to remain locale-agnostic, or configure the test environment locale explicitly with `Intl` polyfills.

**Dialog/confirmation testing:** If the app uses `window.confirm`, stub it with `vi.spyOn(window, 'confirm').mockReturnValue(true)` (or `false` for cancel tests). If a custom modal component is used, interact with its confirm button via `wrapper.find('[data-testid="confirm-btn"]').trigger('click')`.

**Today's date assertion:** Use `vi.setSystemTime(new Date('2026-04-18'))` in `beforeEach` and `vi.useRealTimers()` in `afterEach` when testing date defaults to ensure deterministic assertions.

**Coverage collection:** Run with `vitest --coverage` using the `@vitest/coverage-v8` provider. The coverage threshold for all files under `src/views/finance/` should be set to `100` for statements, branches, functions, and lines in `vitest.config.ts`.
