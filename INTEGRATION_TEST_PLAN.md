# Integration Test Plan: Full CRUD Flow Testing

## Overview

- **Total Controllers**: 62
- **Total Endpoints**: 684
- **Modules**: 12
- **Database**: H2 in-memory (test profile)
- **Test Style**: Full flow (Controller -> Service -> Repository) with real DB, no mocks

---

## Phase 1: Foundation & Auth (Week 1)

**Goal**: Test authentication, roles, and tenant settings that all other modules depend on.

| # | Controller | Endpoints | Priority | Dependencies |
|---|-----------|-----------|----------|--------------|
| 1 | AuthController | 11 | CRITICAL | None |
| 2 | UserController | 11 | CRITICAL | Auth |
| 3 | RoleController | 10 | CRITICAL | Auth |
| 4 | TenantSettingController | 8 | HIGH | Auth |
| 5 | SystemSettingController | 9 | HIGH | Auth, Role |
| 6 | AdminDashboardController | 1 | LOW | Auth |
| 7 | AuditLogController | 13 | MEDIUM | Auth |

**Endpoints in Phase**: 63
**Test Focus**:
- User registration, login, token refresh, logout
- Role CRUD with permission assignment
- User CRUD with role assignment
- Tenant/system settings CRUD
- Permission-based access control verification

---

## Phase 2: Finance Core (Week 2)

**Goal**: Test fundamental finance entities (accounts, currencies, fiscal periods) that other finance modules reference.

| # | Controller | Endpoints | Priority | Dependencies |
|---|-----------|-----------|----------|--------------|
| 1 | CurrencyController | 9 | CRITICAL | Auth |
| 2 | ExchangeRateController | 12 | HIGH | Currency |
| 3 | AccountController | 17 | CRITICAL | Auth |
| 4 | FiscalPeriodController | 17 | CRITICAL | Auth |
| 5 | CustomerController | 14 | CRITICAL | Auth |
| 6 | BankAccountController | 13 | HIGH | Currency, Account |

**Endpoints in Phase**: 82
**Test Focus**:
- Chart of accounts CRUD (parent-child hierarchy)
- Currency CRUD and exchange rate management
- Fiscal year/period creation, open/close lifecycle
- Customer CRUD with balance tracking
- Bank account CRUD linked to GL accounts

---

## Phase 3: Finance Transactions (Week 3)

**Goal**: Test financial transaction flows - journal entries, AP/AR invoices, payments.

| # | Controller | Endpoints | Priority | Dependencies |
|---|-----------|-----------|----------|--------------|
| 1 | JournalEntryController | 9 | CRITICAL | Account, FiscalPeriod |
| 2 | RecurringJournalController | 9 | HIGH | JournalEntry |
| 3 | TaxCodeController | 24 | HIGH | Account |
| 4 | APInvoiceController | 16 | CRITICAL | Account, Customer |
| 5 | APPaymentController | 14 | HIGH | APInvoice, BankAccount |
| 6 | ARInvoiceController | 13 | CRITICAL | Account, Customer |
| 7 | ARPaymentController | 14 | HIGH | ARInvoice, BankAccount |
| 8 | CreditNoteController | 8 | MEDIUM | ARInvoice |
| 9 | BankTransactionController | 10 | MEDIUM | BankAccount |
| 10 | BankReconciliationController | 9 | MEDIUM | BankTransaction |
| 11 | APReportController | 3 | LOW | APInvoice |
| 12 | ARReportController | 3 | LOW | ARInvoice |

**Endpoints in Phase**: 132
**Test Focus**:
- Journal entry create/post/reverse with double-entry validation
- Recurring journal template creation and execution
- Tax code CRUD with rate tiers
- AP invoice lifecycle: draft -> approved -> paid
- AR invoice lifecycle: draft -> sent -> paid
- Payment allocation to invoices
- Credit note creation and application
- Bank reconciliation flow

---

## Phase 4: Inventory (Week 4)

**Goal**: Test product catalog, stock management, and purchasing.

| # | Controller | Endpoints | Priority | Dependencies |
|---|-----------|-----------|----------|--------------|
| 1 | CategoryController | 9 | CRITICAL | Auth |
| 2 | BrandController | 8 | HIGH | Auth |
| 3 | UnitOfMeasureController | 10 | CRITICAL | Auth |
| 4 | ProductUomController | 5 | HIGH | UOM, Product |
| 5 | LocationController | 14 | CRITICAL | Auth |
| 6 | ProductController | 52 | CRITICAL | Category, Brand, UOM |
| 7 | VendorController | 16 | HIGH | Auth |
| 8 | StockController | 15 | HIGH | Product, Location |
| 9 | PurchaseOrderController | 8 | HIGH | Vendor, Product |
| 10 | ReceivingController | 8 | MEDIUM | PurchaseOrder |
| 11 | InventoryCountController | 11 | MEDIUM | Product, Location |
| 12 | InventoryPlanningController | 4 | LOW | Stock |

