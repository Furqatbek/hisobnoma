# Finance Module - Banking & Tax API Documentation

## Overview

The Banking & Tax module provides comprehensive bank account management, transaction recording, bank reconciliation, and tax configuration for the Hisobnoma platform.

## Authentication

All endpoints require JWT Bearer token authentication and appropriate RBAC permissions:
- `FINANCE_BANK_VIEW` - View bank accounts and transactions
- `FINANCE_BANK_MANAGE` - Create, update, delete bank accounts
- `FINANCE_BANK_TRANSACT` - Record bank transactions
- `FINANCE_BANK_RECONCILE` - Perform bank reconciliation
- `FINANCE_BANK_TRANSFER` - Transfer funds between bank accounts
- `FINANCE_TAX_VIEW` - View tax codes and rates
- `FINANCE_TAX_MANAGE` - Create, update, delete tax codes and rates
- `FINANCE_TAX_REPORTS` - View tax reports and summaries
- `FINANCE_TAX_EXPORT` - Export tax reports

## Base URL

```
/api/v1/finance
```

---

## Bank Account Management

### List Bank Accounts

```http
GET /bank-accounts
```

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| page | int | Page number (0-based) |
| size | int | Page size (default: 20) |
| sort | string | Sort field and direction (e.g., `sortOrder,asc`) |

