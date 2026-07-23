# Hisobnoma Platform API Documentation

> **Coverage note.** This file is the platform-wide contract but does not duplicate every module.
> Dedicated, more detailed module docs live in [`docs/api/`](api/):
> AP (`AP_MODULE_API.md`), AR (`AR_MODULE_API.md`), Banking & Tax (`BANKING_TAX_MODULE_API.md`),
> Delivery (`DELIVERY_MODULE_API.md`), POS (`POS_MODULE_API.md`), Reports (`REPORTS_MODULE_API.md`),
> the staff mobile app (`MOBILE_MODULE_API.md`, incl. quick-sale idempotency and shifts), APNs push
> (`MOBILE_PUSH_API.md`), and the customer shop (`MOBILE_SHOP_API.md`).
> Not yet documented anywhere: the HR (`/api/v1/hr/**`), SMS (`/api/v1/sms`), Telegram
> (`/api/v1/telegram`), and expense (`/api/v1/web/expenses`) endpoints — consult the controllers.

## Base URL
```
http://localhost:8080/api/v1
```

## Authentication
All endpoints except `/auth/login`, `/auth/forgot-password`, and `/auth/reset-password` require a valid JWT token in the Authorization header:
```
Authorization: Bearer <access_token>
```

## Response Format
All API responses follow a consistent structure:
```json
{
  "success": true,
  "message": "Optional message",
  "data": { },
  "timestamp": "2026-01-03T12:00:00Z"
}
```

Error responses:
```json
{
  "success": false,
  "message": "Error description",
  "error": {
    "code": "ERROR_CODE",
    "field": "fieldName",
    "fieldErrors": []
  },
  "timestamp": "2026-01-03T12:00:00Z"
}
```

---

## Authentication APIs

### POST /auth/login
Login with username/phone and password.

**Request:**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "tokenType": "Bearer",
    "expiresIn": 3600000,
    "user": {
      "id": 1,
      "username": "admin",
      "phone": "+998901234567",
      "firstName": "Admin",
      "lastName": "User",
      "fullName": "Admin User",
      "tenantId": 1,
      "roles": ["SUPER_ADMIN"],
      "permissions": ["ADMIN_USER_MANAGE", "ADMIN_ROLE_MANAGE", "ADMIN_PERMISSION_MANAGE"]
    }
  },
  "timestamp": "2026-01-03T12:00:00Z"
}
```

---

### POST /auth/refresh
Refresh access token using refresh token.

**Request:**
```json
{
  "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Token refreshed",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "new-refresh-token-uuid",
    "tokenType": "Bearer",
    "expiresIn": 3600000,
    "user": {
      "id": 1,
      "username": "admin",
      "fullName": "Admin User",
      "roles": ["SUPER_ADMIN"],
      "permissions": ["ADMIN_USER_MANAGE", "ADMIN_ROLE_MANAGE"]
    }
  },
  "timestamp": "2026-01-03T12:00:00Z"
}
```

---

### POST /auth/logout
Logout and invalidate tokens. Requires authentication.

**Response:**
```json
{
  "success": true,
  "message": "Logged out successfully",
  "timestamp": "2026-01-03T12:00:00Z"
}
```

---

### GET /auth/me
Get current authenticated user's profile. Requires authentication.

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "username": "admin",
    "phone": "+998901234567",
    "firstName": "Admin",
    "lastName": "User",
    "fullName": "Admin User",
    "tenantId": 1,
    "roles": ["SUPER_ADMIN"],
    "permissions": ["ADMIN_USER_MANAGE", "ADMIN_ROLE_MANAGE"]
  },
  "timestamp": "2026-01-03T12:00:00Z"
}
```

---

### PUT /auth/change-password
Change the current user's password. Requires authentication.

**Request:**
```json
{
  "currentPassword": "admin123",
  "newPassword": "NewPassword456!",
  "confirmPassword": "NewPassword456!"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Password changed successfully",
  "timestamp": "2026-01-03T12:00:00Z"
}
```

---

### POST /auth/forgot-password
Request a password reset token.

**Request:**
```json
{
  "identifier": "admin"
}
```

**Response:**
```json
{
  "success": true,
  "message": "If the account exists, a reset link will be sent",
  "data": "reset-token-uuid-here",
  "timestamp": "2026-01-03T12:00:00Z"
}
```

---

### POST /auth/reset-password
Reset password using the token from forgot-password.

**Request:**
```json
{
  "token": "reset-token-uuid-here",
  "newPassword": "NewPassword789!",
  "confirmPassword": "NewPassword789!"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Password reset successfully",
  "timestamp": "2026-01-03T12:00:00Z"
}
```

---

## User Management APIs

All user management endpoints require `ADMIN_USER_MANAGE` permission.

### GET /users
List users with pagination and search.

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| search | string | - | Search by username, first name, or last name |
| page | int | 0 | Page number (0-based) |
| size | int | 20 | Page size |
| sort | string | createdAt,desc | Sort field and direction |

**Example:** `GET /users?search=john&page=0&size=10`

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 2,
        "username": "john_doe",
        "phone": "+998901234568",
        "firstName": "John",
        "lastName": "Doe",
        "fullName": "John Doe",
        "enabled": true,
        "locked": false,
        "phoneVerified": false,
        "lastLoginAt": "2026-01-02T10:30:00Z",
        "tenantId": 1,
        "roles": ["MANAGER"],
        "createdAt": "2026-01-01T08:00:00Z",
        "updatedAt": "2026-01-02T10:30:00Z"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  },
  "timestamp": "2026-01-03T12:00:00Z"
}
```

---

### GET /users/{id}
Get user by ID.

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 2,
    "username": "john_doe",
    "phone": "+998901234568",
    "firstName": "John",
    "lastName": "Doe",
    "fullName": "John Doe",
    "enabled": true,
    "locked": false,
    "phoneVerified": false,
    "lastLoginAt": "2026-01-02T10:30:00Z",
    "tenantId": 1,
    "roles": ["MANAGER"],
    "createdAt": "2026-01-01T08:00:00Z",
    "updatedAt": "2026-01-02T10:30:00Z"
  },
  "timestamp": "2026-01-03T12:00:00Z"
}
```

---

### POST /users
Create a new user.

**Request:**
```json
{
  "username": "jane_doe",
  "phone": "+998901234569",
  "password": "SecurePass123!",
  "firstName": "Jane",
  "lastName": "Doe",
  "roleCodes": ["CASHIER"]
}
```

**Response:** (HTTP 201 Created)
```json
{
  "success": true,
  "message": "User created successfully",
  "data": {
    "id": 3,
    "username": "jane_doe",
    "phone": "+998901234569",
    "firstName": "Jane",
    "lastName": "Doe",
    "fullName": "Jane Doe",
    "enabled": true,
    "locked": false,
    "phoneVerified": false,
    "lastLoginAt": null,
    "tenantId": 1,
    "roles": ["CASHIER"],
    "createdAt": "2026-01-03T12:00:00Z",
    "updatedAt": "2026-01-03T12:00:00Z"
  },
  "timestamp": "2026-01-03T12:00:00Z"
}
```

---

### PUT /users/{id}
Update user details.

**Request:**
```json
{
  "phone": "+998901234570",
  "firstName": "Jane",
  "lastName": "Smith",
  "enabled": true
}
```

**Response:**
```json
{
  "success": true,
  "message": "User updated successfully",
  "data": {
    "id": 3,
    "username": "jane_doe",
    "phone": "+998901234570",
    "firstName": "Jane",
    "lastName": "Smith",
    "fullName": "Jane Smith",
    "enabled": true,
    "locked": false,
    "tenantId": 1,
    "roles": ["CASHIER"]
  },
  "timestamp": "2026-01-03T12:00:00Z"
}
```

---

### DELETE /users/{id}
Delete a user.

**Response:**
```json
{
  "success": true,
  "message": "User deleted successfully",
  "timestamp": "2026-01-03T12:00:00Z"
}
```

---

### PUT /users/{id}/roles
Assign roles to a user.

**Request:**
```json
["MANAGER", "CASHIER"]
```

**Response:**
```json
{
  "success": true,
  "message": "Roles updated successfully",
  "data": {
    "id": 3,
    "username": "jane_doe",
    "roles": ["MANAGER", "CASHIER"]
  },
  "timestamp": "2026-01-03T12:00:00Z"
}
```

---

### PUT /users/{id}/lock
Lock or unlock a user account.

**Request:**
```json
{
  "locked": true
}
```

**Response:**
```json
{
  "success": true,
  "message": "User locked successfully",
  "data": {
    "id": 3,
    "username": "jane_doe",
    "locked": true
  },
  "timestamp": "2026-01-03T12:00:00Z"
}
```

---

### PUT /users/{id}/reset-password
Admin reset of user password.

**Request:**
```json
{
  "password": "NewUserPassword123!"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Password reset successfully",
  "timestamp": "2026-01-03T12:00:00Z"
}
```

---

## Role Management APIs

All role management endpoints require `ADMIN_ROLE_MANAGE` permission (except permissions assignment which requires `ADMIN_PERMISSION_MANAGE`).

### GET /roles
List roles with pagination.

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | int | 0 | Page number (0-based) |
| size | int | 20 | Page size |
| sort | string | name,asc | Sort field and direction |

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "name": "Super Admin",
        "code": "SUPER_ADMIN",
        "description": "Full system access",
        "systemRole": true,
        "tenantId": null,
        "permissions": ["ADMIN_USER_MANAGE", "ADMIN_ROLE_MANAGE", "ADMIN_PERMISSION_MANAGE"],
        "createdAt": "2026-01-01T00:00:00Z",
        "updatedAt": "2026-01-01T00:00:00Z"
      },
      {
        "id": 2,
        "name": "Manager",
        "code": "MANAGER",
        "description": "Store manager role",
        "systemRole": true,
        "tenantId": null,
        "permissions": ["INVENTORY_READ", "INVENTORY_UPDATE", "POS_VIEW"],
        "createdAt": "2026-01-01T00:00:00Z",
        "updatedAt": "2026-01-01T00:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 2,
    "totalPages": 1
  },
  "timestamp": "2026-01-03T12:00:00Z"
}
```

---

### GET /roles/system
List system (built-in) roles only.

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "Super Admin",
      "code": "SUPER_ADMIN",
      "description": "Full system access",
      "systemRole": true,
      "permissions": ["ADMIN_USER_MANAGE", "ADMIN_ROLE_MANAGE"]
    },
    {
      "id": 2,
      "name": "Manager",
      "code": "MANAGER",
      "description": "Store manager role",
      "systemRole": true,
      "permissions": ["INVENTORY_READ", "INVENTORY_UPDATE"]
    }
  ],
  "timestamp": "2026-01-03T12:00:00Z"
}
```

---

### GET /roles/{id}
Get role by ID.

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "Super Admin",
    "code": "SUPER_ADMIN",
    "description": "Full system access",
    "systemRole": true,
    "tenantId": null,
    "permissions": ["ADMIN_USER_MANAGE", "ADMIN_ROLE_MANAGE", "ADMIN_PERMISSION_MANAGE"],
    "createdAt": "2026-01-01T00:00:00Z",
    "updatedAt": "2026-01-01T00:00:00Z"
  },
  "timestamp": "2026-01-03T12:00:00Z"
}
```

---

### POST /roles
Create a new custom role.

**Request:**
```json
{
  "name": "Warehouse Staff",
  "code": "WAREHOUSE_STAFF",
  "description": "Warehouse inventory management",
  "permissionCodes": ["INVENTORY_READ", "INVENTORY_UPDATE"]
}
```

**Response:** (HTTP 201 Created)
```json
{
  "success": true,
  "message": "Role created successfully",
  "data": {
    "id": 5,
    "name": "Warehouse Staff",
    "code": "WAREHOUSE_STAFF",
    "description": "Warehouse inventory management",
    "systemRole": false,
    "tenantId": 1,
    "permissions": ["INVENTORY_READ", "INVENTORY_UPDATE"],
    "createdAt": "2026-01-03T12:00:00Z",
    "updatedAt": "2026-01-03T12:00:00Z"
  },
  "timestamp": "2026-01-03T12:00:00Z"
}
```

---

### PUT /roles/{id}
Update a custom role (system roles cannot be modified).

**Request:**
```json
{
  "name": "Warehouse Manager",
  "code": "WAREHOUSE_STAFF",
  "description": "Warehouse operations management",
  "permissionCodes": ["INVENTORY_READ", "INVENTORY_UPDATE", "INVENTORY_CREATE"]
}
```

**Response:**
```json
{
  "success": true,
  "message": "Role updated successfully",
  "data": {
    "id": 5,
    "name": "Warehouse Manager",
    "code": "WAREHOUSE_STAFF",
    "description": "Warehouse operations management",
    "systemRole": false,
    "permissions": ["INVENTORY_READ", "INVENTORY_UPDATE", "INVENTORY_CREATE"]
  },
  "timestamp": "2026-01-03T12:00:00Z"
}
```

---

### DELETE /roles/{id}
Delete a custom role (system roles cannot be deleted).

**Response:**
```json
{
  "success": true,
  "message": "Role deleted successfully",
  "timestamp": "2026-01-03T12:00:00Z"
}
```

---

### PUT /roles/{id}/permissions
Assign permissions to a role. Requires `ADMIN_PERMISSION_MANAGE` permission.

**Request:**
```json
["INVENTORY_READ", "INVENTORY_CREATE", "INVENTORY_UPDATE", "INVENTORY_DELETE"]
```

**Response:**
```json
{
  "success": true,
  "message": "Permissions updated successfully",
  "data": {
    "id": 5,
    "name": "Warehouse Staff",
    "code": "WAREHOUSE_STAFF",
    "permissions": ["INVENTORY_READ", "INVENTORY_CREATE", "INVENTORY_UPDATE", "INVENTORY_DELETE"]
  },
  "timestamp": "2026-01-03T12:00:00Z"
}
```

---

### GET /roles/permissions
List all available permissions in the system.

**Response:**
```json
{
  "success": true,
  "data": [
    "ADMIN_PERMISSION_MANAGE",
    "ADMIN_ROLE_MANAGE",
    "ADMIN_USER_MANAGE",
    "FINANCE_APPROVE",
    "FINANCE_CREATE",
    "FINANCE_DELETE",
    "FINANCE_EXPORT",
    "FINANCE_READ",
    "FINANCE_UPDATE",
    "INVENTORY_CREATE",
    "INVENTORY_DELETE",
    "INVENTORY_EXPORT",
    "INVENTORY_READ",
    "INVENTORY_UPDATE",
    "POS_CREATE",
    "POS_DELETE",
    "POS_READ",
    "POS_UPDATE",
    "POS_VIEW",
    "REPORTS_EXPORT",
    "REPORTS_VIEW"
  ],
  "timestamp": "2026-01-03T12:00:00Z"
}
```

---

## Error Codes

| HTTP Status | Error Code | Description |
|-------------|------------|-------------|
| 400 | VALIDATION_ERROR | Request validation failed |
| 401 | UNAUTHORIZED | Authentication required |
| 401 | INVALID_CREDENTIALS | Invalid username or password |
| 401 | TOKEN_EXPIRED | JWT token has expired |
| 403 | FORBIDDEN | Insufficient permissions |
| 403 | ACCOUNT_LOCKED | User account is locked |
| 404 | NOT_FOUND | Resource not found |
| 409 | DUPLICATE_RESOURCE | Resource already exists |
| 422 | BUSINESS_ERROR | Business rule violation |
| 500 | INTERNAL_ERROR | Internal server error |

---

## Pagination

Paginated responses include:
```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5,
  "first": true,
  "last": false
}
```

**Query Parameters:**
- `page` - Zero-based page number (default: 0)
- `size` - Page size (default: 20, max: 100)
- `sort` - Sort field and direction (e.g., `createdAt,desc`)

---

---

## Inventory Module - Product Catalog (Block 3)

All inventory endpoints require `INVENTORY_PRODUCT_READ`, `INVENTORY_PRODUCT_CREATE`, `INVENTORY_PRODUCT_UPDATE`, or `INVENTORY_PRODUCT_DELETE` permissions.

---

## Category APIs

### GET /inventory/categories
List all categories.

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "code": "ELECTRONICS",
      "name": "Electronics",
      "description": "Electronic devices and accessories",
      "imageUrl": null,
      "parentId": null,
      "parentName": null,
      "sortOrder": 0,
      "active": true,
      "level": 0,
      "path": "/1",
      "children": null
    }
  ]
}
```

---

