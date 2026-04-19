# Section 2b-i: Finance — General Ledger & Journal Entries — Test Plan

---

## 1. ChartOfAccountsService Unit Tests

Framework: JUnit 5 + Mockito.

### 1.1 CRUD Operations

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getAccounts_returnsPaged` | `getAccounts(tenantId, pageable)` | Repository returns accounts | Returns `Page<AccountDto>` |
| `getAccount_found_returnsDto` | `getAccount(tenantId, id)` | Account exists | Returns `AccountDto` with all fields |
| `getAccount_notFound_throwsNotFoundException` | `getAccount(tenantId, id)` | Account missing | Throws `NotFoundException` |
| `createAccount_success_returnsDto` | `createAccount(tenantId, request)` | Valid request | Returns `AccountDto` with generated id |
| `createAccount_duplicateCode_throwsDuplicateResourceException` | `createAccount(tenantId, request)` | Account code exists | Throws `DuplicateResourceException` |
| `updateAccount_success_updatesFields` | `updateAccount(tenantId, id, request)` | Valid update | Returns updated dto |
| `updateAccount_notFound_throwsNotFoundException` | `updateAccount(tenantId, id, request)` | Account missing | Throws `NotFoundException` |
| `deleteAccount_success` | `deleteAccount(tenantId, id)` | Account with no journal lines | Account deleted |
| `deleteAccount_hasJournalLines_throwsBusinessException` | `deleteAccount(tenantId, id)` | Account used in GL | Throws `BusinessException` |
| `getAccountsByType_returnsMatchingAccounts` | `getAccountsByType(tenantId, ASSET)` | Mix of account types | Returns only ASSET accounts |

---

## 2. JournalEntryService Unit Tests

### 2.1 `getJournalEntries(tenantId, pageable)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getJournalEntries_returnsPaged` | `getJournalEntries(tenantId, pageable)` | Entries exist | Returns paged `JournalEntryDto` |
| `getJournalEntries_returnsEmpty` | `getJournalEntries(tenantId, pageable)` | None | Returns empty page |

### 2.2 `createJournalEntry(tenantId, request)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `createJournalEntry_balanced_returnsDraftDto` | `createJournalEntry(tenantId, request)` | Debits = Credits | Returns dto with `status=DRAFT` |
| `createJournalEntry_unbalanced_throwsValidationException` | `createJournalEntry(tenantId, request)` | Debits ≠ Credits | Throws `ValidationException` "Debits must equal credits" |
| `createJournalEntry_emptyLines_throwsValidationException` | `createJournalEntry(tenantId, request)` | No lines | Throws `ValidationException` |
| `createJournalEntry_accountNotFound_throwsNotFoundException` | `createJournalEntry(tenantId, request)` | Line references missing account | Throws `NotFoundException` referencing accountId |
| `createJournalEntry_closedPeriod_throwsBusinessException` | `createJournalEntry(tenantId, request)` | Entry date in closed fiscal period | Throws `BusinessException` "Fiscal period is closed" |

### 2.3 `postJournalEntry(tenantId, entryId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `postJournalEntry_draft_updatesLedgerBalances` | `postJournalEntry(tenantId, id)` | Entry `DRAFT` | Status becomes `POSTED`; account balances updated |
| `postJournalEntry_alreadyPosted_throwsBusinessException` | `postJournalEntry(tenantId, id)` | Entry already `POSTED` | Throws `BusinessException` |
| `postJournalEntry_reversed_throwsBusinessException` | `postJournalEntry(tenantId, id)` | Entry `REVERSED` | Throws `BusinessException` |

### 2.4 `reverseJournalEntry(tenantId, entryId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `reverseJournalEntry_posted_createsReversalEntry` | `reverseJournalEntry(tenantId, id)` | Entry `POSTED` | Original entry becomes `REVERSED`; new reversal entry created with swapped DR/CR |
| `reverseJournalEntry_draft_throwsBusinessException` | `reverseJournalEntry(tenantId, id)` | Entry not posted | Throws `BusinessException` |
| `reverseJournalEntry_alreadyReversed_throwsBusinessException` | `reverseJournalEntry(tenantId, id)` | Already reversed | Throws `BusinessException` |

---

## 3. LedgerService Unit Tests

### 3.1 `getAccountLedger(tenantId, accountId, from, to)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getAccountLedger_returnsEntriesInDateRange` | `getAccountLedger(tenantId, accountId, from, to)` | 5 entries: 3 in range, 2 outside | Returns 3 entries |
| `getAccountLedger_returnsEmpty_whenNoEntries` | `getAccountLedger(tenantId, accountId, from, to)` | No entries in range | Returns empty list |
| `getAccountLedger_accountNotFound_throwsNotFoundException` | `getAccountLedger(tenantId, accountId, from, to)` | Account missing | Throws `NotFoundException` |

### 3.2 `getTrialBalance(tenantId, asOfDate)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getTrialBalance_balancedResult` | `getTrialBalance(tenantId, asOfDate)` | Posted entries; DR = CR | Returns trial balance with matching debit/credit totals |
| `getTrialBalance_returnsEmpty_whenNoPostedEntries` | `getTrialBalance(tenantId, asOfDate)` | No posted entries | Returns empty or zero-balance result |

