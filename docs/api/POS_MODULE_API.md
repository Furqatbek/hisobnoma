# POS Module - Core Transactions API Documentation

## Overview

The POS (Point of Sale) module provides comprehensive transaction management for retail operations including terminal management, shift management, sales/returns/exchanges, payments, and integration with inventory and accounting systems.

## Authentication

All endpoints require JWT Bearer token authentication and appropriate RBAC permissions:

### Terminal Permissions
- `POS_TERMINAL_CREATE` - Create new POS terminals
- `POS_TERMINAL_READ` - View POS terminals
- `POS_TERMINAL_UPDATE` - Update POS terminals
- `POS_TERMINAL_DELETE` - Delete POS terminals

### Shift Permissions
- `POS_SHIFT_OPEN` - Open a new shift
- `POS_SHIFT_CLOSE` - Close a shift
- `POS_SHIFT_READ` - View shift information
- `POS_SHIFT_CASH_OPERATION` - Perform cash in/out operations

### Sale Permissions
- `POS_SALE_CREATE` - Create new POS sales
- `POS_SALE_READ` - View POS sales
- `POS_SALE_VOID` - Void POS transactions
- `POS_SALE_REFUND` - Process refunds
- `POS_SALE_HOLD` - Hold and recall transactions

### Discount Permissions
- `POS_DISCOUNT_APPLY` - Apply discounts to transactions
- `POS_PRICE_OVERRIDE` - Override item prices
- `POS_DISCOUNT_OVERRIDE` - Override discount limits

### Payment Permissions
- `POS_PAYMENT_PROCESS` - Process payments
- `POS_PAYMENT_VOID` - Void payments

### Other Permissions
- `POS_DRAWER_OPEN` - Open cash drawer
- `POS_REPORTS_VIEW` - View POS reports

## Base URL

```
/api/v1/pos
```

---

## POS Terminal Management

### List Terminals

```http
GET /terminals
```

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| search | string | Search by code or name |
| page | int | Page number (0-based) |
| size | int | Page size (default: 20) |
| sort | string | Sort field and direction (e.g., `name,asc`) |

