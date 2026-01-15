# Backend Implementation Plan
## Inventory Management Platform - Monolithic Architecture

---

## Technology Stack

### Core Technologies
| Technology | Version | Purpose |
|------------|---------|---------|
| **Java** | 21 (LTS) | Primary language |
| **Spring Boot** | 3.3.x | Application framework |
| **Spring Security** | 6.x | Authentication & authorization |
| **Spring Data JPA** | 3.x | Data persistence |
| **PostgreSQL** | 16 | Primary database |
| **Redis** | 7.x | Caching & session management |

### Boilerplate Reduction & Productivity
| Tool | Purpose |
|------|---------|
| **Lombok** | Reduce boilerplate (getters, setters, builders) |
| **MapStruct** | Type-safe object mapping (DTO ↔ Entity) |
| **Springdoc OpenAPI** | Auto-generate API documentation |
| **Spring Boot DevTools** | Hot reload during development |

### Database & Migrations
| Tool | Purpose |
|------|---------|
| **Flyway** | Database version control & migrations |
| **QueryDSL** | Type-safe complex queries |
| **Hibernate Envers** | Entity auditing & history |

### Testing & Quality
| Tool | Purpose |
|------|---------|
| **JUnit 5** | Unit testing |
| **Testcontainers** | Integration testing with real containers |
| **Mockito** | Mocking framework |
| **ArchUnit** | Architecture testing |
| **JaCoCo** | Code coverage |

### Infrastructure
| Tool | Purpose |
|------|---------|
| **Docker** | Containerization |
| **Docker Compose** | Local development & simple deployment |
| **GitHub Actions** | CI/CD pipeline |
| **Prometheus + Grafana** | Monitoring & alerting |

---

## Monolithic Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         MONOLITHIC APPLICATION                               │
│                         (Single Spring Boot App)                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                        REST API Layer                                │    │
│  │    /api/v1/auth  /api/v1/inventory  /api/v1/finance  /api/v1/pos    │    │
│  │    /api/v1/admin  /api/v1/mobile  /api/v1/web  /api/v1/reports      │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                    │                                         │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                      Service Layer (Business Logic)                  │    │
│  ├────────┬──────────┬─────────┬───────┬───────┬────────┬─────────────┤    │
│  │  Auth  │ Inventory│ Finance │  POS  │ Admin │ Mobile │     Web     │    │
│  │ Module │  Module  │ Module  │Module │Module │ Module │   Module    │    │
│  └────────┴──────────┴─────────┴───────┴───────┴────────┴─────────────┘    │
│                                    │                                         │
│                    Direct Method Calls (No Message Queue)                    │
│                                    │                                         │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                      Repository Layer (Data Access)                  │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                    │                                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                              Redis Cache                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                         PostgreSQL Database                                  │
│                         (Single Database)                                    │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Package Structure (Monolith)
```
com.hisobnoma.platform/
├── HisobnomaApplication.java
├── config/                          # All configurations
│   ├── SecurityConfig.java
│   ├── CacheConfig.java
│   ├── WebConfig.java
│   └── OpenApiConfig.java
├── common/                          # Shared utilities
│   ├── entity/
│   ├── dto/
│   ├── exception/
│   └── util/
├── auth/                            # Authentication module
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   └── dto/
├── inventory/                       # Inventory module
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   └── dto/
├── finance/                         # Finance module
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   └── dto/
├── pos/                             # POS module
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   └── dto/
├── admin/                           # Admin dashboard module
│   ├── controller/
│   ├── service/
│   └── dto/
├── mobile/                          # Mobile API module
│   ├── controller/
│   ├── service/
│   └── dto/
├── web/                             # Web/E-commerce module
│   ├── controller/
│   ├── service/
│   ├── entity/
│   └── dto/
└── reporting/                       # Reporting module
    ├── controller/
    ├── service/
    └── dto/
```

---

# IMPLEMENTATION BLOCKS

---

## BLOCK 1: Project Foundation & Setup
**Complexity: Medium**

### Checkpoint 1.1: Project Initialization
- [ ] Create Spring Boot 3.3.x project with Spring Initializr
- [ ] Configure Maven with all dependencies
- [ ] Setup package structure as defined above
- [ ] Configure application.yml for multiple profiles (dev, test, prod)
- [ ] Setup Lombok and MapStruct annotation processors

### Checkpoint 1.2: Common Module Setup
- [ ] Create BaseEntity with id, createdAt, updatedAt
- [ ] Create AuditableEntity extending BaseEntity with createdBy, updatedBy
- [ ] Create TenantAwareEntity for multi-tenancy
- [ ] Create ApiResponse wrapper class
- [ ] Create PageResponse for pagination
- [ ] Create ErrorResponse for error handling

### Checkpoint 1.3: Exception Handling
- [ ] Create BusinessException base class
- [ ] Create NotFoundException
- [ ] Create ValidationException
- [ ] Create UnauthorizedException
- [ ] Create ForbiddenException
- [ ] Create GlobalExceptionHandler (@ControllerAdvice)

### Checkpoint 1.4: Docker Development Environment
- [ ] Create docker-compose.yml with PostgreSQL and Redis
- [ ] Create Dockerfile for the application
- [ ] Create database initialization scripts
- [ ] Setup environment variable configuration

### Checkpoint 1.5: CI/CD Pipeline
- [ ] Create GitHub Actions workflow for build & test
- [ ] Create workflow for Docker image build
- [ ] Setup code quality checks (Checkstyle)
- [ ] Configure test coverage reporting

**Deliverables:**
- Working Spring Boot project with proper structure
- Docker development environment
- CI/CD pipeline

---

## BLOCK 2: Role-Based Access Control (RBAC) System
**Complexity: High**

### Checkpoint 2.1: RBAC Domain Model
- [x] Create User entity
  - id, username, phone, passwordHash, firstName, lastName
  - enabled, locked, phoneVerified
  - lastLoginAt, failedLoginAttempts, lockedUntil
  - tenantId (for multi-tenancy)
  - **Note:** Phone-based auth chosen over email for better adoption in target markets
- [ ] Create Role entity
  - id, name, code, description
  - isSystemRole (cannot be deleted)
  - tenantId
- [ ] Create Permission entity
  - id, name, code, description
  - module (INVENTORY, FINANCE, POS, ADMIN, REPORTS)
  - action (CREATE, READ, UPDATE, DELETE, EXPORT, APPROVE)
- [ ] Create UserRole join entity (user_id, role_id)
- [ ] Create RolePermission join entity (role_id, permission_id)
- [ ] Write Flyway migrations

