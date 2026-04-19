# Section 6b-i: Frontend — Inventory Views — Test Plan

**Testing Stack:** Vitest + @vue/test-utils + MSW  
**Coverage Goal:** 100% component test coverage for all inventory-related Vue.js views  
**Platform:** Hisobnoma SaaS

---

## Overview

This section covers unit and integration-level component tests for every view under the Inventory module. Each test is designed to be executed in a Vitest environment with `@vue/test-utils` for DOM mounting/interaction and MSW (Mock Service Worker) for intercepting HTTP calls at the network layer. All tests are isolated per component; shared MSW handlers are reset between tests via `afterEach`.

---

## ProductsView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `mount calls getProducts with page=0` | `onMounted` | Component mounts with no route params | MSW handler for `GET /products?page=0` is called exactly once; table renders with returned rows |
| `table renders required columns` | Render | MSW returns a product list with all fields populated | Table headers contain SKU, Name, Category, Brand, Price, Status, Actions; each row cell maps to the correct field |
| `search input triggers debounced API call` | `input` event on search field | User types "Widget" into the search input; wait 300 ms debounce | `GET /products?search=Widget&page=0` is called once; previous full-list rows are replaced by filtered results |
| `rapid typing only triggers one debounced call` | Multiple `input` events in quick succession | User types "W", "Wi", "Wid" within 100 ms each | Only one API call fires after debounce settles; intermediate queries are not dispatched |
| `Active Only toggle calls getActiveProducts` | `change` event on toggle | User enables the "Active Only" toggle | `GET /products/active` is called; rows with status `INACTIVE` are absent from the rendered table |
| `Active Only off reverts to full list` | `change` event on toggle (disable) | User disables the "Active Only" toggle after enabling it | `GET /products?page=0` is called again; inactive rows reappear |
| `click product row navigates to detail` | `click` on table row | User clicks the row for product with id `42` | `router.push` is called with `/inventory/products/42` |
| `Create Product button navigates to create form` | `click` on "Create Product" button | User clicks the "Create Product" button | `router.push` is called with `/inventory/products/create` |
| `deactivate button shows confirmation dialog` | `click` on Deactivate action button for a row | User clicks the Deactivate button on an active product row | A confirmation dialog (modal or native confirm) is rendered/visible; no API call has been made yet |
| `confirm deactivate calls API and shows Inactive badge` | `click` confirm in dialog | User confirms the deactivation dialog | `PATCH /products/42/deactivate` is called; the row's status cell now contains an "Inactive" badge element |
| `cancel deactivate makes no API call` | `click` cancel in dialog | User dismisses the confirmation dialog | No `PATCH /products/.*/deactivate` request is recorded by MSW; row status badge remains "Active" |
| `pagination next calls getProducts with page=1` | `click` on Next page control | Current page is 0; user clicks Next | `GET /products?page=1` is called; table rows update to page-2 data |
| `pagination prev calls getProducts with page=0` | `click` on Previous page control | Current page is 1; user clicks Previous | `GET /products?page=0` is called; table rows update to page-1 data |
| `empty list shows empty state message` | Render after mount | MSW returns `{ data: [], total: 0 }` | Element with text "No products found" is present in the DOM; table body has no `<tr>` data rows |

---

