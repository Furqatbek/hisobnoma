# Section 6a-ii: Frontend — Admin Views — Test Plan

**Stack:** Vitest + @vue/test-utils + MSW  
**Goal:** 100% component test coverage for all admin views  
**Mount strategy:** `mountComponent` via `mount()` with a stubbed `vue-router` and MSW handlers intercepting all API calls at the network layer. Each table row represents one `it()` block.

---

## UsersView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Fetches users on mount with page=0 | `onMounted` | Component mounts; MSW returns a page of users | `getUsers` called once with `{ page: 0 }`; user rows rendered in `<table>` |
| Renders correct table columns | `onMounted` | MSW returns user list | Table header contains columns: Username, Email, Phone, Roles, Status, Actions |
| Renders username cell | `onMounted` | MSW returns user with `username: "alice"` | `<td>` containing `"alice"` present in first row |
| Renders email cell | `onMounted` | MSW returns user with `email: "alice@example.com"` | `<td>` containing `"alice@example.com"` present |
| Renders phone cell | `onMounted` | MSW returns user with `phone: "+998901234567"` | `<td>` containing `"+998901234567"` present |
| Renders roles cell | `onMounted` | MSW returns user with `roles: ["ADMIN","CASHIER"]` | `<td>` displays both role names |
| Renders active status badge | `onMounted` | MSW returns user with `status: "ACTIVE"` | Status badge with text `"active"` (case-insensitive) rendered |
| Renders locked status badge | `onMounted` | MSW returns user with `status: "LOCKED"` | Status badge with text `"locked"` rendered |
| Renders action icons per row | `onMounted` | MSW returns user list | Each row contains edit icon, delete icon, and lock icon |
| Search input triggers debounced API call | `@input` on search field | User types `"bob"` into search input; wait debounce | `getUsers` called with `{ page: 0, search: "bob" }`; table updated with filtered results |
| Search clears results and re-fetches | `@input` on search field | User clears search input | `getUsers` called with `{ page: 0, search: "" }`; original list restored |
| "Create User" button navigates to user form | `@click` on Create User button | Button clicked | `router.push` called with `"/admin/user-form"` |
| Edit icon navigates to user form with id | `@click` on edit icon | Edit icon clicked on row with `userId: 42` | `router.push` called with `"/admin/user-form?id=42"` |
| Delete icon shows confirmation dialog | `@click` on delete icon | Delete icon clicked on a row | Confirmation dialog element becomes visible in DOM |
| Confirmation dialog "Yes" deletes user | `@click` on confirm Yes button | Dialog shown; user clicks "Yes"; MSW handles `deleteUser` | `deleteUser` API called with correct userId; deleted user's row removed from table |
| Confirmation dialog "Cancel" leaves table intact | `@click` on confirm Cancel button | Dialog shown; user clicks "Cancel" | No API call made; all rows remain; dialog closes |
| Lock icon calls lockUser and toggles badge | `@click` on lock icon | Lock icon clicked on ACTIVE user; MSW returns updated user | `lockUser` API called with userId; status badge toggles to `"locked"` |
| Lock icon on LOCKED user unlocks and toggles badge | `@click` on lock icon | Lock icon clicked on LOCKED user; MSW returns updated user | `lockUser` (or `unlockUser`) called; status badge toggles to `"active"` |
| Next page calls getUsers with page+1 | `@click` on next-page button | Pagination next button clicked | `getUsers` called with `{ page: 1 }`; new page of users rendered |
| Previous page calls getUsers with page-1 | `@click` on prev-page button | On page 2; previous button clicked | `getUsers` called with `{ page: 1 }` |
| Empty user list shows empty state message | `onMounted` | MSW returns `{ data: [], total: 0 }` | Element with text `"No users found"` (case-insensitive) visible; table body empty |

---