### Checkpoint 2.2: Default Roles & Permissions Setup
- [ ] Create migration for default permissions:
  ```
  # Inventory Permissions
  INVENTORY_PRODUCT_CREATE, INVENTORY_PRODUCT_READ, INVENTORY_PRODUCT_UPDATE, INVENTORY_PRODUCT_DELETE
  INVENTORY_STOCK_READ, INVENTORY_STOCK_ADJUST, INVENTORY_STOCK_TRANSFER
  INVENTORY_RECEIVING_CREATE, INVENTORY_RECEIVING_APPROVE
  INVENTORY_COUNT_CREATE, INVENTORY_COUNT_APPROVE

  # Finance Permissions
  FINANCE_GL_READ, FINANCE_GL_POST, FINANCE_GL_CLOSE_PERIOD
  FINANCE_AP_CREATE, FINANCE_AP_APPROVE, FINANCE_AP_PAY
  FINANCE_AR_CREATE, FINANCE_AR_RECEIVE_PAYMENT
  FINANCE_REPORTS_VIEW, FINANCE_REPORTS_EXPORT

  # POS Permissions
  POS_SALE_CREATE, POS_SALE_VOID, POS_SALE_REFUND
  POS_DISCOUNT_APPLY, POS_DISCOUNT_OVERRIDE
  POS_DRAWER_OPEN, POS_DRAWER_CLOSE, POS_DRAWER_RECONCILE
  POS_REPORTS_VIEW

  # Admin Permissions
  ADMIN_USER_MANAGE, ADMIN_ROLE_MANAGE, ADMIN_PERMISSION_MANAGE
  ADMIN_TENANT_MANAGE, ADMIN_SETTINGS_MANAGE
  ADMIN_AUDIT_VIEW, ADMIN_SYSTEM_MONITOR

  # Report Permissions
  REPORTS_INVENTORY_VIEW, REPORTS_FINANCE_VIEW, REPORTS_SALES_VIEW
  REPORTS_EXPORT, REPORTS_SCHEDULE
  ```
- [ ] Create default roles:
  - SUPER_ADMIN (all permissions)
  - ADMIN (all except system settings)
  - INVENTORY_MANAGER (all inventory + inventory reports)
  - FINANCE_MANAGER (all finance + finance reports)
  - ACCOUNTANT (finance read + create, no approve)
  - STORE_MANAGER (POS + inventory read + sales reports)
  - CASHIER (POS sale only, no void/refund)
  - WAREHOUSE_STAFF (inventory operations, no approve)
  - VIEWER (read-only access)

### Checkpoint 2.3: Spring Security Configuration
- [ ] Configure SecurityFilterChain
- [ ] Implement JwtAuthenticationFilter
- [ ] Implement JwtTokenProvider (generate, validate, refresh)
- [ ] Configure password encoder (BCrypt)
- [ ] Configure CORS settings
- [ ] Configure session management (stateless)

### Checkpoint 2.4: Permission Checking Infrastructure
- [ ] Create @RequiresPermission annotation
- [ ] Create PermissionAspect for checking permissions
- [ ] Create SecurityContextHelper utility
- [ ] Implement permission caching with Redis
- [ ] Create permission evaluation service

### Checkpoint 2.5: Authentication APIs
- [ ] POST /api/v1/auth/login - Login with username/password
- [ ] POST /api/v1/auth/logout - Invalidate token
- [ ] POST /api/v1/auth/refresh - Refresh access token
- [ ] POST /api/v1/auth/forgot-password - Request password reset
- [ ] POST /api/v1/auth/reset-password - Reset with token
- [ ] GET /api/v1/auth/me - Get current user profile
- [ ] PUT /api/v1/auth/change-password - Change own password

### Checkpoint 2.6: User Management APIs
- [ ] GET /api/v1/users - List users (paginated, filterable)
- [ ] GET /api/v1/users/{id} - Get user details
- [ ] POST /api/v1/users - Create user
- [ ] PUT /api/v1/users/{id} - Update user
- [ ] DELETE /api/v1/users/{id} - Soft delete user
- [ ] PUT /api/v1/users/{id}/roles - Assign roles to user
- [ ] PUT /api/v1/users/{id}/lock - Lock/unlock user
- [ ] PUT /api/v1/users/{id}/reset-password - Admin reset password

### Checkpoint 2.7: Role Management APIs
- [ ] GET /api/v1/roles - List roles
- [ ] GET /api/v1/roles/{id} - Get role with permissions
- [ ] POST /api/v1/roles - Create custom role
- [ ] PUT /api/v1/roles/{id} - Update role
- [ ] DELETE /api/v1/roles/{id} - Delete role (if not system)
- [ ] PUT /api/v1/roles/{id}/permissions - Assign permissions
- [ ] GET /api/v1/permissions - List all permissions

### Checkpoint 2.8: Multi-Tenancy Support
- [ ] Create Tenant entity (id, name, code, settings)
- [ ] Implement TenantFilter for automatic tenant filtering
- [ ] Implement TenantContext (ThreadLocal)
- [ ] Configure tenant resolution from JWT

### Checkpoint 2.9: RBAC Tests
- [ ] Unit tests for permission checking
- [ ] Integration tests for authentication flow
- [ ] Tests for role-based access control
- [ ] Tests for multi-tenancy isolation

**Deliverables:**
- Complete RBAC system
- User authentication with JWT
- Role and permission management
- Multi-tenancy support

---

## BLOCK 3: Inventory Module - Product Catalog
**Complexity: Medium**

### Checkpoint 3.1: Product Domain Model
- [ ] Create Category entity (hierarchical with parentId)
- [ ] Create Brand entity
- [ ] Create UnitOfMeasure entity
- [ ] Create Product entity
  - id, sku, barcode, name, description
  - categoryId, brandId, baseUomId
  - costPrice, sellingPrice
  - trackInventory, allowNegativeStock
  - minStockLevel, reorderPoint
  - isActive, isService
- [ ] Create ProductVariant entity (size, color combinations)
- [ ] Create ProductImage entity
- [ ] Create ProductAttribute entity (custom fields)
- [ ] Write Flyway migrations

### Checkpoint 3.2: Product Catalog APIs
- [ ] CRUD for Categories (with hierarchy support)
- [ ] CRUD for Brands
- [ ] CRUD for Units of Measure
- [ ] CRUD for Products
- [ ] Product variant management
- [ ] Product image upload/delete
- [ ] Product search with filters (name, sku, category, brand)
- [ ] Product barcode lookup
- [ ] Product import from CSV/Excel
- [ ] Product export to CSV/Excel

### Checkpoint 3.3: SKU & Barcode Services
- [ ] Create SKUGeneratorService (configurable patterns)
- [ ] Create BarcodeGeneratorService (Code128, EAN13)
- [ ] Create BarcodeValidatorService

### Checkpoint 3.4: Product Catalog Tests
- [ ] Unit tests for services
- [ ] Integration tests for APIs
- [ ] Test category hierarchy operations

**Deliverables:**
- Product catalog management
- Category hierarchy
- Barcode/SKU system

---

## BLOCK 4: Inventory Module - Stock Management
**Complexity: High**

### Checkpoint 4.1: Location Domain Model
- [ ] Create Location entity
  - id, code, name, type (WAREHOUSE, STORE, VIRTUAL)
  - address, isActive
  - parentLocationId (for zones/bins)