## ProductFormView.vue — Create Mode

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `renders four tabs in create mode` | Render | Component mounted without an `id` route param | Tab elements or tab-panel headers labelled General, Images, UOMs, Vendors are all present in the DOM |
| `General tab contains all required fields` | Render (General tab active by default) | Component mounted in create mode | Inputs/selects for name, SKU, barcode, category, brand, description, sale price, cost price; checkboxes for is_sellable and is_purchasable are all present |
| `SKU field shows Auto-generated placeholder` | Render | General tab rendered in create mode | SKU input has `placeholder` attribute equal to "Auto-generated" |
| `category dropdown loads from getCategoryTree on mount` | `onMounted` | Component mounts | `GET /categories/tree` is called once; category options in the dropdown match the MSW-returned tree leaf names |
| `brand dropdown loads from getActiveBrands on mount` | `onMounted` | Component mounts | `GET /brands/active` is called once; brand options match MSW-returned brand list |
| `submit valid data calls createProduct and redirects` | `submit` form event | All required fields filled; user submits | `POST /products` is called with correct payload; `router.push('/inventory/products')` is invoked |
| `submit with empty name shows validation error` | `submit` form event | Name field is left empty; user submits | No API call is made; element containing text "Name is required" is visible in the DOM |
| `API 409 on duplicate SKU shows error message` | `submit` form event | MSW returns 409 for `POST /products`; payload contains duplicate SKU | Error message "SKU already exists" is visible; user remains on the form |
| `Images tab: file input renders and accepts files` | `click` Images tab, then `change` on file input | User switches to Images tab; selects two image files | Two thumbnail preview elements are rendered in the images grid |
| `upload button calls uploadImage API` | `click` upload button after file selected | User selects a file and clicks the upload button | `POST /products/{id}/images` (or equivalent upload endpoint) is called; returned image appears in the grid with a delete icon |
| `delete image calls deleteImage and removes from grid` | `click` delete icon on an image | Uploaded image is present in grid; user clicks its delete icon | `DELETE /products/{id}/images/{imageId}` is called; the image element is removed from the grid |
| `Set Primary calls setPrimaryImage and shows star icon` | `click` "Set Primary" button on an image | Multiple images present; user clicks "Set Primary" on image with id `7` | `PATCH /products/{id}/images/7/primary` is called; a star icon class/attribute is applied to that image element and removed from others |
| `UOMs tab shows base UOM read-only and Add UOM row` | `click` UOMs tab | Component rendered in create mode; MSW returns a base UOM | Base UOM row is rendered as read-only (no editable input); "Add UOM" row with a UOM dropdown and conversion factor input is present |
| `Save UOM row calls addUom and appends new row` | `click` save on Add UOM row | User selects a UOM and enters conversion factor `2.5`, then saves | `POST /products/{id}/uoms` called with correct payload; a new row containing the selected UOM appears in the table |
| `Remove UOM row calls removeUom` | `click` remove on a non-base UOM row | A non-base UOM row exists; user clicks its remove button | `DELETE /products/{id}/uoms/{uomId}` is called; the row is removed from the DOM |
| `Remove button disabled on base UOM row` | Render | UOMs tab rendered with a base UOM row | The remove/delete button on the base UOM row has a `disabled` attribute or is absent |
| `Vendors tab Add Vendor saves and shows row` | `click` save on Add Vendor row | User selects a vendor and enters a price, then saves | `POST /products/{id}/vendors` called with vendor id and price; new vendor row appears in the list |
| `Remove vendor calls removeVendor` | `click` remove on vendor row | A vendor row exists; user clicks its remove button | `DELETE /products/{id}/vendors/{vendorId}` is called; the vendor row is removed |

---

## ProductFormView.vue — Edit Mode

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `getProduct called on mount with route id` | `onMounted` | Component mounted with route param `id=99` | `GET /products/99` is called exactly once on mount |
| `General fields pre-filled with product data` | Render after mount | MSW returns product with name "Bolt M8", SKU "BLT-M8", price 1.50 | Name input value is "Bolt M8"; SKU input value is "BLT-M8"; price input value is "1.50" |
| `category and brand dropdowns show pre-selected values` | Render after mount | MSW returns product with category id `3` and brand id `7` | Category dropdown selected option matches the name for id 3; brand dropdown selected option matches the name for id 7 |
| `Images tab shows existing images with correct primary indicator` | `click` Images tab after mount | MSW returns product with two images; image id `2` has `is_primary: true` | Two image thumbnails rendered; only the thumbnail for id `2` carries the primary/star indicator class |
| `UOMs tab shows existing UOM rows` | `click` UOMs tab after mount | MSW returns product with 2 UOM associations | Two UOM rows rendered; base UOM row remove button is disabled; non-base UOM remove button is enabled |
| `Vendors tab shows existing vendor rows` | `click` Vendors tab after mount | MSW returns product with 2 vendor links | Two vendor rows rendered with vendor name and price populated correctly |
| `submit in edit mode calls updateProduct and redirects` | `submit` form event | User edits name to "Bolt M10" and submits | `PUT /products/99` called with updated payload; `router.push('/inventory/products')` is invoked |
| `API 404 on mount shows error and redirects` | `onMounted` | MSW returns 404 for `GET /products/99` | An error message is rendered briefly; `router.push('/inventory/products')` is called |