### 3.3 `getIncomeStatement(tenantId, from, to)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getIncomeStatement_correctNetIncome` | `getIncomeStatement(tenantId, from, to)` | Revenue 10000, expenses 6000 | Net income = 4000 |
| `getIncomeStatement_netLoss_whenExpensesExceedRevenue` | `getIncomeStatement(tenantId, from, to)` | Revenue 1000, expenses 3000 | Net income = -2000 |

### 3.4 `getBalanceSheet(tenantId, asOfDate)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getBalanceSheet_assetsEqualsLiabilitiesPlusEquity` | `getBalanceSheet(tenantId, asOfDate)` | Balanced books | Assets = Liabilities + Equity |

---

## 4. Repository Tests (`@DataJpaTest` + Testcontainers)

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `AccountRepository_findByCode` | `findByCode(tenantId, code)` | Code "1010" exists | Returns non-empty Optional |
| `AccountRepository_findByType` | `findByType(tenantId, ASSET)` | Mix of types | Returns only ASSET |
| `JournalEntryRepository_findByStatus` | `findByStatus(tenantId, POSTED)` | Mix of statuses | Returns only POSTED |
| `JournalEntryRepository_findByDateRange` | `findByDateRange(tenantId, from, to)` | 3 in range, 2 outside | Returns 3 |
| `JournalEntryRepository_findByReference` | `findByReference(tenantId, ref)` | Reference "PO-001" | Returns matching entry |
| `JournalLineRepository_findByAccountId` | `findByAccountId(accountId)` | Lines for account | Returns all lines for account |
| `LedgerBalanceRepository_findByAccountAndPeriod` | `findByAccountAndPeriod(id, period)` | Balance exists | Returns Optional with balance |

---

## 5. Integration Tests — AccountController & JournalEntryController

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|---|---|---|---|
| `getAccounts_returns200` | `GET /api/v1/finance/gl/accounts` | Bearer `FINANCE_READ` | `200 OK`; paged accounts |
| `getAccounts_returns403` | `GET /api/v1/finance/gl/accounts` | No `FINANCE_READ` | `403 Forbidden` |
| `createAccount_returns201` | `POST /api/v1/finance/gl/accounts` | Bearer `FINANCE_WRITE`; valid | `201 Created` |
| `createAccount_returns409_duplicateCode` | `POST /api/v1/finance/gl/accounts` | Bearer `FINANCE_WRITE`; dup code | `409 Conflict` |
| `updateAccount_returns200` | `PUT /api/v1/finance/gl/accounts/{id}` | Bearer `FINANCE_WRITE` | `200 OK` |
| `deleteAccount_returns204` | `DELETE /api/v1/finance/gl/accounts/{id}` | Bearer `FINANCE_WRITE`; no entries | `204 No Content` |
| `deleteAccount_returns422_hasEntries` | `DELETE /api/v1/finance/gl/accounts/{id}` | Bearer `FINANCE_WRITE` | `422 Unprocessable Entity` |
| `getJournalEntries_returns200` | `GET /api/v1/finance/gl/journal-entries` | Bearer `FINANCE_READ` | `200 OK`; paged |
| `createJournalEntry_returns201_balanced` | `POST /api/v1/finance/gl/journal-entries` | Bearer `FINANCE_WRITE`; DR=CR | `201 Created`; status=DRAFT |
| `createJournalEntry_returns400_unbalanced` | `POST /api/v1/finance/gl/journal-entries` | Bearer `FINANCE_WRITE`; DR≠CR | `400 Bad Request` |
| `createJournalEntry_returns422_closedPeriod` | `POST /api/v1/finance/gl/journal-entries` | Bearer `FINANCE_WRITE`; closed period | `422 Unprocessable Entity` |
| `postJournalEntry_returns200` | `PUT /api/v1/finance/gl/journal-entries/{id}/post` | Bearer `FINANCE_WRITE` | `200 OK`; status=POSTED |
| `postJournalEntry_returns422_alreadyPosted` | `PUT /api/v1/finance/gl/journal-entries/{id}/post` | Bearer `FINANCE_WRITE` | `422 Unprocessable Entity` |
| `reverseJournalEntry_returns201_reversalCreated` | `POST /api/v1/finance/gl/journal-entries/{id}/reverse` | Bearer `FINANCE_WRITE` | `201 Created`; reversal entry |
| `getTrialBalance_returns200` | `GET /api/v1/finance/gl/reports/trial-balance` | Bearer `FINANCE_READ` | `200 OK`; balanced totals |
| `getIncomeStatement_returns200` | `GET /api/v1/finance/gl/reports/income-statement` | Bearer `FINANCE_READ` | `200 OK`; netIncome field |
| `getBalanceSheet_returns200` | `GET /api/v1/finance/gl/reports/balance-sheet` | Bearer `FINANCE_READ` | `200 OK`; assets = liabilities + equity |
