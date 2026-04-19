# Section 3b-i: Inventory — Stock Management — Test Plan

---

## Overview

This section covers the test plan for `StockService` and all stock-related repositories in the Hisobnoma inventory platform. The goal is **100% unit and integration test coverage** across service logic, repository queries, mapper transformations, and HTTP controller endpoints.

Scope:
- `StockService` — all public methods
- `StockRepository`, `StockMovementRepository`, `StockBatchRepository`, `SerialNumberRepository`, `StockReservationRepository` — custom query methods
- `StockMapper`, `StockMovementMapper`, `StockBatchMapper` — entity-to-DTO transformations
- `StockController` — all REST endpoints under `/api/v1/inventory/stock`

---

## 1. StockService Unit Tests

Framework: JUnit 5 + Mockito. All dependencies (`StockRepository`, `StockMovementRepository`, etc.) are mocked.

### 1.1 `getStock(tenantId, pageable)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getStock_returnsPageOfAllStockRecords` | `getStock(tenantId, pageable)` | Repository returns a populated `Page<Stock>` for the given tenant | Returns a `Page<StockDto>` with the same number of elements, all correctly mapped |
| `getStock_returnsEmptyPage_whenNoStockExists` | `getStock(tenantId, pageable)` | Repository returns an empty `Page<Stock>` | Returns an empty `Page<StockDto>` (not null, not an exception) |
| `getStock_appliesTenantIsolation` | `getStock(tenantId, pageable)` | Two tenants exist; repository is queried with tenant A's ID | Only stock records belonging to tenant A are returned; tenant B records are absent |

### 1.2 `getStockByProduct(tenantId, productId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getStockByProduct_returnsAllLocationsForProduct` | `getStockByProduct(tenantId, productId)` | Product exists and has stock in three locations | Returns a list of three `StockDto` objects, each with a distinct `locationId` |
| `getStockByProduct_returnsEmptyList_whenProductHasNoStock` | `getStockByProduct(tenantId, productId)` | Product exists but no `Stock` records exist for it | Returns an empty list (not null, not an exception) |
| `getStockByProduct_throwsNotFoundException_whenProductNotFound` | `getStockByProduct(tenantId, productId)` | `ProductRepository.findById` returns `Optional.empty()` | Throws `NotFoundException` with message referencing the product ID |

### 1.3 `getStockByLocation(tenantId, locationId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getStockByLocation_returnsAllProductsInLocation` | `getStockByLocation(tenantId, locationId)` | Location exists and holds four distinct products | Returns a list of four `StockDto` objects, each with a distinct `productId` |
| `getStockByLocation_returnsEmptyList_whenLocationIsEmpty` | `getStockByLocation(tenantId, locationId)` | Location exists but contains no stock records | Returns an empty list |
| `getStockByLocation_throwsNotFoundException_whenLocationNotFound` | `getStockByLocation(tenantId, locationId)` | `LocationRepository.findById` returns `Optional.empty()` | Throws `NotFoundException` with message referencing the location ID |

### 1.4 `getStockByProductAndLocation(tenantId, productId, locationId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getStockByProductAndLocation_returnsDto_whenFoundWithPositiveQty` | `getStockByProductAndLocation(tenantId, productId, locationId)` | A `Stock` record exists with `onHandQty = 50` | Returns a `StockDto` with `onHandQty = 50` |
| `getStockByProductAndLocation_returnsZeroQtyDto_whenNoStockRecord` | `getStockByProductAndLocation(tenantId, productId, locationId)` | No `Stock` record exists for the product+location combination | Returns a `StockDto` with `onHandQty = 0` and `availableQty = 0`; does **not** throw a `NotFoundException` |
| `getStockByProductAndLocation_includesReservedQty_inDto` | `getStockByProductAndLocation(tenantId, productId, locationId)` | Stock record exists with `onHandQty = 20`, `reservedQty = 5` | Returned `StockDto` has `onHandQty = 20`, `reservedQty = 5`, `availableQty = 15` |

### 1.5 `getLowStock(tenantId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getLowStock_returnsItems_whereOnHandQtyBelowReorderPoint` | `getLowStock(tenantId)` | Three stock records: two have `onHandQty < reorderPoint`, one does not | Returns a list of two `StockDto` objects corresponding to the under-stocked records |
| `getLowStock_returnsEmptyList_whenAllStockAboveReorderPoint` | `getLowStock(tenantId)` | All stock records have `onHandQty >= reorderPoint` | Returns an empty list |
| `getLowStock_includesZeroQtyItems` | `getLowStock(tenantId)` | One stock record has `onHandQty = 0` with `reorderPoint = 10` | The zero-quantity record is included in the returned list |

