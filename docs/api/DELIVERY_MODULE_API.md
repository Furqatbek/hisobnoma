# Delivery Module API Documentation

## Overview

The Delivery Module provides management of delivery regions and villages (mahallas) for POS delivery address selection. Regions contain villages, allowing quick hierarchical address selection without manual typing.

## Authentication

All endpoints require JWT Bearer token authentication and appropriate RBAC permissions:
- `DELIVERY_REGION_READ` - View delivery regions
- `DELIVERY_REGION_CREATE` - Create new delivery regions
- `DELIVERY_REGION_UPDATE` - Update delivery regions
- `DELIVERY_REGION_DELETE` - Delete delivery regions
- `DELIVERY_VILLAGE_READ` - View delivery villages
- `DELIVERY_VILLAGE_CREATE` - Create new delivery villages
- `DELIVERY_VILLAGE_UPDATE` - Update delivery villages
- `DELIVERY_VILLAGE_DELETE` - Delete delivery villages

## Base URL

```
/api/v1/delivery
```

---

## Delivery Regions

### List Regions (Paginated)

```http
GET /regions
```

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| page | int | Page number (0-based) |
| size | int | Page size (default: 20) |
| sort | string | Sort field and direction (default: `sortOrder,asc`) |
| search | string | Search by name or code |

**Permission:** `DELIVERY_REGION_READ`

**Response:** `200 OK`
```json
{
  "content": [
    {
      "id": 1,
      "name": "Toshkent shahri",
      "code": "TASH",
      "description": "Toshkent shahri hududlari",
      "active": true,
      "sortOrder": 0,
      "villageCount": 5,
      "createdAt": "2026-02-21T10:00:00Z",
      "updatedAt": "2026-02-21T10:00:00Z"
    }
  ],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

### Get Active Regions

```http
GET /regions/active
```

Returns all active regions sorted by sortOrder. Used for dropdown population.

**Permission:** `DELIVERY_REGION_READ`

**Response:** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "Toshkent shahri",
      "code": "TASH",
      "description": "Toshkent shahri hududlari",
      "active": true,
      "sortOrder": 0,
      "villageCount": 5
    }
  ]
}
```

---

### Get Region by ID

```http
GET /regions/{id}
```

**Permission:** `DELIVERY_REGION_READ`

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "Toshkent shahri",
    "code": "TASH",
    "description": "Toshkent shahri hududlari",
    "active": true,
    "sortOrder": 0,
    "villageCount": 5,
    "createdAt": "2026-02-21T10:00:00Z",
    "updatedAt": "2026-02-21T10:00:00Z"
  }
}
```

---

### Create Region

```http
POST /regions
```

**Permission:** `DELIVERY_REGION_CREATE`

**Request:**
```json
{
  "name": "Toshkent shahri",
  "code": "TASH",
  "description": "Toshkent shahri hududlari",
  "active": true,
  "sortOrder": 0
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| name | string | Yes | Region name (max 100 chars) |
| code | string | No | Unique code per tenant (max 50 chars) |
| description | string | No | Description (max 500 chars) |
| active | boolean | No | Active status (default: true) |
| sortOrder | int | No | Display order (default: 0) |

**Response:** `201 Created`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "Toshkent shahri",
    "code": "TASH",
    "description": "Toshkent shahri hududlari",
    "active": true,
    "sortOrder": 0,
    "villageCount": 0
  }
}
```

**Error:** `409 Conflict` if code already exists for this tenant.

---

### Update Region

```http
PUT /regions/{id}
```

**Permission:** `DELIVERY_REGION_UPDATE`

**Request:**
```json
{
  "name": "Toshkent shahri (yangilangan)",
  "code": "TASH",
  "description": "Yangilangan tavsif",
  "active": true,
  "sortOrder": 1
}
```

**Response:** `200 OK`

---

### Delete Region

```http
DELETE /regions/{id}
```

**Permission:** `DELIVERY_REGION_DELETE`

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Region deleted successfully"
}
```

**Error:** `400 Bad Request` if region contains villages. Villages must be deleted first.

---

## Delivery Villages

### List Villages (Paginated)

```http
GET /villages
```

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| page | int | Page number (0-based) |
| size | int | Page size (default: 20) |
| sort | string | Sort field and direction (default: `sortOrder,asc`) |
| search | string | Search by name or code |

**Permission:** `DELIVERY_VILLAGE_READ`

