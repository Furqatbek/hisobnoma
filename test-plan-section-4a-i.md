# Section 4a-i: POS — Transactions & Shifts — Test Plan

---

## 1. Unit Tests

### 1.1 POSTransactionService

#### getTransactions(tenantId, pageable)

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| getTransactions_returnsPaginatedResults | getTransactions(tenantId, pageable) | Repository contains multiple transactions for tenant | Returns `Page<POSTransactionDto>` with correct content, size, and total elements matching repository result |
| getTransactions_returnsEmptyPage | getTransactions(tenantId, pageable) | Repository has no transactions for the given tenantId | Returns `Page<POSTransactionDto>` with empty content; no exception thrown |
| getTransactions_pageableRespected | getTransactions(tenantId, pageable) | Pageable specifies page 1 with size 5; 10 transactions exist | Returns second page of 5 results; `Page.getNumber()` == 1 and `Page.getSize()` == 5 |

---

#### getTransaction(tenantId, transactionId)

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| getTransaction_found | getTransaction(tenantId, transactionId) | Transaction with given id exists and belongs to tenant | Returns `POSTransactionDto` with all fields correctly mapped |
| getTransaction_notFound_throwsNotFoundException | getTransaction(tenantId, transactionId) | No transaction with given id exists for tenant | Throws `NotFoundException` with message referencing transactionId |
| getTransaction_wrongTenant_throwsNotFoundException | getTransaction(tenantId, transactionId) | Transaction exists but belongs to a different tenantId | Throws `NotFoundException`; cross-tenant data is not exposed |

---

#### createTransaction(tenantId, request)

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| createTransaction_createsDraftTransaction | createTransaction(tenantId, request) | Valid request; terminal exists; an open shift is present on the terminal | Returns `POSTransactionDto` with status `DRAFT`; repository `save` called once; shiftId set on entity |
| createTransaction_terminalNotFound_throwsNotFoundException | createTransaction(tenantId, request) | Terminal referenced in request does not exist for tenant | Throws `NotFoundException` referencing terminalId; no transaction persisted |
| createTransaction_noOpenShift_throwsBusinessException | createTransaction(tenantId, request) | Terminal exists but has no open shift | Throws `BusinessException` indicating no open shift is available for the terminal |

---

#### addLine(tenantId, transactionId, request) — product found / not found / COMPLETED lock

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| addLine_productExists_lineAdded | addLine(tenantId, transactionId, request) | Product exists; transaction is in `DRAFT` status | `POSTransactionLineDto` returned; line appended to transaction; line total = qty × unit price |
| addLine_productNotFound_throwsNotFoundException | addLine(tenantId, transactionId, request) | Product referenced in request does not exist for tenant | Throws `NotFoundException` referencing productId; no line persisted |
| addLine_transactionCompleted_throwsBusinessException | addLine(tenantId, transactionId, request) | Transaction status is `COMPLETED` | Throws `BusinessException` with message "Transaction locked" |

---

#### addLine — qty = 0 validation

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| addLine_zeroQty_throwsValidationException | addLine(tenantId, transactionId, request) | Request quantity is 0 | Throws `ValidationException` indicating quantity must be greater than zero |
| addLine_negativeQty_throwsValidationException | addLine(tenantId, transactionId, request) | Request quantity is −1 | Throws `ValidationException` indicating quantity must be greater than zero |
| addLine_nullQty_throwsValidationException | addLine(tenantId, transactionId, request) | Request quantity is null | Throws `ValidationException` indicating quantity is required |

---

#### updateLine(tenantId, transactionId, lineId, request)

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| updateLine_qtyUpdated_lineTotalRecalculated | updateLine(tenantId, transactionId, lineId, request) | Valid lineId; transaction is `DRAFT`; new qty provided | Returns updated `POSTransactionLineDto`; `lineTotal` = new qty × unit price; repository updated |
| updateLine_lineNotFound_throwsNotFoundException | updateLine(tenantId, transactionId, lineId, request) | lineId does not belong to the transaction | Throws `NotFoundException` referencing lineId |
| updateLine_completedTransaction_throwsBusinessException | updateLine(tenantId, transactionId, lineId, request) | Transaction status is `COMPLETED` | Throws `BusinessException` indicating the transaction is locked and cannot be modified |

---

