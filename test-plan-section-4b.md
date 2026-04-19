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