### 1.6 `getValuation(tenantId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getValuation_returnsSumOfOnHandQtyTimesAverageCost` | `getValuation(tenantId)` | Two stock records: (qty=10, cost=5.00) and (qty=4, cost=2.50) | Returns `BigDecimal` equal to `60.00` (10×5.00 + 4×2.50) |
| `getValuation_returnsZero_whenNoStockRecordsExist` | `getValuation(tenantId)` | Repository returns an empty list | Returns `BigDecimal.ZERO`; no exception thrown |
| `getValuation_handlesNullAverageCost_asZero` | `getValuation(tenantId)` | One stock record has `averageCost = null` | The null cost is treated as `0`; valuation equals sum of records with non-null costs only |

### 1.7 `getAvailable(tenantId, productId, locationId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getAvailable_returnsOnHandMinusReserved` | `getAvailable(tenantId, productId, locationId)` | `onHandQty = 30`, `reservedQty = 8` | Returns `22` |
| `getAvailable_returnsOnHandQty_whenNoReservations` | `getAvailable(tenantId, productId, locationId)` | `onHandQty = 15`, `reservedQty = 0` | Returns `15` |
| `getAvailable_returnsZero_whenNoStockRecord` | `getAvailable(tenantId, productId, locationId)` | No `Stock` record exists for product+location | Returns `0`; does not throw an exception |

### 1.8 `checkAvailability(tenantId, productId, locationId, qty)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `checkAvailability_returnsTrue_whenOnHandExceedsRequestedQty` | `checkAvailability(tenantId, productId, locationId, 5)` | `onHandQty = 10`, `reservedQty = 0` | Returns `true` |
| `checkAvailability_returnsFalse_whenOnHandBelowRequestedQty` | `checkAvailability(tenantId, productId, locationId, 5)` | `onHandQty = 3`, `reservedQty = 0` | Returns `false` |
| `checkAvailability_returnsFalse_whenAvailableQtyExactlyZero` | `checkAvailability(tenantId, productId, locationId, 5)` | `onHandQty = 5`, `reservedQty = 5` | Available qty is `0`; returns `false` |

### 1.9 `receiveStock(tenantId, request)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `receiveStock_increasesOnHandQty_andCreatesReceiptMovement` | `receiveStock(tenantId, request)` | Valid request; existing stock record with `onHandQty = 10`; `receivedQty = 5` | `Stock.onHandQty` becomes `15`; one `StockMovement` saved with `movementType = RECEIPT` and `qty = 5` |
| `receiveStock_throwsValidationException_whenQtyIsZero` | `receiveStock(tenantId, request)` | `request.qty = 0` | Throws `ValidationException` with message `"Quantity must be positive"` |
| `receiveStock_throwsValidationException_whenQtyIsNegative` | `receiveStock(tenantId, request)` | `request.qty = -3` | Throws `ValidationException` with message `"Quantity must be positive"` |
| `receiveStock_throwsNotFoundException_whenProductNotFound` | `receiveStock(tenantId, request)` | `ProductRepository.findById` returns `Optional.empty()` | Throws `NotFoundException` referencing the product ID; no stock record is saved |
| `receiveStock_throwsNotFoundException_whenLocationNotFound` | `receiveStock(tenantId, request)` | `LocationRepository.findById` returns `Optional.empty()` | Throws `NotFoundException` referencing the location ID; no stock record is saved |

### 1.10 `issueStock(tenantId, request)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `issueStock_decreasesOnHandQty_andCreatesIssueMovement` | `issueStock(tenantId, request)` | `onHandQty = 20`; `request.qty = 7` | `Stock.onHandQty` becomes `13`; one `StockMovement` saved with `movementType = ISSUE` and `qty = 7` |
| `issueStock_throwsBusinessException_whenInsufficientStock` | `issueStock(tenantId, request)` | `onHandQty = 4`; `request.qty = 10` | Throws `BusinessException` with message `"Insufficient stock"` |
| `issueStock_throwsNotFoundException_whenProductNotFound` | `issueStock(tenantId, request)` | `ProductRepository.findById` returns `Optional.empty()` | Throws `NotFoundException`; no `Stock` record is modified |
| `issueStock_throwsValidationException_whenQtyIsZeroOrNegative` | `issueStock(tenantId, request)` | `request.qty = 0` | Throws `ValidationException` with message `"Quantity must be positive"` |