#### removeLine(tenantId, transactionId, lineId)

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| removeLine_lineRemoved | removeLine(tenantId, transactionId, lineId) | Transaction has multiple lines; lineId is valid | Line deleted from repository; remaining lines intact; no exception |
| removeLine_lineNotFound_throwsNotFoundException | removeLine(tenantId, transactionId, lineId) | lineId does not exist on transaction | Throws `NotFoundException` referencing lineId; no deletion occurs |
| removeLine_lastLine_throwsBusinessException | removeLine(tenantId, transactionId, lineId) | Transaction has exactly one line and its lineId matches | Throws `BusinessException` with message "Cannot remove all lines" |

---

#### addPayment(tenantId, transactionId, request)

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| addPayment_paymentAdded_exactBalance | addPayment(tenantId, transactionId, request) | Payment amount equals remaining balance on `DRAFT` transaction | Returns `POSTransactionDto` with payment recorded; change = 0 |
| addPayment_overpayment_changeCalculated | addPayment(tenantId, transactionId, request) | Payment amount exceeds remaining balance (overpayment) | Payment accepted; `change` field on response = payment − remaining balance |
| addPayment_transactionAlreadyCompleted_throwsBusinessException | addPayment(tenantId, transactionId, request) | Transaction status is `COMPLETED` | Throws `BusinessException` indicating payment cannot be added to a completed transaction |

---

#### voidTransaction(tenantId, transactionId)

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| voidTransaction_draft_statusSetToVoided | voidTransaction(tenantId, transactionId) | Transaction is in `DRAFT` status | Returns `POSTransactionDto` with status `VOIDED`; entity saved with new status |
| voidTransaction_completed_throwsBusinessException | voidTransaction(tenantId, transactionId) | Transaction status is `COMPLETED` | Throws `BusinessException` with message "Cannot void completed transaction" |
| voidTransaction_alreadyVoided_throwsBusinessException | voidTransaction(tenantId, transactionId) | Transaction status is already `VOIDED` | Throws `BusinessException` indicating transaction is already voided |

---

#### completeTransaction(tenantId, transactionId)

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| completeTransaction_fullPayment_statusCompletedAndStockDecremented | completeTransaction(tenantId, transactionId) | Total payments cover full transaction amount; lines present | Returns `POSTransactionDto` with status `COMPLETED`; inventory decremented by line quantities; entity saved |
| completeTransaction_underpayment_throwsBusinessException | completeTransaction(tenantId, transactionId) | Total payments are less than transaction total | Throws `BusinessException` with message "Insufficient payment" |
| completeTransaction_noLines_throwsBusinessException | completeTransaction(tenantId, transactionId) | Transaction has no line items | Throws `BusinessException` with message "No items" |
| completeTransaction_noPayment_throwsBusinessException | completeTransaction(tenantId, transactionId) | Transaction has lines but no payments recorded | Throws `BusinessException` with message "Insufficient payment" |

---

### 1.2 ShiftService

#### openShift(tenantId, terminalId, request)

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| openShift_noExistingOpenShift_shiftCreatedWithOpenStatus | openShift(tenantId, terminalId, request) | Terminal exists; no open shift currently on the terminal | Returns `ShiftDto` with status `OPEN`; opening cash stored; shift persisted |
| openShift_alreadyOpenShift_throwsBusinessException | openShift(tenantId, terminalId, request) | Terminal already has an existing shift in `OPEN` status | Throws `BusinessException` with message "Shift already open" |
| openShift_terminalNotFound_throwsNotFoundException | openShift(tenantId, terminalId, request) | Terminal does not exist for the given tenantId | Throws `NotFoundException` referencing terminalId; no shift created |

---

#### closeShift(tenantId, shiftId, request)

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| closeShift_openShift_statusClosedAndSummaryCalculated | closeShift(tenantId, shiftId, request) | Shift is `OPEN`; no open transactions remain | Returns `ShiftDto` with status `CLOSED`; closing cash stored; sales/cash totals computed and stored |
| closeShift_alreadyClosed_throwsBusinessException | closeShift(tenantId, shiftId, request) | Shift status is already `CLOSED` | Throws `BusinessException` indicating the shift is already closed |
| closeShift_openTransactionsExist_throwsBusinessException | closeShift(tenantId, shiftId, request) | Shift is `OPEN` but one or more transactions are still in `DRAFT` status | Throws `BusinessException` with message "Close open transactions first" |

---