- [ ] Create LocationType enum
- [ ] Write Flyway migrations

### Checkpoint 4.2: Stock Domain Model
- [ ] Create Stock entity
  - id, productId, locationId
  - quantityOnHand, quantityReserved, quantityAvailable
- [ ] Create StockMovement entity
  - id, productId, fromLocationId, toLocationId
  - quantity, movementType, referenceType, referenceId
  - reason, notes, createdBy, createdAt
- [ ] Create MovementType enum (STOCK_IN, STOCK_OUT, TRANSFER, ADJUSTMENT)
- [ ] Create StockBatch entity (for batch tracking)
  - id, productId, locationId, batchNumber
  - quantity, expiryDate, manufactureDate
- [ ] Create SerialNumber entity (for serial tracking)
- [ ] Write Flyway migrations

### Checkpoint 4.3: Stock Query APIs
- [ ] GET /api/v1/stock - Get stock levels (filterable by product, location)
- [ ] GET /api/v1/stock/product/{productId} - Stock by product across locations
- [ ] GET /api/v1/stock/location/{locationId} - All stock at location
- [ ] GET /api/v1/stock/low-stock - Products below reorder point
- [ ] GET /api/v1/stock/movements - Stock movement history
- [ ] GET /api/v1/stock/valuation - Inventory valuation report

### Checkpoint 4.4: Stock Movement APIs
- [ ] POST /api/v1/stock/receive - Stock in (receiving)
- [ ] POST /api/v1/stock/issue - Stock out (issue)
- [ ] POST /api/v1/stock/transfer - Inter-location transfer
- [ ] POST /api/v1/stock/adjust - Stock adjustment with reason
- [ ] Implement automatic stock updates on POS sale (internal call)

### Checkpoint 4.5: Batch & Serial Tracking
- [ ] Batch creation on receiving
- [ ] Serial number assignment
- [ ] Batch/serial lookup APIs
- [ ] Expiry date tracking
- [ ] Expiry alerts

### Checkpoint 4.6: Stock Integration with Other Modules
- [ ] Create StockService with methods for POS to call directly
- [ ] Create InventoryEventPublisher for internal events
- [ ] Implement stock reservation for pending sales
- [ ] Implement automatic stock deduction on sale completion

### Checkpoint 4.7: Stock Management Tests
- [ ] Unit tests for stock calculations
- [ ] Concurrent stock update tests
- [ ] Test stock reservation logic
- [ ] Integration tests

**Deliverables:**
- Multi-location stock management
- Real-time stock tracking
- Batch and serial support
- Internal integration with POS

---

## BLOCK 5: Inventory Module - Operations
**Complexity: High**

### Checkpoint 5.1: Purchase Order Domain
- [ ] Create PurchaseOrder entity
  - id, poNumber, vendorId, status
  - orderDate, expectedDate
  - totalAmount, notes
- [ ] Create PurchaseOrderLine entity
- [ ] Create POStatus enum (DRAFT, APPROVED, PARTIAL, RECEIVED, CANCELLED)
- [ ] Write migrations

### Checkpoint 5.2: Purchase Order APIs
- [ ] CRUD for Purchase Orders
- [ ] PO approval workflow
- [ ] PO line management

### Checkpoint 5.3: Receiving Domain & APIs
- [ ] Create ReceivingOrder entity
- [ ] Create ReceivingLine entity
- [ ] POST /api/v1/receiving - Create receiving from PO
- [ ] PUT /api/v1/receiving/{id}/receive - Confirm receiving
- [ ] Handle partial receiving
- [ ] Handle variances (over/under receiving)
- [ ] Auto-update stock on receiving (direct service call)
- [ ] Auto-create AP invoice on receiving (direct service call to Finance)

### Checkpoint 5.4: Inventory Counting
- [ ] Create InventoryCount entity
- [ ] Create InventoryCountLine entity
- [ ] Create CountStatus enum (DRAFT, IN_PROGRESS, COMPLETED, APPROVED)
- [ ] POST /api/v1/inventory-count - Create count
- [ ] PUT /api/v1/inventory-count/{id}/count - Submit counts
- [ ] PUT /api/v1/inventory-count/{id}/approve - Approve with adjustments
- [ ] Auto-create adjustments on approval (direct call)

### Checkpoint 5.5: Inventory Planning APIs
- [ ] GET /api/v1/inventory/reorder-suggestions
- [ ] GET /api/v1/inventory/abc-analysis
- [ ] GET /api/v1/inventory/slow-moving
- [ ] GET /api/v1/inventory/dead-stock

### Checkpoint 5.6: Operations Tests
- [ ] Test receiving workflows
- [ ] Test counting and adjustments
- [ ] Test planning calculations

**Deliverables:**
- Purchase order management
- Receiving workflow
- Inventory counting
- Planning tools

---

## BLOCK 6: Finance Module - General Ledger
**Complexity: Very High**

### Checkpoint 6.1: Chart of Accounts Domain
- [ ] Create Account entity
  - id, code, name, accountType
  - parentAccountId (hierarchical)
  - isActive, isSystemAccount
  - normalBalance (DEBIT, CREDIT)
- [ ] Create AccountType enum (ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE)
- [ ] Create default chart of accounts template
- [ ] Write migrations

### Checkpoint 6.2: Chart of Accounts APIs
- [ ] CRUD for Accounts
- [ ] Account hierarchy management
- [ ] Account activation/deactivation
- [ ] Chart of accounts import

### Checkpoint 6.3: Fiscal Period Domain
- [ ] Create FiscalYear entity
- [ ] Create FiscalPeriod entity (12 periods per year)
- [ ] Create PeriodStatus enum (OPEN, CLOSED, LOCKED)
- [ ] Period open/close logic
- [ ] Year-end closing process

### Checkpoint 6.4: Journal Entry Domain
- [ ] Create JournalEntry entity
  - id, entryNumber, entryDate
  - description, status, source
  - postedAt, postedBy
- [ ] Create JournalLine entity
  - id, journalEntryId, accountId
  - debitAmount, creditAmount
  - description
- [ ] Create JournalStatus enum (DRAFT, POSTED, REVERSED)
- [ ] Implement double-entry validation (debits = credits)
- [ ] Write migrations

### Checkpoint 6.5: Journal Entry APIs
- [ ] POST /api/v1/gl/journal-entries - Create entry
- [ ] PUT /api/v1/gl/journal-entries/{id}/post - Post entry
- [ ] POST /api/v1/gl/journal-entries/{id}/reverse - Reverse entry
- [ ] GET /api/v1/gl/journal-entries - List entries
- [ ] Recurring journal template support

### Checkpoint 6.6: GL Integration Service
- [ ] Create GLIntegrationService for other modules to call
- [ ] Method: postInventoryMovement(movement) - COGS entries
- [ ] Method: postSalesTransaction(sale) - Revenue entries
- [ ] Method: postPurchaseInvoice(invoice) - AP entries
- [ ] Method: postPayment(payment) - Cash/Bank entries