**Response:** `200 OK`
```json
{
  "content": [
    {
      "id": 1,
      "accountCode": "BANK-001",
      "accountName": "Main Operating Account",
      "description": "Primary business checking account",
      "accountType": "CHECKING",
      "bankName": "National Bank of Uzbekistan",
      "bankBranch": "Main Branch",
      "bankCode": "NBU001",
      "accountNumber": "1234567890",
      "iban": "UZ12345678901234567890",
      "swiftCode": "NBUZUZ22",
      "currency": "UZS",
      "currentBalance": 50000000.00,
      "availableBalance": 50000000.00,
      "openingBalance": 10000000.00,
      "openingBalanceDate": "2026-01-01",
      "lastReconciledDate": "2025-12-31",
      "lastReconciledBalance": 48000000.00,
      "glAccountId": 100,
      "glAccountCode": "1000",
      "glAccountName": "Cash and Cash Equivalents",
      "contactName": "Account Manager",
      "contactPhone": "+998901234567",
      "contactEmail": "manager@bank.uz",
      "active": true,
      "defaultAccount": true,
      "allowPayments": true,
      "allowReceipts": true,
      "sortOrder": 1,
      "notes": "Primary account for all transactions"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

### Get All Bank Accounts (No Pagination)

```http
GET /bank-accounts/all
```

### Get Active Bank Accounts

```http
GET /bank-accounts/active
```

### Get Bank Accounts by Type

```http
GET /bank-accounts/by-type/{type}
```

**Type Values:** `CHECKING`, `SAVINGS`, `CASH`, `CREDIT_CARD`, `PETTY_CASH`, `FOREIGN_CURRENCY`

### Get Payment Accounts

```http
GET /bank-accounts/payment-accounts
```

Returns accounts enabled for outgoing payments.

### Get Receipt Accounts

```http
GET /bank-accounts/receipt-accounts
```

Returns accounts enabled for incoming receipts.

### Get Default Bank Account

```http
GET /bank-accounts/default
```

### Get Bank Account

```http
GET /bank-accounts/{id}
GET /bank-accounts/code/{code}
```

### Search Bank Accounts

```http
GET /bank-accounts/search?query={searchTerm}
```

### Create Bank Account

```http
POST /bank-accounts
```

**Request Body:**
```json
{
  "accountCode": "BANK-002",
  "accountName": "Payroll Account",
  "description": "Account for salary payments",
  "accountType": "CHECKING",
  "bankName": "Asaka Bank",
  "bankBranch": "Central Branch",
  "bankCode": "ASAK001",
  "accountNumber": "9876543210",
  "iban": "UZ98765432109876543210",
  "swiftCode": "ASAKUZ22",
  "currency": "UZS",
  "openingBalance": 5000000.00,
  "openingBalanceDate": "2026-01-01",
  "glAccountId": 101,
  "contactName": "HR Finance",
  "contactPhone": "+998901234568",
  "contactEmail": "hr@company.uz",
  "defaultAccount": false,
  "allowPayments": true,
  "allowReceipts": false,
  "sortOrder": 2,
  "notes": "Only for payroll disbursements"
}
```

**Response:** `201 Created`

### Update Bank Account

```http
PUT /bank-accounts/{id}
```

### Activate/Deactivate Bank Account

```http
PUT /bank-accounts/{id}/activate
PUT /bank-accounts/{id}/deactivate
```

### Set Default Bank Account

```http
PUT /bank-accounts/{id}/set-default
```

---

## Bank Transactions

### List Transactions

```http
GET /bank-transactions
```

### Get Transactions by Bank Account

```http
GET /bank-transactions/by-account/{bankAccountId}
```

### Get Transactions by Date Range

```http
GET /bank-transactions/by-account/{bankAccountId}/date-range?startDate={date}&endDate={date}
```

### Get Transaction

```http
GET /bank-transactions/{id}
```

### Create Transaction

```http
POST /bank-transactions
```

**Request Body:**
```json
{
  "bankAccountId": 1,
  "transactionDate": "2026-01-03",
  "valueDate": "2026-01-03",
  "transactionType": "DEPOSIT",
  "description": "Customer payment received",
  "amount": 5000000.00,
  "isDebit": false,
  "currency": "UZS",
  "exchangeRate": 1.0,
  "referenceNumber": "PAY-12345",
  "checkNumber": null,
  "payeePayer": "Acme Corporation",
  "referenceType": "AR_PAYMENT",
  "referenceId": 100,
  "counterAccountId": 200,
  "transferToAccountId": null,
  "notes": "Invoice INV-001 payment"
}
```

**Transaction Types:** `DEPOSIT`, `WITHDRAWAL`, `TRANSFER_IN`, `TRANSFER_OUT`, `CHECK`, `FEE`, `INTEREST`, `ADJUSTMENT`

**Response:** `201 Created`
```json
{
  "id": 1,
  "bankAccountId": 1,
  "bankAccountCode": "BANK-001",
  "bankAccountName": "Main Operating Account",
  "transactionNumber": "BT-000001",
  "transactionDate": "2026-01-03",
  "valueDate": "2026-01-03",
  "transactionType": "DEPOSIT",
  "status": "PENDING",
  "description": "Customer payment received",
  "debitAmount": 0.00,
  "creditAmount": 5000000.00,
  "runningBalance": 55000000.00,
  "currency": "UZS",
  "exchangeRate": 1.0,
  "baseAmount": 5000000.00,
  "referenceNumber": "PAY-12345",
  "payeePayer": "Acme Corporation",
  "glPosted": false
}
```

### Create Bank Transfer

```http
POST /bank-transactions/transfer?fromAccountId={id}&toAccountId={id}&amount={amount}&transactionDate={date}&description={text}
```

Creates paired transactions for inter-account transfers.

**Response:** `201 Created`

### Clear Transaction

```http
PUT /bank-transactions/{id}/clear
```

Changes status from `PENDING` to `CLEARED`.

### Void Transaction

```http
PUT /bank-transactions/{id}/void?reason={reason}
```

Voids a transaction and reverses its balance impact.

### Get Cash Flow Report

```http
GET /bank-transactions/cash-flow/{bankAccountId}?startDate={date}&endDate={date}
```

**Response:**
```json
{
  "periodStart": "2026-01-01",
  "periodEnd": "2026-01-31",
  "openingBalance": 40000000.00,
  "totalInflows": 25000000.00,
  "totalOutflows": 15000000.00,
  "netCashFlow": 10000000.00,
  "closingBalance": 50000000.00,
  "inflowDetails": [
    {
      "category": "DEPOSIT",
      "description": "DEPOSIT",
      "amount": 20000000.00,
      "transactionCount": 15
    },
    {
      "category": "TRANSFER_IN",
      "description": "TRANSFER_IN",
      "amount": 5000000.00,
      "transactionCount": 2
    }
  ],
  "outflowDetails": [
    {
      "category": "WITHDRAWAL",
      "description": "WITHDRAWAL",
      "amount": 12000000.00,
      "transactionCount": 20
    },
    {
      "category": "FEE",
      "description": "FEE",
      "amount": 3000000.00,
      "transactionCount": 5
    }
  ]
}
```

---

## Bank Reconciliation

### List Reconciliations

```http
GET /bank-reconciliations
```

### Get Reconciliations by Bank Account

```http
GET /bank-reconciliations/by-account/{bankAccountId}
```

### Get Reconciliation

```http
GET /bank-reconciliations/{id}
```

### Get Unreconciled Transactions

```http
GET /bank-reconciliations/unreconciled/{bankAccountId}?endDate={date}
```

Returns transactions that are pending or cleared but not yet reconciled.

### Create Reconciliation

```http
POST /bank-reconciliations
```

**Request Body:**
```json
{
  "bankAccountId": 1,
  "statementDate": "2026-01-31",
  "statementStartDate": "2026-01-01",
  "statementEndDate": "2026-01-31",
  "openingBalance": 40000000.00,
  "statementEndingBalance": 50000000.00,
  "notes": "January 2026 reconciliation"
}
```

**Response:** `201 Created`
```json
{
  "id": 1,
  "bankAccountId": 1,
  "bankAccountCode": "BANK-001",
  "bankAccountName": "Main Operating Account",
  "reconciliationNumber": "REC-000001",
  "statementDate": "2026-01-31",
  "statementStartDate": "2026-01-01",
  "statementEndDate": "2026-01-31",
  "status": "DRAFT",
  "openingBalance": 40000000.00,
  "statementEndingBalance": 50000000.00,
  "bookBalance": 50500000.00,
  "clearedDeposits": 0.00,
  "clearedWithdrawals": 0.00,
  "outstandingDeposits": 0.00,
  "outstandingWithdrawals": 0.00,
  "adjustedBankBalance": null,
  "adjustedBookBalance": null,
  "difference": 0.00,
  "transactionsCleared": 0,
  "transactionsOutstanding": 0
}
```

### Start Reconciliation

```http
PUT /bank-reconciliations/{id}/start
```

Changes status from `DRAFT` to `IN_PROGRESS`.

### Reconcile Transactions

```http
POST /bank-reconciliations/reconcile-transactions
```

**Request Body:**
```json
{
  "reconciliationId": 1,
  "transactionIds": [1, 2, 3, 4, 5],
  "clear": true
}
```

Set `clear: false` to unreconcile transactions.

### Complete Reconciliation

```http
PUT /bank-reconciliations/{id}/complete
```

Completes the reconciliation when the difference is zero. Updates the bank account's last reconciled date and balance.

### Cancel Reconciliation

```http
PUT /bank-reconciliations/{id}/cancel?reason={reason}
```

Cancels the reconciliation and unreserves all transactions.

**Reconciliation Statuses:** `DRAFT`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`

