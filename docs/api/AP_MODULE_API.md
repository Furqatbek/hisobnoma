# Accounts Payable (AP) Module API Documentation

## Overview
The Accounts Payable module manages vendor invoices, payments, and related financial transactions. It integrates with the General Ledger for automatic journal entry posting and supports 3-way matching with Purchase Orders and Receiving Orders.

## Base URLs
- **AP Invoices**: `/api/v1/ap/invoices`
- **AP Payments**: `/api/v1/ap/payments`
- **AP Reports**: `/api/v1/ap/reports`
- **Vendor Contacts**: `/api/v1/inventory/vendors/{vendorId}/contacts`

---

## Authentication
All endpoints require JWT authentication via the `Authorization: Bearer {token}` header.

---

## AP Invoice Endpoints

### Get All Invoices
```
GET /api/v1/ap/invoices
```
**Permission**: `FINANCE_AP_VIEW`

**Query Parameters**:
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | int | 0 | Page number (0-indexed) |
| size | int | 20 | Page size |
| sort | string | createdAt,desc | Sort field and direction |

**Response**: `PageResponse<APInvoiceDto>`

---

### Get Invoices by Vendor
```
GET /api/v1/ap/invoices/vendor/{vendorId}
```
**Permission**: `FINANCE_AP_VIEW`

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| vendorId | Long | Vendor ID |

**Response**: `PageResponse<APInvoiceDto>`

---

### Get Invoices by Status
```
GET /api/v1/ap/invoices/status/{status}
```
**Permission**: `FINANCE_AP_VIEW`

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| status | APInvoiceStatus | Invoice status (DRAFT, PENDING_APPROVAL, APPROVED, PARTIAL, PAID, CANCELLED, ON_HOLD) |

**Response**: `PageResponse<APInvoiceDto>`

---

### Get Invoice by ID
```
GET /api/v1/ap/invoices/{id}
```
**Permission**: `FINANCE_AP_VIEW`

**Response**: `ApiResponse<APInvoiceDto>`

---

### Get Unpaid Invoices by Vendor
```
GET /api/v1/ap/invoices/vendor/{vendorId}/unpaid
```
**Permission**: `FINANCE_AP_VIEW`

**Response**: `ApiResponse<List<APInvoiceDto>>`

---

### Get Overdue Invoices
```
GET /api/v1/ap/invoices/overdue
```
**Permission**: `FINANCE_AP_VIEW`

**Response**: `ApiResponse<List<APInvoiceDto>>`

---

### Create Invoice
```
POST /api/v1/ap/invoices
```
**Permission**: `FINANCE_AP_CREATE`

**Request Body**:
```json
{
  "vendorInvoiceNumber": "INV-2024-001",
  "vendorId": 1,
  "invoiceDate": "2024-01-15",
  "dueDate": "2024-02-14",
  "receivedDate": "2024-01-15",
  "purchaseOrderId": 123,
  "receivingOrderId": 456,
  "discountAmount": 0.00,
  "taxAmount": 150.00,
  "shippingAmount": 50.00,
  "currency": "UZS",
  "exchangeRate": 1.0,
  "paymentTerms": "Net 30",
  "paymentTermsDays": 30,
  "expenseAccountId": 5100,
  "apAccountId": 2100,
  "description": "Office supplies purchase",
  "notes": "Standard monthly order",
  "lines": [
    {
      "productId": 101,
      "description": "Printer Paper A4",
      "expenseAccountId": 5100,
      "quantity": 10,
      "unitOfMeasure": "BOX",
      "unitPrice": 25.00,
      "discountPercent": 5.00,
      "taxCode": "VAT",
      "taxRate": 12.00,
      "purchaseOrderLineId": 1001,
      "receivingLineId": 2001,
      "notes": "White 80gsm"
    }
  ]
}
```

**Response**: `ApiResponse<APInvoiceDto>` (HTTP 201)

---

### Create Invoice from Receiving Order
```
POST /api/v1/ap/invoices/from-receiving/{receivingOrderId}
```
**Permission**: `FINANCE_AP_CREATE`