### Checkpoint 6.7: Multi-Currency Support
- [ ] Create Currency entity
- [ ] Create ExchangeRate entity
- [ ] Exchange rate service
- [ ] Multi-currency transaction handling

### Checkpoint 6.8: GL Tests
- [ ] Test double-entry validation
- [ ] Test trial balance calculation
- [ ] Test period closing
- [ ] Test integration postings

**Deliverables:**
- Complete general ledger
- Chart of accounts
- Journal entry system
- Integration service for other modules

---

## BLOCK 7: Finance Module - Accounts Payable
**Complexity: High**

### Checkpoint 7.1: Vendor Domain
- [ ] Create Vendor entity
  - id, code, name, taxId
  - email, phone, address
  - paymentTerms, creditLimit
  - defaultExpenseAccountId
- [ ] Create VendorContact entity
- [ ] Write migrations

### Checkpoint 7.2: Vendor APIs
- [ ] CRUD for Vendors
- [ ] Vendor search and filtering
- [ ] Vendor statement generation

### Checkpoint 7.3: AP Invoice Domain
- [ ] Create APInvoice entity
  - id, invoiceNumber, vendorId
  - invoiceDate, dueDate
  - totalAmount, paidAmount, balanceDue
  - status (DRAFT, APPROVED, PARTIAL, PAID, CANCELLED)
  - purchaseOrderId, receivingOrderId (for matching)
- [ ] Create APInvoiceLine entity
- [ ] Write migrations

### Checkpoint 7.4: AP Invoice APIs
- [ ] POST /api/v1/ap/invoices - Create invoice
- [ ] PUT /api/v1/ap/invoices/{id}/approve - Approve invoice
- [ ] 3-way matching validation (PO, Receiving, Invoice)
- [ ] Auto-create from receiving (called by Inventory module)
- [ ] Post to GL on approval (call GLIntegrationService)

### Checkpoint 7.5: Payment Processing
- [ ] Create APPayment entity
- [ ] Create APPaymentAllocation entity
- [ ] POST /api/v1/ap/payments - Create payment
- [ ] Payment allocation to invoices
- [ ] Post to GL on payment (call GLIntegrationService)

### Checkpoint 7.6: AP Reports
- [ ] GET /api/v1/ap/aging - AP aging report
- [ ] GET /api/v1/ap/vendor-balance - Vendor balances

### Checkpoint 7.7: AP Tests
- [ ] Test 3-way matching
- [ ] Test payment allocation
- [ ] Test GL integration

**Deliverables:**
- Vendor management
- AP invoice processing
- Payment processing
- AP reporting

---

## BLOCK 8: Finance Module - Accounts Receivable
**Complexity: High**

### Checkpoint 8.1: Customer Domain
- [ ] Create Customer entity
  - id, code, name, taxId
  - email, phone, address
  - paymentTerms, creditLimit, currentBalance
  - priceListId
- [ ] Create CustomerContact entity
- [ ] Write migrations

### Checkpoint 8.2: Customer APIs
- [ ] CRUD for Customers
- [ ] Customer search and filtering
- [ ] Credit limit management
- [ ] Customer statement generation

### Checkpoint 8.3: AR Invoice Domain
- [ ] Create ARInvoice entity
  - id, invoiceNumber, customerId
  - invoiceDate, dueDate
  - totalAmount, paidAmount, balanceDue
  - status, posTransactionId
- [ ] Create ARInvoiceLine entity
- [ ] Create CreditNote entity
- [ ] Write migrations

### Checkpoint 8.4: AR Invoice APIs
- [ ] POST /api/v1/ar/invoices - Create invoice
- [ ] Auto-create from POS sale (called by POS module)
- [ ] Credit note processing
- [ ] Post to GL (call GLIntegrationService)

### Checkpoint 8.5: Payment Collection
- [ ] Create ARPayment entity
- [ ] Create ARPaymentAllocation entity
- [ ] POST /api/v1/ar/payments - Record payment
- [ ] Payment allocation to invoices
- [ ] Post to GL (call GLIntegrationService)

### Checkpoint 8.6: AR Reports
- [ ] GET /api/v1/ar/aging - AR aging report
- [ ] GET /api/v1/ar/customer-balance - Customer balances

### Checkpoint 8.7: AR Tests
- [ ] Test invoice creation from POS
- [ ] Test payment allocation
- [ ] Test GL integration

**Deliverables:**
- Customer management
- AR invoicing (auto from POS)
- Payment collection
- AR reporting

---

## BLOCK 9: Finance Module - Banking & Tax
**Complexity: Medium**

### Checkpoint 9.1: Bank Account Domain
- [ ] Create BankAccount entity
- [ ] Create BankTransaction entity
- [ ] Write migrations

### Checkpoint 9.2: Banking APIs
- [ ] CRUD for Bank Accounts
- [ ] Bank transaction recording
- [ ] Bank reconciliation process
- [ ] Cash flow tracking

### Checkpoint 9.3: Tax Configuration
- [ ] Create TaxCode entity
- [ ] Create TaxRate entity
- [ ] Tax calculation service
- [ ] Inclusive/exclusive tax support

### Checkpoint 9.4: Tax APIs
- [ ] CRUD for Tax Codes
- [ ] Tax calculation endpoints
- [ ] Tax reports (VAT/GST summary)

### Checkpoint 9.5: Banking & Tax Tests
- [ ] Test reconciliation
- [ ] Test tax calculations

**Deliverables:**
- Bank account management
- Bank reconciliation
- Tax configuration
- Tax reporting

---

## BLOCK 10: POS Module - Core Transactions
**Complexity: Very High**

### Checkpoint 10.1: POS Domain Model
- [ ] Create POSTerminal entity
- [ ] Create Shift entity
  - id, terminalId, cashierId
  - openedAt, closedAt, status
  - openingCash, closingCash
- [ ] Create POSTransaction entity
  - id, transactionNumber, terminalId, shiftId
  - customerId, transactionType (SALE, RETURN, EXCHANGE)
  - subtotal, discountAmount, taxAmount, totalAmount
  - status (PENDING, COMPLETED, VOIDED, HELD)
  - completedAt
- [ ] Create POSTransactionLine entity
- [ ] Create POSPayment entity
- [ ] Write migrations

### Checkpoint 10.2: Terminal & Shift APIs
- [ ] POST /api/v1/pos/terminals - Register terminal
- [ ] POST /api/v1/pos/shifts/open - Open shift
- [ ] POST /api/v1/pos/shifts/close - Close shift with reconciliation
- [ ] GET /api/v1/pos/shifts/current - Get current shift

### Checkpoint 10.3: Transaction APIs
- [ ] POST /api/v1/pos/transactions - Start new transaction
- [ ] PUT /api/v1/pos/transactions/{id}/lines - Add/remove line items
- [ ] PUT /api/v1/pos/transactions/{id}/discount - Apply discount
- [ ] PUT /api/v1/pos/transactions/{id}/hold - Hold transaction
- [ ] GET /api/v1/pos/transactions/held - Get held transactions
- [ ] PUT /api/v1/pos/transactions/{id}/recall - Recall held
- [ ] DELETE /api/v1/pos/transactions/{id} - Void transaction

