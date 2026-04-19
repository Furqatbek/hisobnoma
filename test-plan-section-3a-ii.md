# Section 3a-ii: Inventory — Brand, Category, Location, Vendor & UOM — Test Plan

---

## 1. BrandService Unit Tests

Framework: JUnit 5 + Mockito.

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getBrands_returnsPaged` | `getBrands(tenantId, pageable)` | Multiple brands | Returns `Page<BrandDto>` |
| `getBrand_found_returnsDto` | `getBrand(tenantId, id)` | Brand exists | Returns `BrandDto` |
| `getBrand_notFound_throwsNotFoundException` | `getBrand(tenantId, id)` | Missing | Throws `NotFoundException` |
| `createBrand_success` | `createBrand(tenantId, request)` | Valid name | Returns dto |
| `createBrand_duplicateName_throwsDuplicateResourceException` | `createBrand(tenantId, request)` | Name exists | Throws `DuplicateResourceException` |
| `updateBrand_success` | `updateBrand(tenantId, id, request)` | Valid | Returns updated dto |
| `updateBrand_notFound_throwsNotFoundException` | `updateBrand(tenantId, id, request)` | Missing | Throws `NotFoundException` |
| `deleteBrand_success` | `deleteBrand(tenantId, id)` | No products | Deleted |
| `deleteBrand_hasProducts_throwsBusinessException` | `deleteBrand(tenantId, id)` | Products use brand | Throws `BusinessException` |

---

## 2. CategoryService Unit Tests

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getCategories_returnsTree` | `getCategories(tenantId)` | 3 root + 5 child categories | Returns hierarchical list |
| `getCategory_found_returnsDto` | `getCategory(tenantId, id)` | Category exists | Returns dto with children |
| `getCategory_notFound_throwsNotFoundException` | `getCategory(tenantId, id)` | Missing | Throws `NotFoundException` |
| `createCategory_root_success` | `createCategory(tenantId, request)` | No parentId | Root category created |
| `createCategory_withParent_success` | `createCategory(tenantId, request)` | Valid parentId | Child category created |
| `createCategory_parentNotFound_throwsNotFoundException` | `createCategory(tenantId, request)` | Invalid parentId | Throws `NotFoundException` |
| `createCategory_duplicateName_sameParent_throwsDuplicateResourceException` | `createCategory(tenantId, request)` | Name+parent combo exists | Throws `DuplicateResourceException` |
| `updateCategory_success` | `updateCategory(tenantId, id, request)` | Valid | Returns updated dto |
| `deleteCategory_success_noChildren_noProducts` | `deleteCategory(tenantId, id)` | Leaf node, no products | Deleted |
| `deleteCategory_hasChildren_throwsBusinessException` | `deleteCategory(tenantId, id)` | Has subcategories | Throws `BusinessException` |
| `deleteCategory_hasProducts_throwsBusinessException` | `deleteCategory(tenantId, id)` | Products assigned | Throws `BusinessException` |

---

## 3. LocationService Unit Tests

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getLocations_returnsPaged` | `getLocations(tenantId, pageable)` | Multiple | Returns paged `LocationDto` |
| `getLocation_found_returnsDto` | `getLocation(tenantId, id)` | Exists | Returns dto |
| `getLocation_notFound_throwsNotFoundException` | `getLocation(tenantId, id)` | Missing | Throws `NotFoundException` |
| `createLocation_success` | `createLocation(tenantId, request)` | Valid code+name | Returns dto |
| `createLocation_duplicateCode_throwsDuplicateResourceException` | `createLocation(tenantId, request)` | Code exists | Throws `DuplicateResourceException` |
| `updateLocation_success` | `updateLocation(tenantId, id, request)` | Valid | Returns updated |
| `deleteLocation_success` | `deleteLocation(tenantId, id)` | No stock | Deleted |
| `deleteLocation_hasStock_throwsBusinessException` | `deleteLocation(tenantId, id)` | Stock exists | Throws `BusinessException` |
| `getLocationsByWarehouse_returnsMatchingLocations` | `getLocationsByWarehouse(tenantId, warehouseId)` | 3 in warehouse | Returns 3 |
| `getWarehouses_returnsPaged` | `getWarehouses(tenantId, pageable)` | Multiple warehouses | Returns paged |
| `createWarehouse_success` | `createWarehouse(tenantId, request)` | Valid | Returns dto |

---

## 4. VendorService Unit Tests

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getVendors_returnsPaged` | `getVendors(tenantId, pageable)` | Multiple | Returns paged |
| `getVendor_found_returnsDto` | `getVendor(tenantId, id)` | Exists | Returns dto |
| `getVendor_notFound_throwsNotFoundException` | `getVendor(tenantId, id)` | Missing | Throws `NotFoundException` |
| `createVendor_success` | `createVendor(tenantId, request)` | Valid | Returns dto |
| `createVendor_duplicateName_throwsDuplicateResourceException` | `createVendor(tenantId, request)` | Name exists | Throws `DuplicateResourceException` |
| `updateVendor_success` | `updateVendor(tenantId, id, request)` | Valid | Returns updated |
| `deleteVendor_success` | `deleteVendor(tenantId, id)` | No POs | Deleted |
| `deleteVendor_hasPurchaseOrders_throwsBusinessException` | `deleteVendor(tenantId, id)` | Has POs | Throws `BusinessException` |
| `getVendorBalance_returnsNetDebt` | `getVendorBalance(tenantId, id)` | Invoices 5000, payments 2000 | Returns 3000 |

