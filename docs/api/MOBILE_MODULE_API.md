# Mobile Module - API Documentation

## Overview

The Mobile module provides APIs for mobile application integration including authentication, dashboard summaries, alerts management, quick actions (barcode lookup, stock count, quick sales), and offline data synchronization.

## Authentication

All endpoints require JWT Bearer token authentication and appropriate RBAC permissions:

### Mobile Permissions
- `MOBILE_DASHBOARD_VIEW` - View mobile dashboard and summaries
- `MOBILE_INVENTORY_VIEW` - View inventory on mobile
- `MOBILE_ALERTS_VIEW` - View and manage alerts on mobile
- `MOBILE_ALERTS_MANAGE` - Configure alert preferences
- `MOBILE_SYNC_ACCESS` - Access offline sync data
- `MOBILE_QUICK_SALE` - Create quick sales from mobile
- `MOBILE_QUICK_COUNT` - Perform quick stock counts from mobile

## Base URL

```
/api/v1/mobile
```

---

## Mobile Authentication

### Login

```http
POST /auth/login
```

**Request Body:**
```json
{
  "phone": "+998901234567",
  "code": "123456"
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "userId": 1,
    "tenantId": 1,
    "permissions": ["MOBILE_DASHBOARD_VIEW", "MOBILE_INVENTORY_VIEW"]
  }
}
```

### Refresh Token

```http
POST /auth/refresh
```

**Request Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### Register Device

```http
POST /auth/register-device
```

**Request Body:**
```json
{
  "deviceId": "unique-device-identifier",
  "fcmToken": "firebase-cloud-messaging-token",
  "platform": "ANDROID",
  "deviceName": "Samsung Galaxy S21",
  "deviceModel": "SM-G991B",
  "osVersion": "14.0",
  "appVersion": "1.0.0"
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "deviceId": "unique-device-identifier",
    "platform": "ANDROID",
    "deviceName": "Samsung Galaxy S21",
    "active": true,
    "lastActiveAt": "2026-01-15T10:30:00Z"
  }
}
```

### Get Registered Devices

```http
GET /auth/devices
```

**Response:** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "deviceId": "unique-device-identifier",
      "platform": "ANDROID",
      "deviceName": "Samsung Galaxy S21",
      "deviceModel": "SM-G991B",
      "osVersion": "14.0",
      "appVersion": "1.0.0",
      "active": true,
      "lastActiveAt": "2026-01-15T10:30:00Z"
    }
  ]
}
```

### Deactivate Device

```http
DELETE /auth/devices/{deviceId}
```

### Logout

```http
POST /auth/logout
```

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| deviceId | string | Device ID to deactivate (optional) |

---

## Dashboard APIs

### Get Revenue Summary

```http
GET /dashboard/revenue
```

**Required Permissions:** `MOBILE_DASHBOARD_VIEW`, `REPORTS_SALES_VIEW`, or `ADMIN_DASHBOARD_VIEW`

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "todayRevenue": 1500000.00,
    "yesterdayRevenue": 1200000.00,
    "thisWeekRevenue": 8500000.00,
    "lastWeekRevenue": 7800000.00,
    "thisMonthRevenue": 35000000.00,
    "lastMonthRevenue": 32000000.00,
    "todayChangePercent": 25.0,
    "weekChangePercent": 8.97,
    "monthChangePercent": 9.38,
    "todayTransactionCount": 45,
    "thisWeekTransactionCount": 312,
    "thisMonthTransactionCount": 1450,
    "averageTransactionValue": 24137.93,
    "hourlyRevenue": [
      {
        "label": "09:00",
        "value": 250000.00,
        "count": 8
      }
    ],
    "dailyRevenue": [
      {
        "label": "2026-01-01",
        "value": 1200000.00,
        "count": 35
      }
    ],
    "monthlyRevenue": [
      {
        "label": "2026-01",
        "value": 35000000.00,
        "count": 1450
      }
    ]
  }
}
```

### Get Revenue Chart Data

```http
GET /dashboard/revenue/chart
```