## UserFormView.vue — Create Mode (no `id` query param)

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Renders empty username field | `onMounted` | No id param; component mounts | `input[name="username"]` (or equivalent) present with empty value |
| Renders empty firstName field | `onMounted` | No id param | `input[name="firstName"]` present with empty value |
| Renders empty lastName field | `onMounted` | No id param | `input[name="lastName"]` present with empty value |
| Renders empty email field | `onMounted` | No id param | `input[name="email"]` or `input[type="email"]` present with empty value |
| Renders empty phone field | `onMounted` | No id param | `input[name="phone"]` present with empty value |
| Renders empty password field | `onMounted` | No id param | `input[type="password"][name="password"]` present with empty value |
| Renders empty confirmPassword field | `onMounted` | No id param | `input[type="password"][name="confirmPassword"]` (or equivalent) present with empty value |
| Loads roles from getRoles API on mount | `onMounted` | MSW returns roles list with 3 roles | `getRoles` called once; 3 role checkboxes rendered |
| Submit valid form calls createUser | `@submit` | All fields filled validly; MSW 201 from createUser | `createUser` POST called with correct payload; `router.push("/admin/users")` called |
| Duplicate username (409) shows error | `@submit` | MSW returns 409 on createUser | Error message `"Username already taken"` (case-insensitive) visible; navigation not called |
| Mismatched passwords blocks API call | `@submit` | password ≠ confirmPassword | Validation error visible; `createUser` API not called |
| Empty username shows validation error | `@submit` | username field left blank | Validation error `"Username required"` (case-insensitive) visible; API not called |
| Invalid email format shows validation error | `@submit` | Email field contains `"not-an-email"` | Validation error for email visible; API not called |
| Checked role included in createUser request | `@change` on role checkbox | Role checkbox checked; form submitted | `createUser` payload contains that role's id/code |
| Unchecked role excluded from createUser request | `@change` on role checkbox | Role checkbox unchecked; form submitted | `createUser` payload does not contain that role's id/code |
| Multiple roles can be selected simultaneously | `@change` on role checkboxes | Two role checkboxes checked | `createUser` payload contains both roles |

---

## UserFormView.vue — Edit Mode (`id` param present)

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Calls getUser on mount with id from query param | `onMounted` | Route query has `id=7`; MSW returns user | `getUser` called with `7`; no error shown |
| Pre-fills username field | `onMounted` | MSW returns user with `username: "alice"` | `input[name="username"]` has value `"alice"` |
| Pre-fills firstName field | `onMounted` | MSW returns user with `firstName: "Alice"` | `input[name="firstName"]` has value `"Alice"` |
| Pre-fills lastName field | `onMounted` | MSW returns user with `lastName: "Smith"` | `input[name="lastName"]` has value `"Smith"` |
| Pre-fills email field | `onMounted` | MSW returns user with `email: "alice@example.com"` | Email input has value `"alice@example.com"` |
| Pre-fills phone field | `onMounted` | MSW returns user with `phone: "+998901234567"` | Phone input has value `"+998901234567"` |
| Password field shows keep-current hint | `onMounted` | Edit mode | Element with text matching `"leave blank to keep current"` (case-insensitive) visible near password field |
| Submit without changing password omits password from payload | `@submit` | Password field left blank; MSW 200 from updateUser | `updateUser` called; request body does not contain `password` key |
| Submit with new password sends password | `@submit` | Password fields filled with new matching values | `updateUser` called with `password` field in payload |
| Role checkboxes pre-checked for user's existing roles | `onMounted` | MSW user has `roles: ["ADMIN"]`; getRoles returns ADMIN + CASHIER | ADMIN checkbox is checked; CASHIER checkbox is unchecked |
| Submit calls updateUser and navigates | `@submit` | Valid data; MSW 200 | `updateUser` called with userId and updated payload; `router.push("/admin/users")` called |
| API 404 on getUser shows error and redirects | `onMounted` | MSW returns 404 for getUser | Error message visible; `router.push("/admin/users")` called |

---

