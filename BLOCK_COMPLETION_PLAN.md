# Block Completion Plan (1-11)
## Target: 100% Implementation

---

## Phase 1: Critical Integration Fixes
**Priority: HIGH | Estimated Items: 5**

These are cross-module integrations that are partially implemented but not connected.

### 1.1 Receiving → AP Invoice Auto-Creation (Block 5)
**Current State:** Flag `apInvoiceCreated` exists but logic not implemented
**Files to Modify:**
- `src/main/java/com/hisobnoma/platform/inventory/service/ReceivingService.java`

**Tasks:**
- [ ] Inject `APInvoiceService` into `ReceivingService`
- [ ] In `confirmReceiving()` method, after stock update:
  - Call `apInvoiceService.createFromReceiving(receivingOrder)`
  - Set `receivingOrder.setApInvoiceCreated(true)`
  - Set `receivingOrder.setApInvoiceId(invoice.getId())`
- [ ] Add `createFromReceiving(ReceivingOrder)` method to `APInvoiceService`

---

### 1.2 POS Credit Sale → AR Invoice Auto-Creation (Block 10)
**Current State:** Field `arInvoiceId` exists but no auto-creation logic
**Files to Modify:**
- `src/main/java/com/hisobnoma/platform/pos/service/POSTransactionService.java`

**Tasks:**
- [ ] Inject `ARInvoiceService` into `POSTransactionService`
- [ ] In `completeTransaction()` method:
  - Check if any payment type is CREDIT
  - If credit sale, call `arInvoiceService.createFromPOSTransaction(transaction)`
  - Set `transaction.setArInvoiceId(invoice.getId())`
- [ ] Add `createFromPOSTransaction(POSTransaction)` method to `ARInvoiceService`

---

### 1.3 Complete BOGO Promotion Logic (Block 11)
**Current State:** Placeholder calculation returning BigDecimal.ZERO
**Files to Modify:**
- `src/main/java/com/hisobnoma/platform/pos/service/PromotionService.java`

**Tasks:**
- [ ] Implement `calculateBogoDiscount()` method:
  - Find qualifying items in cart matching buyQuantity
  - Calculate free items based on getQuantity
  - Apply getDiscountPercent to free items
  - Return total discount amount
- [ ] Update `calculatePromotionDiscount()` to call new method for BUY_X_GET_Y type

---

### 1.4 Complete Bundle Pricing Logic (Block 11)
**Current State:** PromotionType.BUNDLE exists but no calculation
**Files to Modify:**
- `src/main/java/com/hisobnoma/platform/pos/service/PromotionService.java`

**Tasks:**
- [ ] Create `calculateBundleDiscount()` method:
  - Check if all bundle products are in cart
  - Calculate bundle price vs individual prices
  - Return discount (individual total - bundle price)
- [ ] Add bundle-specific fields to PromotionAction if needed
- [ ] Update `calculatePromotionDiscount()` for BUNDLE type

---

### 1.5 Complete Promotion Condition Evaluations (Block 11)
**Current State:** CATEGORY, BRAND, CUSTOMER_GROUP, FIRST_PURCHASE are simplified
**Files to Modify:**
- `src/main/java/com/hisobnoma/platform/pos/service/PromotionService.java`

**Tasks:**
- [ ] Inject `ProductRepository` and `CustomerRepository`
- [ ] Implement CATEGORY condition: Check if cart products belong to specified categories
- [ ] Implement BRAND condition: Check if cart products belong to specified brands
- [ ] Implement CUSTOMER_GROUP condition: Check customer's group membership
- [ ] Implement FIRST_PURCHASE condition: Query customer's order history

---

## Phase 2: Missing API Endpoints
**Priority: HIGH | Estimated Items: 12**

### 2.1 Product Image Upload/Delete (Block 3)
**Files to Create/Modify:**
- `src/main/java/com/hisobnoma/platform/inventory/controller/ProductController.java`
- `src/main/java/com/hisobnoma/platform/inventory/service/ProductImageService.java` (NEW)

**Tasks:**
- [ ] Create `ProductImageService` with:
  - `uploadImage(Long productId, MultipartFile file)`
  - `deleteImage(Long productId, Long imageId)`
  - `setPrimaryImage(Long productId, Long imageId)`
  - `reorderImages(Long productId, List<Long> imageIds)`