**Required Permissions:** `MOBILE_DASHBOARD_VIEW`, `REPORTS_SALES_VIEW`, or `ADMIN_DASHBOARD_VIEW`

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| period | string | `hourly`, `daily`, or `monthly` (default: `daily`) |

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "hourlyRevenue": [
      {
        "label": "09:00",
        "value": 250000.00,
        "count": 8
      }
    ],
    "dailyRevenue": [
      {
        "label": "2026-01-01",
        "value": 1200000.00,
        "count": 35
      }
    ],
    "monthlyRevenue": [
      {
        "label": "2026-01",
        "value": 35000000.00,
        "count": 1450
      }
    ]
  }
}
```

### Get Inventory Summary

```http
GET /dashboard/inventory
```

**Required Permissions:** `MOBILE_DASHBOARD_VIEW`, `INVENTORY_STOCK_READ`, or `ADMIN_DASHBOARD_VIEW`

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "totalSkuCount": 1500,
    "activeSkuCount": 1420,
    "totalInventoryValue": 250000000.00,
    "lowStockCount": 45,
    "outOfStockCount": 12,
    "expiringCount": 8,
    "reorderPendingCount": 15,
    "lowStockItems": [
      {
        "productId": 123,
        "productName": "Product Name",
        "sku": "SKU-001",
        "currentStock": 5.00,
        "reorderPoint": 10.00,
        "minStockLevel": 3.00,
        "locationName": "Main Warehouse",
        "urgency": "HIGH"
      }
    ],
    "outOfStockItems": [
      {
        "productId": 456,
        "productName": "Product Name",
        "sku": "SKU-002",
        "locationName": "Main Warehouse",
        "lastStockDate": "2026-01-10",
        "daysSinceOutOfStock": 5
      }
    ],
    "expiringItems": [
      {
        "productId": 789,
        "productName": "Product Name",
        "sku": "SKU-003",
        "batchNumber": "BATCH-001",
        "quantity": 50.00,
        "expiryDate": "2026-02-15",
        "daysUntilExpiry": 30,
        "locationName": "Main Warehouse"
      }
    ],
    "recentMovements": [
      {
        "movementId": 1,
        "movementType": "TRANSFER",
        "productName": "Product Name",
        "sku": "SKU-001",
        "quantity": 25.00,
        "fromLocation": "Main Warehouse",
        "toLocation": "Store Front",
        "reference": "TRF-20260115-001",
        "createdBy": "admin",
        "createdAt": "2026-01-15T09:30:00Z"
      }
    ]
  }
}
```

### Get Financial Summary

```http
GET /dashboard/finance
```

**Required Permissions:** `MOBILE_DASHBOARD_VIEW`, `FINANCE_REPORTS_VIEW`, or `ADMIN_DASHBOARD_VIEW`

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "totalCashBalance": 5000000.00,
    "totalBankBalance": 50000000.00,
    "arOutstanding": 15000000.00,
    "apOutstanding": 8000000.00,
    "netCashPosition": 57000000.00,
    "todayIncome": 1500000.00,
    "todayExpenses": 800000.00,
    "todayNetProfit": 700000.00,
    "thisMonthIncome": 35000000.00,
    "thisMonthExpenses": 22000000.00,
    "thisMonthNetProfit": 13000000.00,
    "profitMarginPercent": 37.14,
    "cashFlow": {
      "openingBalance": 4500000.00,
      "totalInflows": 1500000.00,
      "totalOutflows": 1000000.00,
      "closingBalance": 5000000.00,
      "recentFlows": [
        {
          "description": "Payment from Customer A",
          "amount": 500000.00,
          "type": "INFLOW",
          "date": "2026-01-15T10:00:00Z"
        }
      ]
    },
    "receivables": {
      "totalOutstanding": 15000000.00,
      "current": 8000000.00,
      "overdue1To30": 4000000.00,
      "overdue31To60": 2000000.00,
      "overdue61To90": 800000.00,
      "overdueOver90": 200000.00,
      "totalInvoices": 120,
      "overdueInvoices": 35,
      "topOverdueInvoices": [
        {
          "invoiceId": 1,
          "invoiceNumber": "INV-2026-001",
          "customerName": "Customer A",
          "amount": 1500000.00,
          "dueDate": "2025-12-15",
          "daysOverdue": 31
        }
      ]
    },
    "payables": {
      "totalOutstanding": 8000000.00,
      "current": 5000000.00,
      "dueSoon": 2000000.00,
      "overdue": 1000000.00,
      "totalInvoices": 80,
      "dueSoonCount": 12,
      "upcomingPayments": [
        {
          "invoiceId": 1,
          "invoiceNumber": "BILL-2026-001",
          "vendorName": "Vendor A",
          "amount": 500000.00,
          "dueDate": "2026-01-20",
          "daysUntilDue": 5
        }
      ]
    }
  }
}
```

---

## Alerts Management

### Get Alerts

```http
GET /alerts
```

**Required Permissions:** `MOBILE_ALERTS_VIEW`

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| page | int | Page number (0-based) |
| size | int | Page size (default: 20) |
| sort | string | Sort field (default: `createdAt,desc`) |

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "alertType": "LOW_STOCK",
        "title": "Low Stock Alert",
        "message": "Product SKU-001 is running low (5 units remaining)",
        "priority": "HIGH",
        "entityType": "PRODUCT",
        "entityId": 123,
        "actionUrl": "/inventory/products/123",
        "data": "{\"currentStock\": 5, \"reorderPoint\": 10}",
        "read": false,
        "readAt": null,
        "expiresAt": "2026-02-15T09:30:00Z",
        "createdAt": "2026-01-15T09:30:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 15,
    "totalPages": 1
  }
}
```