---

## Tax Code Management

### List Tax Codes

```http
GET /tax-codes
```

### Get All Tax Codes (No Pagination)

```http
GET /tax-codes/all
```

### Get Active Tax Codes

```http
GET /tax-codes/active
```

### Get Tax Codes by Type

```http
GET /tax-codes/by-type/{type}
```

**Tax Types:** `VAT`, `GST`, `SALES_TAX`, `INCOME_TAX`, `WITHHOLDING`, `EXCISE`, `IMPORT_DUTY`, `OTHER`

### Get Sales Tax Codes

```http
GET /tax-codes/sales
```

Returns tax codes applicable to sales.

### Get Purchase Tax Codes

```http
GET /tax-codes/purchases
```

Returns tax codes applicable to purchases.

### Get Tax Code

```http
GET /tax-codes/{id}
GET /tax-codes/code/{code}
```

### Search Tax Codes

```http
GET /tax-codes/search?query={searchTerm}
```

### Create Tax Code

```http
POST /tax-codes
```

**Request Body:**
```json
{
  "code": "VAT15",
  "name": "VAT 15%",
  "description": "Standard VAT at 15%",
  "taxType": "VAT",
  "inclusive": false,
  "compound": false,
  "appliesToSales": true,
  "appliesToPurchases": true,
  "salesAccountId": 300,
  "purchaseAccountId": 301,
  "taxAuthority": "State Tax Committee",
  "taxRegistrationNumber": "VAT123456789",
  "reportCode": "BOX1",
  "sortOrder": 10,
  "notes": "Standard domestic rate"
}
```

**Response:** `201 Created`

### Update Tax Code

```http
PUT /tax-codes/{id}
```

### Activate/Deactivate Tax Code

```http
PUT /tax-codes/{id}/activate
PUT /tax-codes/{id}/deactivate
```

---

## Tax Rate Management

### Get Tax Rates for Code

```http
GET /tax-codes/{taxCodeId}/rates
```