**Response:** `200 OK`
```json
{
  "content": [
    {
      "id": 1,
      "terminalCode": "POS-001",
      "name": "Main Register",
      "description": "Main floor register",
      "locationId": 1,
      "locationName": "Main Store",
      "active": true,
      "ipAddress": "192.168.1.100",
      "macAddress": "00:1A:2B:3C:4D:5E",
      "deviceType": "DESKTOP",
      "printerName": "EPSON TM-T88V",
      "cashDrawerEnabled": true,
      "allowOffline": false,
      "lastSyncAt": "2026-01-04T10:30:00Z",
      "currentShiftId": 5
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

### Get Terminal

```http
GET /terminals/{id}
GET /terminals/code/{code}
```

### Get Terminals by Location

```http
GET /terminals/location/{locationId}
```

### Get Active Terminals

```http
GET /terminals/active
```

### Create Terminal

```http
POST /terminals
```

**Request Body:**
```json
{
  "terminalCode": "POS-002",
  "name": "Checkout 2",
  "description": "Secondary checkout register",
  "locationId": 1,
  "ipAddress": "192.168.1.101",
  "macAddress": "00:1A:2B:3C:4D:5F",
  "deviceType": "TABLET",
  "printerName": "EPSON TM-m30",
  "cashDrawerEnabled": true,
  "allowOffline": true
}
```

**Response:** `201 Created`

### Update Terminal

```http
PUT /terminals/{id}
```

### Activate/Deactivate Terminal

```http
PUT /terminals/{id}/activate
PUT /terminals/{id}/deactivate
```

### Delete Terminal

```http
DELETE /terminals/{id}
```

---

## Shift Management

### List Shifts

```http
GET /shifts
```

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| page | int | Page number (0-based) |
| size | int | Page size (default: 20) |
| sort | string | Sort field (default: `openedAt,desc`) |

**Response:** `200 OK`
```json
{
  "content": [
    {
      "id": 1,
      "shiftNumber": "SH20260104-001",
      "terminalId": 1,
      "terminalCode": "POS-001",
      "terminalName": "Main Register",
      "cashierId": 10,
      "cashierName": "John Doe",
      "status": "OPEN",
      "openedAt": "2026-01-04T08:00:00Z",
      "closedAt": null,
      "openingCash": 500000.0000,
      "closingCash": null,
      "expectedCash": 2500000.0000,
      "cashDifference": null,
      "totalSales": 2000000.0000,
      "totalReturns": 0.0000,
      "totalDiscounts": 50000.0000,
      "totalTaxes": 240000.0000,
      "cashPayments": 1500000.0000,
      "cardPayments": 500000.0000,
      "otherPayments": 0.0000,
      "transactionCount": 25,
      "voidedCount": 2,
      "returnCount": 0,
      "cashIn": 100000.0000,
      "cashOut": 0.0000,
      "notes": "Regular morning shift"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

### Get Shift

```http
GET /shifts/{id}
```

### Get Shifts by Terminal

```http
GET /shifts/terminal/{terminalId}
```

### Get Shifts by Cashier

```http
GET /shifts/cashier/{cashierId}
```

### Get Current Shift for User

```http
GET /shifts/current
```

### Get Current Shift for Terminal

```http
GET /shifts/current/terminal/{terminalId}
```

### Get Open Shifts

```http
GET /shifts/open
```

### Open Shift

```http
POST /shifts/open
```

**Request Body:**
```json
{
  "terminalId": 1,
  "openingCash": 500000.0000,
  "notes": "Morning shift opening"
}
```

**Response:** `201 Created`
```json
{
  "success": true,
  "message": "Shift opened",
  "data": {
    "id": 1,
    "shiftNumber": "SH20260104-001",
    "terminalId": 1,
    "status": "OPEN",
    "openedAt": "2026-01-04T08:00:00Z",
    "openingCash": 500000.0000
  }
}
```

### Close Shift

```http
POST /shifts/{id}/close
```

**Request Body:**
```json
{
  "closingCash": 2600000.0000,
  "closingNotes": "All transactions reconciled"
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Shift closed",
  "data": {
    "id": 1,
    "status": "CLOSED",
    "closedAt": "2026-01-04T18:00:00Z",
    "closingCash": 2600000.0000,
    "expectedCash": 2500000.0000,
    "cashDifference": 100000.0000
  }
}
```

### Cash Operation (Cash In/Out)

```http
POST /shifts/{id}/cash-operation
```

**Request Body:**
```json
{
  "operationType": "CASH_IN",
  "amount": 100000.0000,
  "reason": "Petty cash replenishment"
}
```

**Operation Types:** `CASH_IN`, `CASH_OUT`

### Reconcile Shift

```http
POST /shifts/{id}/reconcile
```

Marks the shift as reconciled for audit purposes.

**Shift Statuses:** `OPEN`, `CLOSED`, `RECONCILED`

---

## Transaction Management

### List Transactions

```http
GET /transactions
```

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| search | string | Search by transaction number |
| page | int | Page number (0-based) |
| size | int | Page size (default: 20) |
| sort | string | Sort field (default: `createdAt,desc`) |

**Response:** `200 OK`
```json
{
  "content": [
    {
      "id": 1,
      "transactionNumber": "TX20260104-00001",
      "terminalId": 1,
      "terminalCode": "POS-001",
      "shiftId": 1,
      "customerId": null,
      "customerName": "Walk-in Customer",
      "customerPhone": null,
      "transactionType": "SALE",
      "status": "COMPLETED",
      "subtotal": 500000.0000,
      "discountAmount": 25000.0000,
      "discountPercent": 5.00,
      "taxAmount": 60000.0000,
      "totalAmount": 535000.0000,
      "paidAmount": 540000.0000,
      "changeAmount": 5000.0000,
      "currency": "UZS",
      "itemCount": 3,
      "lineCount": 2,
      "completedAt": "2026-01-04T10:15:00Z",
      "completedBy": 10,
      "glPosted": true,
      "stockDeducted": true
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

### Get Transaction

```http
GET /transactions/{id}
GET /transactions/number/{transactionNumber}
```

**Response includes lines and payments:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "transactionNumber": "TX20260104-00001",
    "transactionType": "SALE",
    "status": "COMPLETED",
    "subtotal": 500000.0000,
    "discountAmount": 25000.0000,
    "taxAmount": 60000.0000,
    "totalAmount": 535000.0000,
    "paidAmount": 540000.0000,
    "changeAmount": 5000.0000,
    "lines": [
      {
        "id": 1,
        "lineNumber": 1,
        "productId": 100,
        "productCode": "SKU-001",
        "productName": "Product A",
        "barcode": "1234567890123",
        "quantity": 2.0000,
        "unitPrice": 150000.0000,
        "originalPrice": 150000.0000,
        "costPrice": 100000.0000,
        "discountAmount": 0.0000,
        "taxCode": "VAT12",
        "taxRate": 12.00,
        "taxAmount": 36000.0000,
        "lineTotal": 336000.0000,
        "isReturn": false
      }
    ],
    "payments": [
      {
        "id": 1,
        "paymentNumber": 1,
        "paymentType": "CASH",
        "status": "APPROVED",
        "amount": 540000.0000,
        "tenderedAmount": 600000.0000,
        "changeAmount": 60000.0000,
        "processedAt": "2026-01-04T10:14:55Z"
      }
    ]
  }
}
```

### Get Transactions by Shift

```http
GET /transactions/shift/{shiftId}
```

### Get Held Transactions

```http
GET /transactions/held?shiftId={shiftId}
```

Returns transactions with status `HELD` for the specified shift.

### Create Transaction

```http
POST /transactions
```

**Request Body:**
```json
{
  "terminalId": 1,
  "transactionType": "SALE",
  "customerId": null,
  "customerName": "John Smith",
  "customerPhone": "+998901234567",
  "notes": "Priority customer"
}
```

**Transaction Types:** `SALE`, `RETURN`, `EXCHANGE`

**Response:** `201 Created`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "transactionNumber": "TX20260104-00001",
    "status": "PENDING",
    "transactionType": "SALE"
  }
}
```

### Add Line Item

```http
POST /transactions/{id}/lines
```

**Request Body:**
```json
{
  "productId": 100,
  "variantId": null,
  "quantity": 2.0000,
  "unitPrice": null,
  "discountAmount": null,
  "discountPercent": null,
  "discountReason": null,
  "serialNumber": null,
  "batchNumber": null,
  "locationId": null,
  "notes": null
}
```

If `unitPrice` is null, the product's selling price is used.

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Item added",
  "data": {
    "id": 1,
    "transactionNumber": "TX20260104-00001",
    "subtotal": 300000.0000,
    "totalAmount": 336000.0000,
    "lines": [...]
  }
}
```

### Update Line Item

```http
PUT /transactions/{id}/lines/{lineId}
```

**Request Body:**
```json
{
  "quantity": 3.0000,
  "unitPrice": 145000.0000,
  "discountPercent": 5.00,
  "discountReason": "Customer loyalty",
  "notes": "Updated quantity"
}
```

### Remove Line Item

```http
DELETE /transactions/{id}/lines/{lineId}
```

### Apply Discount

```http
POST /transactions/{id}/discount
```

**Request Body (Percentage):**
```json
{
  "percent": 10.00,
  "reason": "Promotional discount"
}
```

**Request Body (Fixed Amount):**
```json
{
  "amount": 50000.0000,
  "reason": "Manager override"
}
```

### Hold Transaction

```http
POST /transactions/{id}/hold
```

**Request Body:**
```json
{
  "reason": "Customer went to get more items"
}
```

Puts the transaction on hold. Can be recalled later.

### Recall Transaction

```http
POST /transactions/{id}/recall
```

Recalls a held transaction back to pending status.

### Void Transaction

```http
POST /transactions/{id}/void
```

**Request Body:**
```json
{
  "reason": "Customer cancelled order"
}
```

Voids the transaction. For completed transactions, reverses stock and GL entries.

### Complete Transaction

```http
POST /transactions/{id}/complete
```

Completes the transaction:
- Validates full payment received
- Deducts stock from inventory
- Posts to General Ledger
- Updates shift totals

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Transaction completed",
  "data": {
    "id": 1,
    "transactionNumber": "TX20260104-00001",
    "status": "COMPLETED",
    "completedAt": "2026-01-04T10:15:00Z",
    "glPosted": true,
    "stockDeducted": true
  }
}
```

**Transaction Statuses:** `PENDING`, `HELD`, `COMPLETED`, `VOIDED`, `CANCELLED`

---

## Payment Management

### Get Payments for Transaction

```http
GET /transactions/{id}/payments
```

**Response:** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "paymentNumber": 1,
      "paymentType": "CASH",
      "status": "APPROVED",
      "amount": 300000.0000,
      "tenderedAmount": 350000.0000,
      "changeAmount": 50000.0000,
      "currency": "UZS",
      "processedAt": "2026-01-04T10:14:50Z",
      "processedBy": 10
    },
    {
      "id": 2,
      "paymentNumber": 2,
      "paymentType": "CARD",
      "status": "APPROVED",
      "amount": 236000.0000,
      "cardType": "VISA",
      "cardLastFour": "4242",
      "authCode": "123456",
      "gatewayReference": "PAY-12345",
      "processedAt": "2026-01-04T10:14:55Z"
    }
  ]
}
```