**Description**: Automatically creates an AP invoice from a completed receiving order, copying line items and amounts.

**Response**: `ApiResponse<APInvoiceDto>` (HTTP 201)

---

### Update Invoice
```
PUT /api/v1/ap/invoices/{id}
```
**Permission**: `FINANCE_AP_UPDATE`

**Note**: Only DRAFT invoices can be updated.

**Response**: `ApiResponse<APInvoiceDto>`

---

### Submit Invoice for Approval
```
POST /api/v1/ap/invoices/{id}/submit
```
**Permission**: `FINANCE_AP_CREATE`

**Response**: `ApiResponse<APInvoiceDto>`

---

### Approve Invoice
```
POST /api/v1/ap/invoices/{id}/approve
```
**Permission**: `FINANCE_AP_APPROVE`

**Description**: Approves the invoice and posts to the General Ledger.

**Response**: `ApiResponse<APInvoiceDto>`

---

### Reject Invoice
```
POST /api/v1/ap/invoices/{id}/reject
```
**Permission**: `FINANCE_AP_APPROVE`

**Request Body**:
```json
{
  "reason": "Invoice amount does not match PO"
}
```

**Response**: `ApiResponse<APInvoiceDto>`

---

### Cancel Invoice
```
POST /api/v1/ap/invoices/{id}/cancel
```
**Permission**: `FINANCE_AP_CANCEL`

**Request Body**:
```json
{
  "reason": "Duplicate invoice"
}
```

**Response**: `ApiResponse<APInvoiceDto>`

---

### Hold Invoice
```
POST /api/v1/ap/invoices/{id}/hold
```
**Permission**: `FINANCE_AP_UPDATE`

**Response**: `ApiResponse<APInvoiceDto>`

---

### Release Hold
```
POST /api/v1/ap/invoices/{id}/release-hold
```
**Permission**: `FINANCE_AP_UPDATE`

**Response**: `ApiResponse<APInvoiceDto>`

---

### Get Total Payable
```
GET /api/v1/ap/invoices/summary/total-payable
```
**Permission**: `FINANCE_AP_VIEW`

**Response**: `ApiResponse<BigDecimal>`

---

### Get Vendor Balance
```
GET /api/v1/ap/invoices/summary/vendor/{vendorId}/balance
```
**Permission**: `FINANCE_AP_VIEW`

**Response**: `ApiResponse<BigDecimal>`

---

### Get Overdue Balance
```
GET /api/v1/ap/invoices/summary/overdue-balance
```
**Permission**: `FINANCE_AP_VIEW`

**Response**: `ApiResponse<BigDecimal>`

---

## AP Payment Endpoints

### Get All Payments
```
GET /api/v1/ap/payments
```
**Permission**: `FINANCE_AP_PAY_VIEW`

**Response**: `PageResponse<APPaymentDto>`

---

### Get Payments by Vendor
```
GET /api/v1/ap/payments/vendor/{vendorId}
```
**Permission**: `FINANCE_AP_PAY_VIEW`

**Response**: `PageResponse<APPaymentDto>`

---

### Get Payments by Status
```
GET /api/v1/ap/payments/status/{status}
```
**Permission**: `FINANCE_AP_PAY_VIEW`

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| status | APPaymentStatus | Payment status (DRAFT, PENDING_APPROVAL, APPROVED, COMPLETED, VOIDED, FAILED) |

**Response**: `PageResponse<APPaymentDto>`

---

### Get Payment by ID
```
GET /api/v1/ap/payments/{id}
```
**Permission**: `FINANCE_AP_PAY_VIEW`

**Response**: `ApiResponse<APPaymentDto>`

---

### Get Payments by Date Range
```
GET /api/v1/ap/payments/date-range?startDate={date}&endDate={date}
```
**Permission**: `FINANCE_AP_PAY_VIEW`

**Query Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| startDate | LocalDate | Start date (ISO format: YYYY-MM-DD) |
| endDate | LocalDate | End date (ISO format: YYYY-MM-DD) |

