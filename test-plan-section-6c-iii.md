# Section 6c-iii: Frontend — Report Views & E2E Flows — Test Plan

---

## Stack

Unit/component: Vitest + @vue/test-utils v2 + MSW.
E2E: Playwright (or Cypress). E2E tests run against a seeded test database.

---

## 1. ReportsView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `renders_report_list_on_mount` | `onMounted` | API returns report definitions | Table with report name, type, last-run columns |
| `shows_empty_state` | `onMounted` | No reports | "No reports configured" message |
| `run_report_button_triggers_execution` | `click` Run | API returns 202 Accepted | "Queued" status shown; row updates |
| `report_status_COMPLETED_shows_download` | Poll / `onMounted` | Report COMPLETED | Download button enabled |
| `report_status_FAILED_shows_error` | `onMounted` | Report FAILED | Error icon + tooltip with failure reason |
| `report_status_PENDING_shows_spinner` | `onMounted` | Report PENDING | Spinner in status column |
| `download_excel_triggers_export` | `click` Download Excel | — | `window.open` or anchor triggered with xlsx URL |
| `download_pdf_triggers_export` | `click` Download PDF | — | Anchor triggered with PDF URL |
| `filter_by_type_shows_matching` | `change` type filter | Select "SALES" | Only SALES report rows shown |
| `search_by_name_filters_list` | `input` search field | Type "Inventory" | Only matching report names shown |

---

## 2. FinancialReportView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `renders_report_type_tabs` | Mount | — | Tabs: Trial Balance, Income Statement, Balance Sheet, Cash Flow |
| `trial_balance_tab_loads_data` | `click` Trial Balance tab | API returns trial balance | Debit/credit columns with account rows |
| `trial_balance_totals_match` | `onMounted` | Balanced data | Footer row shows equal debit/credit totals |
| `income_statement_shows_revenue_and_expenses` | `click` Income Statement tab | API returns income data | Revenue section + Expenses section + Net Income |
| `balance_sheet_shows_assets_liabilities_equity` | `click` Balance Sheet tab | API returns balance data | Three sections rendered; Assets = Liabilities + Equity |
| `date_range_picker_refetches_report` | `change` date range | New period selected | API called with updated from/to params |
| `export_csv_button_available` | Mount | — | Export CSV button rendered in toolbar |
| `export_triggers_download` | `click` Export | — | File download initiated |

---

## 3. SalesReportView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `renders_summary_kpi_cards` | `onMounted` | API returns summary | Total Sales, Transactions, Average Sale cards shown |
| `chart_renders_with_daily_data` | `onMounted` | Daily grouping selected | Chart with 30 data points rendered |
| `chart_switches_to_monthly` | `click` Monthly grouping | — | API called with MONTHLY; chart re-rendered |
| `top_products_table_sorted_by_revenue` | `onMounted` | API returns top products | Products listed highest revenue first |
| `terminal_filter_refetches_data` | `change` terminal dropdown | Select terminal | API called with terminalId param |
| `date_range_refetches_data` | `change` date picker | New range | API called with new dates |
| `zero_sales_period_shows_empty_chart` | `onMounted` | API returns no sales | Chart shows flat line or empty state message |

---

## 4. InventoryReportView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `renders_stock_on_hand_table` | `onMounted` | API returns stock | Table with product, location, qty, value columns |
| `valuation_total_shown_in_footer` | `onMounted` | API returns valuations | Footer shows sum of all stock values |
| `low_stock_tab_shows_below_reorder` | `click` Low Stock tab | API returns low-stock | Only items below reorder point listed |
| `abc_analysis_tab_shows_classification` | `click` ABC Analysis tab | API returns classifications | A/B/C badges on each product row |
| `warehouse_filter_refetches` | `change` warehouse select | — | API called with warehouseId param |
| `export_triggers_download` | `click` Export | — | Download triggered |

---

## 5. E2E Test Flows (Playwright)

### 5.1 Full POS Sale Flow

