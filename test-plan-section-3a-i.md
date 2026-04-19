# Section 3a-i: Inventory — Product Service — Test Plan

---

## 1. ProductService Unit Tests

Framework: JUnit 5 + Mockito.

### 1.1 `getProducts(tenantId, pageable)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getProducts_returnsPaginatedResults` | `getProducts(tenantId, pageable)` | Repository returns page | Returns `Page<ProductDto>` |
| `getProducts_returnsEmpty_whenNone` | `getProducts(tenantId, pageable)` | No products | Returns empty page |
| `getProducts_respectsTenantIsolation` | `getProducts(tenantId, pageable)` | Two tenants | Only tenant-A products |

### 1.2 `getProduct(tenantId, productId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getProduct_found_returnsDto` | `getProduct(tenantId, id)` | Product exists | Returns `ProductDto` |
| `getProduct_notFound_throwsNotFoundException` | `getProduct(tenantId, id)` | Missing | Throws `NotFoundException` |

### 1.3 `createProduct(tenantId, request)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `createProduct_success_returnsDtoWithGeneratedSku` | `createProduct(tenantId, request)` | Valid request | Returns dto with auto-generated SKU |
| `createProduct_categoryNotFound_throwsNotFoundException` | `createProduct(tenantId, request)` | Category missing | Throws `NotFoundException` |
| `createProduct_brandNotFound_throwsNotFoundException` | `createProduct(tenantId, request)` | Brand missing | Throws `NotFoundException` |
| `createProduct_uomNotFound_throwsNotFoundException` | `createProduct(tenantId, request)` | UOM missing | Throws `NotFoundException` |
| `createProduct_duplicateSku_throwsDuplicateResourceException` | `createProduct(tenantId, request)` | SKU exists | Throws `DuplicateResourceException` |
| `createProduct_duplicateBarcode_throwsDuplicateResourceException` | `createProduct(tenantId, request)` | Barcode exists | Throws `DuplicateResourceException` |

### 1.4 `updateProduct(tenantId, productId, request)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `updateProduct_success_updatesFields` | `updateProduct(tenantId, id, request)` | Valid update | Returns updated dto |
| `updateProduct_notFound_throwsNotFoundException` | `updateProduct(tenantId, id, request)` | Missing | Throws `NotFoundException` |
| `updateProduct_changeSku_toExisting_throwsDuplicateResourceException` | `updateProduct(tenantId, id, request)` | New SKU already taken | Throws `DuplicateResourceException` |

### 1.5 `deleteProduct(tenantId, productId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `deleteProduct_success_removesRecord` | `deleteProduct(tenantId, id)` | No stock, no transactions | Deleted |
| `deleteProduct_hasStock_throwsBusinessException` | `deleteProduct(tenantId, id)` | Product has stock | Throws `BusinessException` |
| `deleteProduct_hasTransactions_throwsBusinessException` | `deleteProduct(tenantId, id)` | Used in POS | Throws `BusinessException` |

### 1.6 `searchProducts(tenantId, query, pageable)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `searchProducts_byName_returnsMatches` | `searchProducts(tenantId, "phone", pageable)` | 3 products match | Returns 3 |
| `searchProducts_bySku_returnsMatch` | `searchProducts(tenantId, "PROD-001", pageable)` | Exact SKU | Returns 1 |
| `searchProducts_noMatch_returnsEmpty` | `searchProducts(tenantId, "xyz123", pageable)` | No match | Returns empty page |

### 1.7 `getProductsByCategory(tenantId, categoryId, pageable)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getProductsByCategory_returnsProductsInCategory` | `getProductsByCategory(tenantId, catId, pageable)` | 5 in category | Returns 5 |
| `getProductsByCategory_categoryNotFound_throwsNotFoundException` | `getProductsByCategory(tenantId, catId, pageable)` | Missing | Throws `NotFoundException` |

### 1.8 `activateProduct / deactivateProduct`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `deactivateProduct_active_becomesInactive` | `deactivateProduct(tenantId, id)` | Active product | Status INACTIVE |
| `deactivateProduct_alreadyInactive_idempotent` | `deactivateProduct(tenantId, id)` | Already INACTIVE | No error |
| `activateProduct_inactive_becomesActive` | `activateProduct(tenantId, id)` | Inactive product | Status ACTIVE |

---

## 2. ProductVariantService Unit Tests

