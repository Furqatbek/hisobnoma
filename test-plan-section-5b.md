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