### Get Unread Alerts

```http
GET /alerts/unread
```

**Required Permissions:** `MOBILE_ALERTS_VIEW`

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| page | int | Page number (0-based) |
| size | int | Page size (default: 20) |
| sort | string | Sort field (default: `createdAt,desc`) |

**Response:** Same structure as [Get Alerts](#get-alerts), filtered to unread only.

### Get Alerts by Type

```http
GET /alerts/type/{alertType}
```

**Required Permissions:** `MOBILE_ALERTS_VIEW`

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| alertType | string | Alert type enum value (see [Alert Types](#alert-types)) |

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| page | int | Page number (0-based) |
| size | int | Page size (default: 20) |
| sort | string | Sort field (default: `createdAt,desc`) |

**Response:** Same structure as [Get Alerts](#get-alerts), filtered by type.

### Get Unread Count

```http
GET /alerts/count
```

**Required Permissions:** `MOBILE_ALERTS_VIEW`

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "unreadCount": 5
  }
}
```

### Mark Alert as Read

```http
PUT /alerts/{id}/read
```

**Required Permissions:** `MOBILE_ALERTS_VIEW`

### Mark All Alerts as Read

```http
PUT /alerts/read-all
```

**Required Permissions:** `MOBILE_ALERTS_VIEW`

### Get Alert Settings

```http
GET /alerts/settings
```

**Required Permissions:** `MOBILE_ALERTS_VIEW`

**Response:** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "alertType": "LOW_STOCK",
      "pushEnabled": true,
      "inAppEnabled": true,
      "emailEnabled": false,
      "smsEnabled": false,
      "telegramEnabled": false,
      "thresholdValue": 10,
      "quietHoursStart": 22,
      "quietHoursEnd": 7
    }
  ]
}
```

### Update Alert Settings

```http
PUT /alerts/settings/{alertType}
```

**Required Permissions:** `MOBILE_ALERTS_MANAGE`

**Request Body:**
```json
{
  "pushEnabled": true,
  "inAppEnabled": true,
  "emailEnabled": true,
  "smsEnabled": false,
  "telegramEnabled": false,
  "thresholdValue": 5,
  "quietHoursStart": 22,
  "quietHoursEnd": 7
}
```

---

## Quick Actions

### Barcode Lookup

```http
GET /inventory/barcode/{barcode}
```

**Required Permissions:** `INVENTORY_PRODUCT_READ`, `POS_SALE_CREATE`, or `MOBILE_INVENTORY_VIEW`

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "productId": 123,
    "sku": "SKU-001",
    "barcode": "1234567890123",
    "name": "Product Name",
    "sellingPrice": 25000.00,
    "costPrice": 18000.00,
    "totalStock": 150,
    "category": "Electronics",
    "uom": "pcs",
    "trackInventory": true,
    "stockByLocation": [
      {
        "locationId": 1,
        "locationName": "Main Warehouse",
        "quantityOnHand": 100,
        "quantityReserved": 10,
        "quantityAvailable": 90
      }
    ]
  }
}
```

### Quick Stock Count

```http
POST /inventory/quick-count
```

**Required Permissions:** `MOBILE_QUICK_COUNT`, `INVENTORY_COUNT_CREATE`, or `INVENTORY_STOCK_ADJUST`

**Request Body:**
```json
{
  "productId": 123,
  "variantId": null,
  "locationId": 1,
  "countedQuantity": 95,
  "notes": "Shelf count"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| productId | long | Yes | Product ID |
| variantId | long | No | Product variant ID |
| locationId | long | Yes | Location ID |
| countedQuantity | BigDecimal | Yes | Counted quantity (must be positive) |
| notes | string | No | Optional notes |

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "productId": 123,
    "productName": "Product Name",
    "sku": "SKU-001",
    "locationId": 1,
    "systemQuantity": 100,
    "countedQuantity": 95,
    "variance": -5,
    "variancePercent": -5.0
  }
}
```

### Quick Sale

```http
POST /pos/quick-sale
```

**Required Permissions:** `MOBILE_QUICK_SALE` or `POS_SALE_CREATE`

**Request Body:**
```json
{
  "terminalId": 1,
  "customerId": 5,
  "customerName": "Walk-in Customer",
  "items": [
    {
      "productId": 123,
      "variantId": null,
      "quantity": 2.00,
      "unitPrice": 25000.00,
      "discountAmount": 0
    }
  ],
  "paymentType": "CASH",
  "tenderedAmount": 50000.00,
  "notes": "Walk-in customer"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| terminalId | long | Yes | POS terminal ID |
| customerId | long | No | Customer ID |
| customerName | string | No | Customer name (for walk-in customers) |
| items | array | Yes | At least one item required |
| items[].productId | long | Yes | Product ID |
| items[].variantId | long | No | Product variant ID |
| items[].quantity | BigDecimal | Yes | Quantity |
| items[].unitPrice | BigDecimal | No | Unit price override |
| items[].discountAmount | BigDecimal | No | Discount amount |
| paymentType | string | Yes | Payment type (e.g., `CASH`, `CARD`) |
| tenderedAmount | BigDecimal | No | Tendered amount |
| notes | string | No | Optional notes |

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "id": 456,
    "transactionNumber": "TXN-20260115-0001",
    "transactionType": "SALE",
    "status": "COMPLETED",
    "totalAmount": 50000.00,
    "paidAmount": 50000.00,
    "changeAmount": 0,
    "completedAt": "2026-01-15T10:45:00Z"
  }
}
```

### Search Products

```http
GET /inventory/search
```

**Required Permissions:** `INVENTORY_PRODUCT_READ` or `POS_SALE_CREATE`

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| query | string | Search by name, SKU, or barcode |
| page | int | Page number (0-based, default: 0) |
| size | int | Page size (default: 20) |

### Search Customers

```http
GET /customers/search
```

**Required Permissions:** `FINANCE_AR_CUSTOMER_VIEW` or `POS_SALE_CREATE`

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| query | string | Search by name, code, or phone |
| page | int | Page number (0-based, default: 0) |
| size | int | Page size (default: 20) |

---

## Offline Sync

### Get Products for Sync

```http
GET /sync/products
```

**Required Permissions:** `MOBILE_SYNC_ACCESS`, `INVENTORY_PRODUCT_READ`, or `POS_SALE_CREATE`

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| lastSyncAt | ISO DateTime | Last sync timestamp for incremental sync |

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "lastSyncAt": "2026-01-15T10:30:00Z",
    "syncVersion": "1.0",
    "fullSyncRequired": false,
    "products": [
      {
        "id": 123,
        "sku": "SKU-001",
        "barcode": "1234567890123",
        "name": "Product Name",
        "categoryId": 5,
        "categoryName": "Electronics",
        "sellingPrice": 25000.00,
        "costPrice": 18000.00,
        "unitOfMeasure": "pcs",
        "trackInventory": true,
        "active": true,
        "imageUrl": "https://example.com/images/product-123.jpg",
        "updatedAt": "2026-01-15T09:00:00Z"
      }
    ],
    "prices": [
      {
        "productId": 123,
        "priceListId": 1,
        "priceListName": "Retail",
        "unitPrice": 25000.00,
        "minQuantity": 1.00,
        "effectiveFrom": "2026-01-01T00:00:00Z",
        "effectiveTo": null,
        "updatedAt": "2026-01-01T00:00:00Z"
      }
    ]
  }
}
```

