# Reports Module - API Documentation

## Overview

The Reports module provides comprehensive reporting capabilities for inventory, sales, and financial data. It supports report generation, scheduling, and export to multiple formats (Excel, CSV, PDF, JSON).

## Authentication

All endpoints require JWT Bearer token authentication and appropriate RBAC permissions.

### Report Permissions
- `REPORT_VIEW` - View available reports and report definitions
- `REPORT_GENERATE` - Generate reports
- `REPORT_EXPORT` - Export reports to various formats
- `REPORT_SCHEDULE_VIEW` - View report schedules
- `REPORT_SCHEDULE_MANAGE` - Create, update, delete report schedules
- `REPORT_INVENTORY_VIEW` - Generate inventory reports
- `REPORT_SALES_VIEW` - Generate sales reports
- `REPORT_FINANCIAL_VIEW` - Generate financial reports

## Base URL

```
/api/v1/reports
```

---

## Report Definitions

### List Report Definitions

```http
GET /definitions
```

**Permission:** `REPORT_VIEW`

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | int | 0 | Page number (0-based) |
| size | int | 20 | Page size |
| sort | string | name,asc | Sort field and direction |

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "code": "STOCK_ON_HAND",
        "name": "Stock on Hand Report",
        "description": "Shows current stock levels for all products by location",
        "category": "INVENTORY",
        "systemReport": true,
        "active": true,
        "supportedFormats": ["EXCEL", "CSV", "JSON"]
      }
    ],
    "totalElements": 6,
    "totalPages": 1
  }
}
```

---

### Get Report Definition

```http
GET /definitions/{id}
```

**Permission:** `REPORT_VIEW`

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "code": "STOCK_ON_HAND",
    "name": "Stock on Hand Report",
    "description": "Shows current stock levels for all products by location",
    "category": "INVENTORY",
    "systemReport": true,
    "active": true,
    "supportedFormats": ["EXCEL", "CSV", "JSON"]
  }
}
```

---

### Get Report by Code

```http
GET /definitions/code/{code}
```

**Permission:** `REPORT_VIEW`

---

### List Reports by Category

```http
GET /definitions/category/{category}
```

**Permission:** `REPORT_VIEW`

**Category Values:** `INVENTORY`, `FINANCIAL`, `SALES`, `PURCHASING`, `CUSTOM`

---

## Inventory Reports

### Generate Stock on Hand Report

```http
POST /inventory/stock-on-hand
```

**Permission:** `REPORT_INVENTORY_VIEW`

**Request Body:**
```json
{
  "locationId": null,
  "categoryId": null
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "metadata": {
      "reportName": "Stock on Hand Report",
      "generatedAt": "2026-01-15T10:30:00Z",
      "generatedBy": "Admin User",
      "locationFilter": "All Locations",
      "categoryFilter": "All Categories"
    },
    "summary": {
      "totalSkus": 150,
      "totalQuantity": 5000,
      "totalValue": 250000000.00,
      "lowStockCount": 12,
      "outOfStockCount": 3
    },
    "items": [
      {
        "productId": 1,
        "sku": "SKU-001",
        "productName": "Product A",
        "category": "Electronics",
        "location": "Main Warehouse",
        "quantityOnHand": 100,
        "quantityReserved": 10,
        "quantityAvailable": 90,
        "unitCost": 50000.00,
        "totalValue": 5000000.00,
        "reorderPoint": 20,
        "stockStatus": "IN_STOCK"
      }
    ]
  }
}
```

---

### Export Stock on Hand Report

```http
POST /inventory/stock-on-hand/export
```

**Permission:** `REPORT_INVENTORY_VIEW`

**Request Body:**
```json
{
  "exportFormat": "EXCEL",
  "locationId": null,
  "categoryId": null
}
```

> **Note:** `exportFormat` is optional. Defaults to `EXCEL` if not provided.

**Response:** Binary file download with appropriate Content-Type header.

**Content-Type Values:**
- EXCEL: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- CSV: `text/csv`
- JSON: `application/json`
- PDF: `application/pdf`

---

### Generate Inventory Valuation Report

```http
POST /inventory/valuation
```

**Permission:** `REPORT_INVENTORY_VIEW`