### Checkpoint 10.4: Payment APIs
- [ ] POST /api/v1/pos/transactions/{id}/payments - Add payment
- [ ] Support multiple payment types (CASH, CARD, CREDIT, GIFT_CARD)
- [ ] Split payment support
- [ ] Change calculation
- [ ] POST /api/v1/pos/transactions/{id}/complete - Complete sale

### Checkpoint 10.5: POS Integration with Other Modules
- [ ] On sale complete:
  - [ ] Call StockService.deductStock() - reduce inventory
  - [ ] Call ARService.createInvoice() - create AR invoice if credit sale
  - [ ] Call GLIntegrationService.postSales() - post to GL
- [ ] Use @Transactional for atomicity

### Checkpoint 10.6: Returns & Refunds
- [ ] POST /api/v1/pos/returns - Create return transaction
- [ ] Return with original receipt lookup
- [ ] Refund processing
- [ ] Auto-adjust stock and GL (direct calls)

### Checkpoint 10.7: POS Tests
- [ ] Test transaction flow
- [ ] Test payment processing
- [ ] Test integration with inventory and finance
- [ ] Test concurrent transactions

**Deliverables:**
- Complete POS transaction processing
- Multiple payment methods
- Returns and refunds
- Integrated with Inventory and Finance

---

## BLOCK 11: POS Module - Pricing & Promotions
**Complexity: High**

### Checkpoint 11.1: Pricing Domain
- [ ] Create PriceList entity
- [ ] Create PriceListItem entity (product-specific prices)
- [ ] Create CustomerPriceList mapping
- [ ] Write migrations

### Checkpoint 11.2: Pricing Engine
- [ ] Create PricingService
- [ ] Base price lookup
- [ ] Customer-specific pricing
- [ ] Quantity-based pricing (tiered)
- [ ] Location-specific pricing

### Checkpoint 11.3: Promotion Domain
- [ ] Create Promotion entity
  - id, name, type, startDate, endDate
  - isActive, priority
- [ ] Create PromotionCondition entity (what triggers it)
- [ ] Create PromotionAction entity (what discount to apply)
- [ ] Create Coupon entity
- [ ] Write migrations

### Checkpoint 11.4: Promotion Engine
- [ ] Create PromotionService
- [ ] Percentage discount
- [ ] Fixed amount discount
- [ ] Buy X Get Y (BOGO)
- [ ] Bundle pricing
- [ ] Coupon redemption
- [ ] Promotion stacking rules

### Checkpoint 11.5: Pricing & Promo APIs
- [ ] CRUD for Price Lists
- [ ] CRUD for Promotions
- [ ] POST /api/v1/pos/calculate-price - Calculate final price
- [ ] POST /api/v1/pos/apply-coupon - Validate and apply coupon

### Checkpoint 11.6: Pricing Tests
- [ ] Test price calculation scenarios
- [ ] Test promotion stacking
- [ ] Test coupon validation

**Deliverables:**
- Flexible pricing engine
- Promotion management
- Coupon support

---

## BLOCK 12: Admin Dashboard Module
**Complexity: High**

### Checkpoint 12.1: System Configuration
- [ ] Create SystemSetting entity
- [ ] Create TenantSetting entity
- [ ] Configuration APIs
  - GET/PUT /api/v1/admin/settings - System settings
  - GET/PUT /api/v1/admin/settings/tenant - Tenant settings

### Checkpoint 12.2: User Activity Monitoring
- [ ] Create UserActivity entity (login, logout, actions)
- [ ] GET /api/v1/admin/users/activity - User activity log
- [ ] GET /api/v1/admin/users/online - Currently online users
- [ ] GET /api/v1/admin/users/login-history - Login history

### Checkpoint 12.3: Audit Trail
- [ ] Configure Hibernate Envers for entity auditing
- [ ] Create AuditLog entity for action logging
- [ ] GET /api/v1/admin/audit - Audit trail with filters
- [ ] GET /api/v1/admin/audit/entity/{type}/{id} - Entity change history

### Checkpoint 12.4: System Monitoring APIs
- [ ] GET /api/v1/admin/system/health - System health status
- [ ] GET /api/v1/admin/system/metrics - Key system metrics
  - Active users count
  - Transactions today
  - Database connection pool status
  - Cache hit/miss rates
- [ ] GET /api/v1/admin/system/storage - Storage usage

### Checkpoint 12.5: Business Overview APIs
- [ ] GET /api/v1/admin/dashboard/summary - Executive summary
  - Total revenue (today, week, month)
  - Total transactions
  - Inventory value
  - Pending AP/AR
- [ ] GET /api/v1/admin/dashboard/trends - Trend data
  - Revenue trend (last 30 days)
  - Transaction count trend
  - Top selling products
  - Top customers

### Checkpoint 12.6: Data Management
- [ ] POST /api/v1/admin/data/export - Export data (CSV/Excel)
- [ ] POST /api/v1/admin/data/import - Import data
- [ ] POST /api/v1/admin/data/backup - Trigger backup
- [ ] GET /api/v1/admin/data/backups - List backups

### Checkpoint 12.7: Tenant Management (Super Admin)
- [ ] CRUD for Tenants
- [ ] Tenant activation/deactivation
- [ ] Tenant data isolation verification
- [ ] Tenant usage statistics

### Checkpoint 12.8: Notification Management
- [ ] Create NotificationTemplate entity
- [ ] CRUD for notification templates
- [ ] Email template management
- [ ] SMS template management

### Checkpoint 12.9: Admin Dashboard Tests
- [ ] Test audit trail completeness
- [ ] Test metrics calculation
- [ ] Test tenant isolation

**Deliverables:**
- System configuration management
- User activity monitoring
- Complete audit trail
- System health monitoring
- Executive dashboard APIs

---

## BLOCK 13: Mobile App Integration Module
**Complexity: High**

### Checkpoint 13.1: Mobile Authentication
- [ ] POST /api/v1/mobile/auth/login - Mobile login
- [ ] POST /api/v1/mobile/auth/refresh - Refresh token
- [ ] POST /api/v1/mobile/auth/register-device - Register for push notifications
- [ ] Support for biometric authentication tokens

### Checkpoint 13.2: Revenue & Sales APIs
- [ ] GET /api/v1/mobile/dashboard/revenue - Revenue summary
  - Today's revenue
  - Yesterday's revenue
  - This week's revenue
  - This month's revenue
  - Revenue comparison (vs previous period)
- [ ] GET /api/v1/mobile/dashboard/revenue/chart - Revenue chart data
  - Hourly breakdown (today)
  - Daily breakdown (this month)
  - Monthly breakdown (this year)
