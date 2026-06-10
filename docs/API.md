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

## Web Catalog (Online Shop) APIs

The web catalog is the curated **draft/live item list** shown in the store's customer mobile app.
Staff manage the list through authenticated endpoints; the mobile app reads the live list through
public (anonymous) endpoints. See `docs/WEB_SHOP_PLAN.md` for the full roadmap.

### Public endpoints (no authentication — mobile app contract)

These live under the whitelisted `/api/v1/web/**` prefix. Tenant is resolved from the optional
`X-Tenant-ID` header (defaults to `1`). **Backward compatibility rule:** changes to these
endpoints must be additive only — installed mobile apps cannot be force-updated.

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