### 2.1 CRUD Operations

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getVariants_returnsList` | `getVariants(tenantId, productId)` | 3 variants for product | Returns list of 3 |
| `createVariant_success` | `createVariant(tenantId, productId, request)` | Valid variant (COLOR=Red, SIZE=M) | Returns dto with auto SKU |
| `createVariant_productNotFound_throwsNotFoundException` | `createVariant(tenantId, productId, request)` | Product missing | Throws `NotFoundException` |
| `createVariant_duplicateAttributes_throwsDuplicateResourceException` | `createVariant(tenantId, productId, request)` | Same COLOR+SIZE combo | Throws `DuplicateResourceException` |
| `updateVariant_success` | `updateVariant(tenantId, id, request)` | Valid update | Returns updated dto |
| `deleteVariant_success` | `deleteVariant(tenantId, id)` | No stock | Deleted |
| `deleteVariant_hasStock_throwsBusinessException` | `deleteVariant(tenantId, id)` | Stock exists | Throws `BusinessException` |

---

## 3. Repository Tests (`@DataJpaTest` + Testcontainers)

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `ProductRepository_findBySku` | `findBySku(tenantId, sku)` | SKU "PROD-001" | Returns Optional |
| `ProductRepository_findByBarcode` | `findByBarcode(tenantId, barcode)` | Barcode "4006381333931" | Returns Optional |
| `ProductRepository_findByCategoryId` | `findByCategoryId(tenantId, catId)` | 5 in category | Returns 5 |
| `ProductRepository_findByBrandId` | `findByBrandId(tenantId, brandId)` | 3 for brand | Returns 3 |
| `ProductRepository_searchByNameOrSku` | `searchByNameOrSku(tenantId, "phone")` | 3 match | Returns 3 |
| `ProductRepository_findActiveProducts` | `findActiveProducts(tenantId)` | Mix active/inactive | Returns only active |
| `ProductVariantRepository_findByProductId` | `findByProductId(productId)` | 3 variants | Returns 3 |
| `ProductVariantRepository_findByAttributes` | `findByProductAndAttributes(pId, attrs)` | Exact attr match | Returns Optional |

---

## 4. Mapper Tests

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `ProductMapper_toDto_mapsAllFields` | `ProductMapper.toDto(product)` | Full entity with category, brand | DTO has categoryId, brandId, sku |
| `ProductMapper_fromCreateRequest` | `ProductMapper.fromCreateRequest(req)` | Valid request | Entity fields match request |
| `ProductVariantMapper_toDto_mapsAllFields` | `ProductVariantMapper.toDto(variant)` | Variant with attributes | DTO has productId and attributes |

---

## 5. Integration Tests — ProductController

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|---|---|---|---|
| `getProducts_returns200` | `GET /api/v1/inventory/products` | Bearer `INVENTORY_READ` | `200 OK`; paged |
| `getProducts_returns403` | `GET /api/v1/inventory/products` | No permission | `403 Forbidden` |
| `getProduct_returns200` | `GET /api/v1/inventory/products/{id}` | Bearer `INVENTORY_READ` | `200 OK` |
| `getProduct_returns404` | `GET /api/v1/inventory/products/{id}` | Bearer `INVENTORY_READ`; missing | `404 Not Found` |
| `searchProducts_returns200` | `GET /api/v1/inventory/products/search?q=phone` | Bearer `INVENTORY_READ` | `200 OK`; matching results |
| `getProductsByCategory_returns200` | `GET /api/v1/inventory/products/category/{catId}` | Bearer `INVENTORY_READ` | `200 OK`; products in category |
| `createProduct_returns201` | `POST /api/v1/inventory/products` | Bearer `INVENTORY_WRITE`; valid | `201 Created`; sku not null |
| `createProduct_returns404_badCategory` | `POST /api/v1/inventory/products` | Bearer `INVENTORY_WRITE`; unknown catId | `404 Not Found` |
| `createProduct_returns409_dupSku` | `POST /api/v1/inventory/products` | Bearer `INVENTORY_WRITE`; dup sku | `409 Conflict` |
| `updateProduct_returns200` | `PUT /api/v1/inventory/products/{id}` | Bearer `INVENTORY_WRITE` | `200 OK` |
| `updateProduct_returns404` | `PUT /api/v1/inventory/products/{id}` | Bearer `INVENTORY_WRITE`; missing | `404 Not Found` |
| `deleteProduct_returns204` | `DELETE /api/v1/inventory/products/{id}` | Bearer `INVENTORY_WRITE`; no stock | `204 No Content` |
| `deleteProduct_returns422_hasStock` | `DELETE /api/v1/inventory/products/{id}` | Bearer `INVENTORY_WRITE`; has stock | `422 Unprocessable Entity` |
| `deactivateProduct_returns200` | `PUT /api/v1/inventory/products/{id}/deactivate` | Bearer `INVENTORY_WRITE` | `200 OK`; status=INACTIVE |
| `activateProduct_returns200` | `PUT /api/v1/inventory/products/{id}/activate` | Bearer `INVENTORY_WRITE` | `200 OK`; status=ACTIVE |
| `createVariant_returns201` | `POST /api/v1/inventory/products/{id}/variants` | Bearer `INVENTORY_WRITE`; valid | `201 Created` |
| `createVariant_returns409_dupAttrs` | `POST /api/v1/inventory/products/{id}/variants` | Bearer `INVENTORY_WRITE`; dup combo | `409 Conflict` |
