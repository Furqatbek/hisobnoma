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