---

## CategoriesView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `getCategoryTree called on mount` | `onMounted` | Component mounts | `GET /categories/tree` is called exactly once |
| `root categories rendered at top level` | Render after mount | MSW returns two root categories with no parent | Two top-level category name elements are visible without indentation class |
| `child categories rendered indented under parent` | Render after mount | MSW returns root category with two children | Child category elements carry an indentation/nesting CSS class or are nested in the DOM under their parent |
| `expand chevron shows hidden children` | `click` on collapse chevron of a root category | Root category is initially collapsed (children hidden); user clicks chevron | Children elements become visible; chevron icon rotates or changes direction |
| `collapse chevron hides visible children` | `click` on expand chevron of an already-expanded root category | Children are visible; user clicks chevron again | Children elements are hidden from the DOM or have a hidden CSS class |
| `Add Root Category shows inline form at top` | `click` "Add Root Category" button | No form row visible initially; user clicks the button | An inline form row appears at the top of the category tree with a name text input and a save control |
| `submit root category calls createCategory without parent` | `submit` inline form at root level | User types "Hardware" in the inline input and saves | `POST /categories` called with `{ name: "Hardware", parentId: null }`; tree refreshes via `GET /categories/tree` |
| `Add Subcategory shows indented inline form` | `click` "Add Subcategory" on a root category row | User clicks the subcategory button for category id `5` | An indented inline form row appears directly under category 5's row |
| `submit subcategory calls createCategory with parent ID` | `submit` indented inline form | User types "Bolts" and saves under parent id `5` | `POST /categories` called with `{ name: "Bolts", parentId: 5 }`; tree refreshes |
| `edit category saves updated name` | `click` edit, then `submit` inline name input | User clicks edit on a category, changes name to "Fasteners", and saves | `PUT /categories/5` called with `{ name: "Fasteners" }`; tree row updates to display new name |
| `delete category shows confirmation then calls deleteCategory` | `click` delete, then confirm | User clicks delete on a leaf category; confirms the dialog | `DELETE /categories/5` is called; the category row is removed from the tree |
| `delete category with children shows API 422 error` | `click` delete, confirm | MSW returns 422 for `DELETE /categories/3` (has children) | Error message "Cannot delete category with subcategories" is visible; tree remains unchanged |

---

## BrandsView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `loads paginated brands on mount` | `onMounted` | Component mounts | `GET /brands?page=0` is called; brand rows appear in the list/table |
| `search by name triggers debounced API call` | `input` on search field | User types "Acme" into the search input; waits for debounce | `GET /brands?search=Acme&page=0` is called once; list updates to matching brands |
| `Create Brand button opens modal with name field` | `click` "Create Brand" button | Modal is initially closed; user clicks the button | A modal dialog becomes visible containing a name text input and a submit control |
| `submit brand name calls createBrand and closes modal` | `submit` inside modal | User enters "Ridgid" in the name field and submits | `POST /brands` called with `{ name: "Ridgid" }`; modal closes; new brand row appears in the list |
| `duplicate brand name shows error inside modal` | `submit` inside modal | MSW returns 409 for `POST /brands` | Error message is rendered inside the modal (modal does not close); the duplicate name is highlighted |
| `edit brand opens modal pre-filled` | `click` edit button on brand row | Brand row with id `8`, name "Bosch" exists; user clicks its edit button | Modal opens with the name field already populated with "Bosch" |
| `edit brand submit calls updateBrand` | `submit` inside pre-filled edit modal | User changes name to "Bosch Tools" and submits | `PUT /brands/8` called with `{ name: "Bosch Tools" }`; modal closes; row updates to "Bosch Tools" |
| `delete brand shows confirmation then calls deleteBrand` | `click` delete, then confirm | User clicks delete on brand id `8`; confirms | `DELETE /brands/8` is called; brand row is removed from the list |
| `delete brand with products shows API 422 error` | `click` delete, confirm | MSW returns 422 for `DELETE /brands/8` | An error message is displayed (inline or toast); brand row remains in the list; modal/dialog closes or resets |
| `pagination next calls brands API with page=1` | `click` Next pagination control | Current page is 0; user clicks Next | `GET /brands?page=1` is called; list updates to page-2 brands |

