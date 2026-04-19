# Section 2a-ii: Finance — Accounts Receivable & Customer Ledger — Test Plan

---

## 1. ARInvoiceService Unit Tests

Framework: JUnit 5 + Mockito.

### 1.1 `getARInvoices(tenantId, pageable)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getARInvoices_returnsPaginatedResults` | `getARInvoices(tenantId, pageable)` | Repository returns page | Returns `Page<ARInvoiceDto>` |
| `getARInvoices_returnsEmpty_whenNone` | `getARInvoices(tenantId, pageable)` | No invoices | Returns empty page |
| `getARInvoices_respectsTenantIsolation` | `getARInvoices(tenantId, pageable)` | Two tenants | Only tenant-A invoices returned |

### 1.2 `createARInvoice(tenantId, request)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `createARInvoice_success_returnsDraftDto` | `createARInvoice(tenantId, request)` | Valid customer + lines | Returns dto with `status=DRAFT` |
| `createARInvoice_customerNotFound_throwsNotFoundException` | `createARInvoice(tenantId, request)` | Customer id missing | Throws `NotFoundException` |
| `createARInvoice_emptyLines_throwsValidationException` | `createARInvoice(tenantId, request)` | No line items | Throws `ValidationException` |
| `createARInvoice_duplicateInvoiceNumber_throwsDuplicateResourceException` | `createARInvoice(tenantId, request)` | Invoice number exists for customer | Throws `DuplicateResourceException` |

### 1.3 `approveARInvoice(tenantId, invoiceId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `approveARInvoice_draft_becomesApproved` | `approveARInvoice(tenantId, id)` | Invoice `DRAFT` | Status becomes `APPROVED` |
| `approveARInvoice_notDraft_throwsBusinessException` | `approveARInvoice(tenantId, id)` | Invoice already `APPROVED` | Throws `BusinessException` |

### 1.4 `postARInvoice(tenantId, invoiceId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `postARInvoice_approved_createsJournalEntry` | `postARInvoice(tenantId, id)` | Invoice `APPROVED` | Status `POSTED`; GL journal entry created |
| `postARInvoice_draft_throwsBusinessException` | `postARInvoice(tenantId, id)` | Not approved | Throws `BusinessException` |

### 1.5 `cancelARInvoice(tenantId, invoiceId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `cancelARInvoice_draft_cancelled` | `cancelARInvoice(tenantId, id)` | `DRAFT` invoice | Status becomes `CANCELLED` |
| `cancelARInvoice_hasPayments_throwsBusinessException` | `cancelARInvoice(tenantId, id)` | Invoice partially paid | Throws `BusinessException` |

### 1.6 `getOverdueARInvoices(tenantId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getOverdueARInvoices_returnsPastDueUnpaid` | `getOverdueARInvoices(tenantId)` | 3 past due, 1 future | Returns list of 3 |
| `getOverdueARInvoices_returnsEmpty_whenNone` | `getOverdueARInvoices(tenantId)` | All paid or not due | Returns empty list |

### 1.7 `getCustomerBalance(tenantId, customerId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getCustomerBalance_returnsNetBalance` | `getCustomerBalance(tenantId, customerId)` | Invoices total 1000, payments total 400 | Returns balance 600 |
| `getCustomerBalance_returnsZero_whenFullyPaid` | `getCustomerBalance(tenantId, customerId)` | All invoices fully paid | Returns `BigDecimal.ZERO` |
| `getCustomerBalance_customerNotFound_throwsNotFoundException` | `getCustomerBalance(tenantId, customerId)` | Customer missing | Throws `NotFoundException` |

---

## 2. ARPaymentService Unit Tests

### 2.1 `createARPayment(tenantId, request)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `createARPayment_success_reducesAmountDue` | `createARPayment(tenantId, request)` | Payment amount ≤ invoice balance | Payment saved; invoice balance reduced |
| `createARPayment_fullyPaid_statusUpdated` | `createARPayment(tenantId, request)` | Payment = remaining balance | Invoice status becomes `FULLY_PAID` |
| `createARPayment_overpayment_throwsBusinessException` | `createARPayment(tenantId, request)` | Amount > balance | Throws `BusinessException` |
| `createARPayment_zeroPmt_throwsValidationException` | `createARPayment(tenantId, request)` | `amount=0` | Throws `ValidationException` |
| `createARPayment_invoiceNotFound_throwsNotFoundException` | `createARPayment(tenantId, request)` | Invoice missing | Throws `NotFoundException` |

### 2.2 `voidARPayment(tenantId, paymentId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `voidARPayment_success_restoresBalance` | `voidARPayment(tenantId, id)` | Active payment | Voided; invoice balance restored |
| `voidARPayment_alreadyVoided_throwsBusinessException` | `voidARPayment(tenantId, id)` | Already voided | Throws `BusinessException` |

---

## 3. CustomerService Unit Tests