## RolesView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Fetches roles on mount | `onMounted` | MSW returns list of roles | `getRoles` called once; role rows rendered |
| Renders role name column | `onMounted` | MSW returns role with `name: "Admin"` | `<td>` with `"Admin"` visible |
| Renders role code column | `onMounted` | MSW returns role with `code: "ADMIN"` | `<td>` with `"ADMIN"` visible |
| Renders system role badge | `onMounted` | MSW returns role with `systemRole: true` | System role badge/indicator visible in that row |
| Renders user count column | `onMounted` | MSW returns role with `userCount: 5` | `<td>` with `"5"` visible |
| "Create Role" button navigates | `@click` on Create Role | Button clicked | `router.push("/admin/role-form")` called |
| Edit button navigates with roleId | `@click` on edit button | Edit clicked on row with `roleId: 3` | `router.push("/admin/role-form?id=3")` called |
| Delete button on non-system role shows confirmation | `@click` on delete button | Non-system role row; delete clicked | Confirmation dialog visible |
| Confirm delete calls deleteRole and removes row | `@click` confirm Yes | MSW handles deleteRole | `deleteRole` called with roleId; row removed from table |
| Cancel delete makes no API call | `@click` confirm Cancel | Dialog open; Cancel clicked | No API call; row remains; dialog closes |
| Delete button on system role is disabled | `onMounted` | MSW returns role with `systemRole: true` | Delete button in system role row has `disabled` attribute or is absent |
| System role delete attempt does not trigger dialog | `@click` on disabled delete | System role delete clicked | Confirmation dialog does not appear |

---

## RoleFormView.vue — Create Mode

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Renders empty name field | `onMounted` | No id param | `input[name="name"]` present with empty value |
| Renders empty code field | `onMounted` | No id param | `input[name="code"]` present with empty value |
| Renders empty description field | `onMounted` | No id param | `textarea[name="description"]` or equivalent present with empty value |
| Loads permissions from listAllPermissions on mount | `onMounted` | MSW returns permissions grouped by module | `listAllPermissions` called; checkboxes rendered grouped by module |
| Permissions are visually grouped by module | `onMounted` | MSW returns permissions in 2 modules | Two distinct group headings visible with corresponding checkboxes |
| Submit valid data calls createRole | `@submit` | All fields filled; MSW 201 | `createRole` called with correct payload; `router.push("/admin/roles")` called |
| Duplicate code (409) shows error | `@submit` | MSW returns 409 | Error message visible; navigation not called |
| Code auto-slugifies from name (spaces to underscores, uppercase) | `@input` on name field | User types `"super admin"` into name | Code field value becomes `"SUPER_ADMIN"` |
| Code auto-slugifies special characters | `@input` on name field | User types `"role-name!"` | Code field sanitized appropriately (letters, underscores, uppercase) |
| Checking all permissions in a group includes them in request | `@change` on permission checkboxes | All checkboxes in module group checked; form submitted | `createRole` payload contains all permission ids from that group |
| Unchecked permissions excluded from request | `@change` on permission checkbox | One checkbox unchecked | `createRole` payload does not contain that permission id |

---

## RoleFormView.vue — Edit Mode

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Calls getRole on mount with id | `onMounted` | Route query `id=5`; MSW returns role | `getRole` called with `5` |
| Pre-fills name field | `onMounted` | MSW role has `name: "Manager"` | Name input has value `"Manager"` |
| Pre-fills code field | `onMounted` | MSW role has `code: "MANAGER"` | Code input has value `"MANAGER"` |
| Pre-fills description field | `onMounted` | MSW role has `description: "Manages stuff"` | Description field has value `"Manages stuff"` |
| Permissions pre-checked for assigned permissions | `onMounted` | MSW role has `permissions: ["READ_USERS"]`; listAllPermissions returns READ_USERS + WRITE_USERS | READ_USERS checkbox checked; WRITE_USERS checkbox unchecked |
| Submit calls updateRole with updated permissions | `@submit` | User checks additional permission; submits; MSW 200 | `updateRole` called with updated permissions list including newly added permission |
| 404 from getRole shows error and redirects | `onMounted` | MSW returns 404 | Error message visible; `router.push("/admin/roles")` called |