### 1.11 `transferStock(tenantId, request)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `transferStock_decreasesSource_andIncreasesDestination_andCreatesTwoMovements` | `transferStock(tenantId, request)` | Source `onHandQty = 30`; destination `onHandQty = 10`; `request.qty = 8` | Source `onHandQty` becomes `22`; destination `onHandQty` becomes `18`; two `StockMovement` records saved: one `ISSUE` (source) and one `RECEIPT` (destination) |
| `transferStock_throwsBusinessException_whenSourceAndDestinationAreSame` | `transferStock(tenantId, request)` | `request.sourceLocationId == request.destinationLocationId` | Throws `BusinessException` with message `"Source and destination must differ"` |
| `transferStock_throwsBusinessException_whenInsufficientStockAtSource` | `transferStock(tenantId, request)` | Source `onHandQty = 2`; `request.qty = 10` | Throws `BusinessException` with message `"Insufficient stock"`; no stock records modified; no movements created |
| `transferStock_throwsNotFoundException_whenSourceLocationNotFound` | `transferStock(tenantId, request)` | `LocationRepository.findById(sourceLocationId)` returns `Optional.empty()` | Throws `NotFoundException` referencing the source location ID |

### 1.12 `adjustStock(tenantId, request)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `adjustStock_increasesOnHandQty_whenAdjustmentIsPositive` | `adjustStock(tenantId, request)` | `onHandQty = 10`; `request.adjustmentQty = +5` | `Stock.onHandQty` becomes `15`; one `StockMovement` saved with `movementType = ADJUSTMENT` and `qty = 5` |
| `adjustStock_decreasesOnHandQty_whenAdjustmentIsNegative` | `adjustStock(tenantId, request)` | `onHandQty = 10`; `request.adjustmentQty = -3` | `Stock.onHandQty` becomes `7`; one `StockMovement` saved with `movementType = ADJUSTMENT` and `qty = -3` |
| `adjustStock_throwsBusinessException_whenResultWouldBeNegative` | `adjustStock(tenantId, request)` | `onHandQty = 5`; `request.adjustmentQty = -10` | Throws `BusinessException` with message `"Adjustment would result in negative stock"`; `onHandQty` remains `5`; no movement created |
| `adjustStock_isNoOp_whenAdjustmentQtyIsZero` | `adjustStock(tenantId, request)` | `onHandQty = 10`; `request.adjustmentQty = 0` | `onHandQty` remains `10`; `StockMovementRepository.save` is **never** called |

---

## 2. Stock Repository Tests (`@DataJpaTest` + Testcontainers)

Framework: JUnit 5, `@DataJpaTest`, Testcontainers (PostgreSQL). Each test class bootstraps an isolated schema. Tenant isolation is verified by inserting records for two tenants and asserting that queries scoped to one tenant never return the other's data.

### 2.1 `StockRepository`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `findByProductAndLocation_returnsStock_whenExactMatchExists` | `findByProductAndLocation(product, location)` | A `Stock` record for the exact product+location pair exists in the database | Returns a non-empty `Optional<Stock>` containing the matching record |
| `findByProductAndLocation_returnsEmpty_whenNoMatchExists` | `findByProductAndLocation(product, location)` | No `Stock` record for the given product+location combination | Returns `Optional.empty()` |
| `findLowStockProducts_returnsOnlyRecordsBelowReorderPoint` | `findLowStockProducts(tenantId)` | Three records: `onHandQty=2, reorderPoint=5`; `onHandQty=10, reorderPoint=5`; `onHandQty=0, reorderPoint=1` | Returns only the first and third records |
| `findLowStockProducts_returnsEmptyList_whenAllAboveReorderPoint` | `findLowStockProducts(tenantId)` | All stock records have `onHandQty >= reorderPoint` | Returns an empty list |
| `countLowStockProducts_returnsCorrectCount` | `countLowStockProducts(tenantId)` | Two records below reorder point, one above | Returns `2L` |
| `countOutOfStockProducts_returnsCountOfZeroQtyRecords` | `countOutOfStockProducts(tenantId)` | Two records with `onHandQty = 0`, three with positive qty | Returns `2L` |
| `calculateTotalInventoryValue_returnsCorrectSum` | `calculateTotalInventoryValue(tenantId)` | Two records: (qty=10, avgCost=5.00) and (qty=3, avgCost=4.00) | Returns `BigDecimal` equal to `62.00` |