- [ ] GET /api/v1/mobile/sales/transactions - Recent transactions
- [ ] GET /api/v1/mobile/sales/by-location - Sales by location
- [ ] GET /api/v1/mobile/sales/by-category - Sales by category
- [ ] GET /api/v1/mobile/sales/top-products - Top selling products

### Checkpoint 13.3: Inventory Status APIs
- [ ] GET /api/v1/mobile/inventory/summary - Inventory overview
  - Total SKU count
  - Total inventory value
  - Low stock items count
  - Out of stock items count
- [ ] GET /api/v1/mobile/inventory/low-stock - Low stock alerts
- [ ] GET /api/v1/mobile/inventory/out-of-stock - Out of stock items
- [ ] GET /api/v1/mobile/inventory/search - Quick product search
- [ ] GET /api/v1/mobile/inventory/product/{id} - Product details with stock
- [ ] GET /api/v1/mobile/inventory/movements - Recent stock movements

### Checkpoint 13.4: Financial Overview APIs
- [ ] GET /api/v1/mobile/finance/summary - Financial overview
  - Cash balance
  - Bank balance
  - AR outstanding
  - AP outstanding
  - Profit margin
- [ ] GET /api/v1/mobile/finance/cashflow - Cash flow summary
- [ ] GET /api/v1/mobile/finance/receivables - AR summary
- [ ] GET /api/v1/mobile/finance/payables - AP summary

### Checkpoint 13.5: Alerts & Notifications
- [ ] GET /api/v1/mobile/alerts - All alerts
  - Low stock alerts
  - Expiring inventory alerts
  - Overdue AR alerts
  - Large transaction alerts
  - System alerts
- [ ] PUT /api/v1/mobile/alerts/{id}/read - Mark as read
- [ ] GET /api/v1/mobile/alerts/settings - Alert preferences
- [ ] PUT /api/v1/mobile/alerts/settings - Update preferences

### Checkpoint 13.6: Quick Actions
- [ ] GET /api/v1/mobile/inventory/barcode/{barcode} - Barcode lookup
- [ ] POST /api/v1/mobile/inventory/quick-count - Quick stock count
- [ ] POST /api/v1/mobile/pos/quick-sale - Simple quick sale
- [ ] GET /api/v1/mobile/customers/search - Customer lookup

### Checkpoint 13.7: Push Notification Service
- [ ] Create DeviceToken entity
- [ ] Firebase Cloud Messaging (FCM) integration
- [ ] Push notification for:
  - Low stock alerts
  - Large sales alerts
  - Daily summary
  - Payment received
  - System alerts

### Checkpoint 13.8: Mobile Offline Support
- [ ] GET /api/v1/mobile/sync/products - Product catalog for offline
- [ ] GET /api/v1/mobile/sync/customers - Customer list for offline
- [ ] GET /api/v1/mobile/sync/last-updated - Check for updates
- [ ] Lightweight response formats for mobile

### Checkpoint 13.9: Mobile API Tests
- [ ] Test all mobile endpoints
- [ ] Test push notification delivery
- [ ] Test offline sync data
- [ ] Performance testing for mobile APIs

**Deliverables:**
- Complete mobile API layer
- Revenue and sales dashboards
- Inventory monitoring
- Financial overview
- Push notifications
- Offline support

---

## BLOCK 14: Web E-commerce Module
**Complexity: High**

> **Authentication Model:**
> - All browsing, cart, and pre-checkout APIs are **PUBLIC** (no login required)
> - Authentication via **Phone Number + OTP** only (no email/social login)
> - Login required only when: placing order, managing addresses, wishlist, writing reviews

### Checkpoint 14.1: Web Customer Authentication (Phone + OTP Only)
- [ ] POST /api/v1/web/auth/send-otp - Send OTP to phone number
- [ ] POST /api/v1/web/auth/verify-otp - Verify OTP and login/register
  - Auto-creates account if phone number is new
  - Returns JWT token on success
- [ ] POST /api/v1/web/auth/resend-otp - Resend OTP code
- [ ] POST /api/v1/web/auth/refresh - Refresh access token
- [ ] POST /api/v1/web/auth/logout - Logout and invalidate token
- [ ] GET /api/v1/web/auth/me - Get customer profile (authenticated)
- [ ] PUT /api/v1/web/auth/me - Update customer profile (authenticated)
- [ ] Create OTP entity (phoneNumber, code, expiresAt, verified)
- [ ] Integrate SMS gateway for OTP delivery
- [ ] OTP expiration (5 minutes) and rate limiting

### Checkpoint 14.2: Product Catalog APIs (Public)
- [ ] GET /api/v1/web/products - List products (paginated, filterable)
- [ ] GET /api/v1/web/products/{id} - Product details
- [ ] GET /api/v1/web/products/{id}/reviews - Product reviews
- [ ] GET /api/v1/web/products/search - Full-text product search
- [ ] GET /api/v1/web/products/featured - Featured products
- [ ] GET /api/v1/web/products/bestsellers - Best selling products
- [ ] GET /api/v1/web/products/new-arrivals - New arrivals
- [ ] GET /api/v1/web/categories - Category tree
- [ ] GET /api/v1/web/categories/{id}/products - Products by category
- [ ] GET /api/v1/web/brands - All brands
- [ ] GET /api/v1/web/brands/{id}/products - Products by brand

### Checkpoint 14.3: Shopping Cart Domain
- [ ] Create WebCart entity
  - id, customerId (nullable for guest), sessionId
  - createdAt, updatedAt, expiresAt
- [ ] Create WebCartItem entity
  - id, cartId, productId, variantId
  - quantity, unitPrice, totalPrice
- [ ] Write migrations

### Checkpoint 14.4: Shopping Cart APIs (Public - No Auth Required)
- [ ] POST /api/v1/web/cart - Create cart (uses session/device ID)
- [ ] GET /api/v1/web/cart - Get current cart by session
- [ ] POST /api/v1/web/cart/items - Add item to cart
- [ ] PUT /api/v1/web/cart/items/{itemId} - Update item quantity
- [ ] DELETE /api/v1/web/cart/items/{itemId} - Remove item from cart
- [ ] DELETE /api/v1/web/cart - Clear cart
- [ ] POST /api/v1/web/cart/apply-coupon - Apply coupon code
- [ ] DELETE /api/v1/web/cart/coupon - Remove coupon
- [ ] Cart identified by session token (stored in header/cookie)
- [ ] Cart persisted in Redis with TTL (24 hours)

### Checkpoint 14.5: Web Order Domain
- [ ] Create WebOrder entity
  - id, orderNumber, customerId
  - status (PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED)
  - subtotal, discountAmount, taxAmount, shippingAmount, totalAmount
  - shippingAddress, billingAddress
  - paymentMethod, paymentStatus
  - notes, createdAt
- [ ] Create WebOrderItem entity
- [ ] Create WebOrderStatusHistory entity
- [ ] Write migrations