### Get Customers for Sync

```http
GET /sync/customers
```

**Required Permissions:** `MOBILE_SYNC_ACCESS`, `FINANCE_AR_CUSTOMER_VIEW`, or `POS_SALE_CREATE`

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| lastSyncAt | ISO DateTime | Last sync timestamp for incremental sync |

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "lastSyncAt": "2026-01-15T10:30:00Z",
    "syncVersion": "1.0",
    "fullSyncRequired": false,
    "customers": [
      {
        "id": 1,
        "code": "CUST-001",
        "name": "Customer Name",
        "phone": "+998901234567",
        "email": "customer@example.com",
        "priceListId": 2,
        "creditLimit": 5000000.00,
        "currentBalance": 1500000.00,
        "active": true,
        "updatedAt": "2026-01-15T08:00:00Z"
      }
    ]
  }
}
```

### Get Categories for Sync

```http
GET /sync/categories
```

**Required Permissions:** `MOBILE_SYNC_ACCESS`, `INVENTORY_PRODUCT_READ`, or `POS_SALE_CREATE`

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "lastSyncAt": "2026-01-15T10:30:00Z",
    "syncVersion": "1.0",
    "categories": [
      {
        "id": 1,
        "name": "Electronics",
        "parentId": null,
        "sortOrder": 1,
        "active": true,
        "updatedAt": "2026-01-10T08:00:00Z"
      }
    ]
  }
}
```