**Endpoints in Phase**: 160
**Test Focus**:
- Category hierarchy CRUD (parent-child)
- Brand and UOM CRUD
- Product CRUD with variants, pricing, images
- Location/warehouse CRUD with stock levels
- Vendor CRUD
- Stock movements (transfer, adjust, receive)
- Purchase order lifecycle: draft -> approved -> received
- Inventory count creation, counting, approval
- Reorder point planning

---

## Phase 5: POS (Week 5)

**Goal**: Test point-of-sale terminal management, transactions, pricing, and promotions.

| # | Controller | Endpoints | Priority | Dependencies |
|---|-----------|-----------|----------|--------------|
| 1 | POSTerminalController | 9 | CRITICAL | Location |
| 2 | ShiftController | 11 | CRITICAL | Terminal |
| 3 | PriceListController | 18 | HIGH | Product |
| 4 | PricingController | 5 | HIGH | Product, PriceList |
| 5 | PromotionController | 13 | HIGH | Product |
| 6 | CouponController | 16 | MEDIUM | Promotion |
| 7 | POSTransactionController | 37 | CRITICAL | Terminal, Shift, Product |

**Endpoints in Phase**: 109
**Test Focus**:
- Terminal CRUD and activation
- Shift open/close flow with cash reconciliation
- Price list CRUD with product price items
- Dynamic pricing calculation
- Promotion CRUD with conditions and actions
- Coupon generation, validation, redemption
- Full POS transaction flow: create -> add items -> apply discounts -> payment -> complete
- Returns and refunds
- Void transactions

---

## Phase 6: HR & Payroll (Week 6)

**Goal**: Test HR entities and salary management.

| # | Controller | Endpoints | Priority | Dependencies |
|---|-----------|-----------|----------|--------------|
| 1 | DepartmentController | 5 | CRITICAL | Auth |
| 2 | PositionController | 5 | CRITICAL | Department |
| 3 | EmployeeController | 8 | CRITICAL | Department, Position |
| 4 | SalaryController | 7 | HIGH | Employee |
| 5 | SalaryAdvanceController | 5 | MEDIUM | Employee |

**Endpoints in Phase**: 30
**Test Focus**:
- Department CRUD (hierarchy)
- Position CRUD within departments
- Employee CRUD with department/position assignment
- Salary record management
- Salary advance request/approve/reject flow

---

## Phase 7: Delivery, Expense & Messaging (Week 7)

**Goal**: Test delivery regions, expense tracking, SMS, and Telegram integration.

| # | Controller | Endpoints | Priority | Dependencies |
|---|-----------|-----------|----------|--------------|
| 1 | DeliveryRegionController | 6 | HIGH | Auth |
| 2 | DeliveryVillageController | 7 | HIGH | DeliveryRegion |
| 3 | ExpenseRecordController | 4 | HIGH | Account |
| 4 | SmsController | 14 | MEDIUM | Auth |
| 5 | TelegramAdminController | 11 | MEDIUM | Auth |
| 6 | TelegramController | 3 | LOW | Auth |

**Endpoints in Phase**: 45
**Test Focus**:
- Delivery region CRUD with pricing
- Village CRUD within regions
- Expense record CRUD with category and approval
- SMS template CRUD, sending, delivery tracking
- Telegram bot configuration and messaging

---

## Phase 8: Mobile & Reports (Week 7-8)

**Goal**: Test mobile-specific APIs and report generation.

| # | Controller | Endpoints | Priority | Dependencies |
|---|-----------|-----------|----------|--------------|
| 1 | MobileAuthController | 7 | HIGH | Auth |
| 2 | MobileDashboardController | 4 | MEDIUM | Multi-module |
| 3 | MobileAlertController | 9 | MEDIUM | Auth |
| 4 | MobileQuickActionController | 6 | MEDIUM | POS, Inventory |
| 5 | MobileShiftController | 5 | MEDIUM | POS |
| 6 | MobileSyncController | 4 | LOW | Multi-module |
| 7 | ReportController | 32 | HIGH | Multi-module |

**Endpoints in Phase**: 67
**Test Focus**:
- Mobile authentication (device registration, PIN)
- Mobile dashboard aggregation
- Push notification/alert CRUD
- Quick actions (quick sale, stock check)
- Mobile shift management
- Offline sync flow
- Report definition CRUD
- Report execution with parameters
- Report scheduling
- Report export (PDF, Excel, CSV)