**Request Body:**
```json
{
  "categoryId": null
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "metadata": {
      "reportName": "Inventory Valuation Report",
      "generatedAt": "2026-01-15T10:30:00Z",
      "asOfDate": "2026-01-15",
      "valuationMethod": "Average Cost"
    },
    "summary": {
      "totalSkus": 150,
      "totalQuantity": 5000,
      "totalCostValue": 200000000.00,
      "totalRetailValue": 280000000.00,
      "potentialMargin": 80000000.00,
      "marginPercent": 28.57
    },
    "items": [
      {
        "productId": 1,
        "sku": "SKU-001",
        "productName": "Product A",
        "category": "Electronics",
        "quantity": 100,
        "unitCost": 50000.00,
        "totalCost": 5000000.00,
        "unitRetail": 70000.00,
        "totalRetail": 7000000.00,
        "margin": 2000000.00,
        "marginPercent": 28.57
      }
    ],
    "byCategory": [
      {
        "categoryId": 1,
        "categoryName": "Electronics",
        "skuCount": 50,
        "totalQuantity": 2000,
        "totalCostValue": 100000000.00,
        "totalRetailValue": 140000000.00,
        "percentOfTotal": 50.0
      }
    ]
  }
}
```

---

### Export Inventory Valuation Report

```http
POST /inventory/valuation/export
```

**Permission:** `REPORT_INVENTORY_VIEW`

---

## Sales Reports

### Generate Sales Summary Report

```http
POST /sales/summary
```

**Permission:** `REPORT_SALES_VIEW`

**Request Body:**
```json
{
  "startDate": "2026-01-01",
  "endDate": "2026-01-15",
  "locationId": null
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "metadata": {
      "reportName": "Sales Summary Report",
      "generatedAt": "2026-01-15T10:30:00Z",
      "startDate": "2026-01-01",
      "endDate": "2026-01-15",
      "locationFilter": "All Locations"
    },
    "summary": {
      "grossSales": 50000000.00,
      "discounts": 2500000.00,
      "returns": 1000000.00,
      "netSales": 46500000.00,
      "costOfGoods": 30000000.00,
      "grossProfit": 16500000.00,
      "grossMarginPercent": 35.48,
      "transactionCount": 500,
      "averageTransactionValue": 93000.00,
      "itemsSold": 1500
    },
    "dailyBreakdown": [
      {
        "date": "2026-01-15",
        "dayOfWeek": "Wednesday",
        "netSales": 5000000.00,
        "transactionCount": 50,
        "averageTransaction": 100000.00
      }
    ],
    "byCategory": [
      {
        "categoryId": 1,
        "categoryName": "Electronics",
        "netSales": 25000000.00,
        "itemsSold": 500,
        "percentOfTotal": 53.76
      }
    ],
    "topProducts": [
      {
        "productId": 1,
        "sku": "SKU-001",
        "productName": "Product A",
        "quantitySold": 100,
        "netSales": 7000000.00,
        "profit": 2000000.00
      }
    ],
    "byPaymentMethod": [
      {
        "paymentMethod": "CASH",
        "amount": 30000000.00,
        "transactionCount": 300,
        "percentOfTotal": 64.52
      },
      {
        "paymentMethod": "CARD",
        "amount": 16500000.00,
        "transactionCount": 200,
        "percentOfTotal": 35.48
      }
    ]
  }
}
```

---

### Export Sales Summary Report

```http
POST /sales/summary/export
```

**Permission:** `REPORT_SALES_VIEW`

---

## Financial Reports

### Generate Trial Balance Report

```http
POST /financial/trial-balance
```

**Permission:** `REPORT_FINANCIAL_VIEW`

**Request Body:**
```json
{
  "endDate": "2026-01-15"
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "metadata": {
      "reportName": "Trial Balance",
      "generatedAt": "2026-01-15T10:30:00Z",
      "asOfDate": "2026-01-15",
      "fiscalYear": "2026",
      "period": "JANUARY"
    },
    "accounts": [
      {
        "accountId": 1,
        "accountCode": "1000",
        "accountName": "Cash",
        "accountType": "ASSET",
        "accountSubType": null,
        "debitBalance": 50000000.00,
        "creditBalance": 0,
        "level": 1,
        "isHeader": false
      },
      {
        "accountId": 2,
        "accountCode": "2000",
        "accountName": "Accounts Payable",
        "accountType": "LIABILITY",
        "accountSubType": null,
        "debitBalance": 0,
        "creditBalance": 10000000.00,
        "level": 1,
        "isHeader": false
      }
    ],
    "totals": {
      "totalDebits": 100000000.00,
      "totalCredits": 100000000.00,
      "isBalanced": true,
      "difference": 0
    }
  }
}
```