#### getOpenShift(tenantId, terminalId)

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| getOpenShift_found_returnsShiftDto | getOpenShift(tenantId, terminalId) | An `OPEN` shift exists for the terminal | Returns non-empty `Optional<ShiftDto>` containing the shift data |
| getOpenShift_noOpenShift_returnsEmpty | getOpenShift(tenantId, terminalId) | No open shift exists for the terminal | Returns `Optional.empty()`; no exception thrown |
| getOpenShift_multipleTerminals_returnsCorrectShift | getOpenShift(tenantId, terminalId) | Multiple terminals exist; only one matches | Returns `Optional<ShiftDto>` for the correct terminal only |

---

#### getShiftTransactions(tenantId, shiftId, pageable)

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| getShiftTransactions_returnsTransactionsForShift | getShiftTransactions(tenantId, shiftId, pageable) | Shift exists; several transactions recorded under it | Returns `Page<POSTransactionDto>` containing only that shift's transactions |
| getShiftTransactions_shiftNotFound_throwsNotFoundException | getShiftTransactions(tenantId, shiftId, pageable) | shiftId does not exist for the tenant | Throws `NotFoundException` referencing shiftId |
| getShiftTransactions_emptyShift_returnsEmptyPage | getShiftTransactions(tenantId, shiftId, pageable) | Shift exists but has no transactions | Returns `Page<POSTransactionDto>` with empty content; no exception |

---

#### getShiftSummary(tenantId, shiftId)

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| getShiftSummary_correctTotals | getShiftSummary(tenantId, shiftId) | Shift has multiple completed transactions with mixed payment types | Returns summary DTO with correct `transactionCount`, `totalSales`, `totalCash`, and `expectedCash` = opening cash + totalCash |
| getShiftSummary_noCompletedTransactions_zeroTotals | getShiftSummary(tenantId, shiftId) | Shift exists but all transactions are `VOIDED` or `DRAFT` | Returns summary with `totalSales` = 0, `totalCash` = 0, `transactionCount` = 0 |
| getShiftSummary_shiftNotFound_throwsNotFoundException | getShiftSummary(tenantId, shiftId) | shiftId does not exist | Throws `NotFoundException` referencing shiftId |

---

#### getSalesTotal(tenantId, shiftId)

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| getSalesTotal_sumOfCompletedTransactionTotals | getSalesTotal(tenantId, shiftId) | Shift contains 3 COMPLETED transactions with totals 100, 200, 300 | Returns `BigDecimal` 600 |
| getSalesTotal_noCompletedTransactions_returnsZero | getSalesTotal(tenantId, shiftId) | Shift exists but has no COMPLETED transactions | Returns `BigDecimal` 0 (or zero-equivalent) |
| getSalesTotal_excludesVoidedTransactions | getSalesTotal(tenantId, shiftId) | Mix of COMPLETED and VOIDED transactions in shift | Returns sum of COMPLETED totals only; VOIDED totals excluded |

---

#### getCashTotal(tenantId, shiftId)

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| getCashTotal_sumOfCashPayments | getCashTotal(tenantId, shiftId) | Shift has COMPLETED transactions with CASH and CARD payments | Returns sum of CASH-type payment amounts only |
| getCashTotal_noCashPayments_returnsZero | getCashTotal(tenantId, shiftId) | All payments in shift are CARD or OTHER type | Returns `BigDecimal` 0 |
| getCashTotal_mixedPaymentsPerTransaction | getCashTotal(tenantId, shiftId) | Single transaction has split payment: partial CASH, partial CARD | Returns only the CASH portion of each transaction's payments |

---