---

## Phase 9: Cross-Module Integration (Week 8-9)

**Goal**: End-to-end business flows spanning multiple modules.

| # | Scenario | Modules Involved |
|---|----------|-----------------|
| 1 | Complete Sale Flow | POS -> Inventory -> Finance (stock deduction + revenue journal entry) |
| 2 | Purchase to Payment | Inventory -> Finance (PO -> Receive -> AP Invoice -> Payment) |
| 3 | Employee Payroll | HR -> Finance (Salary calculation -> Journal entry -> Payment) |
| 4 | Customer Credit Sale | POS -> Finance (Sale on credit -> AR Invoice -> Payment tracking) |
| 5 | Inventory Valuation | Inventory -> Finance (Stock value -> GL account balances) |
| 6 | Period Close | Finance (Close period -> validate all entries posted -> lock) |
| 7 | Year-End Close | Finance (Close all periods -> retained earnings -> new year) |
| 8 | Returns & Refunds | POS -> Inventory -> Finance (Return -> restock -> credit note) |
| 9 | Delivery Costing | POS -> Delivery -> Finance (Order -> delivery fee -> revenue split) |
| 10 | Report Accuracy | Reports -> Finance/Inventory/POS (verify report data matches source) |

**Test Focus**:
- Data consistency across modules after multi-step operations
- Transaction rollback on partial failures
- Concurrent operation handling
- Audit trail completeness

---

## Implementation Guidelines

### Test Class Structure
```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class XxxControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired XxxRepository repository;
    
    // Use real DB, real services, real repositories
    // Each test method is transactional (auto-rollback)
}
```

### Authentication in Tests
```java
private Authentication createAuth(Long userId, Long tenantId, String... roles) {
    var authorities = Arrays.stream(roles)
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toList());
    var principal = new UserPrincipal(userId, "testuser", "pass", 
        tenantId, true, true, authorities);
    return new UsernamePasswordAuthenticationToken(principal, null, authorities);
}
```

### Test Naming Convention
```
test[Operation]_[Scenario]_[ExpectedResult]
e.g., testCreate_WithValidData_ReturnsCreated
e.g., testUpdate_NonExistentId_ReturnsNotFound
e.g., testDelete_WithDependencies_ReturnsBadRequest
```

### Each CRUD Test Should Cover
1. **Create** - valid data, invalid data (validation), duplicate checks
2. **Read** - by ID (found/not found), list with pagination, search/filter
3. **Update** - valid update, partial update, not found, stale data (optimistic lock)
4. **Delete** - success, not found, with dependencies (cascade/prevent)
5. **Authorization** - correct tenant isolation, permission checks
6. **Edge Cases** - empty strings, null values, max lengths, special characters

### Priority Levels
- **CRITICAL**: Core business functionality, breaks other modules if failing
- **HIGH**: Important features, frequently used
- **MEDIUM**: Secondary features, less critical paths
- **LOW**: Admin/reporting features, rarely changing

---

## Progress Tracking

| Phase | Status | Tests Written | Tests Passing |
|-------|--------|---------------|---------------|
| Phase 1: Foundation & Auth | COMPLETED | 109 | 109 |
| Phase 2: Finance Core | COMPLETED | 166 | 166 |
| Phase 3: Finance Transactions | NOT STARTED | 0 | 0 |
| Phase 4: Inventory | NOT STARTED | 0 | 0 |
| Phase 5: POS | NOT STARTED | 0 | 0 |
| Phase 6: HR & Payroll | NOT STARTED | 0 | 0 |
| Phase 7: Delivery, Expense & Messaging | NOT STARTED | 0 | 0 |
| Phase 8: Mobile & Reports | NOT STARTED | 0 | 0 |
| Phase 9: Cross-Module Integration | NOT STARTED | 0 | 0 |

**Estimated Total Integration Tests**: ~1500-2000 test methods
**Estimated Timeline**: 8-9 weeks (sequential) or 4-5 weeks (parallel team)

---

## Known Technical Considerations

1. **H2 Compatibility**: Use `TEXT` instead of `JSONB`, quote reserved words (`year`)
2. **Flyway Disabled**: Schema created by Hibernate DDL in test profile
3. **Tenant Isolation**: Every test must verify cross-tenant data is not accessible
4. **Transaction Rollback**: `@Transactional` on test class ensures clean state per test
5. **Test Data Builders**: Create shared builder utilities for common entities (User, Account, Product)
6. **Order Independence**: Tests must not depend on execution order
7. **Spring Context Caching**: Use `@DirtiesContext` sparingly to avoid slow test suites