- [ ] Add endpoints to `ProductController`:
  - `POST /api/v1/inventory/products/{id}/images` - Upload image
  - `DELETE /api/v1/inventory/products/{id}/images/{imageId}` - Delete image
  - `PUT /api/v1/inventory/products/{id}/images/{imageId}/primary` - Set primary
- [ ] Configure file storage (local or S3)

---

### 2.2 Product Variant Independent Management (Block 3)
**Files to Modify:**
- `src/main/java/com/hisobnoma/platform/inventory/controller/ProductController.java`
- `src/main/java/com/hisobnoma/platform/inventory/service/ProductService.java`

**Tasks:**
- [ ] Add endpoints:
  - `POST /api/v1/inventory/products/{id}/variants` - Add variant
  - `PUT /api/v1/inventory/products/{id}/variants/{variantId}` - Update variant
  - `DELETE /api/v1/inventory/products/{id}/variants/{variantId}` - Delete variant
  - `GET /api/v1/inventory/products/{id}/variants` - List variants
- [ ] Add service methods: `addVariant()`, `updateVariant()`, `deleteVariant()`

---

### 2.3 Product CSV/Excel Import (Block 3)
**Files to Create:**
- `src/main/java/com/hisobnoma/platform/inventory/service/ProductImportService.java` (NEW)
- `src/main/java/com/hisobnoma/platform/inventory/dto/ProductImportDto.java` (NEW)

**Tasks:**
- [ ] Add Apache POI dependency to pom.xml (if not present)
- [ ] Create `ProductImportService`:
  - `importFromCsv(MultipartFile file)` - Parse CSV, validate, create products
  - `importFromExcel(MultipartFile file)` - Parse Excel, validate, create products
  - `validateImportRow(ProductImportDto row)` - Validate each row
  - `generateImportTemplate()` - Generate sample template
- [ ] Add endpoint: `POST /api/v1/inventory/products/import`
- [ ] Add endpoint: `GET /api/v1/inventory/products/import/template`

---

### 2.4 Product CSV/Excel Export (Block 3)
**Files to Modify:**
- `src/main/java/com/hisobnoma/platform/inventory/service/ProductService.java`
- `src/main/java/com/hisobnoma/platform/inventory/controller/ProductController.java`

**Tasks:**
- [ ] Add export methods to ProductService:
  - `exportToCsv(ProductFilter filter)` - Export filtered products to CSV
  - `exportToExcel(ProductFilter filter)` - Export to Excel
- [ ] Add endpoints:
  - `GET /api/v1/inventory/products/export?format=csv`
  - `GET /api/v1/inventory/products/export?format=excel`

---

### 2.5 Inventory Planning APIs (Block 5)
**Files to Create:**
- `src/main/java/com/hisobnoma/platform/inventory/service/InventoryPlanningService.java` (NEW)
- `src/main/java/com/hisobnoma/platform/inventory/controller/InventoryPlanningController.java` (NEW)

**Tasks:**
- [ ] Create `InventoryPlanningService`:
  - `getReorderSuggestions()` - Products below reorder point with suggested quantities
  - `performAbcAnalysis()` - Classify products by value/movement (A/B/C)
  - `getSlowMovingProducts(int days)` - Products not sold in X days
  - `getDeadStock(int days)` - Zero movement products
- [ ] Create `InventoryPlanningController`:
  - `GET /api/v1/inventory/planning/reorder-suggestions`
  - `GET /api/v1/inventory/planning/abc-analysis`
  - `GET /api/v1/inventory/planning/slow-moving?days=90`
  - `GET /api/v1/inventory/planning/dead-stock?days=180`

---

### 2.6 Chart of Accounts Import (Block 6)
**Files to Modify:**
- `src/main/java/com/hisobnoma/platform/finance/service/AccountService.java`
- `src/main/java/com/hisobnoma/platform/finance/controller/AccountController.java`

**Tasks:**
- [ ] Add import methods:
  - `importFromCsv(MultipartFile file)` - Import chart from CSV
  - `importFromTemplate(String templateName)` - Import predefined template
  - `generateDefaultChartOfAccounts()` - Create standard chart
- [ ] Add endpoints:
  - `POST /api/v1/finance/accounts/import`
  - `POST /api/v1/finance/accounts/import/template/{templateName}`
  - `GET /api/v1/finance/accounts/import/templates` - List available templates

---