### GET /inventory/categories/tree
Get category tree with nested children.

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "code": "ELECTRONICS",
      "name": "Electronics",
      "children": [
        {
          "id": 6,
          "code": "PHONES",
          "name": "Phones",
          "parentId": 1,
          "children": []
        }
      ]
    }
  ]
}
```

---

### GET /inventory/categories/roots
Get root-level categories only.

---

### GET /inventory/categories/{id}
Get category by ID.

---

### GET /inventory/categories/{id}/children
Get child categories of a specific category.

---

### GET /inventory/categories/search
Search categories.

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| q | string | Yes | Search query |
| page | int | No | Page number (0-based) |
| size | int | No | Page size |

---

### POST /inventory/categories
Create a new category.

**Request:**
```json
{
  "code": "PHONES",
  "name": "Phones",
  "description": "Mobile phones and accessories",
  "imageUrl": "https://example.com/phones.jpg",
  "parentId": 1,
  "sortOrder": 0,
  "active": true
}
```

**Response:** (HTTP 201 Created)
```json
{
  "success": true,
  "data": {
    "id": 6,
    "code": "PHONES",
    "name": "Phones",
    "description": "Mobile phones and accessories",
    "parentId": 1,
    "parentName": "Electronics",
    "level": 1,
    "path": "/1/6",
    "active": true
  }
}
```

---

### PUT /inventory/categories/{id}
Update a category.

**Request:**
```json
{
  "name": "Mobile Phones",
  "description": "Smartphones and accessories",
  "sortOrder": 1,
  "active": true
}
```

---

### DELETE /inventory/categories/{id}
Delete a category (only if no children or products are associated).

---

## Brand APIs

### GET /inventory/brands
List all brands.

---

### GET /inventory/brands/paginated
List brands with pagination.

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | int | 0 | Page number |
| size | int | 20 | Page size |
| sort | string | name,asc | Sort field and direction |

---

### GET /inventory/brands/active
List only active brands.

---

### GET /inventory/brands/{id}
Get brand by ID.

---

### GET /inventory/brands/search
Search brands.

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| q | string | Yes | Search query |

---

### POST /inventory/brands
Create a new brand.

**Request:**
```json
{
  "code": "APPLE",
  "name": "Apple",
  "description": "Apple Inc. products",
  "logoUrl": "https://example.com/apple-logo.png",
  "website": "https://apple.com",
  "sortOrder": 0,
  "active": true
}
```

**Response:** (HTTP 201 Created)
```json
{
  "success": true,
  "data": {
    "id": 1,
    "code": "APPLE",
    "name": "Apple",
    "description": "Apple Inc. products",
    "logoUrl": "https://example.com/apple-logo.png",
    "website": "https://apple.com",
    "sortOrder": 0,
    "active": true
  }
}
```

---

### PUT /inventory/brands/{id}
Update a brand.

---

### DELETE /inventory/brands/{id}
Delete a brand.

---

## Unit of Measure APIs

### GET /inventory/uom
List all units of measure.

---

### GET /inventory/uom/paginated
List UOMs with pagination.

---

### GET /inventory/uom/active
List only active UOMs.

---

### GET /inventory/uom/base
List only base units (not derived units).

---

### GET /inventory/uom/{id}
Get UOM by ID.

---

### GET /inventory/uom/code/{code}
Get UOM by code.

---

### GET /inventory/uom/search
Search UOMs.

---

### POST /inventory/uom
Create a new unit of measure.

**Request:**
```json
{
  "code": "CASE",
  "name": "Case",
  "symbol": "cs",
  "description": "Case of 24 units",
  "baseUomId": 1,
  "conversionFactor": 24,
  "active": true,
  "isBaseUnit": false
}
```

---

### PUT /inventory/uom/{id}
Update a unit of measure.

---

### DELETE /inventory/uom/{id}
Delete a unit of measure (only if not used as base for other units).

---

### GET /inventory/uom/convert
Convert quantity between units.

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| quantity | decimal | Yes | Quantity to convert |
| fromUomId | long | Yes | Source UOM ID |
| toUomId | long | Yes | Target UOM ID |

**Response:**
```json
{
  "result": 24.000000
}
```

---

## Product APIs

### GET /inventory/products
List products with pagination.

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | int | 0 | Page number |
| size | int | 20 | Page size |
| sort | string | name,asc | Sort field and direction |

---

### GET /inventory/products/active
List only active products.

---

### GET /inventory/products/{id}
Get product by ID with full details (variants, images, attributes).

---

### GET /inventory/products/sku/{sku}
Get product by SKU.

---

### GET /inventory/products/barcode/{barcode}
Get product by barcode.

---

### GET /inventory/products/search
Search products by name, SKU, barcode, or description.

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| q | string | Yes | Search query |

---

### GET /inventory/products/category/{categoryId}
Get products by category.

---

### GET /inventory/products/brand/{brandId}
Get products by brand.

---

### POST /inventory/products
Create a new product.

**Request:**
```json
{
  "name": "iPhone 15 Pro",
  "sku": "IPHONE15PRO-256-BLK",
  "barcode": "1234567890123",
  "description": "Apple iPhone 15 Pro 256GB Black",
  "shortDescription": "iPhone 15 Pro 256GB",
  "categoryId": 6,
  "brandId": 1,
  "baseUomId": 1,
  "costPrice": 999.00,
  "sellingPrice": 1299.00,
  "minSellingPrice": 1199.00,
  "wholesalePrice": 1149.00,
  "trackInventory": true,
  "allowNegativeStock": false,
  "minStockLevel": 5,
  "reorderPoint": 10,
  "reorderQuantity": 20,
  "active": true,
  "service": false,
  "sellable": true,
  "purchasable": true,
  "trackBatch": false,
  "trackSerial": true,
  "weight": 0.187,
  "weightUnit": "kg",
  "primaryImageUrl": "https://example.com/iphone15pro.jpg",
  "manufacturer": "Apple",
  "manufacturerPartNumber": "MTUX3LL/A",
  "taxCode": "STANDARD",
  "notes": "Premium smartphone",
  "tags": "phone,apple,smartphone",
  "variants": [
    {
      "name": "256GB Space Black",
      "sku": "IPHONE15PRO-256-BLK",
      "option1Name": "Storage",
      "option1Value": "256GB",
      "option2Name": "Color",
      "option2Value": "Space Black",
      "priceDifference": 0,
      "active": true
    }
  ],
  "attributes": [
    {
      "attributeName": "Screen Size",
      "attributeValue": "6.1 inches",
      "attributeType": "TEXT",
      "attributeGroup": "Display",
      "visible": true,
      "searchable": true,
      "filterable": true
    }
  ]
}
```

**Response:** (HTTP 201 Created)
```json
{
  "success": true,
  "data": {
    "id": 1,
    "sku": "IPHONE15PRO-256-BLK",
    "name": "iPhone 15 Pro",
    "categoryId": 6,
    "categoryName": "Phones",
    "brandId": 1,
    "brandName": "Apple",
    "costPrice": 999.00,
    "sellingPrice": 1299.00,
    "margin": 30.03,
    "markup": 23.09,
    "active": true,
    "variants": [...],
    "images": [...],
    "attributes": [...]
  }
}
```

---

### PUT /inventory/products/{id}
Update a product.

---

### DELETE /inventory/products/{id}
Delete a product.

---

### GET /inventory/products/count
Get product counts.

**Response:**
```json
{
  "total": 150,
  "active": 142
}
```

---

## SKU Generation APIs

### GET /inventory/products/generate-sku
Generate a unique SKU.

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| prefix | string | No | Optional prefix for SKU |

**Response:**
```json
{
  "sku": "PRD-20260103-001"
}
```

---

### GET /inventory/products/generate-sku-from-name
Generate SKU from product name.

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| name | string | Yes | Product name |

**Response:**
```json
{
  "sku": "IPHONE-15-PRO-256GB"
}
```

---

### GET /inventory/products/validate-sku/{sku}
Validate a SKU.

**Response:**
```json
{
  "valid": true,
  "exists": false,
  "available": true
}
```

---

## Barcode APIs

### GET /inventory/products/generate-barcode
Generate an internal barcode.

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| prefix | string | No | Optional prefix |

**Response:**
```json
{
  "barcode": "INT1234567890"
}
```

---

### GET /inventory/products/generate-ean13
Generate an EAN-13 barcode.

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| countryCode | string | 200 | Country code (3 digits) |
| companyCode | string | - | Company code (optional) |

**Response:**
```json
{
  "barcode": "2001234567890"
}
```

---

### GET /inventory/products/validate-barcode/{barcode}
Validate a barcode.

**Response:**
```json
{
  "valid": true,
  "exists": false,
  "available": true,
  "type": "EAN13"
}
```

---

## OpenAPI / Swagger

Interactive API documentation is available at:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`

---

## Inventory Module - Operations (Block 5)

Block 5 provides inventory operations including vendor management, purchase orders, receiving, and inventory counting.

---

## Vendor APIs

All vendor endpoints require `INVENTORY_VENDOR_VIEW` or `INVENTORY_VENDOR_MANAGE` permission.

### GET /inventory/vendors
List vendors with pagination.

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | int | 0 | Page number |
| size | int | 20 | Page size |
| sort | string | name,asc | Sort field and direction |

---

### GET /inventory/vendors/active
List only active vendors.

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "code": "VND001",
      "name": "Acme Supplies",
      "contactPerson": "John Doe",
      "email": "john@acme.com",
      "phone": "+1234567890",
      "address": "123 Main St",
      "city": "New York",
      "active": true,
      "preferred": true,
      "paymentTerms": "Net 30",
      "paymentTermsDays": 30,
      "creditLimit": 50000.00,
      "currentBalance": 12500.00,
      "leadTimeDays": 7
    }
  ]
}
```

---

### GET /inventory/vendors/preferred
List preferred vendors only.

---

### GET /inventory/vendors/{id}
Get vendor by ID.

---

### GET /inventory/vendors/code/{code}
Get vendor by code.

---

### GET /inventory/vendors/search
Search vendors by name or code.

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| q | string | Yes | Search query |
| page | int | No | Page number |
| size | int | No | Page size |

---

### POST /inventory/vendors
Create a new vendor.

**Request:**
```json
{
  "code": "VND001",
  "name": "Acme Supplies",
  "contactPerson": "John Doe",
  "email": "john@acme.com",
  "phone": "+1234567890",
  "altPhone": "+0987654321",
  "address": "123 Main St",
  "city": "New York",
  "state": "NY",
  "country": "USA",
  "postalCode": "10001",
  "taxId": "123-45-6789",
  "paymentTerms": "Net 30",
  "paymentTermsDays": 30,
  "creditLimit": 50000.00,
  "defaultCurrency": "USD",
  "bankName": "First National Bank",
  "bankAccount": "1234567890",
  "bankRouting": "021000021",
  "website": "https://acme.com",
  "notes": "Primary supplier for electronics",
  "active": true,
  "preferred": true,
  "leadTimeDays": 7,
  "minOrderAmount": 100.00
}
```

---

### PUT /inventory/vendors/{id}
Update a vendor.

---

### DELETE /inventory/vendors/{id}
Delete a vendor.

---

### PUT /inventory/vendors/{id}/activate
Activate or deactivate a vendor.

---

## Purchase Order APIs

All PO endpoints require specific permissions: `INVENTORY_PO_VIEW`, `INVENTORY_PO_CREATE`, `INVENTORY_PO_UPDATE`, `INVENTORY_PO_APPROVE`, or `INVENTORY_PO_CANCEL`.

### PO Status Flow
```
DRAFT → PENDING → APPROVED → PARTIAL/RECEIVED → CLOSED
                      ↓
                  CANCELLED
```

### GET /inventory/purchase-orders
List purchase orders with pagination.

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | int | 0 | Page number |
| size | int | 20 | Page size |
| sort | string | orderDate,desc | Sort field and direction |

---

### GET /inventory/purchase-orders/{id}
Get purchase order by ID with full details including line items.

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "poNumber": "PO-202601-0001",
    "vendorId": 1,
    "vendorName": "Acme Supplies",
    "locationId": 1,
    "locationName": "Main Warehouse",
    "status": "APPROVED",
    "orderDate": "2026-01-03",
    "expectedDate": "2026-01-10",
    "subtotal": 5000.00,
    "discountAmount": 250.00,
    "taxAmount": 475.00,
    "shippingAmount": 50.00,
    "totalAmount": 5275.00,
    "currency": "USD",
    "paymentTerms": "Net 30",
    "notes": "Urgent order",
    "approvedBy": "admin",
    "approvedAt": "2026-01-03T14:30:00Z",
    "lines": [
      {
        "id": 1,
        "lineNumber": 1,
        "productId": 1,
        "productName": "iPhone 15 Pro",
        "productSku": "IPHONE15PRO",
        "quantity": 10,
        "receivedQuantity": 0,
        "unitPrice": 500.00,
        "discountPercent": 5.00,
        "taxPercent": 10.00,
        "lineTotal": 5275.00
      }
    ]
  }
}
```

---

### GET /inventory/purchase-orders/status/{status}
Get POs by status.

**Status Values:** `DRAFT`, `PENDING`, `APPROVED`, `PARTIAL`, `RECEIVED`, `CANCELLED`, `CLOSED`

---

### GET /inventory/purchase-orders/vendor/{vendorId}
Get POs for a specific vendor.

---

### GET /inventory/purchase-orders/search
Search POs by PO number or vendor name.

---

### POST /inventory/purchase-orders
Create a new purchase order.

**Request:**
```json
{
  "vendorId": 1,
  "locationId": 1,
  "orderDate": "2026-01-03",
  "expectedDate": "2026-01-10",
  "discountPercent": 5.00,
  "shippingAmount": 50.00,
  "currency": "USD",
  "paymentTerms": "Net 30",
  "shippingMethod": "Ground",
  "shippingAddress": "123 Warehouse Ave",
  "notes": "Urgent order",
  "internalNotes": "Approved by manager",
  "vendorReference": "VND-REF-001",
  "lines": [
    {
      "productId": 1,
      "quantity": 10,
      "unitPrice": 500.00,
      "discountPercent": 5.00,
      "taxPercent": 10.00,
      "notes": "Latest model",
      "expectedDate": "2026-01-10"
    }
  ]
}
```

---

### PUT /inventory/purchase-orders/{id}/submit
Submit PO for approval. Changes status from DRAFT to PENDING.

---

### PUT /inventory/purchase-orders/{id}/approve
Approve a pending PO. Changes status from PENDING to APPROVED.

---

### PUT /inventory/purchase-orders/{id}/cancel
Cancel a PO.

**Request:**
```json
{
  "reason": "Vendor unable to fulfill order"
}
```

---

## Receiving APIs

All receiving endpoints require `INVENTORY_RECEIVING_VIEW`, `INVENTORY_RECEIVING_CREATE`, or `INVENTORY_RECEIVING_CONFIRM` permission.

### Receiving Status Flow
```
DRAFT → PENDING → IN_PROGRESS → COMPLETED
                      ↓
                  CANCELLED
```

### GET /inventory/receiving
List receiving orders with pagination.

---

### GET /inventory/receiving/{id}
Get receiving order by ID with full details.

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "receivingNumber": "RCV-202601-0001",
    "purchaseOrderId": 1,
    "poNumber": "PO-202601-0001",
    "vendorId": 1,
    "vendorName": "Acme Supplies",
    "locationId": 1,
    "locationName": "Main Warehouse",
    "status": "COMPLETED",
    "receivingDate": "2026-01-03",
    "vendorDeliveryNote": "DN-001",
    "vendorInvoiceNumber": "INV-001",
    "subtotal": 4750.00,
    "totalAmount": 5000.00,
    "stockUpdated": true,
    "lines": [
      {
        "id": 1,
        "lineNumber": 1,
        "productId": 1,
        "productName": "iPhone 15 Pro",
        "expectedQuantity": 10,
        "receivedQuantity": 10,
        "acceptedQuantity": 9,
        "rejectedQuantity": 1,
        "rejectionReason": "Damaged packaging",
        "unitCost": 500.00,
        "batchNumber": "BATCH-001",
        "expiryDate": "2027-01-03"
      }
    ]
  }
}
```

---

### GET /inventory/receiving/status/{status}
Get receiving orders by status.

**Status Values:** `DRAFT`, `PENDING`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`

---

### GET /inventory/receiving/purchase-order/{poId}
Get all receiving orders for a specific PO.

---

### GET /inventory/receiving/search
Search receiving orders.

---

### POST /inventory/receiving
Create a receiving order.

**Request:**
```json
{
  "purchaseOrderId": 1,
  "vendorId": 1,
  "locationId": 1,
  "receivingDate": "2026-01-03",
  "vendorDeliveryNote": "DN-001",
  "vendorInvoiceNumber": "INV-001",
  "vendorInvoiceDate": "2026-01-03",
  "discountAmount": 100.00,
  "shippingAmount": 50.00,
  "currency": "USD",
  "notes": "Received in good condition",
  "lines": [
    {
      "poLineId": 1,
      "productId": 1,
      "expectedQuantity": 10,
      "receivedQuantity": 10,
      "acceptedQuantity": 9,
      "rejectedQuantity": 1,
      "rejectionReason": "Damaged packaging",
      "unitCost": 500.00,
      "batchNumber": "BATCH-001",
      "expiryDate": "2027-01-03",
      "targetLocationId": 1
    }
  ]
}
```