### 1.3 POSTransactionRepository (@DataJpaTest + Testcontainers)

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| findByTerminalId_returnsOnlyThatTerminalsTransactions | findByTerminalId(terminalId) | Two terminals exist with different transactions; query by terminal A | Returns only transactions belonging to terminal A; terminal B transactions absent |
| findByTerminalId_noTransactions_returnsEmpty | findByTerminalId(terminalId) | Terminal exists but has no transactions | Returns empty list or page; no exception |
| findByTerminalId_multipleTransactions_allReturned | findByTerminalId(terminalId) | Terminal has 5 transactions in various statuses | All 5 returned regardless of status |
| findByDateRange_transactionsWithinRange | findByDateRange(tenantId, start, end) | Transactions exist before, within, and after the date range | Returns only transactions where createdAt is between start (inclusive) and end (inclusive) |
| findByDateRange_noneInRange_returnsEmpty | findByDateRange(tenantId, start, end) | All transactions have dates outside the specified range | Returns empty result set |
| findByDateRange_tenantIsolation | findByDateRange(tenantId, start, end) | Two tenants have transactions in the same date range | Returns only the querying tenant's transactions |
| sumCompletedSalesByDateRange_correctSum | sumCompletedSalesByDateRange(tenantId, start, end) | Three COMPLETED transactions within range with totals 50, 75, 100 | Returns `BigDecimal` 225 |
| sumCompletedSalesByDateRange_noCompletedSales_returnsZero | sumCompletedSalesByDateRange(tenantId, start, end) | Only DRAFT or VOIDED transactions in range | Returns 0 or null mapped to 0 |
| countCompletedSalesByDateRange_correctCount | countCompletedSalesByDateRange(tenantId, start, end) | Five COMPLETED and two VOIDED transactions in range | Returns count 5 |
| findByStatus_completedOnly | findByStatus(tenantId, COMPLETED) | Mix of DRAFT, COMPLETED, VOIDED transactions for tenant | Returns only transactions with status `COMPLETED` |
| findByStatus_noMatchingStatus_returnsEmpty | findByStatus(tenantId, COMPLETED) | No COMPLETED transactions exist for tenant | Returns empty list |
| findByStatus_tenantIsolation | findByStatus(tenantId, COMPLETED) | Both tenants have COMPLETED transactions | Returns COMPLETED transactions for the specified tenant only |

---

### 1.4 ShiftRepository (@DataJpaTest)

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| findOpenShiftsByTerminal_returnsOnlyOpenShifts | findOpenShiftsByTerminal(terminalId) | Terminal has one OPEN and one CLOSED shift | Returns list containing only the OPEN shift |
| findOpenShiftsByTerminal_noOpenShift_returnsEmpty | findOpenShiftsByTerminal(terminalId) | Terminal has only CLOSED shifts | Returns empty list |
| findOpenShiftsByTerminal_noShiftsAtAll_returnsEmpty | findOpenShiftsByTerminal(terminalId) | Terminal has never had a shift | Returns empty list |
| findByTerminalAndDateRange_dateFiltered | findByTerminalAndDateRange(terminalId, start, end) | Terminal has shifts in and out of date range | Returns only shifts whose openedAt falls within the range |
| findByTerminalAndDateRange_noMatchingDates_returnsEmpty | findByTerminalAndDateRange(terminalId, start, end) | All terminal shifts are outside date range | Returns empty list |
| sumSalesByShift_correctSum | sumSalesByShift(shiftId) | Shift has COMPLETED transactions totalling 400 | Returns `BigDecimal` 400 |
| sumSalesByShift_noCompletedTransactions_returnsZero | sumSalesByShift(shiftId) | Shift has no COMPLETED transactions | Returns 0 |
| sumCashByShift_correctSum | sumCashByShift(shiftId) | Shift transactions include CASH payments totalling 150 | Returns `BigDecimal` 150 |
| sumCashByShift_noCashPayments_returnsZero | sumCashByShift(shiftId) | Shift has only CARD payments | Returns 0 |

---

### 1.5 Mapper Tests

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| posTransactionMapper_toDto_allFieldsMapped | POSTransactionMapper.toDto(entity) | Entity has all fields populated including a non-empty lines list | Returned `POSTransactionDto` has matching id, tenantId, terminalId, shiftId, status, total, createdAt, and lines list with all line DTOs |
| posTransactionMapper_toDto_emptyLines | POSTransactionMapper.toDto(entity) | Entity has an empty lines collection | Returned DTO has empty lines list; no NullPointerException |
| posTransactionMapper_fromCreateRequest_fieldsSet | POSTransactionMapper.fromCreateRequest(request) | Valid `CreateTransactionRequest` provided | Returned entity has terminalId and any other request fields set; status is not set by mapper (set by service) |
| posTransactionLineMapper_toDto_allFieldsMapped | POSTransactionLineMapper.toDto(entity) | Line entity has all fields: id, productId, productName, qty, unitPrice, lineTotal, discount | Returned `POSTransactionLineDto` matches every field exactly |
| posTransactionLineMapper_fromAddLineRequest_fieldsSet | POSTransactionLineMapper.fromAddLineRequest(request) | Valid `AddLineRequest` with productId and qty | Returned entity has productId and qty set; lineTotal not yet calculated (service responsibility) |
| shiftMapper_toDto_allFieldsIncludingTotals | ShiftMapper.toDto(entity) | Shift entity has all fields: id, terminalId, status, openingCash, closingCash, totalSales, totalCash, openedAt, closedAt | Returned `ShiftDto` matches all fields; totals correctly mapped |
| shiftMapper_fromOpenRequest_openingCashAndTerminalSet | ShiftMapper.fromOpenRequest(request, terminalId) | `OpenShiftRequest` with openingCash; terminalId passed separately | Returned entity has terminalId and openingCash set; status not set by mapper |