**Response:**
```json
[
  {
    "id": 1,
    "taxCodeId": 1,
    "taxCodeCode": "VAT12",
    "taxCodeName": "VAT 12%",
    "name": "Standard Rate",
    "rate": 12.0000,
    "effectiveFrom": "2020-01-01",
    "effectiveTo": null,
    "minAmount": null,
    "maxAmount": null,
    "active": true,
    "notes": "Current effective rate"
  }
]
```

### Add Tax Rate

```http
POST /tax-codes/rates
```

**Request Body:**
```json
{
  "taxCodeId": 1,
  "name": "Reduced Rate 2026",
  "rate": 10.0000,
  "effectiveFrom": "2026-07-01",
  "effectiveTo": null,
  "minAmount": null,
  "maxAmount": null,
  "notes": "New reduced rate effective July 2026"
}
```

### Update Tax Rate

```http
PUT /tax-codes/rates/{rateId}
```

### Delete Tax Rate

```http
DELETE /tax-codes/rates/{rateId}
```

---

## Tax Calculations

### Calculate Tax

```http
POST /tax-codes/calculate
```

**Request Body:**
```json
{
  "taxCode": "VAT12",
  "amount": 1000000.00,
  "transactionDate": "2026-01-03",
  "inclusive": false
}
```

**Response:**
```json
{
  "taxCode": "VAT12",
  "taxName": "VAT 12%",
  "rate": 12.0000,
  "baseAmount": 1000000.00,
  "taxAmount": 120000.00,
  "totalAmount": 1120000.00,
  "inclusive": false
}
```

For inclusive tax calculation (tax included in amount):
```json
{
  "taxCode": "VAT12",
  "amount": 1120000.00,
  "transactionDate": "2026-01-03",
  "inclusive": true
}
```

**Response:**
```json
{
  "taxCode": "VAT12",
  "taxName": "VAT 12%",
  "rate": 12.0000,
  "baseAmount": 1000000.00,
  "taxAmount": 120000.00,
  "totalAmount": 1120000.00,
  "inclusive": true
}
```

---

## Tax Reports

### VAT Summary Report

```http
GET /tax-codes/reports/vat-summary?periodStart={date}&periodEnd={date}
```

**Response:**
```json
{
  "periodStart": "2026-01-01",
  "periodEnd": "2026-01-31",
  "taxType": "VAT",
  "totalSales": 100000000.00,
  "totalSalesTax": 12000000.00,
  "totalPurchases": 60000000.00,
  "totalPurchaseTax": 7200000.00,
  "netTaxPayable": 4800000.00,
  "lines": [
    {
      "taxCode": "VAT12",
      "taxName": "VAT 12%",
      "rate": 12.0000,
      "salesAmount": 100000000.00,
      "salesTax": 12000000.00,
      "purchaseAmount": 60000000.00,
      "purchaseTax": 7200000.00,
      "netTax": 4800000.00
    }
  ]
}
```

### Tax Summary by Type

```http
GET /tax-codes/reports/summary-by-type?periodStart={date}&periodEnd={date}
```

Returns tax summaries grouped by tax type.

---

## Error Responses

### 400 Bad Request
```json
{
  "timestamp": "2026-01-03T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "accountCode",
      "message": "Account code is required"
    }
  ]
}
```

### 404 Not Found
```json
{
  "timestamp": "2026-01-03T10:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Bank account not found with id: 999"
}
```

### 409 Business Error
```json
{
  "timestamp": "2026-01-03T10:00:00Z",
  "status": 409,
  "error": "Business Error",
  "message": "Default bank account cannot be deactivated"
}
```

---

## Default Tax Codes

The system includes the following default tax codes for Uzbekistan:

| Code | Name | Rate | Description |
|------|------|------|-------------|
| VAT12 | VAT 12% | 12% | Standard VAT rate for Uzbekistan |
| VAT0 | VAT 0% | 0% | Zero-rated VAT |
| NO_TAX | No Tax | 0% | Tax exempt transactions |

---

## GL Integration

The Banking & Tax module integrates with the General Ledger:

1. **Bank Account GL Linking**
   - Each bank account can be linked to a GL account
   - Transactions automatically post to linked GL accounts when `glPosted` is true

2. **Tax GL Posting**
   - Tax codes can have separate sales and purchase GL accounts
   - Tax amounts are automatically calculated and posted during transaction entry

3. **Reconciliation Updates**
   - Completing a reconciliation updates the bank account's last reconciled date and balance
   - Provides audit trail for financial reporting