**Response**: `ApiResponse<List<APPaymentDto>>`

---

### Get Unreconciled Payments
```
GET /api/v1/ap/payments/unreconciled
```
**Permission**: `FINANCE_AP_PAY_VIEW`

**Response**: `ApiResponse<List<APPaymentDto>>`

---

### Create Payment
```
POST /api/v1/ap/payments
```
**Permission**: `FINANCE_AP_PAY`

**Request Body**:
```json
{
  "vendorId": 1,
  "paymentDate": "2024-01-20",
  "paymentMethod": "BANK_TRANSFER",
  "paymentAmount": 1000.00,
  "currency": "UZS",
  "exchangeRate": 1.0,
  "bankAccountId": 1,
  "cashAccountId": 1100,
  "apAccountId": 2100,
  "referenceNumber": "TRF-2024-001",
  "checkNumber": null,
  "checkDate": null,
  "memo": "Monthly vendor payment",
  "notes": "Covers invoices AP-000001 and AP-000002",
  "allocations": [
    {
      "apInvoiceId": 1,
      "allocatedAmount": 500.00,
      "discountTaken": 25.00,
      "writeOffAmount": 0.00,
      "notes": "Full payment with early payment discount"
    },
    {
      "apInvoiceId": 2,
      "allocatedAmount": 500.00,
      "discountTaken": 0.00,
      "writeOffAmount": 0.00,
      "notes": "Full payment"
    }
  ]
}
```

**Payment Methods**: `CASH`, `CHECK`, `BANK_TRANSFER`, `CREDIT_CARD`, `ACH`, `ONLINE`, `OTHER`

**Response**: `ApiResponse<APPaymentDto>` (HTTP 201)

---

### Submit Payment for Approval
```
POST /api/v1/ap/payments/{id}/submit
```
**Permission**: `FINANCE_AP_PAY`

**Response**: `ApiResponse<APPaymentDto>`

---

### Approve Payment
```
POST /api/v1/ap/payments/{id}/approve
```
**Permission**: `FINANCE_AP_PAY_APPROVE`

**Response**: `ApiResponse<APPaymentDto>`

---

### Process Payment
```
POST /api/v1/ap/payments/{id}/process
```
**Permission**: `FINANCE_AP_PAY`

**Description**: Processes an approved payment, applying amounts to invoices and posting to GL.

**Response**: `ApiResponse<APPaymentDto>`

---

### Void Payment
```
POST /api/v1/ap/payments/{id}/void
```
**Permission**: `FINANCE_AP_PAY_VOID`

**Request Body**:
```json
{
  "reason": "Payment was made in error"
}
```

**Response**: `ApiResponse<APPaymentDto>`

---

### Reconcile Payment
```
POST /api/v1/ap/payments/{id}/reconcile
```
**Permission**: `FINANCE_AP_PAY`

**Description**: Marks a completed payment as reconciled with bank statement.

**Response**: `ApiResponse<APPaymentDto>`

---

### Get Total Payments by Vendor
```
GET /api/v1/ap/payments/summary/vendor/{vendorId}/total
```
**Permission**: `FINANCE_AP_PAY_VIEW`

**Response**: `ApiResponse<BigDecimal>`

---

### Get Total Payments by Date Range
```
GET /api/v1/ap/payments/summary/date-range/total?startDate={date}&endDate={date}
```
**Permission**: `FINANCE_AP_PAY_VIEW`

**Response**: `ApiResponse<BigDecimal>`

---

## AP Reports Endpoints

### Get AP Aging Report
```
GET /api/v1/ap/reports/aging
```
**Permission**: `FINANCE_AP_VIEW`

**Description**: Returns outstanding payables grouped by aging buckets (Current, 1-30, 31-60, 61-90, >90 days).

