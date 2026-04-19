# Section 6c-ii: Frontend — HR, Dashboard, Profile & Layouts — Test Plan

---

## Stack & Coverage Target

Framework: Vitest + @vue/test-utils v2. HTTP calls intercepted with MSW. Coverage target: 100% component branches. Each test mounts the component with `{ global: { plugins: [pinia, router] } }`.

---

## 1. DashboardView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `renders_kpiCards_onMount` | `onMounted` | API returns kpi data | KPI cards for revenue, transactions, customers, low-stock rendered |
| `shows_loading_skeleton_while_fetching` | `onMounted` | API pending | Skeleton loaders visible; KPI values absent |
| `shows_error_banner_on_api_failure` | `onMounted` | API rejects | Error banner with retry button rendered |
| `retry_button_refetches_data` | `click` retry button | After failure, retry clicked | New API call made; data loads on success |
| `revenue_chart_renders_with_data` | `onMounted` | API returns chart data | Revenue chart component present with correct series |
| `top_products_table_renders` | `onMounted` | API returns top-selling products | Top-products table with name, qty, revenue columns |
| `low_stock_panel_shows_items` | `onMounted` | 3 low-stock items | 3 rows in low-stock list |
| `low_stock_panel_empty_state` | `onMounted` | No low-stock items | "All items well-stocked" message |
| `date_range_filter_refetches` | `change` date range selector | User changes to "Last 30 days" | New API call with updated date params |
| `dashboard_links_navigate_correctly` | `click` "View all" links | Click low-stock link | Router navigates to `/inventory/stock` |

---

## 2. EmployeesView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `renders_employee_table_on_mount` | `onMounted` | API returns employees | Table with name, position, phone, status columns |
| `shows_empty_state` | `onMounted` | No employees | "No employees found" message |
| `search_filters_results` | `input` search field | Type "Ali" | Table rows filtered to matching names |
| `status_filter_shows_only_active` | `change` status select | Select "ACTIVE" | Only active employees shown |
| `create_button_navigates_to_form` | `click` "Add Employee" | — | Router pushes to `/hr/employees/new` |
| `row_click_navigates_to_detail` | `click` employee row | — | Router pushes to `/hr/employees/{id}` |
| `edit_button_navigates_to_edit_form` | `click` edit icon | — | Router pushes to `/hr/employees/{id}/edit` |
| `delete_shows_confirmation_dialog` | `click` delete icon | — | Confirmation modal rendered |
| `confirm_delete_removes_row` | `click` confirm in modal | API DELETE succeeds | Employee removed from table |
| `cancel_delete_keeps_row` | `click` cancel in modal | — | Row remains; modal closed |
| `pagination_loads_next_page` | `click` next page | API returns page 2 | Second page of employees rendered |

---

## 3. EmployeeFormView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `create_mode_shows_empty_form` | `onMounted` | Route: `/hr/employees/new` | All fields empty; title "Add Employee" |
| `edit_mode_prefills_form` | `onMounted` | Route: `/hr/employees/5/edit` | Fields prefilled from `getEmployee` API response |
| `edit_mode_shows_not_found_on_404` | `onMounted` | API returns 404 | Redirects to `/hr/employees` |
| `validates_required_first_name` | `submit` | First name empty | Error message under first name field |
| `validates_required_last_name` | `submit` | Last name empty | Error under last name |
| `validates_phone_format` | `submit` | Invalid phone | Error "Invalid phone number" |
| `validates_hire_date_required` | `submit` | No hire date | Error under hire date |
| `create_success_redirects` | `submit` | Valid form; API 201 | Router navigates to `/hr/employees` |
| `update_success_redirects` | `submit` | Valid form; API 200 | Router navigates to `/hr/employees` |
| `cancel_navigates_back` | `click` Cancel | — | Router navigates back to list |

---

## 4. SalaryView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `renders_salary_list_on_mount` | `onMounted` | API returns salary records | Table with employee name, gross, deductions, net columns |
| `month_filter_refetches_data` | `change` month picker | Select March 2026 | New API call with month param |
| `calculate_payroll_button_triggers_calculation` | `click` "Calculate Payroll" | API returns updated salaries | Table refreshes with new values |
| `export_csv_initiates_download` | `click` Export CSV | — | Anchor with `download` attribute triggered |
| `row_shows_advance_deduction` | `onMounted` | Employee has advance | Advance amount in deductions column |

---

## 5. AttendanceView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `renders_attendance_table_on_mount` | `onMounted` | API returns records | Table with employee, check-in, check-out, status |
| `date_filter_refetches` | `change` date picker | Select today | API called with today's date |
| `mark_present_updates_status` | `click` "Mark Present" | API 200 | Status cell updates to PRESENT badge |
| `missing_checkout_highlighted` | `onMounted` | Employee checked in, no checkout | Row highlighted with warning style |

---

## 6. ProfileView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `renders_current_user_info` | `onMounted` | API returns user profile | Name, email, phone, role displayed |
| `edit_name_saves_successfully` | `submit` profile form | Valid name; API 200 | Success toast shown; name updated |
| `change_password_validates_mismatch` | `submit` | New password ≠ confirm | Error "Passwords do not match" |
| `change_password_validates_short` | `submit` | Password < 8 chars | Error about minimum length |
| `change_password_success_toast` | `submit` | Valid passwords; API 200 | Success toast; fields cleared |
| `change_password_wrong_current_shows_error` | `submit` | API 422 on current password | Error "Current password is incorrect" |
| `avatar_upload_preview_shown` | `change` file input | Image file selected | Preview image rendered |
| `avatar_upload_too_large_shows_error` | `change` file input | File > 5MB | Error "File size exceeds 5MB" |

---

## 7. MainLayout.vue & AppSidebar.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `renders_sidebar_with_nav_links` | Mount | Standard user with all permissions | Sidebar nav links for all granted modules |
| `hides_admin_link_without_admin_permission` | Mount | User without ADMIN permission | Admin nav item absent |
| `hides_hr_link_without_hr_permission` | Mount | User without HR permission | HR nav item absent |
| `active_route_link_highlighted` | Mount | Current route `/inventory/products` | Inventory nav item has active class |
| `sidebar_collapse_toggle_works` | `click` collapse button | — | Sidebar collapses; icon-only mode |
| `sidebar_expand_restores_labels` | `click` expand button | Collapsed sidebar | Labels reappear |
| `user_menu_shows_profile_and_logout` | `click` user avatar | — | Dropdown with Profile + Logout |
| `logout_clears_token_and_redirects` | `click` Logout | — | Token removed from storage; router → `/login` |
| `breadcrumb_reflects_current_route` | Route change | Navigate to `/inventory/products` | Breadcrumb shows "Inventory > Products" |
| `notification_badge_shows_count` | Mount | 3 unread notifications | Badge with count "3" on bell icon |

---

## 8. Shared Test Setup Notes

- **MSW handlers** registered in `setupTests.ts`; reset between tests with `server.resetHandlers()`.
- **Pinia** reset between tests via `setActivePinia(createPinia())`.
- **Router** stubs use `createMemoryHistory()` to avoid real navigation side-effects.
- **Fake timers**: `vi.useFakeTimers()` / `vi.runAllTimers()` for debounced search inputs.
- **File upload**: `Object.defineProperty(input, 'files', ...)` to simulate `FileList`.
- **Coverage threshold**: `branches: 100, functions: 100` for all components in `src/views/hr/`, `src/views/dashboard/`, `src/views/profile/`, `src/layouts/`.