---

### Export Trial Balance Report

```http
POST /financial/trial-balance/export
```

**Permission:** `REPORT_FINANCIAL_VIEW`

---

### Generate AR Aging Report

```http
POST /financial/ar-aging
```

**Permission:** `REPORT_FINANCIAL_VIEW`

**Request Body:**
```json
{
  "endDate": "2026-01-15"
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "metadata": {
      "reportName": "Accounts Receivable Aging Report",
      "reportType": "AR",
      "generatedAt": "2026-01-15T10:30:00Z",
      "asOfDate": "2026-01-15"
    },
    "summary": {
      "totalOutstanding": 25000000.00,
      "current": 15000000.00,
      "days1to30": 5000000.00,
      "days31to60": 3000000.00,
      "days61to90": 1500000.00,
      "over90Days": 500000.00,
      "totalAccounts": 25,
      "overdueAccounts": 10
    },
    "details": [
      {
        "entityId": 1,
        "entityCode": "CUST001",
        "entityName": "Customer A",
        "contactInfo": "+998901234567",
        "creditLimit": 10000000.00,
        "totalOutstanding": 5000000.00,
        "current": 3000000.00,
        "days1to30": 1500000.00,
        "days31to60": 500000.00,
        "days61to90": 0,
        "over90Days": 0,
        "overdueInvoices": 2,
        "oldestInvoiceDate": "2025-12-15"
      }
    ]
  }
}
```

---

### Export AR Aging Report

```http
POST /financial/ar-aging/export
```

**Permission:** `REPORT_FINANCIAL_VIEW`

---

### Generate AP Aging Report

```http
POST /financial/ap-aging
```

**Permission:** `REPORT_FINANCIAL_VIEW`

**Request Body:**
```json
{
  "endDate": "2026-01-15"
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "metadata": {
      "reportName": "Accounts Payable Aging Report",
      "reportType": "AP",
      "generatedAt": "2026-01-15T10:30:00Z",
      "asOfDate": "2026-01-15"
    },
    "summary": {
      "totalOutstanding": 15000000.00,
      "current": 10000000.00,
      "days1to30": 3000000.00,
      "days31to60": 1500000.00,
      "days61to90": 300000.00,
      "over90Days": 200000.00,
      "totalAccounts": 15,
      "overdueAccounts": 5
    },
    "details": [
      {
        "entityId": 1,
        "entityCode": "V-1",
        "entityName": "Vendor A",
        "contactInfo": null,
        "creditLimit": null,
        "totalOutstanding": 3000000.00,
        "current": 2000000.00,
        "days1to30": 1000000.00,
        "days31to60": 0,
        "days61to90": 0,
        "over90Days": 0,
        "overdueInvoices": 1,
        "oldestInvoiceDate": "2025-12-20"
      }
    ]
  }
}
```

---

### Export AP Aging Report

```http
POST /financial/ap-aging/export
```

**Permission:** `REPORT_FINANCIAL_VIEW`

---

## Report Schedules

### List Schedules

```http
GET /schedules
```