**Response**:
```json
{
  "success": true,
  "data": {
    "reportDate": "2024-01-20",
    "tenantId": 1,
    "totalPayable": 50000.00,
    "current": 20000.00,
    "overdue1To30": 15000.00,
    "overdue31To60": 8000.00,
    "overdue61To90": 5000.00,
    "overdueOver90": 2000.00,
    "vendorAging": [
      {
        "vendorId": 1,
        "vendorCode": "VENDOR001",
        "vendorName": "ABC Supplies",
        "totalBalance": 25000.00,
        "current": 10000.00,
        "overdue1To30": 8000.00,
        "overdue31To60": 4000.00,
        "overdue61To90": 2000.00,
        "overdueOver90": 1000.00,
        "invoiceCount": 5
      }
    ]
  }
}
```

---

### Get Vendor Balance Report
```
GET /api/v1/ap/reports/vendor-balance
```
**Permission**: `FINANCE_VENDOR_STATEMENT`

**Description**: Returns balance information for all vendors with outstanding payables.

**Response**:
```json
{
  "success": true,
  "data": {
    "reportDate": "2024-01-20",
    "tenantId": 1,
    "totalPayable": 50000.00,
    "totalPayments": 30000.00,
    "netBalance": 50000.00,
    "vendorCount": 10,
    "vendorBalances": [
      {
        "vendorId": 1,
        "vendorCode": "VENDOR001",
        "vendorName": "ABC Supplies",
        "contactPerson": "John Doe",
        "email": "john@abcsupplies.com",
        "phone": "+998901234567",
        "creditLimit": 100000.00,
        "totalInvoiced": 55000.00,
        "totalPaid": 30000.00,
        "currentBalance": 25000.00,
        "availableCredit": 75000.00,
        "openInvoiceCount": 3,
        "lastInvoiceDate": "2024-01-15",
        "lastPaymentDate": "2024-01-10"
      }
    ]
  }
}
```

---

### Get Vendor Statement
```
GET /api/v1/ap/reports/vendor/{vendorId}/statement
```
**Permission**: `FINANCE_VENDOR_STATEMENT`

**Response**: `ApiResponse<VendorBalanceDto>`

---

## Vendor Contacts Endpoints

### Get Vendor Contacts
```
GET /api/v1/inventory/vendors/{vendorId}/contacts
```
**Permission**: `INVENTORY_VENDOR_VIEW`

**Response**: `ApiResponse<List<VendorContactDto>>`

---

### Get Primary Contact
```
GET /api/v1/inventory/vendors/{vendorId}/contacts/primary
```
**Permission**: `INVENTORY_VENDOR_VIEW`

**Response**: `ApiResponse<VendorContactDto>`

---

### Get Billing Contact
```
GET /api/v1/inventory/vendors/{vendorId}/contacts/billing
```
**Permission**: `INVENTORY_VENDOR_VIEW`

**Response**: `ApiResponse<VendorContactDto>`

---

### Get Ordering Contact
```
GET /api/v1/inventory/vendors/{vendorId}/contacts/ordering
```
**Permission**: `INVENTORY_VENDOR_VIEW`

**Response**: `ApiResponse<VendorContactDto>`

---

### Create Vendor Contact
```
POST /api/v1/inventory/vendors/{vendorId}/contacts
```
**Permission**: `INVENTORY_VENDOR_MANAGE`

**Request Body**:
```json
{
  "name": "John Doe",
  "title": "Sales Manager",
  "department": "Sales",
  "email": "john.doe@vendor.com",
  "phone": "+998901234567",
  "mobilePhone": "+998901234568",
  "fax": "+998901234569",
  "primary": true,
  "billingContact": false,
  "orderingContact": true,
  "notes": "Preferred contact for orders"
}
```

**Response**: `ApiResponse<VendorContactDto>` (HTTP 201)

---

### Update Vendor Contact
```
PUT /api/v1/inventory/vendors/contacts/{id}
```
**Permission**: `INVENTORY_VENDOR_MANAGE`

**Response**: `ApiResponse<VendorContactDto>`

---

### Delete Vendor Contact
```
DELETE /api/v1/inventory/vendors/contacts/{id}
```
**Permission**: `INVENTORY_VENDOR_MANAGE`

**Response**: HTTP 204 No Content