### 2.7 Recurring Journal Templates (Block 6)
**Files to Create:**
- `src/main/java/com/hisobnoma/platform/finance/entity/RecurringJournalTemplate.java` (NEW)
- `src/main/java/com/hisobnoma/platform/finance/service/RecurringJournalService.java` (NEW)
- `src/main/java/com/hisobnoma/platform/finance/controller/RecurringJournalController.java` (NEW)

**Tasks:**
- [ ] Create `RecurringJournalTemplate` entity:
  - name, description, frequency (DAILY, WEEKLY, MONTHLY, YEARLY)
  - nextExecutionDate, lastExecutionDate
  - templateLines (same structure as JournalLine)
  - isActive
- [ ] Create migration for `recurring_journal_templates` table
- [ ] Create `RecurringJournalService`:
  - CRUD for templates
  - `executeRecurringEntries()` - Scheduled job to create entries
  - `previewNextExecution(Long templateId)` - Preview next entry
- [ ] Create CRUD endpoints: `/api/v1/finance/recurring-journals`
- [ ] Add `@Scheduled` job to execute recurring entries

---

### 2.8 POS Returns Dedicated Endpoint (Block 10)
**Files to Modify:**
- `src/main/java/com/hisobnoma/platform/pos/controller/POSTransactionController.java`
- `src/main/java/com/hisobnoma/platform/pos/dto/CreateReturnRequest.java` (NEW)

**Tasks:**
- [ ] Create `CreateReturnRequest` DTO with original transaction lookup
- [ ] Add dedicated endpoint: `POST /api/v1/pos/returns`
  - Accept original transaction ID/number
  - Auto-populate return lines from original
  - Allow partial returns (select which items to return)
  - Calculate refund amount
- [ ] Add `createReturn(CreateReturnRequest)` to service

---

## Phase 3: Event/Domain Events System (Block 4) ✅ COMPLETED
**Priority: MEDIUM | Estimated Items: 1**

### 3.1 Inventory Event Publisher
**Files Created:**
- `src/main/java/com/hisobnoma/platform/inventory/event/InventoryEvent.java` ✅
- `src/main/java/com/hisobnoma/platform/inventory/event/StockReceivedEvent.java` ✅
- `src/main/java/com/hisobnoma/platform/inventory/event/StockIssuedEvent.java` ✅
- `src/main/java/com/hisobnoma/platform/inventory/event/StockTransferredEvent.java` ✅
- `src/main/java/com/hisobnoma/platform/inventory/event/StockAdjustedEvent.java` ✅
- `src/main/java/com/hisobnoma/platform/inventory/event/LowStockAlertEvent.java` ✅
- `src/main/java/com/hisobnoma/platform/inventory/event/InventoryEventPublisher.java` ✅
- `src/main/java/com/hisobnoma/platform/inventory/event/InventoryEventListener.java` ✅

**Files Modified:**
- `src/main/java/com/hisobnoma/platform/inventory/service/StockService.java` ✅

**Completed Tasks:**
- [x] Create base `InventoryEvent` class
- [x] Create specific events:
  - `StockReceivedEvent`
  - `StockIssuedEvent`
  - `StockTransferredEvent`
  - `StockAdjustedEvent`
  - `LowStockAlertEvent`
- [x] Create `InventoryEventPublisher` using Spring's `ApplicationEventPublisher`
- [x] Publish events in `StockService` after each operation
- [x] Create `InventoryEventListener` for handling events (logging, notifications)

---

## Phase 4: User Entity Email Field Decision (Block 2)
**Priority: LOW | Estimated Items: 1**

### 4.1 Add Email Field to User Entity
**Current State:** Uses phone instead of email
**Decision Required:** Keep phone-only OR add email as optional

**Option A: Keep Phone-Only (Document Decision)**
- [ ] Update BACKEND_IMPLEMENTATION_PLAN.md to reflect phone-based auth
- [ ] Add comment in User entity explaining design decision

**Option B: Add Email Field**
- [ ] Add `email` and `emailVerified` fields to User entity
- [ ] Create migration to add columns
- [ ] Update UserDto, CreateUserRequest, UpdateUserRequest
- [ ] Update UserMapper
- [ ] Add email validation in UserService
- [ ] Support login by email OR phone

---

## Phase 5: Comprehensive Test Suite
**Priority: HIGH | Estimated Items: 45+ test classes**

### 5.1 Block 2: RBAC Tests
**Files to Create in `src/test/java/com/hisobnoma/platform/auth/`:**

