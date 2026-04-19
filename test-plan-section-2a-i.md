# Section 2a-i: Finance — Accounts Payable — Test Plan

---

## 1. APInvoiceService Unit Tests

Framework: JUnit 5 + Mockito.

### 1.1 `getAPInvoices(tenantId, pageable)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getAPInvoices_returnsPaginatedResults` | `getAPInvoices(tenantId, pageable)` | Repository returns a populated page | Returns `Page<APInvoiceDto>` with correct count and mapped fields |
| `getAPInvoices_returnsEmptyPage_whenNoneExist` | `getAPInvoices(tenantId, pageable)` | Repository returns empty page | Returns empty `Page<APInvoiceDto>` |
| `getAPInvoices_respectsTenantIsolation` | `getAPInvoices(tenantId, pageable)` | Two tenants have invoices | Only tenant-A invoices returned |

### 1.2 `getAPInvoice(tenantId, invoiceId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getAPInvoice_found_returnsDto` | `getAPInvoice(tenantId, id)` | Invoice exists for tenant | Returns `APInvoiceDto` with all fields |
| `getAPInvoice_notFound_throwsNotFoundException` | `getAPInvoice(tenantId, id)` | No invoice with given id | Throws `NotFoundException` referencing invoiceId |
| `getAPInvoice_wrongTenant_throwsNotFoundException` | `getAPInvoice(tenantId, id)` | Invoice belongs to different tenant | Throws `NotFoundException` |

### 1.3 `createAPInvoice(tenantId, request)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `createAPInvoice_success_returnsDraftDto` | `createAPInvoice(tenantId, request)` | Valid request with existing vendor | Returns `APInvoiceDto` with `status=DRAFT` |
| `createAPInvoice_vendorNotFound_throwsNotFoundException` | `createAPInvoice(tenantId, request)` | Vendor id does not exist | Throws `NotFoundException` referencing vendorId |
| `createAPInvoice_emptyLines_throwsValidationException` | `createAPInvoice(tenantId, request)` | Request has no line items | Throws `ValidationException` |
| `createAPInvoice_duplicateInvoiceNumber_throwsDuplicateResourceException` | `createAPInvoice(tenantId, request)` | Invoice number already exists for vendor | Throws `DuplicateResourceException` |

### 1.4 `approveAPInvoice(tenantId, invoiceId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `approveAPInvoice_draft_statusBecomesApproved` | `approveAPInvoice(tenantId, id)` | Invoice in `DRAFT` status | Returns dto with `status=APPROVED` |
| `approveAPInvoice_alreadyApproved_throwsBusinessException` | `approveAPInvoice(tenantId, id)` | Invoice already `APPROVED` | Throws `BusinessException` |
| `approveAPInvoice_cancelled_throwsBusinessException` | `approveAPInvoice(tenantId, id)` | Invoice `CANCELLED` | Throws `BusinessException` |

### 1.5 `postAPInvoice(tenantId, invoiceId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `postAPInvoice_approved_createsJournalEntry` | `postAPInvoice(tenantId, id)` | Invoice `APPROVED`; GL configured | Status becomes `POSTED`; `JournalEntryService.create` called once |
| `postAPInvoice_draft_throwsBusinessException` | `postAPInvoice(tenantId, id)` | Invoice still `DRAFT` | Throws `BusinessException` |

### 1.6 `cancelAPInvoice(tenantId, invoiceId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `cancelAPInvoice_draft_cancelled` | `cancelAPInvoice(tenantId, id)` | Invoice `DRAFT` | Status becomes `CANCELLED` |
| `cancelAPInvoice_partiallyPaid_throwsBusinessException` | `cancelAPInvoice(tenantId, id)` | Invoice has payments | Throws `BusinessException` "Cannot cancel partially paid invoice" |