### Checkpoint 14.6: Checkout APIs
**Public (No Auth):**
- [ ] POST /api/v1/web/checkout/validate - Validate cart before checkout
- [ ] GET /api/v1/web/checkout/shipping-methods - Available shipping methods
- [ ] POST /api/v1/web/checkout/calculate - Calculate totals with shipping

**Requires Phone OTP Authentication:**
- [ ] POST /api/v1/web/orders - Place order (requires authenticated user)
  - If not logged in, prompt for phone + OTP first
  - Links cart to customer account
  - Creates order with customer's phone number
- [ ] GET /api/v1/web/orders - Customer order history
- [ ] GET /api/v1/web/orders/{id} - Order details
- [ ] GET /api/v1/web/orders/{id}/track - Order tracking
- [ ] POST /api/v1/web/orders/{id}/cancel - Cancel order (if allowed)

### Checkpoint 14.7: Payment Integration
- [ ] POST /api/v1/web/payments/initiate - Initiate payment
- [ ] POST /api/v1/web/payments/callback - Payment gateway callback
- [ ] GET /api/v1/web/payments/{orderId}/status - Payment status
- [ ] Support multiple payment gateways:
  - [ ] Credit/Debit Card (Stripe)
  - [ ] PayPal
  - [ ] Cash on Delivery (COD)

### Checkpoint 14.8: Customer Address Management (Authenticated)
- [ ] GET /api/v1/web/addresses - List saved addresses
- [ ] POST /api/v1/web/addresses - Add new address
- [ ] PUT /api/v1/web/addresses/{id} - Update address
- [ ] DELETE /api/v1/web/addresses/{id} - Delete address
- [ ] PUT /api/v1/web/addresses/{id}/default - Set as default

### Checkpoint 14.9: Wishlist (Authenticated)
- [ ] GET /api/v1/web/wishlist - Get wishlist
- [ ] POST /api/v1/web/wishlist/{productId} - Add to wishlist
- [ ] DELETE /api/v1/web/wishlist/{productId} - Remove from wishlist
- [ ] POST /api/v1/web/wishlist/{productId}/move-to-cart - Move to cart

### Checkpoint 14.10: Product Reviews (Authenticated for Write)
- [ ] GET /api/v1/web/products/{id}/reviews - View reviews (public)
- [ ] POST /api/v1/web/products/{id}/reviews - Submit review (authenticated)
- [ ] PUT /api/v1/web/reviews/{id} - Update own review (authenticated)
- [ ] DELETE /api/v1/web/reviews/{id} - Delete own review (authenticated)
- [ ] Review moderation (admin side)

### Checkpoint 14.11: Web Module Integration
- [ ] On order placement:
  - [ ] Call StockService.reserveStock() - reserve inventory
  - [ ] Call StockService.deductStock() - on order confirmation
  - [ ] Call ARService.createInvoice() - create invoice
  - [ ] Call GLIntegrationService.postSales() - post to GL
- [ ] Real-time stock availability check
- [ ] Use @Transactional for atomicity

### Checkpoint 14.12: Web-Specific Features
- [ ] GET /api/v1/web/store-info - Store information (name, logo, contact)
- [ ] GET /api/v1/web/pages/{slug} - CMS pages (about, terms, privacy)
- [ ] POST /api/v1/web/contact - Contact form submission
- [ ] POST /api/v1/web/newsletter/subscribe - Newsletter subscription
- [ ] GET /api/v1/web/banners - Homepage banners/promotions

### Checkpoint 14.13: Web Module Tests
- [ ] Test cart operations
- [ ] Test checkout flow
- [ ] Test payment integration
- [ ] Test order management
- [ ] Test stock reservation
- [ ] Load testing for concurrent orders

**Deliverables:**
- Complete e-commerce API layer
- Shopping cart with guest support
- Checkout and order management
- Payment gateway integration
- Customer account management
- Wishlist and reviews
- Integration with Inventory and Finance

---

## BLOCK 15: Reporting Module
**Complexity: High**

### Checkpoint 15.1: Report Framework
- [ ] Create Report entity (report definitions)
- [ ] Create ReportSchedule entity
- [ ] Report parameter handling
- [ ] Export to PDF, Excel, CSV

### Checkpoint 15.2: Inventory Reports
- [ ] Stock on Hand report
- [ ] Stock Movement report
- [ ] Inventory Valuation report
- [ ] Reorder report
- [ ] ABC Analysis report
- [ ] Expiry report

### Checkpoint 15.3: Financial Reports
- [ ] Trial Balance
- [ ] Balance Sheet
- [ ] Income Statement (P&L)
- [ ] Cash Flow Statement
- [ ] AP Aging report
- [ ] AR Aging report
- [ ] General Ledger Detail

### Checkpoint 15.4: Sales Reports
- [ ] Daily Sales Summary
- [ ] Sales by Product/Category
- [ ] Sales by Location
- [ ] Sales by Payment Method
- [ ] Cashier Performance
- [ ] Returns Analysis
- [ ] Hourly Sales Analysis

### Checkpoint 15.5: Report APIs
- [ ] GET /api/v1/reports/list - Available reports
- [ ] POST /api/v1/reports/{code}/generate - Generate report
- [ ] GET /api/v1/reports/{code}/export - Export report
- [ ] Report scheduling APIs

### Checkpoint 15.6: Reporting Tests
- [ ] Test report generation
- [ ] Test export formats
- [ ] Test data accuracy

**Deliverables:**
- Comprehensive reporting engine
- All major reports
- Export capabilities

---

## BLOCK 16: Notifications & Alerts
**Complexity: Medium**

### Checkpoint 16.1: Notification Infrastructure
- [ ] Create NotificationLog entity
- [ ] Create NotificationPreference entity
- [ ] Email service (Spring Mail)
- [ ] SMS service integration

### Checkpoint 16.2: Email Notifications
- [ ] Thymeleaf email templates
- [ ] Order confirmation emails
- [ ] Invoice emails
- [ ] Password reset emails
- [ ] Low stock alerts
- [ ] Payment reminders

### Checkpoint 16.3: In-App Notifications
- [ ] Notification storage
- [ ] Read/unread status
- [ ] GET /api/v1/notifications - User notifications
- [ ] PUT /api/v1/notifications/{id}/read - Mark as read
- [ ] WebSocket for real-time notifications

### Checkpoint 16.4: Scheduled Alerts
- [ ] Daily summary email
- [ ] Low stock alerts
- [ ] Expiry alerts
- [ ] AR overdue alerts

### Checkpoint 16.5: Notification Tests
- [ ] Test email rendering
- [ ] Test notification delivery
- [ ] Test preferences

**Deliverables:**
- Email notifications
- In-app notifications
- Scheduled alerts

---

## BLOCK 17: External Integrations
**Complexity: Medium**

### Checkpoint 17.1: Webhook System
- [ ] Create Webhook entity
- [ ] Webhook registration API
- [ ] Event publishing
- [ ] Retry mechanism
- [ ] Signature verification