---

### PUT /inventory/receiving/{id}/confirm
Confirm receiving and update stock. This automatically creates stock movements for accepted quantities.

---

### PUT /inventory/receiving/{id}/cancel
Cancel a receiving order.

**Request:**
```json
{
  "reason": "Incorrect shipment received"
}
```

---

## Inventory Count APIs

All inventory count endpoints require `INVENTORY_COUNT_VIEW`, `INVENTORY_COUNT_CREATE`, `INVENTORY_COUNT_PERFORM`, or `INVENTORY_COUNT_APPROVE` permission.

### Count Status Flow
```
DRAFT → IN_PROGRESS → COMPLETED → APPROVED
                          ↓
                      CANCELLED
```

### Count Types
- `FULL` - Complete inventory count of all products
- `PARTIAL` - Count specific products or categories
- `CYCLE` - Regular cycle counting
- `SPOT` - Quick spot check
- `ANNUAL` - Annual inventory audit

### GET /inventory/counts
List inventory counts with pagination.

---

### GET /inventory/counts/{id}
Get inventory count by ID with full details.

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "countNumber": "CNT-202601-0001",
    "locationId": 1,
    "locationName": "Main Warehouse",
    "status": "COMPLETED",
    "countType": "CYCLE",
    "countDate": "2026-01-03",
    "description": "Weekly cycle count",
    "totalItems": 50,
    "countedItems": 50,
    "varianceItems": 3,
    "totalVarianceValue": -150.00,
    "positiveVarianceValue": 50.00,
    "negativeVarianceValue": 200.00,
    "freezeStock": false,
    "startedAt": "2026-01-03T09:00:00Z",
    "completedAt": "2026-01-03T12:00:00Z",
    "adjustmentsPosted": false,
    "lines": [
      {
        "id": 1,
        "lineNumber": 1,
        "productId": 1,
        "productName": "iPhone 15 Pro",
        "productSku": "IPHONE15PRO",
        "systemQuantity": 100,
        "countedQuantity": 98,
        "varianceQuantity": -2,
        "unitCost": 500.00,
        "varianceValue": -1000.00,
        "counted": true,
        "countedBy": "warehouse_staff",
        "countedAt": "2026-01-03T10:30:00Z",
        "recountRequired": false,
        "adjustmentReason": "Possible theft or miscounting"
      }
    ]
  }
}
```

---

### GET /inventory/counts/status/{status}
Get counts by status.

**Status Values:** `DRAFT`, `IN_PROGRESS`, `COMPLETED`, `APPROVED`, `CANCELLED`

---

### GET /inventory/counts/search
Search inventory counts.

---

### POST /inventory/counts
Create an inventory count.

**Request:**
```json
{
  "locationId": 1,
  "countType": "CYCLE",
  "countDate": "2026-01-03",
  "description": "Weekly cycle count - Electronics section",
  "notes": "Focus on high-value items",
  "freezeStock": false,
  "categoryId": 1,
  "productIds": [1, 2, 3, 4, 5]
}
```

**Notes:**
- If `productIds` is provided, only those products will be counted
- If `categoryId` is provided, all products in that category will be counted
- If neither is provided, all products at the location will be counted

---

### PUT /inventory/counts/{id}/start
Start an inventory count. Changes status from DRAFT to IN_PROGRESS.

---

### PUT /inventory/counts/{countId}/lines/{lineId}/count
Record count for a specific line item.

**Request:**
```json
{
  "countedQuantity": 98,
  "batchNumber": "BATCH-001",
  "serialNumber": "SN-12345",
  "notes": "Found 2 units less than expected"
}
```

---

### PUT /inventory/counts/{countId}/lines/{lineId}/recount
Mark a line for recount.

**Request:**
```json
{
  "reason": "Variance too high, needs verification"
}
```

---

### PUT /inventory/counts/{id}/complete
Complete the count. Changes status from IN_PROGRESS to COMPLETED.

---

### PUT /inventory/counts/{id}/approve
Approve the count and post adjustments. Changes status from COMPLETED to APPROVED. Automatically creates stock adjustment movements for all variances.

---

### PUT /inventory/counts/{id}/cancel
Cancel an inventory count.

**Request:**
```json
{
  "reason": "Count interrupted, will reschedule"
}
```

---

## Block 5 Permissions Summary

| Permission | Description |
|------------|-------------|
| INVENTORY_VENDOR_VIEW | View vendors |
| INVENTORY_VENDOR_MANAGE | Create, update, delete vendors |
| INVENTORY_PO_VIEW | View purchase orders |
| INVENTORY_PO_CREATE | Create purchase orders |
| INVENTORY_PO_UPDATE | Update and submit POs |
| INVENTORY_PO_APPROVE | Approve purchase orders |
| INVENTORY_PO_CANCEL | Cancel purchase orders |
| INVENTORY_RECEIVING_VIEW | View receiving orders |
| INVENTORY_RECEIVING_CREATE | Create receiving orders |
| INVENTORY_RECEIVING_CONFIRM | Confirm and cancel receiving |
| INVENTORY_COUNT_VIEW | View inventory counts |
| INVENTORY_COUNT_CREATE | Create inventory counts |
| INVENTORY_COUNT_PERFORM | Start, record, complete counts |
| INVENTORY_COUNT_APPROVE | Approve and cancel counts |

---

## POS Pricing & Promotions APIs

### Price Lists

#### GET /pos/price-lists
Get all price lists with pagination.

**Query Parameters:**
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 20)
- `sort` (optional): Sort field (default: priority,desc)

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "code": "STANDARD",
        "name": "Standard Pricing",
        "description": "Default price list for all customers",
        "type": "STANDARD",
        "currency": "UZS",
        "priority": 0,
        "defaultMarkupPercent": null,
        "startDate": null,
        "endDate": null,
        "locationId": null,
        "active": true,
        "itemCount": 150,
        "customerCount": 0
      }
    ],
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

#### GET /pos/price-lists/active
Get currently active/effective price lists.

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "code": "STANDARD",
      "name": "Standard Pricing",
      "type": "STANDARD",
      "active": true
    }
  ]
}
```

---

#### GET /pos/price-lists/{id}
Get a specific price list by ID with all items.

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "code": "VIP",
    "name": "VIP Customer Pricing",
    "type": "VIP",
    "priority": 10,
    "items": [
      {
        "id": 1,
        "productId": 100,
        "productCode": "SKU-001",
        "productName": "Product A",
        "price": 50000.00,
        "markupPercent": null,
        "minQuantity": 1,
        "maxQuantity": null,
        "active": true
      }
    ]
  }
}
```

---

#### POST /pos/price-lists
Create a new price list.

**Request:**
```json
{
  "code": "WHOLESALE",
  "name": "Wholesale Pricing",
  "description": "Pricing for wholesale customers",
  "type": "WHOLESALE",
  "currency": "UZS",
  "priority": 5,
  "defaultMarkupPercent": -15.00,
  "startDate": "2026-01-01",
  "endDate": "2026-12-31",
  "locationId": null
}
```

**Response:**
```json
{
  "success": true,
  "message": "Price list created",
  "data": {
    "id": 2,
    "code": "WHOLESALE",
    "name": "Wholesale Pricing",
    "active": true
  }
}
```

---

#### PUT /pos/price-lists/{id}
Update an existing price list.

---

#### DELETE /pos/price-lists/{id}
Delete a price list.

---

#### POST /pos/price-lists/{id}/activate
Activate a price list.

---

#### POST /pos/price-lists/{id}/deactivate
Deactivate a price list.

---

#### POST /pos/price-lists/{priceListId}/items
Add an item to a price list.

**Request:**
```json
{
  "productId": 100,
  "variantId": null,
  "price": 45000.00,
  "markupPercent": null,
  "minQuantity": 1,
  "maxQuantity": 100,
  "startDate": null,
  "endDate": null
}
```

---

#### PUT /pos/price-lists/{priceListId}/items/{itemId}
Update a price list item.

---

#### DELETE /pos/price-lists/{priceListId}/items/{itemId}
Remove an item from a price list.

---

#### POST /pos/price-lists/{priceListId}/assign-customer/{customerId}
Assign a price list to a customer.

**Query Parameters:**
- `priority` (optional): Assignment priority (default: 0)

---

#### DELETE /pos/price-lists/{priceListId}/unassign-customer/{customerId}
Remove a price list assignment from a customer.

---

### Promotions

#### GET /pos/promotions
Get all promotions with pagination.

**Query Parameters:**
- `page`, `size`, `sort`

---

#### GET /pos/promotions/active
Get currently active promotions.

**Query Parameters:**
- `locationId` (optional): Filter by location

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "code": "SUMMER20",
      "name": "Summer Sale 20% Off",
      "type": "PERCENTAGE_OFF",
      "scope": "ORDER",
      "discountValue": 20.00,
      "active": true,
      "startDate": "2026-06-01",
      "endDate": "2026-08-31"
    }
  ]
}
```

---

#### POST /pos/promotions
Create a new promotion.

**Request:**
```json
{
  "code": "BOGO",
  "name": "Buy One Get One Free",
  "description": "Buy one item, get one free",
  "type": "BUY_X_GET_Y",
  "scope": "LINE_ITEM",
  "buyQuantity": 1,
  "getQuantity": 1,
  "getDiscountPercent": 100.00,
  "startDate": "2026-01-01",
  "endDate": "2026-01-31",
  "priority": 5,
  "stackable": false,
  "requiresCoupon": true,
  "conditions": [
    {
      "conditionType": "SPECIFIC_PRODUCTS",
      "productIds": "100,101,102"
    }
  ],
  "actions": [
    {
      "actionType": "PERCENTAGE_OFF",
      "discountPercent": 100.00,
      "applyTo": "CHEAPEST",
      "applyCount": 1
    }
  ]
}
```

---

#### PUT /pos/promotions/{id}
Update an existing promotion.

---

#### DELETE /pos/promotions/{id}
Delete a promotion.

---

#### POST /pos/promotions/{id}/activate
Activate a promotion.

---

#### POST /pos/promotions/{id}/deactivate
Deactivate a promotion.

---

#### POST /pos/promotions/{promotionId}/conditions
Add a condition to a promotion.

**Request:**
```json
{
  "conditionType": "MINIMUM_PURCHASE",
  "thresholdAmount": 100000.00,
  "required": true
}
```

---

#### DELETE /pos/promotions/{promotionId}/conditions/{conditionId}
Remove a condition from a promotion.

---

### Coupons

#### GET /pos/coupons
Get all coupons with pagination.

---

#### GET /pos/coupons/promotion/{promotionId}
Get coupons for a specific promotion.

---

#### GET /pos/coupons/status/{status}
Get coupons by status (ACTIVE, INACTIVE, EXPIRED, DEPLETED, CANCELLED).

---

#### GET /pos/coupons/{id}
Get a specific coupon by ID.

---

#### GET /pos/coupons/code/{code}
Get a coupon by its code.

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "code": "SAVE20NOW",
    "promotionId": 1,
    "promotionCode": "SUMMER20",
    "promotionName": "Summer Sale 20% Off",
    "status": "ACTIVE",
    "maxUses": 100,
    "currentUses": 25,
    "remainingUses": 75,
    "maxUsesPerCustomer": 1,
    "startDate": "2026-01-01",
    "endDate": "2026-12-31"
  }
}
```

---

#### POST /pos/coupons
Create a new coupon.

**Request:**
```json
{
  "code": "SAVE20NOW",
  "promotionId": 1,
  "description": "Save 20% on your order",
  "startDate": "2026-01-01",
  "endDate": "2026-12-31",
  "maxUses": 100,
  "maxUsesPerCustomer": 1,
  "customerId": null
}
```

---

#### POST /pos/coupons/generate/{promotionId}
Generate multiple coupons for a promotion.

**Query Parameters:**
- `count`: Number of coupons to generate

**Request:**
```json
{
  "maxUses": 1,
  "maxUsesPerCustomer": 1,
  "startDate": "2026-01-01",
  "endDate": "2026-12-31"
}
```

**Response:**
```json
{
  "success": true,
  "data": [
    {"id": 1, "code": "ABC123XYZ4"},
    {"id": 2, "code": "DEF456UVW7"},
    {"id": 3, "code": "GHI789RST0"}
  ]
}
```

---

#### POST /pos/coupons/{id}/activate
Activate a coupon.

---

#### POST /pos/coupons/{id}/deactivate
Deactivate a coupon.

---

#### POST /pos/coupons/{id}/cancel
Cancel a coupon.

---

#### GET /pos/coupons/{id}/redemptions
Get redemption history for a coupon.

---

### Pricing Engine

#### POST /pos/pricing/calculate
Calculate prices for a set of items.

**Request:**
```json
{
  "customerId": 123,
  "locationId": 1,
  "items": [
    {
      "productId": 100,
      "variantId": null,
      "quantity": 2
    },
    {
      "productId": 101,
      "variantId": 5,
      "quantity": 1
    }
  ],
  "couponCode": "SAVE20NOW"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "productId": 100,
        "productCode": "SKU-001",
        "productName": "Product A",
        "quantity": 2,
        "basePrice": 50000.00,
        "unitPrice": 42500.00,
        "lineDiscount": 15000.00,
        "lineTotal": 85000.00,
        "priceListCode": "VIP",
        "appliedPromotionCodes": []
      }
    ],
    "subtotal": 135000.00,
    "totalDiscount": 27000.00,
    "taxAmount": 16200.00,
    "grandTotal": 124200.00,
    "appliedPromotions": [
      {
        "promotionId": 1,
        "promotionCode": "SUMMER20",
        "promotionName": "Summer Sale 20% Off",
        "discountAmount": 27000.00
      }
    ],
    "couponApplication": {
      "couponCode": "SAVE20NOW",
      "valid": true,
      "discountAmount": 27000.00,
      "promotionName": "Summer Sale 20% Off"
    }
  }
}
```

---

#### GET /pos/pricing/product/{productId}
Get the best price for a single product.

**Query Parameters:**
- `variantId` (optional): Variant ID
- `quantity` (optional, default: 1): Quantity
- `customerId` (optional): Customer ID for customer-specific pricing
- `locationId` (optional): Location ID for location-specific pricing

**Response:**
```json
{
  "success": true,
  "data": 42500.00
}
```

---

#### POST /pos/pricing/apply-coupon
Validate and apply a coupon code.

**Request:**
```json
{
  "couponCode": "SAVE20NOW",
  "orderTotal": 135000.00,
  "customerId": 123
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "couponCode": "SAVE20NOW",
    "valid": true,
    "discountAmount": 27000.00,
    "discountDescription": "20% off",
    "promotionId": 1,
    "promotionName": "Summer Sale 20% Off"
  }
}
```

---

#### POST /pos/pricing/validate-coupon
Validate a coupon code without applying it.

**Query Parameters:**
- `couponCode`: The coupon code to validate
- `customerId` (optional): Customer ID

---

#### POST /pos/pricing/record-coupon-redemption
Record a coupon redemption after a successful transaction.

**Query Parameters:**
- `couponCode`: Coupon code that was used
- `customerId`: Customer ID
- `orderId`: Order/Transaction ID
- `discountApplied`: Discount amount that was applied

---

## Enums

### Price List Types
- `STANDARD` - Default pricing for all customers
- `WHOLESALE` - Wholesale/bulk pricing
- `VIP` - VIP/loyalty customer pricing
- `SEASONAL` - Seasonal/promotional pricing
- `EMPLOYEE` - Employee discount pricing
- `CUSTOM` - Custom price list for specific customers

### Promotion Types
- `PERCENTAGE_OFF` - X% off
- `FIXED_AMOUNT_OFF` - $X off
- `BUY_X_GET_Y` - Buy X get Y free/discounted
- `BUNDLE` - Bundle pricing
- `FREE_ITEM` - Free item with purchase
- `TIERED_DISCOUNT` - Discount increases with quantity
- `SPEND_X_GET_Y` - Spend $X get $Y off

### Promotion Condition Types
- `MINIMUM_PURCHASE` - Minimum purchase amount required
- `MINIMUM_QUANTITY` - Minimum quantity of items
- `SPECIFIC_PRODUCTS` - Specific products must be in cart
- `CATEGORY` - Products from specific category
- `BRAND` - Products from specific brand
- `CUSTOMER_GROUP` - Customer must belong to group
- `FIRST_PURCHASE` - First purchase by customer
- `TIME_BASED` - Active during specific hours
- `DAY_OF_WEEK` - Active on specific days
- `PAYMENT_METHOD` - Specific payment method used

### Promotion Scope
- `ORDER` - Applies to entire order
- `LINE_ITEM` - Applies to specific line items
- `SHIPPING` - Applies to shipping cost
- `CATEGORY` - Applies to items in category
- `PRODUCT` - Applies to specific products

### Coupon Status
- `ACTIVE` - Coupon is active and can be used
- `INACTIVE` - Coupon is inactive (not yet started or paused)
- `EXPIRED` - Coupon has expired
- `DEPLETED` - All uses have been exhausted
- `CANCELLED` - Coupon has been cancelled

---

## Phase 2 APIs - Additional Endpoints

### Inventory Planning APIs

#### GET /inventory/planning/reorder-suggestions
Get products that need reordering based on stock levels and reorder points.

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| locationId | long | No | Filter by location |

**Permission:** `INVENTORY_STOCK_VIEW`

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "productId": 1,
      "productName": "iPhone 15 Pro",
      "productSku": "IPHONE15PRO",
      "locationId": 1,
      "locationName": "Main Warehouse",
      "currentStock": 5,
      "reorderPoint": 10,
      "suggestedQuantity": 20,
      "estimatedCost": 10000.00
    }
  ]
}
```