### Add Payment

```http
POST /transactions/{id}/payments
```

**Cash Payment:**
```json
{
  "paymentType": "CASH",
  "amount": 300000.0000,
  "tenderedAmount": 350000.0000,
  "notes": "Cash payment"
}
```

**Card Payment:**
```json
{
  "paymentType": "CARD",
  "amount": 236000.0000,
  "cardType": "VISA",
  "cardLastFour": "4242",
  "authCode": "123456",
  "gatewayReference": "PAY-12345",
  "notes": null
}
```

**Mobile Payment:**
```json
{
  "paymentType": "MOBILE_PAYMENT",
  "amount": 100000.0000,
  "mobileReference": "PAYME-789456",
  "notes": "Payme payment"
}
```

**Gift Card Payment:**
```json
{
  "paymentType": "GIFT_CARD",
  "amount": 100000.0000,
  "giftCardNumber": "GC-12345678",
  "notes": null
}
```

**Payment Types:** `CASH`, `CARD`, `CREDIT`, `GIFT_CARD`, `MOBILE_PAYMENT`, `CHECK`, `OTHER`

**Response:** `201 Created`
```json
{
  "success": true,
  "message": "Payment added",
  "data": {
    "id": 1,
    "paymentNumber": 1,
    "paymentType": "CASH",
    "status": "APPROVED",
    "amount": 300000.0000,
    "changeAmount": 50000.0000
  }
}
```