---

### Set Primary Contact
```
PUT /api/v1/inventory/vendors/contacts/{id}/set-primary
```
**Permission**: `INVENTORY_VENDOR_MANAGE`

**Response**: `ApiResponse<VendorContactDto>`

---

## Permissions Summary

| Permission Code | Description | Assigned To |
|----------------|-------------|-------------|
| FINANCE_AP_VIEW | View AP invoices | SUPER_ADMIN, ADMIN, FINANCE_MANAGER, ACCOUNTANT, VIEWER |
| FINANCE_AP_CREATE | Create AP invoices | SUPER_ADMIN, ADMIN, FINANCE_MANAGER, ACCOUNTANT |
| FINANCE_AP_UPDATE | Update AP invoices | SUPER_ADMIN, ADMIN, FINANCE_MANAGER, ACCOUNTANT |
| FINANCE_AP_APPROVE | Approve/reject AP invoices | SUPER_ADMIN, ADMIN, FINANCE_MANAGER |
| FINANCE_AP_CANCEL | Cancel AP invoices | SUPER_ADMIN, ADMIN, FINANCE_MANAGER |
| FINANCE_AP_PAY_VIEW | View AP payments | SUPER_ADMIN, ADMIN, FINANCE_MANAGER, ACCOUNTANT, VIEWER |
| FINANCE_AP_PAY | Create/process AP payments | SUPER_ADMIN, ADMIN, FINANCE_MANAGER, ACCOUNTANT |
| FINANCE_AP_PAY_APPROVE | Approve AP payments | SUPER_ADMIN, ADMIN, FINANCE_MANAGER |
| FINANCE_AP_PAY_VOID | Void AP payments | SUPER_ADMIN, ADMIN, FINANCE_MANAGER |
| FINANCE_VENDOR_STATEMENT | View vendor statements/balances | SUPER_ADMIN, ADMIN, FINANCE_MANAGER, ACCOUNTANT, VIEWER |

---

## Invoice Status Workflow

```
DRAFT → PENDING_APPROVAL → APPROVED → PARTIAL → PAID
                       ↓
                   ON_HOLD ← (can be held from any non-terminal status)
                       ↓
                   CANCELLED
```

## Payment Status Workflow

```
DRAFT → PENDING_APPROVAL → APPROVED → COMPLETED
                                    ↓
                                 VOIDED
```

---

## 3-Way Matching

The AP module supports 3-way matching between:
1. **Purchase Order** - What was ordered
2. **Receiving Order** - What was received
3. **AP Invoice** - What was invoiced

When creating an invoice linked to a PO or receiving order, the system automatically:
- Calculates quantity and price variances
- Sets matching status (MATCHED or VARIANCE)
- Records matching notes for review

---

## GL Integration

AP transactions automatically post to the General Ledger:

**Invoice Approval**:
- Debit: Expense accounts (from invoice lines)
- Credit: Accounts Payable

**Payment Processing**:
- Debit: Accounts Payable
- Credit: Cash/Bank account
- Credit: Purchase Discounts (if discount taken)

**Cancellation/Void**: Creates reversing journal entries

---

## Inventory Integration (Auto-Invoice from Receiving)

When a **Receiving Order** is confirmed, an **AP Invoice** is automatically created:

- The AP Invoice is linked to the receiving order via `receivingId`
- Invoice lines are created from receiving order lines
- The receiving order's `apInvoiceCreated` flag is set to `true`
- The receiving order's `apInvoiceId` is populated with the created invoice ID
- The vendor's balance is updated accordingly
- The invoice is created in PENDING_APPROVAL status

This ensures seamless integration between goods receipt and accounts payable.

**Workflow:**
1. **Purchase Order** → Order placed with vendor
2. **Receiving Order** → Goods received and confirmed
3. **AP Invoice** → Auto-created from receiving confirmation
4. **Payment** → Process payment when due

**Note**: If the auto-creation fails, the receiving confirmation still succeeds but the AP invoice must be created manually using the `createFromReceiving` endpoint.