---

#### GET /inventory/planning/abc-analysis
Perform ABC analysis on products based on movement value.

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| days | int | 365 | Number of days to analyze |

**Permission:** `INVENTORY_STOCK_VIEW`

---

#### GET /inventory/planning/slow-moving
Get slow-moving products that haven't sold in X days.

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| days | int | 90 | Days without movement |
| locationId | long | null | Filter by location |

**Permission:** `INVENTORY_STOCK_VIEW`

---

#### GET /inventory/planning/dead-stock
Get dead stock products with zero movement.

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| days | int | 180 | Days without any movement |
| locationId | long | null | Filter by location |

**Permission:** `INVENTORY_STOCK_VIEW`

---

### Recurring Journal APIs

#### GET /finance/recurring-journals
Get all recurring journal templates with pagination.

**Permission:** `FINANCE_RECURRING_VIEW`

---

#### GET /finance/recurring-journals/{id}
Get a recurring journal template by ID.

**Permission:** `FINANCE_RECURRING_VIEW`

---

#### POST /finance/recurring-journals
Create a recurring journal template.

**Request:**
```json
{
  "name": "Monthly Rent",
  "description": "Monthly office rent expense",
  "frequency": "MONTHLY",
  "frequencyDay": 1,
  "startDate": "2026-01-01",
  "endDate": null,
  "active": true,
  "autoPost": false,
  "lines": [
    {
      "accountId": 101,
      "description": "Rent expense",
      "debitAmount": 5000000.00,
      "creditAmount": 0,
      "sortOrder": 1
    },
    {
      "accountId": 201,
      "description": "Cash payment",
      "debitAmount": 0,
      "creditAmount": 5000000.00,
      "sortOrder": 2
    }
  ]
}
```

**Permission:** `FINANCE_RECURRING_MANAGE`

---

#### PUT /finance/recurring-journals/{id}
Update a recurring journal template.

**Permission:** `FINANCE_RECURRING_MANAGE`

---

#### DELETE /finance/recurring-journals/{id}
Delete a recurring journal template.

**Permission:** `FINANCE_RECURRING_MANAGE`

---

#### PUT /finance/recurring-journals/{id}/activate
Activate a recurring journal template.

**Permission:** `FINANCE_RECURRING_MANAGE`

---

#### PUT /finance/recurring-journals/{id}/deactivate
Deactivate a recurring journal template.

**Permission:** `FINANCE_RECURRING_MANAGE`

---

#### POST /finance/recurring-journals/{id}/execute
Manually execute a recurring journal template to create a journal entry.

**Permission:** `FINANCE_RECURRING_MANAGE`

**Response:**
```json
{
  "success": true,
  "data": {
    "journalEntryId": 123
  }
}
```

---

### POS Return APIs

#### POST /pos/transactions/returns
Create a standalone return transaction.

**Request:**
```json
{
  "shiftId": 1,
  "originalTransactionId": null,
  "originalTransactionNumber": null,
  "customerId": 123,
  "returnReason": "Customer changed mind",
  "refundMethod": "ORIGINAL_PAYMENT_METHOD",
  "notes": "Full refund requested",
  "items": [
    {
      "productId": 100,
      "variantId": null,
      "quantity": 1,
      "unitPrice": 50000.00,
      "reason": "Unused product"
    }
  ]
}
```

**Permission:** `POS_RETURN_CREATE`

---

#### POST /pos/transactions/{id}/return
Create a return from an existing transaction.

**Permission:** `POS_RETURN_CREATE`

---

### Product Import/Export APIs

#### POST /inventory/products/import/csv
Import products from CSV file.

**Content-Type:** `multipart/form-data`

**Form Data:**
| Field | Type | Description |
|-------|------|-------------|
| file | file | CSV file |

**Permission:** `INVENTORY_PRODUCT_CREATE`

---

#### POST /inventory/products/import/excel
Import products from Excel file.

**Content-Type:** `multipart/form-data`

**Permission:** `INVENTORY_PRODUCT_CREATE`

---

#### GET /inventory/products/export/csv
Export products to CSV.

**Permission:** `INVENTORY_PRODUCT_READ`

---

#### GET /inventory/products/export/excel
Export products to Excel.

**Permission:** `INVENTORY_PRODUCT_READ`

---

#### GET /inventory/products/import/template/csv
Download CSV import template.

---

#### GET /inventory/products/import/template/excel
Download Excel import template.

---

### Product Variant APIs

#### GET /inventory/products/{productId}/variants
Get all variants for a product.

**Permission:** `INVENTORY_PRODUCT_READ`

---

#### POST /inventory/products/{productId}/variants
Add a variant to a product.

**Request:**
```json
{
  "sku": null,
  "barcode": null,
  "name": "256GB Space Black",
  "option1Name": "Storage",
  "option1Value": "256GB",
  "option2Name": "Color",
  "option2Value": "Space Black",
  "costPrice": 999.00,
  "sellingPrice": 1299.00,
  "active": true
}
```

**Permission:** `INVENTORY_PRODUCT_CREATE`

---

#### PUT /inventory/products/{productId}/variants/{variantId}
Update a product variant.

**Permission:** `INVENTORY_PRODUCT_UPDATE`

---

#### DELETE /inventory/products/{productId}/variants/{variantId}
Delete a product variant.

**Permission:** `INVENTORY_PRODUCT_DELETE`

---

### Chart of Accounts Import API

#### POST /finance/accounts/import
Import chart of accounts from CSV.

**Content-Type:** `multipart/form-data`

**Permission:** `FINANCE_GL_MANAGE`

---

#### POST /finance/accounts/generate-default
Generate a default chart of accounts.

**Permission:** `FINANCE_GL_MANAGE`

---

### Recurring Journal Frequency Enum
- `DAILY` - Run every day
- `WEEKLY` - Run every week
- `BIWEEKLY` - Run every two weeks
- `MONTHLY` - Run every month
- `QUARTERLY` - Run every quarter
- `SEMIANNUALLY` - Run twice a year
- `ANNUALLY` - Run once a year

### POS Return Refund Methods
- `CASH` - Cash refund
- `CARD` - Card refund
- `STORE_CREDIT` - Store credit
- `ORIGINAL_PAYMENT_METHOD` - Refund to original payment method

---

## Admin Dashboard APIs (Block 12)

Admin dashboard provides system configuration, audit logging, and business overview functionality.

---

### Dashboard Statistics

#### GET /admin/dashboard/stats
Get comprehensive dashboard statistics and metrics.

**Permission:** `ADMIN_DASHBOARD_VIEW`

**Response:**
```json
{
  "success": true,
  "data": {
    "totalUsers": 50,
    "activeUsers": 45,
    "newUsersToday": 2,
    "newUsersThisWeek": 8,
    "newUsersThisMonth": 15,
    "totalAuditLogsToday": 250,
    "failedLoginsToday": 3,
    "moduleActivities": [
      {"module": "INVENTORY", "activityCount": 120},
      {"module": "POS", "activityCount": 85},
      {"module": "FINANCE", "activityCount": 45}
    ],
    "topActiveUsers": [
      {"userId": 1, "username": "admin", "activityCount": 150},
      {"userId": 2, "username": "cashier1", "activityCount": 89}
    ],
    "recentActivities": [
      {
        "description": "Created new product",
        "action": "CREATE",
        "entityType": "Product",
        "username": "admin",
        "timestamp": "2026-01-15T10:30:00Z"
      }
    ]
  }
}
```

---

### System Settings APIs

System settings are global configurations that apply across all tenants.

#### GET /admin/settings/system
Get all active system settings.

**Permission:** `ADMIN_SETTINGS_VIEW`

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "settingKey": "session.timeout.minutes",
      "settingValue": "30",
      "defaultValue": "30",
      "description": "Session timeout in minutes",
      "category": "SECURITY",
      "valueType": "INTEGER",
      "sensitive": false,
      "readonly": false,
      "active": true
    }
  ]
}
```

---

#### GET /admin/settings/system/categories
Get distinct setting categories.

**Permission:** `ADMIN_SETTINGS_VIEW`

---

#### GET /admin/settings/system/category/{category}
Get settings by category.

**Permission:** `ADMIN_SETTINGS_VIEW`

---

#### GET /admin/settings/system/{key}
Get a specific system setting by key.

**Permission:** `ADMIN_SETTINGS_VIEW`

---

#### POST /admin/settings/system
Create a new system setting.

**Request:**
```json
{
  "settingKey": "custom.feature.enabled",
  "settingValue": "true",
  "defaultValue": "false",
  "description": "Enable custom feature",
  "category": "FEATURES",
  "valueType": "BOOLEAN",
  "sensitive": false,
  "readonly": false
}
```

**Permission:** `ADMIN_SETTINGS_MANAGE`

---

#### PUT /admin/settings/system/{key}
Update a system setting.

**Permission:** `ADMIN_SETTINGS_MANAGE`

---

#### PUT /admin/settings/system/{key}/value
Update only the value of a system setting.

**Request:**
```json
{
  "value": "60"
}
```

**Permission:** `ADMIN_SETTINGS_MANAGE`

---

#### PUT /admin/settings/system/batch
Batch update multiple settings.

**Request:**
```json
{
  "session.timeout.minutes": "60",
  "login.max.attempts": "3"
}
```

**Permission:** `ADMIN_SETTINGS_MANAGE`

---

#### DELETE /admin/settings/system/{key}
Deactivate a system setting.

**Permission:** `ADMIN_SETTINGS_MANAGE`

---

### Tenant Settings APIs

Tenant settings override system defaults for specific tenants.

#### GET /admin/settings/tenant
Get all tenant settings for the current tenant.

**Permission:** `TENANT_SETTINGS_VIEW`

---

#### GET /admin/settings/tenant/categories
Get distinct categories for tenant settings.

**Permission:** `TENANT_SETTINGS_VIEW`

---

#### GET /admin/settings/tenant/category/{category}
Get tenant settings by category.

**Permission:** `TENANT_SETTINGS_VIEW`

---

#### POST /admin/settings/tenant
Create a tenant-specific setting.

**Request:**
```json
{
  "settingKey": "pos.receipt.footer",
  "settingValue": "Thank you for shopping at Our Store!",
  "description": "Custom receipt footer",
  "category": "POS",
  "valueType": "STRING"
}
```

**Permission:** `TENANT_SETTINGS_MANAGE`

---

#### PUT /admin/settings/tenant/{key}
Update a tenant setting.

**Permission:** `TENANT_SETTINGS_MANAGE`

---

#### PUT /admin/settings/tenant/{key}/value
Update only the value of a tenant setting.

**Permission:** `TENANT_SETTINGS_MANAGE`

---

#### PUT /admin/settings/tenant/batch
Batch update multiple tenant settings.

**Permission:** `TENANT_SETTINGS_MANAGE`

---

#### GET /admin/settings/tenant/map
Get all tenant settings as a key-value map.

**Permission:** `TENANT_SETTINGS_VIEW`

**Response:**
```json
{
  "success": true,
  "data": {
    "pos.receipt.footer": "Thank you for shopping!",
    "inventory.low.stock.threshold": "15"
  }
}
```

---

### Audit Log APIs

Audit logs track all system activities for compliance and debugging.

#### GET /admin/audit-logs
Get paginated audit logs.

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | int | 0 | Page number |
| size | int | 20 | Page size |
| sort | string | actionTimestamp,desc | Sort field and direction |

**Permission:** `ADMIN_AUDIT_VIEW`

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "tenantId": 1,
        "userId": 1,
        "username": "admin",
        "userFullName": "Admin User",
        "action": "CREATE",
        "entityType": "Product",
        "entityId": 100,
        "entityName": "iPhone 15 Pro",
        "module": "INVENTORY",
        "description": "Created new product",
        "ipAddress": "192.168.1.100",
        "actionTimestamp": "2026-01-15T10:30:00Z",
        "success": true
      }
    ],
    "totalElements": 1000,
    "totalPages": 50
  }
}
```

---

#### GET /admin/audit-logs/user/{userId}
Get audit logs for a specific user.

**Permission:** `ADMIN_AUDIT_VIEW`

---

#### GET /admin/audit-logs/entity/{entityType}/{entityId}
Get audit logs for a specific entity.

**Permission:** `ADMIN_AUDIT_VIEW`

---

#### GET /admin/audit-logs/action/{action}
Get audit logs by action type.

**Action Types:** `CREATE`, `READ`, `UPDATE`, `DELETE`, `LOGIN`, `LOGOUT`, `LOGIN_FAILED`, `PASSWORD_CHANGE`, `PERMISSION_CHANGE`, `EXPORT`, `IMPORT`, `APPROVE`, `REJECT`, `SUBMIT`, `CANCEL`, `ACTIVATE`, `DEACTIVATE`, `TRANSFER`, `ADJUSTMENT`, `CUSTOM`

**Permission:** `ADMIN_AUDIT_VIEW`

---

#### GET /admin/audit-logs/module/{module}
Get audit logs for a specific module.

**Permission:** `ADMIN_AUDIT_VIEW`

---

#### GET /admin/audit-logs/date-range
Get audit logs within a date range.

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| startDate | datetime | Yes | Start date (ISO format) |
| endDate | datetime | Yes | End date (ISO format) |

**Permission:** `ADMIN_AUDIT_VIEW`

---

#### GET /admin/audit-logs/failed
Get audit logs for failed actions.

**Permission:** `ADMIN_AUDIT_VIEW`

---

#### GET /admin/audit-logs/stats/actions
Get action count statistics.

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| days | int | 7 | Number of days to look back |

**Permission:** `ADMIN_AUDIT_VIEW`

---

#### GET /admin/audit-logs/stats/modules
Get activity count by module.

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| days | int | 7 | Number of days to look back |

**Permission:** `ADMIN_AUDIT_VIEW`

---

#### GET /admin/audit-logs/stats/users
Get most active users by audit log count.

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| days | int | 7 | Number of days to look back |

**Permission:** `ADMIN_AUDIT_VIEW`

---

#### GET /admin/audit-logs/stats/failed-logins
Get count of failed login attempts.

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| hours | int | 24 | Number of hours to look back |

**Permission:** `ADMIN_AUDIT_VIEW`

**Response:**
```json
{
  "success": true,
  "data": {
    "count": 5
  }
}
```

---

### Admin Permissions Summary

| Permission | Description |
|------------|-------------|
| ADMIN_DASHBOARD_VIEW | View admin dashboard and statistics |
| ADMIN_SETTINGS_VIEW | View system settings |
| ADMIN_SETTINGS_MANAGE | Create, update, delete system settings |
| TENANT_SETTINGS_VIEW | View tenant settings |
| TENANT_SETTINGS_MANAGE | Create, update, delete tenant settings |
| ADMIN_AUDIT_VIEW | View audit logs |
| ADMIN_AUDIT_EXPORT | Export audit logs |
| ADMIN_HEALTH_VIEW | View system health and monitoring |

---

### Setting Value Types

- `STRING` - Text value
- `INTEGER` - Whole number
- `DECIMAL` - Decimal number
- `BOOLEAN` - True/false value
- `JSON` - JSON object or array
- `ENUM` - Predefined options
- `DATE` - Date value
- `DATETIME` - Date and time value

---

## Delivery Module (Regions & Villages)

Delivery address management for POS. Full documentation: [DELIVERY_MODULE_API.md](api/DELIVERY_MODULE_API.md)