### 3.1 CRUD + Auto-code

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getCustomers_returnsPaged` | `getCustomers(tenantId, pageable)` | Multiple customers | Returns correct paged result |
| `getCustomer_found_returnsDto` | `getCustomer(tenantId, id)` | Customer exists | Returns `CustomerDto` |
| `getCustomer_notFound_throwsNotFoundException` | `getCustomer(tenantId, id)` | Missing customer | Throws `NotFoundException` |
| `createCustomer_success_autoCodeGenerated` | `createCustomer(tenantId, request)` | Valid request | Returns dto with non-null `code` like `CUST-000001` |
| `createCustomer_duplicateName_throwsDuplicateResourceException` | `createCustomer(tenantId, request)` | Name already exists | Throws `DuplicateResourceException` |
| `updateCustomer_success_updatesFields` | `updateCustomer(tenantId, id, request)` | Valid update | Returns updated dto |
| `updateCustomer_notFound_throwsNotFoundException` | `updateCustomer(tenantId, id, request)` | Missing customer | Throws `NotFoundException` |
| `deleteCustomer_success_removesRecord` | `deleteCustomer(tenantId, id)` | Customer with no transactions | Record deleted |
| `deleteCustomer_hasTransactions_throwsBusinessException` | `deleteCustomer(tenantId, id)` | Customer has POS transactions | Throws `BusinessException` |

---

## 4. Repository Tests (`@DataJpaTest` + Testcontainers)

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `ARInvoiceRepository_findByCustomerId` | `findByCustomerId(customerId)` | 3 invoices for customer A | Returns 3 |
| `ARInvoiceRepository_findByStatus` | `findByStatus(POSTED)` | Mix of statuses | Returns only POSTED |
| `ARInvoiceRepository_findOverdueInvoices` | `findOverdueInvoices(today)` | 2 past due | Returns 2 |
| `ARInvoiceRepository_sumBalanceByCustomer` | `sumAmountDueByCustomer(customerId)` | 3 invoices with balances | Returns correct sum |
| `ARPaymentRepository_findByInvoiceId` | `findByInvoiceId(invoiceId)` | 2 payments | Returns 2 |
| `ARPaymentRepository_findByCustomerId` | `findByCustomerId(customerId)` | Multiple payments | Returns all for customer |
| `CustomerRepository_findByCode` | `findByCode(tenantId, code)` | Code "CUST-000001" exists | Returns non-empty Optional |
| `CustomerRepository_searchByNameOrPhone` | `searchByNameOrPhone(tenantId, "Ali")` | Customers matching name | Returns matching customers |

---

## 5. Integration Tests — ARInvoiceController, ARPaymentController, CustomerController

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|---|---|---|---|
| `getARInvoices_returns200` | `GET /api/v1/finance/ar/invoices` | Bearer `FINANCE_READ` | `200 OK`; paged JSON |
| `getARInvoices_returns403` | `GET /api/v1/finance/ar/invoices` | No `FINANCE_READ` | `403 Forbidden` |
| `getARInvoice_returns200_whenFound` | `GET /api/v1/finance/ar/invoices/{id}` | Bearer `FINANCE_READ` | `200 OK` |
| `getARInvoice_returns404` | `GET /api/v1/finance/ar/invoices/{id}` | Bearer `FINANCE_READ`; unknown id | `404 Not Found` |
| `createARInvoice_returns201` | `POST /api/v1/finance/ar/invoices` | Bearer `FINANCE_WRITE`; valid | `201 Created`; status=DRAFT |
| `createARInvoice_returns404_unknownCustomer` | `POST /api/v1/finance/ar/invoices` | Bearer `FINANCE_WRITE`; bad customerId | `404 Not Found` |
| `approveARInvoice_returns200` | `PUT /api/v1/finance/ar/invoices/{id}/approve` | Bearer `FINANCE_WRITE` | `200 OK`; status=APPROVED |
| `approveARInvoice_returns422_notDraft` | `PUT /api/v1/finance/ar/invoices/{id}/approve` | Bearer `FINANCE_WRITE` | `422 Unprocessable Entity` |
| `postARInvoice_returns200` | `PUT /api/v1/finance/ar/invoices/{id}/post` | Bearer `FINANCE_WRITE` | `200 OK`; status=POSTED |
| `getOverdueARInvoices_returns200` | `GET /api/v1/finance/ar/invoices/overdue` | Bearer `FINANCE_READ` | `200 OK`; array |
| `createARPayment_returns201` | `POST /api/v1/finance/ar/payments` | Bearer `FINANCE_WRITE`; valid | `201 Created` |
| `createARPayment_returns422_overpayment` | `POST /api/v1/finance/ar/payments` | Bearer `FINANCE_WRITE` | `422 Unprocessable Entity` |
| `voidARPayment_returns200` | `PUT /api/v1/finance/ar/payments/{id}/void` | Bearer `FINANCE_WRITE` | `200 OK` |
| `getCustomers_returns200` | `GET /api/v1/customers` | Bearer `CUSTOMER_READ` | `200 OK`; paged |
| `createCustomer_returns201_withAutoCode` | `POST /api/v1/customers` | Bearer `CUSTOMER_WRITE`; valid | `201 Created`; code not null |
| `updateCustomer_returns200` | `PUT /api/v1/customers/{id}` | Bearer `CUSTOMER_WRITE` | `200 OK`; updated fields |
| `deleteCustomer_returns204` | `DELETE /api/v1/customers/{id}` | Bearer `CUSTOMER_WRITE`; no txns | `204 No Content` |
| `deleteCustomer_returns422_hasTransactions` | `DELETE /api/v1/customers/{id}` | Bearer `CUSTOMER_WRITE`; has txns | `422 Unprocessable Entity` |
| `getCustomerBalance_returns200` | `GET /api/v1/customers/{id}/balance` | Bearer `FINANCE_READ` | `200 OK`; numeric balance |
