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