### Endpoints

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| GET | /delivery/regions | DELIVERY_REGION_READ | List regions (paginated, searchable) |
| GET | /delivery/regions/active | DELIVERY_REGION_READ | Get active regions |
| GET | /delivery/regions/{id} | DELIVERY_REGION_READ | Get region by ID |
| POST | /delivery/regions | DELIVERY_REGION_CREATE | Create region |
| PUT | /delivery/regions/{id} | DELIVERY_REGION_UPDATE | Update region |
| DELETE | /delivery/regions/{id} | DELIVERY_REGION_DELETE | Delete region |
| GET | /delivery/villages | DELIVERY_VILLAGE_READ | List villages (paginated, searchable) |
| GET | /delivery/villages/active | DELIVERY_VILLAGE_READ | Get active villages |
| GET | /delivery/villages/region/{regionId} | DELIVERY_VILLAGE_READ | Get villages by region |
| GET | /delivery/villages/{id} | DELIVERY_VILLAGE_READ | Get village by ID |
| POST | /delivery/villages | DELIVERY_VILLAGE_CREATE | Create village |
| PUT | /delivery/villages/{id} | DELIVERY_VILLAGE_UPDATE | Update village |
| DELETE | /delivery/villages/{id} | DELIVERY_VILLAGE_DELETE | Delete village |

### POS Integration

POS transactions now accept optional `deliveryRegionId` and `deliveryVillageId` fields. The backend resolves and stores region/village names denormalized on the transaction.

---

## Distribution Module — Agents (Slice 1)

Field sales / route / van-sales agents and their territory coverage. First slice of the
wholesale distribution module (routing, mobile selling, KPIs, and B2B marketplace follow in
later slices).

> **Status:** routing, van loadouts, KPIs (incl. strike rate / drop size / trend), visit
> cash collection against AR, the B2B marketplace, **and the agent-facing mobile API**
> (Slice 7 below) are all built. Staff endpoints use `DISTRIBUTION_*` permissions; the
> agent app is a separate public, token-authenticated surface under `/api/v1/agent/**`.

### Endpoints

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| GET | /distribution/agents | DISTRIBUTION_AGENT_VIEW | List all agents |
| GET | /distribution/agents/paginated | DISTRIBUTION_AGENT_VIEW | List agents (paginated) |
| GET | /distribution/agents/active | DISTRIBUTION_AGENT_VIEW | List ACTIVE agents |
| GET | /distribution/agents/search?q= | DISTRIBUTION_AGENT_VIEW | Search by name/code/phone (paginated) |
| GET | /distribution/agents/{id} | DISTRIBUTION_AGENT_VIEW | Get agent by ID |
| POST | /distribution/agents | DISTRIBUTION_AGENT_MANAGE | Create agent |
| PUT | /distribution/agents/{id} | DISTRIBUTION_AGENT_MANAGE | Update agent (partial; `territories` replaces the set when present) |
| DELETE | /distribution/agents/{id} | DISTRIBUTION_AGENT_MANAGE | Delete agent |

### Create request

```json
{
  "code": "AG-001",
  "name": "Alisher Karimov",
  "phone": "+998901112233",
  "email": "alisher@example.uz",
  "userId": null,
  "employeeId": null,
  "vehicleName": "Isuzu NPR",
  "vehiclePlate": "01A123BC",
  "commissionPercent": 3.5,
  "status": "ACTIVE",
  "hiredAt": "2026-01-15",
  "notes": null,
  "territories": [
    { "regionId": 5, "villageId": null, "priority": 1, "exclusive": true, "active": true },
    { "regionId": 6, "villageId": 60, "priority": 0, "exclusive": false, "active": true }
  ]
}
```

- `status` is one of `ACTIVE` | `SUSPENDED` | `TERMINATED` (defaults to `ACTIVE`).
- `code` is immutable after creation and unique per tenant.
- `territories[].regionId` / `villageId` reference `delivery_regions` / `delivery_villages`.
  A null `villageId` means the whole region.
- On update, omitting `territories` (null) leaves the existing set untouched; sending an array
  (including `[]`) replaces it wholesale.

## Distribution Module — Orders (Slice 2)

B2B wholesale sales orders with a fulfilment lifecycle, server-side pricing, best-effort
stock reservation/deduction, and AR-invoice conversion.

### Lifecycle

```
DRAFT ─▶ CONFIRMED ─▶ PICKING ─▶ LOADED ─▶ IN_TRANSIT ─▶ DELIVERED ─▶ INVOICED
  │          │           │          │           │
  └──────────┴───────────┴──────────┴───────────┴──▶ CANCELLED
```

- **CONFIRMED** reserves stock at the order's source location (the tenant's default
  warehouse if none is set).
- **DELIVERED** releases the reservation, deducts stock (a `STOCK_OUT` movement with
  reference type `DISTRIBUTION_ORDER`), stamps `deliveredAt`, and splits the total into
  `cashCollected` vs `creditAmount`.
- **INVOICED** raises a DRAFT AR invoice for the **credit portion** (`salesOrderId` links it
  back; delivery fee and tax are added as explicit invoice lines so the invoice total equals
  the order total). A **fully cash-settled order raises no AR invoice** — no phantom
  receivable. Posting the invoice to the GL and recording the collected cash are
  Finance-module actions.
- **CANCELLED** releases any held reservation. `INVOICED` and `CANCELLED` are terminal.

Stock operations are best-effort (they never block a status change); failures are logged.

### Endpoints

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| GET | /distribution/orders?status=&agentId= | DISTRIBUTION_ORDER_VIEW | List orders (paginated; optional status/agent filter) |
| GET | /distribution/orders/search?q= | DISTRIBUTION_ORDER_VIEW | Search by number/customer |
| GET | /distribution/orders/{id} | DISTRIBUTION_ORDER_VIEW | Get order by ID |
| POST | /distribution/orders | DISTRIBUTION_ORDER_CREATE | Create order (DRAFT) |
| PUT | /distribution/orders/{id} | DISTRIBUTION_ORDER_MANAGE | Edit a DRAFT order |
| DELETE | /distribution/orders/{id} | DISTRIBUTION_ORDER_MANAGE | Delete a DRAFT order |
| POST | /distribution/orders/{id}/confirm | DISTRIBUTION_ORDER_MANAGE | DRAFT → CONFIRMED (reserve stock) |
| POST | /distribution/orders/{id}/pick | DISTRIBUTION_ORDER_MANAGE | CONFIRMED → PICKING |
| POST | /distribution/orders/{id}/load | DISTRIBUTION_ORDER_MANAGE | PICKING → LOADED |
| POST | /distribution/orders/{id}/transit | DISTRIBUTION_ORDER_MANAGE | LOADED → IN_TRANSIT |
| POST | /distribution/orders/{id}/deliver | DISTRIBUTION_ORDER_MANAGE | IN_TRANSIT → DELIVERED, body `{ cashCollected? }` |
| POST | /distribution/orders/{id}/invoice | DISTRIBUTION_ORDER_MANAGE | DELIVERED → INVOICED (creates AR invoice) |
| POST | /distribution/orders/{id}/cancel | DISTRIBUTION_ORDER_MANAGE | → CANCELLED, body `{ reason? }` |

### Create request

```json
{
  "customerId": 100,
  "agentId": 5,
  "visitId": null,
  "routeId": null,
  "paymentMethod": "CREDIT",
  "paymentTermsDays": 14,
  "discountAmount": 0,
  "taxAmount": 0,
  "deliveryFee": 15000,
  "expectedDeliveryDate": "2026-07-10",
  "deliveryAddress": "Chilonzor 12/34",
  "deliveryLat": null,
  "deliveryLng": null,
  "notes": null,
  "lines": [
    { "productId": 10, "quantity": 2, "discountPercent": 0 },
    { "productId": 11, "quantity": 1, "discountPercent": 10 }
  ]
}
```

- Optional `visitId` / `routeId` link the order to the field visit / route it was taken on
  (both tenant-validated). The order snapshots the customer's `priceListId` (pricing basis) and
  returns it on the DTO alongside `deliveryLat` / `deliveryLng`.

- `paymentMethod` is one of `CASH` | `CREDIT` | `MIXED` (defaults to the codebase default `CREDIT`).
- **Unit prices are resolved server-side** via the pricing engine (customer price lists →
  location → base selling price). The client only sends `productId`, `quantity`, and an
  optional per-line `discountPercent`; any client-sent price is ignored.
- Totals are computed server-side: `subtotal − discountAmount + taxAmount + deliveryFee`.
- Only `DRAFT` orders can be edited or deleted.

## Distribution Module — Van Loadouts (Slice 3)

Van-sales (mobile selling): stock is loaded from a warehouse onto an agent's vehicle,
sold in the field, then reconciled (returns / damages / cash) at end of day.

### Lifecycle

```
DRAFT ─▶ LOADED ─▶ RECONCILED        (or CANCELLED from DRAFT / LOADED)
```

- Requires a `VEHICLE`-type inventory `Location` per vehicle (set up under Inventory →
  Locations). The loadout targets that location.
- **LOADED** transfers each line's quantity warehouse → vehicle. This is **atomic** — if any
  line lacks stock the whole load fails and the loadout stays `DRAFT`.
- **RECONCILED** takes per-line `returned` + `damaged`; `sold = loaded − returned − damaged`.
  Returns are transferred back to the warehouse; sold and damaged are issued out of the
  vehicle (movement ref `VAN_LOADOUT`). All reconcile stock moves are **atomic** with the
  status change (a failed move aborts the whole reconciliation, leaving it `LOADED` to retry
  safely). `expectedCash = Σ(sold × unitPrice)`, `cashDifference = actualCash − expectedCash`.
- **CANCELLED** from `LOADED` returns the entire load to the warehouse. `RECONCILED` /
  `CANCELLED` are terminal.

### Endpoints

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| GET | /distribution/van-loadouts?status=&agentId= | DISTRIBUTION_VAN_VIEW | List loadouts (paginated) |
| GET | /distribution/van-loadouts/{id} | DISTRIBUTION_VAN_VIEW | Get loadout by ID |
| POST | /distribution/van-loadouts | DISTRIBUTION_VAN_MANAGE | Create loadout (DRAFT) |
| DELETE | /distribution/van-loadouts/{id} | DISTRIBUTION_VAN_MANAGE | Delete a DRAFT loadout |
| POST | /distribution/van-loadouts/{id}/load | DISTRIBUTION_VAN_MANAGE | DRAFT → LOADED (transfer to vehicle) |
| POST | /distribution/van-loadouts/{id}/reconcile | DISTRIBUTION_VAN_MANAGE | LOADED → RECONCILED, body `{ actualCash, lines:[{lineId, quantityReturned, quantityDamaged}] }` |
| POST | /distribution/van-loadouts/{id}/cancel | DISTRIBUTION_VAN_MANAGE | → CANCELLED |

### Create request

```json
{
  "agentId": 5,
  "vehicleLocationId": 20,
  "sourceLocationId": 10,
  "loadoutDate": "2026-07-06",
  "lines": [
    { "productId": 100, "quantityLoaded": 24 },
    { "productId": 101, "quantityLoaded": 12 }
  ]
}
```

- Unit cost and selling price are snapshotted from the product at create time.
- Van sales in this model are cash sales settled through reconciliation; use the
  Distribution **Orders** flow instead when you need per-customer B2B orders + AR.

## Distribution Module — Routes & Visits (Slice 4)