---

## StockView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `getStock called on mount` | `onMounted` | Component mounts | `GET /stock` is called exactly once |
| `table renders required columns` | Render after mount | MSW returns a stock record with all fields | Table headers and corresponding cells for Product Name, Location, Qty on Hand, Reserved Qty, Available Qty, Unit Cost are all present |
| `low stock rows show orange badge` | Render after mount | MSW returns record with `qty_on_hand` below low-stock threshold | That row contains an element with a class or text indicating low stock; the element has an orange/warning visual indicator |
| `out of stock rows show red badge` | Render after mount | MSW returns record with `qty_on_hand = 0` | That row contains an element indicating out of stock with a red/danger visual indicator |
| `product filter calls getStockByProduct` | `input` or `change` on product filter | User selects or types a product name/id in the product filter | `GET /stock?productId={id}` is called; table rows narrow to that product's stock records |
| `location filter calls getStockByLocation` | `change` on location dropdown filter | User selects location "SHELF-A1" | `GET /stock?locationId={id}` is called; table rows narrow to that location's records |
| `View Movements expands row and loads history` | `click` "View Movements" on a stock row | Expansion section is initially hidden; user clicks the button for product `p1` at location `l1` | `GET /movements?productId=p1` is called; a section below the row expands and shows movement history entries |
| `View Movements collapses on second click` | `click` "View Movements" again on the same row | Expansion is visible; user clicks the button again | The expanded section collapses/hides; no additional API call is made |
| `Adjust Stock modal opens with qty and reason fields` | `click` "Adjust Stock" on a row | Modal is initially closed | Modal becomes visible with a numeric adjustment quantity input (accepts positive/negative values) and a reason text field |
| `submit adjustment calls adjustStock and updates row qty` | `submit` in adjustment modal | User enters `10` in qty and "Cycle count" in reason; submits | `POST /stock/adjust` called with product, location, qty, reason; modal closes; the row's Qty on Hand cell updates to reflect the new quantity |
| `Transfer modal opens with source pre-filled` | `click` "Transfer" on a row | Modal is initially closed; row belongs to location "WH-MAIN" | Modal opens; source location field is pre-filled with "WH-MAIN"; destination location dropdown is editable; qty input is present |
| `submit transfer calls transferStock` | `submit` in transfer modal | User selects destination "SHELF-A1" and qty `5`; submits | `POST /stock/transfer` called with source, destination, product, and qty payload; modal closes |

---