| Test Name | Steps | Expected Result |
|---|---|---|
| `e2e_pos_complete_sale_cash` | 1. Login as cashier → 2. Open shift → 3. Search product → 4. Add to cart → 5. Add payment (cash) → 6. Complete sale | Transaction created with COMPLETED status; receipt shown; stock decremented |
| `e2e_pos_apply_coupon_and_complete` | 1. Login → 2. Open shift → 3. Add items → 4. Enter coupon code → 5. Verify discount → 6. Complete | Discount applied; coupon usageCount incremented |
| `e2e_pos_void_transaction` | 1. Login → 2. Create draft transaction → 3. Void | Transaction status VOIDED; items not deducted |
| `e2e_pos_close_shift_with_summary` | 1. Login → 2. Open shift → 3. Complete 2 sales → 4. Close shift | Shift CLOSED; summary shows correct totals |

### 5.2 Full Inventory Flow

| Test Name | Steps | Expected Result |
|---|---|---|
| `e2e_create_po_and_receive` | 1. Login as inventory manager → 2. Create PO → 3. Release PO → 4. Create receiving order → 5. Receive all lines → 6. Complete receiving | Stock increased by received quantities |
| `e2e_inventory_count_with_adjustment` | 1. Login → 2. Create count → 3. Start count → 4. Record counts (one variance) → 5. Complete | Adjustment movement created for variance |
| `e2e_stock_transfer_between_locations` | 1. Login → 2. Source has 20 units → 3. Transfer 8 to destination → 4. Verify both locations | Source: 12 units; destination: 8 units |

### 5.3 Finance Flow

| Test Name | Steps | Expected Result |
|---|---|---|
| `e2e_create_ar_invoice_and_receive_payment` | 1. Login as accountant → 2. Create AR invoice → 3. Approve → 4. Post → 5. Record payment | Invoice status FULLY_PAID; GL journal entry created |
| `e2e_create_ap_invoice_and_make_payment` | 1. Login → 2. Create AP invoice for vendor → 3. Approve → 4. Post → 5. Make payment | Invoice balance reduced; payment recorded |
| `e2e_journal_entry_and_trial_balance` | 1. Login → 2. Create balanced journal entry → 3. Post → 4. View trial balance | Trial balance reflects posted entry; DR = CR |

### 5.4 Authentication & Authorization

| Test Name | Steps | Expected Result |
|---|---|---|
| `e2e_login_and_access_permitted_module` | 1. Login as inventory user → 2. Navigate to /inventory/products | Products list rendered |
| `e2e_access_denied_for_unauthorized_module` | 1. Login as inventory user → 2. Navigate to /hr/employees | Redirected to 403 or dashboard |
| `e2e_token_expiry_redirects_to_login` | 1. Login → 2. Expire token (mock) → 3. Make API call | Redirected to /login |
| `e2e_password_reset_flow` | 1. Go to /forgot-password → 2. Enter email → 3. Open reset link → 4. Set new password → 5. Login | Login succeeds with new password |

### 5.5 HR Flow

| Test Name | Steps | Expected Result |
|---|---|---|
| `e2e_create_employee_and_calculate_salary` | 1. Login as HR → 2. Create employee → 3. Set salary → 4. Mark attendance → 5. Calculate payroll | Net salary computed correctly |
| `e2e_advance_deducted_from_salary` | 1. Create employee → 2. Record advance → 3. Calculate salary | Advance shown in deductions; net reduced |

---

## 6. Shared E2E Setup Notes

- **Database seeding**: `beforeEach` calls seed script that inserts a test tenant, admin user, and baseline products/accounts.
- **Auth**: `storageState` reused per role (admin, cashier, accountant, inventory-manager) to avoid repeated logins.
- **Teardown**: `afterEach` truncates transaction/movement tables; static reference data (products, accounts) persists across tests in a suite.
- **Selectors**: All interactive elements have `data-testid` attributes; E2E tests use only `data-testid` selectors, never CSS classes.
- **Flake prevention**: `waitForResponse` used for all API-driven state changes before asserting DOM.