### Void Payment

```http
POST /transactions/{id}/payments/{paymentId}/void?reason={reason}
```

Voids a payment on a pending transaction.

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Payment voided",
  "data": {
    "id": 1,
    "status": "VOIDED",
    "voidedAt": "2026-01-04T10:20:00Z",
    "voidReason": "Customer changed payment method"
  }
}
```

### Refund Payment

```http
POST /transactions/{id}/payments/{paymentId}/refund?amount={amount}&reason={reason}
```

Processes a refund for an approved payment on a completed transaction.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| amount | decimal | Refund amount (must not exceed payment amount) |
| reason | string | Reason for refund |

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Payment refunded",
  "data": {
    "id": 1,
    "status": "REFUNDED",
    "refundAmount": 100000.0000,
    "refundedAt": "2026-01-04T11:00:00Z",
    "refundReason": "Partial return of items"
  }
}
```

**Payment Statuses:** `PENDING`, `APPROVED`, `DECLINED`, `REFUNDED`, `VOIDED`

---

## Error Responses

### 400 Bad Request
```json
{
  "timestamp": "2026-01-04T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "quantity",
      "message": "Quantity must be greater than zero"
    }
  ]
}
```

### 404 Not Found
```json
{
  "timestamp": "2026-01-04T10:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Transaction not found: 999"
}
```