### 2.2 `StockMovementRepository`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `findByProductId_returnsAllMovementsForProduct` | `findByProductId(productId)` | Four movements exist for product A, two for product B | Returns a list of four movements; none belong to product B |
| `findByProductAndDateRange_returnsOnlyMovementsWithinRange` | `findByProductAndDateRange(productId, from, to)` | Movements at T-10, T-5, T-3 (in range), T+1 (out of range) | Returns the three in-range movements; the future movement is excluded |
| `findByMovementType_returnsOnlyReceiptMovements` | `findByMovementType(RECEIPT)` | Mix of `RECEIPT`, `ISSUE`, and `ADJUSTMENT` movements | Returns only `RECEIPT` movements |

### 2.3 `StockBatchRepository`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `findByStockId_returnsAllBatchesForStockRecord` | `findByStockId(stockId)` | Three batches linked to stock record A, two to stock record B | Returns three batches; none belong to stock record B |
| `findExpiredBatches_returnsBatchesWithExpiryBeforeToday` | `findExpiredBatches(today)` | Two batches with `expiryDate` in the past, one with today's date, one future | Returns only the two past-expired batches |

### 2.4 `SerialNumberRepository`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `findByProductId_returnsAllSerialsForProduct` | `findByProductId(productId)` | Five serial numbers linked to product X, three to product Y | Returns five serial numbers; none belong to product Y |
| `findBySerialNumber_returnsExactMatch` | `findBySerialNumber(serialNumber)` | Serial number `"SN-00123"` exists in the database | Returns a non-empty `Optional<SerialNumber>` with `serialNumber = "SN-00123"` |

### 2.5 `StockReservationRepository`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `findByProductId_returnsAllReservationsForProduct` | `findByProductId(productId)` | Three reservations (active and expired) for product A | Returns all three reservation records for product A |
| `findActiveReservations_returnsOnlyNonExpiredActiveReservations` | `findActiveReservations(productId)` | Three reservations: one active+non-expired, one expired, one cancelled | Returns only the active, non-expired reservation |

---

## 3. Mapper Tests

Framework: JUnit 5, no Spring context. Mapper instances created directly (no Mockito).

### 3.1 `StockMapper`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `toDto_mapsAllFields_correctly` | `StockMapper.toDto(stock)` | `Stock` entity with `onHandQty=20`, `reservedQty=5`, `averageCost=12.50`, populated `Location`, populated `Product` | Returned `StockDto` has `onHandQty=20`, `reservedQty=5`, `availableQty=15`, `averageCost=12.50`, `locationId` and `productId` matching the entity's associations |

### 3.2 `StockMovementMapper`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `toDto_mapsAllFields_correctly` | `StockMovementMapper.toDto(movement)` | `StockMovement` entity with `movementType=RECEIPT`, `qty=10`, `referenceType="PURCHASE_ORDER"`, `referenceId="PO-001"`, `movementDate=2026-04-17T10:00:00Z` | Returned `StockMovementDto` has all five fields mapped with exact values |

### 3.3 `StockBatchMapper`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `toDto_mapsAllFields_correctly` | `StockBatchMapper.toDto(batch)` | `StockBatch` entity with `batchNumber="BATCH-42"`, `expiryDate=2026-12-31`, `qty=100` | Returned `StockBatchDto` has `batchNumber="BATCH-42"`, `expiryDate=2026-12-31`, `qty=100` |

---

## 4. Integration Tests — `StockController`

Base path: `/api/v1/inventory/stock`

Framework: JUnit 5, `@SpringBootTest(webEnvironment = RANDOM_PORT)`, Testcontainers (PostgreSQL), MockMvc or `TestRestTemplate`. JWT tokens are generated per-role for auth scenarios. Each test runs in a transaction that is rolled back after the test, or uses `@Sql` scripts to seed and clean data.