---

## 5. UOMService Unit Tests

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getUOMs_returnsList` | `getUOMs(tenantId)` | Multiple UOMs | Returns list |
| `getUOM_found_returnsDto` | `getUOM(tenantId, id)` | Exists | Returns dto |
| `getUOM_notFound_throwsNotFoundException` | `getUOM(tenantId, id)` | Missing | Throws `NotFoundException` |
| `createUOM_success` | `createUOM(tenantId, request)` | Valid name+symbol | Returns dto |
| `createUOM_duplicateCode_throwsDuplicateResourceException` | `createUOM(tenantId, request)` | Code exists | Throws `DuplicateResourceException` |
| `updateUOM_success` | `updateUOM(tenantId, id, request)` | Valid | Returns updated |
| `deleteUOM_success` | `deleteUOM(tenantId, id)` | Not used by products | Deleted |
| `deleteUOM_usedByProducts_throwsBusinessException` | `deleteUOM(tenantId, id)` | Products use UOM | Throws `BusinessException` |
| `convertQuantity_success` | `convertQuantity(tenantId, qty, fromUomId, toUomId)` | Conversion factor exists | Returns converted qty |
| `convertQuantity_noConversionFactor_throwsBusinessException` | `convertQuantity(tenantId, qty, fromUomId, toUomId)` | No factor defined | Throws `BusinessException` |

---

## 6. Repository Tests (`@DataJpaTest` + Testcontainers)

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `BrandRepository_findByName` | `findByName(tenantId, name)` | "Samsung" exists | Returns Optional |
| `CategoryRepository_findRootCategories` | `findRootCategories(tenantId)` | 3 roots | Returns 3 |
| `CategoryRepository_findByParentId` | `findByParentId(parentId)` | 2 children | Returns 2 |
| `LocationRepository_findByCode` | `findByCode(tenantId, code)` | "WH-A-01" | Returns Optional |
| `LocationRepository_findByWarehouseId` | `findByWarehouseId(warehouseId)` | 4 locations | Returns 4 |
| `VendorRepository_findByName` | `findByName(tenantId, name)` | Vendor name | Returns Optional |
| `UOMRepository_findByCode` | `findByCode(tenantId, code)` | "KG" | Returns Optional |
| `UOMConversionRepository_findByFromAndToUom` | `findByFromAndTo(fromId, toId)` | Conversion exists | Returns Optional |

---

## 7. Integration Tests

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|---|---|---|---|
| `getBrands_returns200` | `GET /api/v1/inventory/brands` | Bearer `INVENTORY_READ` | `200 OK` |
| `createBrand_returns201` | `POST /api/v1/inventory/brands` | Bearer `INVENTORY_WRITE`; valid | `201 Created` |
| `createBrand_returns409_dup` | `POST /api/v1/inventory/brands` | Bearer `INVENTORY_WRITE`; dup | `409 Conflict` |
| `deleteBrand_returns204` | `DELETE /api/v1/inventory/brands/{id}` | Bearer `INVENTORY_WRITE`; no products | `204 No Content` |
| `deleteBrand_returns422_hasProducts` | `DELETE /api/v1/inventory/brands/{id}` | Bearer `INVENTORY_WRITE` | `422 Unprocessable Entity` |
| `getCategories_returns200` | `GET /api/v1/inventory/categories` | Bearer `INVENTORY_READ` | `200 OK`; tree |
| `createCategory_returns201` | `POST /api/v1/inventory/categories` | Bearer `INVENTORY_WRITE`; valid | `201 Created` |
| `deleteCategory_returns422_hasChildren` | `DELETE /api/v1/inventory/categories/{id}` | Bearer `INVENTORY_WRITE` | `422 Unprocessable Entity` |
| `getLocations_returns200` | `GET /api/v1/inventory/locations` | Bearer `INVENTORY_READ` | `200 OK` |
| `createLocation_returns201` | `POST /api/v1/inventory/locations` | Bearer `INVENTORY_WRITE` | `201 Created` |
| `getVendors_returns200` | `GET /api/v1/inventory/vendors` | Bearer `INVENTORY_READ` | `200 OK` |
| `createVendor_returns201` | `POST /api/v1/inventory/vendors` | Bearer `INVENTORY_WRITE` | `201 Created` |
| `getVendorBalance_returns200` | `GET /api/v1/inventory/vendors/{id}/balance` | Bearer `FINANCE_READ` | `200 OK`; numeric balance |
| `getUOMs_returns200` | `GET /api/v1/inventory/uoms` | Bearer `INVENTORY_READ` | `200 OK`; list |
| `createUOM_returns201` | `POST /api/v1/inventory/uoms` | Bearer `INVENTORY_WRITE` | `201 Created` |
| `deleteUOM_returns422_usedByProducts` | `DELETE /api/v1/inventory/uoms/{id}` | Bearer `INVENTORY_WRITE` | `422 Unprocessable Entity` |