### Check Last Updated

```http
GET /sync/last-updated
```

**Required Permissions:** `MOBILE_SYNC_ACCESS`

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "lastUpdated": "2026-01-15T10:30:00Z",
    "syncRequired": true
  }
}
```

---

## Alert Types

| Alert Type | Description |
|------------|-------------|
| LOW_STOCK | Product stock below reorder point |
| OUT_OF_STOCK | Product completely out of stock |
| EXPIRING_INVENTORY | Stock batch expiring soon |
| LARGE_TRANSACTION | Large transaction amount detected |
| PAYMENT_RECEIVED | Payment received |
| PAYMENT_OVERDUE | Payment past due |
| ORDER_PLACED | New order placed |
| ORDER_CANCELLED | Order cancelled |
| SYSTEM_ALERT | System notification |
| DAILY_SUMMARY | Daily business summary |
| WEEKLY_SUMMARY | Weekly business summary |
| APPROVAL_REQUIRED | Action requires approval |
| CUSTOM | Custom notification |

## Alert Priorities

| Priority | Description |
|----------|-------------|
| LOW | Low priority alert |
| NORMAL | Normal priority alert |
| HIGH | High priority, requires attention |
| URGENT | Urgent, requires immediate attention |

## Device Platforms

| Platform | Description |
|----------|-------------|
| ANDROID | Android mobile device |
| IOS | iOS mobile device |
| WEB | Web browser |

---

## Error Responses

### 400 Bad Request
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid request parameters",
    "details": ["field: must not be null"]
  }
}
```

### 401 Unauthorized
```json
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Invalid or expired token"
  }
}
```

### 403 Forbidden
```json
{
  "success": false,
  "error": {
    "code": "ACCESS_DENIED",
    "message": "Insufficient permissions"
  }
}
```

### 404 Not Found
```json
{
  "success": false,
  "error": {
    "code": "NOT_FOUND",
    "message": "Resource not found"
  }
}
```

---

## Rate Limiting

Mobile API endpoints are rate limited to:
- 100 requests per minute for authenticated users
- 10 requests per minute for unauthenticated endpoints (login)

Rate limit headers are included in responses:
- `X-RateLimit-Limit`: Maximum requests allowed
- `X-RateLimit-Remaining`: Remaining requests in window
- `X-RateLimit-Reset`: Unix timestamp when limit resets