- [ ] `controller/UserControllerTest.java` - User CRUD integration tests
- [ ] `controller/RoleControllerTest.java` - Role CRUD integration tests
- [ ] `service/UserServiceTest.java` - User service unit tests
- [ ] `service/RoleServiceTest.java` - Role service unit tests
- [ ] `security/MultiTenancyIsolationTest.java` - Tenant isolation tests

---

### 5.2 Block 3: Inventory Product Tests
**Files to Create in `src/test/java/com/hisobnoma/platform/inventory/`:**

- [ ] `service/ProductServiceTest.java` - Product CRUD tests
- [ ] `service/CategoryServiceTest.java` - Category hierarchy tests
- [ ] `service/SkuGeneratorServiceTest.java` - SKU generation tests
- [ ] `service/BarcodeServiceTest.java` - Barcode validation tests
- [ ] `controller/ProductControllerTest.java` - API integration tests
- [ ] `controller/CategoryControllerTest.java` - Category API tests

---

### 5.3 Block 4: Stock Management Tests
**Files to Create in `src/test/java/com/hisobnoma/platform/inventory/`:**

- [ ] `service/StockServiceTest.java` - Stock operations tests
- [ ] `service/StockServiceConcurrencyTest.java` - Concurrent access tests
- [ ] `service/StockReservationTest.java` - Reservation logic tests
- [ ] `entity/StockBatchTest.java` - Batch entity tests
- [ ] `entity/SerialNumberTest.java` - Serial entity tests
- [ ] `controller/StockControllerTest.java` - Stock API tests

---

### 5.4 Block 5: Inventory Operations Tests
**Files to Create in `src/test/java/com/hisobnoma/platform/inventory/`:**

- [ ] `service/PurchaseOrderServiceTest.java` - PO workflow tests
- [ ] `service/ReceivingServiceTest.java` - Receiving workflow tests
- [ ] `service/InventoryCountServiceTest.java` - Count workflow tests
- [ ] `service/InventoryPlanningServiceTest.java` - Planning calculations
- [ ] `controller/PurchaseOrderControllerTest.java` - PO API tests
- [ ] `controller/ReceivingControllerTest.java` - Receiving API tests
- [ ] `controller/InventoryCountControllerTest.java` - Count API tests

---

### 5.5 Block 6: General Ledger Tests
**Files to Create in `src/test/java/com/hisobnoma/platform/finance/`:**

- [ ] `service/AccountServiceTest.java` - Account CRUD tests
- [ ] `service/JournalEntryServiceTest.java` - Journal tests
- [ ] `service/JournalEntryValidationTest.java` - Double-entry validation
- [ ] `service/FiscalPeriodServiceTest.java` - Period close tests
- [ ] `service/GLIntegrationServiceTest.java` - GL posting tests
- [ ] `service/ExchangeRateServiceTest.java` - Currency tests
- [ ] `controller/AccountControllerTest.java` - Account API tests
- [ ] `controller/JournalEntryControllerTest.java` - Journal API tests

---

### 5.6 Block 7: Accounts Payable Tests
**Files to Create in `src/test/java/com/hisobnoma/platform/finance/`:**

- [ ] `service/APInvoiceServiceTest.java` - Invoice CRUD tests
- [ ] `service/APInvoiceThreeWayMatchingTest.java` - 3-way matching tests
- [ ] `service/APPaymentServiceTest.java` - Payment tests
- [ ] `service/APPaymentAllocationTest.java` - Allocation tests
- [ ] `service/APReportServiceTest.java` - Report generation tests
- [ ] `controller/APInvoiceControllerTest.java` - Invoice API tests
- [ ] `controller/APPaymentControllerTest.java` - Payment API tests

---

### 5.7 Block 8: Accounts Receivable Tests
**Files to Create in `src/test/java/com/hisobnoma/platform/finance/`:**

- [ ] `service/CustomerServiceTest.java` - Customer CRUD tests
- [ ] `service/ARInvoiceServiceTest.java` - Invoice tests
- [ ] `service/ARInvoiceFromPOSTest.java` - POS integration tests
- [ ] `service/ARPaymentServiceTest.java` - Payment tests
- [ ] `service/ARPaymentAllocationTest.java` - Allocation tests
- [ ] `service/CreditNoteServiceTest.java` - Credit note tests
- [ ] `service/ARReportServiceTest.java` - Report tests
- [ ] `controller/ARInvoiceControllerTest.java` - Invoice API tests
- [ ] `controller/ARPaymentControllerTest.java` - Payment API tests

---