---

## AuditLogsView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Fetches audit logs on mount with default params | `onMounted` | MSW returns log list | `getAuditLogs` called with default params; rows rendered |
| Renders timestamp column | `onMounted` | MSW returns log with `timestamp: "2026-04-17T10:00:00Z"` | Formatted timestamp visible in table |
| Renders user column | `onMounted` | MSW returns log with `user: "alice"` | `"alice"` visible in table row |
| Renders action column | `onMounted` | MSW returns log with `action: "LOGIN"` | `"LOGIN"` visible in table row |
| Renders module column | `onMounted` | MSW returns log with `module: "AUTH"` | `"AUTH"` visible in table row |
| Renders entity column | `onMounted` | MSW returns log with `entity: "User"` | `"User"` visible in table row |
| Renders success indicator | `onMounted` | MSW returns log with `success: true` | Success indicator (badge/icon) visible |
| Renders failure indicator | `onMounted` | MSW returns log with `success: false` | Failure indicator visible |
| Date range filter triggers API call with start date | `@change` on start date picker | Start date selected | `getAuditLogs` called with `startDate` param |
| Date range filter triggers API call with end date | `@change` on end date picker | End date selected | `getAuditLogs` called with `endDate` param |
| Both dates set triggers API call with both params | `@change` on both date pickers | Both dates selected | `getAuditLogs` called with both `startDate` and `endDate` |
| User filter calls getAuditLogsByUser | `@change` on user dropdown | User selected from dropdown | `getAuditLogsByUser` called with selected userId; table updated |
| Action filter calls getAuditLogsByAction | `@change` on action dropdown | Action selected | `getAuditLogsByAction` called with selected action; table updated |
| Module filter calls getAuditLogsByModule | `@change` on module dropdown | Module selected | `getAuditLogsByModule` called with selected module; table updated |
| "Failed only" toggle calls getFailedActions | `@change` on failed-only toggle | Toggle switched on | `getFailedActions` called; table shows only failed entries |
| "Failed only" toggle off restores default | `@change` on failed-only toggle | Toggle switched off | `getAuditLogs` called with default params |
| Pagination next page works in default view | `@click` next-page button | No filters; next page clicked | `getAuditLogs` called with incremented page param |
| Pagination works with user filter active | `@click` next-page button | User filter active; next page clicked | `getAuditLogsByUser` called with incremented page and userId |
| Pagination works with action filter active | `@click` next-page button | Action filter active; next page clicked | `getAuditLogsByAction` called with incremented page |
| Pagination works with failed-only filter active | `@click` next-page button | Failed-only toggle on; next page clicked | `getFailedActions` called with incremented page |
| Empty result shows empty state message | `onMounted` | MSW returns empty list | Element with text `"No audit logs found"` (case-insensitive) visible |

---