### 1.7 `getOverdueAPInvoices(tenantId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getOverdueAPInvoices_returnsPastDueUnpaid` | `getOverdueAPInvoices(tenantId)` | 2 invoices past due date, 1 future | Returns list of 2 |
| `getOverdueAPInvoices_returnsEmpty_whenNone` | `getOverdueAPInvoices(tenantId)` | All invoices paid or not yet due | Returns empty list |

---

## 2. APPaymentService Unit Tests

### 2.1 `createAPPayment(tenantId, request)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `createAPPayment_success_reducesAmountDue` | `createAPPayment(tenantId, request)` | Valid payment against APPROVED invoice | Payment saved; invoice `amountDue` reduced |
| `createAPPayment_invoiceNotFound_throwsNotFoundException` | `createAPPayment(tenantId, request)` | Invoice does not exist | Throws `NotFoundException` |
| `createAPPayment_overpayment_throwsBusinessException` | `createAPPayment(tenantId, request)` | Amount exceeds `amountDue` | Throws `BusinessException` "Payment exceeds invoice balance" |
| `createAPPayment_zeroPmt_throwsValidationException` | `createAPPayment(tenantId, request)` | `amount=0` | Throws `ValidationException` |
| `createAPPayment_fullyPaidInvoice_statusBecomesFullyPaid` | `createAPPayment(tenantId, request)` | Payment equals remaining balance | Invoice status changes to `FULLY_PAID` |

### 2.2 `getAPPayments(tenantId, pageable)` / `getAPPaymentsByInvoice(tenantId, invoiceId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getAPPayments_returnsPaged` | `getAPPayments(tenantId, pageable)` | Multiple payments exist | Returns correct paged result |
| `getAPPaymentsByInvoice_returnsOnly_invoicePayments` | `getAPPaymentsByInvoice(tenantId, invoiceId)` | Payments for two invoices | Returns only payments for specified invoice |

### 2.3 `voidAPPayment(tenantId, paymentId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `voidAPPayment_success_reversesAmountDue` | `voidAPPayment(tenantId, id)` | Valid payment in `COMPLETED` state | Payment voided; invoice `amountDue` restored |
| `voidAPPayment_alreadyVoided_throwsBusinessException` | `voidAPPayment(tenantId, id)` | Payment already voided | Throws `BusinessException` |

---

## 3. Repository Tests (`@DataJpaTest` + Testcontainers)

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `APInvoiceRepository_findByVendorId_returnsList` | `findByVendorId(vendorId)` | 3 invoices for vendor A, 2 for B | Returns 3 for vendor A |
| `APInvoiceRepository_findByStatus_returnsMatchingOnly` | `findByStatus(DRAFT)` | Mix of statuses | Returns only DRAFT invoices |
| `APInvoiceRepository_findOverdueInvoices_returnsPastDue` | `findOverdueInvoices(today)` | 2 past due, 1 future | Returns 2 |
| `APInvoiceRepository_sumByVendorAndDateRange` | `sumTotalByVendorAndDateRange(...)` | 3 invoices in range, 1 outside | Returns sum of 3 |
| `APPaymentRepository_findByInvoiceId` | `findByInvoiceId(invoiceId)` | 2 payments for invoice A, 1 for B | Returns 2 |
| `APPaymentRepository_findByVendorId` | `findByVendorId(vendorId)` | Multiple payments | Returns all for vendor |
| `APInvoiceLineRepository_findByInvoiceId` | `findByInvoiceId(invoiceId)` | 4 lines on invoice | Returns 4 |

---

## 4. Mapper Tests

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `APInvoiceMapper_toDto_mapsAllFields` | `APInvoiceMapper.toDto(entity)` | Populated entity with vendor, lines | DTO has matching vendorId, lineCount, status |
| `APInvoiceMapper_fromCreateRequest_mapsRequest` | `APInvoiceMapper.fromCreateRequest(req)` | Valid create request | Entity has correct vendor ref and lines |
| `APInvoiceLineMapper_toDto_mapsAllFields` | `APInvoiceLineMapper.toDto(line)` | Line with product, qty, unitPrice | DTO has lineTotal = qty × unitPrice |
| `APPaymentMapper_toDto_mapsAllFields` | `APPaymentMapper.toDto(payment)` | Payment entity | DTO has correct amount, invoiceId, paymentDate |

