# Hisobnoma Platform — Complete Test Plan

> **Target**: 100% unit and integration test coverage across every module, service, repository, mapper, and HTTP endpoint.
>
> **Backend stack**: Java 21, Spring Boot 3.3.5, JUnit 5, Mockito, MockMvc, @DataJpaTest, @SpringBootTest, Testcontainers (PostgreSQL)
>
> **Frontend stack**: Vue 3 (Composition API), Pinia, Vitest, @vue/test-utils, MSW (Mock Service Worker), Playwright (E2E)

---


---

# Section 1a: Admin Module — Test Plan

**Platform:** Hisobnoma (Java Spring Boot SaaS)
**Goal:** 100% unit AND integration test coverage for the Admin module
**Scope:** AdminDashboardService, AuditLogService, SystemSettingService, TenantSettingService, AuditLogRepository, Admin Mappers, and all corresponding REST controllers

---

## 1. Unit Tests

### 1.1 AdminDashboardService

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getDashboardStats_activeTenant_returnsNonZeroCounts` | `getDashboardStats(tenantId)` | Tenant has existing users, products, sales, and low-stock items | Returns `DashboardStatsDto` with `userCount > 0`, `productCount > 0`, `todaySalesTotal > 0`, `lowStockCount > 0` |
| `getDashboardStats_newTenant_returnsAllZeroStats` | `getDashboardStats(tenantId)` | Newly created tenant with no data | Returns `DashboardStatsDto` with all numeric fields equal to `0` or `0.0` |
| `getDashboardStats_containsAllExpectedFields` | `getDashboardStats(tenantId)` | Active tenant with known seeded data | Returned DTO contains non-null fields: `userCount`, `productCount`, `todaySalesTotal`, `lowStockCount` |

---

### 1.2 AuditLogService

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getAuditLogs_validTenant_returnsPage` | `getAuditLogs(tenantId, pageable)` | Tenant has audit log entries | Returns `Page<AuditLogDto>` with `totalElements > 0` |
| `getAuditLogs_validTenant_pageContentMatchesTenant` | `getAuditLogs(tenantId, pageable)` | Multiple tenants exist; only one queried | All returned logs have `tenantId` matching the requested tenant |
| `getAuditLogs_noLogs_returnsEmptyPage` | `getAuditLogs(tenantId, pageable)` | No audit logs exist for tenant | Returns `Page<AuditLogDto>` with `totalElements == 0` and empty content list |
| `getAuditLogsByUser_existingUser_returnsFilteredLogs` | `getAuditLogsByUser(tenantId, userId, pageable)` | Logs exist for the given `userId` | Returns page containing only logs where `userId` matches |
| `getAuditLogsByUser_multipleUsers_onlyTargetUserReturned` | `getAuditLogsByUser(tenantId, userId, pageable)` | Logs from multiple users exist | Only logs belonging to the specified `userId` are returned |
| `getAuditLogsByUser_nonExistentUser_returnsEmptyPage` | `getAuditLogsByUser(tenantId, nonExistentUserId, pageable)` | No logs exist for the given `userId` | Returns empty `Page<AuditLogDto>` |
| `getAuditLogsByEntity_existingEntity_returnsFilteredLogs` | `getAuditLogsByEntity(tenantId, "Product", productId, pageable)` | Logs exist for the given entity type and ID | Returns only logs where `entityType == "Product"` and `entityId == productId` |
| `getAuditLogsByEntity_noMatchingEntity_returnsEmptyPage` | `getAuditLogsByEntity(tenantId, "Product", productId, pageable)` | No logs exist for that entity | Returns empty page |
| `getAuditLogsByEntity_differentEntityType_excludedFromResults` | `getAuditLogsByEntity(tenantId, "Product", productId, pageable)` | Logs exist for `Order` but not `Product` | Returns empty page; `Order` logs are excluded |
| `getAuditLogsByAction_deleteAction_returnsOnlyDeleteLogs` | `getAuditLogsByAction(tenantId, "DELETE", pageable)` | Mix of DELETE and CREATE logs exist | Returns only logs with `action == "DELETE"` |
| `getAuditLogsByAction_noMatchingAction_returnsEmptyPage` | `getAuditLogsByAction(tenantId, "DELETE", pageable)` | No DELETE logs exist for tenant | Returns empty page |
| `getAuditLogsByAction_unknownAction_returnsEmptyPage` | `getAuditLogsByAction(tenantId, "PURGE", pageable)` | Action type does not exist in data | Returns empty page without throwing exception |
| `getAuditLogsByModule_inventoryModule_returnsFilteredLogs` | `getAuditLogsByModule(tenantId, "INVENTORY", pageable)` | Logs exist for INVENTORY module | Returns only logs with `module == "INVENTORY"` |
| `getAuditLogsByModule_noMatchingModule_returnsEmptyPage` | `getAuditLogsByModule(tenantId, "INVENTORY", pageable)` | No INVENTORY logs exist | Returns empty page |
| `getAuditLogsByModule_multipleModules_onlyTargetReturned` | `getAuditLogsByModule(tenantId, "INVENTORY", pageable)` | Logs exist for INVENTORY and POS | Only INVENTORY logs are returned |
| `getAuditLogsByDateRange_validRange_returnsFilteredLogs` | `getAuditLogsByDateRange(tenantId, startDate, endDate, pageable)` | Logs exist within and outside the date range | Returns only logs with `timestamp` between `startDate` and `endDate` inclusive |
| `getAuditLogsByDateRange_noLogsInRange_returnsEmptyPage` | `getAuditLogsByDateRange(tenantId, startDate, endDate, pageable)` | No logs exist within the specified range | Returns empty page |
| `getAuditLogsByDateRange_endBeforeStart_returnsEmptyOrThrows` | `getAuditLogsByDateRange(tenantId, endDate before startDate, pageable)` | `endDate` is chronologically before `startDate` | Returns empty page or throws `IllegalArgumentException` |
| `getFailedActions_hasFailed_returnsOnlyFailedLogs` | `getFailedActions(tenantId, pageable)` | Mix of failed and successful action logs | Returns only logs where `failed == true` |
| `getFailedActions_noFailedActions_returnsEmptyPage` | `getFailedActions(tenantId, pageable)` | All logs have `failed == false` | Returns empty page |
| `getFailedActions_allFailed_returnsAllLogs` | `getFailedActions(tenantId, pageable)` | All logs for tenant have `failed == true` | Returns page with all tenant logs |
| `getActionStats_sevenDays_returnsActionCountMap` | `getActionStats(tenantId, days=7)` | Logs with various actions exist in last 7 days | Returns `Map<String, Long>` where each key is an action name and value is its count |
| `getActionStats_noActivity_returnsEmptyMap` | `getActionStats(tenantId, days=7)` | No logs within last 7 days | Returns empty map |
| `getActionStats_countAccuracy_matchesExpected` | `getActionStats(tenantId, days=7)` | 3 DELETE and 5 CREATE actions in last 7 days | Map contains `{"DELETE": 3, "CREATE": 5}` |
| `getModuleStats_thirtyDays_returnsModuleCountMap` | `getModuleStats(tenantId, days=30)` | Logs across multiple modules in last 30 days | Returns `Map<String, Long>` keyed by module name |
| `getModuleStats_noActivity_returnsEmptyMap` | `getModuleStats(tenantId, days=30)` | No logs within last 30 days | Returns empty map |
| `getModuleStats_countAccuracy_matchesExpected` | `getModuleStats(tenantId, days=30)` | 10 INVENTORY, 4 POS actions in last 30 days | Map contains `{"INVENTORY": 10, "POS": 4}` |
| `getMostActiveUsers_thirtyDays_returnsSortedList` | `getMostActiveUsers(tenantId, days=30)` | Multiple users with varying activity levels | Returns list sorted by `actionCount` descending |
| `getMostActiveUsers_noActivity_returnsEmptyList` | `getMostActiveUsers(tenantId, days=30)` | No activity in last 30 days | Returns empty list |
| `getMostActiveUsers_sortOrderCorrect` | `getMostActiveUsers(tenantId, days=30)` | User A: 10 actions, User B: 3 actions, User C: 7 actions | List order: User A (10), User C (7), User B (3) |
| `getFailedLoginCount_last24Hours_returnsCorrectCount` | `getFailedLoginCount(tenantId, hours=24)` | 5 failed logins in last 24h, 3 older than 24h | Returns integer `5` |
| `getFailedLoginCount_noFailedLogins_returnsZero` | `getFailedLoginCount(tenantId, hours=24)` | No failed login events exist | Returns `0` |
| `getFailedLoginCount_exactBoundary_inclusiveOfBoundary` | `getFailedLoginCount(tenantId, hours=24)` | Failed login exactly 24 hours ago | Returns count that includes the boundary event |

---

### 1.3 SystemSettingService

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getAllSettings_settingsExist_returnsAllSettings` | `getAllSettings()` | Multiple system settings are persisted | Returns non-empty `List<SystemSettingDto>` containing all settings |
| `getAllSettings_noSettings_returnsEmptyList` | `getAllSettings()` | No system settings exist | Returns empty list |
| `getAllSettings_returnsCorrectCount` | `getAllSettings()` | Exactly 5 settings exist | Returns list with 5 elements |
| `getCategories_returnsDistinctList` | `getCategories()` | Settings span 3 categories including duplicates | Returns list of 3 distinct category strings |
| `getCategories_noSettings_returnsEmptyList` | `getCategories()` | No settings exist | Returns empty list |
| `getCategories_singleCategory_returnsSingleEntry` | `getCategories()` | All settings share the same category | Returns list with exactly one entry |
| `getSettingsByCategory_securityCategory_returnsFiltered` | `getSettingsByCategory("SECURITY")` | Settings exist in SECURITY and other categories | Returns only SECURITY category settings |
| `getSettingsByCategory_nonExistent_returnsEmptyList` | `getSettingsByCategory("NONEXISTENT")` | Category does not exist | Returns empty list without throwing exception |
| `getSettingsByCategory_caseSensitive_noMatch` | `getSettingsByCategory("security")` | Only "SECURITY" (uppercase) exists | Returns empty list if matching is case-sensitive |
| `getSetting_existingKey_returnsDto` | `getSetting("session.timeout")` | Setting with key `session.timeout` exists | Returns `SystemSettingDto` with correct key and value |
| `getSetting_nonExistentKey_throwsNotFoundException` | `getSetting("nonexistent.key")` | Key does not exist | Throws `NotFoundException` (or `ResourceNotFoundException`) |
| `getSetting_nullKey_throwsException` | `getSetting(null)` | Null key passed | Throws `IllegalArgumentException` or `NotFoundException` |
| `createSetting_validDto_settingSaved` | `createSetting(dto)` | Valid DTO with unique key | Setting is persisted; repository `save` called once; returned DTO matches input |
| `createSetting_duplicateKey_throwsDuplicateResourceException` | `createSetting(dto with duplicate key)` | Key already exists in system settings | Throws `DuplicateResourceException`; repository `save` never called |
| `createSetting_nullKey_throwsValidationException` | `createSetting(dto with null key)` | DTO has null key field | Throws validation or constraint exception |
| `updateSetting_existingKey_successfullyUpdated` | `updateSetting("session.timeout", dto)` | Key exists; DTO has new value | Setting updated; returned DTO reflects new values |
| `updateSetting_nonExistentKey_throwsNotFoundException` | `updateSetting("nonexistent.key", dto)` | Key does not exist | Throws `NotFoundException`; no save performed |
| `updateSetting_sameValue_stillSucceeds` | `updateSetting("session.timeout", dto)` | DTO value same as existing | Returns updated DTO; no exception thrown |
| `updateSettingValue_existingKey_valueUpdated` | `updateSettingValue("session.timeout", "60")` | Key exists; new raw string value provided | Only the value field is updated; returns updated `SystemSettingDto` |
| `updateSettingValue_nonExistentKey_throwsNotFoundException` | `updateSettingValue("nonexistent.key", "60")` | Key does not exist | Throws `NotFoundException` |
| `updateSettingValue_emptyValue_successfullySetToEmpty` | `updateSettingValue("session.timeout", "")` | Empty string value provided | Value set to empty string; no exception thrown |
| `updateSettings_mapOfThreeKeys_allThreeUpdated` | `updateSettings(Map of 3 keys)` | 3 valid keys exist; map provides new values for each | All 3 settings updated; repository `save` called 3 times (or batch) |
| `updateSettings_emptyMap_noUpdatesPerformed` | `updateSettings(empty Map)` | Empty map provided | No repository calls made; returns without error |
| `updateSettings_mapWithOneInvalidKey_throwsNotFoundException` | `updateSettings(Map with 1 invalid key)` | 1 of 3 keys does not exist | Throws `NotFoundException`; behavior for others depends on transactional policy |
| `deleteSetting_existingKey_settingRemoved` | `deleteSetting("session.timeout")` | Key exists | Setting deleted; repository `delete` or `deleteById` called once |
| `deleteSetting_nonExistentKey_throwsNotFoundException` | `deleteSetting("nonexistent.key")` | Key does not exist | Throws `NotFoundException`; no delete attempted |
| `deleteSetting_alreadyDeleted_throwsNotFoundException` | `deleteSetting("session.timeout")` | Key was previously deleted | Throws `NotFoundException` |

---

### 1.4 TenantSettingService

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getAllSettings_validTenant_returnsAllTenantSettings` | `getAllSettings(tenantId)` | Tenant has multiple settings | Returns `List<TenantSettingDto>` for that tenant only |
| `getAllSettings_noSettings_returnsEmptyList` | `getAllSettings(tenantId)` | Tenant has no settings | Returns empty list |
| `getAllSettings_multiTenant_isolatedToRequestedTenant` | `getAllSettings(tenantId)` | Two tenants with settings exist | Only settings for requested tenant returned |
| `getCategories_validTenant_returnsDistinctCategories` | `getCategories(tenantId)` | Tenant settings span multiple categories with duplicates | Returns distinct category list for that tenant |
| `getCategories_noSettings_returnsEmptyList` | `getCategories(tenantId)` | Tenant has no settings | Returns empty list |
| `getCategories_multiTenant_isolatedCategories` | `getCategories(tenantId)` | Two tenants with different categories | Only categories belonging to the requested tenant returned |
| `getSettingsByCategory_posCategory_returnsFiltered` | `getSettingsByCategory(tenantId, "POS")` | Tenant has POS and non-POS settings | Returns only POS category settings for that tenant |
| `getSettingsByCategory_nonExistent_returnsEmptyList` | `getSettingsByCategory(tenantId, "NONEXISTENT")` | Category does not exist for tenant | Returns empty list |
| `getSettingsByCategory_otherTenantCategory_notReturned` | `getSettingsByCategory(tenantId, "POS")` | Another tenant has POS settings; this tenant does not | Returns empty list; cross-tenant isolation maintained |
| `createSetting_validDto_persistedForTenant` | `createSetting(tenantId, dto)` | Valid DTO with unique key for tenant | Setting saved with correct `tenantId`; returned DTO matches |
| `createSetting_duplicateKey_throwsDuplicateResourceException` | `createSetting(tenantId, dto with duplicate key)` | Key already exists for this tenant | Throws `DuplicateResourceException` |
| `createSetting_sameKeyDifferentTenant_succeeds` | `createSetting(tenantId, dto)` | Same key exists for another tenant, not this one | Setting created successfully; no exception thrown |
| `updateSetting_validKey_settingUpdated` | `updateSetting(tenantId, key, dto)` | Key exists for tenant; new values provided | Setting updated; returned DTO reflects changes |
| `updateSetting_nonExistentKey_throwsNotFoundException` | `updateSetting(tenantId, nonexistent key)` | Key does not exist for tenant | Throws `NotFoundException` |
| `updateSetting_wrongTenant_throwsNotFoundException` | `updateSetting(tenantId, key, dto)` | Key exists for another tenant, not this one | Throws `NotFoundException`; cross-tenant isolation maintained |
| `updateSettingValue_existingKey_valueUpdated` | `updateSettingValue(tenantId, key, newValue)` | Key exists; new raw value provided | Only value field updated; returns updated DTO |
| `updateSettingValue_nonExistentKey_throwsNotFoundException` | `updateSettingValue(tenantId, nonexistent key, newValue)` | Key does not exist | Throws `NotFoundException` |
| `updateSettingValue_crossTenantKey_throwsNotFoundException` | `updateSettingValue(tenantId, key, newValue)` | Key belongs to a different tenant | Throws `NotFoundException` |
| `updateSettings_mapOfKeys_allUpdated` | `updateSettings(tenantId, Map of keys)` | All keys exist for tenant | All settings updated; returns confirmation |
| `updateSettings_emptyMap_noOp` | `updateSettings(tenantId, empty Map)` | Empty map provided | No repository calls; returns without error |
| `updateSettings_partialInvalidKeys_throwsNotFoundException` | `updateSettings(tenantId, Map with 1 invalid key)` | One key does not exist for tenant | Throws `NotFoundException` |
| `getSettingsAsMap_validTenant_returnsStringMap` | `getSettingsAsMap(tenantId)` | Tenant has 4 settings | Returns `Map<String, String>` with 4 entries; keys and values match setting records |
| `getSettingsAsMap_noSettings_returnsEmptyMap` | `getSettingsAsMap(tenantId)` | Tenant has no settings | Returns empty `Map<String, String>` |
| `getSettingsAsMap_multiTenant_isolatedToRequestedTenant` | `getSettingsAsMap(tenantId)` | Two tenants with settings | Map contains only entries for the requested tenant |

---

## 2. Repository Tests (`@DataJpaTest` + Testcontainers)

### 2.1 AuditLogRepository

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `findByTenantIdAndDateRange_withinRange_returnsMatchingLogs` | `findByTenantIdAndDateRange(tenantId, startDate, endDate)` | Logs at T+1h and T+2h; range is T to T+3h | Both logs returned |
| `findByTenantIdAndDateRange_outsideRange_excluded` | `findByTenantIdAndDateRange(tenantId, startDate, endDate)` | Log at T-1h (before range) and T+5h (after range); range is T to T+3h | Neither log returned |
| `findByTenantIdAndDateRange_boundaryDates_inclusive` | `findByTenantIdAndDateRange(tenantId, startDate, endDate)` | Logs exactly at `startDate` and `endDate` | Both boundary logs included |
| `countFailedLoginsSince_hasFailedLogins_returnsCorrectCount` | `countFailedLoginsSince(tenantId, since)` | 4 failed login logs after `since`; 2 before | Returns `4` |
| `countFailedLoginsSince_noFailedLogins_returnsZero` | `countFailedLoginsSince(tenantId, since)` | No failed login events for tenant after `since` | Returns `0` |
| `countFailedLoginsSince_multiTenant_isolatedCount` | `countFailedLoginsSince(tenantId, since)` | Other tenant has failed logins; this tenant does not | Returns `0`; cross-tenant isolation correct |
| `countModuleActivityByTenantIdSince_multipleModules_returnsEntries` | `countModuleActivityByTenantIdSince(tenantId, since)` | 6 INVENTORY and 2 POS actions after `since` | Returns result with `INVENTORY=6`, `POS=2` |
| `countModuleActivityByTenantIdSince_noActivity_returnsEmptyResult` | `countModuleActivityByTenantIdSince(tenantId, since)` | No logs after `since` | Returns empty result set |
| `findMostActiveUsersByTenantIdSince_multipleUsers_sortedByCountDesc` | `findMostActiveUsersByTenantIdSince(tenantId, since)` | User A: 8 actions, User B: 2 actions, User C: 5 actions | Returned list order: User A (8), User C (5), User B (2) |
| `findMostActiveUsersByTenantIdSince_noActivity_returnsEmptyList` | `findMostActiveUsersByTenantIdSince(tenantId, since)` | No logs after `since` | Returns empty list |

---

## 3. Mapper Tests

### 3.1 AuditLogMapper

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `toDto_allFields_mappedCorrectly` | `AuditLogMapper.toDto(entity)` | `AuditLog` entity with all fields populated | Returned `AuditLogDto` has identical values for `id`, `tenantId`, `userId`, `action`, `entityType`, `entityId`, `module`, `timestamp`, `failed`, `details` |
| `toDto_nullOptionalFields_handledGracefully` | `AuditLogMapper.toDto(entity)` | Entity with null `details` and `entityId` | DTO has null for those fields; no `NullPointerException` |
| `toDto_failedFlag_preservedCorrectly` | `AuditLogMapper.toDto(entity)` | Entity with `failed = true` | DTO has `failed == true` |

---

### 3.2 SystemSettingMapper

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `toDto_allFields_mappedCorrectly` | `SystemSettingMapper.toDto(entity)` | `SystemSetting` entity with all fields set | Returned `SystemSettingDto` has identical `key`, `value`, `category`, `description`, `dataType`, `editable` values |
| `toDto_nullDescription_handledGracefully` | `SystemSettingMapper.toDto(entity)` | Entity with null `description` | DTO `description` is null; no exception thrown |
| `fromDto_allFields_entityCreatedCorrectly` | `SystemSettingMapper.fromDto(dto)` | `SystemSettingDto` with all fields populated | Returned `SystemSetting` entity has matching field values |
| `fromDto_idNotCopied_entityHasNoId` | `SystemSettingMapper.fromDto(dto)` | DTO does not carry an ID (creation scenario) | Entity `id` is null; appropriate for a new entity |

---

### 3.3 TenantSettingMapper

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `toDto_allFields_mappedCorrectly` | `TenantSettingMapper.toDto(entity)` | `TenantSetting` entity with all fields set | Returned `TenantSettingDto` has identical `tenantId`, `key`, `value`, `category`, `description` values |
| `toDto_nullValue_handledGracefully` | `TenantSettingMapper.toDto(entity)` | Entity with null `value` | DTO `value` is null; no exception thrown |
| `fromDto_allFields_entityCreatedCorrectly` | `TenantSettingMapper.fromDto(dto)` | `TenantSettingDto` with all fields populated | Returned `TenantSetting` entity has matching field values |
| `fromDto_tenantIdPreserved` | `TenantSettingMapper.fromDto(dto)` | DTO includes `tenantId` | Entity `tenantId` matches the DTO's `tenantId` |

---

## 4. Integration Tests

> All integration tests use `@SpringBootTest` with `MockMvc` (or `WebTestClient`) and a Testcontainers-managed PostgreSQL instance. JWT tokens or session tokens are minted per test case to control authentication and authorization.

---

### 4.1 AdminDashboardController — `GET /api/v1/admin/dashboard/stats`

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|---|---|---|---|
| `getDashboardStats_authenticatedAdmin_returns200WithStatsBody` | `GET /api/v1/admin/dashboard/stats` | Valid JWT with `ROLE_ADMIN` | `200 OK`; body contains JSON object with fields `userCount`, `productCount`, `todaySalesTotal`, `lowStockCount` |
| `getDashboardStats_unauthenticated_returns401` | `GET /api/v1/admin/dashboard/stats` | No token / no session | `401 Unauthorized`; body contains error message |
| `getDashboardStats_noAdminPermission_returns403` | `GET /api/v1/admin/dashboard/stats` | Valid JWT with `ROLE_USER` (no `ROLE_ADMIN`) | `403 Forbidden`; body contains error message |

---

### 4.2 AuditLogController — `/api/v1/admin/audit-logs`

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|---|---|---|---|
| `getAuditLogs_adminRead_returns200PaginatedList` | `GET /api/v1/admin/audit-logs` | `ADMIN_READ` permission | `200 OK`; body is paginated JSON with `content`, `totalElements`, `totalPages` |
| `getAuditLogs_noAdminReadPermission_returns403` | `GET /api/v1/admin/audit-logs` | Valid JWT without `ADMIN_READ` | `403 Forbidden` |
| `getAuditLogsByUser_existingUser_returns200FilteredList` | `GET /api/v1/admin/audit-logs/user/{userId}` | `ADMIN_READ` permission | `200 OK`; body contains page of logs filtered to `userId` |
| `getAuditLogsByUser_nonExistentUser_returns200EmptyPage` | `GET /api/v1/admin/audit-logs/user/{userId}` | `ADMIN_READ` permission; `userId` not in system | `200 OK`; body has `totalElements: 0` and empty `content` array (not `404`) |
| `getAuditLogsByEntity_validEntityTypeAndId_returns200` | `GET /api/v1/admin/audit-logs/entity/{entityType}/{entityId}` | `ADMIN_READ` permission | `200 OK`; body contains only logs matching entity type and ID |
| `getAuditLogsByAction_validAction_returns200Filtered` | `GET /api/v1/admin/audit-logs/action/{action}` | `ADMIN_READ` permission | `200 OK`; body contains only logs with matching action |
| `getAuditLogsByModule_validModule_returns200Filtered` | `GET /api/v1/admin/audit-logs/module/{module}` | `ADMIN_READ` permission | `200 OK`; body contains only logs with matching module |
| `getAuditLogsByDateRange_validDates_returns200Filtered` | `GET /api/v1/admin/audit-logs/date-range?start=&end=` | `ADMIN_READ` permission | `200 OK`; body contains only logs within the specified date range |
| `getAuditLogsByDateRange_invalidDateFormat_returns400` | `GET /api/v1/admin/audit-logs/date-range?start=notadate&end=alsonotadate` | `ADMIN_READ` permission | `400 Bad Request`; body contains validation error message |
| `getFailedActions_adminRead_returns200OnlyFailed` | `GET /api/v1/admin/audit-logs/failed` | `ADMIN_READ` permission | `200 OK`; all items in `content` have `failed: true` |
| `getActionStats_sevenDays_returns200Map` | `GET /api/v1/admin/audit-logs/stats/actions?days=7` | `ADMIN_READ` permission | `200 OK`; body is JSON object mapping action strings to long counts |
| `getModuleStats_thirtyDays_returns200Map` | `GET /api/v1/admin/audit-logs/stats/modules?days=30` | `ADMIN_READ` permission | `200 OK`; body is JSON object mapping module strings to long counts |
| `getMostActiveUsers_thirtyDays_returns200List` | `GET /api/v1/admin/audit-logs/stats/users?days=30` | `ADMIN_READ` permission | `200 OK`; body is JSON array of objects with `userId` and `actionCount`, sorted descending |
| `getFailedLoginCount_24Hours_returns200Integer` | `GET /api/v1/admin/audit-logs/stats/failed-logins?hours=24` | `ADMIN_READ` permission | `200 OK`; body is a JSON integer (e.g., `5`) |

---

### 4.3 SystemSettingController — `/api/v1/admin/settings/system`

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|---|---|---|---|
| `getAllSystemSettings_admin_returns200AllSettings` | `GET /api/v1/admin/settings/system` | `ROLE_ADMIN` | `200 OK`; body is JSON array of all system setting DTOs |
| `getSystemSettingCategories_admin_returns200CategoryList` | `GET /api/v1/admin/settings/system/categories` | `ROLE_ADMIN` | `200 OK`; body is JSON array of distinct category strings |
| `getSystemSettingsByCategory_validCategory_returns200Filtered` | `GET /api/v1/admin/settings/system/category/{category}` | `ROLE_ADMIN` | `200 OK`; body contains only settings matching the category |
| `getSystemSettingByKey_existingKey_returns200` | `GET /api/v1/admin/settings/system/{key}` | `ROLE_ADMIN` | `200 OK`; body contains the setting DTO for the given key |
| `getSystemSettingByKey_nonExistentKey_returns404` | `GET /api/v1/admin/settings/system/{key}` | `ROLE_ADMIN`; key does not exist | `404 Not Found`; body contains error message |
| `createSystemSetting_validPayload_returns201` | `POST /api/v1/admin/settings/system` | `ROLE_ADMIN`; valid request body | `201 Created`; body contains newly created setting DTO with generated ID |
| `createSystemSetting_duplicateKey_returns409` | `POST /api/v1/admin/settings/system` | `ROLE_ADMIN`; key already exists | `409 Conflict`; body contains conflict error message |
| `createSystemSetting_invalidPayload_returns400` | `POST /api/v1/admin/settings/system` | `ROLE_ADMIN`; missing required fields | `400 Bad Request`; body contains validation errors |
| `updateSystemSetting_existingKey_returns200` | `PUT /api/v1/admin/settings/system/{key}` | `ROLE_ADMIN`; key exists | `200 OK`; body contains updated setting DTO |
| `updateSystemSetting_nonExistentKey_returns404` | `PUT /api/v1/admin/settings/system/{key}` | `ROLE_ADMIN`; key does not exist | `404 Not Found` |
| `updateSystemSettingValue_existingKey_returns200` | `PUT /api/v1/admin/settings/system/{key}/value` | `ROLE_ADMIN`; key exists | `200 OK`; body contains DTO with updated value only |
| `updateSystemSettingsBatch_validMap_returns200AllUpdated` | `PUT /api/v1/admin/settings/system/batch` | `ROLE_ADMIN`; all keys exist | `200 OK`; body confirms all settings updated |
| `deleteSystemSetting_existingKey_returns204` | `DELETE /api/v1/admin/settings/system/{key}` | `ROLE_ADMIN`; key exists | `204 No Content`; empty body |
| `deleteSystemSetting_nonExistentKey_returns404` | `DELETE /api/v1/admin/settings/system/{key}` | `ROLE_ADMIN`; key does not exist | `404 Not Found` |
| `allSystemSettingEndpoints_noAdminPermission_returns403` | `GET/POST/PUT/DELETE /api/v1/admin/settings/system/**` | Valid JWT without `ROLE_ADMIN` | `403 Forbidden` for all endpoints |

---

### 4.4 TenantSettingController — `/api/v1/admin/settings/tenant`

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|---|---|---|---|
| `getAllTenantSettings_admin_returns200` | `GET /api/v1/admin/settings/tenant` | `ROLE_ADMIN` | `200 OK`; body is JSON array of all tenant setting DTOs |
| `getTenantSettingCategories_admin_returns200` | `GET /api/v1/admin/settings/tenant/categories` | `ROLE_ADMIN` | `200 OK`; body is JSON array of distinct category strings |
| `getTenantSettingsByCategory_validCategory_returns200` | `GET /api/v1/admin/settings/tenant/category/{category}` | `ROLE_ADMIN` | `200 OK`; body contains only settings matching the category |
| `getTenantSettingsAsMap_admin_returns200Map` | `GET /api/v1/admin/settings/tenant/map` | `ROLE_ADMIN` | `200 OK`; body is JSON object (`Map<String, String>`) of all tenant settings |
| `createTenantSetting_validPayload_returns201` | `POST /api/v1/admin/settings/tenant` | `ROLE_ADMIN`; unique key | `201 Created`; body contains newly created tenant setting DTO |
| `createTenantSetting_duplicateKey_returns409` | `POST /api/v1/admin/settings/tenant` | `ROLE_ADMIN`; key already exists for tenant | `409 Conflict` |
| `updateTenantSetting_existingKey_returns200` | `PUT /api/v1/admin/settings/tenant/{key}` | `ROLE_ADMIN`; key exists | `200 OK`; body contains updated tenant setting DTO |
| `updateTenantSetting_nonExistentKey_returns404` | `PUT /api/v1/admin/settings/tenant/{key}` | `ROLE_ADMIN`; key does not exist | `404 Not Found` |
| `updateTenantSettingValue_existingKey_returns200` | `PUT /api/v1/admin/settings/tenant/{key}/value` | `ROLE_ADMIN`; key exists | `200 OK`; body contains updated DTO with new value |
| `updateTenantSettingsBatch_validMap_returns200` | `PUT /api/v1/admin/settings/tenant/batch` | `ROLE_ADMIN`; all keys exist | `200 OK`; confirms all settings updated |
| `allTenantSettingEndpoints_noAdminPermission_returns403` | `GET/POST/PUT /api/v1/admin/settings/tenant/**` | Valid JWT without `ROLE_ADMIN` | `403 Forbidden` for all endpoints |

---

## 5. Coverage Summary

| Layer | Classes Covered | Target Coverage |
|---|---|---|
| Service (Unit) | `AdminDashboardService`, `AuditLogService`, `SystemSettingService`, `TenantSettingService` | 100% line + branch |
| Repository (`@DataJpaTest`) | `AuditLogRepository` | 100% of custom query methods |
| Mapper (Unit) | `AuditLogMapper`, `SystemSettingMapper`, `TenantSettingMapper` | 100% line |
| Controller (Integration) | `AdminDashboardController`, `AuditLogController`, `SystemSettingController`, `TenantSettingController` | 100% endpoints + all auth/permission paths |

---

# Section 1b: Auth Module — Test Plan

## Overview

This test plan targets **100% unit and integration test coverage** for the Auth module of the Hisobnoma SaaS platform. It covers `AuthService`, `UserService`, `RoleService`, `CustomUserDetailsService`, `PermissionCacheService`, repository layer, mappers, and all HTTP endpoints exposed by `AuthController`, `UserController`, and `RoleController`.

---

## Unit Tests

### AuthService

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|------------------|
| `login_validCredentials_returnsAuthResponse` | `login(LoginRequest)` | Username and password are correct, user is active | Returns `AuthResponse` containing non-null `accessToken` and `refreshToken` |
| `login_wrongPassword_throwsUnauthorizedException` | `login(LoginRequest)` | Username exists but password does not match stored hash | Throws `UnauthorizedException` |
| `login_lockedUser_throwsForbiddenException` | `login(LoginRequest)` | User exists and password is correct but `enabled = false` | Throws `ForbiddenException` with message `"Account locked"` |
| `login_nonExistentUser_throwsUnauthorizedException` | `login(LoginRequest)` | Username does not exist in the repository | Throws `UnauthorizedException` |
| `login_userFromDifferentTenant_throwsUnauthorizedException` | `login(LoginRequest)` | Username found but belongs to a different `tenantId` than the request context | Throws `UnauthorizedException` |
| `pinLogin_validPin_returnsAuthResponse` | `pinLogin(PinLoginRequest)` | Correct 4–6-digit PIN provided for user who has a PIN set | Returns `AuthResponse` with valid access and refresh tokens |
| `pinLogin_wrongPin_throwsUnauthorizedException` | `pinLogin(PinLoginRequest)` | PIN provided does not match stored PIN hash | Throws `UnauthorizedException` |
| `pinLogin_noPinSet_throwsBusinessException` | `pinLogin(PinLoginRequest)` | User exists but `pinHash` is `null` | Throws `BusinessException` with message `"PIN not set"` |
| `refresh_validRefreshToken_returnsNewAuthResponse` | `refresh(RefreshRequest)` | Token exists in DB, is not revoked, and has not expired | Returns new `AuthResponse` with freshly issued access and refresh tokens |
| `refresh_expiredRefreshToken_throwsUnauthorizedException` | `refresh(RefreshRequest)` | Token exists but `expiresAt` is in the past | Throws `UnauthorizedException` |
| `refresh_revokedRefreshToken_throwsUnauthorizedException` | `refresh(RefreshRequest)` | Token exists but `revoked = true` | Throws `UnauthorizedException` |
| `refresh_nonExistentToken_throwsUnauthorizedException` | `refresh(RefreshRequest)` | Token string not found in `RefreshTokenRepository` | Throws `UnauthorizedException` |
| `logout_authenticatedUser_revokesRefreshToken` | `logout()` | Valid `SecurityContext` principal; token exists in DB | Refresh token for current user is marked `revoked = true` in the database |
| `changePassword_correctOldPassword_updatesPasswordAndRevokesTokens` | `changePassword(ChangePasswordRequest)` | Old password matches, new password is different | Password hash updated; all refresh tokens for the user are revoked |
| `changePassword_wrongOldPassword_throwsValidationException` | `changePassword(ChangePasswordRequest)` | Provided old password does not match current stored hash | Throws `ValidationException` |
| `changePassword_sameAsOldPassword_throwsValidationException` | `changePassword(ChangePasswordRequest)` | New password is identical to current stored hash | Throws `ValidationException` with message `"Must be different"` |
| `forgotPassword_existingEmail_createsTokenAndSendsEmail` | `forgotPassword(ForgotPasswordRequest)` | Email belongs to an existing user | Reset token persisted in DB; mocked `EmailService.send()` called exactly once |
| `forgotPassword_nonExistentEmail_noExceptionNoEmail` | `forgotPassword(ForgotPasswordRequest)` | Email does not match any user record | No exception thrown (silent fail for security); mocked `EmailService.send()` never called |
| `resetPassword_validToken_updatesPasswordAndMarksTokenUsed` | `resetPassword(ResetPasswordRequest)` | Token is valid, not expired, not used; passwords match | Password updated; token `usedAt` set to current timestamp |
| `resetPassword_expiredToken_throwsBusinessException` | `resetPassword(ResetPasswordRequest)` | Token exists but `expiresAt` is in the past | Throws `BusinessException` with message `"Token expired"` |
| `resetPassword_alreadyUsedToken_throwsBusinessException` | `resetPassword(ResetPasswordRequest)` | Token exists but `usedAt` is non-null | Throws `BusinessException` with message `"Token already used"` |
| `getCurrentUser_authenticated_returnsUserDto` | `getCurrentUser()` | Valid principal present in `SecurityContextHolder` | Returns `UserDto` corresponding to the authenticated user |
| `getCurrentUser_unauthenticated_throwsUnauthorizedException` | `getCurrentUser()` | `SecurityContextHolder` contains no authentication | Throws `UnauthorizedException` |

---

### UserService

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|------------------|
| `getUsers_noSearch_returnsAllTenantUsersPaged` | `getUsers(tenantId, pageable, search)` | `search = null`; tenant has multiple users | Returns paginated `Page<UserDto>` containing all users for the given `tenantId` |
| `getUsers_withSearch_returnsFilteredUsers` | `getUsers(tenantId, pageable, search)` | `search = "john"`; some users match, some do not | Returns only users whose name/username contains `"john"` |
| `getUser_existingId_returnsUserDto` | `getUser(tenantId, userId)` | User exists and belongs to the tenant | Returns populated `UserDto` with correct fields |
| `getUser_nonExistentId_throwsNotFoundException` | `getUser(tenantId, userId)` | No user found for the given `userId` within the tenant | Throws `NotFoundException` |
| `createUser_validRequest_createsUserWithHashedPasswordAndDefaultRole` | `createUser(tenantId, CreateUserRequest)` | Username and phone are unique; no duplicate exists | User entity persisted; `passwordHash` is a BCrypt hash (not plain text); default role assigned |
| `createUser_duplicateUsername_throwsDuplicateResourceException` | `createUser(tenantId, CreateUserRequest)` | A user with the same `username` already exists in the tenant | Throws `DuplicateResourceException` |
| `createUser_duplicatePhone_throwsDuplicateResourceException` | `createUser(tenantId, CreateUserRequest)` | A user with the same `phone` already exists in the tenant | Throws `DuplicateResourceException` |
| `updateUser_validRequest_updatesFieldsAndReturnsDto` | `updateUser(tenantId, userId, UpdateUserRequest)` | User exists; all provided fields are valid and unique | Entity updated; returned `UserDto` reflects new values |
| `updateUser_nonExistentId_throwsNotFoundException` | `updateUser(tenantId, userId, UpdateUserRequest)` | No user found for the given `userId` | Throws `NotFoundException` |
| `updateUser_duplicateUsername_throwsDuplicateResourceException` | `updateUser(tenantId, userId, UpdateUserRequest)` | New username already taken by a different user in the same tenant | Throws `DuplicateResourceException` |
| `deleteUser_existingId_deletesUser` | `deleteUser(tenantId, userId)` | User exists and is not the currently authenticated user | User is deleted (or soft-deleted); repository `delete`/`save` called |
| `deleteUser_nonExistentId_throwsNotFoundException` | `deleteUser(tenantId, userId)` | No user found for the given `userId` | Throws `NotFoundException` |
| `deleteUser_currentUserId_throwsBusinessException` | `deleteUser(tenantId, userId)` | `userId` matches the currently authenticated principal's ID | Throws `BusinessException` with message `"Cannot delete yourself"` |
| `assignRoles_validRoleCodes_replacesRolesOnUser` | `assignRoles(tenantId, userId, List<String>)` | Both `"CASHIER"` and `"MANAGER"` codes exist in DB | User's role collection replaced entirely with the two resolved roles |
| `assignRoles_invalidRoleCode_throwsNotFoundException` | `assignRoles(tenantId, userId, List<String>)` | `"INVALID_CODE"` does not match any role in DB | Throws `NotFoundException` for the unrecognised role code |
| `lockUser_enabledFalse_disablesUser` | `lockUser(tenantId, userId, lock)` | `lock = true`; user currently enabled | `user.enabled` set to `false` and persisted |
| `lockUser_enabledTrue_enablesUser` | `lockUser(tenantId, userId, lock)` | `lock = false`; user currently disabled | `user.enabled` set to `true` and persisted |
| `lockUser_nonExistentId_throwsNotFoundException` | `lockUser(tenantId, userId, lock)` | No user found for the given `userId` | Throws `NotFoundException` |
| `resetPassword_existingUser_hashesAndSavesPassword` | `resetPassword(tenantId, userId, newPassword)` | User exists; new password provided by admin | `passwordHash` updated with BCrypt hash of `newPassword`; entity saved |
| `setUserPin_validPin_hashesAndSavesPin` | `setUserPin(tenantId, userId, pin)` | PIN is `"1234"` — 4 numeric digits | `pinHash` updated with hash of `"1234"`; entity saved |
| `setUserPin_tooShortPin_throwsValidationException` | `setUserPin(tenantId, userId, pin)` | PIN is `"12"` — fewer than 4 digits | Throws `ValidationException` with message `"PIN must be 4-6 digits"` |
| `setUserPin_nonNumericPin_throwsValidationException` | `setUserPin(tenantId, userId, pin)` | PIN is `"abcd"` — contains non-digit characters | Throws `ValidationException` with message `"PIN must be numeric"` |

---

### RoleService

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|------------------|
| `getRoles_tenantId_returnsTenantAndSystemRoles` | `getRoles(tenantId, pageable)` | Tenant has custom roles; system roles also exist | Returns paginated result combining both tenant-scoped and `isSystem = true` roles |
| `getRole_existingId_returnsRoleDtoWithPermissions` | `getRole(tenantId, roleId)` | Role exists and belongs to tenant (or is a system role) | Returns `RoleDto` with fully populated `permissions` list |
| `getRole_nonExistentId_throwsNotFoundException` | `getRole(tenantId, roleId)` | No role found for the given `roleId` | Throws `NotFoundException` |
| `createRole_validRequest_createsRole` | `createRole(tenantId, CreateRoleRequest)` | Code is unique within tenant; request fields are valid | Role entity persisted with correct `code`, `name`, and `tenantId` |
| `createRole_duplicateCode_throwsDuplicateResourceException` | `createRole(tenantId, CreateRoleRequest)` | A role with the same `code` already exists in the tenant | Throws `DuplicateResourceException` |
| `updateRole_validRequest_updatesNameAndDescription` | `updateRole(tenantId, roleId, UpdateRoleRequest)` | Role exists, is not a system role, and request is valid | `name` and `description` updated; updated `RoleDto` returned |
| `updateRole_systemRole_throwsBusinessException` | `updateRole(tenantId, roleId, UpdateRoleRequest)` | Target role has `isSystem = true` | Throws `BusinessException` with message `"Cannot modify system role"` |
| `updateRole_nonExistentId_throwsNotFoundException` | `updateRole(tenantId, roleId, UpdateRoleRequest)` | No role found for the given `roleId` | Throws `NotFoundException` |
| `deleteRole_existingRole_deletesRole` | `deleteRole(tenantId, roleId)` | Role exists, is not a system role, and is not assigned to any user | Role entity removed from repository |
| `deleteRole_systemRole_throwsBusinessException` | `deleteRole(tenantId, roleId)` | Target role has `isSystem = true` | Throws `BusinessException` with message `"Cannot delete system role"` |
| `deleteRole_roleInUse_throwsBusinessException` | `deleteRole(tenantId, roleId)` | Role is currently assigned to at least one user | Throws `BusinessException` with message `"Role assigned to users"` |
| `deleteRole_nonExistentId_throwsNotFoundException` | `deleteRole(tenantId, roleId)` | No role found for the given `roleId` | Throws `NotFoundException` |
| `assignPermissions_validCodes_replacesPermissions` | `assignPermissions(tenantId, roleId, List<String>)` | Both `"INVENTORY_READ"` and `"POS_READ"` codes exist in DB | Role's permission collection fully replaced with the two resolved permissions |
| `assignPermissions_invalidCode_throwsNotFoundException` | `assignPermissions(tenantId, roleId, List<String>)` | `"INVALID"` does not match any permission in DB | Throws `NotFoundException` for the unrecognised permission code |

---

### CustomUserDetailsService

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|------------------|
| `loadUserByUsername_existingUser_returnsUserPrincipalWithAuthorities` | `loadUserByUsername(String)` | Username `"john"` found in repository | Returns `UserPrincipal` with correct `username`, `password`, and populated `GrantedAuthority` list |
| `loadUserByUsername_nonExistentUser_throwsUsernameNotFoundException` | `loadUserByUsername(String)` | Username not found in repository | Throws Spring Security `UsernameNotFoundException` |
| `loadUserByPhone_existingPhone_returnsUserPrincipal` | `loadUserByPhone(String)` | Phone `"+998901234567"` found in repository | Returns `UserPrincipal` with correct principal data and authorities |
| `loadUserByPhone_unknownPhone_throwsUsernameNotFoundException` | `loadUserByPhone(String)` | Phone number not found in repository | Throws `UsernameNotFoundException` |

---

### PermissionCacheService

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|------------------|
| `getPermissions_cacheMiss_loadsFromDbAndCaches` | `getPermissions(userId)` | Cache does not contain an entry for `userId` | Repository queried exactly once; result stored in cache; correct permission set returned |
| `getPermissions_cacheHit_returnsCachedValueWithoutDbCall` | `getPermissions(userId)` | Cache already contains entry for `userId` | Repository never queried; cached permission set returned immediately |
| `invalidate_userId_removesEntryAndForcesDbOnNextCall` | `invalidate(userId)` | Cache contains entry for `userId`; entry is then invalidated; `getPermissions` called again | Cache entry removed; subsequent `getPermissions` call triggers a fresh DB query |

---

### Repository Tests (`@DataJpaTest` + Testcontainers)

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|------------------|
| `findByUsernameWithRolesAndPermissions_existingUser_loadsGraph` | `UserRepository.findByUsernameWithRolesAndPermissions(username)` | User with roles and permissions exists | Returns user entity with non-null, non-empty roles and permissions collections (no lazy-load exceptions) |
| `findByPhoneWithRolesAndPermissions_existingPhone_loadsGraph` | `UserRepository.findByPhoneWithRolesAndPermissions(phone)` | User with matching phone exists | Returns fully populated user entity with JOIN-FETCHed roles and permissions |
| `findByIdWithRolesAndPermissions_existingId_loadsGraph` | `UserRepository.findByIdWithRolesAndPermissions(id)` | User with given ID exists | Returns user entity with eagerly loaded roles and permissions graph |
| `findAllByTenantId_multiTenantDb_returnsOnlyTenantUsers` | `UserRepository.findAllByTenantId(tenantId, pageable)` | DB contains users from two different tenants | Returns only users whose `tenantId` matches the argument |
| `searchByTenantId_partialName_returnsMatchingUsersOnly` | `UserRepository.searchByTenantId(tenantId, "john", pageable)` | Tenant has users `"john_doe"`, `"jane_doe"`, `"john_smith"` | Returns only `"john_doe"` and `"john_smith"`; `"jane_doe"` excluded |
| `countByTenantIdAndEnabledTrue_mixedEnabledStatus_returnsCorrectCount` | `UserRepository.countByTenantIdAndEnabledTrue(tenantId)` | Tenant has 3 enabled and 2 disabled users | Returns `3` |
| `findUsersWithTelegramByTenantId_someNullChatId_returnsOnlyNonNull` | `UserRepository.findUsersWithTelegramByTenantId(tenantId)` | Tenant has users with and without `telegramChatId` | Returns only users where `telegramChatId` is not `null` |
| `findByCode_existingCode_returnsRole` | `RoleRepository.findByCode(code)` | Role with code `"ADMIN"` exists in DB | Returns non-empty `Optional<Role>` containing the matching role |
| `findByCode_nonExistentCode_returnsEmpty` | `RoleRepository.findByCode(code)` | No role with code `"NONEXISTENT"` exists | Returns `Optional.empty()` |
| `findByCodeAndTenantId_tenantScopedLookup_returnsCorrectRole` | `RoleRepository.findByCodeAndTenantId(code, tenantId)` | Same code exists in two tenants | Returns role belonging to the specified `tenantId` only |
| `findAllSystemRoles_mixedRoles_returnsOnlySystemRoles` | `RoleRepository.findAllSystemRoles()` | DB has both `isSystem = true` and `isSystem = false` roles | Returns only roles where `isSystem = true` |
| `revokeAllByUser_multipleTokens_setsAllRevoked` | `RefreshTokenRepository.revokeAllByUser(userId)` | User has 3 active refresh tokens | All 3 tokens have `revoked = true` after the call |
| `findValidTokensByUser_mixedTokens_returnsOnlyValid` | `RefreshTokenRepository.findValidTokensByUser(userId)` | User has expired, revoked, and valid tokens | Returns only tokens that are not revoked and not expired |
| `findExpiredTokens_pastExpiry_returnsExpiredOnly` | `RefreshTokenRepository.findExpiredTokens(now)` | Mix of future-expiry and past-expiry tokens in DB | Returns only tokens whose `expiresAt` is before the provided timestamp |
| `passwordResetToken_findByToken_andCheckExpiry` | `PasswordResetTokenRepository` custom queries | Token string exists with future expiry; second token is expired | `findByToken` returns correct entity; expiry predicate correctly identifies each token's status |

---

### Mapper Tests

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|------------------|
| `userMapper_toDto_mapsAllFieldsExcludesPassword` | `UserMapper.toDto(User)` | Fully populated `User` entity | All non-sensitive fields mapped to `UserDto`; `passwordHash` and `pinHash` not present in DTO |
| `userMapper_toListItem_mapsMinimalFields` | `UserMapper.toListItem(User)` | Fully populated `User` entity | Returns lightweight list-view DTO with only ID, username, full name, and role names |
| `userMapper_fromCreateRequest_createsEntityWithoutHashing` | `UserMapper.fromCreateRequest(CreateUserRequest)` | Valid `CreateUserRequest` provided | Returned `User` entity has `password` field set to the plain-text value from request (hashing is service responsibility); no BCrypt prefix present |
| `userMapper_fromUpdateRequest_mapsOnlyUpdatableFields` | `UserMapper.fromUpdateRequest(UpdateUserRequest, User)` | Partial update request with subset of fields | Only fields present in the request are applied; unrelated fields on existing entity unchanged |
| `roleMapper_toDto_includesPermissionList` | `RoleMapper.toDto(Role)` | `Role` entity with 3 associated `Permission` entities | `RoleDto` contains `permissions` list with 3 items, each with correct code and description |
| `roleMapper_fromCreateRequest_createsEntityFromRequest` | `RoleMapper.fromCreateRequest(CreateRoleRequest)` | Valid `CreateRoleRequest` with code and name | Returns `Role` entity with correct `code`, `name`, and `description`; `id` is null (not yet persisted) |

---

## Integration Tests

> All integration tests run against a real PostgreSQL instance via **Testcontainers** and use `@SpringBootTest(webEnvironment = RANDOM_PORT)` with `MockMvc` or `TestRestTemplate`.

---

### AuthController — `/api/v1/auth`

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|-----------|-------------------|-----------------|------------------------------|
| `login_validCredentials_returns200WithTokens` | `POST /api/v1/auth/login` | None (public) | `200 OK`; body contains `accessToken`, `refreshToken`, and `user` object with id, username, roles |
| `login_wrongPassword_returns401` | `POST /api/v1/auth/login` | None (public) | `401 Unauthorized`; body contains `error` message |
| `login_lockedAccount_returns403` | `POST /api/v1/auth/login` | None (public) | `403 Forbidden`; body message `"Account locked"` |
| `login_missingUsername_returns400` | `POST /api/v1/auth/login` | None (public) | `400 Bad Request`; body contains field validation error for `username` |
| `pinLogin_validPin_returns200` | `POST /api/v1/auth/pin-login` | None (public) | `200 OK`; body contains `accessToken` and `refreshToken` |
| `pinLogin_wrongPin_returns401` | `POST /api/v1/auth/pin-login` | None (public) | `401 Unauthorized` |
| `getUsersForPinScreen_authenticated_returns200List` | `GET /api/v1/auth/users/list` | Any valid JWT | `200 OK`; body is array of user summaries (id, username, fullName) |
| `getUsersForPinScreen_unauthenticated_returns401` | `GET /api/v1/auth/users/list` | None | `401 Unauthorized` |
| `setPin_authenticated_returns200` | `PUT /api/v1/auth/set-pin` | Any valid JWT | `200 OK` |
| `setPin_invalidFormat_returns400` | `PUT /api/v1/auth/set-pin` | Any valid JWT | `400 Bad Request`; body contains validation error for PIN format |
| `refresh_validToken_returns200NewTokens` | `POST /api/v1/auth/refresh` | None (public — carries refresh token in body) | `200 OK`; body contains new `accessToken` and `refreshToken` |
| `refresh_expiredToken_returns401` | `POST /api/v1/auth/refresh` | None (public) | `401 Unauthorized` |
| `logout_authenticated_returns200AndRevokesToken` | `POST /api/v1/auth/logout` | Any valid JWT | `200 OK`; subsequent use of the same refresh token returns `401` |
| `getMe_authenticated_returns200CurrentUser` | `GET /api/v1/auth/me` | Any valid JWT | `200 OK`; body matches authenticated user's `UserDto` |
| `getMe_unauthenticated_returns401` | `GET /api/v1/auth/me` | None | `401 Unauthorized` |
| `changePassword_correctCurrentPassword_returns200` | `PUT /api/v1/auth/change-password` | Any valid JWT | `200 OK` |
| `changePassword_wrongCurrentPassword_returns400` | `PUT /api/v1/auth/change-password` | Any valid JWT | `400 Bad Request`; body contains error indicating current password mismatch |
| `forgotPassword_anyEmail_alwaysReturns200` | `POST /api/v1/auth/forgot-password` | None (public) | `200 OK` regardless of whether the email exists in the system |
| `resetPassword_validToken_returns200` | `POST /api/v1/auth/reset-password` | None (public) | `200 OK`; subsequent login with new password succeeds |
| `resetPassword_expiredToken_returns400` | `POST /api/v1/auth/reset-password` | None (public) | `400 Bad Request`; body message references expired token |

---

### UserController — `/api/v1/users`

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|-----------|-------------------|-----------------|------------------------------|
| `getUsers_adminAuthenticated_returns200Paginated` | `GET /api/v1/users` | JWT with `USER_READ` permission | `200 OK`; body is paginated response with `content`, `totalElements`, `totalPages` |
| `getUsers_noPermission_returns403` | `GET /api/v1/users` | JWT without `USER_READ` | `403 Forbidden` |
| `getUsers_withSearchFilter_returnsFilteredList` | `GET /api/v1/users?search=john` | JWT with `USER_READ` | `200 OK`; all returned users match `"john"` in name or username |
| `getUser_foundById_returns200` | `GET /api/v1/users/{id}` | JWT with `USER_READ` | `200 OK`; body is `UserDto` with correct id |
| `getUser_notFound_returns404` | `GET /api/v1/users/{id}` | JWT with `USER_READ` | `404 Not Found` |
| `createUser_validRequest_returns201` | `POST /api/v1/users` | JWT with `USER_CREATE` | `201 Created`; body is the created `UserDto`; `Location` header set |
| `createUser_duplicateUsername_returns409` | `POST /api/v1/users` | JWT with `USER_CREATE` | `409 Conflict`; body contains duplicate resource error |
| `createUser_invalidRequest_returns400` | `POST /api/v1/users` | JWT with `USER_CREATE` | `400 Bad Request`; body contains field-level validation errors |
| `updateUser_validRequest_returns200` | `PUT /api/v1/users/{id}` | JWT with `USER_UPDATE` | `200 OK`; body reflects updated fields |
| `updateUser_notFound_returns404` | `PUT /api/v1/users/{id}` | JWT with `USER_UPDATE` | `404 Not Found` |
| `deleteUser_validId_returns204` | `DELETE /api/v1/users/{id}` | JWT with `USER_DELETE` | `204 No Content` |
| `deleteUser_notFound_returns404` | `DELETE /api/v1/users/{id}` | JWT with `USER_DELETE` | `404 Not Found` |
| `deleteUser_selfDelete_returns422` | `DELETE /api/v1/users/{id}` | JWT — `{id}` is the caller's own ID | `422 Unprocessable Entity`; body message `"Cannot delete yourself"` |
| `assignRoles_validRequest_returns200` | `PUT /api/v1/users/{id}/roles` | JWT with `USER_UPDATE` | `200 OK`; body reflects new role assignments |
| `lockUser_lockSuccess_returns200` | `PUT /api/v1/users/{id}/lock` | JWT with `USER_UPDATE` | `200 OK`; subsequent login attempt returns `403` |
| `lockUser_notFound_returns404` | `PUT /api/v1/users/{id}/lock` | JWT with `USER_UPDATE` | `404 Not Found` |
| `resetPasswordAdmin_success_returns200` | `PUT /api/v1/users/{id}/reset-password` | JWT with `USER_UPDATE` | `200 OK`; user can log in with the new password |
| `setUserPin_validPin_returns200` | `PUT /api/v1/users/{id}/set-pin` | JWT with `USER_UPDATE` | `200 OK` |
| `setUserPin_invalidFormat_returns400` | `PUT /api/v1/users/{id}/set-pin` | JWT with `USER_UPDATE` | `400 Bad Request`; body contains PIN format validation error |

---

### RoleController — `/api/v1/roles`

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|-----------|-------------------|-----------------|------------------------------|
| `getRoles_authenticated_returns200Paginated` | `GET /api/v1/roles` | JWT with `ROLE_READ` | `200 OK`; paginated body including both tenant roles and system roles |
| `getSystemRoles_returns200SystemRolesOnly` | `GET /api/v1/roles/system` | JWT with `ROLE_READ` | `200 OK`; all returned roles have `isSystem = true` |
| `getRole_foundById_returns200WithPermissions` | `GET /api/v1/roles/{id}` | JWT with `ROLE_READ` | `200 OK`; `RoleDto` body includes non-empty `permissions` array |
| `getRole_notFound_returns404` | `GET /api/v1/roles/{id}` | JWT with `ROLE_READ` | `404 Not Found` |
| `getAllPermissions_returns200PermissionCodes` | `GET /api/v1/roles/permissions` | JWT with `ROLE_READ` | `200 OK`; body is array of all permission code strings defined in the system |
| `createRole_validRequest_returns201` | `POST /api/v1/roles` | JWT with `ROLE_CREATE` | `201 Created`; body is the created `RoleDto` with the assigned `id` |
| `createRole_duplicateCode_returns409` | `POST /api/v1/roles` | JWT with `ROLE_CREATE` | `409 Conflict`; body references the duplicate `code` |
| `updateRole_validRequest_returns200` | `PUT /api/v1/roles/{id}` | JWT with `ROLE_UPDATE` | `200 OK`; body contains updated `name` and `description` |
| `updateRole_notFound_returns404` | `PUT /api/v1/roles/{id}` | JWT with `ROLE_UPDATE` | `404 Not Found` |
| `updateRole_systemRole_returns422` | `PUT /api/v1/roles/{id}` | JWT with `ROLE_UPDATE` — `{id}` is a system role | `422 Unprocessable Entity`; body message `"Cannot modify system role"` |
| `deleteRole_validRole_returns204` | `DELETE /api/v1/roles/{id}` | JWT with `ROLE_DELETE` | `204 No Content` |
| `deleteRole_notFound_returns404` | `DELETE /api/v1/roles/{id}` | JWT with `ROLE_DELETE` | `404 Not Found` |
| `deleteRole_roleInUse_returns422` | `DELETE /api/v1/roles/{id}` | JWT with `ROLE_DELETE` — role assigned to a user | `422 Unprocessable Entity`; body message `"Role assigned to users"` |
| `deleteRole_systemRole_returns422` | `DELETE /api/v1/roles/{id}` | JWT with `ROLE_DELETE` — `{id}` is a system role | `422 Unprocessable Entity`; body message `"Cannot delete system role"` |
| `assignPermissions_validCodes_returns200` | `PUT /api/v1/roles/{id}/permissions` | JWT with `ROLE_UPDATE` | `200 OK`; body's `permissions` array exactly matches the submitted codes |

---

## Coverage Summary

| Layer | Classes Targeted | Min Test Cases |
|-------|-----------------|----------------|
| Service — AuthService | `AuthService` | 23 |
| Service — UserService | `UserService` | 22 |
| Service — RoleService | `RoleService` | 14 |
| Service — CustomUserDetailsService | `CustomUserDetailsService` | 4 |
| Service — PermissionCacheService | `PermissionCacheService` | 3 |
| Repository (`@DataJpaTest`) | `UserRepository`, `RoleRepository`, `RefreshTokenRepository`, `PasswordResetTokenRepository` | 14 |
| Mapper | `UserMapper`, `RoleMapper` | 6 |
| Integration — AuthController | `/api/v1/auth/**` | 20 |
| Integration — UserController | `/api/v1/users/**` | 19 |
| Integration — RoleController | `/api/v1/roles/**` | 15 |
| **Total** | | **140** |

---

# Section 1c: Security, Configuration & Exception Handling — Test Plan

**Platform:** Hisobnoma (Java Spring Boot SaaS)
**Coverage Goal:** 100% unit AND integration test coverage for the Security, Configuration, and Exception Handling layers
**Last Updated:** 2026-04-17

---

## Table of Contents

1. [JwtTokenProvider — Unit Tests](#1-jwttokenprovider--unit-tests)
2. [JwtAuthenticationFilter — Unit Tests](#2-jwtauthenticationfilter--unit-tests)
3. [SecurityContextHelper — Unit Tests](#3-securitycontexthelper--unit-tests)
4. [TenantContext — Unit Tests](#4-tenantcontext--unit-tests)
5. [TenantFilter — Unit Tests](#5-tenantfilter--unit-tests)
6. [TenantAspect — Unit Tests](#6-tenantaspect--unit-tests)
7. [PermissionAspect — Unit Tests](#7-permissionaspect--unit-tests)
8. [UserPrincipal — Unit Tests](#8-userprincipal--unit-tests)
9. [GlobalExceptionHandler — Integration Tests](#9-globalexceptionhandler--integration-tests)
10. [SecurityConfig — Integration Tests](#10-securityconfig--integration-tests)
11. [Application Startup — Integration Tests](#11-application-startup--integration-tests)

---

## 1. JwtTokenProvider — Unit Tests

**Test Class:** `JwtTokenProviderTest`
**Framework:** JUnit 5 + Mockito
**Notes:** Use a fixed HMAC-SHA256 secret (256-bit minimum) injected via `@Value` in tests. Use `Jwts.parser()` directly in assertions to independently verify claims without relying on the class under test.

| Test Name | Method / Component | Scenario | Expected Outcome |
|---|---|---|---|
| `generateToken_returnsNonNullJwtString` | `generateToken(username, tenantId, expirationTime)` | Valid username `"alice"`, tenantId `42L`, expirationTime `3600000L` (1 hour) | Returns a non-null, non-blank String in compact JWT format (three dot-separated Base64URL segments) |
| `generateToken_containsSubjectClaim` | `generateToken(username, tenantId, expirationTime)` | Same valid inputs as above | Parsing the returned token with the known secret yields a `sub` claim exactly equal to `"alice"` |
| `generateToken_containsTenantIdClaim` | `generateToken(username, tenantId, expirationTime)` | tenantId = `42L` | Parsing the returned token yields a custom claim `"tenantId"` with numeric value `42` |
| `generateToken_expiryEqualsNowPlusExpirationTime` | `generateToken(username, tenantId, expirationTime)` | expirationTime = `3600000L`; clock frozen via `Clock` injection or test spy | Token `exp` claim equals `issuedAt + 3600000ms` (within a 1-second tolerance to account for execution time) |
| `extractUsername_validToken_returnsCorrectUsername` | `extractUsername(token)` | Token generated for `"bob"` | Returns `"bob"` |
| `extractUsername_malformedToken_throwsJwtException` | `extractUsername(token)` | Token is `"not.a.jwt"` | Throws `io.jsonwebtoken.JwtException` (or a subtype) |
| `extractUsername_expiredToken_throwsExpiredJwtException` | `extractUsername(token)` | Token generated with `expirationTime = -1000L` (already expired at issuance) | Throws `io.jsonwebtoken.ExpiredJwtException` |
| `extractTenantId_validToken_returnsCorrectLong` | `extractTenantId(token)` | Token contains `tenantId = 99L` | Returns `Long` value `99L` |
| `extractTenantId_tokenWithoutClaim_throwsOrReturnsNull` | `extractTenantId(token)` | Token generated without a `tenantId` claim | Throws `MissingClaimException` OR returns `null` (documented contract must be one or the other; test enforces whichever is defined) |
| `validateToken_validToken_returnsTrue` | `validateToken(token)` | Token is well-formed, not expired, signature matches secret | Returns `true` |
| `validateToken_expiredToken_returnsFalse` | `validateToken(token)` | Token `exp` is in the past | Returns `false` (exception caught internally; does NOT propagate) |
| `validateToken_tamperedSignatureToken_returnsFalse` | `validateToken(token)` | Valid token with last character of signature segment changed | Returns `false` |
| `validateToken_malformedToken_returnsFalse` | `validateToken(token)` | Token string is `"garbage.garbage"` | Returns `false` |
| `validateToken_nullToken_returnsFalse` | `validateToken(null)` | `null` passed as token | Returns `false` without throwing `NullPointerException` |
| `getExpirationDate_validToken_returnsCorrectDate` | `getExpirationDate(token)` | Token generated with known `expirationTime` | Returns a `Date` whose epoch millisecond value matches `issuedAt + expirationTime` (within 1-second tolerance) |
| `getExpirationDate_malformedToken_throwsJwtException` | `getExpirationDate(token)` | Token is `"bad.token.string"` | Throws `io.jsonwebtoken.JwtException` |

---

## 2. JwtAuthenticationFilter — Unit Tests

**Test Class:** `JwtAuthenticationFilterTest`
**Framework:** JUnit 5 + Mockito
**Notes:** Mock `JwtTokenProvider`, `UserDetailsService`, and `SecurityContextHolder`. Pass a `MockHttpServletRequest` / `MockHttpServletResponse` and a mock `FilterChain`. Assert on `SecurityContextHolder.getContext().getAuthentication()` after each invocation.

| Test Name | Method / Component | Scenario | Expected Outcome |
|---|---|---|---|
| `doFilterInternal_validBearerToken_populatesSecurityContext` | `doFilterInternal(request, response, filterChain)` | `Authorization: Bearer <validToken>` header present; `JwtTokenProvider.validateToken` returns `true`; `UserDetailsService.loadUserByUsername` returns a populated `UserPrincipal` | `SecurityContextHolder` authentication is non-null; authentication principal equals the loaded `UserPrincipal`; `filterChain.doFilter` is called exactly once |
| `doFilterInternal_missingAuthorizationHeader_contextEmpty` | `doFilterInternal(request, response, filterChain)` | No `Authorization` header on the request | `SecurityContextHolder` authentication remains `null`; `filterChain.doFilter` is called exactly once; no exception thrown |
| `doFilterInternal_invalidToken_contextNotPopulated` | `doFilterInternal(request, response, filterChain)` | `Authorization: Bearer <invalidToken>` header present; `JwtTokenProvider.validateToken` returns `false` | `SecurityContextHolder` authentication remains `null`; `filterChain.doFilter` is called exactly once |
| `doFilterInternal_expiredToken_returns401` | `doFilterInternal(request, response, filterChain)` | Token is expired; `JwtTokenProvider.validateToken` returns `false` (or `validateToken` throws and is caught) | `SecurityContextHolder` authentication is NOT populated; HTTP response status is set to `401` OR `filterChain.doFilter` is not called (depending on implementation contract); no unhandled exception propagates |
| `doFilterInternal_tokenForNonExistentUser_contextNotPopulated` | `doFilterInternal(request, response, filterChain)` | Token is valid and signed correctly; `UserDetailsService.loadUserByUsername` throws `UsernameNotFoundException` | `SecurityContextHolder` authentication remains `null`; filter does not propagate the exception (catches it internally); `filterChain.doFilter` behavior matches implementation contract (called or 401 set) |

---

## 3. SecurityContextHelper — Unit Tests

**Test Class:** `SecurityContextHelperTest`
**Framework:** JUnit 5 + Mockito + Spring Security `SecurityContextHolder` setup/teardown in `@BeforeEach` / `@AfterEach`
**Notes:** Manually populate `SecurityContextHolder` with a `UsernamePasswordAuthenticationToken` wrapping a `UserPrincipal` stub before each test that requires an authenticated context. Clear it in `@AfterEach`.

| Test Name | Method / Component | Scenario | Expected Outcome |
|---|---|---|---|
| `getCurrentUserId_authenticatedContext_returnsLong` | `getCurrentUserId()` | `SecurityContextHolder` contains a principal with `id = 7L` | Returns `Long` value `7L` |
| `getCurrentUserId_unauthenticatedContext_returnsNullOrThrows` | `getCurrentUserId()` | `SecurityContextHolder` has no authentication (anonymous) | Returns `null` OR throws a documented exception (e.g., `UnauthorizedException`); test enforces whichever contract is defined |
| `getCurrentUsername_authenticatedContext_returnsString` | `getCurrentUsername()` | Principal username is `"carol"` | Returns `"carol"` |
| `getCurrentTenantId_authenticatedContext_returnsLong` | `getCurrentTenantId()` | Principal has `tenantId = 5L` | Returns `Long` value `5L` |
| `getCurrentUser_authenticatedContext_returnsFullPrincipal` | `getCurrentUser()` | Security context contains a fully populated `UserPrincipal` | Returns the exact same `UserPrincipal` object (or an equal one); all fields (id, username, tenantId, permissions) match |
| `isCurrentUser_matchingId_returnsTrue` | `isCurrentUser(id)` | Authenticated user has `id = 3L`; argument is `3L` | Returns `true` |
| `isCurrentUser_differentId_returnsFalse` | `isCurrentUser(id)` | Authenticated user has `id = 3L`; argument is `99L` | Returns `false` |
| `isCurrentUser_nullId_returnsFalse` | `isCurrentUser(null)` | `null` passed as argument regardless of authenticated user | Returns `false` without throwing `NullPointerException` |

---

## 4. TenantContext — Unit Tests

**Test Class:** `TenantContextTest`
**Framework:** JUnit 5
**Notes:** `TenantContext` wraps a `ThreadLocal<Long>`. All tests run in single-threaded mode unless explicitly testing isolation. Call `TenantContext.clear()` in `@AfterEach` to prevent bleed between tests.

| Test Name | Method / Component | Scenario | Expected Outcome |
|---|---|---|---|
| `setAndGet_returnsSameValue` | `setCurrentTenantId(id)` then `getCurrentTenantId()` | `setCurrentTenantId(1L)` called, then `getCurrentTenantId()` called in the same thread | Returns `1L` |
| `clear_thenGetReturnsNull` | `clear()` then `getCurrentTenantId()` | `setCurrentTenantId(1L)` called, then `clear()` called, then `getCurrentTenantId()` called | Returns `null` |
| `threadIsolation_eachThreadSeesOwnValue` | `setCurrentTenantId` in two concurrent threads | Thread A sets `tenantId = 1L`; Thread B sets `tenantId = 2L`; both read back their own value after a `CountDownLatch` sync point | Thread A reads `1L`; Thread B reads `2L`; no cross-contamination |
| `clearInFinallyBlock_noLeakBetweenRequests` | `clear()` called in `finally` | Simulate two sequential pseudo-requests: first sets `tenantId = 10L` and calls `clear()` in finally; second request reads `getCurrentTenantId()` before setting anything | Second request sees `null`, not `10L` |

---

## 5. TenantFilter — Unit Tests

**Test Class:** `TenantFilterTest`
**Framework:** JUnit 5 + Mockito
**Notes:** Mock `JwtTokenProvider` and `TenantContext` (or use `MockedStatic` for `TenantContext` if it is a static utility). Use `MockHttpServletRequest` / `MockHttpServletResponse` and a mock `FilterChain`.

| Test Name | Method / Component | Scenario | Expected Outcome |
|---|---|---|---|
| `extractAndSet_jwtContainsTenantId_setsContext` | `doFilterInternal` / `extractAndSetTenantId` | Valid `Bearer` token in request; `JwtTokenProvider.extractTenantId` returns `7L` | `TenantContext.setCurrentTenantId(7L)` is called; `filterChain.doFilter` is called |
| `extractAndSet_noJwt_tenantContextNotSet` | `doFilterInternal` / `extractAndSetTenantId` | No `Authorization` header present | `TenantContext.setCurrentTenantId` is NOT called (or is called with `null` per implementation contract); `filterChain.doFilter` is still called |
| `afterFilterCompletes_contextCleared_noLeak` | `doFilterInternal` post-chain | Filter completes normally (or chain throws exception caught by filter) | `TenantContext.clear()` is called exactly once in a `finally` block, regardless of whether the chain succeeds or throws |

---

## 6. TenantAspect — Unit Tests

**Test Class:** `TenantAspectTest`
**Framework:** JUnit 5 + Spring Test (AspectJ weaving via `@EnableAspectJAutoProxy`) + Mockito
**Notes:** Create a minimal Spring context containing only the aspect and a stub `@Repository` bean. Set and clear `TenantContext` manually in `@BeforeEach` / `@AfterEach`.

| Test Name | Method / Component | Scenario | Expected Outcome |
|---|---|---|---|
| `repositoryCall_tenantContextSet_queryFilteredByTenantId` | Aspect intercept on `@Repository` method | `TenantContext.getCurrentTenantId()` returns `5L` before the repository method is invoked | The advised repository method receives or applies `tenantId = 5L` as a filter parameter (verified via argument captor or query inspection) |
| `repositoryCall_noTenantContext_throwsIllegalStateException` | Aspect intercept on `@Repository` method | `TenantContext.getCurrentTenantId()` returns `null` (not set) | Throws `IllegalStateException` (or uses a defined system/admin context — test enforces whichever contract is documented) |
| `aspectIntercepts_allRepositoriesInPackage` | Aspect pointcut | Three stub `@Repository` classes in `com.hisobnoma.platform` package all have their methods called with `TenantContext` set | Aspect advice fires for all three; no `@Repository` in the target package bypasses the aspect |

---

## 7. PermissionAspect — Unit Tests

**Test Class:** `PermissionAspectTest`
**Framework:** JUnit 5 + Spring Test (AspectJ) + Mockito
**Notes:** Create a minimal Spring context with `PermissionAspect`, `SecurityContextHelper` (mocked), and a stub service bean whose methods are annotated with `@RequiresPermission`. Populate or clear `SecurityContextHolder` in each test.

| Test Name | Method / Component | Scenario | Expected Outcome |
|---|---|---|---|
| `requiredPermission_userHasPermission_methodExecutes` | Aspect intercept on `@RequiresPermission("INVENTORY_READ")` | Authenticated principal has `"INVENTORY_READ"` in their `GrantedAuthority` collection | Advised method executes normally and returns its expected result; no exception thrown |
| `requiredPermission_userLacksPermission_throwsForbiddenException` | Aspect intercept on `@RequiresPermission("INVENTORY_READ")` | Authenticated principal does NOT have `"INVENTORY_READ"` | Throws `ForbiddenException` (HTTP 403 semantic); advised method body is never executed |
| `requiredPermission_unauthenticated_throwsUnauthorizedException` | Aspect intercept on `@RequiresPermission("ADMIN")` | `SecurityContextHolder` has no authentication (anonymous / null principal) | Throws `UnauthorizedException` (HTTP 401 semantic); advised method body is never executed |
| `requiredPermissions_anyMatch_userHasOne_proceeds` | Aspect intercept on `@RequiresPermission({"A", "B"})` with any-match semantics | Authenticated user has permission `"A"` but not `"B"` | Advised method executes normally; no exception thrown |
| `requiredPermissions_anyMatch_userHasNeither_throwsForbiddenException` | Aspect intercept on `@RequiresPermission({"A", "B"})` with any-match semantics | Authenticated user has neither `"A"` nor `"B"` | Throws `ForbiddenException`; advised method body is never executed |

---

## 8. UserPrincipal — Unit Tests

**Test Class:** `UserPrincipalTest`
**Framework:** JUnit 5
**Notes:** Construct `UserPrincipal` instances directly using the production constructor or builder. No Spring context required.

| Test Name | Method / Component | Scenario | Expected Outcome |
|---|---|---|---|
| `getAuthorities_returnsGrantedAuthoritiesForPermissions` | `getAuthorities()` | `UserPrincipal` constructed with permissions `["INVOICE_READ", "INVOICE_WRITE"]` | Returns a `Collection<GrantedAuthority>` of size 2; `.getAuthority()` values are `"INVOICE_READ"` and `"INVOICE_WRITE"` |
| `isAccountNonExpired_activeUser_returnsTrue` | `isAccountNonExpired()` | User status is active (no expiry flag set) | Returns `true` |
| `isAccountNonLocked_enabledUser_returnsTrue` | `isAccountNonLocked()` | `user.enabled = true`, no lock flag set | Returns `true` |
| `isAccountNonLocked_lockedUser_returnsFalse` | `isAccountNonLocked()` | User has a locked flag set (or `enabled = false` maps to locked per implementation) | Returns `false` |
| `isEnabled_enabledUser_returnsTrue` | `isEnabled()` | `user.enabled = true` | Returns `true` |
| `isEnabled_disabledUser_returnsFalse` | `isEnabled()` | `user.enabled = false` | Returns `false` |
| `getUsername_returnsUsernameString` | `getUsername()` | `UserPrincipal` constructed with username `"dave"` | Returns `"dave"` |
| `getTenantId_returnsLong` | `getTenantId()` | `UserPrincipal` constructed with `tenantId = 12L` | Returns `12L` |

---

## 9. GlobalExceptionHandler — Integration Tests

**Test Class:** `GlobalExceptionHandlerIntegrationTest`
**Framework:** JUnit 5 + `@WebMvcTest(TestExceptionTriggerController.class)` + `MockMvc`
**Notes:** Create a minimal `@RestController` (inner class or test-scoped class) whose endpoints each throw one of the mapped exceptions. The `@WebMvcTest` slice loads `GlobalExceptionHandler` via `@ControllerAdvice` auto-detection. No full application context or database required. Verify response body structure with `jsonPath`.

| Test Name | Scenario | Expected HTTP Status + Behavior |
|---|---|---|
| `handleNotFoundException_returns404WithMessage` | Test controller endpoint throws `NotFoundException("User not found")` | HTTP `404`; response body contains `{"error": "User not found"}` (or equivalent field name per API contract); `Content-Type: application/json` |
| `handleValidationException_returns400WithFieldErrors` | Test controller endpoint throws `ValidationException` populated with field-level errors (e.g., `field: "email", message: "must not be blank"`) | HTTP `400`; response body is a JSON object containing a list of field error objects; each entry includes the field name and violation message |
| `handleBusinessException_returns422WithMessage` | Test controller endpoint throws `BusinessException("Insufficient funds")` | HTTP `422`; response body contains the business exception message; no stack trace fields present in the JSON |
| `handleDuplicateResourceException_returns409WithMessage` | Test controller endpoint throws `DuplicateResourceException("Username already exists")` | HTTP `409`; response body contains the duplication message |
| `handleForbiddenException_returns403` | Test controller endpoint throws `ForbiddenException` | HTTP `403`; response body contains an appropriate error message |
| `handleUnauthorizedException_returns401` | Test controller endpoint throws `UnauthorizedException` | HTTP `401`; response body contains an appropriate error message |
| `handleGeneralException_returns500WithGenericMessage` | Test controller endpoint throws an uncaught `RuntimeException("Internal detail")` | HTTP `500`; response body contains a generic, user-safe message (e.g., `"An unexpected error occurred"`); the raw exception message `"Internal detail"` and stack trace are NOT present in the response body |
| `handleConstraintViolationException_returns400WithViolationDetails` | Request body fails `@Valid` Bean Validation (e.g., missing required field); Spring raises `MethodArgumentNotValidException` or `ConstraintViolationException` | HTTP `400`; response body lists each constraint violation with field name and message; `Content-Type: application/json` |

---

## 10. SecurityConfig — Integration Tests

**Test Class:** `SecurityConfigIntegrationTest`
**Framework:** JUnit 5 + `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate` (or `@AutoConfigureMockMvc` + `MockMvc`)
**Notes:** Use Testcontainers PostgreSQL for the data source. A minimal valid JWT is generated in `@BeforeAll` using the same secret configured for the test profile. Stub or use a real `UserDetailsService` backed by an in-memory or Testcontainers-backed user. CSRF is expected to be disabled (stateless JWT API).

| Test Name | Scenario | Expected HTTP Status + Behavior |
|---|---|---|
| `publicEndpoint_login_accessibleWithoutToken` | `POST /api/v1/auth/login` with valid credentials JSON body; no `Authorization` header | HTTP `200` (or `400` for bad credentials, but NOT `401`); endpoint is reachable without a token |
| `publicEndpoint_refresh_accessibleWithoutToken` | `POST /api/v1/auth/refresh` with a refresh token body; no `Authorization` header | HTTP `200` or `400`; NOT `401`; security filter does not block the request before the controller handles it |
| `protectedEndpoint_noToken_returns401` | `GET /api/v1/users` with no `Authorization` header | HTTP `401`; response body or `WWW-Authenticate` header indicates authentication is required |
| `protectedEndpoint_validJwt_reachesController` | `GET /api/v1/users` with `Authorization: Bearer <validToken>` | HTTP `200` (or the controller's actual response code); the request passes through `JwtAuthenticationFilter` and reaches the controller; authentication is populated in `SecurityContext` |
| `cors_allowedOrigin_respondsWithCorsHeader` | `GET /api/v1/users` (or any endpoint) with `Origin: https://app.hisobnoma.com` header (configured as an allowed origin) | Response includes `Access-Control-Allow-Origin: https://app.hisobnoma.com` header |
| `cors_preflight_returns200WithCorsHeaders` | `OPTIONS /api/v1/users` with `Origin: https://app.hisobnoma.com` and `Access-Control-Request-Method: GET` | HTTP `200`; response includes `Access-Control-Allow-Origin`, `Access-Control-Allow-Methods`, and `Access-Control-Allow-Headers` headers |
| `csrf_disabled_postWithoutCsrfToken_notRejected` | `POST /api/v1/auth/login` with no CSRF token (neither header nor cookie) | Request is NOT rejected with `403 Forbidden` due to CSRF protection; response reflects the actual handler outcome (e.g., `200` or `400`), confirming CSRF is disabled for this stateless API |

---

## 11. Application Startup — Integration Tests

**Test Class:** `ApplicationStartupIntegrationTest`
**Framework:** JUnit 5 + `@SpringBootTest` + Testcontainers (`@Testcontainers`, `@Container`)
**Notes:** A single Testcontainers PostgreSQL instance (annotated `static`) is shared across all tests in this class for startup efficiency. Spring properties for datasource URL, username, and password are overridden via `@DynamicPropertySource`. Flyway is expected to run automatically on context startup.

| Test Name | Scenario | Expected HTTP Status + Behavior |
|---|---|---|
| `applicationContext_loadsWithoutErrors` | `@SpringBootTest` starts the full application context (all beans: security, data, web, aspect) | `ApplicationContext` is not null; no `BeanCreationException`, `UnsatisfiedDependencyException`, or other Spring startup exception is thrown; the test completes without error |
| `dataSource_connectsToTestcontainersPostgres` | Full context started with Testcontainers PostgreSQL; `DataSource.getConnection()` called in the test | `Connection` is returned without `SQLException`; `connection.isValid(2)` returns `true`; the connection metadata reflects the PostgreSQL JDBC URL provided by the container |
| `flywayMigrations_appliedSuccessfully` | Full context started; Flyway runs automatically against the Testcontainers PostgreSQL instance | All migration scripts in `db/migration` are applied in order without error; `flyway_schema_history` table exists and all rows have `success = true`; no `FlywayException` is thrown during context startup |

---

## Appendix: Test Infrastructure Conventions

### Naming Convention
All test classes follow the pattern `<ClassUnderTest>Test` for unit tests and `<Feature>IntegrationTest` for integration tests.

### Shared Utilities
- `JwtTestUtils` — generates test tokens with configurable claims and expiry for use across multiple test classes.
- `SecurityTestContextFactory` — sets up and tears down `SecurityContextHolder` state for unit tests requiring an authenticated principal.
- `TestcontainersConfig` — a reusable `@Configuration` class annotating a shared static `PostgreSQLContainer` with `@DynamicPropertySource` registration, imported by all integration test classes that require a database.

### Coverage Enforcement
Jacoco minimum thresholds for the `com.hisobnoma.platform.security`, `com.hisobnoma.platform.config`, and `com.hisobnoma.platform.exception` packages are set to **100% instruction coverage** and **100% branch coverage** in `pom.xml` / `build.gradle`. The CI pipeline fails if any threshold is not met.

### Test Profiles
Integration tests run under the `test` Spring profile (`application-test.yml`), which sets:
- A fixed, short JWT secret for deterministic token generation.
- Flyway `clean-on-validation-error: true` (test DB only).
- Logging level `DEBUG` for `com.hisobnoma` to assist in diagnosing failures.

---

# Section 2a-i: Finance — Accounts Payable — Test Plan

---

## 1. APInvoiceService Unit Tests

Framework: JUnit 5 + Mockito.

### 1.1 `getAPInvoices(tenantId, pageable)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getAPInvoices_returnsPaginatedResults` | `getAPInvoices(tenantId, pageable)` | Repository returns a populated page | Returns `Page<APInvoiceDto>` with correct count and mapped fields |
| `getAPInvoices_returnsEmptyPage_whenNoneExist` | `getAPInvoices(tenantId, pageable)` | Repository returns empty page | Returns empty `Page<APInvoiceDto>` |
| `getAPInvoices_respectsTenantIsolation` | `getAPInvoices(tenantId, pageable)` | Two tenants have invoices | Only tenant-A invoices returned |

### 1.2 `getAPInvoice(tenantId, invoiceId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getAPInvoice_found_returnsDto` | `getAPInvoice(tenantId, id)` | Invoice exists for tenant | Returns `APInvoiceDto` with all fields |
| `getAPInvoice_notFound_throwsNotFoundException` | `getAPInvoice(tenantId, id)` | No invoice with given id | Throws `NotFoundException` referencing invoiceId |
| `getAPInvoice_wrongTenant_throwsNotFoundException` | `getAPInvoice(tenantId, id)` | Invoice belongs to different tenant | Throws `NotFoundException` |

### 1.3 `createAPInvoice(tenantId, request)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `createAPInvoice_success_returnsDraftDto` | `createAPInvoice(tenantId, request)` | Valid request with existing vendor | Returns `APInvoiceDto` with `status=DRAFT` |
| `createAPInvoice_vendorNotFound_throwsNotFoundException` | `createAPInvoice(tenantId, request)` | Vendor id does not exist | Throws `NotFoundException` referencing vendorId |
| `createAPInvoice_emptyLines_throwsValidationException` | `createAPInvoice(tenantId, request)` | Request has no line items | Throws `ValidationException` |
| `createAPInvoice_duplicateInvoiceNumber_throwsDuplicateResourceException` | `createAPInvoice(tenantId, request)` | Invoice number already exists for vendor | Throws `DuplicateResourceException` |

### 1.4 `approveAPInvoice(tenantId, invoiceId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `approveAPInvoice_draft_statusBecomesApproved` | `approveAPInvoice(tenantId, id)` | Invoice in `DRAFT` status | Returns dto with `status=APPROVED` |
| `approveAPInvoice_alreadyApproved_throwsBusinessException` | `approveAPInvoice(tenantId, id)` | Invoice already `APPROVED` | Throws `BusinessException` |
| `approveAPInvoice_cancelled_throwsBusinessException` | `approveAPInvoice(tenantId, id)` | Invoice `CANCELLED` | Throws `BusinessException` |

### 1.5 `postAPInvoice(tenantId, invoiceId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `postAPInvoice_approved_createsJournalEntry` | `postAPInvoice(tenantId, id)` | Invoice `APPROVED`; GL configured | Status becomes `POSTED`; `JournalEntryService.create` called once |
| `postAPInvoice_draft_throwsBusinessException` | `postAPInvoice(tenantId, id)` | Invoice still `DRAFT` | Throws `BusinessException` |

### 1.6 `cancelAPInvoice(tenantId, invoiceId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `cancelAPInvoice_draft_cancelled` | `cancelAPInvoice(tenantId, id)` | Invoice `DRAFT` | Status becomes `CANCELLED` |
| `cancelAPInvoice_partiallyPaid_throwsBusinessException` | `cancelAPInvoice(tenantId, id)` | Invoice has payments | Throws `BusinessException` "Cannot cancel partially paid invoice" |

### 1.7 `getOverdueAPInvoices(tenantId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getOverdueAPInvoices_returnsPastDueUnpaid` | `getOverdueAPInvoices(tenantId)` | 2 invoices past due date, 1 future | Returns list of 2 |
| `getOverdueAPInvoices_returnsEmpty_whenNone` | `getOverdueAPInvoices(tenantId)` | All invoices paid or not yet due | Returns empty list |

---

## 2. APPaymentService Unit Tests

### 2.1 `createAPPayment(tenantId, request)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `createAPPayment_success_reducesAmountDue` | `createAPPayment(tenantId, request)` | Valid payment against APPROVED invoice | Payment saved; invoice `amountDue` reduced |
| `createAPPayment_invoiceNotFound_throwsNotFoundException` | `createAPPayment(tenantId, request)` | Invoice does not exist | Throws `NotFoundException` |
| `createAPPayment_overpayment_throwsBusinessException` | `createAPPayment(tenantId, request)` | Amount exceeds `amountDue` | Throws `BusinessException` "Payment exceeds invoice balance" |
| `createAPPayment_zeroPmt_throwsValidationException` | `createAPPayment(tenantId, request)` | `amount=0` | Throws `ValidationException` |
| `createAPPayment_fullyPaidInvoice_statusBecomesFullyPaid` | `createAPPayment(tenantId, request)` | Payment equals remaining balance | Invoice status changes to `FULLY_PAID` |

### 2.2 `getAPPayments(tenantId, pageable)` / `getAPPaymentsByInvoice(tenantId, invoiceId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getAPPayments_returnsPaged` | `getAPPayments(tenantId, pageable)` | Multiple payments exist | Returns correct paged result |
| `getAPPaymentsByInvoice_returnsOnly_invoicePayments` | `getAPPaymentsByInvoice(tenantId, invoiceId)` | Payments for two invoices | Returns only payments for specified invoice |

### 2.3 `voidAPPayment(tenantId, paymentId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `voidAPPayment_success_reversesAmountDue` | `voidAPPayment(tenantId, id)` | Valid payment in `COMPLETED` state | Payment voided; invoice `amountDue` restored |
| `voidAPPayment_alreadyVoided_throwsBusinessException` | `voidAPPayment(tenantId, id)` | Payment already voided | Throws `BusinessException` |

---

## 3. Repository Tests (`@DataJpaTest` + Testcontainers)

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `APInvoiceRepository_findByVendorId_returnsList` | `findByVendorId(vendorId)` | 3 invoices for vendor A, 2 for B | Returns 3 for vendor A |
| `APInvoiceRepository_findByStatus_returnsMatchingOnly` | `findByStatus(DRAFT)` | Mix of statuses | Returns only DRAFT invoices |
| `APInvoiceRepository_findOverdueInvoices_returnsPastDue` | `findOverdueInvoices(today)` | 2 past due, 1 future | Returns 2 |
| `APInvoiceRepository_sumByVendorAndDateRange` | `sumTotalByVendorAndDateRange(...)` | 3 invoices in range, 1 outside | Returns sum of 3 |
| `APPaymentRepository_findByInvoiceId` | `findByInvoiceId(invoiceId)` | 2 payments for invoice A, 1 for B | Returns 2 |
| `APPaymentRepository_findByVendorId` | `findByVendorId(vendorId)` | Multiple payments | Returns all for vendor |
| `APInvoiceLineRepository_findByInvoiceId` | `findByInvoiceId(invoiceId)` | 4 lines on invoice | Returns 4 |

---

## 4. Mapper Tests

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `APInvoiceMapper_toDto_mapsAllFields` | `APInvoiceMapper.toDto(entity)` | Populated entity with vendor, lines | DTO has matching vendorId, lineCount, status |
| `APInvoiceMapper_fromCreateRequest_mapsRequest` | `APInvoiceMapper.fromCreateRequest(req)` | Valid create request | Entity has correct vendor ref and lines |
| `APInvoiceLineMapper_toDto_mapsAllFields` | `APInvoiceLineMapper.toDto(line)` | Line with product, qty, unitPrice | DTO has lineTotal = qty × unitPrice |
| `APPaymentMapper_toDto_mapsAllFields` | `APPaymentMapper.toDto(payment)` | Payment entity | DTO has correct amount, invoiceId, paymentDate |

---

## 5. Integration Tests — APInvoiceController & APPaymentController

Base path: `/api/v1/finance/ap`

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|---|---|---|---|
| `getAPInvoices_returns200_withFinanceRead` | `GET /api/v1/finance/ap/invoices` | Bearer `FINANCE_READ` | `200 OK`; paged JSON |
| `getAPInvoices_returns403_withoutPermission` | `GET /api/v1/finance/ap/invoices` | Bearer without `FINANCE_READ` | `403 Forbidden` |
| `getAPInvoice_returns200_whenFound` | `GET /api/v1/finance/ap/invoices/{id}` | Bearer `FINANCE_READ` | `200 OK`; invoice JSON |
| `getAPInvoice_returns404_whenNotFound` | `GET /api/v1/finance/ap/invoices/{id}` | Bearer `FINANCE_READ`; unknown id | `404 Not Found` |
| `createAPInvoice_returns201_withValidRequest` | `POST /api/v1/finance/ap/invoices` | Bearer `FINANCE_WRITE`; valid body | `201 Created`; status=DRAFT |
| `createAPInvoice_returns404_whenVendorNotFound` | `POST /api/v1/finance/ap/invoices` | Bearer `FINANCE_WRITE`; bad vendorId | `404 Not Found` |
| `createAPInvoice_returns400_whenEmptyLines` | `POST /api/v1/finance/ap/invoices` | Bearer `FINANCE_WRITE`; no lines | `400 Bad Request` |
| `approveAPInvoice_returns200_fromDraft` | `PUT /api/v1/finance/ap/invoices/{id}/approve` | Bearer `FINANCE_WRITE` | `200 OK`; status=APPROVED |
| `approveAPInvoice_returns422_whenAlreadyApproved` | `PUT /api/v1/finance/ap/invoices/{id}/approve` | Bearer `FINANCE_WRITE`; already approved | `422 Unprocessable Entity` |
| `postAPInvoice_returns200_fromApproved` | `PUT /api/v1/finance/ap/invoices/{id}/post` | Bearer `FINANCE_WRITE` | `200 OK`; status=POSTED |
| `cancelAPInvoice_returns200_fromDraft` | `PUT /api/v1/finance/ap/invoices/{id}/cancel` | Bearer `FINANCE_WRITE` | `200 OK`; status=CANCELLED |
| `cancelAPInvoice_returns422_whenHasPayments` | `PUT /api/v1/finance/ap/invoices/{id}/cancel` | Bearer `FINANCE_WRITE`; paid invoice | `422 Unprocessable Entity` |
| `getOverdueAPInvoices_returns200_withList` | `GET /api/v1/finance/ap/invoices/overdue` | Bearer `FINANCE_READ` | `200 OK`; array of overdue invoices |
| `createAPPayment_returns201_reducesBalance` | `POST /api/v1/finance/ap/payments` | Bearer `FINANCE_WRITE`; valid body | `201 Created`; invoice balance reduced |
| `createAPPayment_returns422_whenOverpayment` | `POST /api/v1/finance/ap/payments` | Bearer `FINANCE_WRITE`; amount > balance | `422 Unprocessable Entity` |
| `getAPPayments_returns200_paged` | `GET /api/v1/finance/ap/payments` | Bearer `FINANCE_READ` | `200 OK`; paged payments |
| `voidAPPayment_returns200_restoresBalance` | `PUT /api/v1/finance/ap/payments/{id}/void` | Bearer `FINANCE_WRITE` | `200 OK`; invoice balance restored |
| `voidAPPayment_returns422_whenAlreadyVoided` | `PUT /api/v1/finance/ap/payments/{id}/void` | Bearer `FINANCE_WRITE`; already voided | `422 Unprocessable Entity` |

---

# Section 2a-ii: Finance — Accounts Receivable & Customer Ledger — Test Plan

---

## 1. ARInvoiceService Unit Tests

Framework: JUnit 5 + Mockito.

### 1.1 `getARInvoices(tenantId, pageable)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getARInvoices_returnsPaginatedResults` | `getARInvoices(tenantId, pageable)` | Repository returns page | Returns `Page<ARInvoiceDto>` |
| `getARInvoices_returnsEmpty_whenNone` | `getARInvoices(tenantId, pageable)` | No invoices | Returns empty page |
| `getARInvoices_respectsTenantIsolation` | `getARInvoices(tenantId, pageable)` | Two tenants | Only tenant-A invoices returned |

### 1.2 `createARInvoice(tenantId, request)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `createARInvoice_success_returnsDraftDto` | `createARInvoice(tenantId, request)` | Valid customer + lines | Returns dto with `status=DRAFT` |
| `createARInvoice_customerNotFound_throwsNotFoundException` | `createARInvoice(tenantId, request)` | Customer id missing | Throws `NotFoundException` |
| `createARInvoice_emptyLines_throwsValidationException` | `createARInvoice(tenantId, request)` | No line items | Throws `ValidationException` |
| `createARInvoice_duplicateInvoiceNumber_throwsDuplicateResourceException` | `createARInvoice(tenantId, request)` | Invoice number exists for customer | Throws `DuplicateResourceException` |

### 1.3 `approveARInvoice(tenantId, invoiceId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `approveARInvoice_draft_becomesApproved` | `approveARInvoice(tenantId, id)` | Invoice `DRAFT` | Status becomes `APPROVED` |
| `approveARInvoice_notDraft_throwsBusinessException` | `approveARInvoice(tenantId, id)` | Invoice already `APPROVED` | Throws `BusinessException` |

### 1.4 `postARInvoice(tenantId, invoiceId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `postARInvoice_approved_createsJournalEntry` | `postARInvoice(tenantId, id)` | Invoice `APPROVED` | Status `POSTED`; GL journal entry created |
| `postARInvoice_draft_throwsBusinessException` | `postARInvoice(tenantId, id)` | Not approved | Throws `BusinessException` |

### 1.5 `cancelARInvoice(tenantId, invoiceId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `cancelARInvoice_draft_cancelled` | `cancelARInvoice(tenantId, id)` | `DRAFT` invoice | Status becomes `CANCELLED` |
| `cancelARInvoice_hasPayments_throwsBusinessException` | `cancelARInvoice(tenantId, id)` | Invoice partially paid | Throws `BusinessException` |

### 1.6 `getOverdueARInvoices(tenantId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getOverdueARInvoices_returnsPastDueUnpaid` | `getOverdueARInvoices(tenantId)` | 3 past due, 1 future | Returns list of 3 |
| `getOverdueARInvoices_returnsEmpty_whenNone` | `getOverdueARInvoices(tenantId)` | All paid or not due | Returns empty list |

### 1.7 `getCustomerBalance(tenantId, customerId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getCustomerBalance_returnsNetBalance` | `getCustomerBalance(tenantId, customerId)` | Invoices total 1000, payments total 400 | Returns balance 600 |
| `getCustomerBalance_returnsZero_whenFullyPaid` | `getCustomerBalance(tenantId, customerId)` | All invoices fully paid | Returns `BigDecimal.ZERO` |
| `getCustomerBalance_customerNotFound_throwsNotFoundException` | `getCustomerBalance(tenantId, customerId)` | Customer missing | Throws `NotFoundException` |

---

## 2. ARPaymentService Unit Tests

### 2.1 `createARPayment(tenantId, request)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `createARPayment_success_reducesAmountDue` | `createARPayment(tenantId, request)` | Payment amount ≤ invoice balance | Payment saved; invoice balance reduced |
| `createARPayment_fullyPaid_statusUpdated` | `createARPayment(tenantId, request)` | Payment = remaining balance | Invoice status becomes `FULLY_PAID` |
| `createARPayment_overpayment_throwsBusinessException` | `createARPayment(tenantId, request)` | Amount > balance | Throws `BusinessException` |
| `createARPayment_zeroPmt_throwsValidationException` | `createARPayment(tenantId, request)` | `amount=0` | Throws `ValidationException` |
| `createARPayment_invoiceNotFound_throwsNotFoundException` | `createARPayment(tenantId, request)` | Invoice missing | Throws `NotFoundException` |

### 2.2 `voidARPayment(tenantId, paymentId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `voidARPayment_success_restoresBalance` | `voidARPayment(tenantId, id)` | Active payment | Voided; invoice balance restored |
| `voidARPayment_alreadyVoided_throwsBusinessException` | `voidARPayment(tenantId, id)` | Already voided | Throws `BusinessException` |

---

## 3. CustomerService Unit Tests

### 3.1 CRUD + Auto-code

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getCustomers_returnsPaged` | `getCustomers(tenantId, pageable)` | Multiple customers | Returns correct paged result |
| `getCustomer_found_returnsDto` | `getCustomer(tenantId, id)` | Customer exists | Returns `CustomerDto` |
| `getCustomer_notFound_throwsNotFoundException` | `getCustomer(tenantId, id)` | Missing customer | Throws `NotFoundException` |
| `createCustomer_success_autoCodeGenerated` | `createCustomer(tenantId, request)` | Valid request | Returns dto with non-null `code` like `CUST-000001` |
| `createCustomer_duplicateName_throwsDuplicateResourceException` | `createCustomer(tenantId, request)` | Name already exists | Throws `DuplicateResourceException` |
| `updateCustomer_success_updatesFields` | `updateCustomer(tenantId, id, request)` | Valid update | Returns updated dto |
| `updateCustomer_notFound_throwsNotFoundException` | `updateCustomer(tenantId, id, request)` | Missing customer | Throws `NotFoundException` |
| `deleteCustomer_success_removesRecord` | `deleteCustomer(tenantId, id)` | Customer with no transactions | Record deleted |
| `deleteCustomer_hasTransactions_throwsBusinessException` | `deleteCustomer(tenantId, id)` | Customer has POS transactions | Throws `BusinessException` |

---

## 4. Repository Tests (`@DataJpaTest` + Testcontainers)

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `ARInvoiceRepository_findByCustomerId` | `findByCustomerId(customerId)` | 3 invoices for customer A | Returns 3 |
| `ARInvoiceRepository_findByStatus` | `findByStatus(POSTED)` | Mix of statuses | Returns only POSTED |
| `ARInvoiceRepository_findOverdueInvoices` | `findOverdueInvoices(today)` | 2 past due | Returns 2 |
| `ARInvoiceRepository_sumBalanceByCustomer` | `sumAmountDueByCustomer(customerId)` | 3 invoices with balances | Returns correct sum |
| `ARPaymentRepository_findByInvoiceId` | `findByInvoiceId(invoiceId)` | 2 payments | Returns 2 |
| `ARPaymentRepository_findByCustomerId` | `findByCustomerId(customerId)` | Multiple payments | Returns all for customer |
| `CustomerRepository_findByCode` | `findByCode(tenantId, code)` | Code "CUST-000001" exists | Returns non-empty Optional |
| `CustomerRepository_searchByNameOrPhone` | `searchByNameOrPhone(tenantId, "Ali")` | Customers matching name | Returns matching customers |

---

## 5. Integration Tests — ARInvoiceController, ARPaymentController, CustomerController

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|---|---|---|---|
| `getARInvoices_returns200` | `GET /api/v1/finance/ar/invoices` | Bearer `FINANCE_READ` | `200 OK`; paged JSON |
| `getARInvoices_returns403` | `GET /api/v1/finance/ar/invoices` | No `FINANCE_READ` | `403 Forbidden` |
| `getARInvoice_returns200_whenFound` | `GET /api/v1/finance/ar/invoices/{id}` | Bearer `FINANCE_READ` | `200 OK` |
| `getARInvoice_returns404` | `GET /api/v1/finance/ar/invoices/{id}` | Bearer `FINANCE_READ`; unknown id | `404 Not Found` |
| `createARInvoice_returns201` | `POST /api/v1/finance/ar/invoices` | Bearer `FINANCE_WRITE`; valid | `201 Created`; status=DRAFT |
| `createARInvoice_returns404_unknownCustomer` | `POST /api/v1/finance/ar/invoices` | Bearer `FINANCE_WRITE`; bad customerId | `404 Not Found` |
| `approveARInvoice_returns200` | `PUT /api/v1/finance/ar/invoices/{id}/approve` | Bearer `FINANCE_WRITE` | `200 OK`; status=APPROVED |
| `approveARInvoice_returns422_notDraft` | `PUT /api/v1/finance/ar/invoices/{id}/approve` | Bearer `FINANCE_WRITE` | `422 Unprocessable Entity` |
| `postARInvoice_returns200` | `PUT /api/v1/finance/ar/invoices/{id}/post` | Bearer `FINANCE_WRITE` | `200 OK`; status=POSTED |
| `getOverdueARInvoices_returns200` | `GET /api/v1/finance/ar/invoices/overdue` | Bearer `FINANCE_READ` | `200 OK`; array |
| `createARPayment_returns201` | `POST /api/v1/finance/ar/payments` | Bearer `FINANCE_WRITE`; valid | `201 Created` |
| `createARPayment_returns422_overpayment` | `POST /api/v1/finance/ar/payments` | Bearer `FINANCE_WRITE` | `422 Unprocessable Entity` |
| `voidARPayment_returns200` | `PUT /api/v1/finance/ar/payments/{id}/void` | Bearer `FINANCE_WRITE` | `200 OK` |
| `getCustomers_returns200` | `GET /api/v1/customers` | Bearer `CUSTOMER_READ` | `200 OK`; paged |
| `createCustomer_returns201_withAutoCode` | `POST /api/v1/customers` | Bearer `CUSTOMER_WRITE`; valid | `201 Created`; code not null |
| `updateCustomer_returns200` | `PUT /api/v1/customers/{id}` | Bearer `CUSTOMER_WRITE` | `200 OK`; updated fields |
| `deleteCustomer_returns204` | `DELETE /api/v1/customers/{id}` | Bearer `CUSTOMER_WRITE`; no txns | `204 No Content` |
| `deleteCustomer_returns422_hasTransactions` | `DELETE /api/v1/customers/{id}` | Bearer `CUSTOMER_WRITE`; has txns | `422 Unprocessable Entity` |
| `getCustomerBalance_returns200` | `GET /api/v1/customers/{id}/balance` | Bearer `FINANCE_READ` | `200 OK`; numeric balance |

---

# Section 2b-i: Finance — General Ledger & Journal Entries — Test Plan

---

## 1. ChartOfAccountsService Unit Tests

Framework: JUnit 5 + Mockito.

### 1.1 CRUD Operations

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getAccounts_returnsPaged` | `getAccounts(tenantId, pageable)` | Repository returns accounts | Returns `Page<AccountDto>` |
| `getAccount_found_returnsDto` | `getAccount(tenantId, id)` | Account exists | Returns `AccountDto` with all fields |
| `getAccount_notFound_throwsNotFoundException` | `getAccount(tenantId, id)` | Account missing | Throws `NotFoundException` |
| `createAccount_success_returnsDto` | `createAccount(tenantId, request)` | Valid request | Returns `AccountDto` with generated id |
| `createAccount_duplicateCode_throwsDuplicateResourceException` | `createAccount(tenantId, request)` | Account code exists | Throws `DuplicateResourceException` |
| `updateAccount_success_updatesFields` | `updateAccount(tenantId, id, request)` | Valid update | Returns updated dto |
| `updateAccount_notFound_throwsNotFoundException` | `updateAccount(tenantId, id, request)` | Account missing | Throws `NotFoundException` |
| `deleteAccount_success` | `deleteAccount(tenantId, id)` | Account with no journal lines | Account deleted |
| `deleteAccount_hasJournalLines_throwsBusinessException` | `deleteAccount(tenantId, id)` | Account used in GL | Throws `BusinessException` |
| `getAccountsByType_returnsMatchingAccounts` | `getAccountsByType(tenantId, ASSET)` | Mix of account types | Returns only ASSET accounts |

---

## 2. JournalEntryService Unit Tests

### 2.1 `getJournalEntries(tenantId, pageable)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getJournalEntries_returnsPaged` | `getJournalEntries(tenantId, pageable)` | Entries exist | Returns paged `JournalEntryDto` |
| `getJournalEntries_returnsEmpty` | `getJournalEntries(tenantId, pageable)` | None | Returns empty page |

### 2.2 `createJournalEntry(tenantId, request)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `createJournalEntry_balanced_returnsDraftDto` | `createJournalEntry(tenantId, request)` | Debits = Credits | Returns dto with `status=DRAFT` |
| `createJournalEntry_unbalanced_throwsValidationException` | `createJournalEntry(tenantId, request)` | Debits ≠ Credits | Throws `ValidationException` "Debits must equal credits" |
| `createJournalEntry_emptyLines_throwsValidationException` | `createJournalEntry(tenantId, request)` | No lines | Throws `ValidationException` |
| `createJournalEntry_accountNotFound_throwsNotFoundException` | `createJournalEntry(tenantId, request)` | Line references missing account | Throws `NotFoundException` referencing accountId |
| `createJournalEntry_closedPeriod_throwsBusinessException` | `createJournalEntry(tenantId, request)` | Entry date in closed fiscal period | Throws `BusinessException` "Fiscal period is closed" |

### 2.3 `postJournalEntry(tenantId, entryId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `postJournalEntry_draft_updatesLedgerBalances` | `postJournalEntry(tenantId, id)` | Entry `DRAFT` | Status becomes `POSTED`; account balances updated |
| `postJournalEntry_alreadyPosted_throwsBusinessException` | `postJournalEntry(tenantId, id)` | Entry already `POSTED` | Throws `BusinessException` |
| `postJournalEntry_reversed_throwsBusinessException` | `postJournalEntry(tenantId, id)` | Entry `REVERSED` | Throws `BusinessException` |

### 2.4 `reverseJournalEntry(tenantId, entryId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `reverseJournalEntry_posted_createsReversalEntry` | `reverseJournalEntry(tenantId, id)` | Entry `POSTED` | Original entry becomes `REVERSED`; new reversal entry created with swapped DR/CR |
| `reverseJournalEntry_draft_throwsBusinessException` | `reverseJournalEntry(tenantId, id)` | Entry not posted | Throws `BusinessException` |
| `reverseJournalEntry_alreadyReversed_throwsBusinessException` | `reverseJournalEntry(tenantId, id)` | Already reversed | Throws `BusinessException` |

---

## 3. LedgerService Unit Tests

### 3.1 `getAccountLedger(tenantId, accountId, from, to)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getAccountLedger_returnsEntriesInDateRange` | `getAccountLedger(tenantId, accountId, from, to)` | 5 entries: 3 in range, 2 outside | Returns 3 entries |
| `getAccountLedger_returnsEmpty_whenNoEntries` | `getAccountLedger(tenantId, accountId, from, to)` | No entries in range | Returns empty list |
| `getAccountLedger_accountNotFound_throwsNotFoundException` | `getAccountLedger(tenantId, accountId, from, to)` | Account missing | Throws `NotFoundException` |

### 3.2 `getTrialBalance(tenantId, asOfDate)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getTrialBalance_balancedResult` | `getTrialBalance(tenantId, asOfDate)` | Posted entries; DR = CR | Returns trial balance with matching debit/credit totals |
| `getTrialBalance_returnsEmpty_whenNoPostedEntries` | `getTrialBalance(tenantId, asOfDate)` | No posted entries | Returns empty or zero-balance result |

### 3.3 `getIncomeStatement(tenantId, from, to)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getIncomeStatement_correctNetIncome` | `getIncomeStatement(tenantId, from, to)` | Revenue 10000, expenses 6000 | Net income = 4000 |
| `getIncomeStatement_netLoss_whenExpensesExceedRevenue` | `getIncomeStatement(tenantId, from, to)` | Revenue 1000, expenses 3000 | Net income = -2000 |

### 3.4 `getBalanceSheet(tenantId, asOfDate)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getBalanceSheet_assetsEqualsLiabilitiesPlusEquity` | `getBalanceSheet(tenantId, asOfDate)` | Balanced books | Assets = Liabilities + Equity |

---

## 4. Repository Tests (`@DataJpaTest` + Testcontainers)

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `AccountRepository_findByCode` | `findByCode(tenantId, code)` | Code "1010" exists | Returns non-empty Optional |
| `AccountRepository_findByType` | `findByType(tenantId, ASSET)` | Mix of types | Returns only ASSET |
| `JournalEntryRepository_findByStatus` | `findByStatus(tenantId, POSTED)` | Mix of statuses | Returns only POSTED |
| `JournalEntryRepository_findByDateRange` | `findByDateRange(tenantId, from, to)` | 3 in range, 2 outside | Returns 3 |
| `JournalEntryRepository_findByReference` | `findByReference(tenantId, ref)` | Reference "PO-001" | Returns matching entry |
| `JournalLineRepository_findByAccountId` | `findByAccountId(accountId)` | Lines for account | Returns all lines for account |
| `LedgerBalanceRepository_findByAccountAndPeriod` | `findByAccountAndPeriod(id, period)` | Balance exists | Returns Optional with balance |

---

## 5. Integration Tests — AccountController & JournalEntryController

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|---|---|---|---|
| `getAccounts_returns200` | `GET /api/v1/finance/gl/accounts` | Bearer `FINANCE_READ` | `200 OK`; paged accounts |
| `getAccounts_returns403` | `GET /api/v1/finance/gl/accounts` | No `FINANCE_READ` | `403 Forbidden` |
| `createAccount_returns201` | `POST /api/v1/finance/gl/accounts` | Bearer `FINANCE_WRITE`; valid | `201 Created` |
| `createAccount_returns409_duplicateCode` | `POST /api/v1/finance/gl/accounts` | Bearer `FINANCE_WRITE`; dup code | `409 Conflict` |
| `updateAccount_returns200` | `PUT /api/v1/finance/gl/accounts/{id}` | Bearer `FINANCE_WRITE` | `200 OK` |
| `deleteAccount_returns204` | `DELETE /api/v1/finance/gl/accounts/{id}` | Bearer `FINANCE_WRITE`; no entries | `204 No Content` |
| `deleteAccount_returns422_hasEntries` | `DELETE /api/v1/finance/gl/accounts/{id}` | Bearer `FINANCE_WRITE` | `422 Unprocessable Entity` |
| `getJournalEntries_returns200` | `GET /api/v1/finance/gl/journal-entries` | Bearer `FINANCE_READ` | `200 OK`; paged |
| `createJournalEntry_returns201_balanced` | `POST /api/v1/finance/gl/journal-entries` | Bearer `FINANCE_WRITE`; DR=CR | `201 Created`; status=DRAFT |
| `createJournalEntry_returns400_unbalanced` | `POST /api/v1/finance/gl/journal-entries` | Bearer `FINANCE_WRITE`; DR≠CR | `400 Bad Request` |
| `createJournalEntry_returns422_closedPeriod` | `POST /api/v1/finance/gl/journal-entries` | Bearer `FINANCE_WRITE`; closed period | `422 Unprocessable Entity` |
| `postJournalEntry_returns200` | `PUT /api/v1/finance/gl/journal-entries/{id}/post` | Bearer `FINANCE_WRITE` | `200 OK`; status=POSTED |
| `postJournalEntry_returns422_alreadyPosted` | `PUT /api/v1/finance/gl/journal-entries/{id}/post` | Bearer `FINANCE_WRITE` | `422 Unprocessable Entity` |
| `reverseJournalEntry_returns201_reversalCreated` | `POST /api/v1/finance/gl/journal-entries/{id}/reverse` | Bearer `FINANCE_WRITE` | `201 Created`; reversal entry |
| `getTrialBalance_returns200` | `GET /api/v1/finance/gl/reports/trial-balance` | Bearer `FINANCE_READ` | `200 OK`; balanced totals |
| `getIncomeStatement_returns200` | `GET /api/v1/finance/gl/reports/income-statement` | Bearer `FINANCE_READ` | `200 OK`; netIncome field |
| `getBalanceSheet_returns200` | `GET /api/v1/finance/gl/reports/balance-sheet` | Bearer `FINANCE_READ` | `200 OK`; assets = liabilities + equity |

---

# Section 2b-ii: Finance — Bank, Tax, Currency & Fiscal Period — Test Plan

---

## 1. BankAccountService Unit Tests

Framework: JUnit 5 + Mockito.

### 1.1 CRUD Operations

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getBankAccounts_returnsPaged` | `getBankAccounts(tenantId, pageable)` | Multiple accounts | Returns `Page<BankAccountDto>` |
| `getBankAccount_found_returnsDto` | `getBankAccount(tenantId, id)` | Account exists | Returns `BankAccountDto` |
| `getBankAccount_notFound_throwsNotFoundException` | `getBankAccount(tenantId, id)` | Missing | Throws `NotFoundException` |
| `createBankAccount_success` | `createBankAccount(tenantId, request)` | Valid request | Returns dto with id |
| `createBankAccount_duplicateAccountNumber_throwsDuplicateResourceException` | `createBankAccount(tenantId, request)` | Account number exists | Throws `DuplicateResourceException` |
| `updateBankAccount_success` | `updateBankAccount(tenantId, id, request)` | Valid | Returns updated dto |
| `deactivateBankAccount_success` | `deactivateBankAccount(tenantId, id)` | Active account | Status becomes INACTIVE |
| `deactivateBankAccount_alreadyInactive_idempotent` | `deactivateBankAccount(tenantId, id)` | Already INACTIVE | No error; remains INACTIVE |

### 1.2 `getBankBalance(tenantId, accountId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getBankBalance_returnsNetBalance` | `getBankBalance(tenantId, id)` | Deposits 10000, withdrawals 3000 | Returns 7000 |
| `getBankBalance_returnsOpeningBalance_whenNoTransactions` | `getBankBalance(tenantId, id)` | No transactions | Returns opening balance |

---

## 2. BankTransactionService Unit Tests

### 2.1 `createBankTransaction(tenantId, request)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `createBankTransaction_deposit_increasesBalance` | `createBankTransaction(tenantId, request)` | type=DEPOSIT, amount=500 | Returns dto; balance increases by 500 |
| `createBankTransaction_withdrawal_decreasesBalance` | `createBankTransaction(tenantId, request)` | type=WITHDRAWAL, amount=200 | Returns dto; balance decreases by 200 |
| `createBankTransaction_insufficientFunds_throwsBusinessException` | `createBankTransaction(tenantId, request)` | Withdrawal > balance | Throws `BusinessException` "Insufficient funds" |
| `createBankTransaction_zeroPmt_throwsValidationException` | `createBankTransaction(tenantId, request)` | amount=0 | Throws `ValidationException` |
| `createBankTransaction_accountNotFound_throwsNotFoundException` | `createBankTransaction(tenantId, request)` | Account missing | Throws `NotFoundException` |

### 2.2 `reconcileBankTransaction(tenantId, transactionId)`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `reconcile_unreconciled_becomesReconciled` | `reconcile(tenantId, id)` | Transaction UNRECONCILED | Status becomes RECONCILED |
| `reconcile_alreadyReconciled_throwsBusinessException` | `reconcile(tenantId, id)` | Already RECONCILED | Throws `BusinessException` |

---

## 3. TaxService Unit Tests

### 3.1 CRUD + Calculation

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getTaxRates_returnsPaged` | `getTaxRates(tenantId, pageable)` | Multiple rates | Returns paged dto |
| `getTaxRate_found_returnsDto` | `getTaxRate(tenantId, id)` | Rate exists | Returns dto |
| `getTaxRate_notFound_throwsNotFoundException` | `getTaxRate(tenantId, id)` | Missing | Throws `NotFoundException` |
| `createTaxRate_success` | `createTaxRate(tenantId, request)` | Valid rate 15% | Returns dto with rate=15 |
| `createTaxRate_duplicateName_throwsDuplicateResourceException` | `createTaxRate(tenantId, request)` | Name exists | Throws `DuplicateResourceException` |
| `updateTaxRate_success` | `updateTaxRate(tenantId, id, request)` | Valid update | Returns updated dto |
| `calculateTax_returnsCorrectAmount` | `calculateTax(tenantId, amount, taxRateId)` | amount=1000, rate=15% | Returns 150 |
| `calculateTax_zeroRate_returnsZero` | `calculateTax(tenantId, amount, taxRateId)` | rate=0% | Returns 0 |
| `calculateTax_exemptProduct_returnsZero` | `calculateTax(tenantId, amount, taxRateId)` | Product is tax-exempt | Returns 0 |
| `getActiveTaxRates_returnsOnlyActive` | `getActiveTaxRates(tenantId)` | Mix of active/inactive | Returns only active rates |

---

## 4. CurrencyService Unit Tests

### 4.1 CRUD + Conversion

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getCurrencies_returnsList` | `getCurrencies(tenantId)` | 3 currencies configured | Returns list of 3 |
| `getBaseCurrency_returnsDefault` | `getBaseCurrency(tenantId)` | Base currency configured | Returns base currency dto |
| `createCurrency_success` | `createCurrency(tenantId, request)` | Valid "USD" | Returns dto |
| `createCurrency_duplicateCode_throwsDuplicateResourceException` | `createCurrency(tenantId, request)` | "USD" exists | Throws `DuplicateResourceException` |
| `updateExchangeRate_success` | `updateExchangeRate(tenantId, currencyId, rate)` | Rate=12700 | Exchange rate updated |
| `convertAmount_correctResult` | `convertAmount(tenantId, 100, "USD", "UZS")` | Rate 12700 | Returns 1270000 |
| `convertAmount_sameTargetCurrency_returnsOriginal` | `convertAmount(tenantId, 100, "UZS", "UZS")` | Same source/target | Returns 100 |
| `convertAmount_currencyNotFound_throwsNotFoundException` | `convertAmount(tenantId, 100, "XYZ", "UZS")` | "XYZ" not configured | Throws `NotFoundException` |

---

## 5. FiscalPeriodService Unit Tests

### 5.1 CRUD + Period Management

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getFiscalPeriods_returnsList` | `getFiscalPeriods(tenantId)` | Multiple periods | Returns list |
| `getFiscalPeriod_found_returnsDto` | `getFiscalPeriod(tenantId, id)` | Period exists | Returns dto |
| `getCurrentFiscalPeriod_returnsOpenPeriod` | `getCurrentFiscalPeriod(tenantId)` | One OPEN period matching today | Returns that period |
| `getCurrentFiscalPeriod_noOpenPeriod_throwsBusinessException` | `getCurrentFiscalPeriod(tenantId)` | No OPEN periods | Throws `BusinessException` |
| `createFiscalPeriod_success` | `createFiscalPeriod(tenantId, request)` | Valid dates, no overlap | Returns dto with status=OPEN |
| `createFiscalPeriod_overlappingDates_throwsBusinessException` | `createFiscalPeriod(tenantId, request)` | Dates overlap existing | Throws `BusinessException` "Fiscal periods cannot overlap" |
| `closeFiscalPeriod_open_becomesClosed` | `closeFiscalPeriod(tenantId, id)` | Period OPEN | Status becomes CLOSED |
| `closeFiscalPeriod_alreadyClosed_throwsBusinessException` | `closeFiscalPeriod(tenantId, id)` | Already CLOSED | Throws `BusinessException` |
| `reopenFiscalPeriod_closed_becomesOpen` | `reopenFiscalPeriod(tenantId, id)` | Period CLOSED | Status becomes OPEN |

---

## 6. Repository Tests (`@DataJpaTest` + Testcontainers)

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `BankAccountRepository_findByAccountNumber` | `findByAccountNumber(tenantId, acct)` | Account exists | Non-empty Optional |
| `BankAccountRepository_findActiveAccounts` | `findActiveAccounts(tenantId)` | Mix active/inactive | Returns only active |
| `BankTransactionRepository_findByBankAccountId` | `findByBankAccountId(id)` | Multiple transactions | Returns all for account |
| `BankTransactionRepository_sumByTypeAndDateRange` | `sumByTypeAndDateRange(id, DEPOSIT, from, to)` | 3 deposits in range | Returns sum |
| `TaxRateRepository_findByCode` | `findByCode(tenantId, code)` | Code "VAT20" | Returns Optional |
| `TaxRateRepository_findActiveRates` | `findActiveRates(tenantId)` | Mix active/inactive | Returns only active |
| `CurrencyRepository_findByCode` | `findByCode(tenantId, "USD")` | USD exists | Returns Optional |
| `CurrencyRepository_findBaseCurrency` | `findBaseCurrency(tenantId)` | One base currency | Returns it |
| `FiscalPeriodRepository_findOpenPeriod` | `findOpenPeriodContainingDate(tenantId, date)` | Period covers date | Returns matching period |
| `FiscalPeriodRepository_findOverlapping` | `findOverlapping(tenantId, from, to)` | Overlap exists | Returns non-empty list |

---

## 7. Integration Tests

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|---|---|---|---|
| `getBankAccounts_returns200` | `GET /api/v1/finance/bank/accounts` | Bearer `FINANCE_READ` | `200 OK`; paged |
| `createBankAccount_returns201` | `POST /api/v1/finance/bank/accounts` | Bearer `FINANCE_WRITE`; valid | `201 Created` |
| `createBankAccount_returns409_duplicateNumber` | `POST /api/v1/finance/bank/accounts` | Bearer `FINANCE_WRITE`; dup | `409 Conflict` |
| `createBankTransaction_returns201_deposit` | `POST /api/v1/finance/bank/transactions` | Bearer `FINANCE_WRITE`; DEPOSIT | `201 Created`; balance increased |
| `createBankTransaction_returns422_insufficientFunds` | `POST /api/v1/finance/bank/transactions` | Bearer `FINANCE_WRITE`; WITHDRAWAL > balance | `422 Unprocessable Entity` |
| `reconcileTransaction_returns200` | `PUT /api/v1/finance/bank/transactions/{id}/reconcile` | Bearer `FINANCE_WRITE` | `200 OK`; status=RECONCILED |
| `getTaxRates_returns200` | `GET /api/v1/finance/tax/rates` | Bearer `FINANCE_READ` | `200 OK` |
| `createTaxRate_returns201` | `POST /api/v1/finance/tax/rates` | Bearer `FINANCE_WRITE`; valid | `201 Created` |
| `calculateTax_returns200` | `POST /api/v1/finance/tax/calculate` | Bearer `FINANCE_READ` | `200 OK`; tax amount field |
| `getCurrencies_returns200` | `GET /api/v1/finance/currencies` | Bearer `FINANCE_READ` | `200 OK`; list |
| `createCurrency_returns201` | `POST /api/v1/finance/currencies` | Bearer `FINANCE_WRITE` | `201 Created` |
| `convertAmount_returns200` | `POST /api/v1/finance/currencies/convert` | Bearer `FINANCE_READ` | `200 OK`; converted amount |
| `getFiscalPeriods_returns200` | `GET /api/v1/finance/fiscal-periods` | Bearer `FINANCE_READ` | `200 OK`; list |
| `createFiscalPeriod_returns201` | `POST /api/v1/finance/fiscal-periods` | Bearer `FINANCE_WRITE` | `201 Created`; status=OPEN |
| `createFiscalPeriod_returns422_overlap` | `POST /api/v1/finance/fiscal-periods` | Bearer `FINANCE_WRITE`; overlapping | `422 Unprocessable Entity` |
| `closeFiscalPeriod_returns200` | `PUT /api/v1/finance/fiscal-periods/{id}/close` | Bearer `FINANCE_WRITE` | `200 OK`; status=CLOSED |
| `closeFiscalPeriod_returns422_alreadyClosed` | `PUT /api/v1/finance/fiscal-periods/{id}/close` | Bearer `FINANCE_WRITE` | `422 Unprocessable Entity` |

---

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

---

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

---

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

---

# Section 3b-ii: Inventory — Purchase Orders, Receiving, Counts, Planning & Barcode — Test Plan

---

## 1. PurchaseOrderService Unit Tests

Framework: JUnit 5 + Mockito.

### 1.1 CRUD

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getPurchaseOrders_returnsPaged` | `getPurchaseOrders(tenantId, pageable)` | Multiple POs | Returns paged `PurchaseOrderDto` |
| `getPurchaseOrder_found_returnsDto` | `getPurchaseOrder(tenantId, id)` | PO exists | Returns dto |
| `getPurchaseOrder_notFound_throwsNotFoundException` | `getPurchaseOrder(tenantId, id)` | Missing | Throws `NotFoundException` |
| `createPurchaseOrder_success_returnsDraft` | `createPurchaseOrder(tenantId, request)` | Valid vendor + lines | Returns dto with `status=DRAFT` |
| `createPurchaseOrder_vendorNotFound_throwsNotFoundException` | `createPurchaseOrder(tenantId, request)` | Vendor missing | Throws `NotFoundException` |
| `createPurchaseOrder_emptyLines_throwsValidationException` | `createPurchaseOrder(tenantId, request)` | No lines | Throws `ValidationException` |
| `updatePurchaseOrder_success` | `updatePurchaseOrder(tenantId, id, request)` | PO in DRAFT | Returns updated dto |
| `updatePurchaseOrder_released_throwsBusinessException` | `updatePurchaseOrder(tenantId, id, request)` | Status=RELEASED | Throws `BusinessException` |

### 1.2 Status Transitions

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `releasePurchaseOrder_draft_becomesReleased` | `releasePurchaseOrder(tenantId, id)` | DRAFT | Status RELEASED |
| `releasePurchaseOrder_alreadyReleased_throwsBusinessException` | `releasePurchaseOrder(tenantId, id)` | Already RELEASED | Throws `BusinessException` |
| `releasePurchaseOrder_cancelled_throwsBusinessException` | `releasePurchaseOrder(tenantId, id)` | CANCELLED | Throws `BusinessException` |
| `cancelPurchaseOrder_draft_becomesCancelled` | `cancelPurchaseOrder(tenantId, id)` | DRAFT | Status CANCELLED |
| `cancelPurchaseOrder_released_becomesCancelled` | `cancelPurchaseOrder(tenantId, id)` | RELEASED | Status CANCELLED |
| `cancelPurchaseOrder_fullyReceived_throwsBusinessException` | `cancelPurchaseOrder(tenantId, id)` | All lines received | Throws `BusinessException` |

### 1.3 `receivePurchaseOrder`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `receivePurchaseOrder_released_validQty_updatesStock` | `receivePurchaseOrder(tenantId, id, request)` | RELEASED; qty ≤ ordered | Stock updated; movement RECEIPT created |
| `receivePurchaseOrder_notReleased_throwsBusinessException` | `receivePurchaseOrder(tenantId, id, request)` | Not RELEASED | Throws `BusinessException` |
| `receivePurchaseOrder_overReceive_throwsBusinessException` | `receivePurchaseOrder(tenantId, id, request)` | qty > ordered | Throws `BusinessException` |

---

## 2. ReceivingService Unit Tests

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getReceivingOrders_returnsPaged` | `getReceivingOrders(tenantId, pageable)` | Multiple | Returns paged |
| `getReceivingOrder_found_returnsDto` | `getReceivingOrder(tenantId, id)` | Exists | Returns dto |
| `getReceivingOrder_notFound_throwsNotFoundException` | `getReceivingOrder(tenantId, id)` | Missing | Throws `NotFoundException` |
| `getReceivingOrdersByPO_returnsList` | `getReceivingOrdersForPO(tenantId, poId)` | 2 receiving orders for PO | Returns 2 |
| `createReceivingOrder_success` | `createReceivingOrder(tenantId, request)` | PO in RELEASED status | Returns dto with PENDING status |
| `createReceivingOrder_poNotFound_throwsNotFoundException` | `createReceivingOrder(tenantId, request)` | PO missing | Throws `NotFoundException` |
| `createReceivingOrder_poNotReleased_throwsBusinessException` | `createReceivingOrder(tenantId, request)` | PO DRAFT | Throws `BusinessException` |
| `updateReceivingOrder_success` | `updateReceivingOrder(tenantId, id, request)` | Not COMPLETED | Returns updated |
| `updateReceivingOrder_completed_throwsBusinessException` | `updateReceivingOrder(tenantId, id, request)` | COMPLETED | Throws `BusinessException` |
| `receiveLines_validQty_createsMovement` | `receiveLines(tenantId, id, lines)` | qty>0 per line | StockMovement RECEIPT created |
| `receiveLines_zeroQty_lineSkipped` | `receiveLines(tenantId, id, lines)` | qty=0 for line | Line skipped; no movement |
| `receiveLines_overQuantity_throwsBusinessException` | `receiveLines(tenantId, id, lines)` | qty > ordered qty | Throws `BusinessException` |
| `completeReceiving_success_stockPosted` | `completeReceiving(tenantId, id)` | Lines received | Status COMPLETED; stock posted |
| `completeReceiving_alreadyCompleted_throwsBusinessException` | `completeReceiving(tenantId, id)` | Already COMPLETED | Throws `BusinessException` |

---

## 3. InventoryCountService Unit Tests

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getInventoryCounts_returnsPaged` | `getInventoryCounts(tenantId, pageable)` | Multiple | Returns paged |
| `createInventoryCount_success_returnsDraft` | `createInventoryCount(tenantId, request)` | Valid location | Returns dto with DRAFT |
| `createInventoryCount_locationNotFound_throwsNotFoundException` | `createInventoryCount(tenantId, request)` | Location missing | Throws `NotFoundException` |
| `updateInventoryCount_success` | `updateInventoryCount(tenantId, id, request)` | Not COMPLETED | Returns updated |
| `updateInventoryCount_completed_throwsBusinessException` | `updateInventoryCount(tenantId, id, request)` | COMPLETED | Throws `BusinessException` |
| `recordLineCount_success` | `recordLineCount(tenantId, id, lineId, qty)` | Count IN_PROGRESS | Qty recorded |
| `recordLineCount_negativeQty_throwsValidationException` | `recordLineCount(tenantId, id, lineId, qty)` | qty < 0 | Throws `ValidationException` |
| `recordLineCount_notInProgress_throwsBusinessException` | `recordLineCount(tenantId, id, lineId, qty)` | Count is DRAFT | Throws `BusinessException` |
| `completeCount_success_createsAdjustments` | `completeCount(tenantId, id)` | Lines counted, variances exist | COMPLETED; ADJUSTMENT movements created |
| `completeCount_noLinesCounted_throwsBusinessException` | `completeCount(tenantId, id)` | No lines counted | Throws `BusinessException` |

---

## 4. InventoryPlanningService Unit Tests

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getReorderSuggestions_returnsBelowReorderPoint` | `getReorderSuggestions(tenantId)` | 2 below reorder point | Returns 2 suggestions |
| `getReorderSuggestions_allAbove_returnsEmpty` | `getReorderSuggestions(tenantId)` | All above reorder | Returns empty list |
| `performAbcAnalysis_classifiesCorrectly` | `performAbcAnalysis(tenantId)` | Top 20% by revenue = A, next 30% = B, rest = C | A/B/C correctly assigned |
| `getSlowMovingProducts_returnsNoMovementIn90Days` | `getSlowMovingProducts(tenantId, 90)` | No movement for 90 days | Returns those products |
| `getSlowMovingProducts_allActive_returnsEmpty` | `getSlowMovingProducts(tenantId, 90)` | All products have recent movement | Returns empty list |
| `getDeadStock_returnsNeverMovedProducts` | `getDeadStock(tenantId)` | Zero movement ever | Returns dead stock |
| `getDeadStock_allMoving_returnsEmpty` | `getDeadStock(tenantId)` | All have movements | Returns empty list |

---

## 5. BarcodeService Unit Tests

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `generateBarcode_success` | `generateBarcode(tenantId, productId)` | Product exists | Returns barcode bytes |
| `generateBarcode_productNotFound_throwsNotFoundException` | `generateBarcode(tenantId, productId)` | Product missing | Throws `NotFoundException` |
| `validateBarcode_validEan13_returnsTrue` | `validateBarcode("4006381333931")` | Valid EAN-13 | Returns true |
| `validateBarcode_invalidChecksum_returnsFalse` | `validateBarcode("4006381333932")` | Wrong check digit | Returns false |
| `validateBarcode_empty_returnsFalse` | `validateBarcode("")` | Empty string | Returns false |
| `validateBarcode_null_returnsFalse` | `validateBarcode(null)` | Null input | Returns false |
| `getProductByBarcode_found_returnsDto` | `getProductByBarcode(tenantId, barcode)` | Product has barcode | Returns `ProductDto` |
| `getProductByBarcode_notFound_throwsNotFoundException` | `getProductByBarcode(tenantId, barcode)` | No product | Throws `NotFoundException` |

---

## 6. SkuGeneratorService Unit Tests

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `generateSku_returnsUniqueFormattedSku` | `generateSku(tenantId)` | First call | Returns "PROD-000001" |
| `generateSequentialSku_firstCall` | `generateSequentialSku(tenantId, "PROD")` | First for prefix | Returns "PROD-000001" |
| `generateSequentialSku_secondCall` | `generateSequentialSku(tenantId, "PROD")` | Existing sequence | Returns "PROD-000002" |
| `generateVariantSku_appendsSuffix` | `generateVariantSku("SHIRT-001", "RED-M")` | Valid parent + variant | Returns "SHIRT-001-RED-M" |
| `generateSkuFromName_normalizesToSlug` | `generateSkuFromName(tenantId, "Apple iPhone")` | Mixed case with space | Returns slug+sequence |
| `skuExists_returnsTrue_whenExists` | `skuExists(tenantId, sku)` | SKU in DB | Returns true |
| `skuExists_returnsFalse_whenNotExists` | `skuExists(tenantId, sku)` | SKU not in DB | Returns false |

---

## 7. Repository Tests (`@DataJpaTest`)

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `PurchaseOrderRepository_findByVendorId` | `findByVendorId(vendorId)` | 3 POs for vendor | Returns 3 |
| `PurchaseOrderRepository_findByStatus` | `findByStatus(tenantId, RELEASED)` | Mix | Returns only RELEASED |
| `PurchaseOrderLineRepository_findByPurchaseOrderId` | `findByPurchaseOrderId(poId)` | 4 lines | Returns 4 |
| `ReceivingOrderRepository_findByPurchaseOrderId` | `findByPurchaseOrderId(poId)` | 2 receivings | Returns 2 |
| `ReceivingOrderRepository_findByStatus` | `findByStatus(tenantId, COMPLETED)` | Mix | Returns COMPLETED |
| `InventoryCountRepository_findByLocationId` | `findByLocationId(locId)` | 2 counts | Returns 2 |
| `InventoryCountRepository_findByStatus` | `findByStatus(tenantId, DRAFT)` | Mix | Returns DRAFT |

---

## 8. Integration Tests — PO, Receiving, Count Controllers

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|---|---|---|---|
| `getPurchaseOrders_returns200` | `GET /api/v1/inventory/purchase-orders` | Bearer `INVENTORY_READ` | `200 OK`; paged |
| `getPurchaseOrders_returns403` | `GET /api/v1/inventory/purchase-orders` | No permission | `403 Forbidden` |
| `createPurchaseOrder_returns201` | `POST /api/v1/inventory/purchase-orders` | Bearer `INVENTORY_WRITE`; valid | `201 Created`; status=DRAFT |
| `createPurchaseOrder_returns404_badVendor` | `POST /api/v1/inventory/purchase-orders` | Bearer `INVENTORY_WRITE` | `404 Not Found` |
| `createPurchaseOrder_returns400_emptyLines` | `POST /api/v1/inventory/purchase-orders` | Bearer `INVENTORY_WRITE` | `400 Bad Request` |
| `releasePurchaseOrder_returns200` | `PUT /api/v1/inventory/purchase-orders/{id}/release` | Bearer `INVENTORY_WRITE` | `200 OK`; status=RELEASED |
| `releasePurchaseOrder_returns422_alreadyReleased` | `PUT /api/v1/inventory/purchase-orders/{id}/release` | Bearer `INVENTORY_WRITE` | `422 Unprocessable Entity` |
| `cancelPurchaseOrder_returns200` | `PUT /api/v1/inventory/purchase-orders/{id}/cancel` | Bearer `INVENTORY_WRITE` | `200 OK`; status=CANCELLED |
| `cancelPurchaseOrder_returns422_fullyReceived` | `PUT /api/v1/inventory/purchase-orders/{id}/cancel` | Bearer `INVENTORY_WRITE` | `422 Unprocessable Entity` |
| `receivePurchaseOrder_returns200` | `POST /api/v1/inventory/purchase-orders/{id}/receive` | Bearer `INVENTORY_WRITE` | `200 OK`; stock updated |
| `receivePurchaseOrder_returns422_notReleased` | `POST /api/v1/inventory/purchase-orders/{id}/receive` | Bearer `INVENTORY_WRITE` | `422 Unprocessable Entity` |
| `getReceivingOrders_returns200` | `GET /api/v1/inventory/receiving` | Bearer `INVENTORY_READ` | `200 OK` |
| `createReceivingOrder_returns201` | `POST /api/v1/inventory/receiving` | Bearer `INVENTORY_WRITE`; RELEASED PO | `201 Created` |
| `createReceivingOrder_returns422_poNotReleased` | `POST /api/v1/inventory/receiving` | Bearer `INVENTORY_WRITE` | `422 Unprocessable Entity` |
| `receiveLines_returns200` | `POST /api/v1/inventory/receiving/{id}/receive-lines` | Bearer `INVENTORY_WRITE` | `200 OK` |
| `receiveLines_returns422_overQty` | `POST /api/v1/inventory/receiving/{id}/receive-lines` | Bearer `INVENTORY_WRITE` | `422 Unprocessable Entity` |
| `completeReceiving_returns200` | `POST /api/v1/inventory/receiving/{id}/complete` | Bearer `INVENTORY_WRITE` | `200 OK`; status=COMPLETED |
| `getInventoryCounts_returns200` | `GET /api/v1/inventory/counts` | Bearer `INVENTORY_READ` | `200 OK` |
| `createInventoryCount_returns201` | `POST /api/v1/inventory/counts` | Bearer `INVENTORY_WRITE`; valid location | `201 Created`; status=DRAFT |
| `recordLineCount_returns200` | `PUT /api/v1/inventory/counts/{id}/lines/{lineId}` | Bearer `INVENTORY_WRITE` | `200 OK` |
| `recordLineCount_returns400_negativeQty` | `PUT /api/v1/inventory/counts/{id}/lines/{lineId}` | Bearer `INVENTORY_WRITE`; qty=-1 | `400 Bad Request` |
| `completeCount_returns200` | `POST /api/v1/inventory/counts/{id}/complete` | Bearer `INVENTORY_WRITE` | `200 OK`; status=COMPLETED |

---

# Section 4a-i: POS — Transactions & Shifts — Test Plan

---

## 1. Unit Tests

### 1.1 POSTransactionService

#### getTransactions(tenantId, pageable)

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| getTransactions_returnsPaginatedResults | getTransactions(tenantId, pageable) | Repository contains multiple transactions for tenant | Returns `Page<POSTransactionDto>` with correct content, size, and total elements matching repository result |
| getTransactions_returnsEmptyPage | getTransactions(tenantId, pageable) | Repository has no transactions for the given tenantId | Returns `Page<POSTransactionDto>` with empty content; no exception thrown |
| getTransactions_pageableRespected | getTransactions(tenantId, pageable) | Pageable specifies page 1 with size 5; 10 transactions exist | Returns second page of 5 results; `Page.getNumber()` == 1 and `Page.getSize()` == 5 |

---

#### getTransaction(tenantId, transactionId)

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| getTransaction_found | getTransaction(tenantId, transactionId) | Transaction with given id exists and belongs to tenant | Returns `POSTransactionDto` with all fields correctly mapped |
| getTransaction_notFound_throwsNotFoundException | getTransaction(tenantId, transactionId) | No transaction with given id exists for tenant | Throws `NotFoundException` with message referencing transactionId |
| getTransaction_wrongTenant_throwsNotFoundException | getTransaction(tenantId, transactionId) | Transaction exists but belongs to a different tenantId | Throws `NotFoundException`; cross-tenant data is not exposed |

---

#### createTransaction(tenantId, request)

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| createTransaction_createsDraftTransaction | createTransaction(tenantId, request) | Valid request; terminal exists; an open shift is present on the terminal | Returns `POSTransactionDto` with status `DRAFT`; repository `save` called once; shiftId set on entity |
| createTransaction_terminalNotFound_throwsNotFoundException | createTransaction(tenantId, request) | Terminal referenced in request does not exist for tenant | Throws `NotFoundException` referencing terminalId; no transaction persisted |
| createTransaction_noOpenShift_throwsBusinessException | createTransaction(tenantId, request) | Terminal exists but has no open shift | Throws `BusinessException` indicating no open shift is available for the terminal |

---

#### addLine(tenantId, transactionId, request) — product found / not found / COMPLETED lock

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| addLine_productExists_lineAdded | addLine(tenantId, transactionId, request) | Product exists; transaction is in `DRAFT` status | `POSTransactionLineDto` returned; line appended to transaction; line total = qty × unit price |
| addLine_productNotFound_throwsNotFoundException | addLine(tenantId, transactionId, request) | Product referenced in request does not exist for tenant | Throws `NotFoundException` referencing productId; no line persisted |
| addLine_transactionCompleted_throwsBusinessException | addLine(tenantId, transactionId, request) | Transaction status is `COMPLETED` | Throws `BusinessException` with message "Transaction locked" |

---

#### addLine — qty = 0 validation

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| addLine_zeroQty_throwsValidationException | addLine(tenantId, transactionId, request) | Request quantity is 0 | Throws `ValidationException` indicating quantity must be greater than zero |
| addLine_negativeQty_throwsValidationException | addLine(tenantId, transactionId, request) | Request quantity is −1 | Throws `ValidationException` indicating quantity must be greater than zero |
| addLine_nullQty_throwsValidationException | addLine(tenantId, transactionId, request) | Request quantity is null | Throws `ValidationException` indicating quantity is required |

---

#### updateLine(tenantId, transactionId, lineId, request)

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| updateLine_qtyUpdated_lineTotalRecalculated | updateLine(tenantId, transactionId, lineId, request) | Valid lineId; transaction is `DRAFT`; new qty provided | Returns updated `POSTransactionLineDto`; `lineTotal` = new qty × unit price; repository updated |
| updateLine_lineNotFound_throwsNotFoundException | updateLine(tenantId, transactionId, lineId, request) | lineId does not belong to the transaction | Throws `NotFoundException` referencing lineId |
| updateLine_completedTransaction_throwsBusinessException | updateLine(tenantId, transactionId, lineId, request) | Transaction status is `COMPLETED` | Throws `BusinessException` indicating the transaction is locked and cannot be modified |

---

#### removeLine(tenantId, transactionId, lineId)

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| removeLine_lineRemoved | removeLine(tenantId, transactionId, lineId) | Transaction has multiple lines; lineId is valid | Line deleted from repository; remaining lines intact; no exception |
| removeLine_lineNotFound_throwsNotFoundException | removeLine(tenantId, transactionId, lineId) | lineId does not exist on transaction | Throws `NotFoundException` referencing lineId; no deletion occurs |
| removeLine_lastLine_throwsBusinessException | removeLine(tenantId, transactionId, lineId) | Transaction has exactly one line and its lineId matches | Throws `BusinessException` with message "Cannot remove all lines" |

---

#### addPayment(tenantId, transactionId, request)

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| addPayment_paymentAdded_exactBalance | addPayment(tenantId, transactionId, request) | Payment amount equals remaining balance on `DRAFT` transaction | Returns `POSTransactionDto` with payment recorded; change = 0 |
| addPayment_overpayment_changeCalculated | addPayment(tenantId, transactionId, request) | Payment amount exceeds remaining balance (overpayment) | Payment accepted; `change` field on response = payment − remaining balance |
| addPayment_transactionAlreadyCompleted_throwsBusinessException | addPayment(tenantId, transactionId, request) | Transaction status is `COMPLETED` | Throws `BusinessException` indicating payment cannot be added to a completed transaction |

---

#### voidTransaction(tenantId, transactionId)

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| voidTransaction_draft_statusSetToVoided | voidTransaction(tenantId, transactionId) | Transaction is in `DRAFT` status | Returns `POSTransactionDto` with status `VOIDED`; entity saved with new status |
| voidTransaction_completed_throwsBusinessException | voidTransaction(tenantId, transactionId) | Transaction status is `COMPLETED` | Throws `BusinessException` with message "Cannot void completed transaction" |
| voidTransaction_alreadyVoided_throwsBusinessException | voidTransaction(tenantId, transactionId) | Transaction status is already `VOIDED` | Throws `BusinessException` indicating transaction is already voided |

---

#### completeTransaction(tenantId, transactionId)

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| completeTransaction_fullPayment_statusCompletedAndStockDecremented | completeTransaction(tenantId, transactionId) | Total payments cover full transaction amount; lines present | Returns `POSTransactionDto` with status `COMPLETED`; inventory decremented by line quantities; entity saved |
| completeTransaction_underpayment_throwsBusinessException | completeTransaction(tenantId, transactionId) | Total payments are less than transaction total | Throws `BusinessException` with message "Insufficient payment" |
| completeTransaction_noLines_throwsBusinessException | completeTransaction(tenantId, transactionId) | Transaction has no line items | Throws `BusinessException` with message "No items" |
| completeTransaction_noPayment_throwsBusinessException | completeTransaction(tenantId, transactionId) | Transaction has lines but no payments recorded | Throws `BusinessException` with message "Insufficient payment" |

---

### 1.2 ShiftService

#### openShift(tenantId, terminalId, request)

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| openShift_noExistingOpenShift_shiftCreatedWithOpenStatus | openShift(tenantId, terminalId, request) | Terminal exists; no open shift currently on the terminal | Returns `ShiftDto` with status `OPEN`; opening cash stored; shift persisted |
| openShift_alreadyOpenShift_throwsBusinessException | openShift(tenantId, terminalId, request) | Terminal already has an existing shift in `OPEN` status | Throws `BusinessException` with message "Shift already open" |
| openShift_terminalNotFound_throwsNotFoundException | openShift(tenantId, terminalId, request) | Terminal does not exist for the given tenantId | Throws `NotFoundException` referencing terminalId; no shift created |

---

#### closeShift(tenantId, shiftId, request)

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| closeShift_openShift_statusClosedAndSummaryCalculated | closeShift(tenantId, shiftId, request) | Shift is `OPEN`; no open transactions remain | Returns `ShiftDto` with status `CLOSED`; closing cash stored; sales/cash totals computed and stored |
| closeShift_alreadyClosed_throwsBusinessException | closeShift(tenantId, shiftId, request) | Shift status is already `CLOSED` | Throws `BusinessException` indicating the shift is already closed |
| closeShift_openTransactionsExist_throwsBusinessException | closeShift(tenantId, shiftId, request) | Shift is `OPEN` but one or more transactions are still in `DRAFT` status | Throws `BusinessException` with message "Close open transactions first" |

---

#### getOpenShift(tenantId, terminalId)

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| getOpenShift_found_returnsShiftDto | getOpenShift(tenantId, terminalId) | An `OPEN` shift exists for the terminal | Returns non-empty `Optional<ShiftDto>` containing the shift data |
| getOpenShift_noOpenShift_returnsEmpty | getOpenShift(tenantId, terminalId) | No open shift exists for the terminal | Returns `Optional.empty()`; no exception thrown |
| getOpenShift_multipleTerminals_returnsCorrectShift | getOpenShift(tenantId, terminalId) | Multiple terminals exist; only one matches | Returns `Optional<ShiftDto>` for the correct terminal only |

---

#### getShiftTransactions(tenantId, shiftId, pageable)

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| getShiftTransactions_returnsTransactionsForShift | getShiftTransactions(tenantId, shiftId, pageable) | Shift exists; several transactions recorded under it | Returns `Page<POSTransactionDto>` containing only that shift's transactions |
| getShiftTransactions_shiftNotFound_throwsNotFoundException | getShiftTransactions(tenantId, shiftId, pageable) | shiftId does not exist for the tenant | Throws `NotFoundException` referencing shiftId |
| getShiftTransactions_emptyShift_returnsEmptyPage | getShiftTransactions(tenantId, shiftId, pageable) | Shift exists but has no transactions | Returns `Page<POSTransactionDto>` with empty content; no exception |

---

#### getShiftSummary(tenantId, shiftId)

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| getShiftSummary_correctTotals | getShiftSummary(tenantId, shiftId) | Shift has multiple completed transactions with mixed payment types | Returns summary DTO with correct `transactionCount`, `totalSales`, `totalCash`, and `expectedCash` = opening cash + totalCash |
| getShiftSummary_noCompletedTransactions_zeroTotals | getShiftSummary(tenantId, shiftId) | Shift exists but all transactions are `VOIDED` or `DRAFT` | Returns summary with `totalSales` = 0, `totalCash` = 0, `transactionCount` = 0 |
| getShiftSummary_shiftNotFound_throwsNotFoundException | getShiftSummary(tenantId, shiftId) | shiftId does not exist | Throws `NotFoundException` referencing shiftId |

---

#### getSalesTotal(tenantId, shiftId)

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| getSalesTotal_sumOfCompletedTransactionTotals | getSalesTotal(tenantId, shiftId) | Shift contains 3 COMPLETED transactions with totals 100, 200, 300 | Returns `BigDecimal` 600 |
| getSalesTotal_noCompletedTransactions_returnsZero | getSalesTotal(tenantId, shiftId) | Shift exists but has no COMPLETED transactions | Returns `BigDecimal` 0 (or zero-equivalent) |
| getSalesTotal_excludesVoidedTransactions | getSalesTotal(tenantId, shiftId) | Mix of COMPLETED and VOIDED transactions in shift | Returns sum of COMPLETED totals only; VOIDED totals excluded |

---

#### getCashTotal(tenantId, shiftId)

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| getCashTotal_sumOfCashPayments | getCashTotal(tenantId, shiftId) | Shift has COMPLETED transactions with CASH and CARD payments | Returns sum of CASH-type payment amounts only |
| getCashTotal_noCashPayments_returnsZero | getCashTotal(tenantId, shiftId) | All payments in shift are CARD or OTHER type | Returns `BigDecimal` 0 |
| getCashTotal_mixedPaymentsPerTransaction | getCashTotal(tenantId, shiftId) | Single transaction has split payment: partial CASH, partial CARD | Returns only the CASH portion of each transaction's payments |

---

### 1.3 POSTransactionRepository (@DataJpaTest + Testcontainers)

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| findByTerminalId_returnsOnlyThatTerminalsTransactions | findByTerminalId(terminalId) | Two terminals exist with different transactions; query by terminal A | Returns only transactions belonging to terminal A; terminal B transactions absent |
| findByTerminalId_noTransactions_returnsEmpty | findByTerminalId(terminalId) | Terminal exists but has no transactions | Returns empty list or page; no exception |
| findByTerminalId_multipleTransactions_allReturned | findByTerminalId(terminalId) | Terminal has 5 transactions in various statuses | All 5 returned regardless of status |
| findByDateRange_transactionsWithinRange | findByDateRange(tenantId, start, end) | Transactions exist before, within, and after the date range | Returns only transactions where createdAt is between start (inclusive) and end (inclusive) |
| findByDateRange_noneInRange_returnsEmpty | findByDateRange(tenantId, start, end) | All transactions have dates outside the specified range | Returns empty result set |
| findByDateRange_tenantIsolation | findByDateRange(tenantId, start, end) | Two tenants have transactions in the same date range | Returns only the querying tenant's transactions |
| sumCompletedSalesByDateRange_correctSum | sumCompletedSalesByDateRange(tenantId, start, end) | Three COMPLETED transactions within range with totals 50, 75, 100 | Returns `BigDecimal` 225 |
| sumCompletedSalesByDateRange_noCompletedSales_returnsZero | sumCompletedSalesByDateRange(tenantId, start, end) | Only DRAFT or VOIDED transactions in range | Returns 0 or null mapped to 0 |
| countCompletedSalesByDateRange_correctCount | countCompletedSalesByDateRange(tenantId, start, end) | Five COMPLETED and two VOIDED transactions in range | Returns count 5 |
| findByStatus_completedOnly | findByStatus(tenantId, COMPLETED) | Mix of DRAFT, COMPLETED, VOIDED transactions for tenant | Returns only transactions with status `COMPLETED` |
| findByStatus_noMatchingStatus_returnsEmpty | findByStatus(tenantId, COMPLETED) | No COMPLETED transactions exist for tenant | Returns empty list |
| findByStatus_tenantIsolation | findByStatus(tenantId, COMPLETED) | Both tenants have COMPLETED transactions | Returns COMPLETED transactions for the specified tenant only |

---

### 1.4 ShiftRepository (@DataJpaTest)

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| findOpenShiftsByTerminal_returnsOnlyOpenShifts | findOpenShiftsByTerminal(terminalId) | Terminal has one OPEN and one CLOSED shift | Returns list containing only the OPEN shift |
| findOpenShiftsByTerminal_noOpenShift_returnsEmpty | findOpenShiftsByTerminal(terminalId) | Terminal has only CLOSED shifts | Returns empty list |
| findOpenShiftsByTerminal_noShiftsAtAll_returnsEmpty | findOpenShiftsByTerminal(terminalId) | Terminal has never had a shift | Returns empty list |
| findByTerminalAndDateRange_dateFiltered | findByTerminalAndDateRange(terminalId, start, end) | Terminal has shifts in and out of date range | Returns only shifts whose openedAt falls within the range |
| findByTerminalAndDateRange_noMatchingDates_returnsEmpty | findByTerminalAndDateRange(terminalId, start, end) | All terminal shifts are outside date range | Returns empty list |
| sumSalesByShift_correctSum | sumSalesByShift(shiftId) | Shift has COMPLETED transactions totalling 400 | Returns `BigDecimal` 400 |
| sumSalesByShift_noCompletedTransactions_returnsZero | sumSalesByShift(shiftId) | Shift has no COMPLETED transactions | Returns 0 |
| sumCashByShift_correctSum | sumCashByShift(shiftId) | Shift transactions include CASH payments totalling 150 | Returns `BigDecimal` 150 |
| sumCashByShift_noCashPayments_returnsZero | sumCashByShift(shiftId) | Shift has only CARD payments | Returns 0 |

---

### 1.5 Mapper Tests

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| posTransactionMapper_toDto_allFieldsMapped | POSTransactionMapper.toDto(entity) | Entity has all fields populated including a non-empty lines list | Returned `POSTransactionDto` has matching id, tenantId, terminalId, shiftId, status, total, createdAt, and lines list with all line DTOs |
| posTransactionMapper_toDto_emptyLines | POSTransactionMapper.toDto(entity) | Entity has an empty lines collection | Returned DTO has empty lines list; no NullPointerException |
| posTransactionMapper_fromCreateRequest_fieldsSet | POSTransactionMapper.fromCreateRequest(request) | Valid `CreateTransactionRequest` provided | Returned entity has terminalId and any other request fields set; status is not set by mapper (set by service) |
| posTransactionLineMapper_toDto_allFieldsMapped | POSTransactionLineMapper.toDto(entity) | Line entity has all fields: id, productId, productName, qty, unitPrice, lineTotal, discount | Returned `POSTransactionLineDto` matches every field exactly |
| posTransactionLineMapper_fromAddLineRequest_fieldsSet | POSTransactionLineMapper.fromAddLineRequest(request) | Valid `AddLineRequest` with productId and qty | Returned entity has productId and qty set; lineTotal not yet calculated (service responsibility) |
| shiftMapper_toDto_allFieldsIncludingTotals | ShiftMapper.toDto(entity) | Shift entity has all fields: id, terminalId, status, openingCash, closingCash, totalSales, totalCash, openedAt, closedAt | Returned `ShiftDto` matches all fields; totals correctly mapped |
| shiftMapper_fromOpenRequest_openingCashAndTerminalSet | ShiftMapper.fromOpenRequest(request, terminalId) | `OpenShiftRequest` with openingCash; terminalId passed separately | Returned entity has terminalId and openingCash set; status not set by mapper |

---

## 2. Integration Tests

### 2.1 POSTransactionController — `/api/v1/pos/transactions`

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|-----------|-------------------|-----------------|----------------------------|
| getTransactions_paginated_200 | GET /api/v1/pos/transactions | Authenticated user with `POS_READ` permission | 200 OK; body is paginated JSON with `content`, `totalElements`, `totalPages`, `page` fields |
| getTransactions_noPermission_403 | GET /api/v1/pos/transactions | Authenticated user without `POS_READ` permission | 403 Forbidden; body contains error message |
| getTransactions_filterByStatus_200 | GET /api/v1/pos/transactions?status=COMPLETED | Authenticated user with `POS_READ` | 200 OK; all items in `content` have `status` = `COMPLETED` |
| getTransactions_filterByTerminalId_200 | GET /api/v1/pos/transactions?terminalId={id} | Authenticated user with `POS_READ` | 200 OK; all returned transactions have `terminalId` matching the filter |
| getTransactions_filterByDateRange_200 | GET /api/v1/pos/transactions?startDate=&endDate= | Authenticated user with `POS_READ` | 200 OK; all returned transactions have `createdAt` within the specified range |
| getTransaction_found_200 | GET /api/v1/pos/transactions/{id} | Authenticated user with `POS_READ` | 200 OK; body contains single `POSTransactionDto` matching the id |
| getTransaction_notFound_404 | GET /api/v1/pos/transactions/{id} | Authenticated user with `POS_READ` | 404 Not Found; body contains error detail |
| createTransaction_valid_201 | POST /api/v1/pos/transactions | Authenticated user with `POS_WRITE`; valid request body | 201 Created; body contains `POSTransactionDto` with `status` = `DRAFT`; `Location` header set |
| createTransaction_noOpenShift_422 | POST /api/v1/pos/transactions | Authenticated user with `POS_WRITE`; terminal has no open shift | 422 Unprocessable Entity; error body describes missing open shift |
| createTransaction_terminalNotFound_404 | POST /api/v1/pos/transactions | Authenticated user with `POS_WRITE`; unknown terminalId in body | 404 Not Found; error body references terminalId |
| addLine_valid_200 | PUT /api/v1/pos/transactions/{id}/lines | Authenticated user with `POS_WRITE`; valid product and DRAFT transaction | 200 OK; response body contains updated transaction with the new line |
| addLine_productNotFound_404 | PUT /api/v1/pos/transactions/{id}/lines | Authenticated user with `POS_WRITE`; unknown productId | 404 Not Found; error body references productId |
| addLine_completedTransaction_422 | PUT /api/v1/pos/transactions/{id}/lines | Authenticated user with `POS_WRITE`; transaction is COMPLETED | 422 Unprocessable Entity; error body mentions "Transaction locked" |
| updateLine_valid_200 | PUT /api/v1/pos/transactions/{id}/lines/{lineId} | Authenticated user with `POS_WRITE`; valid lineId and DRAFT transaction | 200 OK; response body contains line with updated quantity and recalculated total |
| updateLine_notFound_404 | PUT /api/v1/pos/transactions/{id}/lines/{lineId} | Authenticated user with `POS_WRITE`; unknown lineId | 404 Not Found; error body references lineId |
| removeLine_valid_204 | DELETE /api/v1/pos/transactions/{id}/lines/{lineId} | Authenticated user with `POS_WRITE`; multiple lines exist | 204 No Content; line no longer present on subsequent GET |
| removeLine_lastLine_422 | DELETE /api/v1/pos/transactions/{id}/lines/{lineId} | Authenticated user with `POS_WRITE`; lineId is the only line | 422 Unprocessable Entity; error body mentions "Cannot remove all lines" |
| addPayment_valid_200 | POST /api/v1/pos/transactions/{id}/payments | Authenticated user with `POS_WRITE`; valid payment on DRAFT transaction | 200 OK; response body contains updated transaction with payment recorded |
| addPayment_completedTransaction_422 | POST /api/v1/pos/transactions/{id}/payments | Authenticated user with `POS_WRITE`; transaction is COMPLETED | 422 Unprocessable Entity; error body explains no payment can be added |
| voidTransaction_valid_200 | PUT /api/v1/pos/transactions/{id}/void | Authenticated user with `POS_WRITE`; transaction is DRAFT | 200 OK; response body has `status` = `VOIDED` |
| voidTransaction_alreadyVoided_422 | PUT /api/v1/pos/transactions/{id}/void | Authenticated user with `POS_WRITE`; transaction already VOIDED | 422 Unprocessable Entity; error body explains transaction is already voided |
| completeTransaction_valid_200 | POST /api/v1/pos/transactions/{id}/complete | Authenticated user with `POS_WRITE`; full payment present with at least one line | 200 OK; response body has `status` = `COMPLETED` |
| completeTransaction_underpayment_422 | POST /api/v1/pos/transactions/{id}/complete | Authenticated user with `POS_WRITE`; total payments less than transaction total | 422 Unprocessable Entity; error body mentions "Insufficient payment" |
| completeTransaction_noLines_422 | POST /api/v1/pos/transactions/{id}/complete | Authenticated user with `POS_WRITE`; transaction has no lines | 422 Unprocessable Entity; error body mentions "No items" |

---

### 2.2 ShiftController — `/api/v1/pos/shifts`

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|-----------|-------------------|-----------------|----------------------------|
| getShifts_200 | GET /api/v1/pos/shifts | Authenticated user with `POS_READ` | 200 OK; body is a list (or paginated result) of `ShiftDto` objects |
| getShifts_noPermission_403 | GET /api/v1/pos/shifts | Authenticated user without `POS_READ` | 403 Forbidden; error body present |
| getCurrentShift_found_200 | GET /api/v1/pos/shifts/current?terminalId={id} | Authenticated user with `POS_READ`; terminal has an open shift | 200 OK; body contains `ShiftDto` with `status` = `OPEN` and matching `terminalId` |
| getCurrentShift_noOpenShift_404 | GET /api/v1/pos/shifts/current?terminalId={id} | Authenticated user with `POS_READ`; terminal has no open shift | 404 Not Found; error body explains no open shift found |
| openShift_valid_201 | POST /api/v1/pos/shifts/open | Authenticated user with `POS_WRITE`; terminal exists with no open shift | 201 Created; body contains `ShiftDto` with `status` = `OPEN`; `openingCash` matches request |
| openShift_alreadyOpen_422 | POST /api/v1/pos/shifts/open | Authenticated user with `POS_WRITE`; terminal already has an open shift | 422 Unprocessable Entity; error body mentions "Shift already open" |
| closeShift_valid_200 | POST /api/v1/pos/shifts/{id}/close | Authenticated user with `POS_WRITE`; shift is OPEN; no open transactions | 200 OK; body contains `ShiftDto` with `status` = `CLOSED`; totals calculated |
| closeShift_alreadyClosed_422 | POST /api/v1/pos/shifts/{id}/close | Authenticated user with `POS_WRITE`; shift is already CLOSED | 422 Unprocessable Entity; error body explains shift is already closed |
| closeShift_openTransactions_422 | POST /api/v1/pos/shifts/{id}/close | Authenticated user with `POS_WRITE`; shift has DRAFT transactions | 422 Unprocessable Entity; error body mentions "Close open transactions first" |
| getShift_found_200 | GET /api/v1/pos/shifts/{id} | Authenticated user with `POS_READ`; shift exists | 200 OK; body is `ShiftDto` matching the given shiftId |
| getShift_notFound_404 | GET /api/v1/pos/shifts/{id} | Authenticated user with `POS_READ`; unknown shiftId | 404 Not Found; error body references shiftId |
| getShiftSummary_200 | GET /api/v1/pos/shifts/{id}/summary | Authenticated user with `POS_READ`; shift exists with completed transactions | 200 OK; body contains summary object with `transactionCount`, `totalSales`, `totalCash`, `expectedCash` fields populated with correct values |

---

# Section 4a-ii: POS — Terminals, Pricing, Promotions & Coupons — Test Plan

---

## 1. POSTerminalService Unit Tests

Framework: JUnit 5 + Mockito.

### 1.1 CRUD

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getTerminals_returnsPaged` | `getTerminals(tenantId, pageable)` | Multiple terminals | Returns `Page<POSTerminalDto>` |
| `getActiveTerminals_returnsOnlyActive` | `getActiveTerminals(tenantId)` | Mix of statuses | Returns only ACTIVE terminals |
| `getTerminal_found_returnsDto` | `getTerminal(tenantId, id)` | Terminal exists | Returns `POSTerminalDto` |
| `getTerminal_notFound_throwsNotFoundException` | `getTerminal(tenantId, id)` | Missing | Throws `NotFoundException` |
| `createTerminal_success` | `createTerminal(tenantId, request)` | Valid code + name | Returns dto |
| `createTerminal_duplicateCode_throwsDuplicateResourceException` | `createTerminal(tenantId, request)` | Code exists | Throws `DuplicateResourceException` |
| `updateTerminal_success` | `updateTerminal(tenantId, id, request)` | Valid update | Returns updated dto |
| `updateTerminal_notFound_throwsNotFoundException` | `updateTerminal(tenantId, id, request)` | Missing | Throws `NotFoundException` |

### 1.2 Activation

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `activateTerminal_inactive_becomesActive` | `activateTerminal(tenantId, id)` | INACTIVE terminal | Status ACTIVE |
| `activateTerminal_alreadyActive_idempotent` | `activateTerminal(tenantId, id)` | Already ACTIVE | No error; remains ACTIVE |
| `activateTerminal_notFound_throwsNotFoundException` | `activateTerminal(tenantId, id)` | Missing | Throws `NotFoundException` |
| `deactivateTerminal_active_becomesInactive` | `deactivateTerminal(tenantId, id)` | ACTIVE; no open shift | Status INACTIVE |
| `deactivateTerminal_hasOpenShift_throwsBusinessException` | `deactivateTerminal(tenantId, id)` | Has open shift | Throws `BusinessException` "Close shift first" |
| `deactivateTerminal_notFound_throwsNotFoundException` | `deactivateTerminal(tenantId, id)` | Missing | Throws `NotFoundException` |

---

## 2. PriceListService Unit Tests

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getPriceLists_returnsPaged` | `getPriceLists(tenantId, pageable)` | Multiple | Returns paged |
| `getActivePriceLists_returnsOnlyActive` | `getActivePriceLists(tenantId)` | Mix | Returns only active |
| `getPriceList_found_returnsDto` | `getPriceList(tenantId, id)` | Exists | Returns dto |
| `getPriceList_notFound_throwsNotFoundException` | `getPriceList(tenantId, id)` | Missing | Throws `NotFoundException` |
| `createPriceList_success` | `createPriceList(tenantId, request)` | Valid | Returns dto |
| `createPriceList_duplicateName_throwsDuplicateResourceException` | `createPriceList(tenantId, request)` | Name exists | Throws `DuplicateResourceException` |
| `updatePriceList_success` | `updatePriceList(tenantId, id, request)` | Valid | Returns updated |
| `addPriceListItem_success` | `addPriceListItem(tenantId, listId, request)` | Product not in list | Item added |
| `addPriceListItem_duplicate_throwsDuplicateResourceException` | `addPriceListItem(tenantId, listId, request)` | Product already in list | Throws `DuplicateResourceException` |
| `addPriceListItem_productNotFound_throwsNotFoundException` | `addPriceListItem(tenantId, listId, request)` | Product missing | Throws `NotFoundException` |
| `removePriceListItem_success` | `removePriceListItem(tenantId, listId, itemId)` | Item exists | Item removed |
| `removePriceListItem_notFound_throwsNotFoundException` | `removePriceListItem(tenantId, listId, itemId)` | Item missing | Throws `NotFoundException` |
| `calculatePrice_productInList_returnsListPrice` | `calculatePrice(tenantId, productId, listId, qty)` | Product in list with override | Returns override price |
| `calculatePrice_productNotInList_returnsBasePrice` | `calculatePrice(tenantId, productId, listId, qty)` | Not in list | Returns base product price |
| `calculatePrice_qtyBreak_returnsLowerPrice` | `calculatePrice(tenantId, productId, listId, qty)` | Qty meets break threshold | Returns discounted tier price |

---

## 3. PricingService Unit Tests

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `calculatePrice_basePrice_returned` | `calculatePrice(tenantId, productId, null, qty)` | No price list | Returns product base price |
| `calculatePrice_withPriceListOverride` | `calculatePrice(tenantId, productId, listId, qty)` | Price list override exists | Returns override price |
| `applyDiscount_10pct_reducesSubtotal` | `applyDiscount(subtotal, 10)` | 10% on 100 | Returns 90 |
| `applyDiscount_zeroPercent_noChange` | `applyDiscount(subtotal, 0)` | 0% discount | Returns original amount |
| `applyDiscount_over100pct_throwsValidationException` | `applyDiscount(subtotal, 101)` | 101% | Throws `ValidationException` |
| `applyDiscount_negative_throwsValidationException` | `applyDiscount(subtotal, -5)` | -5% | Throws `ValidationException` |
| `applyTax_standardRate_applied` | `applyTax(tenantId, amount, productId)` | Product has standard tax | Returns amount + tax |
| `applyTax_exemptProduct_returnsZeroTax` | `applyTax(tenantId, amount, productId)` | Product tax-exempt | Returns original amount; tax=0 |
| `calculateFinalPrice_combinedCorrectly` | `calculateFinalPrice(tenantId, request)` | Price + discount + tax | Returns correct final amount |

---

## 4. PromotionService Unit Tests

### 4.1 CRUD

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getPromotions_returnsPaged` | `getPromotions(tenantId, pageable)` | Multiple | Returns paged |
| `getActivePromotions_returnsOnlyActive` | `getActivePromotions(tenantId)` | Mix | Returns active within date range |
| `createPromotion_success` | `createPromotion(tenantId, request)` | Valid | Returns dto |
| `updatePromotion_success` | `updatePromotion(tenantId, id, request)` | Valid | Returns updated |
| `updatePromotion_notFound_throwsNotFoundException` | `updatePromotion(tenantId, id, request)` | Missing | Throws `NotFoundException` |
| `deactivatePromotion_success` | `deactivatePromotion(tenantId, id)` | Active | Status INACTIVE |
| `deactivatePromotion_alreadyInactive_idempotent` | `deactivatePromotion(tenantId, id)` | Already inactive | No error |

### 4.2 Evaluation

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `evaluateConditions_allMet_returnsTrue` | `evaluatePromotionConditions(tenantId, promoId, context)` | All conditions satisfied | Returns true |
| `evaluateConditions_oneFails_returnsFalse` | `evaluatePromotionConditions(tenantId, promoId, context)` | One condition fails | Returns false |
| `evaluateConditions_minQtyMet_returnsTrue` | `evaluatePromotionConditions(tenantId, promoId, context)` | qty ≥ MIN_QTY | Returns true |
| `evaluateConditions_minQtyNotMet_returnsFalse` | `evaluatePromotionConditions(tenantId, promoId, context)` | qty < MIN_QTY | Returns false |
| `evaluateConditions_minAmountMet_returnsTrue` | `evaluatePromotionConditions(tenantId, promoId, context)` | amount ≥ MIN_AMOUNT | Returns true |
| `applyAction_discountPct_applied` | `applyPromotionAction(tenantId, promoId, transaction)` | DISCOUNT_PCT action | Transaction total reduced by % |
| `applyAction_fixedDiscount_applied` | `applyPromotionAction(tenantId, promoId, transaction)` | FIXED_DISCOUNT action | Fixed amount deducted |
| `applyAction_freeItem_addedToTransaction` | `applyPromotionAction(tenantId, promoId, transaction)` | FREE_ITEM action | Free product line added |

---

## 5. CouponService Unit Tests

### 5.1 CRUD

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getCoupons_returnsPaged` | `getCoupons(tenantId, pageable)` | Multiple | Returns paged |
| `getActiveCoupons_returnsOnlyActive` | `getActiveCoupons(tenantId)` | Mix | Returns active only |
| `createCoupon_success` | `createCoupon(tenantId, request)` | Valid | Returns dto |
| `updateCoupon_success` | `updateCoupon(tenantId, id, request)` | Valid | Returns updated |
| `updateCoupon_notFound_throwsNotFoundException` | `updateCoupon(tenantId, id, request)` | Missing | Throws `NotFoundException` |
| `deactivateCoupon_success` | `deactivateCoupon(tenantId, id)` | Active | Status INACTIVE |

### 5.2 Validation & Redemption

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `validateCoupon_valid_returnsCouponDto` | `validateCoupon(tenantId, code, customerId)` | Valid active coupon | Returns `CouponDto` |
| `validateCoupon_expired_throwsBusinessException` | `validateCoupon(tenantId, code, customerId)` | Past expiry date | Throws `BusinessException` "Coupon expired" |
| `validateCoupon_fullyRedeemed_throwsBusinessException` | `validateCoupon(tenantId, code, customerId)` | usageCount >= maxUsage | Throws `BusinessException` |
| `validateCoupon_wrongCustomer_throwsBusinessException` | `validateCoupon(tenantId, code, customerId)` | Customer-specific coupon; wrong customer | Throws `BusinessException` |
| `validateCoupon_inactive_throwsBusinessException` | `validateCoupon(tenantId, code, customerId)` | INACTIVE coupon | Throws `BusinessException` |
| `validateCoupon_notFound_throwsNotFoundException` | `validateCoupon(tenantId, code, customerId)` | Code missing | Throws `NotFoundException` |
| `redeemCoupon_success_incrementsUsageCount` | `redeemCoupon(tenantId, couponId, transactionId)` | Valid redemption | `usageCount` incremented by 1 |
| `redeemCoupon_sameTransaction_idempotent` | `redeemCoupon(tenantId, couponId, transactionId)` | Already redeemed for same txn | No double increment |

---

## 6. Repository Tests (`@DataJpaTest`)

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `POSTerminalRepository_findByCode` | `findByCode(tenantId, code)` | "TERM-01" exists | Returns Optional |
| `POSTerminalRepository_findActiveTerminals` | `findActiveTerminals(tenantId)` | Mix | Returns only ACTIVE |
| `POSTerminalRepository_findByLocationId` | `findByLocationId(locId)` | 2 terminals in location | Returns 2 |
| `CouponRepository_findActiveByCode` | `findActiveByCode(tenantId, code)` | Active code | Returns Optional |
| `CouponRepository_findActiveByCode_expired` | `findActiveByCode(tenantId, code)` | Expired coupon | Returns empty Optional |
| `CouponRedemptionRepository_findByCouponId` | `findByCouponId(couponId)` | 3 redemptions | Returns 3 |
| `PriceListRepository_findActiveByCustomer` | `findActiveByCustomer(tenantId, customerId)` | Customer-assigned list | Returns matching |
| `PromotionRepository_findActivePromotions` | `findActivePromotions(tenantId, date)` | 2 active in date range, 1 expired | Returns 2 |

---

## 7. Integration Tests

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|---|---|---|---|
| `getTerminals_returns200` | `GET /api/v1/pos/terminals` | Bearer `POS_READ` | `200 OK`; paged |
| `getTerminals_returns403` | `GET /api/v1/pos/terminals` | No permission | `403 Forbidden` |
| `createTerminal_returns201` | `POST /api/v1/pos/terminals` | Bearer `POS_WRITE`; valid | `201 Created` |
| `createTerminal_returns409_dupCode` | `POST /api/v1/pos/terminals` | Bearer `POS_WRITE`; dup | `409 Conflict` |
| `activateTerminal_returns200` | `PUT /api/v1/pos/terminals/{id}/activate` | Bearer `POS_WRITE` | `200 OK`; status=ACTIVE |
| `deactivateTerminal_returns200` | `PUT /api/v1/pos/terminals/{id}/deactivate` | Bearer `POS_WRITE`; no shift | `200 OK`; status=INACTIVE |
| `deactivateTerminal_returns422_hasShift` | `PUT /api/v1/pos/terminals/{id}/deactivate` | Bearer `POS_WRITE`; has shift | `422 Unprocessable Entity` |
| `getPriceLists_returns200` | `GET /api/v1/pos/price-lists` | Bearer `POS_READ` | `200 OK` |
| `createPriceList_returns201` | `POST /api/v1/pos/price-lists` | Bearer `POS_WRITE` | `201 Created` |
| `addPriceListItem_returns201` | `POST /api/v1/pos/price-lists/{id}/items` | Bearer `POS_WRITE` | `201 Created` |
| `addPriceListItem_returns409_dup` | `POST /api/v1/pos/price-lists/{id}/items` | Bearer `POS_WRITE`; dup | `409 Conflict` |
| `removePriceListItem_returns204` | `DELETE /api/v1/pos/price-lists/{id}/items/{itemId}` | Bearer `POS_WRITE` | `204 No Content` |
| `calculatePrice_returns200` | `POST /api/v1/pos/pricing/calculate` | Bearer `POS_READ` | `200 OK`; price field |
| `applyDiscount_returns200` | `POST /api/v1/pos/pricing/apply-discount` | Bearer `POS_READ` | `200 OK`; discounted amount |
| `applyDiscount_returns400_invalidPct` | `POST /api/v1/pos/pricing/apply-discount` | Bearer `POS_READ`; pct=101 | `400 Bad Request` |
| `getActivePromotions_returns200` | `GET /api/v1/pos/promotions/active` | Bearer `POS_READ` | `200 OK`; list |
| `evaluatePromotion_returns200_true` | `POST /api/v1/pos/promotions/{id}/evaluate` | Bearer `POS_READ`; conditions met | `200 OK`; `{"result":true}` |
| `evaluatePromotion_returns200_false` | `POST /api/v1/pos/promotions/{id}/evaluate` | Bearer `POS_READ`; not met | `200 OK`; `{"result":false}` |
| `validateCoupon_returns200_valid` | `POST /api/v1/pos/coupons/validate` | Bearer `POS_READ`; valid code | `200 OK`; coupon dto |
| `validateCoupon_returns422_expired` | `POST /api/v1/pos/coupons/validate` | Bearer `POS_READ`; expired | `422 Unprocessable Entity` |
| `redeemCoupon_returns200` | `POST /api/v1/pos/coupons/redeem` | Bearer `POS_WRITE` | `200 OK` |

---

# Section 4b: HR, Delivery & Expense Modules — Test Plan

---

## HR MODULE

### Unit Tests

#### DepartmentService

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| getDepartments_returnsDepartmentPage | getDepartments(tenantId, pageable) | Valid tenantId with existing departments | Returns a Page<DepartmentDto> containing all departments for that tenant |
| getDepartments_returnsEmptyPage | getDepartments(tenantId, pageable) | Valid tenantId with no departments | Returns an empty Page<DepartmentDto> |
| getDepartment_found | getDepartment(tenantId, deptId) | Department exists for given tenantId and deptId | Returns the matching DepartmentDto |
| getDepartment_notFound | getDepartment(tenantId, deptId) | No department exists for the given deptId under that tenant | Throws NotFoundException |
| createDepartment_success | createDepartment(tenantId, dto) | Valid dto, no name conflict | Saves entity and returns DepartmentDto with generated id |
| createDepartment_duplicateName | createDepartment(tenantId, dto) | Department name already exists for tenantId | Throws DuplicateResourceException (or BusinessException with 409 semantics) |
| updateDepartment_success | updateDepartment(tenantId, deptId, dto) | Department exists, no name conflict | Updates entity fields and returns updated DepartmentDto |
| updateDepartment_notFound | updateDepartment(tenantId, deptId, dto) | Department does not exist | Throws NotFoundException |
| updateDepartment_duplicateName | updateDepartment(tenantId, deptId, dto) | New name already used by another department | Throws DuplicateResourceException |
| deleteDepartment_success | deleteDepartment(tenantId, deptId) | Department exists and has no employees | Deletes entity without error |
| deleteDepartment_notFound | deleteDepartment(tenantId, deptId) | Department does not exist | Throws NotFoundException |
| deleteDepartment_hasEmployees | deleteDepartment(tenantId, deptId) | Department has one or more linked employees | Throws BusinessException indicating employees must be reassigned first |

#### EmployeeService

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| getEmployees_returnsPage | getEmployees(tenantId, pageable) | Valid tenantId with existing employees | Returns Page<EmployeeDto> with correct content |
| getEmployees_returnsEmptyPage | getEmployees(tenantId, pageable) | Valid tenantId with no employees | Returns an empty Page<EmployeeDto> |
| getEmployee_found | getEmployee(tenantId, employeeId) | Employee exists under the given tenant | Returns the matching EmployeeDto |
| getEmployee_notFound | getEmployee(tenantId, employeeId) | No employee with that id exists | Throws NotFoundException |
| createEmployee_success | createEmployee(tenantId, request) | Valid request, unique employee number, valid department | Saves and returns EmployeeDto |
| createEmployee_duplicateEmployeeNumber | createEmployee(tenantId, request) | Employee number already used within tenant | Throws DuplicateResourceException |
| createEmployee_invalidDepartment | createEmployee(tenantId, request) | Provided departmentId does not exist or belongs to different tenant | Throws NotFoundException or ValidationException |
| updateEmployee_success | updateEmployee(tenantId, employeeId, request) | Employee exists, valid update payload | Updates and returns updated EmployeeDto |
| updateEmployee_notFound | updateEmployee(tenantId, employeeId, request) | Employee does not exist | Throws NotFoundException |
| deleteEmployee_success | deleteEmployee(tenantId, employeeId) | Employee exists, no linked salary records | Deletes employee without error |
| deleteEmployee_notFound | deleteEmployee(tenantId, employeeId) | Employee does not exist | Throws NotFoundException |
| deleteEmployee_hasSalaryRecords | deleteEmployee(tenantId, employeeId) | Employee has one or more salary records | Throws BusinessException |
| getEmployeesByDepartment_found | getEmployeesByDepartment(tenantId, departmentId) | Department has employees | Returns non-empty List<EmployeeDto> |
| getEmployeesByDepartment_emptyList | getEmployeesByDepartment(tenantId, departmentId) | Department exists but has no employees | Returns empty List<EmployeeDto> |

#### PositionService

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| getPositions_returnsPage | getPositions(tenantId, pageable) | Valid tenantId with positions | Returns Page<PositionDto> |
| getPositions_returnsEmptyPage | getPositions(tenantId, pageable) | No positions exist for tenant | Returns empty Page<PositionDto> |
| getPosition_found | getPosition(tenantId, positionId) | Position exists | Returns matching PositionDto |
| getPosition_notFound | getPosition(tenantId, positionId) | No position with that id | Throws NotFoundException |
| createPosition_success | createPosition(tenantId, dto) | Valid dto, title unique within tenant | Saves and returns PositionDto |
| createPosition_duplicateTitle | createPosition(tenantId, dto) | Title already in use within tenant | Throws DuplicateResourceException |
| updatePosition_success | updatePosition(tenantId, positionId, dto) | Position exists, valid payload | Updates and returns PositionDto |
| updatePosition_notFound | updatePosition(tenantId, positionId, dto) | Position does not exist | Throws NotFoundException |
| deletePosition_success | deletePosition(tenantId, positionId) | Position exists and has no employees | Deletes position without error |
| deletePosition_hasEmployees | deletePosition(tenantId, positionId) | One or more employees hold this position | Throws BusinessException |

#### SalaryService

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| getSalaryRecords_returnsPage | getSalaryRecords(tenantId, pageable) | Salary records exist for tenant | Returns Page<SalaryRecordDto> |
| getSalaryRecords_returnsEmptyPage | getSalaryRecords(tenantId, pageable) | No records exist | Returns empty Page<SalaryRecordDto> |
| getSalaryRecord_found | getSalaryRecord(tenantId, recordId) | Record exists | Returns SalaryRecordDto |
| getSalaryRecord_notFound | getSalaryRecord(tenantId, recordId) | No record with that id | Throws NotFoundException |
| createSalaryRecord_success | createSalaryRecord(tenantId, request) | Valid request, employee exists, period valid | Saves and returns SalaryRecordDto |
| createSalaryRecord_employeeNotFound | createSalaryRecord(tenantId, request) | EmployeeId references non-existent employee | Throws NotFoundException |
| createSalaryRecord_invalidPeriod | createSalaryRecord(tenantId, request) | Period is malformed or in the future beyond allowed range | Throws ValidationException |
| updateSalaryRecord_success | updateSalaryRecord(tenantId, recordId, request) | Record exists, valid payload | Updates and returns updated SalaryRecordDto |
| updateSalaryRecord_notFound | updateSalaryRecord(tenantId, recordId, request) | Record does not exist | Throws NotFoundException |
| calculateNetSalary_correctResult | calculateNetSalary(grossSalary, deductions, advances) | grossSalary=5000, deductions=500, advances=200 | Returns 4300.00 |
| calculateNetSalary_clampedToZero | calculateNetSalary(grossSalary, deductions, advances) | Deductions + advances exceed grossSalary | Returns 0 (never negative) |

#### SalaryAdvanceService

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| getSalaryAdvances_returnsPage | getSalaryAdvances(tenantId, pageable) | Advances exist | Returns Page<SalaryAdvanceDto> |
| getSalaryAdvances_returnsEmptyPage | getSalaryAdvances(tenantId, pageable) | No advances | Returns empty Page<SalaryAdvanceDto> |
| getSalaryAdvance_found | getSalaryAdvance(tenantId, advanceId) | Advance exists | Returns SalaryAdvanceDto |
| getSalaryAdvance_notFound | getSalaryAdvance(tenantId, advanceId) | No advance with that id | Throws NotFoundException |
| createSalaryAdvance_success | createSalaryAdvance(tenantId, request) | Valid request, employee exists, within limit | Saves and returns SalaryAdvanceDto with PENDING status |
| createSalaryAdvance_employeeNotFound | createSalaryAdvance(tenantId, request) | Referenced employee does not exist | Throws NotFoundException |
| createSalaryAdvance_exceedsMaxLimit | createSalaryAdvance(tenantId, request) | Requested amount exceeds configured max advance limit | Throws BusinessException |
| updateSalaryAdvance_success | updateSalaryAdvance(tenantId, advanceId, request) | Advance exists and is still PENDING | Updates and returns updated SalaryAdvanceDto |
| updateSalaryAdvance_alreadyApprovedLocked | updateSalaryAdvance(tenantId, advanceId, request) | Advance is in APPROVED state | Throws BusinessException (locked record) |
| approveSalaryAdvance_success | approveSalaryAdvance(tenantId, advanceId) | Advance exists and is PENDING | Transitions status to APPROVED and returns updated dto |
| approveSalaryAdvance_alreadyApproved | approveSalaryAdvance(tenantId, advanceId) | Advance is already APPROVED | Throws BusinessException |
| approveSalaryAdvance_notFound | approveSalaryAdvance(tenantId, advanceId) | Advance does not exist | Throws NotFoundException |
| rejectSalaryAdvance_success | rejectSalaryAdvance(tenantId, advanceId, reason) | Advance exists and is PENDING | Transitions status to REJECTED, stores reason, returns dto |
| rejectSalaryAdvance_alreadyRejected | rejectSalaryAdvance(tenantId, advanceId, reason) | Advance is already REJECTED | Throws BusinessException |
| rejectSalaryAdvance_notFound | rejectSalaryAdvance(tenantId, advanceId, reason) | Advance does not exist | Throws NotFoundException |

#### Repository Tests (@DataJpaTest + Testcontainers)

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| departmentRepo_findByTenantId | DepartmentRepository.findByTenantId(tenantId, pageable) | Two tenants have departments; query with tenant A | Returns only departments belonging to tenant A |
| departmentRepo_countByTenantId | DepartmentRepository.countByTenantId(tenantId) | Tenant has 3 departments | Returns 3 |
| employeeRepo_findByDepartmentId | EmployeeRepository.findByDepartmentId(departmentId) | Department has 2 employees | Returns list of 2 employees |
| employeeRepo_findActiveEmployees | EmployeeRepository.findActiveEmployees(tenantId) | Mix of active and inactive employees | Returns only employees with ACTIVE status |
| employeeRepo_searchByName | EmployeeRepository.searchByName(tenantId, "ali") | Employees named "Ali Valiyev" and "Alisher" exist | Returns both matching employees case-insensitively |
| positionRepo_findActivePositions | PositionRepository.findActivePositions(tenantId) | Mix of active and archived positions | Returns only active positions |
| salaryRecordRepo_findByEmployeeId | SalaryRecordRepository.findByEmployeeId(employeeId) | Employee has 3 salary records | Returns all 3 records |
| salaryRecordRepo_findByDateRange | SalaryRecordRepository.findByDateRange(tenantId, start, end) | Records span multiple months; query for a specific range | Returns only records falling within the date range |
| salaryAdvanceRepo_findByEmployeeId | SalaryAdvanceRepository.findByEmployeeId(employeeId) | Employee has 2 advances | Returns both advances |
| salaryAdvanceRepo_findPendingAdvances | SalaryAdvanceRepository.findPendingAdvances(tenantId) | Mix of PENDING, APPROVED, REJECTED advances | Returns only PENDING advances |

#### Mapper Tests

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| departmentMapper_toDto | DepartmentMapper.toDto(entity) | Valid Department entity | Returns DepartmentDto with all fields correctly mapped |
| departmentMapper_fromDto | DepartmentMapper.fromDto(dto) | Valid DepartmentDto | Returns Department entity with all fields correctly mapped |
| employeeMapper_toDto | EmployeeMapper.toDto(entity) | Valid Employee entity with department and position | Returns EmployeeDto with nested references |
| employeeMapper_fromCreateRequest | EmployeeMapper.fromCreateRequest(request) | Valid CreateEmployeeRequest | Returns Employee entity with fields set from request |
| employeeMapper_fromUpdateRequest | EmployeeMapper.fromUpdateRequest(entity, request) | Existing entity, partial update request | Merges updated fields onto existing entity |
| positionMapper_toDto | PositionMapper.toDto(entity) | Valid Position entity | Returns PositionDto with all fields mapped |
| positionMapper_fromDto | PositionMapper.fromDto(dto) | Valid PositionDto | Returns Position entity with all fields mapped |
| salaryRecordMapper_toDto | SalaryRecordMapper.toDto(entity) | Valid SalaryRecord entity | Returns SalaryRecordDto with gross, deductions, net fields |
| salaryRecordMapper_fromCreateRequest | SalaryRecordMapper.fromCreateRequest(request) | Valid CreateSalaryRecordRequest | Returns SalaryRecord entity populated from request |
| salaryAdvanceMapper_toDto | SalaryAdvanceMapper.toDto(entity) | Valid SalaryAdvance entity | Returns SalaryAdvanceDto with status and amount |
| salaryAdvanceMapper_fromCreateRequest | SalaryAdvanceMapper.fromCreateRequest(request) | Valid CreateSalaryAdvanceRequest | Returns SalaryAdvance entity with PENDING status |

---

### Integration Tests

#### DepartmentController — /api/v1/hr/departments

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|-----------|-------------------|----------------|----------------------------|
| listDepartments_ok | GET /api/v1/hr/departments | Bearer token with HR_READ | 200 OK — paginated JSON body with `content`, `totalElements`, `totalPages` |
| listDepartments_emptyPage | GET /api/v1/hr/departments?page=0&size=10 | Bearer token with HR_READ, no departments seeded | 200 OK — `content: []`, `totalElements: 0` |
| getDepartment_found | GET /api/v1/hr/departments/{id} | Bearer token with HR_READ | 200 OK — JSON body with department id, name, tenantId |
| getDepartment_notFound | GET /api/v1/hr/departments/{id} | Bearer token with HR_READ | 404 Not Found — error body with message |
| createDepartment_valid | POST /api/v1/hr/departments | Bearer token with HR_WRITE | 201 Created — JSON body with new department id and name |
| createDepartment_duplicateName | POST /api/v1/hr/departments | Bearer token with HR_WRITE, duplicate name in body | 409 Conflict — error body indicating duplicate name |
| updateDepartment_success | PUT /api/v1/hr/departments/{id} | Bearer token with HR_WRITE | 200 OK — JSON body with updated department |
| updateDepartment_notFound | PUT /api/v1/hr/departments/{id} | Bearer token with HR_WRITE | 404 Not Found |
| deleteDepartment_success | DELETE /api/v1/hr/departments/{id} | Bearer token with HR_WRITE | 204 No Content |
| deleteDepartment_hasEmployees | DELETE /api/v1/hr/departments/{id} | Bearer token with HR_WRITE, department has employees | 422 Unprocessable Entity — error body explaining constraint |
| listDepartments_forbidden | GET /api/v1/hr/departments | Bearer token without HR_READ | 403 Forbidden |
| createDepartment_forbidden | POST /api/v1/hr/departments | Bearer token without HR_WRITE | 403 Forbidden |
| updateDepartment_forbidden | PUT /api/v1/hr/departments/{id} | Bearer token without HR_WRITE | 403 Forbidden |
| deleteDepartment_forbidden | DELETE /api/v1/hr/departments/{id} | Bearer token without HR_WRITE | 403 Forbidden |

#### EmployeeController — /api/v1/hr/employees

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|-----------|-------------------|----------------|----------------------------|
| listEmployees_paginated | GET /api/v1/hr/employees | Bearer token with HR_READ | 200 OK — paginated body with employee list |
| listEmployees_searchByName | GET /api/v1/hr/employees?search=ali | Bearer token with HR_READ | 200 OK — filtered list of employees whose name contains "ali" |
| getEmployee_found | GET /api/v1/hr/employees/{id} | Bearer token with HR_READ | 200 OK — JSON with full employee details |
| getEmployee_notFound | GET /api/v1/hr/employees/{id} | Bearer token with HR_READ | 404 Not Found |
| createEmployee_valid | POST /api/v1/hr/employees | Bearer token with HR_WRITE | 201 Created — JSON with new employee including generated id |
| createEmployee_duplicateNumber | POST /api/v1/hr/employees | Bearer token with HR_WRITE, duplicate employee number | 409 Conflict |
| createEmployee_invalidDepartment | POST /api/v1/hr/employees | Bearer token with HR_WRITE, nonexistent departmentId | 400 Bad Request — validation error body |
| updateEmployee_success | PUT /api/v1/hr/employees/{id} | Bearer token with HR_WRITE | 200 OK — updated employee body |
| updateEmployee_notFound | PUT /api/v1/hr/employees/{id} | Bearer token with HR_WRITE | 404 Not Found |
| deleteEmployee_success | DELETE /api/v1/hr/employees/{id} | Bearer token with HR_WRITE | 204 No Content |
| deleteEmployee_hasSalaryRecords | DELETE /api/v1/hr/employees/{id} | Bearer token with HR_WRITE, employee has salary records | 422 Unprocessable Entity |
| listEmployeesByDepartment_withResults | GET /api/v1/hr/employees/by-department/{deptId} | Bearer token with HR_READ | 200 OK — non-empty list of employees |
| listEmployeesByDepartment_emptyList | GET /api/v1/hr/employees/by-department/{deptId} | Bearer token with HR_READ, no employees in dept | 200 OK — empty list `[]` |

#### PositionController — /api/v1/hr/positions

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|-----------|-------------------|----------------|----------------------------|
| listPositions_ok | GET /api/v1/hr/positions | Bearer token with HR_READ | 200 OK — paginated position list |
| listPositions_emptyPage | GET /api/v1/hr/positions | Bearer token with HR_READ, no positions seeded | 200 OK — `content: []` |
| getPosition_found | GET /api/v1/hr/positions/{id} | Bearer token with HR_READ | 200 OK — position dto body |
| getPosition_notFound | GET /api/v1/hr/positions/{id} | Bearer token with HR_READ | 404 Not Found |
| createPosition_valid | POST /api/v1/hr/positions | Bearer token with HR_WRITE | 201 Created — new position body |
| createPosition_duplicateTitle | POST /api/v1/hr/positions | Bearer token with HR_WRITE, title already exists | 409 Conflict |
| updatePosition_success | PUT /api/v1/hr/positions/{id} | Bearer token with HR_WRITE | 200 OK — updated position body |
| updatePosition_notFound | PUT /api/v1/hr/positions/{id} | Bearer token with HR_WRITE | 404 Not Found |
| deletePosition_success | DELETE /api/v1/hr/positions/{id} | Bearer token with HR_WRITE, no employees assigned | 204 No Content |
| deletePosition_hasEmployees | DELETE /api/v1/hr/positions/{id} | Bearer token with HR_WRITE, position has employees | 422 Unprocessable Entity |
| listPositions_forbidden | GET /api/v1/hr/positions | No token or wrong permission | 403 Forbidden |

#### SalaryController — /api/v1/hr/salary-records

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|-----------|-------------------|----------------|----------------------------|
| listSalaryRecords_ok | GET /api/v1/hr/salary-records | Bearer token with HR_READ | 200 OK — paginated salary record list |
| listSalaryRecords_emptyPage | GET /api/v1/hr/salary-records | Bearer token with HR_READ, no records | 200 OK — empty page |
| getSalaryRecord_found | GET /api/v1/hr/salary-records/{id} | Bearer token with HR_READ | 200 OK — salary record dto |
| getSalaryRecord_notFound | GET /api/v1/hr/salary-records/{id} | Bearer token with HR_READ | 404 Not Found |
| createSalaryRecord_success | POST /api/v1/hr/salary-records | Bearer token with HR_WRITE, valid employee | 201 Created — new salary record body |
| createSalaryRecord_employeeNotFound | POST /api/v1/hr/salary-records | Bearer token with HR_WRITE, invalid employeeId | 404 Not Found — error body |
| updateSalaryRecord_success | PUT /api/v1/hr/salary-records/{id} | Bearer token with HR_WRITE | 200 OK — updated salary record |
| calculatePayroll_ok | POST /api/v1/hr/salary-records/calculate | Bearer token with HR_READ, valid gross/deductions/advances | 200 OK — JSON with calculated net salary |

#### SalaryAdvanceController — /api/v1/hr/salary-advances

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|-----------|-------------------|----------------|----------------------------|
| listSalaryAdvances_ok | GET /api/v1/hr/salary-advances | Bearer token with HR_READ | 200 OK — paginated advance list |
| listSalaryAdvances_emptyPage | GET /api/v1/hr/salary-advances | Bearer token with HR_READ, no advances | 200 OK — empty page |
| getSalaryAdvance_found | GET /api/v1/hr/salary-advances/{id} | Bearer token with HR_READ | 200 OK — advance dto |
| getSalaryAdvance_notFound | GET /api/v1/hr/salary-advances/{id} | Bearer token with HR_READ | 404 Not Found |
| createSalaryAdvance_success | POST /api/v1/hr/salary-advances | Bearer token with HR_WRITE, valid employee and amount | 201 Created — advance dto with PENDING status |
| createSalaryAdvance_employeeNotFound | POST /api/v1/hr/salary-advances | Bearer token with HR_WRITE, invalid employeeId | 404 Not Found |
| createSalaryAdvance_exceedsLimit | POST /api/v1/hr/salary-advances | Bearer token with HR_WRITE, amount > max limit | 422 Unprocessable Entity |
| updateSalaryAdvance_success | PUT /api/v1/hr/salary-advances/{id} | Bearer token with HR_WRITE, advance is PENDING | 200 OK — updated advance dto |
| updateSalaryAdvance_locked | PUT /api/v1/hr/salary-advances/{id} | Bearer token with HR_WRITE, advance is APPROVED | 422 Unprocessable Entity — locked record message |
| approveSalaryAdvance_success | PUT /api/v1/hr/salary-advances/{id}/approve | Bearer token with HR_WRITE, advance is PENDING | 200 OK — advance dto with APPROVED status |
| approveSalaryAdvance_alreadyApproved | PUT /api/v1/hr/salary-advances/{id}/approve | Bearer token with HR_WRITE, advance already APPROVED | 422 Unprocessable Entity |
| rejectSalaryAdvance_success | PUT /api/v1/hr/salary-advances/{id}/reject | Bearer token with HR_WRITE, advance is PENDING, reason in body | 200 OK — advance dto with REJECTED status and reason |
| rejectSalaryAdvance_alreadyRejected | PUT /api/v1/hr/salary-advances/{id}/reject | Bearer token with HR_WRITE, advance already REJECTED | 422 Unprocessable Entity |

---

## DELIVERY MODULE

### Unit Tests

#### DeliveryRegionService

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| getRegions_returnsPage | getRegions(tenantId, pageable) | Regions exist for tenant | Returns Page<DeliveryRegionDto> with all regions |
| getRegions_returnsEmptyPage | getRegions(tenantId, pageable) | No regions for tenant | Returns empty Page<DeliveryRegionDto> |
| getRegion_found | getRegion(tenantId, regionId) | Region exists | Returns DeliveryRegionDto |
| getRegion_notFound | getRegion(tenantId, regionId) | No region with that id | Throws NotFoundException |
| createRegion_success | createRegion(tenantId, dto) | Valid dto, name unique | Saves and returns DeliveryRegionDto |
| createRegion_duplicateName | createRegion(tenantId, dto) | Name already used in tenant | Throws DuplicateResourceException |
| updateRegion_success | updateRegion(tenantId, regionId, dto) | Region exists, valid payload | Updates and returns updated DeliveryRegionDto |
| updateRegion_notFound | updateRegion(tenantId, regionId, dto) | Region does not exist | Throws NotFoundException |
| deleteRegion_success | deleteRegion(tenantId, regionId) | Region exists and has no villages | Deletes without error |
| deleteRegion_hasVillages | deleteRegion(tenantId, regionId) | Region has associated villages | Throws BusinessException |
| getActiveRegions_returnsOnlyActive | getActiveRegions(tenantId) | Mix of active and inactive regions | Returns only regions with ACTIVE status |

#### DeliveryVillageService

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| getVillages_returnsPage | getVillages(tenantId, pageable) | Villages exist | Returns Page<DeliveryVillageDto> |
| getVillages_returnsEmptyPage | getVillages(tenantId, pageable) | No villages | Returns empty Page<DeliveryVillageDto> |
| getVillage_found | getVillage(tenantId, villageId) | Village exists | Returns DeliveryVillageDto |
| getVillage_notFound | getVillage(tenantId, villageId) | No village with that id | Throws NotFoundException |
| createVillage_success | createVillage(tenantId, dto) | Valid dto, unique name within region, valid regionId | Saves and returns DeliveryVillageDto |
| createVillage_duplicateNameInRegion | createVillage(tenantId, dto) | Village name already exists in that region | Throws DuplicateResourceException |
| createVillage_invalidRegionId | createVillage(tenantId, dto) | regionId does not exist or belongs to another tenant | Throws NotFoundException |
| updateVillage_success | updateVillage(tenantId, villageId, dto) | Village exists, valid payload | Updates and returns DeliveryVillageDto |
| updateVillage_notFound | updateVillage(tenantId, villageId, dto) | Village does not exist | Throws NotFoundException |
| deleteVillage_success | deleteVillage(tenantId, villageId) | Village exists | Deletes without error |
| deleteVillage_notFound | deleteVillage(tenantId, villageId) | Village does not exist | Throws NotFoundException |
| getActiveVillages_returnsOnlyActive | getActiveVillages(tenantId) | Mix of active and inactive villages | Returns only villages with ACTIVE status |

#### Repository Tests

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| deliveryRegionRepo_findByTenantId | DeliveryRegionRepository.findByTenantId(tenantId, pageable) | Two tenants have regions | Returns only regions for the queried tenant |
| deliveryRegionRepo_findActiveByTenantId | DeliveryRegionRepository.findActiveByTenantId(tenantId) | Tenant has 3 active and 2 inactive regions | Returns only 3 active regions |
| deliveryVillageRepo_findByRegionId | DeliveryVillageRepository.findByRegionId(regionId) | Region has 4 villages | Returns all 4 villages |
| deliveryVillageRepo_findActiveByRegionId | DeliveryVillageRepository.findActiveByRegionId(regionId) | Region has 4 villages, 2 active | Returns only 2 active villages |

#### Mapper Tests

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| deliveryRegionMapper_toDto | DeliveryRegionMapper.toDto(entity) | Valid DeliveryRegion entity | Returns DeliveryRegionDto with id, name, status, tenantId |
| deliveryRegionMapper_fromDto | DeliveryRegionMapper.fromDto(dto) | Valid DeliveryRegionDto | Returns DeliveryRegion entity with all fields mapped |
| deliveryVillageMapper_toDto | DeliveryVillageMapper.toDto(entity) | Valid DeliveryVillage entity with region reference | Returns DeliveryVillageDto including regionId |
| deliveryVillageMapper_fromDto | DeliveryVillageMapper.fromDto(dto) | Valid DeliveryVillageDto | Returns DeliveryVillage entity with all fields mapped |

---

### Integration Tests

#### DeliveryRegionController — /api/v1/delivery/regions

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|-----------|-------------------|----------------|----------------------------|
| listRegions_ok | GET /api/v1/delivery/regions | Bearer token with DELIVERY_READ | 200 OK — paginated region list |
| listRegions_emptyPage | GET /api/v1/delivery/regions | Bearer token with DELIVERY_READ, no regions | 200 OK — empty page |
| listActiveRegions_ok | GET /api/v1/delivery/regions/active | Bearer token with DELIVERY_READ | 200 OK — list of active regions only |
| getRegion_found | GET /api/v1/delivery/regions/{id} | Bearer token with DELIVERY_READ | 200 OK — region dto body |
| getRegion_notFound | GET /api/v1/delivery/regions/{id} | Bearer token with DELIVERY_READ | 404 Not Found |
| createRegion_success | POST /api/v1/delivery/regions | Bearer token with DELIVERY_WRITE | 201 Created — new region dto |
| createRegion_duplicateName | POST /api/v1/delivery/regions | Bearer token with DELIVERY_WRITE, duplicate name | 409 Conflict |
| updateRegion_success | PUT /api/v1/delivery/regions/{id} | Bearer token with DELIVERY_WRITE | 200 OK — updated region dto |
| updateRegion_notFound | PUT /api/v1/delivery/regions/{id} | Bearer token with DELIVERY_WRITE | 404 Not Found |
| deleteRegion_success | DELETE /api/v1/delivery/regions/{id} | Bearer token with DELIVERY_WRITE, no villages | 204 No Content |
| deleteRegion_hasVillages | DELETE /api/v1/delivery/regions/{id} | Bearer token with DELIVERY_WRITE, region has villages | 422 Unprocessable Entity |
| listRegions_forbidden | GET /api/v1/delivery/regions | No token or wrong permission | 403 Forbidden |
| createRegion_forbidden | POST /api/v1/delivery/regions | Bearer token without DELIVERY_WRITE | 403 Forbidden |
| updateRegion_forbidden | PUT /api/v1/delivery/regions/{id} | Bearer token without DELIVERY_WRITE | 403 Forbidden |
| deleteRegion_forbidden | DELETE /api/v1/delivery/regions/{id} | Bearer token without DELIVERY_WRITE | 403 Forbidden |

#### DeliveryVillageController — /api/v1/delivery/villages

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|-----------|-------------------|----------------|----------------------------|
| listVillages_ok | GET /api/v1/delivery/villages | Bearer token with DELIVERY_READ | 200 OK — paginated village list |
| listVillages_emptyPage | GET /api/v1/delivery/villages | Bearer token with DELIVERY_READ, no villages | 200 OK — empty page |
| listActiveVillages_ok | GET /api/v1/delivery/villages/active | Bearer token with DELIVERY_READ | 200 OK — list of active villages only |
| getVillage_found | GET /api/v1/delivery/villages/{id} | Bearer token with DELIVERY_READ | 200 OK — village dto body |
| getVillage_notFound | GET /api/v1/delivery/villages/{id} | Bearer token with DELIVERY_READ | 404 Not Found |
| createVillage_success | POST /api/v1/delivery/villages | Bearer token with DELIVERY_WRITE, valid regionId | 201 Created — new village dto |
| createVillage_invalidRegion | POST /api/v1/delivery/villages | Bearer token with DELIVERY_WRITE, nonexistent regionId | 400 Bad Request — validation error body |
| updateVillage_success | PUT /api/v1/delivery/villages/{id} | Bearer token with DELIVERY_WRITE | 200 OK — updated village dto |
| updateVillage_notFound | PUT /api/v1/delivery/villages/{id} | Bearer token with DELIVERY_WRITE | 404 Not Found |
| deleteVillage_success | DELETE /api/v1/delivery/villages/{id} | Bearer token with DELIVERY_WRITE | 204 No Content |
| deleteVillage_notFound | DELETE /api/v1/delivery/villages/{id} | Bearer token with DELIVERY_WRITE | 404 Not Found |

---

## EXPENSE MODULE

### Unit Tests

#### ExpenseRecordService

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| getExpenses_returnsPage | getExpenses(tenantId, pageable) | Expense records exist | Returns Page<ExpenseRecordDto> |
| getExpenses_returnsEmptyPage | getExpenses(tenantId, pageable) | No expense records | Returns empty Page<ExpenseRecordDto> |
| getExpense_found | getExpense(tenantId, expenseId) | Expense exists | Returns ExpenseRecordDto |
| getExpense_notFound | getExpense(tenantId, expenseId) | No expense with that id | Throws NotFoundException |
| createExpense_success | createExpense(tenantId, request) | Valid request, positive amount, valid category | Saves and returns ExpenseRecordDto |
| createExpense_invalidAmountNegative | createExpense(tenantId, request) | Amount is negative | Throws ValidationException |
| createExpense_invalidCategory | createExpense(tenantId, request) | Category value does not match any known enum/entity | Throws ValidationException |
| updateExpense_success | updateExpense(tenantId, expenseId, request) | Expense exists, valid payload | Updates and returns updated ExpenseRecordDto |
| updateExpense_notFound | updateExpense(tenantId, expenseId, request) | Expense does not exist | Throws NotFoundException |
| deleteExpense_success | deleteExpense(tenantId, expenseId) | Expense exists | Deletes without error |
| deleteExpense_notFound | deleteExpense(tenantId, expenseId) | Expense does not exist | Throws NotFoundException |

#### ExpenseRecordRepository

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| expenseRepo_findByTenantId | findByTenantId(tenantId, pageable) | Two tenants have expenses; query tenant A | Returns only expenses for tenant A |
| expenseRepo_findByTenantIdAndDateRange | findByTenantIdAndDateRange(tenantId, start, end) | Expenses exist outside and inside date range | Returns only expenses whose date falls within [start, end] |
| expenseRepo_findByCategory | findByCategory(tenantId, category) | Expenses in multiple categories | Returns only expenses matching the given category |
| expenseRepo_sumByTenantIdAndDateRange | sumByTenantIdAndDateRange(tenantId, start, end) | Three expenses totalling 1500.00 within range | Returns BigDecimal sum of 1500.00 |

---

### Integration Tests

#### ExpenseRecordController — /api/v1/expenses

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|-----------|-------------------|----------------|----------------------------|
| listExpenses_ok | GET /api/v1/expenses | Bearer token with EXPENSE_READ | 200 OK — paginated expense list |
| listExpenses_emptyPage | GET /api/v1/expenses | Bearer token with EXPENSE_READ, no records | 200 OK — empty page |
| listExpenses_dateRangeFilter | GET /api/v1/expenses?startDate=2025-01-01&endDate=2025-03-31 | Bearer token with EXPENSE_READ | 200 OK — list filtered to the given date range |
| getExpense_found | GET /api/v1/expenses/{id} | Bearer token with EXPENSE_READ | 200 OK — expense dto body |
| getExpense_notFound | GET /api/v1/expenses/{id} | Bearer token with EXPENSE_READ | 404 Not Found |
| createExpense_success | POST /api/v1/expenses | Bearer token with EXPENSE_WRITE, valid positive amount | 201 Created — new expense dto |
| createExpense_negativeAmount | POST /api/v1/expenses | Bearer token with EXPENSE_WRITE, negative amount in body | 400 Bad Request — validation error body |
| updateExpense_success | PUT /api/v1/expenses/{id} | Bearer token with EXPENSE_WRITE | 200 OK — updated expense dto |
| updateExpense_notFound | PUT /api/v1/expenses/{id} | Bearer token with EXPENSE_WRITE | 404 Not Found |
| deleteExpense_success | DELETE /api/v1/expenses/{id} | Bearer token with EXPENSE_WRITE | 204 No Content |
| deleteExpense_notFound | DELETE /api/v1/expenses/{id} | Bearer token with EXPENSE_WRITE | 404 Not Found |
| listExpenses_forbidden | GET /api/v1/expenses | No token or wrong permission | 403 Forbidden |
| createExpense_forbidden | POST /api/v1/expenses | Bearer token without EXPENSE_WRITE | 403 Forbidden |
| updateExpense_forbidden | PUT /api/v1/expenses/{id} | Bearer token without EXPENSE_WRITE | 403 Forbidden |
| deleteExpense_forbidden | DELETE /api/v1/expenses/{id} | Bearer token without EXPENSE_WRITE | 403 Forbidden |

---

# Section 5a: Mobile Module — Test Plan

---

## 1. MobileAuthService Unit Tests

Framework: JUnit 5 + Mockito.

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `mobileLogin_validCredentials_returnsTokenPair` | `mobileLogin(request)` | Valid username + password | Returns `MobileAuthResponse` with accessToken + refreshToken |
| `mobileLogin_invalidPassword_throwsAuthException` | `mobileLogin(request)` | Wrong password | Throws `AuthenticationException` |
| `mobileLogin_userNotFound_throwsAuthException` | `mobileLogin(request)` | Username missing | Throws `AuthenticationException` |
| `mobileLogin_inactiveUser_throwsAuthException` | `mobileLogin(request)` | User INACTIVE | Throws `AuthenticationException` "Account disabled" |
| `mobileRefreshToken_valid_returnsNewAccessToken` | `mobileRefreshToken(refreshToken)` | Valid non-expired refresh token | Returns new accessToken |
| `mobileRefreshToken_expired_throwsAuthException` | `mobileRefreshToken(refreshToken)` | Expired refresh token | Throws `AuthenticationException` "Token expired" |
| `mobileRefreshToken_invalid_throwsAuthException` | `mobileRefreshToken(refreshToken)` | Tampered token | Throws `AuthenticationException` |
| `mobileLogout_success_invalidatesToken` | `mobileLogout(refreshToken)` | Valid token | Token blacklisted; future refresh fails |

---

## 2. MobileProductService Unit Tests

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getProducts_returnsPaged` | `getProducts(tenantId, pageable)` | Multiple active products | Returns paged `MobileProductDto` |
| `getProduct_found_returnsDto` | `getProduct(tenantId, id)` | Product exists | Returns dto with price, stock info |
| `getProduct_notFound_throwsNotFoundException` | `getProduct(tenantId, id)` | Missing | Throws `NotFoundException` |
| `searchProducts_byName_returnsMatches` | `searchProducts(tenantId, query, pageable)` | 3 products match | Returns 3 |
| `searchProducts_byBarcode_returnsMatch` | `searchProducts(tenantId, barcode, pageable)` | Exact barcode | Returns 1 |
| `getProductsByCategory_returnsFiltered` | `getProductsByCategory(tenantId, catId, pageable)` | 5 in category | Returns 5 |
| `getFeaturedProducts_returnsMarkedProducts` | `getFeaturedProducts(tenantId)` | 3 marked featured | Returns 3 |
| `getProductStock_returnsAvailableQty` | `getProductStock(tenantId, productId, locationId)` | Stock exists | Returns available quantity |
| `scanBarcode_returnsProductDto` | `scanBarcode(tenantId, barcode)` | Valid barcode | Returns `MobileProductDto` |
| `scanBarcode_notFound_throwsNotFoundException` | `scanBarcode(tenantId, barcode)` | Unknown barcode | Throws `NotFoundException` |

---

## 3. MobileCartService Unit Tests

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getCart_returnsCurrentCart` | `getCart(tenantId, userId)` | Cart exists in session/cache | Returns `MobileCartDto` with lines |
| `getCart_empty_returnsEmptyCart` | `getCart(tenantId, userId)` | No cart | Returns empty cart dto |
| `addItem_newProduct_lineCreated` | `addItem(tenantId, userId, request)` | Product not yet in cart | New line added; qty = requested |
| `addItem_existingProduct_qtyIncremented` | `addItem(tenantId, userId, request)` | Product already in cart | Qty incremented; no duplicate line |
| `addItem_insufficientStock_throwsBusinessException` | `addItem(tenantId, userId, request)` | Available qty < requested | Throws `BusinessException` "Insufficient stock" |
| `addItem_productNotFound_throwsNotFoundException` | `addItem(tenantId, userId, request)` | Product missing | Throws `NotFoundException` |
| `updateItemQty_success` | `updateItemQty(tenantId, userId, lineId, qty)` | Valid qty | Line updated; totals recalculated |
| `updateItemQty_zero_removesLine` | `updateItemQty(tenantId, userId, lineId, 0)` | qty = 0 | Line removed from cart |
| `removeItem_success` | `removeItem(tenantId, userId, lineId)` | Line exists | Line removed |
| `removeItem_notFound_throwsNotFoundException` | `removeItem(tenantId, userId, lineId)` | Line missing | Throws `NotFoundException` |
| `clearCart_success` | `clearCart(tenantId, userId)` | Cart has items | All lines removed |
| `applyCartCoupon_valid_discountApplied` | `applyCartCoupon(tenantId, userId, code)` | Valid coupon | Discount reflected in cart total |
| `applyCartCoupon_invalid_throwsBusinessException` | `applyCartCoupon(tenantId, userId, code)` | Expired coupon | Throws `BusinessException` |
| `checkoutCart_success_createsTransaction` | `checkoutCart(tenantId, userId, request)` | Sufficient payment | Creates `POSTransaction`; cart cleared |
| `checkoutCart_underpayment_throwsBusinessException` | `checkoutCart(tenantId, userId, request)` | Payment < total | Throws `BusinessException` |

---

## 4. MobileSyncService Unit Tests

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `syncProducts_returnsChangedSince` | `syncProducts(tenantId, lastSync)` | 5 products changed after lastSync | Returns list of 5 |
| `syncProducts_nothingChanged_returnsEmpty` | `syncProducts(tenantId, lastSync)` | No changes since lastSync | Returns empty list |
| `syncCategories_returnsChangedSince` | `syncCategories(tenantId, lastSync)` | 2 categories updated | Returns 2 |
| `syncPriceLists_returnsActiveLists` | `syncPriceLists(tenantId, lastSync)` | Changed price lists | Returns updated lists |
| `getSyncManifest_returnsAllCounts` | `getSyncManifest(tenantId)` | Products, categories, prices | Returns counts per resource type |

---

## 5. MobileReceivingService Unit Tests

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getPendingReceivingOrders_returnsList` | `getPendingReceivingOrders(tenantId)` | 3 pending | Returns list of 3 |
| `scanReceivingItem_validBarcode_recordsQty` | `scanReceivingItem(tenantId, orderId, barcode, qty)` | Barcode matches a PO line | Qty recorded; line updated |
| `scanReceivingItem_barcodeNotInOrder_throwsBusinessException` | `scanReceivingItem(tenantId, orderId, barcode, qty)` | Barcode not on PO | Throws `BusinessException` |
| `completeReceiving_success` | `completeReceiving(tenantId, orderId)` | All lines scanned | Status COMPLETED; stock posted |

---

## 6. Repository Tests (`@DataJpaTest`)

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `MobileSessionRepository_findByRefreshToken` | `findByRefreshToken(token)` | Token exists | Returns Optional with session |
| `MobileSessionRepository_findByUserId` | `findByUserId(userId)` | Active session | Returns active sessions |
| `MobileCartRepository_findByUserIdAndTenantId` | `findByUserAndTenant(userId, tenantId)` | Cart exists | Returns Optional |

---

## 7. Integration Tests — Mobile API

Base path: `/api/v1/mobile`

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|---|---|---|---|
| `mobileLogin_returns200_withTokenPair` | `POST /api/v1/mobile/auth/login` | No auth; valid credentials | `200 OK`; accessToken + refreshToken |
| `mobileLogin_returns401_badCredentials` | `POST /api/v1/mobile/auth/login` | No auth; wrong password | `401 Unauthorized` |
| `mobileRefreshToken_returns200_newToken` | `POST /api/v1/mobile/auth/refresh` | Valid refresh token | `200 OK`; new accessToken |
| `mobileRefreshToken_returns401_expired` | `POST /api/v1/mobile/auth/refresh` | Expired token | `401 Unauthorized` |
| `mobileLogout_returns200` | `POST /api/v1/mobile/auth/logout` | Bearer mobile token | `200 OK` |
| `getProducts_returns200` | `GET /api/v1/mobile/products` | Bearer mobile token | `200 OK`; paged products |
| `getProducts_returns401_noToken` | `GET /api/v1/mobile/products` | No token | `401 Unauthorized` |
| `searchProducts_returns200` | `GET /api/v1/mobile/products/search?q=phone` | Bearer mobile token | `200 OK`; matching |
| `scanBarcode_returns200_productFound` | `GET /api/v1/mobile/products/barcode/{code}` | Bearer mobile token; valid | `200 OK`; product dto |
| `scanBarcode_returns404_notFound` | `GET /api/v1/mobile/products/barcode/{code}` | Bearer mobile token; unknown | `404 Not Found` |
| `getCart_returns200` | `GET /api/v1/mobile/cart` | Bearer mobile token | `200 OK`; cart dto |
| `addCartItem_returns200` | `POST /api/v1/mobile/cart/items` | Bearer mobile token; valid | `200 OK`; updated cart |
| `addCartItem_returns422_insufficientStock` | `POST /api/v1/mobile/cart/items` | Bearer mobile token; low stock | `422 Unprocessable Entity` |
| `updateCartItemQty_returns200` | `PUT /api/v1/mobile/cart/items/{lineId}` | Bearer mobile token | `200 OK`; updated line |
| `removeCartItem_returns200` | `DELETE /api/v1/mobile/cart/items/{lineId}` | Bearer mobile token | `200 OK` |
| `checkoutCart_returns201_transactionCreated` | `POST /api/v1/mobile/cart/checkout` | Bearer mobile token; valid | `201 Created`; transaction dto |
| `checkoutCart_returns422_underpayment` | `POST /api/v1/mobile/cart/checkout` | Bearer mobile token; low payment | `422 Unprocessable Entity` |
| `syncProducts_returns200` | `GET /api/v1/mobile/sync/products?lastSync=...` | Bearer mobile token | `200 OK`; changed products |
| `getPendingReceiving_returns200` | `GET /api/v1/mobile/receiving` | Bearer mobile token | `200 OK`; pending orders |
| `completeReceiving_returns200` | `POST /api/v1/mobile/receiving/{id}/complete` | Bearer mobile token | `200 OK` |

---

# Section 5b: SMS, Telegram & Reports — Test Plan

---

## SMS MODULE

### Unit Tests

#### SmsService

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| sendSms_success | sendSms("+998901234567", "Hello") | Valid Uzbek phone number and non-empty message | SMS dispatched via provider; returns delivery receipt or success status |
| sendSms_invalidPhone_throwsValidationException | sendSms("invalid", "Hello") | Phone number fails format validation | Throws ValidationException with message indicating invalid phone format |
| sendSms_providerError_throwsBusinessException | sendSms("+998901234567", "Hello") | Provider API returns error response | Throws BusinessException wrapping the provider failure; message not silently dropped |
| sendBulkSms_allSent | sendBulkSms(["+998901234567", "+998901234568", "+998901234569"], "msg") | All three numbers are valid and provider accepts all | Returns result indicating 3 successes, 0 failures |
| sendBulkSms_partialFailures_partialResult | sendBulkSms(["+998901234567", "+998901234568", "+998901234569"], "msg") | Provider rejects second number mid-batch | Returns partial result with 2 successes and 1 failure; does not throw exception |
| sendBulkSms_emptyList_throwsValidationException | sendBulkSms([], "msg") | Recipients list is empty | Throws ValidationException before any provider call is made |
| sendTemplatedSms_success | sendTemplatedSms("+998901234567", "WELCOME", {name: "John"}) | Template with code "WELCOME" exists, all variables present | Template body retrieved, "John" substituted, SMS sent successfully |
| sendTemplatedSms_templateNotFound_throwsNotFoundException | sendTemplatedSms("+998901234567", "UNKNOWN_CODE", {name: "John"}) | No template with code "UNKNOWN_CODE" exists | Throws NotFoundException referencing the missing template code |
| sendTemplatedSms_missingVariable_throwsBusinessException | sendTemplatedSms("+998901234567", "WELCOME", {}) | Template expects {{name}} but map is empty | Throws BusinessException indicating required template variable is missing |

#### SmsTemplateService

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| getTemplates_returnsPage | getTemplates(pageable) | Multiple SMS templates exist | Returns Page<SmsTemplateDto> with all templates for the tenant |
| getTemplates_returnsEmptyPage | getTemplates(pageable) | No templates exist | Returns empty Page<SmsTemplateDto> |
| getTemplate_found | getTemplate(templateId) | Template with given id exists | Returns matching SmsTemplateDto |
| getTemplate_notFound | getTemplate(templateId) | No template with that id | Throws NotFoundException |
| createTemplate_success | createTemplate(request) | Valid request with unique code | Saves and returns SmsTemplateDto with generated id |
| createTemplate_duplicateCode_throwsDuplicateResourceException | createTemplate(request) | Template code already exists for tenant | Throws DuplicateResourceException indicating code collision |
| updateTemplate_success | updateTemplate(templateId, request) | Template exists, valid update payload | Updates and returns updated SmsTemplateDto |
| updateTemplate_notFound | updateTemplate(templateId, request) | Template does not exist | Throws NotFoundException |
| deleteTemplate_success | deleteTemplate(templateId) | Template exists | Deletes template without error |
| deleteTemplate_notFound | deleteTemplate(templateId) | Template does not exist | Throws NotFoundException |

#### PhoneUtils

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| validatePhoneNumber_validUzbek_returnsTrue | validatePhoneNumber("+998901234567") | Standard Uzbek mobile number with country code and + prefix | Returns true |
| validatePhoneNumber_missingPlus_returnsFalse | validatePhoneNumber("998901234567") | Number missing leading + | Returns false |
| validatePhoneNumber_nonNumeric_returnsFalse | validatePhoneNumber("+998901ABCDEF") | Non-numeric characters after country code | Returns false |
| validatePhoneNumber_empty_returnsFalse | validatePhoneNumber("") | Empty string | Returns false |
| validatePhoneNumber_null_returnsFalse | validatePhoneNumber(null) | Null input | Returns false without throwing NullPointerException |
| formatPhoneNumber_addsPlus | formatPhoneNumber("998901234567") | Number without leading + | Returns "+998901234567" |
| formatPhoneNumber_alreadyFormatted_unchanged | formatPhoneNumber("+998901234567") | Number already has + prefix | Returns "+998901234567" unchanged |
| normalizePhoneNumber_stripsSpaces | normalizePhoneNumber("+998 90 123 45 67") | Phone number with spaces | Returns "+998901234567" with all spaces removed |
| normalizePhoneNumber_stripsDashes | normalizePhoneNumber("+998-90-123-45-67") | Phone number with dashes | Returns "+998901234567" with all dashes removed |

#### Repository Tests (@DataJpaTest)

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| smsTemplateRepo_findByCode_found | SmsTemplateRepository.findByCode(tenantId, "WELCOME") | Template with code "WELCOME" exists for tenant | Returns Optional containing matching SmsTemplate entity |
| smsTemplateRepo_findByCode_notFound | SmsTemplateRepository.findByCode(tenantId, "MISSING") | No template with that code | Returns empty Optional |
| smsTemplateRepo_findActiveTemplates | SmsTemplateRepository.findActiveTemplates(tenantId) | Tenant has 4 templates, 2 active and 2 inactive | Returns only the 2 active templates |

---

### Integration Tests

#### SmsController — /api/v1/sms

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|-----------|-------------------|----------------|----------------------------|
| sendSms_success | POST /api/v1/sms/send | Bearer token with SMS_SEND | 200 OK — JSON body confirming delivery with messageId |
| sendSms_invalidPhone_badRequest | POST /api/v1/sms/send | Bearer token with SMS_SEND, malformed phone in body | 400 Bad Request — validation error body indicating invalid phone format |
| sendSms_forbidden | POST /api/v1/sms/send | Bearer token without SMS_SEND | 403 Forbidden |
| sendBulkSms_allSuccess | POST /api/v1/sms/send-bulk | Bearer token with SMS_SEND, 3 valid numbers | 200 OK — result body showing 3 successes |
| sendBulkSms_partialFailure | POST /api/v1/sms/send-bulk | Bearer token with SMS_SEND, 3 numbers of which one fails at provider | 207 Multi-Status — partial result body listing per-number status |
| listTemplates_ok | GET /api/v1/sms/templates | Bearer token with SMS_READ | 200 OK — paginated list of SmsTemplateDto |
| getTemplate_found | GET /api/v1/sms/templates/{id} | Bearer token with SMS_READ | 200 OK — SmsTemplateDto body |
| getTemplate_notFound | GET /api/v1/sms/templates/{id} | Bearer token with SMS_READ | 404 Not Found — error body with message |
| createTemplate_success | POST /api/v1/sms/templates | Bearer token with SMS_WRITE, unique code | 201 Created — new SmsTemplateDto with generated id |
| createTemplate_duplicateCode | POST /api/v1/sms/templates | Bearer token with SMS_WRITE, code already exists | 409 Conflict — error body indicating duplicate template code |
| updateTemplate_success | PUT /api/v1/sms/templates/{id} | Bearer token with SMS_WRITE | 200 OK — updated SmsTemplateDto |
| deleteTemplate_success | DELETE /api/v1/sms/templates/{id} | Bearer token with SMS_WRITE | 204 No Content |

---

## TELEGRAM MODULE

### Unit Tests

#### TelegramBotService

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| handleWebhook_validSecretAndPayload_processed | handleWebhook(validUpdate, correctSecret) | Valid update object with correct X-Telegram-Bot-Api-Secret-Token header | Update processed without error; appropriate handler invoked |
| handleWebhook_invalidSecret_throwsForbiddenException | handleWebhook(validUpdate, wrongSecret) | Secret token header does not match configured value | Throws ForbiddenException; update is rejected |
| handleWebhook_unknownCommand_ignored | handleWebhook(updateWithUnknownCommand, correctSecret) | Incoming message starts with unknown /command | Update silently ignored; no exception thrown; no response sent |
| sendMessage_success_apiCalled | sendMessage(chatId, "Hello") | Valid chatId, message non-empty | Telegram Bot API called with correct chatId and text; no exception |
| sendMessage_invalidChatId_loggedNotThrown | sendMessage(invalidChatId, "Hello") | Telegram API returns "chat not found" error | Error is logged at WARN/ERROR level; no exception propagated to caller |
| sendBotMessage_userHasTelegram_callsSendMessage | sendBotMessage(userId, "msg") | User record has a linked Telegram chatId | sendMessage is invoked with the user's chatId and the given message |
| sendBotMessage_userNoTelegram_noOp | sendBotMessage(userId, "msg") | User record has no linked Telegram chatId | Method returns without calling sendMessage; no error |

#### TelegramNotificationService

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| sendNotification_userHasTelegram_sent | sendNotification(userId, notification) | User exists and has Telegram chatId | Notification delivered via sendMessage; no exception |
| sendNotification_userNotFound_throwsNotFoundException | sendNotification(unknownUserId, notification) | No user with that id exists | Throws NotFoundException |
| sendNotification_userNoTelegram_noOp | sendNotification(userId, notification) | User exists but has no linked Telegram account | Method completes silently without attempting to send |
| sendAlert_subscribedManagers_allReceiveMessage | sendAlert(tenantId, LOW_STOCK, data) | Tenant has 3 managers subscribed to LOW_STOCK alerts | sendMessage called once per subscribed manager with formatted alert text |
| sendAlert_noSubscribers_noOp | sendAlert(tenantId, LOW_STOCK, data) | No managers are subscribed to LOW_STOCK alerts for tenant | Method completes without calling sendMessage |

#### TelegramDailyReportService

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| sendReport_subscribersExist_reportSent | sendReport(tenantId) | Tenant has subscribers and report data exists | Report generated and delivered to all subscribers |
| sendReport_noSubscribers_noOp | sendReport(tenantId) | No subscribers configured for daily report | Method completes without generating or sending anything |
| generateAndSendDailyReport_correctMetrics | generateAndSendDailyReport(tenantId) | Tenant has transactions today | Generated report message contains correct sales totals and transaction count for the day |
| generateAndSendDailyReport_noTransactionsToday_noSalesReport | generateAndSendDailyReport(tenantId) | No transactions recorded for today | Report message sent contains "No sales" text; subscribers still receive the report |
| generateAndSendDailyReport_apiError_loggedNotPropagated | generateAndSendDailyReport(tenantId) | Telegram API throws exception during delivery | Exception caught and logged; method returns normally without re-throwing |

---

### Integration Tests

#### TelegramController — /api/v1/telegram & /api/v1/admin/telegram

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|-----------|-------------------|----------------|----------------------------|
| webhook_validSecret_processed | POST /api/v1/telegram/webhook | Valid X-Telegram-Bot-Api-Secret-Token header and well-formed JSON update | 200 OK — empty body or `{"status":"ok"}` |
| webhook_badSecret_forbidden | POST /api/v1/telegram/webhook | Wrong or missing X-Telegram-Bot-Api-Secret-Token header | 403 Forbidden |
| webhook_badJson_badRequest | POST /api/v1/telegram/webhook | Valid secret but malformed JSON body | 400 Bad Request — error body indicating JSON parse failure |
| getAdminStatus_ok | GET /api/v1/admin/telegram/status | Bearer token with ADMIN role | 200 OK — JSON body with bot connection status and bot username |
| adminSendMessage_success | POST /api/v1/admin/telegram/send | Bearer token with ADMIN role, valid userId | 200 OK — body confirming message dispatched |
| adminSendMessage_userNotFound | POST /api/v1/admin/telegram/send | Bearer token with ADMIN role, unknown userId | 404 Not Found — error body |
| adminSendMessage_forbidden | POST /api/v1/admin/telegram/send | Bearer token without ADMIN role | 403 Forbidden |
| adminSendReport_success | POST /api/v1/admin/telegram/send-report | Bearer token with ADMIN role, valid tenantId | 200 OK — body confirming report dispatched to subscribers |

---

## REPORTS MODULE

### Unit Tests

#### ReportService

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| getReportDefinitions_returnsPage | getReportDefinitions(tenantId, pageable) | Report definitions exist | Returns Page<ReportDefinitionDto> for the tenant |
| getReportDefinitions_returnsEmptyPage | getReportDefinitions(tenantId, pageable) | No definitions exist | Returns empty Page<ReportDefinitionDto> |
| getReportDefinition_found | getReportDefinition(tenantId, definitionId) | Definition exists | Returns matching ReportDefinitionDto |
| getReportDefinition_notFound | getReportDefinition(tenantId, definitionId) | No definition with that id | Throws NotFoundException |
| createReportDefinition_success | createReportDefinition(tenantId, request) | Valid request, name unique within module | Saves and returns ReportDefinitionDto with generated id |
| createReportDefinition_duplicateName | createReportDefinition(tenantId, request) | Report name already exists for same module | Throws DuplicateResourceException |
| updateReportDefinition_success | updateReportDefinition(tenantId, definitionId, request) | Definition exists, valid payload | Updates and returns ReportDefinitionDto |
| updateReportDefinition_notFound | updateReportDefinition(tenantId, definitionId, request) | Definition does not exist | Throws NotFoundException |
| executeReport_returnsPendingAndTriggersAsync | executeReport(tenantId, definitionId, params) | Valid definition id and valid parameters | Returns ReportExecutionDto with status PENDING; async execution triggered |
| executeReport_definitionNotFound | executeReport(tenantId, unknownDefinitionId, params) | Definition id does not exist | Throws NotFoundException |
| executeReport_invalidParams_throwsValidationException | executeReport(tenantId, definitionId, invalidParams) | Required parameter missing or type mismatch | Throws ValidationException with field-level details |
| getReportExecution_completed | getReportExecution(tenantId, executionId) | Execution finished successfully | Returns ReportExecutionDto with status COMPLETED and download reference |
| getReportExecution_pending | getReportExecution(tenantId, executionId) | Execution still running | Returns ReportExecutionDto with status PENDING |
| getReportExecution_failed | getReportExecution(tenantId, executionId) | Execution encountered error | Returns ReportExecutionDto with status FAILED and error message |
| getReportExecution_notFound | getReportExecution(tenantId, executionId) | Execution record does not exist | Throws NotFoundException |

#### FinancialReportService

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| generateTrialBalance_debitsEqualCredits | generateTrialBalance(tenantId, period) | Journal has balanced entries for the period | Total debit column sum equals total credit column sum in returned trial balance |
| generateTrialBalance_noEntries_zeroBalances | generateTrialBalance(tenantId, period) | No journal entries for the period | Returns trial balance with all account balances set to zero |
| generateIncomeStatement_correctNetIncome | generateIncomeStatement(tenantId, period) | Revenue = 100 000, Expenses = 70 000 | Net income in returned statement equals 30 000 |
| generateIncomeStatement_emptyPeriod_zeros | generateIncomeStatement(tenantId, period) | No revenue or expense entries | Returns income statement with all values zero |
| generateBalanceSheet_balancingEquation | generateBalanceSheet(tenantId, asOf) | Assets, liabilities, and equity accounts all have balances | Total assets equals total liabilities plus total equity |
| generateCashFlow_allSectionsPresent | generateCashFlow(tenantId, period) | Transactions span operating, investing, and financing categories | Returned cash flow statement contains non-null operating, investing, and financing sections |

#### SalesReportService

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| generateSalesSummaryReport_correctTotals | generateSalesSummaryReport(tenantId, dateRange) | Multiple sales orders in range | Report totals match sum of order amounts for the period |
| generateSalesSummaryReport_emptyRange_zeros | generateSalesSummaryReport(tenantId, dateRange) | No sales in the given date range | Report returns with all numeric fields as zero |
| getSalesMetrics_daily | getSalesMetrics(tenantId, DAILY, date) | Sales exist for the specific day | Returns daily metrics including total revenue, order count, and average order value |
| getSalesMetrics_monthly | getSalesMetrics(tenantId, MONTHLY, yearMonth) | Sales exist for the month | Returns monthly metrics aggregated over the full month |
| getTopSellingProducts_sortedDescending | getTopSellingProducts(tenantId, dateRange, limit) | Multiple products sold in varying quantities | Returns list of products sorted by quantity sold in descending order |
| getTopSellingProducts_noSales_emptyList | getTopSellingProducts(tenantId, dateRange, limit) | No sales in the date range | Returns empty list without error |

#### InventoryReportService

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| generateStockOnHandReport_allProducts | generateStockOnHandReport(tenantId) | Tenant has products with varying stock levels | Returns report containing an entry for every product including zero-stock items |
| generateInventoryValuationReport_correctValue | generateInventoryValuationReport(tenantId) | Product has quantity 10 and unit cost 50 | Valuation report line for that product shows 500 total value (qty × cost) |
| generateAgingReport_correctBuckets | generateAgingReport(tenantId, asOf) | Stock items have varying purchase dates | Report groups items into correct aging buckets (e.g. 0-30, 31-60, 61-90, 90+ days) |

#### SalaryReportService

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| generateSalaryReport_allEmployeesNetSalary | generateSalaryReport(tenantId, period) | Multiple employees with salary records for the period | Report contains one line per employee with correct net salary; no employee omitted |
| calculatePayroll_correctNet | calculatePayroll(grossSalary, deductions, advances) | grossSalary=6 000, deductions=400, advances=600 | Returns net salary of 5 000 |

#### ReportExportService

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| exportToExcel_nonEmptyData_returnsXlsxBytes | exportToExcel(reportData) | Report data contains multiple rows | Returns non-empty byte array; first two bytes are the XLSX magic number (PK signature) |
| exportToExcel_emptyData_headerRowOnly | exportToExcel(emptyReportData) | Report data has column definitions but zero data rows | Returns non-empty byte array; resulting workbook contains only the header row |
| exportToPdf_nonEmptyData_returnsPdfBytes | exportToPdf(reportData) | Report data contains rows | Returns non-empty byte array; content starts with %PDF magic string |
| exportToCsv_validData_validCsv | exportToCsv(reportData) | Report data with standard field values | Returns valid CSV string with correct delimiter and line breaks |
| exportToCsv_commaInValue_quoted | exportToCsv(reportDataWithCommaInField) | One field contains a comma character | That field is enclosed in double-quotes in the output |
| exportToCsv_emptyData_headerOnly | exportToCsv(emptyReportData) | No data rows | Returns CSV string containing only the header line |
| scheduleReportExport_persistsSchedule | scheduleReportExport(tenantId, request) | Valid cron expression and report definition | ReportSchedule entity persisted to database with correct fields |

#### Repository Tests (@DataJpaTest)

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| reportDefinitionRepo_findByModule | ReportDefinitionRepository.findByModule(tenantId, module) | Multiple definitions across different modules | Returns only definitions belonging to the specified module |
| reportDefinitionRepo_findActiveReports | ReportDefinitionRepository.findActiveReports(tenantId) | Mix of active and inactive definitions | Returns only definitions with active status |
| reportExecutionRepo_findByDefinitionId | ReportExecutionRepository.findByDefinitionId(definitionId) | Definition has 3 executions | Returns all 3 execution records |
| reportExecutionRepo_findExecutionsByDateRange | ReportExecutionRepository.findExecutionsByDateRange(tenantId, start, end) | Executions exist both inside and outside date range | Returns only executions whose startedAt falls within [start, end] |
| reportScheduleRepo_findActiveSchedules | ReportScheduleRepository.findActiveSchedules(tenantId) | Tenant has enabled and disabled schedules | Returns only enabled schedules |
| reportScheduleRepo_findSchedulesDueToRun | ReportScheduleRepository.findSchedulesDueToRun(now) | Schedules with past nextRunAt and future nextRunAt exist | Returns only schedules whose nextRunAt is at or before the given timestamp |

#### Mapper Tests

| Test Name | Method | Scenario | Expected Outcome |
|-----------|--------|----------|-----------------|
| reportDefinitionMapper_toDto | ReportDefinitionMapper.toDto(entity) | Valid ReportDefinition entity | Returns ReportDefinitionDto with all fields including module, name, and parameters schema |
| reportDefinitionMapper_fromCreateRequest | ReportDefinitionMapper.fromCreateRequest(request) | Valid CreateReportDefinitionRequest | Returns ReportDefinition entity populated from request fields |
| reportExecutionMapper_toDto | ReportExecutionMapper.toDto(entity) | Valid ReportExecution entity with status COMPLETED | Returns ReportExecutionDto with status, startedAt, completedAt, and output reference |
| reportExecutionMapper_fromCreateRequest | ReportExecutionMapper.fromCreateRequest(request) | Valid CreateReportExecutionRequest | Returns ReportExecution entity with status set to PENDING |
| reportScheduleMapper_toDto | ReportScheduleMapper.toDto(entity) | Valid ReportSchedule entity | Returns ReportScheduleDto with cronExpression, nextRunAt, and enabled flag |
| reportScheduleMapper_fromCreateRequest | ReportScheduleMapper.fromCreateRequest(request) | Valid CreateReportScheduleRequest | Returns ReportSchedule entity with enabled=true and computed nextRunAt |

---

### Integration Tests

#### ReportDefinitionController — /api/v1/reports/definitions

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|-----------|-------------------|----------------|----------------------------|
| listDefinitions_ok | GET /api/v1/reports/definitions | Bearer token with REPORTS_READ | 200 OK — paginated list of ReportDefinitionDto |
| getDefinition_found | GET /api/v1/reports/definitions/{id} | Bearer token with REPORTS_READ | 200 OK — ReportDefinitionDto body |
| getDefinition_notFound | GET /api/v1/reports/definitions/{id} | Bearer token with REPORTS_READ | 404 Not Found — error body with message |
| createDefinition_success | POST /api/v1/reports/definitions | Bearer token with REPORTS_WRITE, unique name | 201 Created — new ReportDefinitionDto with generated id |
| createDefinition_duplicateName | POST /api/v1/reports/definitions | Bearer token with REPORTS_WRITE, name already exists | 409 Conflict — error body indicating duplicate report name |
| updateDefinition_success | PUT /api/v1/reports/definitions/{id} | Bearer token with REPORTS_WRITE | 200 OK — updated ReportDefinitionDto |

#### ReportExecutionController — /api/v1/reports

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|-----------|-------------------|----------------|----------------------------|
| executeReport_accepted | POST /api/v1/reports/execute | Bearer token with REPORTS_EXECUTE, valid definitionId and params | 202 Accepted — ReportExecutionDto with status PENDING and executionId |
| executeReport_invalidParams | POST /api/v1/reports/execute | Bearer token with REPORTS_EXECUTE, missing required parameter | 400 Bad Request — validation error body listing missing fields |
| executeReport_definitionNotFound | POST /api/v1/reports/execute | Bearer token with REPORTS_EXECUTE, unknown definitionId | 404 Not Found |
| getExecution_completed | GET /api/v1/reports/executions/{id} | Bearer token with REPORTS_READ, execution is COMPLETED | 200 OK — ReportExecutionDto with status COMPLETED and output download URL |
| getExecution_pending | GET /api/v1/reports/executions/{id} | Bearer token with REPORTS_READ, execution still running | 200 OK — ReportExecutionDto with status PENDING |
| getExecution_failed | GET /api/v1/reports/executions/{id} | Bearer token with REPORTS_READ, execution failed | 200 OK — ReportExecutionDto with status FAILED and error message |
| getExecution_notFound | GET /api/v1/reports/executions/{id} | Bearer token with REPORTS_READ | 404 Not Found — error body |

---

# Section 6a-i: Frontend — Stores, Services, Router & Auth Views — Test Plan

**Platform:** Hisobnoma SaaS  
**Scope:** Pinia stores (`auth.js`, `receipt.js`), services (`tokenStorage.js`, `api.js`), Vue Router guards, and Auth Views  
**Goal:** 100% test coverage  
**Stack:** Vitest + @vue/test-utils + MSW (Mock Service Worker) + Pinia test helpers

---

## 1. Pinia Store: `auth.js`

**State shape:** `user`, `accessToken`, `refreshToken`, `permissions`, `roles`

### 1.1 Action: `login(credentials)`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| login — success sets tokens in state | `login({ username, password })` | Valid credentials, API returns 200 with `accessToken` and `refreshToken` | `state.accessToken` and `state.refreshToken` equal the returned token values; `state.user` populated |
| login — success writes tokens to localStorage | `login({ username, password })` | Valid credentials, API returns 200 | `localStorage.getItem('accessToken')` and `localStorage.getItem('refreshToken')` equal the returned token values |
| login — invalid password sets error | `login({ username, password })` | API returns 401 | `state.accessToken` is `null`; `state.refreshToken` is `null`; `state.error` contains a non-empty error message |
| login — locked account sets error | `login({ username, password })` | API returns 403 | `state.error` equals `"Account locked"`; tokens remain `null` |
| login — network error sets error | `login({ username, password })` | Network request fails (no response) | `state.error` equals `"Network error"`; tokens remain `null` |

### 1.2 Action: `logout()`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| logout — clears user from state | `logout()` | Called after a successful login | `state.user` is `null` |
| logout — clears accessToken from state | `logout()` | Called after a successful login | `state.accessToken` is `null` |
| logout — clears refreshToken from state | `logout()` | Called after a successful login | `state.refreshToken` is `null` |
| logout — removes accessToken from localStorage | `logout()` | Tokens previously written to localStorage | `localStorage.getItem('accessToken')` is `null` |
| logout — removes refreshToken from localStorage | `logout()` | Tokens previously written to localStorage | `localStorage.getItem('refreshToken')` is `null` |
| logout — resets permissions to empty array | `logout()` | State had populated permissions array | `state.permissions` deep-equals `[]` |

### 1.3 Action: `refresh()`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| refresh — valid token updates accessToken in state | `refresh()` | API returns 200 with new `accessToken` | `state.accessToken` equals the new token value |
| refresh — valid token writes new accessToken to localStorage | `refresh()` | API returns 200 with new `accessToken` | `localStorage.getItem('accessToken')` equals the new token value |
| refresh — expired token calls logout and clears state | `refresh()` | API returns 401 | `logout()` action invoked; `state.accessToken` is `null`; `state.refreshToken` is `null` |
| refresh — network error calls logout | `refresh()` | Network request fails | `logout()` action invoked; state cleared |

### 1.4 Action: `changePassword(data)`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| changePassword — correct password resolves without state change | `changePassword({ current, newPassword })` | API returns 200 | Promise resolves; no change to `accessToken`, `user`, or `permissions` in state |
| changePassword — wrong password propagates error | `changePassword({ current, newPassword })` | API returns 400 | Promise rejects (or throws); error propagated to caller; state unchanged |

### 1.5 Action: `forgotPassword(email)`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| forgotPassword — existing email returns success flag | `forgotPassword('user@example.com')` | API returns 200 | Resolves with a truthy success indicator; no state mutation |
| forgotPassword — non-existent email also returns success flag | `forgotPassword('nobody@example.com')` | API returns 200 (security: no user enumeration) | Resolves with a truthy success indicator; no state mutation; no error set in state |

### 1.6 Action: `resetPassword(data)`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| resetPassword — valid token and matching passwords succeeds | `resetPassword({ token, password, confirm })` | `password === confirm`, API returns 200 | Resolves with success flag; no error in state |
| resetPassword — expired token propagates error | `resetPassword({ token, password, confirm })` | API returns 400 | Rejects or sets `state.error`; error communicated to caller |
| resetPassword — password mismatch skips API call | `resetPassword({ token, password, confirm })` | `password !== confirm` | Validation error thrown/returned before any HTTP request is made; API endpoint never called |

### 1.7 Getters

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| isAuthenticated — true when accessToken present | `isAuthenticated` getter | `state.accessToken` is a non-null string | Returns `true` |
| isAuthenticated — false when accessToken is null | `isAuthenticated` getter | `state.accessToken` is `null` | Returns `false` |
| currentUser — returns user object | `currentUser` getter | `state.user` is populated with a user object | Returns the exact `state.user` object |
| permissions — returns permissions array | `permissions` getter | `state.permissions` is `['INVENTORY_READ', 'POS_READ']` | Returns `['INVENTORY_READ', 'POS_READ']` |
| roles — returns roles array | `roles` getter | `state.roles` is `['cashier']` | Returns `['cashier']` |
| hasPermission — true when permission in list | `hasPermission('INVENTORY_READ')` | `state.permissions` includes `'INVENTORY_READ'` | Returns `true` |
| hasPermission — false when permission not in list | `hasPermission('ADMIN')` | `state.permissions` does not include `'ADMIN'` | Returns `false` |

### 1.8 Token Persistence

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| token persistence — both tokens written after login | `login(credentials)` | Successful login | `localStorage.getItem('accessToken')` and `localStorage.getItem('refreshToken')` are both set |
| token persistence — loadTokens restores state from localStorage | `loadTokens()` (or app-init hook) | Tokens present in localStorage before store initialises | `state.accessToken` and `state.refreshToken` equal the localStorage values |
| token persistence — localStorage keys cleared after logout | `logout()` | Tokens exist in localStorage and state | Both localStorage keys return `null` after logout |

---

## 2. Pinia Store: `receipt.js`

**State shape:** `currentReceipt`, `items` (array), `totals` (`subtotal`, `discount`, `tax`, `total`)

### 2.1 Action: `addItem(product, qty)`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| addItem — new product appended to items | `addItem(product, 2)` | Product not already in `state.items` | `state.items` has length 1; item contains `productId`, `qty: 2`, `lineTotal: price × 2` |
| addItem — lineTotal calculated correctly for new item | `addItem({ id: 1, price: 10.00 }, 3)` | New product with qty 3 | `state.items[0].lineTotal === 30.00` |
| addItem — existing product increments qty | `addItem(product, 2)` called twice | Same product added twice | `state.items` still has length 1; `qty === 4` |
| addItem — existing product recalculates lineTotal | `addItem({ id: 1, price: 5.00 }, 2)` then `addItem({ id: 1, price: 5.00 }, 3)` | Same product added twice | `state.items[0].lineTotal === 25.00` |

### 2.2 Action: `removeItem(productId)`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| removeItem — existing product removed from array | `removeItem(productId)` | Product with `productId` is in `state.items` | `state.items` no longer contains an item with that `productId`; array length decremented |
| removeItem — non-existent productId is a no-op | `removeItem(9999)` | No item with `productId 9999` in `state.items` | `state.items` unchanged; no error thrown |

### 2.3 Action: `calculateTotals()`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| calculateTotals — subtotal equals sum of all lineTotals | `calculateTotals()` | Items with lineTotals `[100, 50, 25]` | `state.totals.subtotal === 175.00` |
| calculateTotals — discount applied at 10% | `calculateTotals()` | Subtotal = 100, discount rate = 10% | `state.totals.discount === 10.00` |
| calculateTotals — tax applied at 15% on discounted subtotal | `calculateTotals()` | Subtotal = 100, discount = 10 (net = 90), tax rate = 15% | `state.totals.tax === 13.50` |
| calculateTotals — total equals subtotal minus discount plus tax | `calculateTotals()` | Subtotal = 100, discount = 10, tax = 13.50 | `state.totals.total === 103.50` |
| calculateTotals — zero items yields all-zero totals | `calculateTotals()` | `state.items` is empty | All four totals equal `0` |

### 2.4 Action: `clearReceipt()`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| clearReceipt — items reset to empty array | `clearReceipt()` | Items array had entries | `state.items` deep-equals `[]` |
| clearReceipt — subtotal reset to zero | `clearReceipt()` | `state.totals.subtotal` was non-zero | `state.totals.subtotal === 0` |
| clearReceipt — discount reset to zero | `clearReceipt()` | `state.totals.discount` was non-zero | `state.totals.discount === 0` |
| clearReceipt — tax reset to zero | `clearReceipt()` | `state.totals.tax` was non-zero | `state.totals.tax === 0` |
| clearReceipt — total reset to zero | `clearReceipt()` | `state.totals.total` was non-zero | `state.totals.total === 0` |

---

## 3. Service: `tokenStorage.js`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| getAccessToken — returns stored value | `getAccessToken()` | `localStorage` contains the access token key with value `'tok-abc'` | Returns `'tok-abc'` |
| getAccessToken — returns null when not set | `getAccessToken()` | `localStorage` does not contain the access token key | Returns `null` |
| getRefreshToken — returns stored value | `getRefreshToken()` | `localStorage` contains the refresh token key with value `'ref-xyz'` | Returns `'ref-xyz'` |
| getRefreshToken — returns null when not set | `getRefreshToken()` | `localStorage` does not contain the refresh token key | Returns `null` |
| setTokens — writes access token to localStorage | `setTokens('abc', 'xyz')` | Clean localStorage | `localStorage.getItem(ACCESS_KEY) === 'abc'` |
| setTokens — writes refresh token to localStorage | `setTokens('abc', 'xyz')` | Clean localStorage | `localStorage.getItem(REFRESH_KEY) === 'xyz'` |
| clearTokens — removes access token key | `clearTokens()` | Both keys previously set | `localStorage.getItem(ACCESS_KEY)` is `null` |
| clearTokens — removes refresh token key | `clearTokens()` | Both keys previously set | `localStorage.getItem(REFRESH_KEY)` is `null` |
| round trip — getAccessToken returns value set by setTokens | `setTokens('abc', 'xyz')` then `getAccessToken()` | Clean localStorage | Returns `'abc'` |
| round trip — getRefreshToken returns value set by setTokens | `setTokens('abc', 'xyz')` then `getRefreshToken()` | Clean localStorage | Returns `'xyz'` |

---

## 4. Service: `api.js` — Auth API Methods (via MSW)

> Each method is tested with three MSW handler scenarios unless noted: **success**, **401 response**, and **500 response**.

### 4.1 `login(credentials)` — POST `/api/v1/auth/login`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| login API — success returns token and user payload | `login({ username, password })` | MSW returns 200 `{ accessToken, refreshToken, user }` | Resolved value matches `{ accessToken, refreshToken, user }` |
| login API — 401 causes axios to throw | `login({ username, password })` | MSW returns 401 | Promise rejects; error has response status `401` |
| login API — 500 causes axios to throw | `login({ username, password })` | MSW returns 500 | Promise rejects; error has response status `500` |

### 4.2 `pinLogin(request)` — POST `/api/v1/auth/pin-login`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| pinLogin API — success returns token and user payload | `pinLogin({ phone, pin })` | MSW returns 200 `{ accessToken, refreshToken, user }` | Resolved value matches `{ accessToken, refreshToken, user }` |
| pinLogin API — 401 causes axios to throw | `pinLogin({ phone, pin })` | MSW returns 401 | Promise rejects; error has response status `401` |
| pinLogin API — 500 causes axios to throw | `pinLogin({ phone, pin })` | MSW returns 500 | Promise rejects; error has response status `500` |

### 4.3 `getUsersList()` — GET `/api/v1/auth/users/list`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| getUsersList API — success returns array of users | `getUsersList()` | MSW returns 200 with array of `{ id, username, displayName }` | Resolved value is an array matching the mocked user objects |
| getUsersList API — 401 causes axios to throw | `getUsersList()` | MSW returns 401 | Promise rejects; error has response status `401` |
| getUsersList API — 500 causes axios to throw | `getUsersList()` | MSW returns 500 | Promise rejects; error has response status `500` |

### 4.4 `setPin(data)` — PUT `/api/v1/auth/set-pin`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| setPin API — success resolves with 200 | `setPin({ pin })` | MSW returns 200 OK | Promise resolves; response status is `200` |
| setPin API — 401 causes axios to throw | `setPin({ pin })` | MSW returns 401 | Promise rejects; error has response status `401` |
| setPin API — 500 causes axios to throw | `setPin({ pin })` | MSW returns 500 | Promise rejects; error has response status `500` |

### 4.5 `logout()` — POST `/api/v1/auth/logout`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| logout API — success resolves with 200 | `logout()` | MSW returns 200 OK | Promise resolves; response status is `200` |
| logout API — 401 causes axios to throw | `logout()` | MSW returns 401 | Promise rejects; error has response status `401` |
| logout API — 500 causes axios to throw | `logout()` | MSW returns 500 | Promise rejects; error has response status `500` |

### 4.6 `refresh(token)` — POST `/api/v1/auth/refresh`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| refresh API — success returns new tokens | `refresh('ref-token')` | MSW returns 200 `{ accessToken, refreshToken }` | Resolved value contains new `accessToken` and `refreshToken` |
| refresh API — 401 causes axios to throw | `refresh('expired-token')` | MSW returns 401 | Promise rejects; error has response status `401` |
| refresh API — 500 causes axios to throw | `refresh('ref-token')` | MSW returns 500 | Promise rejects; error has response status `500` |

### 4.7 `me()` — GET `/api/v1/auth/me`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| me API — success returns user object | `me()` | MSW returns 200 with user object | Resolved value matches the mocked user object |
| me API — 401 causes axios to throw | `me()` | MSW returns 401 | Promise rejects; error has response status `401` |
| me API — 500 causes axios to throw | `me()` | MSW returns 500 | Promise rejects; error has response status `500` |

### 4.8 `changePassword(data)` — PUT `/api/v1/auth/change-password`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| changePassword API — success resolves with 200 | `changePassword({ current, newPassword })` | MSW returns 200 OK | Promise resolves; response status is `200` |
| changePassword API — 401 causes axios to throw | `changePassword({ current, newPassword })` | MSW returns 401 | Promise rejects; error has response status `401` |
| changePassword API — 500 causes axios to throw | `changePassword({ current, newPassword })` | MSW returns 500 | Promise rejects; error has response status `500` |

### 4.9 `forgotPassword(data)` — POST `/api/v1/auth/forgot-password`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| forgotPassword API — success resolves with 200 | `forgotPassword({ email })` | MSW returns 200 OK | Promise resolves; response status is `200` |
| forgotPassword API — 401 causes axios to throw | `forgotPassword({ email })` | MSW returns 401 | Promise rejects; error has response status `401` |
| forgotPassword API — 500 causes axios to throw | `forgotPassword({ email })` | MSW returns 500 | Promise rejects; error has response status `500` |

### 4.10 `resetPassword(data)` — POST `/api/v1/auth/reset-password`

| Test Name | Action/Method | Scenario | Expected State or Return |
|---|---|---|---|
| resetPassword API — success resolves with 200 | `resetPassword({ token, password })` | MSW returns 200 OK | Promise resolves; response status is `200` |
| resetPassword API — 401 causes axios to throw | `resetPassword({ token, password })` | MSW returns 401 | Promise rejects; error has response status `401` |
| resetPassword API — 500 causes axios to throw | `resetPassword({ token, password })` | MSW returns 500 | Promise rejects; error has response status `500` |

---

## 5. Router Tests

| Test Name | Route | Scenario | Expected |
|---|---|---|---|
| guard — unauthenticated redirected from /dashboard | `/dashboard` | `auth.isAuthenticated === false` | Navigation redirected to `/login` |
| guard — unauthenticated redirected from /pos | `/pos` | `auth.isAuthenticated === false` | Navigation redirected to `/login` |
| guard — authenticated user redirected from /login | `/login` | `auth.isAuthenticated === true` | Navigation redirected to `/dashboard` |
| guard — INVENTORY_READ grants access to /inventory/products | `/inventory/products` | User authenticated, `permissions` includes `'INVENTORY_READ'` | Navigation resolves; component rendered (no redirect) |
| guard — missing INVENTORY_READ blocks /inventory/products | `/inventory/products` | User authenticated, `permissions` does NOT include `'INVENTORY_READ'` | Navigation redirected to `/no-access` |
| guard — POS_READ grants access to /pos | `/pos` | User authenticated, `permissions` includes `'POS_READ'` | Navigation resolves; component rendered (no redirect) |
| guard — missing POS_READ blocks /pos | `/pos` | User authenticated, `permissions` does NOT include `'POS_READ'` | Navigation redirected to `/no-access` |
| guard — ADMIN grants access to /admin/users | `/admin/users` | User authenticated, `roles` or `permissions` includes `'ADMIN'` | Navigation resolves; component rendered (no redirect) |
| guard — missing ADMIN blocks /admin/users | `/admin/users` | User authenticated, no `'ADMIN'` role/permission | Navigation redirected to `/no-access` |
| router — unknown path renders NotFoundView | `/completely-unknown-path` | No matching route defined | `NotFoundView` component is rendered |
| router — route change updates document.title | Any route with `meta.title` set | Navigation between two routes with different `meta.title` values | `document.title` updated to match the new route's `meta.title` after navigation |

---

## 6. Auth Views

### 6.1 `LoginView.vue`

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| LoginView — renders username input | `mount()` | Default render | DOM contains an `<input>` with type `text` (or relevant selector) for username |
| LoginView — renders password input | `mount()` | Default render | DOM contains an `<input>` with `type="password"` |
| LoginView — renders Sign In button | `mount()` | Default render | DOM contains a button with text `"Sign In"` |
| LoginView — valid submit calls auth.login | `trigger('submit')` on form | Username and password fields populated with valid values | `auth.login` called once with `{ username, password }` |
| LoginView — valid submit navigates to /dashboard | `trigger('submit')` on form | `auth.login()` resolves successfully | `router.push` called with `'/dashboard'` |
| LoginView — empty username shows required error | `trigger('submit')` on form | Username field is empty | Error text `"Username is required"` appears in DOM; `auth.login` not called |
| LoginView — empty password shows required error | `trigger('submit')` on form | Password field is empty | Error text `"Password is required"` appears in DOM; `auth.login` not called |
| LoginView — failed login shows error under form | `trigger('submit')` on form | `auth.login()` throws (wrong credentials) | Error message from the thrown error rendered beneath the form; user remains on `/login` |
| LoginView — loading spinner visible during API call | `trigger('submit')` on form | `auth.login()` is pending (not yet resolved) | Loading spinner element present in DOM while promise is in flight |
| LoginView — loading spinner hidden after API resolves | `trigger('submit')` on form | `auth.login()` resolves or rejects | Loading spinner no longer visible in DOM |
| LoginView — PIN toggle button rendered | `mount()` | Default render | DOM contains a button with text matching `"Sign in with PIN"` (or equivalent) |
| LoginView — PIN toggle replaces username with phone + PIN | `trigger('click')` on PIN toggle | Default credential mode active | Username input replaced by phone input and PIN input; password input no longer present |
| LoginView — PIN login submit calls auth.pinLogin | `trigger('submit')` on form | PIN mode active; phone and PIN fields populated | `auth.pinLogin` called once with the phone and PIN values |

### 6.2 `ForgotPasswordView.vue`

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| ForgotPasswordView — renders email input | `mount()` | Default render | DOM contains an `<input>` of type `email` (or text) for email address |
| ForgotPasswordView — renders Send Reset Link button | `mount()` | Default render | DOM contains a button with text `"Send Reset Link"` |
| ForgotPasswordView — renders Back to Login link | `mount()` | Default render | DOM contains a link or router-link pointing to `/login` |
| ForgotPasswordView — valid email submit calls API | `trigger('submit')` on form | Email field contains a valid email address | `api.forgotPassword` (or equivalent store action) called once with the email value |
| ForgotPasswordView — valid email shows success message | `trigger('submit')` on form | API resolves with 200 | DOM contains text `"Check your email for reset instructions"` |
| ForgotPasswordView — invalid email format shows error | `trigger('submit')` on form | Email field contains `"not-an-email"` | Error text `"Invalid email format"` appears in DOM; API not called |
| ForgotPasswordView — empty email shows required error | `trigger('submit')` on form | Email field is empty | Error text `"Email is required"` appears in DOM; API not called |
| ForgotPasswordView — Back to Login link navigates to /login | `trigger('click')` on Back to Login link | Default render | Router navigates to `/login` |

### 6.3 `ResetPasswordView.vue`

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| ResetPasswordView — renders new password input | `mount()` | Default render | DOM contains a `type="password"` input for new password |
| ResetPasswordView — renders confirm password input | `mount()` | Default render | DOM contains a second `type="password"` input for password confirmation |
| ResetPasswordView — renders submit button | `mount()` | Default render | DOM contains a submit button |
| ResetPasswordView — token extracted from URL query params on mount | `mount()` with route query `?token=abc123` | Component mounted with query param present | Component internally holds `token === 'abc123'`; no error state shown on initial render |
| ResetPasswordView — matching passwords with valid token calls API | `trigger('submit')` on form | Both password fields match; token present in URL | `api.resetPassword` called once with the token and new password |
| ResetPasswordView — matching passwords success navigates to /login | `trigger('submit')` on form | API resolves with 200 | Router navigates to `/login` after successful reset |
| ResetPasswordView — mismatched passwords shows error, skips API | `trigger('submit')` on form | New password and confirm password fields contain different values | Error text `"Passwords do not match"` appears in DOM; API not called |
| ResetPasswordView — password shorter than 8 chars shows error | `trigger('submit')` on form | New password field contains fewer than 8 characters | Validation error rendered in DOM; API not called |
| ResetPasswordView — API 400 shows expired token error | `trigger('submit')` on form | Passwords match; API returns 400 | Error text `"Reset link has expired"` appears in DOM; user remains on current view |

---

# Section 6a-ii: Frontend — Admin Views — Test Plan

**Stack:** Vitest + @vue/test-utils + MSW  
**Goal:** 100% component test coverage for all admin views  
**Mount strategy:** `mountComponent` via `mount()` with a stubbed `vue-router` and MSW handlers intercepting all API calls at the network layer. Each table row represents one `it()` block.

---

## UsersView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Fetches users on mount with page=0 | `onMounted` | Component mounts; MSW returns a page of users | `getUsers` called once with `{ page: 0 }`; user rows rendered in `<table>` |
| Renders correct table columns | `onMounted` | MSW returns user list | Table header contains columns: Username, Email, Phone, Roles, Status, Actions |
| Renders username cell | `onMounted` | MSW returns user with `username: "alice"` | `<td>` containing `"alice"` present in first row |
| Renders email cell | `onMounted` | MSW returns user with `email: "alice@example.com"` | `<td>` containing `"alice@example.com"` present |
| Renders phone cell | `onMounted` | MSW returns user with `phone: "+998901234567"` | `<td>` containing `"+998901234567"` present |
| Renders roles cell | `onMounted` | MSW returns user with `roles: ["ADMIN","CASHIER"]` | `<td>` displays both role names |
| Renders active status badge | `onMounted` | MSW returns user with `status: "ACTIVE"` | Status badge with text `"active"` (case-insensitive) rendered |
| Renders locked status badge | `onMounted` | MSW returns user with `status: "LOCKED"` | Status badge with text `"locked"` rendered |
| Renders action icons per row | `onMounted` | MSW returns user list | Each row contains edit icon, delete icon, and lock icon |
| Search input triggers debounced API call | `@input` on search field | User types `"bob"` into search input; wait debounce | `getUsers` called with `{ page: 0, search: "bob" }`; table updated with filtered results |
| Search clears results and re-fetches | `@input` on search field | User clears search input | `getUsers` called with `{ page: 0, search: "" }`; original list restored |
| "Create User" button navigates to user form | `@click` on Create User button | Button clicked | `router.push` called with `"/admin/user-form"` |
| Edit icon navigates to user form with id | `@click` on edit icon | Edit icon clicked on row with `userId: 42` | `router.push` called with `"/admin/user-form?id=42"` |
| Delete icon shows confirmation dialog | `@click` on delete icon | Delete icon clicked on a row | Confirmation dialog element becomes visible in DOM |
| Confirmation dialog "Yes" deletes user | `@click` on confirm Yes button | Dialog shown; user clicks "Yes"; MSW handles `deleteUser` | `deleteUser` API called with correct userId; deleted user's row removed from table |
| Confirmation dialog "Cancel" leaves table intact | `@click` on confirm Cancel button | Dialog shown; user clicks "Cancel" | No API call made; all rows remain; dialog closes |
| Lock icon calls lockUser and toggles badge | `@click` on lock icon | Lock icon clicked on ACTIVE user; MSW returns updated user | `lockUser` API called with userId; status badge toggles to `"locked"` |
| Lock icon on LOCKED user unlocks and toggles badge | `@click` on lock icon | Lock icon clicked on LOCKED user; MSW returns updated user | `lockUser` (or `unlockUser`) called; status badge toggles to `"active"` |
| Next page calls getUsers with page+1 | `@click` on next-page button | Pagination next button clicked | `getUsers` called with `{ page: 1 }`; new page of users rendered |
| Previous page calls getUsers with page-1 | `@click` on prev-page button | On page 2; previous button clicked | `getUsers` called with `{ page: 1 }` |
| Empty user list shows empty state message | `onMounted` | MSW returns `{ data: [], total: 0 }` | Element with text `"No users found"` (case-insensitive) visible; table body empty |

---

## UserFormView.vue — Create Mode (no `id` query param)

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Renders empty username field | `onMounted` | No id param; component mounts | `input[name="username"]` (or equivalent) present with empty value |
| Renders empty firstName field | `onMounted` | No id param | `input[name="firstName"]` present with empty value |
| Renders empty lastName field | `onMounted` | No id param | `input[name="lastName"]` present with empty value |
| Renders empty email field | `onMounted` | No id param | `input[name="email"]` or `input[type="email"]` present with empty value |
| Renders empty phone field | `onMounted` | No id param | `input[name="phone"]` present with empty value |
| Renders empty password field | `onMounted` | No id param | `input[type="password"][name="password"]` present with empty value |
| Renders empty confirmPassword field | `onMounted` | No id param | `input[type="password"][name="confirmPassword"]` (or equivalent) present with empty value |
| Loads roles from getRoles API on mount | `onMounted` | MSW returns roles list with 3 roles | `getRoles` called once; 3 role checkboxes rendered |
| Submit valid form calls createUser | `@submit` | All fields filled validly; MSW 201 from createUser | `createUser` POST called with correct payload; `router.push("/admin/users")` called |
| Duplicate username (409) shows error | `@submit` | MSW returns 409 on createUser | Error message `"Username already taken"` (case-insensitive) visible; navigation not called |
| Mismatched passwords blocks API call | `@submit` | password ≠ confirmPassword | Validation error visible; `createUser` API not called |
| Empty username shows validation error | `@submit` | username field left blank | Validation error `"Username required"` (case-insensitive) visible; API not called |
| Invalid email format shows validation error | `@submit` | Email field contains `"not-an-email"` | Validation error for email visible; API not called |
| Checked role included in createUser request | `@change` on role checkbox | Role checkbox checked; form submitted | `createUser` payload contains that role's id/code |
| Unchecked role excluded from createUser request | `@change` on role checkbox | Role checkbox unchecked; form submitted | `createUser` payload does not contain that role's id/code |
| Multiple roles can be selected simultaneously | `@change` on role checkboxes | Two role checkboxes checked | `createUser` payload contains both roles |

---

## UserFormView.vue — Edit Mode (`id` param present)

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Calls getUser on mount with id from query param | `onMounted` | Route query has `id=7`; MSW returns user | `getUser` called with `7`; no error shown |
| Pre-fills username field | `onMounted` | MSW returns user with `username: "alice"` | `input[name="username"]` has value `"alice"` |
| Pre-fills firstName field | `onMounted` | MSW returns user with `firstName: "Alice"` | `input[name="firstName"]` has value `"Alice"` |
| Pre-fills lastName field | `onMounted` | MSW returns user with `lastName: "Smith"` | `input[name="lastName"]` has value `"Smith"` |
| Pre-fills email field | `onMounted` | MSW returns user with `email: "alice@example.com"` | Email input has value `"alice@example.com"` |
| Pre-fills phone field | `onMounted` | MSW returns user with `phone: "+998901234567"` | Phone input has value `"+998901234567"` |
| Password field shows keep-current hint | `onMounted` | Edit mode | Element with text matching `"leave blank to keep current"` (case-insensitive) visible near password field |
| Submit without changing password omits password from payload | `@submit` | Password field left blank; MSW 200 from updateUser | `updateUser` called; request body does not contain `password` key |
| Submit with new password sends password | `@submit` | Password fields filled with new matching values | `updateUser` called with `password` field in payload |
| Role checkboxes pre-checked for user's existing roles | `onMounted` | MSW user has `roles: ["ADMIN"]`; getRoles returns ADMIN + CASHIER | ADMIN checkbox is checked; CASHIER checkbox is unchecked |
| Submit calls updateUser and navigates | `@submit` | Valid data; MSW 200 | `updateUser` called with userId and updated payload; `router.push("/admin/users")` called |
| API 404 on getUser shows error and redirects | `onMounted` | MSW returns 404 for getUser | Error message visible; `router.push("/admin/users")` called |

---

## RolesView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Fetches roles on mount | `onMounted` | MSW returns list of roles | `getRoles` called once; role rows rendered |
| Renders role name column | `onMounted` | MSW returns role with `name: "Admin"` | `<td>` with `"Admin"` visible |
| Renders role code column | `onMounted` | MSW returns role with `code: "ADMIN"` | `<td>` with `"ADMIN"` visible |
| Renders system role badge | `onMounted` | MSW returns role with `systemRole: true` | System role badge/indicator visible in that row |
| Renders user count column | `onMounted` | MSW returns role with `userCount: 5` | `<td>` with `"5"` visible |
| "Create Role" button navigates | `@click` on Create Role | Button clicked | `router.push("/admin/role-form")` called |
| Edit button navigates with roleId | `@click` on edit button | Edit clicked on row with `roleId: 3` | `router.push("/admin/role-form?id=3")` called |
| Delete button on non-system role shows confirmation | `@click` on delete button | Non-system role row; delete clicked | Confirmation dialog visible |
| Confirm delete calls deleteRole and removes row | `@click` confirm Yes | MSW handles deleteRole | `deleteRole` called with roleId; row removed from table |
| Cancel delete makes no API call | `@click` confirm Cancel | Dialog open; Cancel clicked | No API call; row remains; dialog closes |
| Delete button on system role is disabled | `onMounted` | MSW returns role with `systemRole: true` | Delete button in system role row has `disabled` attribute or is absent |
| System role delete attempt does not trigger dialog | `@click` on disabled delete | System role delete clicked | Confirmation dialog does not appear |

---

## RoleFormView.vue — Create Mode

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Renders empty name field | `onMounted` | No id param | `input[name="name"]` present with empty value |
| Renders empty code field | `onMounted` | No id param | `input[name="code"]` present with empty value |
| Renders empty description field | `onMounted` | No id param | `textarea[name="description"]` or equivalent present with empty value |
| Loads permissions from listAllPermissions on mount | `onMounted` | MSW returns permissions grouped by module | `listAllPermissions` called; checkboxes rendered grouped by module |
| Permissions are visually grouped by module | `onMounted` | MSW returns permissions in 2 modules | Two distinct group headings visible with corresponding checkboxes |
| Submit valid data calls createRole | `@submit` | All fields filled; MSW 201 | `createRole` called with correct payload; `router.push("/admin/roles")` called |
| Duplicate code (409) shows error | `@submit` | MSW returns 409 | Error message visible; navigation not called |
| Code auto-slugifies from name (spaces to underscores, uppercase) | `@input` on name field | User types `"super admin"` into name | Code field value becomes `"SUPER_ADMIN"` |
| Code auto-slugifies special characters | `@input` on name field | User types `"role-name!"` | Code field sanitized appropriately (letters, underscores, uppercase) |
| Checking all permissions in a group includes them in request | `@change` on permission checkboxes | All checkboxes in module group checked; form submitted | `createRole` payload contains all permission ids from that group |
| Unchecked permissions excluded from request | `@change` on permission checkbox | One checkbox unchecked | `createRole` payload does not contain that permission id |

---

## RoleFormView.vue — Edit Mode

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Calls getRole on mount with id | `onMounted` | Route query `id=5`; MSW returns role | `getRole` called with `5` |
| Pre-fills name field | `onMounted` | MSW role has `name: "Manager"` | Name input has value `"Manager"` |
| Pre-fills code field | `onMounted` | MSW role has `code: "MANAGER"` | Code input has value `"MANAGER"` |
| Pre-fills description field | `onMounted` | MSW role has `description: "Manages stuff"` | Description field has value `"Manages stuff"` |
| Permissions pre-checked for assigned permissions | `onMounted` | MSW role has `permissions: ["READ_USERS"]`; listAllPermissions returns READ_USERS + WRITE_USERS | READ_USERS checkbox checked; WRITE_USERS checkbox unchecked |
| Submit calls updateRole with updated permissions | `@submit` | User checks additional permission; submits; MSW 200 | `updateRole` called with updated permissions list including newly added permission |
| 404 from getRole shows error and redirects | `onMounted` | MSW returns 404 | Error message visible; `router.push("/admin/roles")` called |

---

## AuditLogsView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Fetches audit logs on mount with default params | `onMounted` | MSW returns log list | `getAuditLogs` called with default params; rows rendered |
| Renders timestamp column | `onMounted` | MSW returns log with `timestamp: "2026-04-17T10:00:00Z"` | Formatted timestamp visible in table |
| Renders user column | `onMounted` | MSW returns log with `user: "alice"` | `"alice"` visible in table row |
| Renders action column | `onMounted` | MSW returns log with `action: "LOGIN"` | `"LOGIN"` visible in table row |
| Renders module column | `onMounted` | MSW returns log with `module: "AUTH"` | `"AUTH"` visible in table row |
| Renders entity column | `onMounted` | MSW returns log with `entity: "User"` | `"User"` visible in table row |
| Renders success indicator | `onMounted` | MSW returns log with `success: true` | Success indicator (badge/icon) visible |
| Renders failure indicator | `onMounted` | MSW returns log with `success: false` | Failure indicator visible |
| Date range filter triggers API call with start date | `@change` on start date picker | Start date selected | `getAuditLogs` called with `startDate` param |
| Date range filter triggers API call with end date | `@change` on end date picker | End date selected | `getAuditLogs` called with `endDate` param |
| Both dates set triggers API call with both params | `@change` on both date pickers | Both dates selected | `getAuditLogs` called with both `startDate` and `endDate` |
| User filter calls getAuditLogsByUser | `@change` on user dropdown | User selected from dropdown | `getAuditLogsByUser` called with selected userId; table updated |
| Action filter calls getAuditLogsByAction | `@change` on action dropdown | Action selected | `getAuditLogsByAction` called with selected action; table updated |
| Module filter calls getAuditLogsByModule | `@change` on module dropdown | Module selected | `getAuditLogsByModule` called with selected module; table updated |
| "Failed only" toggle calls getFailedActions | `@change` on failed-only toggle | Toggle switched on | `getFailedActions` called; table shows only failed entries |
| "Failed only" toggle off restores default | `@change` on failed-only toggle | Toggle switched off | `getAuditLogs` called with default params |
| Pagination next page works in default view | `@click` next-page button | No filters; next page clicked | `getAuditLogs` called with incremented page param |
| Pagination works with user filter active | `@click` next-page button | User filter active; next page clicked | `getAuditLogsByUser` called with incremented page and userId |
| Pagination works with action filter active | `@click` next-page button | Action filter active; next page clicked | `getAuditLogsByAction` called with incremented page |
| Pagination works with failed-only filter active | `@click` next-page button | Failed-only toggle on; next page clicked | `getFailedActions` called with incremented page |
| Empty result shows empty state message | `onMounted` | MSW returns empty list | Element with text `"No audit logs found"` (case-insensitive) visible |

---

## SettingsView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Loads all settings on mount | `onMounted` | MSW returns settings list | `getAllSettings` called once; settings rendered |
| Settings grouped by category | `onMounted` | MSW returns settings in 2 categories | Two section/tab headings corresponding to categories visible |
| Renders setting key | `onMounted` | MSW returns setting with `key: "MAX_RETRIES"` | `"MAX_RETRIES"` visible in table/list |
| Renders current string value | `onMounted` | MSW returns STRING setting with `value: "hello"` | `"hello"` visible in the row |
| Renders type label | `onMounted` | MSW returns setting with `type: "NUMBER"` | `"NUMBER"` label visible in row |
| Renders edit button per row | `onMounted` | MSW returns settings | Each row has an edit button |
| Click edit on STRING setting shows inline text input | `@click` on edit button | STRING type setting | Inline `<input type="text">` appears with current value |
| Click edit on NUMBER setting shows inline number input | `@click` on edit button | NUMBER type setting | Inline `<input type="number">` (or text) appears with current value |
| Click edit on BOOLEAN setting shows toggle switch | `@click` on edit button | BOOLEAN type setting | Toggle switch (not text input) appears reflecting current boolean value |
| Save changed STRING value calls updateSettingValue | `@click` on save after editing | Value changed; save clicked; MSW 200 | `updateSettingValue` called with `{ key, value: newValue }`; input collapses |
| Save changed BOOLEAN value calls updateSettingValue | toggle switch changed + save | Boolean toggled; MSW 200 | `updateSettingValue` called with `{ key, value: newBooleanValue }` |
| Save changed NUMBER value calls updateSettingValue | inline input changed + save | Number changed; MSW 200 | `updateSettingValue` called with correct numeric value |
| Unsaved changes indicator appears when value modified | `@input` on inline field | Value typed but not yet saved | Unsaved changes indicator element visible in DOM |
| Unsaved changes indicator disappears after save | `@click` save | After save completes | Unsaved changes indicator no longer visible |
| "Batch Update" button sends all changed values | `@click` on Batch Update | Multiple settings modified; Batch Update clicked; MSW 200 | `updateSettings` called with array/object containing all changed key-value pairs |
| Batch Update does not send unchanged settings | `@click` on Batch Update | Only one setting modified | `updateSettings` payload contains only the modified setting |

---

## TerminalsView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Fetches terminals on mount | `onMounted` | MSW returns terminal list | API called; terminal rows rendered |
| Renders code column | `onMounted` | MSW returns terminal with `code: "T-01"` | `"T-01"` visible in table |
| Renders name column | `onMounted` | MSW returns terminal with `name: "Main Terminal"` | `"Main Terminal"` visible |
| Renders location column | `onMounted` | MSW returns terminal with `location: "Branch A"` | `"Branch A"` visible |
| Renders active status | `onMounted` | MSW returns terminal with `status: "ACTIVE"` | Active status indicator visible |
| Renders inactive status | `onMounted` | MSW returns terminal with `status: "INACTIVE"` | Inactive status indicator visible |
| "Create Terminal" button navigates | `@click` | Button clicked | `router.push("/admin/terminal-form")` called |
| Edit button navigates with id | `@click` on edit | Row with `id: 10`; edit clicked | `router.push("/admin/terminal-form?id=10")` called |
| Activate toggle calls activateTerminal | `@click` on toggle | Terminal with `status: "INACTIVE"`; toggle clicked; MSW 200 | `activateTerminal` called with terminal id; status updated to active |
| Deactivate toggle calls deactivateTerminal | `@click` on toggle | Terminal with `status: "ACTIVE"`; toggle clicked; MSW 200 | `deactivateTerminal` called with terminal id; status updated to inactive |

---

## TerminalFormView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Renders empty code field in create mode | `onMounted` | No id param | `input[name="code"]` present with empty value |
| Renders empty name field in create mode | `onMounted` | No id param | `input[name="name"]` present with empty value |
| Renders location dropdown in create mode | `onMounted` | No id param | Location `<select>` or dropdown component present |
| Loads locations from API on mount | `onMounted` | MSW returns locations list | Locations API called; dropdown options populated |
| Submit create calls createTerminal and redirects | `@submit` | Valid data; MSW 201 | `createTerminal` called with payload; `router.push("/admin/terminals")` called |
| Submit edit calls updateTerminal and redirects | `@submit` | Edit mode; valid data; MSW 200 | `updateTerminal` called with id and payload; `router.push("/admin/terminals")` called |
| Pre-fills code in edit mode | `onMounted` | MSW returns terminal with `code: "T-02"` | Code input has value `"T-02"` |
| Pre-fills name in edit mode | `onMounted` | MSW returns terminal with `name: "East Terminal"` | Name input has value `"East Terminal"` |
| Pre-selects location in edit mode | `onMounted` | MSW returns terminal with `locationId: 3` | Location dropdown shows location id 3 selected |
| Duplicate code (409) shows error | `@submit` | MSW returns 409 | Error message visible; navigation not called |

---

## RegionsView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Lists regions on mount | `onMounted` | MSW returns regions list | Regions API called; region rows rendered |
| Renders region name | `onMounted` | MSW returns region with `name: "North Region"` | `"North Region"` visible in table |
| "Create Region" button navigates | `@click` | Button clicked | `router.push("/admin/region-form")` called |
| Edit button navigates with id | `@click` on edit | Row with `id: 2`; edit clicked | `router.push("/admin/region-form?id=2")` called |
| Delete shows confirmation dialog | `@click` on delete | Delete clicked on a row | Confirmation dialog visible |
| Confirm delete calls deleteRegion and removes row | `@click` confirm Yes | MSW handles deleteRegion | `deleteRegion` called with regionId; row removed |
| Cancel delete makes no API call | `@click` confirm Cancel | Dialog open; Cancel clicked | No API call; row remains |

---

## RegionFormView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Renders empty name field in create mode | `onMounted` | No id param | Name input present with empty value |
| Renders active checkbox in create mode | `onMounted` | No id param | Active checkbox present (default checked or unchecked per spec) |
| Create calls createRegion and redirects | `@submit` | Valid name; MSW 201 | `createRegion` called with payload; `router.push("/admin/regions")` called |
| Edit loads data on mount | `onMounted` | Route has `id=4`; MSW returns region | Region API called with `4`; fields pre-filled |
| Pre-fills name in edit mode | `onMounted` | MSW region `name: "South Region"` | Name input value is `"South Region"` |
| Pre-checks active checkbox when region is active | `onMounted` | MSW region `active: true` | Active checkbox is checked |
| Pre-unchecks active checkbox when region is inactive | `onMounted` | MSW region `active: false` | Active checkbox is unchecked |
| Edit calls updateRegion and redirects | `@submit` | Valid data; MSW 200 | `updateRegion` called with id and payload; `router.push("/admin/regions")` called |

---

## VillagesView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Lists villages on mount | `onMounted` | MSW returns villages list | Villages API called; village rows rendered |
| Renders village name | `onMounted` | MSW returns village with `name: "Yangi Hayot"` | `"Yangi Hayot"` visible in table |
| Region filter dropdown present | `onMounted` | Component mounts | Region filter `<select>` or dropdown visible |
| Selecting region filter updates village list | `@change` on region filter | Region selected; MSW returns filtered villages | API called with `regionId` param; table updated |
| Clearing region filter shows all villages | `@change` on region filter | Region filter cleared | API called without regionId; all villages shown |
| "Create Village" button navigates | `@click` | Button clicked | `router.push("/admin/village-form")` called |
| Edit button navigates with id | `@click` on edit | Row with `id: 8`; edit clicked | `router.push("/admin/village-form?id=8")` called |
| Delete shows confirmation dialog | `@click` on delete | Delete clicked | Confirmation dialog visible |
| Confirm delete calls deleteVillage and removes row | `@click` confirm Yes | MSW handles deleteVillage | `deleteVillage` called; row removed |
| Cancel delete makes no API call | `@click` confirm Cancel | Dialog open; Cancel clicked | No API call; row remains |

---

## VillageFormView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Renders empty name field in create mode | `onMounted` | No id param | Name input present and empty |
| Renders region dropdown in create mode | `onMounted` | No id param | Region `<select>` or dropdown present |
| Loads regions from API on mount | `onMounted` | MSW returns regions | Regions API called; dropdown options populated |
| Renders active checkbox | `onMounted` | No id param | Active checkbox present |
| Create calls createVillage and redirects | `@submit` | Valid data with region selected; MSW 201 | `createVillage` called with payload; `router.push("/admin/villages")` called |
| Edit loads village data on mount | `onMounted` | Route has `id=9`; MSW returns village | Village API called; fields pre-filled |
| Pre-fills name in edit mode | `onMounted` | MSW village `name: "Old Town"` | Name input value is `"Old Town"` |
| Pre-selects region in edit mode | `onMounted` | MSW village `regionId: 2` | Region dropdown shows region 2 selected |
| Pre-checks active in edit mode | `onMounted` | MSW village `active: true` | Active checkbox is checked |
| Edit calls updateVillage and redirects | `@submit` | Valid data; MSW 200 | `updateVillage` called; `router.push("/admin/villages")` called |
| Region required validation error if not selected | `@submit` | Region dropdown left unselected | Validation error for region visible; API not called |

---

## SmsAdminView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Lists SMS templates on mount | `onMounted` | MSW returns templates list | Templates API called; template entries rendered |
| Renders template name | `onMounted` | MSW returns template with `name: "Welcome"` | `"Welcome"` visible in list |
| Renders template code | `onMounted` | MSW returns template with `code: "WELCOME_MSG"` | `"WELCOME_MSG"` visible |
| "Create Template" button shows create form/modal | `@click` on Create Template | Button clicked | Inline form or modal with name, code, body fields becomes visible |
| Create form contains name field | `@click` on Create Template | Form/modal opened | Name input visible in form |
| Create form contains code field | `@click` on Create Template | Form/modal opened | Code input visible in form |
| Create form contains body field | `@click` on Create Template | Form/modal opened | Body textarea visible in form |
| Body field shows character count | `@input` on body textarea | User types into body | Character count indicator updates in real time |
| Body field shows placeholder syntax hint | `onMounted` (or form open) | Form visible | Hint text containing `{variable}` or similar placeholder syntax visible |
| Submit create calls createTemplate and adds to list | `@submit` | Valid name/code/body; MSW 201 | `createTemplate` called; new template appears in list without full page reload |
| Edit template opens pre-filled modal | `@click` on edit | MSW returns template data | Modal/form appears with existing name, code, body pre-filled |
| Edit submit calls updateTemplate | `@submit` in edit modal | Updated body; MSW 200 | `updateTemplate` called with id and new payload; list updated |
| Delete shows confirmation dialog | `@click` on delete | Delete clicked on a template | Confirmation dialog visible |
| Confirm delete calls deleteTemplate | `@click` confirm Yes | MSW handles deleteTemplate | `deleteTemplate` called; template removed from list |
| Cancel delete makes no API call | `@click` confirm Cancel | Dialog shown; Cancel clicked | No API call; template remains |
| "Send Test SMS" section present | `onMounted` | Component mounts | Send Test SMS section with phone input and message field visible |
| Send Test SMS calls sendSms | `@click` send test | Phone and message filled; MSW 200 | `sendSms` called with phone and message; success toast shown |

---

## TelegramAdminView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Calls status API on mount | `onMounted` | MSW returns bot status | Status API called once on mount |
| Renders CONNECTED status badge (green) | `onMounted` | MSW returns `{ status: "CONNECTED" }` | Green badge / indicator with text `"CONNECTED"` visible |
| Renders DISCONNECTED status badge (red) | `onMounted` | MSW returns `{ status: "DISCONNECTED" }` | Red badge / indicator with text `"DISCONNECTED"` visible |
| "Send Message" section has userId input | `onMounted` | Component mounts | userId input field present in Send Message section |
| "Send Message" section has message textarea | `onMounted` | Component mounts | Message `<textarea>` present |
| Submit send message calls sendTelegramMessage | `@click` send | userId and message filled; MSW 200 | `sendTelegramMessage` called with `{ userId, message }`; success toast shown |
| Invalid userId (404) shows error | `@click` send | MSW returns 404 for sendTelegramMessage | Error message visible; no success toast |
| "Send Daily Report" button present | `onMounted` | Component mounts | Send Daily Report button visible |
| "Send Daily Report" calls sendReport | `@click` | Button clicked; MSW 200 | `sendReport` API called; success toast shown |
| sendReport API error shows error message | `@click` | MSW returns 500 for sendReport | Error message visible; no success toast |

---

# Section 6b-i: Frontend — Inventory Views — Test Plan

**Testing Stack:** Vitest + @vue/test-utils + MSW  
**Coverage Goal:** 100% component test coverage for all inventory-related Vue.js views  
**Platform:** Hisobnoma SaaS

---

## Overview

This section covers unit and integration-level component tests for every view under the Inventory module. Each test is designed to be executed in a Vitest environment with `@vue/test-utils` for DOM mounting/interaction and MSW (Mock Service Worker) for intercepting HTTP calls at the network layer. All tests are isolated per component; shared MSW handlers are reset between tests via `afterEach`.

---

## ProductsView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `mount calls getProducts with page=0` | `onMounted` | Component mounts with no route params | MSW handler for `GET /products?page=0` is called exactly once; table renders with returned rows |
| `table renders required columns` | Render | MSW returns a product list with all fields populated | Table headers contain SKU, Name, Category, Brand, Price, Status, Actions; each row cell maps to the correct field |
| `search input triggers debounced API call` | `input` event on search field | User types "Widget" into the search input; wait 300 ms debounce | `GET /products?search=Widget&page=0` is called once; previous full-list rows are replaced by filtered results |
| `rapid typing only triggers one debounced call` | Multiple `input` events in quick succession | User types "W", "Wi", "Wid" within 100 ms each | Only one API call fires after debounce settles; intermediate queries are not dispatched |
| `Active Only toggle calls getActiveProducts` | `change` event on toggle | User enables the "Active Only" toggle | `GET /products/active` is called; rows with status `INACTIVE` are absent from the rendered table |
| `Active Only off reverts to full list` | `change` event on toggle (disable) | User disables the "Active Only" toggle after enabling it | `GET /products?page=0` is called again; inactive rows reappear |
| `click product row navigates to detail` | `click` on table row | User clicks the row for product with id `42` | `router.push` is called with `/inventory/products/42` |
| `Create Product button navigates to create form` | `click` on "Create Product" button | User clicks the "Create Product" button | `router.push` is called with `/inventory/products/create` |
| `deactivate button shows confirmation dialog` | `click` on Deactivate action button for a row | User clicks the Deactivate button on an active product row | A confirmation dialog (modal or native confirm) is rendered/visible; no API call has been made yet |
| `confirm deactivate calls API and shows Inactive badge` | `click` confirm in dialog | User confirms the deactivation dialog | `PATCH /products/42/deactivate` is called; the row's status cell now contains an "Inactive" badge element |
| `cancel deactivate makes no API call` | `click` cancel in dialog | User dismisses the confirmation dialog | No `PATCH /products/.*/deactivate` request is recorded by MSW; row status badge remains "Active" |
| `pagination next calls getProducts with page=1` | `click` on Next page control | Current page is 0; user clicks Next | `GET /products?page=1` is called; table rows update to page-2 data |
| `pagination prev calls getProducts with page=0` | `click` on Previous page control | Current page is 1; user clicks Previous | `GET /products?page=0` is called; table rows update to page-1 data |
| `empty list shows empty state message` | Render after mount | MSW returns `{ data: [], total: 0 }` | Element with text "No products found" is present in the DOM; table body has no `<tr>` data rows |

---

## ProductFormView.vue — Create Mode

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `renders four tabs in create mode` | Render | Component mounted without an `id` route param | Tab elements or tab-panel headers labelled General, Images, UOMs, Vendors are all present in the DOM |
| `General tab contains all required fields` | Render (General tab active by default) | Component mounted in create mode | Inputs/selects for name, SKU, barcode, category, brand, description, sale price, cost price; checkboxes for is_sellable and is_purchasable are all present |
| `SKU field shows Auto-generated placeholder` | Render | General tab rendered in create mode | SKU input has `placeholder` attribute equal to "Auto-generated" |
| `category dropdown loads from getCategoryTree on mount` | `onMounted` | Component mounts | `GET /categories/tree` is called once; category options in the dropdown match the MSW-returned tree leaf names |
| `brand dropdown loads from getActiveBrands on mount` | `onMounted` | Component mounts | `GET /brands/active` is called once; brand options match MSW-returned brand list |
| `submit valid data calls createProduct and redirects` | `submit` form event | All required fields filled; user submits | `POST /products` is called with correct payload; `router.push('/inventory/products')` is invoked |
| `submit with empty name shows validation error` | `submit` form event | Name field is left empty; user submits | No API call is made; element containing text "Name is required" is visible in the DOM |
| `API 409 on duplicate SKU shows error message` | `submit` form event | MSW returns 409 for `POST /products`; payload contains duplicate SKU | Error message "SKU already exists" is visible; user remains on the form |
| `Images tab: file input renders and accepts files` | `click` Images tab, then `change` on file input | User switches to Images tab; selects two image files | Two thumbnail preview elements are rendered in the images grid |
| `upload button calls uploadImage API` | `click` upload button after file selected | User selects a file and clicks the upload button | `POST /products/{id}/images` (or equivalent upload endpoint) is called; returned image appears in the grid with a delete icon |
| `delete image calls deleteImage and removes from grid` | `click` delete icon on an image | Uploaded image is present in grid; user clicks its delete icon | `DELETE /products/{id}/images/{imageId}` is called; the image element is removed from the grid |
| `Set Primary calls setPrimaryImage and shows star icon` | `click` "Set Primary" button on an image | Multiple images present; user clicks "Set Primary" on image with id `7` | `PATCH /products/{id}/images/7/primary` is called; a star icon class/attribute is applied to that image element and removed from others |
| `UOMs tab shows base UOM read-only and Add UOM row` | `click` UOMs tab | Component rendered in create mode; MSW returns a base UOM | Base UOM row is rendered as read-only (no editable input); "Add UOM" row with a UOM dropdown and conversion factor input is present |
| `Save UOM row calls addUom and appends new row` | `click` save on Add UOM row | User selects a UOM and enters conversion factor `2.5`, then saves | `POST /products/{id}/uoms` called with correct payload; a new row containing the selected UOM appears in the table |
| `Remove UOM row calls removeUom` | `click` remove on a non-base UOM row | A non-base UOM row exists; user clicks its remove button | `DELETE /products/{id}/uoms/{uomId}` is called; the row is removed from the DOM |
| `Remove button disabled on base UOM row` | Render | UOMs tab rendered with a base UOM row | The remove/delete button on the base UOM row has a `disabled` attribute or is absent |
| `Vendors tab Add Vendor saves and shows row` | `click` save on Add Vendor row | User selects a vendor and enters a price, then saves | `POST /products/{id}/vendors` called with vendor id and price; new vendor row appears in the list |
| `Remove vendor calls removeVendor` | `click` remove on vendor row | A vendor row exists; user clicks its remove button | `DELETE /products/{id}/vendors/{vendorId}` is called; the vendor row is removed |

---

## ProductFormView.vue — Edit Mode

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `getProduct called on mount with route id` | `onMounted` | Component mounted with route param `id=99` | `GET /products/99` is called exactly once on mount |
| `General fields pre-filled with product data` | Render after mount | MSW returns product with name "Bolt M8", SKU "BLT-M8", price 1.50 | Name input value is "Bolt M8"; SKU input value is "BLT-M8"; price input value is "1.50" |
| `category and brand dropdowns show pre-selected values` | Render after mount | MSW returns product with category id `3` and brand id `7` | Category dropdown selected option matches the name for id 3; brand dropdown selected option matches the name for id 7 |
| `Images tab shows existing images with correct primary indicator` | `click` Images tab after mount | MSW returns product with two images; image id `2` has `is_primary: true` | Two image thumbnails rendered; only the thumbnail for id `2` carries the primary/star indicator class |
| `UOMs tab shows existing UOM rows` | `click` UOMs tab after mount | MSW returns product with 2 UOM associations | Two UOM rows rendered; base UOM row remove button is disabled; non-base UOM remove button is enabled |
| `Vendors tab shows existing vendor rows` | `click` Vendors tab after mount | MSW returns product with 2 vendor links | Two vendor rows rendered with vendor name and price populated correctly |
| `submit in edit mode calls updateProduct and redirects` | `submit` form event | User edits name to "Bolt M10" and submits | `PUT /products/99` called with updated payload; `router.push('/inventory/products')` is invoked |
| `API 404 on mount shows error and redirects` | `onMounted` | MSW returns 404 for `GET /products/99` | An error message is rendered briefly; `router.push('/inventory/products')` is called |

---

## CategoriesView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `getCategoryTree called on mount` | `onMounted` | Component mounts | `GET /categories/tree` is called exactly once |
| `root categories rendered at top level` | Render after mount | MSW returns two root categories with no parent | Two top-level category name elements are visible without indentation class |
| `child categories rendered indented under parent` | Render after mount | MSW returns root category with two children | Child category elements carry an indentation/nesting CSS class or are nested in the DOM under their parent |
| `expand chevron shows hidden children` | `click` on collapse chevron of a root category | Root category is initially collapsed (children hidden); user clicks chevron | Children elements become visible; chevron icon rotates or changes direction |
| `collapse chevron hides visible children` | `click` on expand chevron of an already-expanded root category | Children are visible; user clicks chevron again | Children elements are hidden from the DOM or have a hidden CSS class |
| `Add Root Category shows inline form at top` | `click` "Add Root Category" button | No form row visible initially; user clicks the button | An inline form row appears at the top of the category tree with a name text input and a save control |
| `submit root category calls createCategory without parent` | `submit` inline form at root level | User types "Hardware" in the inline input and saves | `POST /categories` called with `{ name: "Hardware", parentId: null }`; tree refreshes via `GET /categories/tree` |
| `Add Subcategory shows indented inline form` | `click` "Add Subcategory" on a root category row | User clicks the subcategory button for category id `5` | An indented inline form row appears directly under category 5's row |
| `submit subcategory calls createCategory with parent ID` | `submit` indented inline form | User types "Bolts" and saves under parent id `5` | `POST /categories` called with `{ name: "Bolts", parentId: 5 }`; tree refreshes |
| `edit category saves updated name` | `click` edit, then `submit` inline name input | User clicks edit on a category, changes name to "Fasteners", and saves | `PUT /categories/5` called with `{ name: "Fasteners" }`; tree row updates to display new name |
| `delete category shows confirmation then calls deleteCategory` | `click` delete, then confirm | User clicks delete on a leaf category; confirms the dialog | `DELETE /categories/5` is called; the category row is removed from the tree |
| `delete category with children shows API 422 error` | `click` delete, confirm | MSW returns 422 for `DELETE /categories/3` (has children) | Error message "Cannot delete category with subcategories" is visible; tree remains unchanged |

---

## BrandsView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `loads paginated brands on mount` | `onMounted` | Component mounts | `GET /brands?page=0` is called; brand rows appear in the list/table |
| `search by name triggers debounced API call` | `input` on search field | User types "Acme" into the search input; waits for debounce | `GET /brands?search=Acme&page=0` is called once; list updates to matching brands |
| `Create Brand button opens modal with name field` | `click` "Create Brand" button | Modal is initially closed; user clicks the button | A modal dialog becomes visible containing a name text input and a submit control |
| `submit brand name calls createBrand and closes modal` | `submit` inside modal | User enters "Ridgid" in the name field and submits | `POST /brands` called with `{ name: "Ridgid" }`; modal closes; new brand row appears in the list |
| `duplicate brand name shows error inside modal` | `submit` inside modal | MSW returns 409 for `POST /brands` | Error message is rendered inside the modal (modal does not close); the duplicate name is highlighted |
| `edit brand opens modal pre-filled` | `click` edit button on brand row | Brand row with id `8`, name "Bosch" exists; user clicks its edit button | Modal opens with the name field already populated with "Bosch" |
| `edit brand submit calls updateBrand` | `submit` inside pre-filled edit modal | User changes name to "Bosch Tools" and submits | `PUT /brands/8` called with `{ name: "Bosch Tools" }`; modal closes; row updates to "Bosch Tools" |
| `delete brand shows confirmation then calls deleteBrand` | `click` delete, then confirm | User clicks delete on brand id `8`; confirms | `DELETE /brands/8` is called; brand row is removed from the list |
| `delete brand with products shows API 422 error` | `click` delete, confirm | MSW returns 422 for `DELETE /brands/8` | An error message is displayed (inline or toast); brand row remains in the list; modal/dialog closes or resets |
| `pagination next calls brands API with page=1` | `click` Next pagination control | Current page is 0; user clicks Next | `GET /brands?page=1` is called; list updates to page-2 brands |

---

## StockView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `getStock called on mount` | `onMounted` | Component mounts | `GET /stock` is called exactly once |
| `table renders required columns` | Render after mount | MSW returns a stock record with all fields | Table headers and corresponding cells for Product Name, Location, Qty on Hand, Reserved Qty, Available Qty, Unit Cost are all present |
| `low stock rows show orange badge` | Render after mount | MSW returns record with `qty_on_hand` below low-stock threshold | That row contains an element with a class or text indicating low stock; the element has an orange/warning visual indicator |
| `out of stock rows show red badge` | Render after mount | MSW returns record with `qty_on_hand = 0` | That row contains an element indicating out of stock with a red/danger visual indicator |
| `product filter calls getStockByProduct` | `input` or `change` on product filter | User selects or types a product name/id in the product filter | `GET /stock?productId={id}` is called; table rows narrow to that product's stock records |
| `location filter calls getStockByLocation` | `change` on location dropdown filter | User selects location "SHELF-A1" | `GET /stock?locationId={id}` is called; table rows narrow to that location's records |
| `View Movements expands row and loads history` | `click` "View Movements" on a stock row | Expansion section is initially hidden; user clicks the button for product `p1` at location `l1` | `GET /movements?productId=p1` is called; a section below the row expands and shows movement history entries |
| `View Movements collapses on second click` | `click` "View Movements" again on the same row | Expansion is visible; user clicks the button again | The expanded section collapses/hides; no additional API call is made |
| `Adjust Stock modal opens with qty and reason fields` | `click` "Adjust Stock" on a row | Modal is initially closed | Modal becomes visible with a numeric adjustment quantity input (accepts positive/negative values) and a reason text field |
| `submit adjustment calls adjustStock and updates row qty` | `submit` in adjustment modal | User enters `10` in qty and "Cycle count" in reason; submits | `POST /stock/adjust` called with product, location, qty, reason; modal closes; the row's Qty on Hand cell updates to reflect the new quantity |
| `Transfer modal opens with source pre-filled` | `click` "Transfer" on a row | Modal is initially closed; row belongs to location "WH-MAIN" | Modal opens; source location field is pre-filled with "WH-MAIN"; destination location dropdown is editable; qty input is present |
| `submit transfer calls transferStock` | `submit` in transfer modal | User selects destination "SHELF-A1" and qty `5`; submits | `POST /stock/transfer` called with source, destination, product, and qty payload; modal closes |

---

## WarehousesView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `loads all locations on mount` | `onMounted` | Component mounts | `GET /locations` is called exactly once; location rows appear in the table |
| `table renders required columns` | Render after mount | MSW returns a location with all fields | Table cells for Code, Name, Type, and Active Status are all present in each row |
| `filter by type calls getLocationsByType` | `change` on type filter dropdown | User selects type "SHELF" | `GET /locations?type=SHELF` is called; table updates to show only SHELF-type rows |
| `Create Location button opens modal` | `click` "Create Location" button | Modal initially closed | Modal becomes visible with inputs for code, name, type selector, and description |
| `submit create location calls createLocation and adds row` | `submit` in create modal | User fills in code "BIN-001", name "Bin 1", type "BIN", and submits | `POST /locations` called with the correct payload; modal closes; new row for "BIN-001" appears in the table |
| `edit location opens modal pre-filled` | `click` edit on an existing location row | Location with id `3`, code "WH-MAIN" exists | Modal opens with code field containing "WH-MAIN" and all other fields populated from the record |
| `edit submit calls updateLocation` | `submit` inside pre-filled edit modal | User changes name to "Main Warehouse" and submits | `PUT /locations/3` called with updated payload; modal closes; row updates to "Main Warehouse" |
| `delete location shows confirmation then calls deleteLocation` | `click` delete, then confirm | User clicks delete on location id `3`; confirms | `DELETE /locations/3` is called; the row is removed from the table |
| `delete location with stock shows API 422 error` | `click` delete, confirm | MSW returns 422 for `DELETE /locations/3` | Error message "Cannot delete location with existing stock" is visible; row remains in the table |
| `active status badge reflects location state` | Render after mount | MSW returns one active and one inactive location | Active row shows an "Active" badge; inactive row shows an "Inactive" badge with distinct styling |

---

## UOMView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `loads all UOMs on mount` | `onMounted` | Component mounts | `GET /uoms` is called exactly once; UOM rows appear in the table |
| `table renders required columns` | Render after mount | MSW returns UOMs with all fields | Table cells for Code, Name, Type, and Is Base UOM indicator are all present in each row |
| `base UOM row shows badge` | Render after mount | MSW returns one UOM with `is_base: true` | That row contains a badge or icon element indicating it is the base UOM |
| `Create UOM button opens modal` | `click` "Create UOM" button | Modal is initially closed | Modal becomes visible with inputs for code, name, and an "Is Base" checkbox |
| `submit create UOM calls createUom and adds row` | `submit` in create modal | User fills in code "KG", name "Kilogram", leaves Is Base unchecked, and submits | `POST /uoms` called with `{ code: "KG", name: "Kilogram", is_base: false }`; modal closes; new row for "KG" appears in the table |
| `edit UOM opens modal pre-filled` | `click` edit on an existing UOM row | UOM with id `2`, code "PCS" exists | Modal opens with code field "PCS" and all other fields populated |
| `edit UOM submit calls updateUom` | `submit` inside pre-filled edit modal | User changes name to "Pieces (each)" and submits | `PUT /uoms/2` called with updated payload; modal closes; row updates to show new name |
| `delete non-base UOM shows confirmation then calls deleteUom` | `click` delete, then confirm | Non-base UOM row with id `2` exists; user clicks delete and confirms | `DELETE /uoms/2` is called; the row is removed from the table |
| `delete button disabled for base UOM` | Render after mount | MSW returns a base UOM row | The delete/remove button on the base UOM row has a `disabled` attribute or is absent from the DOM |
| `disabled base UOM delete button has tooltip` | Hover or `title`/`aria-label` check on disabled button | Base UOM row rendered; inspect the disabled delete trigger | The element has a tooltip, `title`, or `aria-label` containing "Cannot delete base UOM" |
| `delete UOM in use by products shows API 422 error` | `click` delete, confirm | MSW returns 422 for `DELETE /uoms/2` | An error message is displayed indicating the UOM is in use; the row remains in the table |

---

## Shared Test Setup Notes

The following setup applies to all views in this section and should be placed in a shared `vitest.setup.ts` or per-test `beforeAll`/`afterEach` hooks.

```typescript
// vitest.setup.ts (excerpt)
import { setupServer } from 'msw/node'
import { handlers } from './mocks/handlers'

const server = setupServer(...handlers)

beforeAll(() => server.listen({ onUnhandledRequest: 'warn' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())
```

**Router stub:** Use `createRouter({ history: createMemoryHistory(), routes })` and pass it to `mount` via the `global.plugins` option. Assert navigation via `router.currentRoute.value.path`.

**Debounce testing:** Use `vi.useFakeTimers()` before each debounce test and `vi.advanceTimersByTime(300)` (or the configured debounce value) to trigger the call synchronously. Restore with `vi.useRealTimers()` in `afterEach`.

**Dialog/confirmation testing:** If the app uses `window.confirm`, stub it with `vi.spyOn(window, 'confirm').mockReturnValue(true)` (or `false` for cancel tests). If a custom modal component is used, interact with its confirm button via `wrapper.find('[data-testid="confirm-btn"]').trigger('click')`.

**Coverage collection:** Run with `vitest --coverage` using the `@vitest/coverage-v8` provider. The coverage threshold for all files under `src/views/inventory/` should be set to `100` for statements, branches, functions, and lines in `vitest.config.ts`.

---

# Section 6b-ii: Frontend — POS Views — Test Plan

**Stack:** Vitest + @vue/test-utils + MSW (Mock Service Worker)
**Coverage target:** 100% component test coverage for all POS-related Vue.js views
**Conventions used in this section:**
- "mount" = `mount()` via `@vue/test-utils` with a Pinia store and Vue Router stub
- MSW handlers intercept all API calls at the network layer; each test seeds its own handler overrides via `server.use(...)`
- "trigger" = `wrapper.find(...).trigger('...')` + `await nextTick()` unless stated otherwise
- Debounce tests use `vi.useFakeTimers()` + `vi.advanceTimersByTime(n)` to settle the timer synchronously; `vi.useRealTimers()` restored in `afterEach`
- Dialog/confirm tests stub `window.confirm` via `vi.spyOn(window, 'confirm')` or interact with a custom modal's `[data-testid="confirm-btn"]`
- Each table row represents one `it()` block

---

## POSView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `mount with no open shift shows undismissable Open Shift modal` | `onMounted` / `getCurrentShift` | MSW returns 404 or empty on `GET /shifts/current`; no open shift exists | Open Shift modal element is present and visible in DOM; backdrop has no close handler; POS interface controls (product search, cart, payment panel) are absent or disabled |
| `Open Shift modal cannot be dismissed without submitting` | `keydown Escape` + click outside modal | Open Shift modal is displayed | Pressing Escape and clicking the modal backdrop do not close the modal; modal remains visible |
| `Open Shift modal submit calls openShift and enables POS interface` | `click` submit button in Open Shift modal | User fills required fields and submits; MSW returns new shift on `POST /shifts` | `openShift` called once with correct payload; modal element removed from DOM; product search input, cart area, and Complete Sale button are now enabled/visible |
| `product search is inactive before shift is open` | Render | MSW returns no open shift on mount | Product search input is absent or has `disabled` attribute while Open Shift modal is displayed |
| `product search with 3+ chars triggers debounced searchProducts` | `input` event on product search field | User types "mil" and debounce settles; MSW returns matching products | `GET /products/search?q=mil` called exactly once after debounce; results dropdown is visible containing matched product entries |
| `product search with fewer than 3 chars does not call API` | `input` event on product search field | User types "mi" (2 chars) | No API call is made; results dropdown is absent |
| `rapid typing only triggers one debounced search call` | Multiple `input` events in quick succession | User types "m", "mi", "mil" within 100 ms each | Only one API call fires after debounce settles; intermediate queries not dispatched |
| `product search dropdown shows name, price, and stock for each result` | Render after search | MSW returns products with name, unitPrice, and stockQty | Each dropdown item contains the product name, formatted unit price, and stock quantity |
| `selecting product from dropdown calls addLine and shows cart row` | `click` on a dropdown item | Dropdown is visible with one result; user clicks it | `addLine` called with the selected product; a new cart row appears showing the product name, unit price, quantity 1, and calculated line total |
| `selecting same product again increments qty instead of duplicating row` | `click` same dropdown item a second time | Cart already contains a row for the product | Row count does not increase; the existing row's quantity becomes 2 and line total updates accordingly |
| `qty input change in cart row calls updateLine and recalculates line total` | `input` event on qty field in cart row | Cart has one row with unitPrice=15000; user changes qty from 1 to 3 | `updateLine` called with new qty; line total cell immediately shows 45000 |
| `qty set to 0 or blank is rejected with validation feedback` | `input` event on qty field | User clears qty input or enters 0 | Qty field shows validation error or reverts to 1; `updateLine` not called with invalid qty |
| `trash icon on cart row calls removeLine and removes row` | `click` trash icon on a cart row | Cart has two rows; user clicks the trash icon on the first row | `removeLine` called with the correct line id; that row is removed; remaining row is still present |
| `coupon input + Apply with valid code calls validateCoupon and shows discount row` | `click` Apply coupon button | User types "SAVE10" in coupon input; MSW returns valid coupon with 10% discount | `validateCoupon` called with "SAVE10"; a discount row appears in the totals section showing the discount label and computed discount amount |
| `applying valid coupon does not close or disrupt cart rows` | `click` Apply coupon button | Cart has two items; coupon applied successfully | Cart row count unchanged; only totals section updates to include discount row |
| `expired coupon code shows "Coupon expired" error` | `click` Apply coupon button | MSW returns 422 with `{ code: "COUPON_EXPIRED" }` for the coupon code | Error message "Coupon expired" is visible in or near the coupon input area; no discount row added to totals |
| `invalid coupon code shows "Invalid coupon" error` | `click` Apply coupon button | MSW returns 404 or 422 with `{ code: "COUPON_INVALID" }` | Error message "Invalid coupon" is visible; no discount row added to totals |
| `error message clears when user modifies coupon input` | `input` event on coupon field after error | Error "Invalid coupon" shown; user starts typing a new code | Error message element is removed from DOM |
| `CASH tab selected + amount entered + Complete Sale calls addPayment then completeTransaction` | `click` CASH tab, `input` cash amount, `click` Complete Sale | Cart has items totalling 50000; user enters 60000 in cash amount field; MSW returns success | `addPayment` called with `{ method: "CASH", amount: 60000 }`; `completeTransaction` called; success screen element becomes visible |
| `success screen shows correct change amount` | Render after completeTransaction | Transaction total=50000; cash tendered=60000 | Change due label displays 10000 (or formatted equivalent) |
| `success screen shows Print Receipt button` | Render after completeTransaction | Successful sale completed | Element with text "Print Receipt" is present and enabled in DOM |
| `success screen shows New Sale button` | Render after completeTransaction | Successful sale completed | Element with text "New Sale" is present and enabled in DOM |
| `underpayment shows "Insufficient payment amount" error` | `click` Complete Sale | Cart total=50000; cash amount entered=30000 | Error message "Insufficient payment amount" is visible; `addPayment` / `completeTransaction` not called; cart and payment form remain active |
| `New Sale button calls createTransaction and resets all state` | `click` "New Sale" on success screen | Success screen displayed | `createTransaction` called; cart rows cleared (empty cart); coupon input cleared; product search field cleared; success screen hidden; POS interface in initial ready state |
| `Complete Sale button is disabled when cart is empty` | Render | Cart has no line items | Complete Sale button has `disabled` attribute; clicking it triggers no API call |
| `Complete Sale button becomes enabled when cart has items` | Render after adding item | Cart gains at least one line | Complete Sale button `disabled` attribute removed; button is interactive |
| `Void Transaction button shows confirmation dialog` | `click` Void Transaction | Cart has items; transaction is in progress | A confirmation dialog or modal becomes visible; cart rows not yet cleared |
| `confirming void dialog calls voidTransaction and clears cart` | `click` confirm in void dialog | Confirmation dialog visible; user confirms | `voidTransaction` called; cart rows cleared; coupon cleared; product search reset |
| `cancelling void dialog leaves cart unchanged` | `click` cancel in void dialog | Confirmation dialog visible; user clicks cancel | Dialog closes; cart rows unchanged; `voidTransaction` not called |
| `Close Shift button opens modal with running totals and actual cash input` | `click` Close Shift button | Shift is open with some completed sales | Close Shift modal becomes visible; modal shows total sales count and sales amount computed from shift; actual cash input field is present and empty |
| `Close Shift modal confirm calls closeShift with actual cash amount` | `click` confirm in Close Shift modal | User enters actual cash amount in field and confirms | `closeShift` called with payload containing actual cash amount; modal closes; UI reflects shift closed state (POS interface disabled or Open Shift prompt shown) |
| `Close Shift modal cancel does not call closeShift` | `click` cancel in Close Shift modal | Close Shift modal is open; user clicks cancel | Modal closes; `closeShift` not called; POS interface remains active |

---

## TransactionsView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `getTransactions called on mount` | `onMounted` | Component mounts; MSW returns a page of transactions | `getTransactions` called exactly once on mount; table renders returned rows |
| `transactions table renders all required columns` | Render after mount | MSW returns one transaction with all fields | Table headers and corresponding row cells contain transaction number, date, cashier, total, and status |
| `status badge renders correctly for COMPLETED` | Render after mount | MSW returns a transaction with `status: "COMPLETED"` | Status cell for that row contains a badge element with text "COMPLETED" and a green/success visual style |
| `status badge renders correctly for VOIDED` | Render after mount | MSW returns a transaction with `status: "VOIDED"` | Status cell for that row contains a badge element with text "VOIDED" and a red/danger visual style |
| `date range filter change calls getTransactionsByDateRange` | `change` on date range picker | User sets a from-date and to-date | `getTransactionsByDateRange` called with the selected date range params; table updates to show filtered results |
| `status filter COMPLETED calls getTransactionsByStatus` | `change` on status filter dropdown | User selects "COMPLETED" | `getTransactionsByStatus({ status: "COMPLETED" })` called; table updates |
| `status filter VOIDED calls getTransactionsByStatus` | `change` on status filter dropdown | User selects "VOIDED" | `getTransactionsByStatus({ status: "VOIDED" })` called; table updates |
| `status filter cleared resets to all transactions` | `change` on status filter dropdown | User selects blank/all option after filtering | `getTransactions` called without status filter; all transactions shown |
| `terminal filter dropdown calls getTransactionsByTerminal` | `change` on terminal filter dropdown | User selects a specific terminal from the dropdown | `getTransactionsByTerminal({ terminalId: id })` called; table updates to show only that terminal's transactions |
| `row click opens receipt detail modal with line items` | `click` on a transaction row | Transaction has 2 line items; user clicks the row | Receipt detail modal becomes visible; modal body contains 2 line item rows showing product name and amounts |
| `receipt detail modal shows payment info` | `click` on a transaction row | Transaction paid with CASH; change amount recorded | Modal contains payment method "CASH" and change given amount |
| `receipt detail modal close button hides modal` | `click` close in modal | Receipt detail modal is open | Modal element is removed or hidden; no API call made |
| `Void button present on COMPLETED row` | Render after mount | MSW returns a COMPLETED transaction | A Void action button or icon is present in that row |
| `Void button absent on VOIDED row` | Render after mount | MSW returns a VOIDED transaction | No Void button or action element is present in that row |
| `Void button on COMPLETED row calls voidTransaction and changes badge to VOIDED` | `click` Void button | Void button clicked on COMPLETED transaction id=55; MSW returns success | `voidTransaction(55)` called; status badge in that row changes to "VOIDED"; Void button disappears from that row |
| `pagination next calls API with incremented page param` | `click` next-page control | Current page is 1; total pages > 1 | A new API call is made with `page=2`; table updates to page 2 data |
| `pagination prev calls API with decremented page param` | `click` prev-page control | Current page is 2 | A new API call is made with `page=1`; table updates to page 1 data |
| `pagination prev disabled on first page` | Render | Mount on page 1 | Previous/back page button has `disabled` attribute or is absent |
| `empty result shows "No transactions found" empty state` | Render after mount | MSW returns `{ data: [], meta: { totalPages: 0 } }` | Element containing text "No transactions found" is visible; table body has no data rows |

---

## ShiftsView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `getShifts and getCurrentShift called on mount` | `onMounted` | Component mounts; MSW returns both a current shift and a list of historical shifts | `getShifts` and `getCurrentShift` each called exactly once on mount |
| `open shift section shows green OPEN badge` | Render after mount | MSW returns an open shift | A badge element with text "OPEN" and a green/success CSS class is visible in the open-shift summary area |
| `open shift section shows terminal name` | Render after mount | Open shift has `terminalName: "Terminal 1"` | Terminal name "Terminal 1" is displayed in the open-shift section |
| `open shift section shows opened timestamp` | Render after mount | Open shift has `openedAt: "2026-04-18T08:00:00"` | The opened-at timestamp is displayed in a human-readable format in the open-shift section |
| `open shift section shows running sales total` | Render after mount | Open shift has `totalSales: 750000` | Formatted sales total (e.g. "750,000") is displayed in the open-shift section |
| `Close Shift button present on open shift section` | Render after mount | An open shift exists | "Close Shift" button is visible in the open-shift summary area |
| `Close Shift button click opens modal with total sales and cash input` | `click` Close Shift button | Open shift has total sales and a Close Shift button | Close Shift modal becomes visible; modal shows the computed total sales amount; actual cash input field is present and empty |
| `confirm close calls closeShift and marks shift CLOSED in list` | `click` confirm in Close Shift modal | User enters actual cash amount and confirms | `closeShift` called with actual cash payload; open-shift summary section is removed or shows no open shift; the closed shift appears with a "CLOSED" badge in the historical shifts table |
| `cancel close modal does not call closeShift` | `click` cancel in Close Shift modal | Close Shift modal is open; user clicks cancel | Modal closes; `closeShift` not called; open-shift section unchanged |
| `historical shifts table renders required columns` | Render after mount | MSW returns 2 historical shifts | Table headers and row cells contain shift number, terminal, cashier, opened time, closed time, and total sales |
| `historical shift CLOSED badge rendered` | Render after mount | Shift in list has `status: "CLOSED"` | Status cell for that row contains a "CLOSED" badge with a grey/neutral visual style |
| `clicking historical shift row expands transactions sub-table` | `click` on a historical shift row | Row is initially collapsed; shift has 3 transactions | A transactions sub-table expands below that row showing all 3 transactions; each sub-row shows relevant transaction info |
| `expanding sub-table calls getTransactionsByShift` | `click` on a historical shift row | Shift row clicked for shift id=8 | `getTransactionsByShift(8)` (or equivalent) called; returned transactions rendered in the expanded sub-table |
| `clicking expanded shift row again collapses sub-table` | `click` on an already-expanded shift row | Sub-table is visible | Sub-table collapses; no additional API call made |
| `expanding a different shift collapses the previously expanded one` | `click` on a second shift row | One shift row is already expanded | Previously expanded sub-table collapses; newly clicked shift's sub-table expands |
| `no open shift shows no open-shift summary section` | Render after mount | MSW returns 404 or empty for `GET /shifts/current` | Open-shift summary area (with OPEN badge and Close Shift button) is absent from DOM |
| `empty shifts list shows "No shifts yet" message` | Render after mount | MSW returns `{ data: [] }` for `GET /shifts` | Element containing text "No shifts yet" is visible; historical shifts table body has no data rows |

---

## MSW Handler Reference (shared setup)

```typescript
// vitest.setup.ts (excerpt)
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'

export const server = setupServer(
  // Shifts
  http.get('/api/shifts/current',             () => HttpResponse.json({})),
  http.post('/api/shifts',                    () => HttpResponse.json({ id: 1 }, { status: 201 })),
  http.post('/api/shifts/:id/close',          () => HttpResponse.json({})),
  http.get('/api/shifts',                     () => HttpResponse.json({ data: [], meta: { page: 1, totalPages: 1 } })),
  http.get('/api/shifts/:id/transactions',    () => HttpResponse.json({ data: [] })),

  // Transactions
  http.get('/api/transactions',               () => HttpResponse.json({ data: [], meta: { page: 1, totalPages: 1 } })),
  http.post('/api/transactions',              () => HttpResponse.json({ id: 100 }, { status: 201 })),
  http.post('/api/transactions/:id/void',     () => HttpResponse.json({})),
  http.post('/api/transactions/:id/payments', () => HttpResponse.json({})),
  http.post('/api/transactions/:id/complete', () => HttpResponse.json({ change: 0 })),

  // Products / POS search
  http.get('/api/products/search',            () => HttpResponse.json({ data: [] })),

  // Coupons
  http.post('/api/coupons/validate',          () => HttpResponse.json({ discount: 0 })),
)

beforeAll(() => server.listen({ onUnhandledRequest: 'warn' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())
```

> Each test that requires a non-default response calls `server.use(http.get(..., handler))` before mounting the component.

**Debounce testing:** Use `vi.useFakeTimers()` before each debounce test and call `vi.advanceTimersByTime(300)` (or the configured debounce interval) to fire the timer synchronously. Restore with `vi.useRealTimers()` in `afterEach`.

**Dialog/confirmation testing:** If the app uses `window.confirm`, stub it with `vi.spyOn(window, 'confirm').mockReturnValue(true)` (or `false` for cancel tests). If a custom modal component is used, click its confirm control via `wrapper.find('[data-testid="confirm-btn"]').trigger('click')`.

**Coverage collection:** Run with `vitest --coverage` using the `@vitest/coverage-v8` provider. Set statement, branch, function, and line thresholds to `100` for all files under `src/views/pos/` in `vitest.config.ts`.

---

# Section 6b-iii: Frontend — Customer & Purchase Views — Test Plan

**Stack:** Vitest + @vue/test-utils + MSW (Mock Service Worker)
**Coverage target:** 100% component test coverage for all customer- and purchase-related views
**Conventions used in this section:**
- "mount" = `mountAsync` via `@vue/test-utils` with a Pinia store and Vue Router stub
- MSW handlers intercept all API calls; each test seeds its own handler overrides
- "trigger" = `wrapper.find(...).trigger('...')` + `await nextTick()` unless stated otherwise
- "emitted" = assertion on `wrapper.emitted()`
- DOM assertions use `wrapper.text()`, `wrapper.find()`, or `wrapper.html()` as appropriate

---

## 1. CustomersView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Loads paginated customer list on mount | `onMounted` lifecycle | MSW returns 2 customers on `GET /customers?page=1` | `getCustomers` is called once; table renders exactly 2 rows with code, name, phone, email, balance, and status columns populated |
| Renders all required table columns | Render / DOM inspection | Mount with 1 seeded customer record | Table headers contain "Code", "Name", "Phone", "Email", "Balance", "Status"; row cells contain matching data values |
| Search input triggers debounced searchCustomers | User input | Type "Ahmad" into search input; advance fake timers past debounce delay | `searchCustomers` called with `{ q: 'Ahmad' }`; table re-renders with only matched rows; previous rows removed |
| Search results replace table content | User input + MSW | MSW returns 1 result for search query; 3 rows shown before search | After debounce resolves, table contains exactly 1 row matching search result |
| "Create Customer" button navigates to create route | Click | Click "Create Customer" button | `router.push` called with `'/customers/create'` |
| Edit button navigates to edit route for correct id | Click | Click edit button on row with id=7 | `router.push` called with `'/customers/7/edit'` |
| "View History" button navigates to history route | Click | Click "View History" on row with id=7 | `router.push` called with `'/customers/7/history'` |
| Delete button shows confirmation dialog | Click | Click delete button on any row | Confirmation dialog/modal becomes visible in DOM; row is not yet removed |
| Confirm delete calls deleteCustomer and removes row | Click confirm in dialog | Confirm deletion of customer id=7; MSW returns 200 | `deleteCustomer(7)` called; row for customer id=7 removed from table; success toast/notification shown |
| Delete customer with invoices shows 422 error | Click confirm in dialog | MSW returns 422 `{ message: "Cannot delete customer with invoices" }` | Error message "Cannot delete customer with invoices" visible in DOM; row remains in table |
| Cancel delete closes dialog without removing row | Click cancel in dialog | Click delete, then click cancel | Dialog closes; row count unchanged; `deleteCustomer` not called |
| Pagination next button calls API with page 2 | Click | Click "Next" page button; total pages > 1 | `getCustomers` called with `{ page: 2 }`; table updates to page 2 data |
| Pagination prev button calls API with page 1 | Click | On page 2, click "Previous" button | `getCustomers` called with `{ page: 1 }` |
| Pagination prev disabled on first page | Render | Mount on page 1 | "Previous" button is disabled or absent |
| Empty customer list shows empty state | Render | MSW returns empty array | Table body has no data rows; empty-state message or illustration visible |
| Status badge renders correct style per status | Render | Seed customers with ACTIVE and INACTIVE statuses | ACTIVE rows show green/active badge; INACTIVE rows show grey/inactive badge |

---

## 2. CustomerFormView.vue — Create Mode

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Calls getNextCustomerCode on mount | `onMounted` | MSW returns `{ nextCode: "CUST-000042" }` on `GET /customers/next-code` | `getNextCustomerCode` called once; code input placeholder reads "Auto-generated: CUST-000042" |
| Code field is editable in create mode | Render | Mount in create mode (no route id param) | Code input is not disabled; user can type into it |
| User can override auto-generated code | User input | Clear code input and type "CUST-TEST" | Input value reflects "CUST-TEST" |
| Blank code field submits without code | Submit | Leave code field empty, fill all other fields; MSW returns 201 | `createCustomer` called with payload that does not include `code` key, or includes `code: ""` |
| Custom code submitted when provided | Submit | Enter "CUST-TEST" in code field, fill required fields; MSW returns 201 | `createCustomer` called with `{ code: "CUST-TEST", ... }` |
| Duplicate code API 409 shows error | Submit | MSW returns 409 `{ message: "Customer code already exists" }` | Error message "Customer code already exists" rendered in form; no navigation occurs |
| Empty name shows validation error | Submit | Leave Name field blank, click Submit | Validation error "Name is required" shown near name field; `createCustomer` not called |
| Invalid phone format shows validation error | Submit | Enter "abc" in phone field, click Submit | Validation error message for phone format shown; `createCustomer` not called |
| All required form fields render | Render | Mount in create mode | Form contains inputs labelled Code, Name, Phone, Email, Address, Credit Limit |
| Submit valid form navigates to /customers | Submit | All fields valid; MSW returns 201 | `createCustomer` called with correct payload; `router.push('/customers')` called |
| Credit Limit field accepts numeric input only | User input | Type alphabetic characters into Credit Limit | Input rejects non-numeric characters or shows validation error |
| Form shows loading state while submitting | Submit | Introduce artificial API delay via MSW | Submit button disabled or shows spinner while request is in-flight |

---

## 3. CustomerFormView.vue — Edit Mode

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| getCustomer called on mount with route id | `onMounted` | Mount with route param `id=5`; MSW returns customer data | `getCustomer(5)` called once on mount |
| All fields pre-filled with existing data | Render | MSW returns customer `{ id:5, code:"CUST-0005", name:"Ali", phone:"0911...", email:"ali@x.uz", address:"Tashkent", creditLimit:5000000 }` | Each form input value matches the corresponding customer property |
| Code field is disabled in edit mode | Render | Mount with route param `id=5` | Code input has `disabled` attribute; user cannot type in it |
| Code field shows existing code as value, not placeholder | Render | Mount with route param `id=5`; code = "CUST-0005" | Code input `.value` = "CUST-0005"; placeholder not used to display the code |
| Name field is editable | User input | Clear name field and type "Hassan" | Name input accepts the new value |
| Phone field is editable | User input | Change phone to new number | Phone input reflects new value |
| Email field is editable | User input | Change email to new address | Email input reflects new value |
| Address field is editable | User input | Change address text | Address input reflects new value |
| Credit Limit field is editable | User input | Update credit limit to 10000000 | Credit limit input reflects new numeric value |
| Submit calls updateCustomer with correct id | Submit | Modify name and submit; MSW returns 200 | `updateCustomer(5, { name: "Hassan", ... })` called; code field value not included in editable payload changes |
| Successful update navigates to /customers | Submit | MSW returns 200 on PUT/PATCH | `router.push('/customers')` called after successful update |
| API 404 on load shows error and redirects | `onMounted` | MSW returns 404 for `GET /customers/999` | Error message rendered; `router.push('/customers')` called |
| getNextCustomerCode is NOT called in edit mode | `onMounted` | Mount with route param id present | `getNextCustomerCode` endpoint not called |

---

## 4. CustomerHistoryView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| getCustomer called on mount | `onMounted` | Mount with route param `id=3`; MSW returns customer data | `getCustomer(3)` called; customer name, code, and balance displayed in header area |
| Customer header shows name, code, balance | Render | Customer `{ name:"Dilnoza", code:"CUST-003", balance:1500000 }` | Header/summary area contains "Dilnoza", "CUST-003", and formatted balance |
| AR Invoices tab active by default on mount | Render | Mount component | AR Invoices tab is the active/selected tab; invoice table visible |
| AR Invoices tab loads getARInvoices on mount | `onMounted` | MSW returns 2 invoices filtered by `customerId=3` | `getARInvoices({ customerId: 3 })` called; table shows 2 invoice rows |
| Invoices table shows correct columns | Render | 1 seeded invoice | Table shows number, date, due date, amount, balance due, and status columns |
| UNPAID status badge renders red | Render | Invoice with status=UNPAID | Status cell has CSS class or style indicating red/danger colour |
| PARTIAL status badge renders yellow | Render | Invoice with status=PARTIAL | Status cell has CSS class or style indicating yellow/warning colour |
| PAID status badge renders green | Render | Invoice with status=PAID | Status cell has CSS class or style indicating green/success colour |
| Overdue invoice row highlighted red | Render | Invoice with due date in the past and status != PAID | Row has overdue CSS class or red highlight; non-overdue rows do not |
| Payments tab click loads getARPayments | Click | Click "Payments" tab | `getARPayments({ customerId: 3 })` called; payments table rendered with number, date, method, amount columns |
| Credit Notes tab click loads getCreditNotes | Click | Click "Credit Notes" tab | `getCreditNotes({ customerId: 3 })` called; credit notes table rendered with number, date, amount, status columns |
| Credit note status OPEN and APPLIED shown | Render | Seed one OPEN and one APPLIED credit note | Both status values rendered correctly in status column |
| Balance summary shows correct totals | Render | MSW returns invoices totalling 3000000, payments 1500000 | Summary area shows Total Invoiced, Total Paid, Outstanding Balance with correct computed values |
| "Create Invoice" button navigates with customerId | Click | Click "Create Invoice" | `router.push('/finance/debtors/create?customerId=3')` called |
| "Record Payment" button opens payment modal | Click | Click "Record Payment" | Payment modal becomes visible in DOM |
| Switching tabs does not repeat customer API call | Click | Click through all 3 tabs | `getCustomer` called exactly once; tab data APIs called per tab activation |

---

## 5. SuppliersView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Loads vendors on mount with pagination | `onMounted` | MSW returns paginated vendor list | `getVendors` called once with `{ page: 1 }`; table renders returned rows |
| Vendor table shows required columns | Render | 1 seeded vendor record | Table contains Code, Name, Phone, Email, Status columns with correct values |
| Search input calls searchVendors | User input | Type "Toshmat" and advance past debounce | `searchVendors({ q: "Toshmat" })` called; table updates with search results |
| "Create Supplier" button navigates to create route | Click | Click "Create Supplier" | Navigation to supplier form or modal triggered |
| Edit button navigates to edit route | Click | Click edit on vendor id=4 | Navigation to vendor edit view for id=4 triggered |
| "View History" button navigates correctly | Click | Click "View History" on vendor id=4 | `router.push('/purchases/supplier-history/4')` called |
| Delete button shows confirmation dialog | Click | Click delete on any vendor row | Confirmation dialog visible; vendor row not yet removed |
| Confirm delete calls deleteVendor and removes row | Click confirm | Confirm deletion; MSW returns 200 | `deleteVendor(id)` called; row removed from table |
| Delete with POs returns 422 error | Click confirm | MSW returns 422 with error message | Error message visible; row not removed |
| Pagination next loads page 2 | Click | Click "Next" when total pages > 1 | `getVendors({ page: 2 })` called |
| Pagination prev disabled on first page | Render | Mount on page 1 | Previous button disabled or absent |
| Empty vendor list shows empty state | Render | MSW returns empty array | Empty-state message or illustration shown; no table rows |
| Status badge renders per vendor status | Render | Mix of ACTIVE and INACTIVE vendors | ACTIVE and INACTIVE badges styled distinctly |

---

## 6. PurchaseOrdersView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Loads purchase orders on mount | `onMounted` | MSW returns list of POs on `GET /purchase-orders?page=1` | `getPurchaseOrders` called once; PO rows rendered in table |
| Table shows required columns | Render | 1 seeded PO | Table shows PO number, vendor, date, status badge, total amount |
| Status badge renders per PO status | Render | Seed POs with DRAFT, RELEASED, RECEIVED, CANCELLED | Each status renders a visually distinct badge |
| Status filter dropdown triggers filtered API call | Change | Select "RELEASED" in status filter dropdown | `getPurchaseOrders({ status: "RELEASED" })` called; table updates |
| Date range filter triggers filtered API call | Change | Set from-date and to-date | `getPurchaseOrders({ dateFrom: "...", dateTo: "..." })` called |
| "Create PO" button navigates to create form | Click | Click "Create PO" | `router.push('/purchases/purchase-orders/create')` called |
| Click PO row navigates to detail view | Click | Click row for PO id=12 | `router.push('/purchases/purchase-orders/12')` called |
| Empty list renders empty state message | Render | MSW returns empty array | "No purchase orders found" message visible; no table rows |
| Pagination next triggers page 2 call | Click | Click "Next"; total pages > 1 | `getPurchaseOrders({ page: 2 })` called |
| Pagination prev triggers page 1 call | Click | On page 2, click "Previous" | `getPurchaseOrders({ page: 1 })` called |
| Clearing status filter resets to all POs | Change | Select "RELEASED", then select blank/all option | `getPurchaseOrders` called without status filter; all POs shown |
| Combined filters pass all params | Change | Set status=DRAFT and date range | API called with both `status` and date params simultaneously |

---

## 7. PurchaseOrderFormView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| Vendor dropdown loaded on mount | `onMounted` | MSW returns 3 active vendors | `getActiveVendors` called once; dropdown contains 3 vendor options |
| Order date defaults to today | Render | Mount component | Date picker value equals today's date (2026-04-17) |
| "Add Line" button adds a new empty line row | Click | Click "Add Line" | New row appears in lines table with empty product search, qty, and unit cost fields |
| Product search in line triggers searchProducts | User input | Type "Cement" in product search of first line | `searchProducts({ q: "Cement" })` called; dropdown of matching products shown |
| Selecting product pre-fills name and unit cost | Click | Select product from dropdown | Product name field and unit cost field populated with product data |
| Entering qty calculates line total | User input | Enter qty=5 with unit cost=200000 | Line total cell shows 1000000 |
| Remove line button deletes the row | Click | Add two lines, click remove on first line | First row removed; one row remains |
| Cannot remove last remaining line | Click | Only one line present, click remove | Remove button absent or disabled on the sole remaining row |
| Submit with vendor + lines calls createPurchaseOrder | Submit | Select vendor, add one valid line, click Submit; MSW returns 201 | `createPurchaseOrder` called with correct payload; redirect to PO detail view |
| Submit with no vendor shows validation error | Submit | Leave vendor blank, add valid lines, click Submit | Validation error "Vendor is required" shown; `createPurchaseOrder` not called |
| Submit with no lines shows validation error | Submit | Select vendor, no lines added, click Submit | Validation error "At least one line is required" shown; `createPurchaseOrder` not called |
| Submit with qty=0 on a line shows validation error | Submit | Set qty=0 on a line, click Submit | Validation error for zero-quantity line shown; `createPurchaseOrder` not called |
| Line total updates when qty changes | User input | Change qty after initial entry | Line total recalculates reactively |
| Line total updates when unit cost changes | User input | Change unit cost after initial entry | Line total recalculates reactively |
| Redirect to PO detail on successful create | Submit | MSW returns 201 with new PO id=99 | `router.push('/purchases/purchase-orders/99')` called |

---

## 8. PurchaseOrderDetailView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| getOnePurchaseOrder called on mount with id | `onMounted` | Mount with route param `id=12` | `getOnePurchaseOrder(12)` called once |
| Header shows PO metadata | Render | PO `{ number:"PO-0012", vendor:"Toshmat LLC", createdAt:"2026-04-10", status:"DRAFT", total:3000000 }` | Header displays PO number, vendor name, created date, status badge, and total |
| Lines table shows all required columns | Render | PO with 2 line items | Table shows product, ordered qty, received qty (0), unit cost, and line total per row |
| Received qty defaults to 0 initially | Render | PO that has not been received | Received qty column shows 0 for all lines |
| "Release PO" visible only when DRAFT | Render | PO with status=DRAFT | "Release PO" button visible; not present for RELEASED/RECEIVED/CANCELLED |
| Click Release shows confirmation then calls API | Click | Click "Release PO", confirm dialog | Confirmation dialog appears; on confirm `releasePurchaseOrder(12)` called |
| Status badge updates to RELEASED after release | Click | Successful release API call | Status badge changes from DRAFT to RELEASED without full page reload |
| "Cancel PO" visible when DRAFT or RELEASED | Render | Mount with DRAFT and separately with RELEASED PO | "Cancel PO" button visible for both DRAFT and RELEASED statuses |
| "Cancel PO" not visible when RECEIVED | Render | PO with status=RECEIVED | "Cancel PO" button absent from DOM |
| Click Cancel shows confirmation then calls API | Click | Click "Cancel PO", confirm | `cancelPurchaseOrder(12)` called; status badge changes to CANCELLED |
| "Create Receiving" visible only when RELEASED | Render | RELEASED PO | "Create Receiving" button visible; absent for DRAFT and CANCELLED |
| Click "Create Receiving" navigates or opens modal | Click | Click "Create Receiving" on RELEASED PO | Navigate to receiving form for this PO, or receiving modal becomes visible |
| API 404 on load shows error and redirects | `onMounted` | MSW returns 404 for `GET /purchase-orders/999` | Error message rendered; `router.push('/purchases/purchase-orders')` called |
| Total in header matches sum of lines | Render | PO with 2 lines; total=5000000 | Header total equals sum of individual line totals |

---

## 9. SupplierHistoryView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| getVendor called on mount with route id | `onMounted` | Mount with route param `id=6` | `getVendor(6)` called once |
| Vendor info displayed at top | Render | Vendor `{ name:"Toshmat LLC", code:"VEND-006", phone:"0901..." }` | Header area shows vendor name and code |
| AP Invoices tab active by default | Render | Mount component | AP Invoices tab selected; AP invoice table visible |
| AP Invoices tab loads getAPInvoices on mount | `onMounted` | MSW returns 2 AP invoices filtered by `vendorId=6` | `getAPInvoices({ vendorId: 6 })` called; table shows 2 rows |
| AP Invoices table has required columns | Render | 1 seeded AP invoice | Columns include invoice number, date, amount, balance, status |
| Payments tab click loads getAPPayments | Click | Click "Payments" tab | `getAPPayments({ vendorId: 6 })` called; table shows payment number, date, method, amount |
| Purchase Orders tab click loads getPurchaseOrders | Click | Click "Purchase Orders" tab | `getPurchaseOrders({ vendorId: 6 })` called; table shows PO number, date, status, total |
| PO table in history has required columns | Render | 1 seeded PO in history | PO number, date, status badge, total all rendered |
| Balance summary shows correct totals | Render | AP invoices total=4000000, payments=2000000 | Summary shows Total Invoiced, Total Paid, Outstanding Payable with computed values |
| Outstanding Payable = Total Invoiced - Total Paid | Render | Invoiced=4000000, Paid=2000000 | Outstanding Payable displayed as 2000000 |
| "Create Invoice" button navigates to AP invoice create | Click | Click "Create Invoice" | Navigation to AP invoice create view or modal triggered with vendorId pre-filled |
| getVendor called only once across tab switches | Click | Click all 3 tabs | `getVendor` called exactly once; tab-specific API calls once per tab activation |
| AP invoice status badges render distinctly | Render | Mix of UNPAID, PARTIAL, PAID invoices | Each status renders with distinct visual styling |
| Empty AP invoices shows empty state | Render | MSW returns empty array for AP invoices | Empty-state message visible; no table rows in AP Invoices tab |

---

## MSW Handler Reference (shared setup)

```js
// vitest.setup.ts (excerpt)
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'

export const server = setupServer(
  http.get('/api/customers',         () => HttpResponse.json({ data: [], meta: { page: 1, totalPages: 1 } })),
  http.get('/api/customers/next-code', () => HttpResponse.json({ nextCode: 'CUST-000042' })),
  http.get('/api/customers/:id',     () => HttpResponse.json({})),
  http.post('/api/customers',        () => HttpResponse.json({}, { status: 201 })),
  http.put('/api/customers/:id',     () => HttpResponse.json({})),
  http.delete('/api/customers/:id',  () => new HttpResponse(null, { status: 204 })),
  http.get('/api/vendors',           () => HttpResponse.json({ data: [], meta: {} })),
  http.get('/api/vendors/:id',       () => HttpResponse.json({})),
  http.delete('/api/vendors/:id',    () => new HttpResponse(null, { status: 204 })),
  http.get('/api/purchase-orders',   () => HttpResponse.json({ data: [], meta: {} })),
  http.get('/api/purchase-orders/:id', () => HttpResponse.json({})),
  http.post('/api/purchase-orders',  () => HttpResponse.json({ id: 99 }, { status: 201 })),
  http.post('/api/purchase-orders/:id/release', () => HttpResponse.json({})),
  http.post('/api/purchase-orders/:id/cancel',  () => HttpResponse.json({})),
  http.get('/api/ar-invoices',       () => HttpResponse.json({ data: [] })),
  http.get('/api/ar-payments',       () => HttpResponse.json({ data: [] })),
  http.get('/api/credit-notes',      () => HttpResponse.json({ data: [] })),
  http.get('/api/ap-invoices',       () => HttpResponse.json({ data: [] })),
  http.get('/api/ap-payments',       () => HttpResponse.json({ data: [] })),
  http.get('/api/products/search',   () => HttpResponse.json({ data: [] })),
)

beforeAll(() => server.listen())
afterEach(() => server.resetHandlers())
afterAll(() => server.close())
```

> Each test that requires a non-default response calls `server.use(http.get(..., handler))` before mounting the component.

---

# Section 6c-i: Frontend — Finance Views — Test Plan

**Testing Stack:** Vitest + @vue/test-utils + MSW  
**Coverage Goal:** 100% component test coverage for all finance-related Vue.js views  
**Platform:** Hisobnoma SaaS

---

## Overview

This section covers unit and integration-level component tests for every view under the Finance module. Each test is designed to be executed in a Vitest environment with `@vue/test-utils` for DOM mounting/interaction and MSW (Mock Service Worker) for intercepting HTTP calls at the network layer. All tests are isolated per component; shared MSW handlers are reset between tests via `afterEach`.

---

## ExpensesView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `mount calls getExpenses and renders table` | `onMounted` | Component mounts with no filter params | MSW handler for `GET /expenses?page=0` is called exactly once; table rows appear with date, category, description, and amount columns present in the header |
| `table renders required columns` | Render after mount | MSW returns an expense list with all fields populated | Table headers contain Date, Category, Description, Amount; each row cell maps to the correct field |
| `date range filter triggers filtered API call` | `change` event on date range inputs | User sets start date `2024-01-01` and end date `2024-01-31` | `GET /expenses?from=2024-01-01&to=2024-01-31` is called; table rows update to matching expenses only |
| `category filter dropdown triggers filtered API call` | `change` event on category dropdown | User selects category "Office Supplies" | `GET /expenses?category=Office+Supplies` (or equivalent id param) is called; table rows update to matching category only |
| `Create Expense button navigates to create form` | `click` on "Create Expense" button | User clicks the "Create Expense" button | `router.push` is called with `/finance/expenses/create` |
| `click expense row navigates to detail` | `click` on a table row | User clicks the row for expense with id `17` | `router.push` is called with `/finance/expenses/17` |
| `amount cell formatted as currency` | Render after mount | MSW returns an expense with amount `1234.5` | Amount cell text is formatted as a currency string (e.g., `$1,234.50` or locale equivalent); raw number `1234.5` is not displayed as-is |
| `footer shows total of displayed amounts` | Render after mount | MSW returns expenses with amounts `100.00`, `200.00`, and `50.00` | A footer row or summary element displays the summed total `350.00` formatted as currency |
| `pagination next calls getExpenses with page=1` | `click` on Next page control | Current page is 0; user clicks Next | `GET /expenses?page=1` is called; table rows update to page-2 data |
| `pagination prev calls getExpenses with page=0` | `click` on Previous page control | Current page is 1; user clicks Previous | `GET /expenses?page=0` is called; table rows update to page-1 data |
| `empty list shows empty state message` | Render after mount | MSW returns `{ data: [], total: 0 }` | Element with text "No expenses found" is present in the DOM; table body has no `<tr>` data rows |

---

## ExpenseFormView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `create mode renders all required fields` | Render | Component mounted without an `id` route param | Inputs/selects for amount, category, description, and date are all present in the DOM |
| `date field defaults to today in create mode` | Render | Component mounted without an `id` route param | Date input value equals today's date in ISO format (`2026-04-18` or locale-formatted equivalent) |
| `submit valid form calls createExpense and redirects` | `submit` form event | All required fields filled with valid values; amount is positive | `POST /expenses` is called with the correct payload; `router.push('/finance/expenses')` is invoked |
| `submit negative amount shows validation error` | `submit` form event | Amount field contains `-50` | No API call is made; element containing text "Amount must be positive" is visible in the DOM |
| `submit zero amount shows validation error` | `submit` form event | Amount field contains `0` | No API call is made; a validation error element is visible in the DOM (e.g., "Amount must be positive" or "Amount must be greater than zero") |
| `submit with no category shows validation error` | `submit` form event | Category field/select is left blank or unselected | No API call is made; element containing text "Category required" is visible in the DOM |
| `edit mode calls getExpense on mount` | `onMounted` | Component mounted with route param `id=42` | `GET /expenses/42` is called exactly once on mount |
| `edit mode pre-fills all fields from loaded expense` | Render after mount | MSW returns expense with amount `250.00`, category "Travel", description "Flight to Tashkent", date `2024-03-15` | Amount input value is `250.00`; category select shows "Travel"; description input contains "Flight to Tashkent"; date input contains `2024-03-15` |
| `edit mode submit calls updateExpense` | `submit` form event | Component in edit mode; user changes description and submits | `PUT /expenses/42` is called with the updated payload; `router.push('/finance/expenses')` is invoked |
| `404 on edit load shows error and redirects to list` | `onMounted` | MSW returns 404 for `GET /expenses/42` | An error message is rendered (or shown briefly); `router.push('/finance/expenses')` is called |

---

## ExpenseDetailView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `mount calls getExpense by ID and renders all fields` | `onMounted` | Component mounted with route param `id=42` | `GET /expenses/42` is called exactly once; elements for date, category, description, and amount are all present in the DOM and contain the MSW-returned values |
| `Edit button navigates to edit form` | `click` on "Edit" button | Expense detail loaded for id `42` | `router.push` is called with `/finance/expenses/42/edit` |
| `Delete button shows confirmation dialog` | `click` on "Delete" button | Expense detail loaded; user clicks Delete | A confirmation dialog (modal or native confirm) is rendered/visible; no API call has been made yet |
| `confirm delete calls deleteExpense and redirects to list` | `click` confirm in dialog | User confirms the deletion dialog | `DELETE /expenses/42` is called; `router.push('/finance/expenses')` is invoked |
| `cancel delete makes no API call` | `click` cancel in dialog | User dismisses the confirmation dialog | No `DELETE /expenses/.*` request is recorded by MSW; user remains on the detail view |

---

## DebtorsView.vue (AR Invoices)

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `mount calls getARInvoices and renders table` | `onMounted` | Component mounts with no filter params | MSW handler for `GET /ar-invoices?page=0` is called exactly once; table rows appear |
| `table renders required columns` | Render after mount | MSW returns an AR invoice with all fields populated | Table headers contain Invoice Number, Customer, Date, Due Date, Amount, Balance, and Status; each row cell maps to the correct field |
| `UNPAID badge renders with red styling` | Render after mount | MSW returns an invoice with `status: "UNPAID"` | The status cell contains a badge element with a red colour class/attribute (e.g., `badge-red`, `text-red-600`, or equivalent) |
| `PARTIAL badge renders with yellow styling` | Render after mount | MSW returns an invoice with `status: "PARTIAL"` | The status cell contains a badge element with a yellow colour class/attribute |
| `PAID badge renders with green styling` | Render after mount | MSW returns an invoice with `status: "PAID"` | The status cell contains a badge element with a green colour class/attribute |
| `overdue unpaid row is highlighted red` | Render after mount | MSW returns an invoice with a due date in the past and `status: "UNPAID"` | The table row element carries a red highlight class/attribute (e.g., `row-overdue`, `bg-red-50`); a PAID or future-due row does not carry this class |
| `overdue partial row is highlighted red` | Render after mount | MSW returns an invoice with a due date in the past and `status: "PARTIAL"` | The table row element carries the red overdue highlight class |
| `paid row past due date is NOT highlighted red` | Render after mount | MSW returns an invoice with a due date in the past and `status: "PAID"` | The table row does NOT carry the overdue highlight class |
| `customer search filter triggers debounced API call` | `input` event on customer search field | User types "Alisher" in the customer search input; wait for debounce | `GET /ar-invoices?customer=Alisher` (or equivalent query param) is called once after debounce settles; previous rows replaced by filtered results |
| `rapid customer search typing only triggers one debounced call` | Multiple `input` events in quick succession | User types "A", "Al", "Ali" within 100 ms each | Only one API call fires after debounce settles; intermediate queries are not dispatched |
| `status filter triggers filtered API call` | `change` event on status filter dropdown | User selects status "UNPAID" | `GET /ar-invoices?status=UNPAID` is called; table rows update to show only UNPAID invoices |
| `Create Invoice button navigates to create form` | `click` on "Create Invoice" button | User clicks "Create Invoice" | `router.push` is called with `/finance/debtors/create` |
| `click invoice row navigates to detail` | `click` on a table row | User clicks the row for invoice with id `55` | `router.push` is called with `/finance/debtors/55` |
| `pagination next calls getARInvoices with page=1` | `click` on Next page control | Current page is 0; user clicks Next | `GET /ar-invoices?page=1` is called; table rows update to page-2 data |
| `pagination prev calls getARInvoices with page=0` | `click` on Previous page control | Current page is 1; user clicks Previous | `GET /ar-invoices?page=0` is called; table rows update to page-1 data |

---

## DebtorFormView.vue (AR Invoice Create)

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `customer dropdown loads from getActiveCustomers on mount` | `onMounted` | Component mounts | `GET /customers/active` is called once; the customer dropdown options match the MSW-returned customer list |
| `tax code dropdown loads from getActiveTaxCodes on mount` | `onMounted` | Component mounts | `GET /tax-codes/active` is called once; tax code options in each line's tax code selector match the MSW-returned tax code list |
| `Add Line button appends a new line row` | `click` on "Add Line" button | Form rendered with one initial line | A new line row appears containing inputs for description, qty, unit price, and a tax code dropdown |
| `line total auto-calculated as qty × unit price` | `input` on qty or unit price field | User enters qty `3` and unit price `100.00` on a line row | That line's total cell displays `300.00` without any explicit save action |
| `line total updates when qty changes` | `input` on qty field | Line has unit price `50.00`; user changes qty from `2` to `5` | Line total cell updates from `100.00` to `250.00` |
| `line total updates when unit price changes` | `input` on unit price field | Line has qty `4`; user changes unit price from `10.00` to `20.00` | Line total cell updates from `40.00` to `80.00` |
| `Remove Line button removes the row` | `click` on "Remove Line" button on a row | Form has two line rows; user clicks Remove on the second row | The second row is removed from the DOM; only one line row remains |
| `Remove Line button disabled when only one line remains` | Render | Form has exactly one line row | The Remove Line button on that row has a `disabled` attribute or is absent from the DOM |
| `subtotal auto-updates when line totals change` | `input` on qty or unit price field | Two lines with totals `200.00` and `150.00`; user changes one | Subtotal element at the bottom reflects the updated sum |
| `tax amount auto-updates based on line tax codes` | `input` on qty or unit price, or `change` on tax code | Line has qty `1`, unit price `100.00`, tax code with rate `15%` | Tax total element reflects `15.00` |
| `grand total auto-updates as subtotal + tax` | `input` on any line field | Subtotal = `200.00`, tax = `30.00` | Grand total element displays `230.00` |
| `submit valid invoice calls createARInvoice and redirects` | `submit` form event | Customer selected; at least one valid line; all fields filled | `POST /ar-invoices` is called with the correct payload; `router.push('/finance/debtors')` is invoked |
| `submit with no customer shows validation error` | `submit` form event | Customer dropdown left unselected; lines otherwise valid | No API call is made; element containing text "Customer required" is visible in the DOM |
| `submit with no lines shows validation error` | `submit` form event | All line rows removed before submit | No API call is made; element containing text "At least one line required" is visible in the DOM |
| `submit with line qty=0 shows validation error` | `submit` form event | A line row has qty set to `0`; all other fields valid | No API call is made; a validation error is visible in the DOM on or near the offending line row |

---

## PaymentsView.vue (AR Payments)

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `mount calls getARPayments and renders table` | `onMounted` | Component mounts with no filter params | MSW handler for `GET /ar-payments?page=0` is called exactly once; table rows appear |
| `table renders required columns` | Render after mount | MSW returns an AR payment with all fields populated | Table headers contain Payment Number, Customer, Date, Method, and Amount; each row cell maps to the correct field |
| `customer filter triggers filtered API call` | `input` or `change` on customer filter | User selects or types customer "Nodir Toshmatov" | `GET /ar-payments?customer=…` (or equivalent query param) is called; table rows update to that customer's payments |
| `date range filter triggers filtered API call` | `change` on date range inputs | User sets start date `2024-02-01` and end date `2024-02-28` | `GET /ar-payments?from=2024-02-01&to=2024-02-28` is called; table rows update to the filtered date range |
| `combined customer and date range filters applied together` | `change` events on both filters | User selects a customer AND sets a date range | A single API call includes both query params; table rows satisfy both filter criteria |
| `Record Payment button opens modal` | `click` on "Record Payment" button | Modal is initially closed | A modal dialog becomes visible containing inputs for customer, amount, payment method, and date |
| `modal submit calls createARPayment, closes modal, and refreshes list` | `submit` inside modal | All modal fields filled with valid values; amount is positive | `POST /ar-payments` is called with the correct payload; modal is no longer visible; `GET /ar-payments` is called again to refresh the list |
| `modal negative amount shows validation error` | `submit` inside modal | Amount field contains `-100`; other fields valid | No API call is made; a validation error is visible inside the modal; modal remains open |
| `modal zero amount shows validation error` | `submit` inside modal | Amount field contains `0`; other fields valid | No API call is made; a validation error is visible inside the modal; modal remains open |
| `modal no customer shows validation error` | `submit` inside modal | Customer field left blank; other fields valid | No API call is made; element containing a customer-required error is visible inside the modal; modal remains open |

---

## Shared Test Setup Notes

The following setup applies to all views in this section and should be placed in a shared `vitest.setup.ts` or per-test `beforeAll`/`afterEach` hooks.

```typescript
// vitest.setup.ts (excerpt)
import { setupServer } from 'msw/node'
import { handlers } from './mocks/handlers'

const server = setupServer(...handlers)

beforeAll(() => server.listen({ onUnhandledRequest: 'warn' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())
```

**Router stub:** Use `createRouter({ history: createMemoryHistory(), routes })` and pass it to `mount` via the `global.plugins` option. Assert navigation via `router.currentRoute.value.path`.

**Debounce testing:** Use `vi.useFakeTimers()` before each debounce test and `vi.advanceTimersByTime(300)` (or the configured debounce value) to trigger the call synchronously. Restore with `vi.useRealTimers()` in `afterEach`.

**Currency formatting:** Assert formatted output with a flexible regex (e.g., `/1[,.]234[.,]50/`) to remain locale-agnostic, or configure the test environment locale explicitly with `Intl` polyfills.

**Dialog/confirmation testing:** If the app uses `window.confirm`, stub it with `vi.spyOn(window, 'confirm').mockReturnValue(true)` (or `false` for cancel tests). If a custom modal component is used, interact with its confirm button via `wrapper.find('[data-testid="confirm-btn"]').trigger('click')`.

**Today's date assertion:** Use `vi.setSystemTime(new Date('2026-04-18'))` in `beforeEach` and `vi.useRealTimers()` in `afterEach` when testing date defaults to ensure deterministic assertions.

**Coverage collection:** Run with `vitest --coverage` using the `@vitest/coverage-v8` provider. The coverage threshold for all files under `src/views/finance/` should be set to `100` for statements, branches, functions, and lines in `vitest.config.ts`.

---

# Section 6c-ii: Frontend — HR, Dashboard, Profile & Layouts — Test Plan

---

## Stack & Coverage Target

Framework: Vitest + @vue/test-utils v2. HTTP calls intercepted with MSW. Coverage target: 100% component branches. Each test mounts the component with `{ global: { plugins: [pinia, router] } }`.

---

## 1. DashboardView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `renders_kpiCards_onMount` | `onMounted` | API returns kpi data | KPI cards for revenue, transactions, customers, low-stock rendered |
| `shows_loading_skeleton_while_fetching` | `onMounted` | API pending | Skeleton loaders visible; KPI values absent |
| `shows_error_banner_on_api_failure` | `onMounted` | API rejects | Error banner with retry button rendered |
| `retry_button_refetches_data` | `click` retry button | After failure, retry clicked | New API call made; data loads on success |
| `revenue_chart_renders_with_data` | `onMounted` | API returns chart data | Revenue chart component present with correct series |
| `top_products_table_renders` | `onMounted` | API returns top-selling products | Top-products table with name, qty, revenue columns |
| `low_stock_panel_shows_items` | `onMounted` | 3 low-stock items | 3 rows in low-stock list |
| `low_stock_panel_empty_state` | `onMounted` | No low-stock items | "All items well-stocked" message |
| `date_range_filter_refetches` | `change` date range selector | User changes to "Last 30 days" | New API call with updated date params |
| `dashboard_links_navigate_correctly` | `click` "View all" links | Click low-stock link | Router navigates to `/inventory/stock` |

---

## 2. EmployeesView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `renders_employee_table_on_mount` | `onMounted` | API returns employees | Table with name, position, phone, status columns |
| `shows_empty_state` | `onMounted` | No employees | "No employees found" message |
| `search_filters_results` | `input` search field | Type "Ali" | Table rows filtered to matching names |
| `status_filter_shows_only_active` | `change` status select | Select "ACTIVE" | Only active employees shown |
| `create_button_navigates_to_form` | `click` "Add Employee" | — | Router pushes to `/hr/employees/new` |
| `row_click_navigates_to_detail` | `click` employee row | — | Router pushes to `/hr/employees/{id}` |
| `edit_button_navigates_to_edit_form` | `click` edit icon | — | Router pushes to `/hr/employees/{id}/edit` |
| `delete_shows_confirmation_dialog` | `click` delete icon | — | Confirmation modal rendered |
| `confirm_delete_removes_row` | `click` confirm in modal | API DELETE succeeds | Employee removed from table |
| `cancel_delete_keeps_row` | `click` cancel in modal | — | Row remains; modal closed |
| `pagination_loads_next_page` | `click` next page | API returns page 2 | Second page of employees rendered |

---

## 3. EmployeeFormView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `create_mode_shows_empty_form` | `onMounted` | Route: `/hr/employees/new` | All fields empty; title "Add Employee" |
| `edit_mode_prefills_form` | `onMounted` | Route: `/hr/employees/5/edit` | Fields prefilled from `getEmployee` API response |
| `edit_mode_shows_not_found_on_404` | `onMounted` | API returns 404 | Redirects to `/hr/employees` |
| `validates_required_first_name` | `submit` | First name empty | Error message under first name field |
| `validates_required_last_name` | `submit` | Last name empty | Error under last name |
| `validates_phone_format` | `submit` | Invalid phone | Error "Invalid phone number" |
| `validates_hire_date_required` | `submit` | No hire date | Error under hire date |
| `create_success_redirects` | `submit` | Valid form; API 201 | Router navigates to `/hr/employees` |
| `update_success_redirects` | `submit` | Valid form; API 200 | Router navigates to `/hr/employees` |
| `cancel_navigates_back` | `click` Cancel | — | Router navigates back to list |

---

## 4. SalaryView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `renders_salary_list_on_mount` | `onMounted` | API returns salary records | Table with employee name, gross, deductions, net columns |
| `month_filter_refetches_data` | `change` month picker | Select March 2026 | New API call with month param |
| `calculate_payroll_button_triggers_calculation` | `click` "Calculate Payroll" | API returns updated salaries | Table refreshes with new values |
| `export_csv_initiates_download` | `click` Export CSV | — | Anchor with `download` attribute triggered |
| `row_shows_advance_deduction` | `onMounted` | Employee has advance | Advance amount in deductions column |

---

## 5. AttendanceView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `renders_attendance_table_on_mount` | `onMounted` | API returns records | Table with employee, check-in, check-out, status |
| `date_filter_refetches` | `change` date picker | Select today | API called with today's date |
| `mark_present_updates_status` | `click` "Mark Present" | API 200 | Status cell updates to PRESENT badge |
| `missing_checkout_highlighted` | `onMounted` | Employee checked in, no checkout | Row highlighted with warning style |

---

## 6. ProfileView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `renders_current_user_info` | `onMounted` | API returns user profile | Name, email, phone, role displayed |
| `edit_name_saves_successfully` | `submit` profile form | Valid name; API 200 | Success toast shown; name updated |
| `change_password_validates_mismatch` | `submit` | New password ≠ confirm | Error "Passwords do not match" |
| `change_password_validates_short` | `submit` | Password < 8 chars | Error about minimum length |
| `change_password_success_toast` | `submit` | Valid passwords; API 200 | Success toast; fields cleared |
| `change_password_wrong_current_shows_error` | `submit` | API 422 on current password | Error "Current password is incorrect" |
| `avatar_upload_preview_shown` | `change` file input | Image file selected | Preview image rendered |
| `avatar_upload_too_large_shows_error` | `change` file input | File > 5MB | Error "File size exceeds 5MB" |

---

## 7. MainLayout.vue & AppSidebar.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `renders_sidebar_with_nav_links` | Mount | Standard user with all permissions | Sidebar nav links for all granted modules |
| `hides_admin_link_without_admin_permission` | Mount | User without ADMIN permission | Admin nav item absent |
| `hides_hr_link_without_hr_permission` | Mount | User without HR permission | HR nav item absent |
| `active_route_link_highlighted` | Mount | Current route `/inventory/products` | Inventory nav item has active class |
| `sidebar_collapse_toggle_works` | `click` collapse button | — | Sidebar collapses; icon-only mode |
| `sidebar_expand_restores_labels` | `click` expand button | Collapsed sidebar | Labels reappear |
| `user_menu_shows_profile_and_logout` | `click` user avatar | — | Dropdown with Profile + Logout |
| `logout_clears_token_and_redirects` | `click` Logout | — | Token removed from storage; router → `/login` |
| `breadcrumb_reflects_current_route` | Route change | Navigate to `/inventory/products` | Breadcrumb shows "Inventory > Products" |
| `notification_badge_shows_count` | Mount | 3 unread notifications | Badge with count "3" on bell icon |

---

## 8. Shared Test Setup Notes

- **MSW handlers** registered in `setupTests.ts`; reset between tests with `server.resetHandlers()`.
- **Pinia** reset between tests via `setActivePinia(createPinia())`.
- **Router** stubs use `createMemoryHistory()` to avoid real navigation side-effects.
- **Fake timers**: `vi.useFakeTimers()` / `vi.runAllTimers()` for debounced search inputs.
- **File upload**: `Object.defineProperty(input, 'files', ...)` to simulate `FileList`.
- **Coverage threshold**: `branches: 100, functions: 100` for all components in `src/views/hr/`, `src/views/dashboard/`, `src/views/profile/`, `src/layouts/`.

---

# Section 6c-iii: Frontend — Report Views & E2E Flows — Test Plan

---

## Stack

Unit/component: Vitest + @vue/test-utils v2 + MSW.
E2E: Playwright (or Cypress). E2E tests run against a seeded test database.

---

## 1. ReportsView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `renders_report_list_on_mount` | `onMounted` | API returns report definitions | Table with report name, type, last-run columns |
| `shows_empty_state` | `onMounted` | No reports | "No reports configured" message |
| `run_report_button_triggers_execution` | `click` Run | API returns 202 Accepted | "Queued" status shown; row updates |
| `report_status_COMPLETED_shows_download` | Poll / `onMounted` | Report COMPLETED | Download button enabled |
| `report_status_FAILED_shows_error` | `onMounted` | Report FAILED | Error icon + tooltip with failure reason |
| `report_status_PENDING_shows_spinner` | `onMounted` | Report PENDING | Spinner in status column |
| `download_excel_triggers_export` | `click` Download Excel | — | `window.open` or anchor triggered with xlsx URL |
| `download_pdf_triggers_export` | `click` Download PDF | — | Anchor triggered with PDF URL |
| `filter_by_type_shows_matching` | `change` type filter | Select "SALES" | Only SALES report rows shown |
| `search_by_name_filters_list` | `input` search field | Type "Inventory" | Only matching report names shown |

---

## 2. FinancialReportView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `renders_report_type_tabs` | Mount | — | Tabs: Trial Balance, Income Statement, Balance Sheet, Cash Flow |
| `trial_balance_tab_loads_data` | `click` Trial Balance tab | API returns trial balance | Debit/credit columns with account rows |
| `trial_balance_totals_match` | `onMounted` | Balanced data | Footer row shows equal debit/credit totals |
| `income_statement_shows_revenue_and_expenses` | `click` Income Statement tab | API returns income data | Revenue section + Expenses section + Net Income |
| `balance_sheet_shows_assets_liabilities_equity` | `click` Balance Sheet tab | API returns balance data | Three sections rendered; Assets = Liabilities + Equity |
| `date_range_picker_refetches_report` | `change` date range | New period selected | API called with updated from/to params |
| `export_csv_button_available` | Mount | — | Export CSV button rendered in toolbar |
| `export_triggers_download` | `click` Export | — | File download initiated |

---

## 3. SalesReportView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `renders_summary_kpi_cards` | `onMounted` | API returns summary | Total Sales, Transactions, Average Sale cards shown |
| `chart_renders_with_daily_data` | `onMounted` | Daily grouping selected | Chart with 30 data points rendered |
| `chart_switches_to_monthly` | `click` Monthly grouping | — | API called with MONTHLY; chart re-rendered |
| `top_products_table_sorted_by_revenue` | `onMounted` | API returns top products | Products listed highest revenue first |
| `terminal_filter_refetches_data` | `change` terminal dropdown | Select terminal | API called with terminalId param |
| `date_range_refetches_data` | `change` date picker | New range | API called with new dates |
| `zero_sales_period_shows_empty_chart` | `onMounted` | API returns no sales | Chart shows flat line or empty state message |

---

## 4. InventoryReportView.vue

| Test Name | Interaction/Lifecycle | Scenario | Expected DOM / Behavior |
|---|---|---|---|
| `renders_stock_on_hand_table` | `onMounted` | API returns stock | Table with product, location, qty, value columns |
| `valuation_total_shown_in_footer` | `onMounted` | API returns valuations | Footer shows sum of all stock values |
| `low_stock_tab_shows_below_reorder` | `click` Low Stock tab | API returns low-stock | Only items below reorder point listed |
| `abc_analysis_tab_shows_classification` | `click` ABC Analysis tab | API returns classifications | A/B/C badges on each product row |
| `warehouse_filter_refetches` | `change` warehouse select | — | API called with warehouseId param |
| `export_triggers_download` | `click` Export | — | Download triggered |

---

## 5. E2E Test Flows (Playwright)

### 5.1 Full POS Sale Flow

| Test Name | Steps | Expected Result |
|---|---|---|
| `e2e_pos_complete_sale_cash` | 1. Login as cashier → 2. Open shift → 3. Search product → 4. Add to cart → 5. Add payment (cash) → 6. Complete sale | Transaction created with COMPLETED status; receipt shown; stock decremented |
| `e2e_pos_apply_coupon_and_complete` | 1. Login → 2. Open shift → 3. Add items → 4. Enter coupon code → 5. Verify discount → 6. Complete | Discount applied; coupon usageCount incremented |
| `e2e_pos_void_transaction` | 1. Login → 2. Create draft transaction → 3. Void | Transaction status VOIDED; items not deducted |
| `e2e_pos_close_shift_with_summary` | 1. Login → 2. Open shift → 3. Complete 2 sales → 4. Close shift | Shift CLOSED; summary shows correct totals |

### 5.2 Full Inventory Flow

| Test Name | Steps | Expected Result |
|---|---|---|
| `e2e_create_po_and_receive` | 1. Login as inventory manager → 2. Create PO → 3. Release PO → 4. Create receiving order → 5. Receive all lines → 6. Complete receiving | Stock increased by received quantities |
| `e2e_inventory_count_with_adjustment` | 1. Login → 2. Create count → 3. Start count → 4. Record counts (one variance) → 5. Complete | Adjustment movement created for variance |
| `e2e_stock_transfer_between_locations` | 1. Login → 2. Source has 20 units → 3. Transfer 8 to destination → 4. Verify both locations | Source: 12 units; destination: 8 units |

### 5.3 Finance Flow

| Test Name | Steps | Expected Result |
|---|---|---|
| `e2e_create_ar_invoice_and_receive_payment` | 1. Login as accountant → 2. Create AR invoice → 3. Approve → 4. Post → 5. Record payment | Invoice status FULLY_PAID; GL journal entry created |
| `e2e_create_ap_invoice_and_make_payment` | 1. Login → 2. Create AP invoice for vendor → 3. Approve → 4. Post → 5. Make payment | Invoice balance reduced; payment recorded |
| `e2e_journal_entry_and_trial_balance` | 1. Login → 2. Create balanced journal entry → 3. Post → 4. View trial balance | Trial balance reflects posted entry; DR = CR |

### 5.4 Authentication & Authorization

| Test Name | Steps | Expected Result |
|---|---|---|
| `e2e_login_and_access_permitted_module` | 1. Login as inventory user → 2. Navigate to /inventory/products | Products list rendered |
| `e2e_access_denied_for_unauthorized_module` | 1. Login as inventory user → 2. Navigate to /hr/employees | Redirected to 403 or dashboard |
| `e2e_token_expiry_redirects_to_login` | 1. Login → 2. Expire token (mock) → 3. Make API call | Redirected to /login |
| `e2e_password_reset_flow` | 1. Go to /forgot-password → 2. Enter email → 3. Open reset link → 4. Set new password → 5. Login | Login succeeds with new password |

### 5.5 HR Flow

| Test Name | Steps | Expected Result |
|---|---|---|
| `e2e_create_employee_and_calculate_salary` | 1. Login as HR → 2. Create employee → 3. Set salary → 4. Mark attendance → 5. Calculate payroll | Net salary computed correctly |
| `e2e_advance_deducted_from_salary` | 1. Create employee → 2. Record advance → 3. Calculate salary | Advance shown in deductions; net reduced |

---

## 6. Shared E2E Setup Notes

- **Database seeding**: `beforeEach` calls seed script that inserts a test tenant, admin user, and baseline products/accounts.
- **Auth**: `storageState` reused per role (admin, cashier, accountant, inventory-manager) to avoid repeated logins.
- **Teardown**: `afterEach` truncates transaction/movement tables; static reference data (products, accounts) persists across tests in a suite.
- **Selectors**: All interactive elements have `data-testid` attributes; E2E tests use only `data-testid` selectors, never CSS classes.
- **Flake prevention**: `waitForResponse` used for all API-driven state changes before asserting DOM.