**Permission:** `REPORT_SCHEDULE_VIEW`

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | int | 0 | Page number (0-based) |
| size | int | 20 | Page size |
| sort | string | createdAt,desc | Sort field and direction |

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "reportDefinitionId": 1,
        "reportCode": "STOCK_ON_HAND",
        "reportName": "Stock on Hand Report",
        "name": "Daily Stock Report",
        "frequency": "DAILY",
        "cronExpression": null,
        "runTime": "06:00:00",
        "dayOfWeek": null,
        "dayOfMonth": null,
        "exportFormat": "EXCEL",
        "deliveryMethod": "EMAIL",
        "emailRecipients": "manager@company.com",
        "lastRunAt": "2026-01-14T06:00:00Z",
        "nextRunAt": "2026-01-15T06:00:00Z",
        "lastRunStatus": "COMPLETED",
        "active": true
      }
    ],
    "totalElements": 5,
    "totalPages": 1
  }
}
```

---

### Get Schedule

```http
GET /schedules/{id}
```

**Permission:** `REPORT_SCHEDULE_VIEW`

---

### Create Schedule

```http
POST /schedules
```

**Permission:** `REPORT_SCHEDULE_MANAGE`

**Request Body:**
```json
{
  "reportDefinitionId": 1,
  "name": "Daily Stock Report",
  "frequency": "DAILY",
  "runTime": "06:00:00",
  "exportFormat": "EXCEL",
  "deliveryMethod": "EMAIL",
  "emailRecipients": "manager@company.com,warehouse@company.com"
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Report schedule created successfully",
  "data": {
    "id": 1,
    "name": "Daily Stock Report",
    "nextRunAt": "2026-01-16T06:00:00Z",
    "active": true
  }
}
```

---

### Update Schedule

```http
PUT /schedules/{id}
```

**Permission:** `REPORT_SCHEDULE_MANAGE`

---

### Delete Schedule

```http
DELETE /schedules/{id}
```

**Permission:** `REPORT_SCHEDULE_MANAGE`

---

### Toggle Schedule

```http
PUT /schedules/{id}/toggle?active=true
```

**Permission:** `REPORT_SCHEDULE_MANAGE`

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| active | boolean | Yes | Enable (true) or disable (false) the schedule |

---

## Report Executions

### List Executions

```http
GET /executions
```

**Permission:** `REPORT_VIEW`

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | int | 0 | Page number (0-based) |
| size | int | 20 | Page size |
| sort | string | startedAt,desc | Sort field and direction |

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "reportDefinitionId": 1,
        "reportCode": "STOCK_ON_HAND",
        "reportName": "Stock on Hand Report",
        "executedByUserId": 1,
        "executedByUsername": "admin",
        "source": "MANUAL",
        "status": "COMPLETED",
        "exportFormat": "EXCEL",
        "startedAt": "2026-01-15T10:30:00Z",
        "completedAt": "2026-01-15T10:30:05Z",
        "durationMs": 5000,
        "rowCount": 150,
        "fileSize": 45678,
        "errorMessage": null
      }
    ],
    "totalElements": 100,
    "totalPages": 5
  }
}
```

---

### Get Execution Details

```http
GET /executions/{id}
```

**Permission:** `REPORT_VIEW`

---

### Get Executions by Report

```http
GET /executions/report/{reportId}
```

**Permission:** `REPORT_VIEW`

---

## Enums

### Report Categories
- `INVENTORY` - Inventory-related reports
- `FINANCIAL` - Financial reports (Trial Balance, Aging, etc.)
- `SALES` - Sales and revenue reports
- `PURCHASING` - Purchasing and vendor reports
- `CUSTOM` - Custom user-defined reports

### Export Formats
- `PDF` - PDF document
- `EXCEL` - Excel spreadsheet (.xlsx)
- `CSV` - Comma-separated values
- `JSON` - JSON format

### Schedule Frequencies
- `DAILY` - Run every day
- `WEEKLY` - Run every week
- `MONTHLY` - Run every month
- `QUARTERLY` - Run every quarter
- `YEARLY` - Run once a year
- `CUSTOM_CRON` - Custom cron expression

### Delivery Methods
- `EMAIL` - Send via email
- `STORE_ONLY` - Store in system only
- `WEBHOOK` - Send to webhook URL

### Execution Status
- `PENDING` - Waiting to start
- `RUNNING` - Currently executing
- `COMPLETED` - Finished successfully
- `FAILED` - Failed with error
- `CANCELLED` - Cancelled by user

### Execution Source
- `MANUAL` - User-initiated
- `SCHEDULED` - Automated scheduled execution
- `API` - API-triggered

---

## Error Responses

### 400 Bad Request
```json
{
  "success": false,
  "message": "Export format is required",
  "error": {
    "code": "VALIDATION_ERROR"
  }
}
```

### 403 Forbidden
```json
{
  "success": false,
  "message": "Access denied. Required permission: REPORT_INVENTORY_VIEW",
  "error": {
    "code": "FORBIDDEN"
  }
}
```

### 404 Not Found
```json
{
  "success": false,
  "message": "Report definition not found: UNKNOWN_REPORT",
  "error": {
    "code": "NOT_FOUND"
  }
}
```