---

## 2. Integration Tests

### 2.1 POSTransactionController — `/api/v1/pos/transactions`

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|-----------|-------------------|-----------------|----------------------------|
| getTransactions_paginated_200 | GET /api/v1/pos/transactions | Authenticated user with `POS_READ` permission | 200 OK; body is paginated JSON with `content`, `totalElements`, `totalPages`, `page` fields |
| getTransactions_noPermission_403 | GET /api/v1/pos/transactions | Authenticated user without `POS_READ` permission | 403 Forbidden; body contains error message |
| getTransactions_filterByStatus_200 | GET /api/v1/pos/transactions?status=COMPLETED | Authenticated user with `POS_READ` | 200 OK; all items in `content` have `status` = `COMPLETED` |
| getTransactions_filterByTerminalId_200 | GET /api/v1/pos/transactions?terminalId={id} | Authenticated user with `POS_READ` | 200 OK; all returned transactions have `terminalId` matching the filter |
| getTransactions_filterByDateRange_200 | GET /api/v1/pos/transactions?startDate=&endDate= | Authenticated user with `POS_READ` | 200 OK; all returned transactions have `createdAt` within the specified range |
| getTransaction_found_200 | GET /api/v1/pos/transactions/{id} | Authenticated user with `POS_READ` | 200 OK; body contains single `POSTransactionDto` matching the id |
| getTransaction_notFound_404 | GET /api/v1/pos/transactions/{id} | Authenticated user with `POS_READ` | 404 Not Found; body contains error detail |
| createTransaction_valid_201 | POST /api/v1/pos/transactions | Authenticated user with `POS_WRITE`; valid request body | 201 Created; body contains `POSTransactionDto` with `status` = `DRAFT`; `Location` header set |
| createTransaction_noOpenShift_422 | POST /api/v1/pos/transactions | Authenticated user with `POS_WRITE`; terminal has no open shift | 422 Unprocessable Entity; error body describes missing open shift |
| createTransaction_terminalNotFound_404 | POST /api/v1/pos/transactions | Authenticated user with `POS_WRITE`; unknown terminalId in body | 404 Not Found; error body references terminalId |
| addLine_valid_200 | PUT /api/v1/pos/transactions/{id}/lines | Authenticated user with `POS_WRITE`; valid product and DRAFT transaction | 200 OK; response body contains updated transaction with the new line |
| addLine_productNotFound_404 | PUT /api/v1/pos/transactions/{id}/lines | Authenticated user with `POS_WRITE`; unknown productId | 404 Not Found; error body references productId |
| addLine_completedTransaction_422 | PUT /api/v1/pos/transactions/{id}/lines | Authenticated user with `POS_WRITE`; transaction is COMPLETED | 422 Unprocessable Entity; error body mentions "Transaction locked" |
| updateLine_valid_200 | PUT /api/v1/pos/transactions/{id}/lines/{lineId} | Authenticated user with `POS_WRITE`; valid lineId and DRAFT transaction | 200 OK; response body contains line with updated quantity and recalculated total |
| updateLine_notFound_404 | PUT /api/v1/pos/transactions/{id}/lines/{lineId} | Authenticated user with `POS_WRITE`; unknown lineId | 404 Not Found; error body references lineId |
| removeLine_valid_204 | DELETE /api/v1/pos/transactions/{id}/lines/{lineId} | Authenticated user with `POS_WRITE`; multiple lines exist | 204 No Content; line no longer present on subsequent GET |
| removeLine_lastLine_422 | DELETE /api/v1/pos/transactions/{id}/lines/{lineId} | Authenticated user with `POS_WRITE`; lineId is the only line | 422 Unprocessable Entity; error body mentions "Cannot remove all lines" |
| addPayment_valid_200 | POST /api/v1/pos/transactions/{id}/payments | Authenticated user with `POS_WRITE`; valid payment on DRAFT transaction | 200 OK; response body contains updated transaction with payment recorded |
| addPayment_completedTransaction_422 | POST /api/v1/pos/transactions/{id}/payments | Authenticated user with `POS_WRITE`; transaction is COMPLETED | 422 Unprocessable Entity; error body explains no payment can be added |
| voidTransaction_valid_200 | PUT /api/v1/pos/transactions/{id}/void | Authenticated user with `POS_WRITE`; transaction is DRAFT | 200 OK; response body has `status` = `VOIDED` |
| voidTransaction_alreadyVoided_422 | PUT /api/v1/pos/transactions/{id}/void | Authenticated user with `POS_WRITE`; transaction already VOIDED | 422 Unprocessable Entity; error body explains transaction is already voided |
| completeTransaction_valid_200 | POST /api/v1/pos/transactions/{id}/complete | Authenticated user with `POS_WRITE`; full payment present with at least one line | 200 OK; response body has `status` = `COMPLETED` |
| completeTransaction_underpayment_422 | POST /api/v1/pos/transactions/{id}/complete | Authenticated user with `POS_WRITE`; total payments less than transaction total | 422 Unprocessable Entity; error body mentions "Insufficient payment" |
| completeTransaction_noLines_422 | POST /api/v1/pos/transactions/{id}/complete | Authenticated user with `POS_WRITE`; transaction has no lines | 422 Unprocessable Entity; error body mentions "No items" |