Route plans (an agent's ordered customer stops) and GPS-stamped field visits.

### Routes

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| GET | /distribution/routes | DISTRIBUTION_ROUTE_VIEW | List routes (paginated) |
| GET | /distribution/routes/by-agent/{agentId} | DISTRIBUTION_ROUTE_VIEW | Routes for an agent |
| GET | /distribution/routes/search?q= | DISTRIBUTION_ROUTE_VIEW | Search by name/code |
| GET | /distribution/routes/{id} | DISTRIBUTION_ROUTE_VIEW | Get route by ID |
| POST | /distribution/routes | DISTRIBUTION_ROUTE_MANAGE | Create route |
| PUT | /distribution/routes/{id} | DISTRIBUTION_ROUTE_MANAGE | Update route (a non-null `stops` replaces the set) |
| DELETE | /distribution/routes/{id} | DISTRIBUTION_ROUTE_MANAGE | Delete route |

A route has `code`, `name`, optional `agentId` / `territoryRegionId` / `dayOfWeek`
(`MONDAY`…`SUNDAY`), `status` (`DRAFT`|`ACTIVE`|`ARCHIVED`), and ordered `stops`
(each: `customerId`, `sortOrder`, optional `visitWindowStart`/`visitWindowEnd` (HH:mm),
`latitude`/`longitude`, `address`). Stop `customerName` is snapshotted server-side.

### Visits

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| GET | /distribution/visits?agentId=&routeId=&date= | DISTRIBUTION_VISIT_VIEW | List visits (paginated; filter by agent, route, or day) |
| GET | /distribution/visits/{id} | DISTRIBUTION_VISIT_VIEW | Get visit by ID |
| POST | /distribution/visits/check-in | DISTRIBUTION_VISIT_MANAGE | Record a check-in |
| POST | /distribution/visits/{id}/check-out | DISTRIBUTION_VISIT_MANAGE | Record the outcome + check-out |

- **check-in** body: `{ agentId, customerId, routeId?, routeStopId?, visitType?, latitude?, longitude?, notes? }`.
  `visitType` is `PLANNED`|`AD_HOC`|`RETURN_VISIT`; the visit starts `outcome=PENDING`,
  `checkInAt=now`. `agentId`/`customerId`/`routeId` are tenant-validated; `routeStopId`
  (if given) must belong to `routeId`.
- **check-out** body: `{ outcome, latitude?, longitude?, distributionOrderId?, collectedAmount?, notes? }`.
  `outcome` is `ORDER_PLACED`|`NO_ORDER`|`PAYMENT_COLLECTED`|`RESCHEDULED`|`CLOSED`.
- **Cash collection:** when `collectedAmount > 0`, check-out creates a completed
  (GL-posted) AR payment for the customer, allocated **oldest-due-first** across their
  open invoices (`PENDING`/`SENT`/`PARTIAL`/`OVERDUE`); any excess stays unallocated as a
  customer advance. The visit stores `collectedAmount` + `arPaymentId`, and a repeated
  check-out carrying a collection on a visit that already has one is rejected
  (`COLLECTION_EXISTS`) so money is never double-recorded. These collections roll into the
  agent's KPI `cashCollected`.
- GPS is captured on the mobile/agent side; the admin panel shows coordinates and an
  "open in maps" link (no embedded map).

## Distribution Module — Agent KPIs (Slice 5)

Per-agent performance targets (stored) vs actuals (computed live from orders and visits),
with a revenue leaderboard.

### Targets

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| GET | /distribution/agent-targets/by-agent/{agentId} | DISTRIBUTION_KPI_VIEW | An agent's targets |
| GET | /distribution/agent-targets/{id} | DISTRIBUTION_KPI_VIEW | Get target by ID |
| POST | /distribution/agent-targets | DISTRIBUTION_KPI_MANAGE | Create a target |
| PUT | /distribution/agent-targets/{id} | DISTRIBUTION_KPI_MANAGE | Update target values |
| DELETE | /distribution/agent-targets/{id} | DISTRIBUTION_KPI_MANAGE | Delete target |

A target is `{ agentId, periodType (DAILY|WEEKLY|MONTHLY), periodStart, periodEnd,
targetRevenue, targetOrders, targetVisits, targetNewCustomers, targetCollection, notes }`.
One target per agent per exact `(periodStart, periodEnd)` (unique).

### Dashboard

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| GET | /distribution/kpi/dashboard?from=&to= | DISTRIBUTION_KPI_VIEW | Per-agent KPIs for the period, ordered by revenue (leaderboard) |
| GET | /distribution/kpi/trend?from=&to= | DISTRIBUTION_KPI_VIEW | Daily revenue/orders/visits/collections (zero-filled) for the trend chart |

Returns one row per agent (every agent, zeros if no activity):
`{ agentId, agentName, revenue, orders, visits, cashCollected, customersReached,
targetRevenue, targetOrders, targetVisits, targetNewCustomers, targetCollection,
revenueAchievementPercent, strikeRatePercent, avgDropSize }`.

- **Actuals** are computed live: revenue/orders/cash/distinct-customers from non-cancelled
  distribution orders with `orderDate` in `[from, to]`; visits from check-ins in the same span.
  `cashCollected` sums cash-on-delivery order cash **and** visit AR collections.
- **Efficiency metrics:** `strikeRatePercent = orders / visits × 100` (null when no visits);
  `avgDropSize = revenue / orders` (null when no orders).
- The **target** block is attached only when a target exists for exactly `[from, to]`;
  `revenueAchievementPercent = revenue / targetRevenue × 100` (null when no revenue target).
- **Trend** returns `[{ date, revenue, orders, visits, collected }]` — one entry per day in
  `[from, to]` (missing days zero-filled so the chart has no gaps).
- `from`/`to` are both inclusive dates.

---

# Distribution Module — Agent Mobile API (Slice 7)

A **public, agent-facing** self-service surface for the field-sales mobile app. Mirrors the
web-shop and B2B public pattern: endpoints are anonymous at the Spring Security layer
(whitelisted under `/api/v1/agent/**`); the **tenant** comes from `X-Tenant-ID` on login,
and the **agent identity** from a dedicated bearer token thereafter.

## Auth (phone + SMS OTP)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/v1/agent/auth/request-otp | Send a 6-digit SMS code to the agent's phone |
| POST | /api/v1/agent/auth/verify | Exchange phone+code for a bearer token |
| GET | /api/v1/agent/me | The authenticated agent's profile |

- **request-otp** body `{ phone }`. Always returns 200 (never reveals whether a phone is
  registered); an SMS is sent only if the phone maps to an **ACTIVE** agent in the header
  tenant. Abuse limits mirror the shop OTP: per-IP limiter, 60 s resend cooldown, ≤5
  codes/day/phone, 5-minute code expiry, 5 wrong attempts per code. Only the salted SHA-256
  hash is stored (`distribution_agent_otps`, V88).
- **verify** body `{ phone, code }` → `{ token, agentId, code, name, phone }`. The phone must
  resolve to exactly one active agent; an ambiguous phone (shared by >1 active agent) is
  rejected (`AGENT_PHONE_AMBIGUOUS`) rather than logging in the wrong person.
- **Token**: a JWT signed with a key **derived** from the staff secret
  (`secret + "::distribution-agent"`), so it can never pass the staff / web / B2B signature
  check. Subject=agentId, claims tenantId+code, 30-day expiry (`app.jwt.agent-expiration`).
  The tenant is taken from the token on every request — never a header — and a
  `SUSPENDED`/`TERMINATED` agent is rejected at use time even with a still-valid token.

## Portal (token-guarded, `/api/v1/agent/me`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /me/summary | Today's snapshot: routes, visitsToday, hasActiveLoadout |
| GET | /me/routes | The agent's assigned routes |
| GET | /me/visits | The agent's visits (paginated) |
| GET | /me/loadout/current | The agent's current (most recent LOADED) van loadout, or null |
| GET | /me/orders | The agent's distribution orders (paginated) |
| POST | /me/orders | Place an order from the field (see below) |
| POST | /me/visits/check-in | Record a check-in `{ customerId, routeId?, routeStopId?, visitType?, latitude?, longitude?, notes? }` |
| POST | /me/visits/{id}/check-out | Record outcome + optional cash collection `{ outcome, latitude?, longitude?, distributionOrderId?, collectedAmount?, notes? }` |

- Every endpoint scopes strictly to `(tenant, agentId)` from the token — a client **never**
  supplies an agentId. Check-in forces the agent to the token holder; check-out loads the
  visit under the token's tenant and 404s if it belongs to another agent (no cross-agent edit).
- **Cash collection** on check-out reuses the staff flow: `collectedAmount` creates a
  completed, GL-posted AR payment allocated oldest-due-first across the customer's open
  invoices, idempotent per visit. See "Visits" (Slice 4) for the full contract.
- **Field order placement** — `POST /me/orders` body
  `{ customerId, visitId?, routeId?, paymentMethod?, paymentTermsDays?, discountAmount?,
  deliveryFee?, deliveryAddress?, notes?, confirmNow?, lines:[{ productId, quantity,
  discountPercent? }] }`. There is **no agentId** (forced to the token holder) and **no
  sourceLocationId**: the sale defaults to drawing down the agent's current van loadout
  location. Prices are resolved **server-side** (the client's numbers are ignored); an
  attached `visitId` must be the agent's own. The order is created `DRAFT`; set
  `confirmNow: true` to immediately CONFIRM it (reserving van stock) — the typical van-sale
  capture. Fulfilment (deliver → invoice, with the cash/credit split and AR bridge)
  continues through the normal order lifecycle.

---

# B2B Marketplace — Public API Reference (Slice 6)

A **public, customer-facing** self-service ordering portal for wholesale (B2B) buyers,
consumed by a separate buyer app/site (not the staff admin panel). Mirrors the mobile-shop
public pattern: endpoints are anonymous at the Spring Security layer (whitelisted under
`/api/v1/b2b/**`); the **tenant** comes from the `X-Tenant-ID` header and the **buyer identity**
from a dedicated B2B JWT validated inside the services.

## Conventions

- **Base URL**: `/api/v1/b2b`
- **Tenant**: every request must send `X-Tenant-ID: <tenantId>`.
- **Auth**: after login, send `Authorization: Bearer <b2b-token>` on all other calls. The
  token is a dedicated JWT (signed with a key derived from the staff secret so it can never
  cross-validate as a staff or web-customer token); it carries the buyer's finance-customer id
  and tenant. 30-day expiry.
- The buyer is an existing finance **Customer** (with its price list, credit terms, currency).

## Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | /b2b/auth/login | tenant only | Log in with `{ code, phone }` → `{ token, customerId, code, name, currency, availableCredit }` |
| GET | /b2b/me | bearer | Buyer profile + credit info |
| GET | /b2b/catalog?search=&categoryId=&page= | bearer | Active sellable products priced at the buyer's price list |
| GET | /b2b/catalog/{productId} | bearer | Single product priced for the buyer |
| GET | /b2b/orders | bearer | The buyer's own distribution orders (paginated) |
| GET | /b2b/orders/{orderNumber} | bearer | One of the buyer's orders |
| POST | /b2b/orders | bearer | Place an order → creates a **DRAFT** distribution order |

### Login

```
POST /b2b/auth/login      (X-Tenant-ID: 1)
{ "code": "C-00042", "phone": "+998 90 111-22-33" }
```

- Auth is **code + registered phone** (both required; phone matched on digits only). Unknown
  code and wrong phone both return the same `401 Invalid code or phone` (no user enumeration).
- *Note:* this is a shared-secret login suited to a first B2B release; it can be upgraded to
  phone-OTP (reusing the web-shop OTP infrastructure) without changing the downstream contract.

### Place order

```
POST /b2b/orders          (X-Tenant-ID: 1, Authorization: Bearer <token>)
{
  "deliveryAddress": "Chilonzor 5",
  "notes": null,
  "lines": [
    { "productId": 10, "quantity": 3 },
    { "productId": 11, "quantity": 2 }
  ]
}
```

- **Unit prices are resolved server-side** against the buyer's price list; the client sends
  only `productId` + `quantity`. The order is created **DRAFT** (payment method `CREDIT`,
  terms from the customer) for staff to review, confirm and fulfil via the Distribution
  Orders flow (slice 2). Ordering is blocked (`CREDIT_HOLD`) while the account is on credit hold.
- Buyers can only read their own orders (a foreign order number returns `404`).

---

# Mobile Shop App — Public API Reference

This is the **complete, authoritative contract for the customer-facing shop mobile app**.
Every endpoint the app consumes is documented below with request and response examples.
The staff/admin-facing management endpoints (`/api/v1/web-catalog`, `/web-orders`,
`/web-customers`, `/web-campaigns`) are documented in the sections that follow — the app
never calls those.

## Conventions

**Base URL**
```
https://<host>/api/v1/web
```

**Tenant resolution.** Every request carries the shop's tenant in a header. Anonymous
endpoints read it directly; authenticated endpoints also derive it from the token but the
header should still be sent.
```
X-Tenant-ID: 1
```
The header is **required** on anonymous shop calls: if neither it nor a customer token
identifies the tenant, the request is rejected with `400 TENANT_REQUIRED` (fail closed —
single-shop installs must send their tenant id too).

**Response envelope.** Single-object endpoints wrap the payload in the standard envelope:
```json
{ "success": true, "message": "Optional", "data": { }, "timestamp": "2026-06-11T12:00:00Z" }
```
Paged endpoints return a `content` array plus `page` metadata (no `success` envelope):
```json
{
  "content": [ ],
  "page": { "number": 0, "size": 20, "totalElements": 42, "totalPages": 3,
            "first": true, "last": false, "empty": false }
}
```

**Authentication.** Two kinds of access:
- **Anonymous** — catalog browse, cart price/coupon preview, checkout, order status,
  delivery lookups, and OTP request/verify.
- **Web-customer JWT** — everything under `/me/**`. Obtained from `POST /web/auth/verify`
  and sent as `Authorization: Bearer <token>`. These tokens are signed with a key *derived
  from* the staff secret, so they can never authenticate against staff endpoints (and staff
  JWTs are rejected on `/web/me/**`). A missing/expired/invalid token on a `/me` endpoint
  returns `401`.

**Backward-compatibility rule.** Changes to these endpoints must be **additive only** —
installed apps cannot be force-updated. New fields may appear; existing fields never change
meaning or disappear.

**Errors.** Error envelope with HTTP status:
```json
{ "success": false, "message": "Купон яроқсиз ёки муддати ўтган",
  "error": { "code": "COUPON_INVALID" }, "timestamp": "2026-06-13T08:00:00Z" }
```
`message` is **localized (Uzbek) and user-safe** — show it directly to the customer. Internal
detail and stack traces are never exposed; unknown errors collapse to a generic localized
message by status. `error.code` is for programmatic handling (it's stable; the `message`
wording may change). If `message` is ever absent, fall back to a generic message by status.

Common statuses: `400` validation/business error, `401` missing/invalid customer token,
`404` not found (or hidden/draft item, or order whose phone doesn't match), `429` rate
limited, `503` payment provider not configured, `201` created (checkout only).

> **Payments are cash-on-delivery only for now.** The card-payment endpoints below are
> documented for when a provider (Payme/Click/Uzum) is enabled; until then
> `POST /web/orders/{orderNumber}/payment` returns `503 PAYMENT_NOT_CONFIGURED` and checkout
> always creates orders as `CASH`/`NONE`.

Known `error.code` values: `VALIDATION_FAILED`, `COUPON_INVALID`, `PRODUCT_UNAVAILABLE`,
`OTP_INVALID`, `OTP_EXPIRED`, `INVALID_PHONE`, `ORDER_ALREADY_PAID`, `ORDER_NOT_PAYABLE`,
`INVALID_DELIVERY`, `NOT_FOUND`, `UNAUTHORIZED`, `TOO_MANY_REQUESTS`,
`PAYMENT_NOT_CONFIGURED`, `TENANT_REQUIRED`, `INTERNAL_ERROR`.

**Money & quantities.** All amounts are `UZS` decimals. `currency` is always `"UZS"`.
Quantities allow up to 3 decimals (`0.001`–`10000`).

---

## 1. Catalog browse

### GET /web/catalog/products
Paged list of **LIVE** items. Anonymous.

**Query params:** `search` (name filter, optional), `categoryId` (optional),
`page` (default 0), `size` (default 20).

**Request:**
```http
GET /api/v1/web/catalog/products?search=cola&categoryId=3&page=0&size=20
X-Tenant-ID: 1
```

**Response:** `200 OK`
```json
{
  "content": [
    {
      "id": 1,
      "name": "Cola Bottle",
      "shortDescription": "0.5L",
      "description": "Chilled cola, 0.5 litre bottle",
      "price": 12000.0,
      "salePrice": 10200.0,
      "promotionLabel": "-15%",
      "currency": "UZS",
      "categoryId": 3,
      "categoryName": "Drinks",
      "brandName": "Coca-Cola",
      "unitName": "Pieces",
      "inStock": true,
      "fractional": false,
      "step": 1,
      "imageUrl": "/uploads/products/1/main.jpg",
      "images": ["/uploads/products/1/main.jpg"]
    }
  ],
  "page": { "number": 0, "size": 20, "totalElements": 1, "totalPages": 1,
            "first": true, "last": true, "empty": false }
}
```
`salePrice` and `promotionLabel` are **null/omitted unless an active WEB-channel percentage
promotion applies** (computed server-side, cached up to 60s). Show `salePrice` with the
original `price` struck through. The badge is a single-unit preview — the authoritative
cart discount comes from `POST /web/cart/price` and from checkout. Cost/wholesale prices and
exact stock quantities are **never** exposed — only the `inStock` flag.
`fractional`/`step` describe how the item is ordered: when `fractional` is `true` the app
shows a decimal quantity stepper incrementing by `step` (e.g. `0.5` kg); otherwise `step` is
`1` (whole units). Order lines already accept decimal `quantity` (0.001–10000).

### GET /web/catalog/products/{id}
Single LIVE item detail. Anonymous. Returns `404` for draft/hidden items.

**Request:**
```http
GET /api/v1/web/catalog/products/1
X-Tenant-ID: 1
```

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "id": 1, "name": "Cola Bottle", "shortDescription": "0.5L",
    "description": "Chilled cola, 0.5 litre bottle",
    "price": 12000.0, "salePrice": 10200.0, "promotionLabel": "-15%",
    "currency": "UZS", "categoryId": 3, "categoryName": "Drinks",
    "brandName": "Coca-Cola", "unitName": "Pieces", "inStock": true,
    "imageUrl": "/uploads/products/1/main.jpg",
    "images": ["/uploads/products/1/main.jpg"]
  },
  "timestamp": "2026-06-11T12:00:00Z"
}
```

### GET /web/catalog/categories
Categories that contain at least one LIVE item. Anonymous.

**Request:**
```http
GET /api/v1/web/catalog/categories
X-Tenant-ID: 1
```

**Response:** `200 OK`
```json
{
  "success": true,
  "data": [
    { "id": 3, "name": "Drinks" },
    { "id": 5, "name": "Snacks" }
  ],
  "timestamp": "2026-06-11T12:00:00Z"
}
```

---

## 2. Cart pricing & coupons

These are **display-only previews** — checkout recomputes everything server-side. An optional
`Authorization: Bearer <token>` personalises customer-specific promotion conditions and binds
per-customer coupon limits.

### POST /web/cart/price
Cart pricing preview with promotion discounts. Anonymous (token optional).
**Rate limited 5 calls / 10s per IP** (`429`). Max 50 lines.

**Request:**
```http
POST /api/v1/web/cart/price
X-Tenant-ID: 1
Authorization: Bearer <token>   # optional
Content-Type: application/json

{
  "lines": [
    { "catalogItemId": 1, "quantity": 2 },
    { "catalogItemId": 5, "quantity": 1 }
  ]
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "lines": [
      { "catalogItemId": 1, "productName": "Cola Bottle", "quantity": 2,
        "unitPrice": 12000.0, "lineTotal": 24000.0 },
      { "catalogItemId": 5, "productName": "Chips", "quantity": 1,
        "unitPrice": 8000.0, "lineTotal": 8000.0 }
    ],
    "subtotal": 32000.0,
    "discountTotal": 3600.0,
    "total": 28400.0,
    "currency": "UZS",
    "appliedPromotions": ["15% off Drinks"]
  },
  "timestamp": "2026-06-11T12:00:00Z"
}
```
`appliedPromotions` lists promotion **names only** — never conditions or usage counters.

### POST /web/cart/validate-coupon
Coupon check before checkout (the discount depends on the cart). Anonymous (token optional).
**Strictly rate limited 5/min per IP** (`429`) — coupon codes are guessable.

**Request:**
```http
POST /api/v1/web/cart/validate-coupon
X-Tenant-ID: 1
Content-Type: application/json

{
  "code": "SAVE10",
  "lines": [ { "catalogItemId": 1, "quantity": 2 } ]
}
```

**Response (valid):** `200 OK`
```json
{
  "success": true,
  "data": { "couponCode": "SAVE10", "valid": true, "discount": 2400.0 },
  "timestamp": "2026-06-11T12:00:00Z"
}
```

**Response (invalid):** `200 OK`
```json
{
  "success": true,
  "data": { "couponCode": "SAVE10", "valid": false, "discount": 0 },
  "timestamp": "2026-06-11T12:00:00Z"
}
```
Every invalid reason — unknown, expired, depleted, wrong channel, below min order — produces
the **same** generic `valid: false`, so the endpoint can't be used to probe codes.

---

## 3. Checkout & orders

### POST /web/orders
Anonymous checkout. Tenant from `X-Tenant-ID`. Returns `201 Created`.
Max 50 lines, qty `0.001`–`10000`, products must be LIVE. **Prices and discounts are always
computed server-side.** Rate limited per IP+phone (5/min → `429`).

**Request:**
```http
POST /api/v1/web/orders
X-Tenant-ID: 1
Content-Type: application/json

