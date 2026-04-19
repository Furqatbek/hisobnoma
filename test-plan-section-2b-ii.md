# Section 2b-ii: Finance — Bank, Tax, Currency & Fiscal Period — Test Plan

---

## 1. BankAccountService Unit Tests

Framework: JUnit 5 + Mockito.

### 1.1 CRUD Operations

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getBankAccounts_returnsPaged` | `getBankAccounts(tenantId, pageable)` | Multiple accounts | Returns `Page<BankAccountDto>` |
| `getBankAccount_found_returnsDto` | `getBankAccount(tenantId, id)` | Account exists | Returns `BankAccountDto` |
| `getBankAccount_notFound_throwsNotFoundException` | `getBankAccount(tenantId, id)` | Missing | Throws `NotFoundException` |
| `createBankAccount_success` | `createBankAccount(tenantId, request)` | Valid request | Returns dto with id |
| `createBankAccount_duplicateAccountNumber_throwsDuplicateResourceException` | `createBankAccount(tenantId, request)` | Account number exists | Throws `DuplicateResourceException` |
| `updateBankAccount_success` | `updateBankAccount(tenantId, id, request)` | Valid | Returns updated dto |
| `deactivateBankAccount_success` | `deactivateBankAccount(tenantId, id)` | Active account | Status becomes INACTIVE |
| `deactivateBankAccount_alreadyInactive_idempotent` | `deactivateBankAccount(tenantId, id)` | Already INACTIVE | No error; remains INACTIVE |

### 1.2 `getBankBalance(tenantId, accountId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getBankBalance_returnsNetBalance` | `getBankBalance(tenantId, id)` | Deposits 10000, withdrawals 3000 | Returns 7000 |
| `getBankBalance_returnsOpeningBalance_whenNoTransactions` | `getBankBalance(tenantId, id)` | No transactions | Returns opening balance |

---

## 2. BankTransactionService Unit Tests

### 2.1 `createBankTransaction(tenantId, request)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `createBankTransaction_deposit_increasesBalance` | `createBankTransaction(tenantId, request)` | type=DEPOSIT, amount=500 | Returns dto; balance increases by 500 |
| `createBankTransaction_withdrawal_decreasesBalance` | `createBankTransaction(tenantId, request)` | type=WITHDRAWAL, amount=200 | Returns dto; balance decreases by 200 |
| `createBankTransaction_insufficientFunds_throwsBusinessException` | `createBankTransaction(tenantId, request)` | Withdrawal > balance | Throws `BusinessException` "Insufficient funds" |
| `createBankTransaction_zeroPmt_throwsValidationException` | `createBankTransaction(tenantId, request)` | amount=0 | Throws `ValidationException` |
| `createBankTransaction_accountNotFound_throwsNotFoundException` | `createBankTransaction(tenantId, request)` | Account missing | Throws `NotFoundException` |

### 2.2 `reconcileBankTransaction(tenantId, transactionId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `reconcile_unreconciled_becomesReconciled` | `reconcile(tenantId, id)` | Transaction UNRECONCILED | Status becomes RECONCILED |
| `reconcile_alreadyReconciled_throwsBusinessException` | `reconcile(tenantId, id)` | Already RECONCILED | Throws `BusinessException` |

---

## 3. TaxService Unit Tests

### 3.1 CRUD + Calculation

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getTaxRates_returnsPaged` | `getTaxRates(tenantId, pageable)` | Multiple rates | Returns paged dto |
| `getTaxRate_found_returnsDto` | `getTaxRate(tenantId, id)` | Rate exists | Returns dto |
| `getTaxRate_notFound_throwsNotFoundException` | `getTaxRate(tenantId, id)` | Missing | Throws `NotFoundException` |
| `createTaxRate_success` | `createTaxRate(tenantId, request)` | Valid rate 15% | Returns dto with rate=15 |
| `createTaxRate_duplicateName_throwsDuplicateResourceException` | `createTaxRate(tenantId, request)` | Name exists | Throws `DuplicateResourceException` |
| `updateTaxRate_success` | `updateTaxRate(tenantId, id, request)` | Valid update | Returns updated dto |
| `calculateTax_returnsCorrectAmount` | `calculateTax(tenantId, amount, taxRateId)` | amount=1000, rate=15% | Returns 150 |
| `calculateTax_zeroRate_returnsZero` | `calculateTax(tenantId, amount, taxRateId)` | rate=0% | Returns 0 |
| `calculateTax_exemptProduct_returnsZero` | `calculateTax(tenantId, amount, taxRateId)` | Product is tax-exempt | Returns 0 |
| `getActiveTaxRates_returnsOnlyActive` | `getActiveTaxRates(tenantId)` | Mix of active/inactive | Returns only active rates |

---

## 4. CurrencyService Unit Tests

### 4.1 CRUD + Conversion

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getCurrencies_returnsList` | `getCurrencies(tenantId)` | 3 currencies configured | Returns list of 3 |
| `getBaseCurrency_returnsDefault` | `getBaseCurrency(tenantId)` | Base currency configured | Returns base currency dto |
| `createCurrency_success` | `createCurrency(tenantId, request)` | Valid "USD" | Returns dto |
| `createCurrency_duplicateCode_throwsDuplicateResourceException` | `createCurrency(tenantId, request)` | "USD" exists | Throws `DuplicateResourceException` |
| `updateExchangeRate_success` | `updateExchangeRate(tenantId, currencyId, rate)` | Rate=12700 | Exchange rate updated |
| `convertAmount_correctResult` | `convertAmount(tenantId, 100, "USD", "UZS")` | Rate 12700 | Returns 1270000 |
| `convertAmount_sameTargetCurrency_returnsOriginal` | `convertAmount(tenantId, 100, "UZS", "UZS")` | Same source/target | Returns 100 |
| `convertAmount_currencyNotFound_throwsNotFoundException` | `convertAmount(tenantId, 100, "XYZ", "UZS")` | "XYZ" not configured | Throws `NotFoundException` |

---

## 5. FiscalPeriodService Unit Tests

### 5.1 CRUD + Period Management

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getFiscalPeriods_returnsList` | `getFiscalPeriods(tenantId)` | Multiple periods | Returns list |
| `getFiscalPeriod_found_returnsDto` | `getFiscalPeriod(tenantId, id)` | Period exists | Returns dto |
| `getCurrentFiscalPeriod_returnsOpenPeriod` | `getCurrentFiscalPeriod(tenantId)` | One OPEN period matching today | Returns that period |
| `getCurrentFiscalPeriod_noOpenPeriod_throwsBusinessException` | `getCurrentFiscalPeriod(tenantId)` | No OPEN periods | Throws `BusinessException` |
| `createFiscalPeriod_success` | `createFiscalPeriod(tenantId, request)` | Valid dates, no overlap | Returns dto with status=OPEN |
| `createFiscalPeriod_overlappingDates_throwsBusinessException` | `createFiscalPeriod(tenantId, request)` | Dates overlap existing | Throws `BusinessException` "Fiscal periods cannot overlap" |
| `closeFiscalPeriod_open_becomesClosed` | `closeFiscalPeriod(tenantId, id)` | Period OPEN | Status becomes CLOSED |
| `closeFiscalPeriod_alreadyClosed_throwsBusinessException` | `closeFiscalPeriod(tenantId, id)` | Already CLOSED | Throws `BusinessException` |
| `reopenFiscalPeriod_closed_becomesOpen` | `reopenFiscalPeriod(tenantId, id)` | Period CLOSED | Status becomes OPEN |

---

## 6. Repository Tests (`@DataJpaTest` + Testcontainers)

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `BankAccountRepository_findByAccountNumber` | `findByAccountNumber(tenantId, acct)` | Account exists | Non-empty Optional |
| `BankAccountRepository_findActiveAccounts` | `findActiveAccounts(tenantId)` | Mix active/inactive | Returns only active |
| `BankTransactionRepository_findByBankAccountId` | `findByBankAccountId(id)` | Multiple transactions | Returns all for account |
| `BankTransactionRepository_sumByTypeAndDateRange` | `sumByTypeAndDateRange(id, DEPOSIT, from, to)` | 3 deposits in range | Returns sum |
| `TaxRateRepository_findByCode` | `findByCode(tenantId, code)` | Code "VAT20" | Returns Optional |
| `TaxRateRepository_findActiveRates` | `findActiveRates(tenantId)` | Mix active/inactive | Returns only active |
| `CurrencyRepository_findByCode` | `findByCode(tenantId, "USD")` | USD exists | Returns Optional |
| `CurrencyRepository_findBaseCurrency` | `findBaseCurrency(tenantId)` | One base currency | Returns it |
| `FiscalPeriodRepository_findOpenPeriod` | `findOpenPeriodContainingDate(tenantId, date)` | Period covers date | Returns matching period |
| `FiscalPeriodRepository_findOverlapping` | `findOverlapping(tenantId, from, to)` | Overlap exists | Returns non-empty list |

---

## 7. Integration Tests

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|---|---|---|---|
| `getBankAccounts_returns200` | `GET /api/v1/finance/bank/accounts` | Bearer `FINANCE_READ` | `200 OK`; paged |
| `createBankAccount_returns201` | `POST /api/v1/finance/bank/accounts` | Bearer `FINANCE_WRITE`; valid | `201 Created` |
| `createBankAccount_returns409_duplicateNumber` | `POST /api/v1/finance/bank/accounts` | Bearer `FINANCE_WRITE`; dup | `409 Conflict` |
| `createBankTransaction_returns201_deposit` | `POST /api/v1/finance/bank/transactions` | Bearer `FINANCE_WRITE`; DEPOSIT | `201 Created`; balance increased |
| `createBankTransaction_returns422_insufficientFunds` | `POST /api/v1/finance/bank/transactions` | Bearer `FINANCE_WRITE`; WITHDRAWAL > balance | `422 Unprocessable Entity` |
| `reconcileTransaction_returns200` | `PUT /api/v1/finance/bank/transactions/{id}/reconcile` | Bearer `FINANCE_WRITE` | `200 OK`; status=RECONCILED |
| `getTaxRates_returns200` | `GET /api/v1/finance/tax/rates` | Bearer `FINANCE_READ` | `200 OK` |
| `createTaxRate_returns201` | `POST /api/v1/finance/tax/rates` | Bearer `FINANCE_WRITE`; valid | `201 Created` |
| `calculateTax_returns200` | `POST /api/v1/finance/tax/calculate` | Bearer `FINANCE_READ` | `200 OK`; tax amount field |
| `getCurrencies_returns200` | `GET /api/v1/finance/currencies` | Bearer `FINANCE_READ` | `200 OK`; list |
| `createCurrency_returns201` | `POST /api/v1/finance/currencies` | Bearer `FINANCE_WRITE` | `201 Created` |
| `convertAmount_returns200` | `POST /api/v1/finance/currencies/convert` | Bearer `FINANCE_READ` | `200 OK`; converted amount |
| `getFiscalPeriods_returns200` | `GET /api/v1/finance/fiscal-periods` | Bearer `FINANCE_READ` | `200 OK`; list |
| `createFiscalPeriod_returns201` | `POST /api/v1/finance/fiscal-periods` | Bearer `FINANCE_WRITE` | `201 Created`; status=OPEN |
| `createFiscalPeriod_returns422_overlap` | `POST /api/v1/finance/fiscal-periods` | Bearer `FINANCE_WRITE`; overlapping | `422 Unprocessable Entity` |
| `closeFiscalPeriod_returns200` | `PUT /api/v1/finance/fiscal-periods/{id}/close` | Bearer `FINANCE_WRITE` | `200 OK`; status=CLOSED |
| `closeFiscalPeriod_returns422_alreadyClosed` | `PUT /api/v1/finance/fiscal-periods/{id}/close` | Bearer `FINANCE_WRITE` | `422 Unprocessable Entity` |