## SettingsView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Loads all settings on mount | `onMounted` | MSW returns settings list | `getAllSettings` called once; settings rendered |
| Settings grouped by category | `onMounted` | MSW returns settings in 2 categories | Two section/tab headings corresponding to categories visible |
| Renders setting key | `onMounted` | MSW returns setting with `key: "MAX_RETRIES"` | `"MAX_RETRIES"` visible in table/list |
| Renders current string value | `onMounted` | MSW returns STRING setting with `value: "hello"` | `"hello"` visible in the row |
| Renders type label | `onMounted` | MSW returns setting with `type: "NUMBER"` | `"NUMBER"` label visible in row |
| Renders edit button per row | `onMounted` | MSW returns settings | Each row has an edit button |
| Click edit on STRING setting shows inline text input | `@click` on edit button | STRING type setting | Inline `<input type="text">` appears with current value |
| Click edit on NUMBER setting shows inline number input | `@click` on edit button | NUMBER type setting | Inline `<input type="number">` (or text) appears with current value |
| Click edit on BOOLEAN setting shows toggle switch | `@click` on edit button | BOOLEAN type setting | Toggle switch (not text input) appears reflecting current boolean value |
| Save changed STRING value calls updateSettingValue | `@click` on save after editing | Value changed; save clicked; MSW 200 | `updateSettingValue` called with `{ key, value: newValue }`; input collapses |
| Save changed BOOLEAN value calls updateSettingValue | toggle switch changed + save | Boolean toggled; MSW 200 | `updateSettingValue` called with `{ key, value: newBooleanValue }` |
| Save changed NUMBER value calls updateSettingValue | inline input changed + save | Number changed; MSW 200 | `updateSettingValue` called with correct numeric value |
| Unsaved changes indicator appears when value modified | `@input` on inline field | Value typed but not yet saved | Unsaved changes indicator element visible in DOM |
| Unsaved changes indicator disappears after save | `@click` save | After save completes | Unsaved changes indicator no longer visible |
| "Batch Update" button sends all changed values | `@click` on Batch Update | Multiple settings modified; Batch Update clicked; MSW 200 | `updateSettings` called with array/object containing all changed key-value pairs |
| Batch Update does not send unchanged settings | `@click` on Batch Update | Only one setting modified | `updateSettings` payload contains only the modified setting |

---

## TerminalsView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Fetches terminals on mount | `onMounted` | MSW returns terminal list | API called; terminal rows rendered |
| Renders code column | `onMounted` | MSW returns terminal with `code: "T-01"` | `"T-01"` visible in table |
| Renders name column | `onMounted` | MSW returns terminal with `name: "Main Terminal"` | `"Main Terminal"` visible |
| Renders location column | `onMounted` | MSW returns terminal with `location: "Branch A"` | `"Branch A"` visible |
| Renders active status | `onMounted` | MSW returns terminal with `status: "ACTIVE"` | Active status indicator visible |
| Renders inactive status | `onMounted` | MSW returns terminal with `status: "INACTIVE"` | Inactive status indicator visible |
| "Create Terminal" button navigates | `@click` | Button clicked | `router.push("/admin/terminal-form")` called |
| Edit button navigates with id | `@click` on edit | Row with `id: 10`; edit clicked | `router.push("/admin/terminal-form?id=10")` called |
| Activate toggle calls activateTerminal | `@click` on toggle | Terminal with `status: "INACTIVE"`; toggle clicked; MSW 200 | `activateTerminal` called with terminal id; status updated to active |
| Deactivate toggle calls deactivateTerminal | `@click` on toggle | Terminal with `status: "ACTIVE"`; toggle clicked; MSW 200 | `deactivateTerminal` called with terminal id; status updated to inactive |

---

## TerminalFormView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Renders empty code field in create mode | `onMounted` | No id param | `input[name="code"]` present with empty value |
| Renders empty name field in create mode | `onMounted` | No id param | `input[name="name"]` present with empty value |
| Renders location dropdown in create mode | `onMounted` | No id param | Location `<select>` or dropdown component present |
| Loads locations from API on mount | `onMounted` | MSW returns locations list | Locations API called; dropdown options populated |
| Submit create calls createTerminal and redirects | `@submit` | Valid data; MSW 201 | `createTerminal` called with payload; `router.push("/admin/terminals")` called |
| Submit edit calls updateTerminal and redirects | `@submit` | Edit mode; valid data; MSW 200 | `updateTerminal` called with id and payload; `router.push("/admin/terminals")` called |
| Pre-fills code in edit mode | `onMounted` | MSW returns terminal with `code: "T-02"` | Code input has value `"T-02"` |
| Pre-fills name in edit mode | `onMounted` | MSW returns terminal with `name: "East Terminal"` | Name input has value `"East Terminal"` |
| Pre-selects location in edit mode | `onMounted` | MSW returns terminal with `locationId: 3` | Location dropdown shows location id 3 selected |
| Duplicate code (409) shows error | `@submit` | MSW returns 409 | Error message visible; navigation not called |