{
  "customerName": "Ali Valiyev",
  "phone": "+998901234567",
  "regionId": 2,
  "villageId": 14,
  "address": "Chilonzor 5, dom 12, kv 3",
  "note": "Call on arrival",
  "couponCode": "SAVE10",
  "pointsToSpend": 5000,
  "lines": [
    { "catalogItemId": 1, "quantity": 2 },
    { "catalogItemId": 5, "quantity": 1 }
  ]
}
```
`regionId`, `villageId`, `address`, `note`, `couponCode`, `pointsToSpend`
are all optional. `address` is the free-text delivery address (street/house/landmark) — the
app makes it required in its UI; the backend stores it and shows it to fulfilment.
**Payment is cash-on-delivery only for now**: every order is created `CASH` / payment status
`NONE`. Any `paymentMethod` field sent by the client is ignored (no order is parked awaiting an
online-payment flow). `pointsToSpend` redeems loyalty points (capped server-side by the shop's
max-redeem-percent and the customer's balance; ignored if the loyalty program is off or the
phone has no account). An invalid `couponCode` **rejects** the checkout with `400` (never
silently drops the discount).

On a successful order the tenant's staff are notified over three isolated channels: a durable
in-app alert (`ORDER_PLACED`, priority `HIGH`, `entityType: "WEB_ORDER"`, `entityId`: the order
id) for every active staff member — visible in the admin app's alerts feed and unread count,
surviving a missed push; a Telegram broadcast (to staff who linked Telegram); and an APNs push to
every registered admin device (`type: "new_order"`, `id`: the order id). Notification failures on
any channel never affect the checkout result or the other channels.

**Response:** `201 Created`
```json
{
  "success": true,
  "data": {
    "orderNumber": "WO-000007",
    "status": "NEW",
    "paymentMethod": "CASH",
    "paymentStatus": "NONE",
    "address": "Chilonzor 5, dom 12, kv 3",
    "deliveryFee": 10000.0,
    "discountTotal": 3600.0,
    "couponCode": "SAVE10",
    "couponDiscount": 2400.0,
    "pointsSpent": 5000.0,
    "totalAmount": 27400.0,
    "currency": "UZS",
    "createdAt": "2026-06-11T12:00:00Z",
    "lines": [
      { "productName": "Cola Bottle", "quantity": 2, "unitPrice": 12000.0, "lineTotal": 24000.0 },
      { "productName": "Chips", "quantity": 1, "unitPrice": 8000.0, "lineTotal": 8000.0 }
    ]
  },
  "timestamp": "2026-06-11T12:00:00Z"
}
```
Line `productName`/`unitPrice` are snapshotted **at full price**; promotion, coupon and
points discounts live at order level:
`totalAmount = Σ lineTotal − discountTotal − couponDiscount − pointsSpent + deliveryFee`.
`discountTotal` is promotions only; the coupon discount is separate (`couponDiscount`) — do
**not** add them twice.
Order lifecycle: `NEW → CONFIRMED → DELIVERING → COMPLETED` (cancellable until completed).

**Payment fields:** `paymentMethod` is `CASH` | `CARD`. `paymentStatus` is
`NONE` | `PENDING` | `PAID` | `FAILED` | `CANCELLED` | `REFUNDED` — `NONE` for cash orders,
`PENDING` for a fresh card order (until the card-payment flow confirms it), and `REFUNDED`
when a paid order is later cancelled. Absent/empty → show no payment state (legacy-safe).

### GET /web/orders/{orderNumber}?phone=
Order status lookup. Anonymous, but the `phone` must match the order (`404` otherwise) — this
is the "track my order" screen for guests without an account. **Rate limited per IP** (≈5 /
10s → `429`): order numbers are sequential and phone-only auth is weak, so this throttles
enumeration while staying comfortable for one customer polling their order.

**Request:**
```http
GET /api/v1/web/orders/WO-000007?phone=+998901234567
X-Tenant-ID: 1
```

**Response:** `200 OK` — same `PublicOrderDto` shape as checkout (current `status`,
`paymentStatus`).

### POST /web/orders/{orderNumber}/payment
Start an online card payment for an existing order. Anonymous; the `phone` in the body must
match the order (same guard as the status lookup) — no phone/order number in the polling URL.

**Request:**
```http
POST /api/v1/web/orders/WO-000042/payment
X-Tenant-ID: 1
Content-Type: application/json

{ "phone": "+998901234567", "provider": "PAYME", "returnUrl": "https://app.example/return" }
```
`provider` is `PAYME` | `CLICK` | `UZUM`. `returnUrl` is optional and **must be HTTPS**
(anything else is dropped). The amount is taken from the order server-side — never the client.

**Response:** `201 Created`
```json
{
  "success": true,
  "data": {
    "id": "pay_4f3c2a1b9d8e7f60",
    "paymentUrl": "https://checkout.paycom.uz/<base64>",
    "status": "PENDING",
    "provider": "PAYME",
    "amount": 33000.0
  },
  "timestamp": "2026-06-13T08:00:00Z"
}
```
- `id` — opaque token; poll it via `GET /web/payments/{id}`.
- `paymentUrl` — always HTTPS; open it in a browser/WebView.
- `503 PAYMENT_NOT_CONFIGURED` if the chosen provider has no merchant credentials in this
  environment — the app should fall back (e.g. offer cash on delivery), not treat it as fatal.
- `400` if the order is cancelled or already paid.

### GET /web/payments/{id}
Poll a payment's status by its opaque id. Anonymous (the id carries no PII).

**Response:** `200 OK`
```json
{
  "success": true,
  "data": { "id": "pay_4f3c2a1b9d8e7f60", "status": "PAID", "provider": "PAYME", "amount": 33000.0 },
  "timestamp": "2026-06-13T08:01:00Z"
}
```
`status`: `PENDING` (keep waiting) · `PAID` (success — show receipt; the order's
`paymentStatus` also becomes `PAID`) · `FAILED` / `CANCELLED` (terminal — offer retry).

> **Provider wiring status.** The redirect URL is built per each provider's documented
> hosted-checkout method (Payme GET-init, Click pay-link, Uzum). Confirmation currently
> arrives via the staff **confirm-payment** action (`POST /web-orders/{id}/confirm-payment`);
> the asynchronous provider webhooks (Payme JSON-RPC, Click prepare/complete) plug into the
> same `WebPaymentService.markPaid(...)` hook and are wired once a merchant account + sandbox
> credentials are available. Every provider is **disabled by default** (`503`) until
> `hisobnoma.payment.<provider>.enabled=true` with a merchant id, so no environment can ever
> fake a successful payment.

### GET /web/delivery/regions
Active delivery regions for the checkout form. Anonymous.

**Request:**
```http
GET /api/v1/web/delivery/regions
X-Tenant-ID: 1
```

**Response:** `200 OK`
```json
{
  "success": true,
  "data": [
    { "id": 2, "name": "Yunusobod", "deliveryFee": 10000.0 },
    { "id": 3, "name": "Chilonzor", "deliveryFee": 12000.0 }
  ],
  "timestamp": "2026-06-11T12:00:00Z"
}
```

### GET /web/delivery/villages?regionId=
Active villages, optionally filtered by region. Anonymous.

**Request:**
```http
GET /api/v1/web/delivery/villages?regionId=2
X-Tenant-ID: 1
```

**Response:** `200 OK`
```json
{
  "success": true,
  "data": [
    { "id": 14, "name": "Bodomzor", "regionId": 2 },
    { "id": 15, "name": "Minor", "regionId": 2 }
  ],
  "timestamp": "2026-06-11T12:00:00Z"
}
```

---

## 4. Authentication (phone + SMS OTP)

### POST /web/auth/request-otp
Sends a 6-digit SMS code. Anonymous. Limits: 60s cooldown per phone, max 5 codes/day/phone,
per-IP rate limit (`429`). Codes are stored salted+hashed and expire in 5 minutes.

**Request:**
```http
POST /api/v1/web/auth/request-otp
X-Tenant-ID: 1
Content-Type: application/json

{ "phone": "+998901234567" }
```

**Response:** `200 OK`
```json
{ "success": true, "message": "Code sent", "timestamp": "2026-06-11T12:00:00Z" }
```

### POST /web/auth/verify
Verifies the code and returns the customer token. Anonymous. 5 wrong attempts lock the code
(`429`); a new code must then be requested. On **first** login, `name` is stored and an
optional `referralCode` is applied (credits the referrer once this customer's first order
completes).

**Request:**
```http
POST /api/v1/web/auth/verify
X-Tenant-ID: 1
Content-Type: application/json

{
  "phone": "+998901234567",
  "code": "123456",
  "name": "Ali Valiyev",
  "referralCode": "ALI7K2"
}
```
`name` and `referralCode` are optional.

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "phone": "+998901234567",
    "name": "Ali Valiyev"
  },
  "timestamp": "2026-06-11T12:00:00Z"
}
```
Store `token` and send it as `Authorization: Bearer <token>` on all `/me/**` calls.

---

## 5. Customer profile (`/me`)

All `/me/**` endpoints require `Authorization: Bearer <token>`; a missing/invalid token → `401`.

### GET /web/me
Current customer's basic profile.

**Request:**
```http
GET /api/v1/web/me
Authorization: Bearer <token>
X-Tenant-ID: 1
```

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "phone": "+998901234567",
    "name": "Ali Valiyev",
    "customerCode": "WC-00042",
    "tenantSlug": "hisobnoma"
  },
  "timestamp": "2026-06-11T12:00:00Z"
}
```

`customerCode` is a stable public identity code assigned on first verification
(globally unique, never PII). `tenantSlug` is the tenant's short code. Together
they back the in-app wallet QR, which encodes `{base}/{tenantSlug}/{customerCode}`
(e.g. `https://hisobnoma.uz/w/hisobnoma/WC-00042`). Both are always present;
older accounts are backfilled.

### GET /web/me/orders
Paged list of the customer's own orders (matched by verified phone), newest first.

**Request:**
```http
GET /api/v1/web/me/orders?page=0&size=20
Authorization: Bearer <token>
X-Tenant-ID: 1
```

**Response:** `200 OK` — paged `PublicOrderDto` (same shape as checkout):
```json
{
  "content": [
    {
      "orderNumber": "WO-000007", "status": "CONFIRMED",
      "deliveryFee": 10000.0, "discountTotal": 3600.0,
      "couponCode": "SAVE10", "couponDiscount": 2400.0, "pointsSpent": 5000.0,
      "totalAmount": 27400.0, "currency": "UZS", "createdAt": "2026-06-11T12:00:00Z",
      "lines": [
        { "productName": "Cola Bottle", "quantity": 2, "unitPrice": 12000.0, "lineTotal": 24000.0 }
      ]
    }
  ],
  "page": { "number": 0, "size": 20, "totalElements": 1, "totalPages": 1,
            "first": true, "last": true, "empty": false }
}
```

### GET /web/me/loyalty
The customer's loyalty/cashback balance and recent ledger.

**Request:**
```http
GET /api/v1/web/me/loyalty
Authorization: Bearer <token>
X-Tenant-ID: 1
```

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "balance": 18500.0,
    "enabled": true,
    "minRedeem": 5000.0,
    "maxRedeemPercent": 30,
    "entries": [
      { "id": 12, "type": "EARN", "amount": 1200.0, "orderNumber": "WO-000007",
        "note": null, "expiresAt": "2026-12-08T00:00:00Z", "createdAt": "2026-06-11T12:05:00Z" },
      { "id": 9, "type": "SPEND", "amount": -5000.0, "orderNumber": "WO-000007",
        "note": null, "expiresAt": null, "createdAt": "2026-06-11T12:00:00Z" }
    ]
  },
  "timestamp": "2026-06-11T12:00:00Z"
}
```
`balance` is the aggregate of the append-only ledger. When `enabled` is `false` the program
is off (`balance: 0`, empty `entries`) and `pointsToSpend` at checkout is ignored. Redemption
rules: `minRedeem` is the minimum spendable amount; points can cover at most
`maxRedeemPercent`% of an order. `type` ∈ `EARN | SPEND | EXPIRE | ADJUST`.

---

## 6. Device tokens (push)

Register the device's FCM token after login (and on app resume / token refresh) so the shop
can send order-status and wishlist push notifications. Phase 5 push deep-links into the app.

### POST /web/me/device-token
Register (idempotent) the current device's push token.

**Request:**
```http
POST /api/v1/web/me/device-token
Authorization: Bearer <token>
X-Tenant-ID: 1
Content-Type: application/json

{ "token": "fcm-device-token-abc123", "platform": "android" }
```
`platform` is a free-form tag (`android` / `ios`), max 20 chars; `token` max 500 chars.

**Response:** `200 OK`
```json
{ "success": true, "message": "Device token registered", "timestamp": "2026-06-11T12:00:00Z" }
```

### DELETE /web/me/device-token
Remove a device token (call on logout). Optional `token` query param removes a single token;
omit it to remove all of the customer's tokens.

**Request:**
```http
DELETE /api/v1/web/me/device-token?token=fcm-device-token-abc123
Authorization: Bearer <token>
X-Tenant-ID: 1
```

**Response:** `200 OK`
```json
{ "success": true, "message": "Device token removed", "timestamp": "2026-06-11T12:00:00Z" }
```

---

## 7. Referral code

### GET /web/me/referral-code
The customer's own referral code (created on first request, then stable). Share it; new users
enter it as `referralCode` in `POST /web/auth/verify`, and both sides are rewarded when the
referred customer's first order completes.

**Request:**
```http
GET /api/v1/web/me/referral-code
Authorization: Bearer <token>
X-Tenant-ID: 1
```

**Response:** `200 OK`
```json
{ "success": true, "data": { "code": "ALI7K2" }, "timestamp": "2026-06-11T12:00:00Z" }
```

### GET /web/me/referral-stats
Full referral dashboard: the customer's code, whether the referral programme is enabled for this
tenant, number of people invited, and total loyalty points earned from referrals.

**Request:**
```http
GET /api/v1/web/me/referral-stats
Authorization: Bearer <token>
X-Tenant-ID: 1
```

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "code": "ALI7K2",
    "enabled": true,
    "invitedCount": 3,
    "pointsEarned": 15000.0000
  },
  "timestamp": "2026-06-11T12:00:00Z"
}
```

---

## 8. Notifications feed

Push notifications are now persisted so customers can review them later. Every push sent via
`WebPushService.sendToCustomer()` is automatically stored in `web_notifications`.

### GET /web/me/notifications
Paged notification history, newest first. Each entry has a `type` (e.g. `ORDER_STATUS`,
`GENERAL`), optional `referenceType`/`referenceId` for deep-linking, and a `read` flag.

**Request:**
```http
GET /api/v1/web/me/notifications?page=0&size=20
Authorization: Bearer <token>
X-Tenant-ID: 1
```

**Response:** `200 OK`
```json
{
  "content": [
    {
      "id": 42,
      "title": "Буюртма тасдиқланди",
      "body": "Буюртма WO-001 қабул қилинди",
      "type": "ORDER_STATUS",
      "referenceType": null,
      "referenceId": "WO-001",
      "read": false,
      "createdAt": "2026-06-11T10:30:00Z"
    }
  ],
  "page": { "number": 0, "size": 20, "totalElements": 1, "totalPages": 1,
            "first": true, "last": true, "empty": false }
}
```

### GET /web/me/notifications/unread-count
Badge counter — lightweight endpoint to poll from the app home screen.

**Request:**
```http
GET /api/v1/web/me/notifications/unread-count
Authorization: Bearer <token>
X-Tenant-ID: 1
```

**Response:** `200 OK`
```json
{ "success": true, "data": { "count": 5 }, "timestamp": "2026-06-11T12:00:00Z" }
```

### PUT /web/me/notifications/{id}/read
Mark a single notification as read.

**Request:**
```http
PUT /api/v1/web/me/notifications/42/read
Authorization: Bearer <token>
X-Tenant-ID: 1
```

**Response:** `200 OK`
```json
{ "success": true, "timestamp": "2026-06-11T12:00:00Z" }
```

### PUT /web/me/notifications/read-all
Mark all unread notifications as read.

**Request:**
```http
PUT /api/v1/web/me/notifications/read-all
Authorization: Bearer <token>
X-Tenant-ID: 1
```

**Response:** `200 OK`
```json
{ "success": true, "timestamp": "2026-06-11T12:00:00Z" }
```

---

## 9. Coupons

### GET /web/me/coupons
Lists all coupon codes currently valid for this customer. Includes only coupons whose parent
promotion targets the `WEB` or `ALL` channel, is within date range, hasn't been fully used,
and hasn't exceeded this customer's per-customer limit. Customer-specific coupons (assigned
by staff) are included alongside general ones.

**Request:**
```http
GET /api/v1/web/me/coupons
Authorization: Bearer <token>
X-Tenant-ID: 1
```