---

## 5. Integration Tests — APInvoiceController & APPaymentController

Base path: `/api/v1/finance/ap`

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|---|---|---|---|
| `getAPInvoices_returns200_withFinanceRead` | `GET /api/v1/finance/ap/invoices` | Bearer `FINANCE_READ` | `200 OK`; paged JSON |
| `getAPInvoices_returns403_withoutPermission` | `GET /api/v1/finance/ap/invoices` | Bearer without `FINANCE_READ` | `403 Forbidden` |
| `getAPInvoice_returns200_whenFound` | `GET /api/v1/finance/ap/invoices/{id}` | Bearer `FINANCE_READ` | `200 OK`; invoice JSON |
| `getAPInvoice_returns404_whenNotFound` | `GET /api/v1/finance/ap/invoices/{id}` | Bearer `FINANCE_READ`; unknown id | `404 Not Found` |
| `createAPInvoice_returns201_withValidRequest` | `POST /api/v1/finance/ap/invoices` | Bearer `FINANCE_WRITE`; valid body | `201 Created`; status=DRAFT |
| `createAPInvoice_returns404_whenVendorNotFound` | `POST /api/v1/finance/ap/invoices` | Bearer `FINANCE_WRITE`; bad vendorId | `404 Not Found` |
| `createAPInvoice_returns400_whenEmptyLines` | `POST /api/v1/finance/ap/invoices` | Bearer `FINANCE_WRITE`; no lines | `400 Bad Request` |
| `approveAPInvoice_returns200_fromDraft` | `PUT /api/v1/finance/ap/invoices/{id}/approve` | Bearer `FINANCE_WRITE` | `200 OK`; status=APPROVED |
| `approveAPInvoice_returns422_whenAlreadyApproved` | `PUT /api/v1/finance/ap/invoices/{id}/approve` | Bearer `FINANCE_WRITE`; already approved | `422 Unprocessable Entity` |
| `postAPInvoice_returns200_fromApproved` | `PUT /api/v1/finance/ap/invoices/{id}/post` | Bearer `FINANCE_WRITE` | `200 OK`; status=POSTED |
| `cancelAPInvoice_returns200_fromDraft` | `PUT /api/v1/finance/ap/invoices/{id}/cancel` | Bearer `FINANCE_WRITE` | `200 OK`; status=CANCELLED |
| `cancelAPInvoice_returns422_whenHasPayments` | `PUT /api/v1/finance/ap/invoices/{id}/cancel` | Bearer `FINANCE_WRITE`; paid invoice | `422 Unprocessable Entity` |
| `getOverdueAPInvoices_returns200_withList` | `GET /api/v1/finance/ap/invoices/overdue` | Bearer `FINANCE_READ` | `200 OK`; array of overdue invoices |
| `createAPPayment_returns201_reducesBalance` | `POST /api/v1/finance/ap/payments` | Bearer `FINANCE_WRITE`; valid body | `201 Created`; invoice balance reduced |
| `createAPPayment_returns422_whenOverpayment` | `POST /api/v1/finance/ap/payments` | Bearer `FINANCE_WRITE`; amount > balance | `422 Unprocessable Entity` |
| `getAPPayments_returns200_paged` | `GET /api/v1/finance/ap/payments` | Bearer `FINANCE_READ` | `200 OK`; paged payments |
| `voidAPPayment_returns200_restoresBalance` | `PUT /api/v1/finance/ap/payments/{id}/void` | Bearer `FINANCE_WRITE` | `200 OK`; invoice balance restored |
| `voidAPPayment_returns422_whenAlreadyVoided` | `PUT /api/v1/finance/ap/payments/{id}/void` | Bearer `FINANCE_WRITE`; already voided | `422 Unprocessable Entity` |