## WarehousesView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `loads all locations on mount` | `onMounted` | Component mounts | `GET /locations` is called exactly once; location rows appear in the table |
| `table renders required columns` | Render after mount | MSW returns a location with all fields | Table cells for Code, Name, Type, and Active Status are all present in each row |
| `filter by type calls getLocationsByType` | `change` on type filter dropdown | User selects type "SHELF" | `GET /locations?type=SHELF` is called; table updates to show only SHELF-type rows |
| `Create Location button opens modal` | `click` "Create Location" button | Modal initially closed | Modal becomes visible with inputs for code, name, type selector, and description |
| `submit create location calls createLocation and adds row` | `submit` in create modal | User fills in code "BIN-001", name "Bin 1", type "BIN", and submits | `POST /locations` called with the correct payload; modal closes; new row for "BIN-001" appears in the table |
| `edit location opens modal pre-filled` | `click` edit on an existing location row | Location with id `3`, code "WH-MAIN" exists | Modal opens with code field containing "WH-MAIN" and all other fields populated from the record |
| `edit submit calls updateLocation` | `submit` inside pre-filled edit modal | User changes name to "Main Warehouse" and submits | `PUT /locations/3` called with updated payload; modal closes; row updates to "Main Warehouse" |
| `delete location shows confirmation then calls deleteLocation` | `click` delete, then confirm | User clicks delete on location id `3`; confirms | `DELETE /locations/3` is called; the row is removed from the table |
| `delete location with stock shows API 422 error` | `click` delete, confirm | MSW returns 422 for `DELETE /locations/3` | Error message "Cannot delete location with existing stock" is visible; row remains in the table |
| `active status badge reflects location state` | Render after mount | MSW returns one active and one inactive location | Active row shows an "Active" badge; inactive row shows an "Inactive" badge with distinct styling |

---

## UOMView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `loads all UOMs on mount` | `onMounted` | Component mounts | `GET /uoms` is called exactly once; UOM rows appear in the table |
| `table renders required columns` | Render after mount | MSW returns UOMs with all fields | Table cells for Code, Name, Type, and Is Base UOM indicator are all present in each row |
| `base UOM row shows badge` | Render after mount | MSW returns one UOM with `is_base: true` | That row contains a badge or icon element indicating it is the base UOM |
| `Create UOM button opens modal` | `click` "Create UOM" button | Modal is initially closed | Modal becomes visible with inputs for code, name, and an "Is Base" checkbox |
| `submit create UOM calls createUom and adds row` | `submit` in create modal | User fills in code "KG", name "Kilogram", leaves Is Base unchecked, and submits | `POST /uoms` called with `{ code: "KG", name: "Kilogram", is_base: false }`; modal closes; new row for "KG" appears in the table |
| `edit UOM opens modal pre-filled` | `click` edit on an existing UOM row | UOM with id `2`, code "PCS" exists | Modal opens with code field "PCS" and all other fields populated |
| `edit UOM submit calls updateUom` | `submit` inside pre-filled edit modal | User changes name to "Pieces (each)" and submits | `PUT /uoms/2` called with updated payload; modal closes; row updates to show new name |
| `delete non-base UOM shows confirmation then calls deleteUom` | `click` delete, then confirm | Non-base UOM row with id `2` exists; user clicks delete and confirms | `DELETE /uoms/2` is called; the row is removed from the table |
| `delete button disabled for base UOM` | Render after mount | MSW returns a base UOM row | The delete/remove button on the base UOM row has a `disabled` attribute or is absent from the DOM |
| `disabled base UOM delete button has tooltip` | Hover or `title`/`aria-label` check on disabled button | Base UOM row rendered; inspect the disabled delete trigger | The element has a tooltip, `title`, or `aria-label` containing "Cannot delete base UOM" |
| `delete UOM in use by products shows API 422 error` | `click` delete, confirm | MSW returns 422 for `DELETE /uoms/2` | An error message is displayed indicating the UOM is in use; the row remains in the table |

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

**Dialog/confirmation testing:** If the app uses `window.confirm`, stub it with `vi.spyOn(window, 'confirm').mockReturnValue(true)` (or `false` for cancel tests). If a custom modal component is used, interact with its confirm button via `wrapper.find('[data-testid="confirm-btn"]').trigger('click')`.

**Coverage collection:** Run with `vitest --coverage` using the `@vitest/coverage-v8` provider. The coverage threshold for all files under `src/views/inventory/` should be set to `100` for statements, branches, functions, and lines in `vitest.config.ts`.