### 4.1 Stock Queries

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|---|---|---|---|
| `getStock_returns200PagedResults_withInventoryReadPermission` | `GET /api/v1/inventory/stock` | Bearer token with `INVENTORY_READ` | `200 OK`; response body is a valid JSON page object with `content` array and pagination metadata |
| `getStock_returns403_withoutInventoryReadPermission` | `GET /api/v1/inventory/stock` | Bearer token without `INVENTORY_READ` | `403 Forbidden` |
| `getStock_returns401_withNoToken` | `GET /api/v1/inventory/stock` | No Authorization header | `401 Unauthorized` |
| `getStockByProduct_returns200_withMatchingStockRecords` | `GET /api/v1/inventory/stock/product/{productId}` | Bearer token with `INVENTORY_READ` | `200 OK`; JSON array of `StockDto` objects all sharing the requested `productId` |
| `getStockByProduct_returns404_whenProductNotFound` | `GET /api/v1/inventory/stock/product/{productId}` | Bearer token with `INVENTORY_READ`; product ID does not exist | `404 Not Found`; error body contains a descriptive message |
| `getStockByLocation_returns200_withMatchingStockRecords` | `GET /api/v1/inventory/stock/location/{locationId}` | Bearer token with `INVENTORY_READ` | `200 OK`; JSON array of `StockDto` objects all sharing the requested `locationId` |
| `getStockByProductAndLocation_returns200_withPositiveQty` | `GET /api/v1/inventory/stock/product/{productId}/location/{locationId}` | Bearer token with `INVENTORY_READ`; stock record exists with `onHandQty > 0` | `200 OK`; response body contains `onHandQty` greater than `0` |
| `getStockByProductAndLocation_returns200WithZeroQty_whenNoStockRecord` | `GET /api/v1/inventory/stock/product/{productId}/location/{locationId}` | Bearer token with `INVENTORY_READ`; no stock record exists for the combination | `200 OK`; response body contains `onHandQty: 0`; **not** `404` |
| `getLowStock_returns200_withLowStockItems` | `GET /api/v1/inventory/stock/low-stock` | Bearer token with `INVENTORY_READ` | `200 OK`; JSON array; every item in the array has `onHandQty < reorderPoint` |
| `getValuation_returns200_withTotalValue` | `GET /api/v1/inventory/stock/valuation` | Bearer token with `INVENTORY_READ` | `200 OK`; JSON body contains a numeric `totalValue` field |
| `getAvailable_returns200_withAvailableQty` | `GET /api/v1/inventory/stock/available?productId=&locationId=` | Bearer token with `INVENTORY_READ`; valid product+location | `200 OK`; JSON body is a non-negative integer representing available quantity |

### 4.2 Availability Check

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|---|---|---|---|
| `checkAvailability_returns200True_whenSufficientStock` | `POST /api/v1/inventory/stock/check-availability` | Bearer token with `INVENTORY_READ`; request body `{productId, locationId, qty: 5}`; `onHandQty = 10` | `200 OK`; response body `{"available": true}` |
| `checkAvailability_returns200False_whenInsufficientStock` | `POST /api/v1/inventory/stock/check-availability` | Bearer token with `INVENTORY_READ`; request body `{productId, locationId, qty: 5}`; `onHandQty = 3` | `200 OK`; response body `{"available": false}` |

### 4.3 Movement History

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|---|---|---|---|
| `getMovements_returns200PagedMovementHistory` | `GET /api/v1/inventory/stock/movements` | Bearer token with `INVENTORY_READ` | `200 OK`; paged JSON response with `content` array of `StockMovementDto` objects |
| `getMovementsByProduct_returns200_withProductMovements` | `GET /api/v1/inventory/stock/movements/product/{productId}` | Bearer token with `INVENTORY_READ` | `200 OK`; all returned movements have the requested `productId` |
| `getMovementsByLocation_returns200_withLocationMovements` | `GET /api/v1/inventory/stock/movements/location/{locationId}` | Bearer token with `INVENTORY_READ` | `200 OK`; all returned movements are associated with the requested `locationId` |

### 4.4 Receive Stock

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|---|---|---|---|
| `receiveStock_returns201_andUpdatesOnHandQty` | `POST /api/v1/inventory/stock/receive` | Bearer token with `INVENTORY_WRITE`; valid request body with `qty = 10` | `201 Created`; response body contains updated `StockDto` with increased `onHandQty`; a corresponding `RECEIPT` movement is retrievable via the movements endpoint |
| `receiveStock_returns400_whenQtyIsZeroOrNegative` | `POST /api/v1/inventory/stock/receive` | Bearer token with `INVENTORY_WRITE`; request body `{qty: 0}` | `400 Bad Request`; error body contains `"Quantity must be positive"` |
| `receiveStock_returns404_whenProductNotFound` | `POST /api/v1/inventory/stock/receive` | Bearer token with `INVENTORY_WRITE`; non-existent `productId` | `404 Not Found`; error body references the product ID |