**Response:** `200 OK`
```json
{
  "content": [
    {
      "id": 1,
      "regionId": 1,
      "regionName": "Toshkent shahri",
      "name": "Yunusobod tumani",
      "code": "YUN",
      "description": "Yunusobod tumani mahallalari",
      "active": true,
      "sortOrder": 0,
      "createdAt": "2026-02-21T10:00:00Z",
      "updatedAt": "2026-02-21T10:00:00Z"
    }
  ],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

### Get Active Villages

```http
GET /villages/active
```

Returns all active villages sorted by sortOrder. Used for dropdown population.

**Permission:** `DELIVERY_VILLAGE_READ`

**Response:** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "regionId": 1,
      "regionName": "Toshkent shahri",
      "name": "Yunusobod tumani",
      "code": "YUN",
      "active": true,
      "sortOrder": 0
    }
  ]
}
```

---

### Get Villages by Region

```http
GET /villages/region/{regionId}
```

Returns all active villages for a specific region. Used for cascading dropdown in POS.

**Permission:** `DELIVERY_VILLAGE_READ`

**Response:** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "regionId": 1,
      "regionName": "Toshkent shahri",
      "name": "Yunusobod tumani",
      "code": "YUN",
      "active": true,
      "sortOrder": 0
    }
  ]
}
```

---

### Get Village by ID

```http
GET /villages/{id}
```

**Permission:** `DELIVERY_VILLAGE_READ`

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "regionId": 1,
    "regionName": "Toshkent shahri",
    "name": "Yunusobod tumani",
    "code": "YUN",
    "description": "Yunusobod tumani mahallalari",
    "active": true,
    "sortOrder": 0,
    "createdAt": "2026-02-21T10:00:00Z",
    "updatedAt": "2026-02-21T10:00:00Z"
  }
}
```

---

### Create Village

```http
POST /villages
```

**Permission:** `DELIVERY_VILLAGE_CREATE`

**Request:**
```json
{
  "regionId": 1,
  "name": "Yunusobod tumani",
  "code": "YUN",
  "description": "Yunusobod tumani mahallalari",
  "active": true,
  "sortOrder": 0
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| regionId | long | Yes | Parent region ID |
| name | string | Yes | Village name (max 100 chars) |
| code | string | No | Unique code per tenant (max 50 chars) |
| description | string | No | Description (max 500 chars) |
| active | boolean | No | Active status (default: true) |
| sortOrder | int | No | Display order (default: 0) |

**Response:** `201 Created`

**Error:** `404 Not Found` if region does not exist. `409 Conflict` if code already exists.

---

### Update Village

```http
PUT /villages/{id}
```

**Permission:** `DELIVERY_VILLAGE_UPDATE`

**Request:**
```json
{
  "regionId": 1,
  "name": "Yunusobod tumani (yangilangan)",
  "code": "YUN",
  "description": "Yangilangan tavsif",
  "active": true,
  "sortOrder": 1
}
```

**Response:** `200 OK`

---

### Delete Village

```http
DELETE /villages/{id}
```

**Permission:** `DELIVERY_VILLAGE_DELETE`

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Village deleted successfully"
}
```

---

## POS Transaction Integration

When creating a POS transaction, delivery region and village can be optionally specified:

```http
POST /api/v1/pos/transactions
```

**Additional Fields in CreateTransactionRequest:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| deliveryRegionId | long | No | Delivery region ID |
| deliveryVillageId | long | No | Delivery village ID |

The backend resolves region and village names automatically and stores them denormalized on the transaction record.

**Transaction Response** includes:
- `deliveryRegionId` - Region ID (if set)
- `deliveryRegionName` - Region name (resolved)
- `deliveryVillageId` - Village ID (if set)
- `deliveryVillageName` - Village name (resolved)

---

## Role Permissions

| Role | Permissions |
|------|-------------|
| SUPER_ADMIN | All delivery permissions |
| ADMIN | All delivery permissions |
| STORE_MANAGER | Read, Create, Update (no Delete) |
| CASHIER | Read only (for POS address selection) |
| VIEWER | Read only |

---

## Database Schema

### delivery_regions
| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| tenant_id | BIGINT | Tenant isolation |
| name | VARCHAR(100) | Region name |
| code | VARCHAR(50) | Unique code per tenant |
| description | VARCHAR(500) | Description |
| active | BOOLEAN | Active status |
| sort_order | INT | Display order |
| created_at | TIMESTAMP | Creation time |
| updated_at | TIMESTAMP | Last update time |
| created_by | BIGINT | Creator user ID |
| updated_by | BIGINT | Updater user ID |
| version | BIGINT | Optimistic locking |

### delivery_villages
| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| tenant_id | BIGINT | Tenant isolation |
| region_id | BIGINT | FK to delivery_regions |
| name | VARCHAR(100) | Village name |
| code | VARCHAR(50) | Unique code per tenant |
| description | VARCHAR(500) | Description |
| active | BOOLEAN | Active status |
| sort_order | INT | Display order |
| created_at | TIMESTAMP | Creation time |
| updated_at | TIMESTAMP | Last update time |
| created_by | BIGINT | Creator user ID |
| updated_by | BIGINT | Updater user ID |
| version | BIGINT | Optimistic locking |
