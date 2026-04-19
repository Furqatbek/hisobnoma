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