### Checkpoint 17.2: API Documentation
- [ ] Springdoc OpenAPI configuration
- [ ] Document all endpoints
- [ ] Create Postman collection
- [ ] API versioning

### Checkpoint 17.3: Payment Gateway Integration
- [ ] Abstract payment gateway interface
- [ ] Stripe integration (sample)
- [ ] Payment callback handling

### Checkpoint 17.4: Integration Tests
- [ ] Test webhook delivery
- [ ] Test payment flow

**Deliverables:**
- Webhook system
- API documentation
- Payment gateway sample

---

## BLOCK 18: Performance & Security
**Complexity: High**

### Checkpoint 18.1: Caching Strategy
- [ ] Redis caching for products
- [ ] Redis caching for prices
- [ ] Cache invalidation
- [ ] Cache metrics

### Checkpoint 18.2: Database Optimization
- [ ] Add proper indexes
- [ ] Query optimization
- [ ] Connection pooling (HikariCP)
- [ ] Slow query logging

### Checkpoint 18.3: Security Hardening
- [ ] Input validation
- [ ] SQL injection prevention (JPA handles)
- [ ] XSS prevention
- [ ] CORS configuration
- [ ] Rate limiting
- [ ] Security headers
- [ ] Sensitive data encryption

### Checkpoint 18.4: Performance Testing
- [ ] Gatling/JMeter tests
- [ ] Load testing
- [ ] Bottleneck identification

### Checkpoint 18.5: Security Testing
- [ ] OWASP dependency check
- [ ] Static code analysis

**Deliverables:**
- Optimized caching
- Security hardened
- Performance tested

---

## BLOCK 19: Deployment & DevOps
**Complexity: Medium**

### Checkpoint 19.1: Docker Deployment
- [ ] Production Dockerfile
- [ ] docker-compose for production
- [ ] Environment configuration
- [ ] Health checks

### Checkpoint 19.2: Monitoring Setup
- [ ] Prometheus metrics endpoint
- [ ] Grafana dashboards
- [ ] Alert rules
- [ ] Log aggregation

### Checkpoint 19.3: Backup & Recovery
- [ ] Database backup scripts
- [ ] Backup scheduling
- [ ] Recovery procedures

### Checkpoint 19.4: CI/CD Finalization
- [ ] Production deployment workflow
- [ ] Rollback procedures
- [ ] Blue-green deployment (optional)

**Deliverables:**
- Production-ready deployment
- Monitoring setup
- Backup procedures

---

## BLOCK 20: Documentation & Testing
**Complexity: Low**

### Checkpoint 20.1: API Documentation
- [ ] Complete OpenAPI specifications
- [ ] Postman collections
- [ ] API usage guides

### Checkpoint 20.2: Developer Documentation
- [ ] Architecture overview
- [ ] Database schema documentation
- [ ] Local development guide
- [ ] Module integration guide

### Checkpoint 20.3: Final Testing
- [ ] End-to-end tests
- [ ] Integration test suite
- [ ] UAT support

**Deliverables:**
- Complete documentation
- Test suite

---

## Summary: Implementation Order

| Block | Name | Dependencies | Complexity |
|-------|------|--------------|------------|
| 1 | Project Foundation | None | Medium |
| 2 | RBAC System | Block 1 | High |
| 3 | Inventory - Product Catalog | Block 2 | Medium |
| 4 | Inventory - Stock Management | Block 3 | High |
| 5 | Inventory - Operations | Block 4 | High |
| 6 | Finance - General Ledger | Block 2 | Very High |
| 7 | Finance - Accounts Payable | Block 5, 6 | High |
| 8 | Finance - Accounts Receivable | Block 6 | High |
| 9 | Finance - Banking & Tax | Block 6 | Medium |
| 10 | POS - Core Transactions | Block 4, 6 | Very High |
| 11 | POS - Pricing & Promotions | Block 10 | High |
| 12 | Admin Dashboard | Block 2, 6 | High |
| 13 | Mobile App Integration | Block 4, 6, 10 | High |
| 14 | **Web E-commerce** | Block 4, 6, 8, 11 | High |
| 15 | Reporting | All modules | High |
| 16 | Notifications & Alerts | Block 2 | Medium |
| 17 | External Integrations | All modules | Medium |
| 18 | Performance & Security | All modules | High |
| 19 | Deployment & DevOps | All modules | Medium |
| 20 | Documentation & Testing | All modules | Low |

---

## Module Integration Points

Since this is a **monolithic architecture**, modules communicate via **direct method calls** within the same JVM. This simplifies integration significantly.

### Key Integration Flows:

**1. POS Sale → Inventory → Finance**
```java
@Transactional
public POSTransaction completeSale(Long transactionId) {
    POSTransaction tx = posRepository.findById(transactionId);

    // Deduct inventory (direct call)
    stockService.deductStock(tx.getLines());

    // Create AR invoice if credit sale (direct call)
    if (tx.isCredit()) {
        arService.createInvoiceFromPOS(tx);
    }

    // Post to GL (direct call)
    glService.postSalesTransaction(tx);

    return posRepository.save(tx);
}
```

**2. Receiving → Inventory → Finance (AP)**
```java
@Transactional
public ReceivingOrder completeReceiving(Long receivingId) {
    ReceivingOrder receiving = receivingRepository.findById(receivingId);

    // Add stock (direct call)
    stockService.addStock(receiving.getLines());

    // Create AP invoice (direct call)
    apService.createInvoiceFromReceiving(receiving);

    return receivingRepository.save(receiving);
}
```

**3. Stock Adjustment → Finance (COGS)**
```java
@Transactional
public StockMovement adjustStock(StockAdjustmentRequest request) {
    StockMovement movement = stockService.adjust(request);

    // Post COGS adjustment (direct call)
    glService.postInventoryAdjustment(movement);

    return movement;
}
```

**4. Web Order → Inventory → Finance (AR)**
```java
@Transactional
public WebOrder placeOrder(WebOrderRequest request) {
    WebOrder order = webOrderRepository.save(createOrder(request));

    // Reserve stock (direct call)
    stockService.reserveStock(order.getItems());

    // On payment confirmation, deduct stock
    if (order.isPaid()) {
        stockService.deductStock(order.getItems());
        arService.createInvoiceFromWebOrder(order);
        glService.postSalesTransaction(order);
    }

    // Send confirmation email
    notificationService.sendOrderConfirmation(order);

    return order;
}
```

---

## Quick Start Commands

```bash
# Clone and setup
git clone <repository>
cd hisobnoma

# Start infrastructure (PostgreSQL + Redis)
docker-compose up -d

# Run application
./mvnw spring-boot:run

# Access
# API: http://localhost:8080/api/v1
# Swagger UI: http://localhost:8080/swagger-ui.html

# Run tests
./mvnw test

# Build Docker image
docker build -t hisobnoma:latest .
```

---

*Document Version: 3.0*
*Updated: 2026-01-03*
*Architecture: Monolithic*
*Total Blocks: 20*
