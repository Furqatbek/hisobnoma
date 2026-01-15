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
    "averageTransactionValue": 24137.93
  }
}
```

### Get Revenue Chart Data

```http
GET /dashboard/revenue/chart
```

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| period | string | `hourly`, `daily`, or `monthly` |

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "dailyRevenue": [
      {
        "label": "2026-01-01",
        "value": 1200000.00,
        "count": 35
      },
      {
        "label": "2026-01-02",
        "value": 1350000.00,
        "count": 42
      }
    ]
  }
}
```

### Get Inventory Summary

```http
GET /dashboard/inventory
```

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
    "expiringCount": 8
  }
}
```

### Get Financial Summary

```http
GET /dashboard/financial
```

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "totalBankBalance": 50000000.00,
    "totalCashBalance": 5000000.00,
    "arOutstanding": 15000000.00,
    "apOutstanding": 8000000.00,
    "netCashPosition": 57000000.00
  }
}
```

---

## Alerts Management

### Get Alerts

```http
GET /alerts
```

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| unreadOnly | boolean | Filter to unread alerts only |
| page | int | Page number (0-based) |
| size | int | Page size (default: 20) |

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
        "isRead": false,
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

### Get Unread Count

```http
GET /alerts/unread-count
```

**Response:** `200 OK`
```json
{
  "success": true,
  "data": 5
}
```

### Mark Alert as Read

```http
PUT /alerts/{id}/read
```

### Mark All Alerts as Read

```http
PUT /alerts/read-all
```

### Get Alert Preferences

```http
GET /alerts/preferences
```

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
      "thresholdValue": 10
    }
  ]
}
```

### Update Alert Preference

```http
PUT /alerts/preferences/{alertType}
```

**Request Body:**
```json
{
  "pushEnabled": true,
  "inAppEnabled": true,
  "emailEnabled": true,
  "smsEnabled": false,
  "thresholdValue": 5
}
```

---

## Quick Actions

### Barcode Lookup

```http
GET /barcode/{barcode}
```

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
POST /quick-count
```

**Request Body:**
```json
{
  "productId": 123,
  "locationId": 1,
  "countedQuantity": 95,
  "notes": "Shelf count"
}
```

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
POST /quick-sale
```

**Request Body:**
```json
{
  "terminalId": 1,
  "customerId": 5,
  "items": [
    {
      "productId": 123,
      "variantId": null,
      "quantity": 2,
      "unitPrice": 25000.00,
      "discountAmount": 0
    }
  ],
  "paymentType": "CASH",
  "tenderedAmount": 50000.00,
  "notes": "Walk-in customer"
}
```

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
GET /products/search
```

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| query | string | Search by name, SKU, or barcode |
| page | int | Page number (0-based) |
| size | int | Page size (default: 20) |

### Search Customers

```http
GET /customers/search
```

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| query | string | Search by name, code, or phone |
| page | int | Page number (0-based) |
| size | int | Page size (default: 20) |

---

## Offline Sync

### Get Products for Sync

```http
GET /sync/products
```

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
        "updatedAt": "2026-01-15T09:00:00Z"
      }
    ]
  }
}
```

### Get Customers for Sync

```http
GET /sync/customers
```

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

**Response:** `200 OK`
```json
{
  "success": true,
  "data": "2026-01-15T10:30:00Z"
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
| DAILY_SUMMARY | Daily business summary |
| PRICE_CHANGE | Product price changed |
| NEW_ORDER | New order received |
| PAYMENT_RECEIVED | Payment received |
| PAYMENT_DUE | Payment coming due |
| PAYMENT_OVERDUE | Payment past due |
| SYSTEM | System notification |

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
