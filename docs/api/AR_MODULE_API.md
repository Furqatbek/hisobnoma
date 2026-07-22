# Accounts Receivable (AR) Module API Documentation

## Overview

The AR Module provides comprehensive accounts receivable management including customer management, invoicing, credit notes, payment collection, and reporting.

## Authentication

All endpoints require JWT Bearer token authentication and appropriate RBAC permissions:
- `FINANCE_AR_READ` - View customers, invoices, payments
- `FINANCE_AR_WRITE` - Create and edit AR documents
- `FINANCE_AR_APPROVE` - Post invoices, complete payments

## Base URL

```
/api/v1/finance
```

---

## Customer Management

### List Customers

```http
GET /customers
```

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| page | int | Page number (0-based) |
| size | int | Page size (default: 20) |
| sort | string | Sort field and direction (e.g., `name,asc`) |

**Response:** `200 OK`
```json
{
  "content": [
    {
      "id": 1,
      "code": "CUST-001",
      "name": "Acme Corporation",
      "email": "contact@acme.com",
      "phone": "+998901234567",
      "creditLimit": 10000000.00,
      "currentBalance": 5000000.00,
      "availableCredit": 5000000.00,
      "overCreditLimit": false,
      "creditHold": false,
      "active": true
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

### Get Customer

```http
GET /customers/{id}
```

### Get Customer by Code

```http
GET /customers/code/{code}
```

### Search Customers

```http
GET /customers/search?query={searchTerm}
```

### Create Customer

```http
POST /customers
```

**Request Body:**
```json
{
  "code": "CUST-002",
  "name": "New Customer Inc",
  "email": "info@newcustomer.com",
  "phone": "+998901234567",
  "address": "123 Business Street",
  "city": "Tashkent",
  "country": "Uzbekistan",
  "paymentTerms": "Net 30",
  "paymentTermsDays": 30,
  "creditLimit": 5000000.00,
  "taxId": "123456789"
}
```

### Update Customer

```http
PUT /customers/{id}
```

### Delete Customer

```http
DELETE /customers/{id}
```

### Manage Credit

```http
PATCH /customers/{id}/credit-hold?hold={true|false}
PATCH /customers/{id}/credit-limit?creditLimit={amount}
```

### Credit Check

```http
GET /customers/{id}/can-invoice?amount={amount}
```

**Response:**
```json
true
```

---

## AR Invoices

### List Invoices

```http
GET /ar-invoices
```

### Get Invoices by Customer

```http
GET /ar-invoices/customer/{customerId}
```

### Get Invoices by Status

```http
GET /ar-invoices/status/{status}
```

**Status Values:** `DRAFT`, `PENDING`, `SENT`, `PARTIAL`, `PAID`, `OVERDUE`, `CANCELLED`, `WRITTEN_OFF`

### Get Invoice

```http
GET /ar-invoices/{id}
GET /ar-invoices/number/{invoiceNumber}
```

### Get Unpaid Invoices

```http
GET /ar-invoices/customer/{customerId}/unpaid
```

### Get Overdue Invoices

```http
GET /ar-invoices/overdue
```

### Create Invoice

```http
POST /ar-invoices
```

**Request Body:**
```json
{
  "customerId": 1,
  "invoiceDate": "2026-01-03",
  "dueDate": "2026-02-02",
  "discountPercent": 5.00,
  "shippingAmount": 50000.00,
  "currency": "UZS",
  "paymentTerms": 30,
  "billingAddress": "123 Customer St",
  "description": "Monthly service invoice",
  "lines": [
    {
      "itemId": 100,
      "description": "Product A",
      "quantity": 10,
      "unitPrice": 100000.00,
      "unitCost": 75000.00,
      "discountPercent": 0,
      "taxAmount": 120000.00
    }
  ]
}
```

**Response:** `201 Created`
```json
{
  "id": 1,
  "invoiceNumber": "INV-000001",
  "customerName": "Acme Corporation",
  "invoiceDate": "2026-01-03",
  "dueDate": "2026-02-02",
  "status": "DRAFT",
  "subtotal": 1000000.00,
  "discountAmount": 50000.00,
  "taxAmount": 120000.00,
  "shippingAmount": 50000.00,
  "totalAmount": 1120000.00,
  "paidAmount": 0.00,
  "balanceDue": 1120000.00,
  "overdue": false,
  "daysOverdue": 0
}
```

### Update Invoice

```http
PUT /ar-invoices/{id}
```

### Post Invoice (Finalize)

```http
POST /ar-invoices/{id}/post
```

Posts the invoice to GL and makes it available for payment.

### Send Invoice

```http
POST /ar-invoices/{id}/send
```

Marks invoice as sent to customer.

### Cancel Invoice

```http
POST /ar-invoices/{id}/cancel?reason={reason}
```

---

## AR Payments

### List Payments

```http
GET /ar-payments
```

### Get Payments by Customer

```http
GET /ar-payments/customer/{customerId}
```

### Get Payments by Status

```http
GET /ar-payments/status/{status}
```

**Status Values:** `PENDING`, `COMPLETED`, `DEPOSITED`, `REFUNDED`, `CANCELLED`

### Get Payment

```http
GET /ar-payments/{id}
GET /ar-payments/number/{paymentNumber}
```

### Create Payment

```http
POST /ar-payments
```

**Request Body:**
```json
{
  "customerId": 1,
  "paymentDate": "2026-01-03",
  "paymentAmount": 500000.00,
  "paymentMethod": "CASH",
  "currency": "UZS",
  "referenceNumber": "REF-12345",
  "notes": "Partial payment"
}
```

**Payment Methods:** `CASH`, `BANK_TRANSFER`, `CHECK`, `CREDIT_CARD`, `DEBIT_CARD`, `MOBILE_PAYMENT`

### Allocate Payment to Invoice

```http
POST /ar-payments/{id}/allocations
```

**Request Body:**
```json
{
  "invoiceId": 1,
  "allocatedAmount": 500000.00,
  "discountTaken": 0.00,
  "writeOffAmount": 0.00
}
```

### Complete Payment

```http
POST /ar-payments/{id}/complete
```

Posts payment to GL.

### Deposit Payment

```http
POST /ar-payments/{id}/deposit?bankReference={reference}
```

### Cancel Payment

```http
POST /ar-payments/{id}/cancel?reason={reason}
```

---

## Credit Notes

### List Credit Notes

```http
GET /credit-notes
```

### Get Credit Notes by Customer

```http
GET /credit-notes/customer/{customerId}
```

### Get Available Credit Notes

```http
GET /credit-notes/customer/{customerId}/available
```

### Get Credit Note

```http
GET /credit-notes/{id}
GET /credit-notes/number/{creditNoteNumber}
```

### Create Credit Note

```http
POST /credit-notes
```

**Request Body:**
```json
{
  "customerId": 1,
  "originalInvoiceId": 5,
  "creditNoteDate": "2026-01-03",
  "creditAmount": 100000.00,
  "reasonCode": "RETURN",
  "reason": "Product return - defective item",
  "description": "Credit for returned merchandise"
}
```

### Approve Credit Note

```http
POST /credit-notes/{id}/approve
```

Posts credit note to GL.

### Apply Credit to Invoice

```http
POST /credit-notes/{id}/apply?invoiceId={invoiceId}&amount={amount}
```

### Cancel Credit Note

```http
POST /credit-notes/{id}/cancel?reason={reason}
```

---

## AR Reports

### AR Aging Report

```http
GET /ar-reports/aging
```

**Response:**
```json
{
  "reportDate": "2026-01-03",
  "tenantId": 1,
  "customerAgingList": [
    {
      "customerId": 1,
      "customerCode": "CUST-001",
      "customerName": "Acme Corporation",
      "currentAmount": 1000000.00,
      "days1To30": 500000.00,
      "days31To60": 250000.00,
      "days61To90": 0.00,
      "over90Days": 0.00,
      "totalBalance": 1750000.00,
      "creditLimit": 10000000.00,
      "availableCredit": 8250000.00
    }
  ],
  "totalCurrent": 1000000.00,
  "total1To30Days": 500000.00,
  "total31To60Days": 250000.00,
  "total61To90Days": 0.00,
  "totalOver90Days": 0.00,
  "grandTotal": 1750000.00,
  "customerCount": 1
}
```

### Customer Balance Report

```http
GET /ar-reports/customer-balance
```

### Single Customer Balance

```http
GET /ar-reports/customer-balance/{customerId}
```

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
      "field": "customerId",
      "message": "Customer ID is required"
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
  "message": "Customer not found with id: 999"
}
```

### 409 Business Error
```json
{
  "timestamp": "2026-01-03T10:00:00Z",
  "status": 409,
  "error": "Business Error",
  "message": "Customer is on credit hold and cannot be invoiced"
}
```

---

## GL Integration

The AR module automatically creates GL journal entries for:

1. **Invoice Posting** (FINANCE_AR_APPROVE required)
   - Debit: Accounts Receivable (invoice total)
   - Credit: Sales Revenue — net of tax; the total already reflects any header discount, so the
     entry always balances
   - Credit: VAT Payable, account 2130 (the Σ of line tax amounts, when > 0)
   - Debit: Cost of Goods Sold (if cost tracked)
   - Credit: Inventory (if cost tracked)

   Posting records the journal-entry id on the invoice (`glJournalEntryId`/`glPosted`), so a later
   cancel/void reverses the GL entry.

2. **Payment Completion**
   - Debit: Cash/Bank
   - Credit: Accounts Receivable
   - Debit: Sales Discounts (if discount taken)
   - Debit: Bad Debt Expense (if write-off)

3. **Credit Note Approval**
   - Debit: Sales Returns & Allowances
   - Credit: Accounts Receivable
