# Hisobnoma Platform API Documentation

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