---

### 2.2 ShiftController — `/api/v1/pos/shifts`

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|-----------|-------------------|-----------------|----------------------------|
| getShifts_200 | GET /api/v1/pos/shifts | Authenticated user with `POS_READ` | 200 OK; body is a list (or paginated result) of `ShiftDto` objects |
| getShifts_noPermission_403 | GET /api/v1/pos/shifts | Authenticated user without `POS_READ` | 403 Forbidden; error body present |
| getCurrentShift_found_200 | GET /api/v1/pos/shifts/current?terminalId={id} | Authenticated user with `POS_READ`; terminal has an open shift | 200 OK; body contains `ShiftDto` with `status` = `OPEN` and matching `terminalId` |
| getCurrentShift_noOpenShift_404 | GET /api/v1/pos/shifts/current?terminalId={id} | Authenticated user with `POS_READ`; terminal has no open shift | 404 Not Found; error body explains no open shift found |
| openShift_valid_201 | POST /api/v1/pos/shifts/open | Authenticated user with `POS_WRITE`; terminal exists with no open shift | 201 Created; body contains `ShiftDto` with `status` = `OPEN`; `openingCash` matches request |
| openShift_alreadyOpen_422 | POST /api/v1/pos/shifts/open | Authenticated user with `POS_WRITE`; terminal already has an open shift | 422 Unprocessable Entity; error body mentions "Shift already open" |
| closeShift_valid_200 | POST /api/v1/pos/shifts/{id}/close | Authenticated user with `POS_WRITE`; shift is OPEN; no open transactions | 200 OK; body contains `ShiftDto` with `status` = `CLOSED`; totals calculated |
| closeShift_alreadyClosed_422 | POST /api/v1/pos/shifts/{id}/close | Authenticated user with `POS_WRITE`; shift is already CLOSED | 422 Unprocessable Entity; error body explains shift is already closed |
| closeShift_openTransactions_422 | POST /api/v1/pos/shifts/{id}/close | Authenticated user with `POS_WRITE`; shift has DRAFT transactions | 422 Unprocessable Entity; error body mentions "Close open transactions first" |
| getShift_found_200 | GET /api/v1/pos/shifts/{id} | Authenticated user with `POS_READ`; shift exists | 200 OK; body is `ShiftDto` matching the given shiftId |
| getShift_notFound_404 | GET /api/v1/pos/shifts/{id} | Authenticated user with `POS_READ`; unknown shiftId | 404 Not Found; error body references shiftId |
| getShiftSummary_200 | GET /api/v1/pos/shifts/{id}/summary | Authenticated user with `POS_READ`; shift exists with completed transactions | 200 OK; body contains summary object with `transactionCount`, `totalSales`, `totalCash`, `expectedCash` fields populated with correct values |