### 409 Business Error
```json
{
  "timestamp": "2026-01-04T10:00:00Z",
  "status": 409,
  "error": "Business Error",
  "message": "Transaction is not fully paid. Balance due: 100000.0000"
}
```

---

## Workflow Examples

### Complete Sale Workflow

1. **Create Transaction**
   ```http
   POST /transactions
   {"terminalId": 1, "transactionType": "SALE"}
   ```

2. **Add Items**
   ```http
   POST /transactions/1/lines
   {"productId": 100, "quantity": 2}
   ```

3. **Apply Discount (optional)**
   ```http
   POST /transactions/1/discount
   {"percent": 5, "reason": "Member discount"}
   ```

4. **Add Payment(s)**
   ```http
   POST /transactions/1/payments
   {"paymentType": "CASH", "amount": 500000, "tenderedAmount": 500000}
   ```

5. **Complete Transaction**
   ```http
   POST /transactions/1/complete
   ```

### Return Workflow

1. **Create Return Transaction**
   ```http
   POST /transactions
   {"terminalId": 1, "transactionType": "RETURN", "originalTransactionId": 1}
   ```

2. **Add Return Items**
   ```http
   POST /transactions/2/lines
   {"productId": 100, "quantity": 1}
   ```

3. **Process Refund Payment**
   ```http
   POST /transactions/2/payments
   {"paymentType": "CASH", "amount": 168000}
   ```

4. **Complete Return**
   ```http
   POST /transactions/2/complete
   ```

---

## Integration Points

### Inventory Integration
- Stock is automatically deducted when a transaction is completed
- Stock is restored when a completed transaction is voided
- Returns add stock back to inventory

### General Ledger Integration
- Sales transactions are posted to GL when completed
- GL entries are reversed when transactions are voided
- Tax amounts are posted to appropriate tax accounts

### Shift Management
- All transactions are linked to the current open shift
- Shift totals are updated when transactions complete
- Cash payments affect the shift's expected cash balance

### Accounts Receivable Integration (Credit Sales)
- When a transaction is completed with a **CREDIT** payment type, an **AR Invoice** is automatically created
- The AR Invoice includes the credit portion of the transaction
- The transaction's `arInvoiceId` field is populated with the created invoice ID
- Customer must be associated with the transaction for credit sales
- Customer credit limit is validated before creating the AR Invoice
- The AR Invoice is posted to GL and updates the customer's balance
- For split payments (partial credit), the AR Invoice only covers the credit amount

**Credit Sale Example:**
```json
// Add a CREDIT payment
POST /transactions/1/payments
{
  "paymentType": "CREDIT",
  "amount": 200000.0000,
  "notes": "30-day credit terms"
}

// Complete transaction - AR Invoice auto-created
POST /transactions/1/complete

// Response includes AR Invoice reference
{
  "success": true,
  "data": {
    "id": 1,
    "transactionNumber": "TX20260104-00001",
    "status": "COMPLETED",
    "arInvoiceId": 15,  // Auto-created AR Invoice
    "glPosted": true,
    "stockDeducted": true
  }
}
```

---

## Default Role Permissions

### Store Manager
All POS permissions except terminal creation/deletion

### Cashier
- `POS_SHIFT_READ` - View shift information
- `POS_SALE_CREATE` - Create new sales
- `POS_SALE_READ` - View sales
- `POS_SALE_HOLD` - Hold and recall transactions
- `POS_DISCOUNT_APPLY` - Apply discounts
- `POS_PAYMENT_PROCESS` - Process payments
- `POS_DRAWER_OPEN` - Open cash drawer