---

## RegionsView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Lists regions on mount | `onMounted` | MSW returns regions list | Regions API called; region rows rendered |
| Renders region name | `onMounted` | MSW returns region with `name: "North Region"` | `"North Region"` visible in table |
| "Create Region" button navigates | `@click` | Button clicked | `router.push("/admin/region-form")` called |
| Edit button navigates with id | `@click` on edit | Row with `id: 2`; edit clicked | `router.push("/admin/region-form?id=2")` called |
| Delete shows confirmation dialog | `@click` on delete | Delete clicked on a row | Confirmation dialog visible |
| Confirm delete calls deleteRegion and removes row | `@click` confirm Yes | MSW handles deleteRegion | `deleteRegion` called with regionId; row removed |
| Cancel delete makes no API call | `@click` confirm Cancel | Dialog open; Cancel clicked | No API call; row remains |

---

## RegionFormView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Renders empty name field in create mode | `onMounted` | No id param | Name input present with empty value |
| Renders active checkbox in create mode | `onMounted` | No id param | Active checkbox present (default checked or unchecked per spec) |
| Create calls createRegion and redirects | `@submit` | Valid name; MSW 201 | `createRegion` called with payload; `router.push("/admin/regions")` called |
| Edit loads data on mount | `onMounted` | Route has `id=4`; MSW returns region | Region API called with `4`; fields pre-filled |
| Pre-fills name in edit mode | `onMounted` | MSW region `name: "South Region"` | Name input value is `"South Region"` |
| Pre-checks active checkbox when region is active | `onMounted` | MSW region `active: true` | Active checkbox is checked |
| Pre-unchecks active checkbox when region is inactive | `onMounted` | MSW region `active: false` | Active checkbox is unchecked |
| Edit calls updateRegion and redirects | `@submit` | Valid data; MSW 200 | `updateRegion` called with id and payload; `router.push("/admin/regions")` called |

---

## VillagesView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Lists villages on mount | `onMounted` | MSW returns villages list | Villages API called; village rows rendered |
| Renders village name | `onMounted` | MSW returns village with `name: "Yangi Hayot"` | `"Yangi Hayot"` visible in table |
| Region filter dropdown present | `onMounted` | Component mounts | Region filter `<select>` or dropdown visible |
| Selecting region filter updates village list | `@change` on region filter | Region selected; MSW returns filtered villages | API called with `regionId` param; table updated |
| Clearing region filter shows all villages | `@change` on region filter | Region filter cleared | API called without regionId; all villages shown |
| "Create Village" button navigates | `@click` | Button clicked | `router.push("/admin/village-form")` called |
| Edit button navigates with id | `@click` on edit | Row with `id: 8`; edit clicked | `router.push("/admin/village-form?id=8")` called |
| Delete shows confirmation dialog | `@click` on delete | Delete clicked | Confirmation dialog visible |
| Confirm delete calls deleteVillage and removes row | `@click` confirm Yes | MSW handles deleteVillage | `deleteVillage` called; row removed |
| Cancel delete makes no API call | `@click` confirm Cancel | Dialog open; Cancel clicked | No API call; row remains |

---

## VillageFormView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Renders empty name field in create mode | `onMounted` | No id param | Name input present and empty |
| Renders region dropdown in create mode | `onMounted` | No id param | Region `<select>` or dropdown present |
| Loads regions from API on mount | `onMounted` | MSW returns regions | Regions API called; dropdown options populated |
| Renders active checkbox | `onMounted` | No id param | Active checkbox present |
| Create calls createVillage and redirects | `@submit` | Valid data with region selected; MSW 201 | `createVillage` called with payload; `router.push("/admin/villages")` called |
| Edit loads village data on mount | `onMounted` | Route has `id=9`; MSW returns village | Village API called; fields pre-filled |
| Pre-fills name in edit mode | `onMounted` | MSW village `name: "Old Town"` | Name input value is `"Old Town"` |
| Pre-selects region in edit mode | `onMounted` | MSW village `regionId: 2` | Region dropdown shows region 2 selected |
| Pre-checks active in edit mode | `onMounted` | MSW village `active: true` | Active checkbox is checked |
| Edit calls updateVillage and redirects | `@submit` | Valid data; MSW 200 | `updateVillage` called; `router.push("/admin/villages")` called |
| Region required validation error if not selected | `@submit` | Region dropdown left unselected | Validation error for region visible; API not called |