**Response:** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "code": "SUMMER25",
      "description": "Summer sale — 25% off everything",
      "discountType": "PERCENT",
      "discountValue": 25.0000,
      "minOrderAmount": 50000.0000,
      "startDate": "2026-06-01",
      "endDate": "2026-08-31"
    },
    {
      "code": "VIP5000",
      "description": "Loyal customer bonus",
      "discountType": "FIXED",
      "discountValue": 5000.0000,
      "minOrderAmount": null,
      "startDate": null,
      "endDate": null
    }
  ],
  "timestamp": "2026-06-11T12:00:00Z"
}
```
Apply a coupon at checkout via `POST /web/cart/validate-coupon` (existing endpoint).

---

## 10. Wishlist ("like")

Customers tap a heart on any product. Wishlisted products that get a web discount or come
back in stock trigger a push/SMS alert (see Phase 6). Anonymous taps should route to the
login screen, then complete the like.

### GET /web/me/wishlist
Paged wishlist, each entry rendered with current price/sale/availability, newest first.

**Request:**
```http
GET /api/v1/web/me/wishlist?page=0&size=20
Authorization: Bearer <token>
X-Tenant-ID: 1
```

**Response:** `200 OK`
```json
{
  "content": [
    {
      "catalogItemId": 1,
      "name": "Cola Bottle",
      "price": 12000.0,
      "salePrice": 10200.0,
      "promotionLabel": "-15%",
      "inStock": true,
      "available": true,
      "imageUrl": "/uploads/products/1/main.jpg",
      "addedAt": "2026-06-10T09:30:00Z",
      "priceDrop": true
    }
  ],
  "page": { "number": 0, "size": 20, "totalElements": 1, "totalPages": 1,
            "first": true, "last": true, "empty": false }
}
```
`available` is the strict catalog rule (LIVE + active + sellable + in stock); `inStock` is the
stock flag alone. `priceDrop: true` means the sale price dropped since the customer was last
notified — show a "нарх тушди" badge. A wishlisted item whose catalog entry was deleted comes
back with `name: "(ўчирилган)"` and `available: false`.

### GET /web/me/wishlist/ids
Lightweight id list so the app can paint hearts on the catalog grid without N calls. Fetch
once after login, refresh on app resume.

**Request:**
```http
GET /api/v1/web/me/wishlist/ids
Authorization: Bearer <token>
X-Tenant-ID: 1
```

**Response:** `200 OK`
```json
{ "success": true, "data": [1, 5, 42], "timestamp": "2026-06-11T12:00:00Z" }
```

### PUT /web/me/wishlist/{catalogItemId}
Like a product (idempotent — liking twice is a no-op). The item must exist for the tenant
(DRAFT items are allowed — they may come back). Unknown item → `400`.

**Request:**
```http
PUT /api/v1/web/me/wishlist/1
Authorization: Bearer <token>
X-Tenant-ID: 1
```

**Response:** `200 OK`
```json
{ "success": true, "message": "Liked", "timestamp": "2026-06-11T12:00:00Z" }
```

### DELETE /web/me/wishlist/{catalogItemId}
Unlike a product (idempotent).

**Request:**
```http
DELETE /api/v1/web/me/wishlist/1
Authorization: Bearer <token>
X-Tenant-ID: 1
```

**Response:** `200 OK`
```json
{ "success": true, "message": "Unliked", "timestamp": "2026-06-11T12:00:00Z" }
```

---

## Web Catalog (Online Shop) APIs

The web catalog is the curated **draft/live item list** shown in the store's customer mobile app.
Staff manage the list through authenticated endpoints; the mobile app reads the live list through
public (anonymous) endpoints. See `docs/WEB_SHOP_PLAN.md` for the full roadmap.

### Public endpoints (no authentication — mobile app contract)

These live under the whitelisted `/api/v1/web/**` prefix. Tenant is resolved from the optional
`X-Tenant-ID` header (defaults to `1`). **Backward compatibility rule:** changes to these
endpoints must be additive only — installed mobile apps cannot be force-updated.

> Full request/response examples are in
> **[Mobile Shop App — Public API Reference](#mobile-shop-app--public-api-reference)** above.

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /web/catalog/products | Paged list of LIVE items. Params: `search`, `categoryId`, `page`, `size` |
| GET | /web/catalog/products/{id} | Single LIVE item detail (404 for draft/hidden items) |
| GET | /web/catalog/categories | Categories that contain at least one LIVE item |

Public product payload (customer-safe fields only — never cost price, wholesale price or
stock quantities):
```json
{
  "id": 1,
  "name": "Cola Bottle",
  "shortDescription": "0.5L",
  "description": "…",
  "price": 12000.0,
  "salePrice": 10200.0,
  "promotionLabel": "-15%",
  "currency": "UZS",
  "categoryId": 3,
  "categoryName": "Drinks",
  "brandName": "Coca-Cola",
  "unitName": "Pieces",
  "inStock": true,
  "imageUrl": "/uploads/products/1/main.jpg",
  "images": ["/uploads/products/1/main.jpg"]
}
```

`salePrice` and `promotionLabel` are **null/omitted unless an active WEB-channel
percentage promotion applies** to the product (computed server-side, cached up to 60s).
Clients should show `salePrice` with the original `price` struck through. The badge is a
preview for a single unit — the authoritative discount for a full cart comes from
`POST /web/cart/price` and from checkout itself.

### Staff endpoints (authenticated)

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| GET | /web-catalog | WEB_CATALOG_VIEW | Paged admin list (all items, drafts included). Param: `search` |
| GET | /web-catalog/counts | WEB_CATALOG_VIEW | `{ live, draft, total }` counters |
| POST | /web-catalog/items | WEB_CATALOG_MANAGE | Add products: `{ "productIds": [1, 2] }`. Existing products are skipped; new items start as DRAFT |
| PUT | /web-catalog/items/{id} | WEB_CATALOG_MANAGE | Set overrides: `{ "displayName": "…", "priceOverride": 9999.5 }` (null clears) |
| DELETE | /web-catalog/items/{id} | WEB_CATALOG_MANAGE | Remove item from the catalog |
| POST | /web-catalog/items/{id}/publish | WEB_CATALOG_MANAGE | Set status LIVE (rejected if product inactive/non-sellable) |
| POST | /web-catalog/items/{id}/unpublish | WEB_CATALOG_MANAGE | Set status DRAFT |
| POST | /web-catalog/items/{id}/move-up | WEB_CATALOG_MANAGE | Swap sort order with the item above |
| POST | /web-catalog/items/{id}/move-down | WEB_CATALOG_MANAGE | Swap sort order with the item below |
| POST | /web-catalog/publish-all | WEB_CATALOG_MANAGE | Publish all valid drafts; returns count |
| POST | /web-catalog/unpublish-all | WEB_CATALOG_MANAGE | Hide all live items; returns count |

The admin dashboard stats payload (`GET /admin/dashboard/stats`) now includes
`catalogLiveCount` and `catalogDraftCount`.

---

## Web Orders (Online Shop) APIs

In-app ordering for the customer mobile app. Same conventions as the web catalog:
public endpoints under `/api/v1/web/**` (anonymous, `X-Tenant-ID` header, additive-only
changes), staff endpoints authenticated with permissions.

### Public endpoints (mobile app contract)

> Full request/response examples are in
> **[Mobile Shop App — Public API Reference](#mobile-shop-app--public-api-reference)** above.

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /web/cart/price | Cart pricing preview with promotion discounts. Body: `{ lines: [{catalogItemId, quantity}] }` (max 50 lines). Optional `Authorization: Bearer <web-customer-token>` personalises customer-specific promotion conditions. Rate limited 5 calls / 10s per IP (429). Returns `{ lines: [{catalogItemId, productName, quantity, unitPrice, lineTotal}], subtotal, discountTotal, total, currency, appliedPromotions: ["10% off"] }` — promotion **names only**, never conditions or usage counters. Display-only: checkout recomputes everything |
| POST | /web/cart/validate-coupon | Coupon check before checkout. Body: `{ code, lines: [{catalogItemId, quantity}] }` (the discount depends on the cart). Optional bearer token binds per-customer limits. **Strictly rate limited 5/min per IP** (429) — coupon codes are guessable. Returns `{ couponCode, valid, discount }`; every invalid reason (unknown / expired / depleted / wrong channel / below min order) produces the same generic `valid: false` so the endpoint can't probe codes |
| POST | /web/orders | Checkout. Body: `{ customerName, phone, regionId?, villageId?, note?, couponCode?, pointsToSpend?, lines: [{catalogItemId, quantity}] }`. Max 50 lines, qty 0.001–10000, products must be LIVE. **Prices and discounts are always computed server-side**. An invalid `couponCode` rejects the checkout with 400 (never silently drops the discount). `pointsToSpend` redeems loyalty points (capped server-side). Rate limited per IP+phone (5/min → 429). Returns 201 with `{ orderNumber, status, deliveryFee, discountTotal, couponCode, couponDiscount, pointsSpent, totalAmount, lines }` |
| GET | /web/orders/{orderNumber}?phone= | Order status lookup; the phone must match the order (404 otherwise). **Rate limited ≈5/10s per IP** (429) — enumerable order numbers. Includes `paymentMethod`, `paymentStatus`, `address`, `discountTotal`, `couponCode`, `couponDiscount` and `deliveryFee` |
| POST | /web/orders/{orderNumber}/payment | Start a card payment. Body: `{ phone, provider (PAYME\|CLICK\|UZUM), returnUrl? }`. Phone must match the order. Returns `{ id, paymentUrl (HTTPS), status, provider, amount }`. `503` if the provider isn't configured |
| GET | /web/payments/{id} | Poll payment status by opaque id. Returns `{ id, status (PENDING\|PAID\|FAILED\|CANCELLED), provider, amount }` |
| GET | /web/delivery/regions | Active delivery regions for the checkout form (includes `deliveryFee`) |
| GET | /web/delivery/villages?regionId= | Active villages, optionally filtered by region |

Order lifecycle: `NEW → CONFIRMED → DELIVERING → COMPLETED`, cancellable until completed.
Each new order triggers a Telegram `ORDER_PLACED` broadcast to staff. Orders record
source IP and user agent; product name/price are snapshotted on each line **at full price** —
promotion and coupon discounts live at order level, so
`totalAmount = Σ lineTotal − discountTotal − couponDiscount + deliveryFee`. WEB/ALL-channel
promotions are applied automatically at checkout; POS-only promotions and coupons never
affect online orders. Promotion usage and coupon redemptions are counted when staff confirm
the order (redemption rows carry `webOrderId`) and released/reversed if a confirmed order is
cancelled — the customer can use the coupon again. Per-customer coupon limits bind to the AR
customer linked to the phone's web account; unknown phones get only the coupon's global limits.

### Staff endpoints (authenticated)

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| GET | /web-orders | WEB_ORDER_VIEW | Paged inbox, newest first. Param: `status` |
| GET | /web-orders/counts/new | WEB_ORDER_VIEW | `{ newOrders }` count for the sidebar badge |
| GET | /web-orders/{id} | WEB_ORDER_VIEW | Order detail with lines |
| POST | /web-orders/{id}/status | WEB_ORDER_MANAGE | `{ status, reason? }` — transition validation enforced; `reason` required for CANCELLED |
| POST | /web-orders/{id}/convert-to-invoice | WEB_ORDER_MANAGE | Creates an AR invoice (debt) from the order; auto-creates an AR customer from name/phone when not linked. Rejected for cancelled or already-converted orders |

The admin dashboard stats payload now also includes `newOnlineOrders`,
`onlineOrdersToday`, `recentOnlineOrders` (latest 5) and `lastCampaign`
(the most recent SMS campaign's status + sent/failed counts).

The web order detail/list and the customer's own order history also expose
`couponCode` and `couponDiscount` (Phase 2). Web customers now carry
`smsOptOut` and `lastOrderAt`.

---

## Web Campaigns (Online Shop SMS marketing)

Staff-only. Build a campaign against a customer segment + SMS template
(optionally a promotion for personal coupons), preview the cost, then send once.
No public/mobile endpoints — SMS arrives out of band; campaign coupon codes work
through the Phase 2 coupon flow.

### Staff endpoints (authenticated)

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| GET | /web-campaigns | WEB_CAMPAIGN_VIEW | Paged list, newest first |
| GET | /web-campaigns/{id} | WEB_CAMPAIGN_VIEW | Campaign detail |
| POST | /web-campaigns | WEB_CAMPAIGN_MANAGE | Create draft: `{ name, segmentType, segmentParam?, smsTemplateId, promotionId? }` |
| PUT | /web-campaigns/{id} | WEB_CAMPAIGN_MANAGE | Update a draft (only DRAFT is editable) |
| DELETE | /web-campaigns/{id} | WEB_CAMPAIGN_MANAGE | Delete a draft |
| POST | /web-campaigns/{id}/preview | WEB_CAMPAIGN_MANAGE | `{ recipientCount, estimatedCost, smsBalance, sufficientBalance }` — mandatory before sending |
| POST | /web-campaigns/{id}/send | WEB_CAMPAIGN_MANAGE | Sends once (DRAFT→SENDING, async); resending a non-draft is rejected (400) |

**Segments** (`segmentType`, all exclude SMS-opted-out customers): `ALL_CUSTOMERS`,
`ORDERED_LAST_N_DAYS` (param = days), `NO_ORDER_IN_N_DAYS` (param = days, has ordered
before but not recently), `MIN_TOTAL_SPENT` (param = amount, completed orders),
`NEVER_ORDERED`.

When a `promotionId` is attached, one personal single-use coupon is generated per
recipient and injected as the `{coupon}` template variable (`{name}` is always
available). The promotion must target the online shop (WEB or ALL channel). Cost is
`recipientCount × 200 UZS`; an insufficient known balance blocks the send. Opt-out is
toggled via `POST /web-customers/{id}/sms-opt-out { optOut }`.

---

## Web Customer Accounts (SMS OTP) APIs

Phone-based accounts for the shop mobile app. Web-customer tokens are signed with a key
**derived from** the staff JWT secret, so they can never authenticate against staff endpoints
(and staff JWTs are equally rejected on `/web/me/**`).

### Public endpoints (mobile app contract)

> Full request/response examples for **every** endpoint below are in
> **[Mobile Shop App — Public API Reference](#mobile-shop-app--public-api-reference)** above.

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /web/auth/request-otp | `{ phone }` → sends a 6-digit SMS code. Limits: 60 s cooldown per phone, max 5 codes/day/phone, per-IP rate limit (429). Codes are stored salted+hashed, expire in 5 min |
| POST | /web/auth/verify | `{ phone, code, name?, referralCode? }` → `{ token, phone, name }`. 5 wrong attempts lock the code (429); then a new code must be requested. `referralCode` is applied on first registration |
| GET | /web/me | Bearer token → `{ phone, name }` |
| GET | /web/me/orders | Bearer token → paged list of the customer's own orders (matched by verified phone) |
| GET | /web/me/loyalty | Bearer token → `{ balance, enabled, minRedeem, maxRedeemPercent, entries[] }` cashback balance + ledger |
| POST | /web/me/device-token | Bearer token + `{ token, platform }` → registers an FCM push token (idempotent) |
| DELETE | /web/me/device-token | Bearer token, optional `?token=` → removes one (or all) push tokens; call on logout |
| GET | /web/me/referral-code | Bearer token → `{ code }` the customer's own referral code (created on first request) |
| GET | /web/me/referral-stats | Bearer token → `{ code, enabled, invitedCount, pointsEarned }` full referral dashboard |
| GET | /web/me/notifications | Bearer token → paged notification history (title, body, type, read flag), newest first |
| GET | /web/me/notifications/unread-count | Bearer token → `{ count }` unread notification badge counter |
| PUT | /web/me/notifications/{id}/read | Bearer token → mark a single notification as read |
| PUT | /web/me/notifications/read-all | Bearer token → mark all notifications as read |
| GET | /web/me/coupons | Bearer token → list of valid coupon codes available to this customer (WEB/ALL channel only) |
| GET | /web/me/wishlist | Bearer token → paged wishlist with price/sale/availability and a `priceDrop` flag |
| GET | /web/me/wishlist/ids | Bearer token → `[catalogItemId, …]` for painting hearts on the catalog grid |
| PUT | /web/me/wishlist/{catalogItemId} | Bearer token → like a product (idempotent; unknown item → 400) |
| DELETE | /web/me/wishlist/{catalogItemId} | Bearer token → unlike a product (idempotent) |

### Staff endpoints (authenticated)

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| GET | /web-customers | WEB_CUSTOMER_VIEW | Paged list, newest login first. Param: `search` (phone/name) |
| GET | /web-customers/{id} | WEB_CUSTOMER_VIEW | Account detail incl. order count and linked AR customer |
| POST | /web-customers/{id}/link-customer | WEB_CUSTOMER_MANAGE | `{ customerId }` — link to an AR customer; conversion of this customer's orders then reuses that debtor record |
| POST | /web-customers/{id}/unlink-customer | WEB_CUSTOMER_MANAGE | Remove the link |

The admin dashboard stats payload now also includes `onlineCustomers`.

---

## Phase 5 additions: delivery fees & stock reservation

- `DeliveryRegion` (staff CRUD at `/delivery/regions`) has a `deliveryFee` field.
- Public `GET /web/delivery/regions` items include `deliveryFee`.
- Checkout snapshots the region's fee onto the order (`deliveryFee`) and includes it in
  `totalAmount`; both appear in checkout/status/my-orders responses and the staff order DTO.
- Converting an order with a fee to an AR invoice adds an explicit "Етказиб бериш" line,
  keeping the invoice total equal to the order total.
- Confirming an order reserves stock for each line (reference type `WEB_ORDER`) at the
  location with the most availability; cancelling or completing the order releases the
  reservation. Best-effort: insufficient stock never blocks confirmation.