### 5.8 Block 10: POS Tests
**Files to Create in `src/test/java/com/hisobnoma/platform/pos/`:**

- [ ] `service/POSTerminalServiceTest.java` - Terminal tests
- [ ] `service/ShiftServiceTest.java` - Shift workflow tests
- [ ] `service/POSTransactionServiceTest.java` - Transaction flow tests
- [ ] `service/POSPaymentServiceTest.java` - Payment tests
- [ ] `service/POSIntegrationTest.java` - Stock/GL integration
- [ ] `controller/POSTerminalControllerTest.java` - Terminal API tests
- [ ] `controller/ShiftControllerTest.java` - Shift API tests
- [ ] `controller/POSTransactionControllerTest.java` - Transaction API tests

---

### 5.9 Block 11: Pricing & Promotions Tests
**Files to Create in `src/test/java/com/hisobnoma/platform/pos/`:**

- [ ] `service/PriceListServiceTest.java` - Price list CRUD tests
- [ ] `service/PricingServiceTest.java` - Price calculation tests
- [ ] `service/PricingServiceTieredTest.java` - Tiered pricing tests
- [ ] `service/PromotionServiceTest.java` - Promotion CRUD tests
- [ ] `service/PromotionEngineTest.java` - Promotion application
- [ ] `service/PromotionStackingTest.java` - Stacking rules tests
- [ ] `service/CouponServiceTest.java` - Coupon validation tests
- [ ] `service/BogoPromotionTest.java` - BOGO calculation tests
- [ ] `controller/PriceListControllerTest.java` - Price list API tests
- [ ] `controller/PromotionControllerTest.java` - Promotion API tests
- [ ] `controller/PricingControllerTest.java` - Pricing API tests

---

## Phase 6: Documentation Updates
**Priority: LOW | Estimated Items: 3**

### 6.1 Update API Documentation
- [ ] Update `docs/API.md` with all new endpoints
- [ ] Add missing endpoint documentation for planning APIs
- [ ] Add recurring journal API documentation

### 6.2 Update Postman Collection
- [ ] Add missing endpoints to Postman collection
- [ ] Add planning APIs
- [ ] Add import/export endpoints
- [ ] Add recurring journal endpoints

### 6.3 Update Implementation Plan
- [ ] Mark all completed items in BACKEND_IMPLEMENTATION_PLAN.md
- [ ] Document design decisions (phone vs email)
- [ ] Update completion percentages

---

## Execution Order Summary

| Phase | Description | Priority | Est. Effort |
|-------|-------------|----------|-------------|
| **1** | Critical Integration Fixes | HIGH | 2-3 days |
| **2** | Missing API Endpoints | HIGH | 4-5 days |
| **3** | Event System | MEDIUM | 1 day |
| **4** | User Email Decision | LOW | 0.5 day |
| **5** | Test Suite | HIGH | 5-7 days |
| **6** | Documentation | LOW | 1 day |

**Total Estimated Effort: 14-18 days**

---

## Quick Reference: Files to Create

### New Services (8)
1. `inventory/service/ProductImageService.java`
2. `inventory/service/ProductImportService.java`
3. `inventory/service/InventoryPlanningService.java`
4. `inventory/event/InventoryEventPublisher.java`
5. `inventory/event/InventoryEventListener.java`
6. `finance/service/RecurringJournalService.java`

### New Controllers (2)
1. `inventory/controller/InventoryPlanningController.java`
2. `finance/controller/RecurringJournalController.java`

### New Entities (1)
1. `finance/entity/RecurringJournalTemplate.java`

### New DTOs (3+)
1. `inventory/dto/ProductImportDto.java`
2. `pos/dto/CreateReturnRequest.java`
3. `inventory/event/StockChangedEvent.java` (and related events)

### New Migrations (2)
1. `V18__recurring_journal_templates.sql`
2. `V19__user_email_field.sql` (if Option B chosen)

### Test Classes (45+)
As listed in Phase 5

---

## Checklist Summary

- [x] **Phase 1:** 5 integration fixes ✅ COMPLETED
- [x] **Phase 2:** 8 API implementations ✅ COMPLETED
- [x] **Phase 3:** 1 event system ✅ COMPLETED
- [ ] **Phase 4:** 1 email decision
- [ ] **Phase 5:** 45+ test classes
- [ ] **Phase 6:** 3 documentation updates (Partially completed in Phase 2)

**Total Tasks: ~63 items**
**Completed: Phases 1, 2, 3**