---

## SmsAdminView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Lists SMS templates on mount | `onMounted` | MSW returns templates list | Templates API called; template entries rendered |
| Renders template name | `onMounted` | MSW returns template with `name: "Welcome"` | `"Welcome"` visible in list |
| Renders template code | `onMounted` | MSW returns template with `code: "WELCOME_MSG"` | `"WELCOME_MSG"` visible |
| "Create Template" button shows create form/modal | `@click` on Create Template | Button clicked | Inline form or modal with name, code, body fields becomes visible |
| Create form contains name field | `@click` on Create Template | Form/modal opened | Name input visible in form |
| Create form contains code field | `@click` on Create Template | Form/modal opened | Code input visible in form |
| Create form contains body field | `@click` on Create Template | Form/modal opened | Body textarea visible in form |
| Body field shows character count | `@input` on body textarea | User types into body | Character count indicator updates in real time |
| Body field shows placeholder syntax hint | `onMounted` (or form open) | Form visible | Hint text containing `{variable}` or similar placeholder syntax visible |
| Submit create calls createTemplate and adds to list | `@submit` | Valid name/code/body; MSW 201 | `createTemplate` called; new template appears in list without full page reload |
| Edit template opens pre-filled modal | `@click` on edit | MSW returns template data | Modal/form appears with existing name, code, body pre-filled |
| Edit submit calls updateTemplate | `@submit` in edit modal | Updated body; MSW 200 | `updateTemplate` called with id and new payload; list updated |
| Delete shows confirmation dialog | `@click` on delete | Delete clicked on a template | Confirmation dialog visible |
| Confirm delete calls deleteTemplate | `@click` confirm Yes | MSW handles deleteTemplate | `deleteTemplate` called; template removed from list |
| Cancel delete makes no API call | `@click` confirm Cancel | Dialog shown; Cancel clicked | No API call; template remains |
| "Send Test SMS" section present | `onMounted` | Component mounts | Send Test SMS section with phone input and message field visible |
| Send Test SMS calls sendSms | `@click` send test | Phone and message filled; MSW 200 | `sendSms` called with phone and message; success toast shown |

---

## TelegramAdminView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Calls status API on mount | `onMounted` | MSW returns bot status | Status API called once on mount |
| Renders CONNECTED status badge (green) | `onMounted` | MSW returns `{ status: "CONNECTED" }` | Green badge / indicator with text `"CONNECTED"` visible |
| Renders DISCONNECTED status badge (red) | `onMounted` | MSW returns `{ status: "DISCONNECTED" }` | Red badge / indicator with text `"DISCONNECTED"` visible |
| "Send Message" section has userId input | `onMounted` | Component mounts | userId input field present in Send Message section |
| "Send Message" section has message textarea | `onMounted` | Component mounts | Message `<textarea>` present |
| Submit send message calls sendTelegramMessage | `@click` send | userId and message filled; MSW 200 | `sendTelegramMessage` called with `{ userId, message }`; success toast shown |
| Invalid userId (404) shows error | `@click` send | MSW returns 404 for sendTelegramMessage | Error message visible; no success toast |
| "Send Daily Report" button present | `onMounted` | Component mounts | Send Daily Report button visible |
| "Send Daily Report" calls sendReport | `@click` | Button clicked; MSW 200 | `sendReport` API called; success toast shown |
| sendReport API error shows error message | `@click` | MSW returns 500 for sendReport | Error message visible; no success toast |