### 4.5 Issue Stock

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|---|---|---|---|
| `issueStock_returns200_andDecreasesOnHandQty` | `POST /api/v1/inventory/stock/issue` | Bearer token with `INVENTORY_WRITE`; `onHandQty = 20`; request `qty = 5` | `200 OK`; response body has `onHandQty = 15`; an `ISSUE` movement is persisted |
| `issueStock_returns422_whenInsufficientStock` | `POST /api/v1/inventory/stock/issue` | Bearer token with `INVENTORY_WRITE`; `onHandQty = 3`; request `qty = 10` | `422 Unprocessable Entity`; error body contains `"Insufficient stock"` |
| `issueStock_returns400_whenQtyIsInvalid` | `POST /api/v1/inventory/stock/issue` | Bearer token with `INVENTORY_WRITE`; request `qty = -1` | `400 Bad Request`; error body contains validation message |

### 4.6 Transfer Stock

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|---|---|---|---|
| `transferStock_returns200_andUpdatesBothLocations` | `POST /api/v1/inventory/stock/transfer` | Bearer token with `INVENTORY_WRITE`; source `onHandQty = 20`; destination `onHandQty = 5`; `qty = 8` | `200 OK`; source `onHandQty` is `12`; destination `onHandQty` is `13`; two movements (`ISSUE` + `RECEIPT`) are persisted |
| `transferStock_returns422_whenSourceAndDestinationAreSame` | `POST /api/v1/inventory/stock/transfer` | Bearer token with `INVENTORY_WRITE`; `sourceLocationId == destinationLocationId` | `422 Unprocessable Entity`; error body contains `"Source and destination must differ"` |
| `transferStock_returns422_whenInsufficientStockAtSource` | `POST /api/v1/inventory/stock/transfer` | Bearer token with `INVENTORY_WRITE`; source `onHandQty = 2`; request `qty = 15` | `422 Unprocessable Entity`; error body contains `"Insufficient stock"` |

### 4.7 Adjust Stock

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|---|---|---|---|
| `adjustStock_returns200_whenAdjustmentIsPositive` | `POST /api/v1/inventory/stock/adjust` | Bearer token with `INVENTORY_WRITE`; `onHandQty = 10`; `adjustmentQty = 5` | `200 OK`; response body has `onHandQty = 15`; an `ADJUSTMENT` movement is persisted |
| `adjustStock_returns200_whenAdjustmentIsNegative` | `POST /api/v1/inventory/stock/adjust` | Bearer token with `INVENTORY_WRITE`; `onHandQty = 10`; `adjustmentQty = -3` | `200 OK`; response body has `onHandQty = 7`; an `ADJUSTMENT` movement is persisted |
| `adjustStock_returns422_whenResultWouldBeNegative` | `POST /api/v1/inventory/stock/adjust` | Bearer token with `INVENTORY_WRITE`; `onHandQty = 5`; `adjustmentQty = -10` | `422 Unprocessable Entity`; error body contains `"Adjustment would result in negative stock"`; `onHandQty` is unchanged in database |

---

## 5. Coverage Summary

| Component | Unit Tests | Integration Tests | Total Scenarios |
|---|---|---|---|
| `StockService` | 28 | — | 28 |
| `StockRepository` | 7 | — | 7 |
| `StockMovementRepository` | 3 | — | 3 |
| `StockBatchRepository` | 2 | — | 2 |
| `SerialNumberRepository` | 2 | — | 2 |
| `StockReservationRepository` | 2 | — | 2 |
| `StockMapper` | 1 | — | 1 |
| `StockMovementMapper` | 1 | — | 1 |
| `StockBatchMapper` | 1 | — | 1 |
| `StockController` | — | 30 | 30 |
| **Total** | **47** | **30** | **77** |

---

## 6. Test Infrastructure Notes

- **Testcontainers**: All `@DataJpaTest` and `@SpringBootTest` tests use a `PostgreSQLContainer` singleton shared across the test suite via a base class to avoid repeated container spin-up costs.
- **Tenant isolation**: Every repository test inserts records under two tenant IDs and asserts that queries return only data belonging to the queried tenant.
- **Auth tokens**: Integration tests use a `TestTokenFactory` utility that generates signed JWTs with configurable roles and permissions, pointing to an embedded or mocked JWKS endpoint.
- **Transaction management**: `@Transactional` on integration test methods ensures database state is rolled back after each test. Write-operation tests (POST endpoints) use `@Sql` cleanup scripts where rollback is insufficient due to auto-committed side effects.
- **Coverage tooling**: JaCoCo is configured to fail the build if line coverage drops below 100% for packages under `com.hisobnoma.inventory.stock`. Branch coverage threshold is set to 90% to account for generated code (Lombok, MapStruct).
