package com.hisobnoma.platform.reports.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.finance.entity.*;
import com.hisobnoma.platform.finance.repository.*;
import com.hisobnoma.platform.inventory.entity.Vendor;
import com.hisobnoma.platform.inventory.repository.VendorRepository;
import com.hisobnoma.platform.reports.dto.AgingReportDTO;
import com.hisobnoma.platform.reports.dto.GenerateReportRequest;
import com.hisobnoma.platform.reports.dto.IncomeStatementDTO;
import com.hisobnoma.platform.reports.dto.TrialBalanceReportDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Service for generating financial reports.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FinancialReportService {

    private final SecurityContextHelper securityContextHelper;
    private final AccountRepository accountRepository;
    private final JournalLineRepository journalLineRepository;
    private final ARInvoiceRepository arInvoiceRepository;
    private final APInvoiceRepository apInvoiceRepository;
    private final CustomerRepository customerRepository;
    private final VendorRepository vendorRepository;
    private final TenantRepository tenantRepository;
    private final FiscalYearRepository fiscalYearRepository;

    private LocalDate today(Long tenantId) {
        ZoneId zone = tenantRepository.findById(tenantId)
                .map(Tenant::getTimezone)
                .filter(tz -> tz != null && !tz.isBlank())
                .map(tz -> {
                    try {
                        return ZoneId.of(tz);
                    } catch (Exception e) {
                        log.warn("Invalid tenant timezone '{}', falling back to UTC", tz);
                        return (ZoneId) ZoneOffset.UTC;
                    }
                })
                .orElse(ZoneOffset.UTC);
        return LocalDate.now(zone);
    }

    private static final List<ARInvoiceStatus> AR_EXCLUDED_STATUSES = Arrays.asList(
            ARInvoiceStatus.DRAFT, ARInvoiceStatus.CANCELLED, ARInvoiceStatus.PAID, ARInvoiceStatus.WRITTEN_OFF);
    private static final List<APInvoiceStatus> AP_EXCLUDED_STATUSES = Arrays.asList(
            APInvoiceStatus.DRAFT, APInvoiceStatus.CANCELLED, APInvoiceStatus.PAID);

    /**
     * Generate Trial Balance Report.
     *
     * Computed from posted journal lines so the asOfDate parameter is honored
     * (previously the method ignored it and always used the YTD snapshot stored
     * on the Account entity).
     *
     * Permanent accounts (Asset/Liability/Equity) carry forward openingBalance plus
     * all posted activity through asOfDate. Temporary accounts (Revenue/Expense)
     * show only activity within the fiscal year containing asOfDate, since they
     * are closed to retained earnings annually.
     */
    public TrialBalanceReportDTO generateTrialBalanceReport(GenerateReportRequest request) {
        Long tenantId = securityContextHelper.getRequiredTenantId();
        LocalDate asOfDate = request.getEndDate() != null ? request.getEndDate() : today(tenantId);

        log.info("Generating Trial Balance report for tenant {} as of {}", tenantId, asOfDate);

        // Resolve fiscal year boundaries. Fall back to calendar year if no FiscalYear is configured for asOfDate.
        var fiscalYear = fiscalYearRepository.findByDateAndTenantId(asOfDate, tenantId);
        LocalDate fiscalYearStart = fiscalYear
                .map(fy -> fy.getStartDate())
                .orElse(LocalDate.of(asOfDate.getYear(), 1, 1));
        String fiscalYearLabel = fiscalYear
                .map(fy -> String.valueOf(fy.getYear()))
                .orElse(String.valueOf(asOfDate.getYear()));

        // Include all accounts (active + inactive) — inactive accounts can still hold historical balances
        // and excluding them would unbalance the trial balance.
        List<Account> accounts = accountRepository.findAllByTenantId(tenantId);

        List<TrialBalanceReportDTO.AccountBalance> balances = new ArrayList<>();
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (Account account : accounts) {
            boolean temporary = account.getAccountType() == AccountType.REVENUE
                    || account.getAccountType() == AccountType.EXPENSE;

            BigDecimal debit;
            BigDecimal credit;
            if (temporary) {
                // Revenue/Expense: only activity within the fiscal year containing asOfDate.
                debit = journalLineRepository.sumDebitByAccountAndDateRange(
                        account.getId(), tenantId, fiscalYearStart, asOfDate);
                credit = journalLineRepository.sumCreditByAccountAndDateRange(
                        account.getId(), tenantId, fiscalYearStart, asOfDate);
            } else {
                // Asset/Liability/Equity: cumulative through asOfDate.
                debit = journalLineRepository.sumDebitByAccountAsOf(account.getId(), tenantId, asOfDate);
                credit = journalLineRepository.sumCreditByAccountAsOf(account.getId(), tenantId, asOfDate);
            }
            if (debit == null) debit = BigDecimal.ZERO;
            if (credit == null) credit = BigDecimal.ZERO;

            // Opening balance applies to permanent accounts only — temporary accounts close annually.
            BigDecimal opening = !temporary && account.getOpeningBalance() != null
                    ? account.getOpeningBalance()
                    : BigDecimal.ZERO;

            // Net balance on the account's natural side.
            BigDecimal netBalance = account.getNormalBalance() == NormalBalance.DEBIT
                    ? opening.add(debit).subtract(credit)
                    : opening.add(credit).subtract(debit);

            BigDecimal debitBalance = BigDecimal.ZERO;
            BigDecimal creditBalance = BigDecimal.ZERO;
            if (netBalance.signum() != 0) {
                boolean naturalSide = netBalance.signum() > 0;
                BigDecimal magnitude = netBalance.abs();
                if (account.getNormalBalance() == NormalBalance.DEBIT) {
                    if (naturalSide) debitBalance = magnitude; else creditBalance = magnitude;
                } else {
                    if (naturalSide) creditBalance = magnitude; else debitBalance = magnitude;
                }
            }

            if (debitBalance.signum() != 0 || creditBalance.signum() != 0) {
                balances.add(TrialBalanceReportDTO.AccountBalance.builder()
                        .accountId(account.getId())
                        .accountCode(account.getCode())
                        .accountName(account.getName())
                        .accountType(account.getAccountType().name())
                        .accountSubType(null)
                        .debitBalance(debitBalance)
                        .creditBalance(creditBalance)
                        .level(account.getAccountLevel())
                        .isHeader(!account.isAllowsDirectPosting())
                        .build());

                totalDebits = totalDebits.add(debitBalance);
                totalCredits = totalCredits.add(creditBalance);
            }
        }

        balances.sort(Comparator.comparing(TrialBalanceReportDTO.AccountBalance::getAccountCode));

        BigDecimal difference = totalDebits.subtract(totalCredits).abs();
        boolean isBalanced = difference.compareTo(new BigDecimal("0.01")) < 0;

        return TrialBalanceReportDTO.builder()
                .metadata(TrialBalanceReportDTO.ReportMetadata.builder()
                        .reportName("Trial Balance")
                        .generatedAt(Instant.now())
                        .asOfDate(asOfDate)
                        .fiscalYear(fiscalYearLabel)
                        .period(asOfDate.getMonth().name())
                        .build())
                .accounts(balances)
                .totals(TrialBalanceReportDTO.Totals.builder()
                        .totalDebits(totalDebits)
                        .totalCredits(totalCredits)
                        .isBalanced(isBalanced)
                        .difference(difference)
                        .build())
                .build();
    }

    /**
     * Generate Income Statement (Profit & Loss / Daromad va Xarajatlar) Report.
     * Shows all revenue and expense accounts with their balances for a date range.
     */
    public IncomeStatementDTO generateIncomeStatement(GenerateReportRequest request) {
        Long tenantId = securityContextHelper.getRequiredTenantId();

        LocalDate today = today(tenantId);
        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : today.withDayOfMonth(1);
        LocalDate endDate = request.getEndDate() != null ? request.getEndDate() : today;

        log.info("Generating Income Statement for tenant {} from {} to {}", tenantId, startDate, endDate);

        List<Account> accounts = accountRepository.findAllActiveByTenantId(tenantId);
        accounts.sort(Comparator.comparing(Account::getCode));

        List<IncomeStatementDTO.LineItem> revenueItems = new ArrayList<>();
        List<IncomeStatementDTO.LineItem> expenseItems = new ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (Account account : accounts) {
            if (account.getAccountType() != AccountType.REVENUE && account.getAccountType() != AccountType.EXPENSE) {
                continue;
            }

            // Calculate balance from posted journal lines in the date range
            BigDecimal debit = journalLineRepository.sumDebitByAccountAndDateRange(
                    account.getId(), tenantId, startDate, endDate);
            BigDecimal credit = journalLineRepository.sumCreditByAccountAndDateRange(
                    account.getId(), tenantId, startDate, endDate);

            if (debit == null) debit = BigDecimal.ZERO;
            if (credit == null) credit = BigDecimal.ZERO;

            BigDecimal amount;
            if (account.getAccountType() == AccountType.REVENUE) {
                // Revenue normal balance is CREDIT: amount = credits - debits
                amount = credit.subtract(debit);
            } else {
                // Expense normal balance is DEBIT: amount = debits - credits
                amount = debit.subtract(credit);
            }

            if (amount.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            IncomeStatementDTO.LineItem item = IncomeStatementDTO.LineItem.builder()
                    .accountId(account.getId())
                    .accountCode(account.getCode())
                    .accountName(account.getName())
                    .amount(amount)
                    .build();

            if (account.getAccountType() == AccountType.REVENUE) {
                revenueItems.add(item);
                totalRevenue = totalRevenue.add(amount);
            } else {
                expenseItems.add(item);
                totalExpenses = totalExpenses.add(amount);
            }
        }

        return IncomeStatementDTO.builder()
                .metadata(IncomeStatementDTO.ReportMetadata.builder()
                        .reportName("Daromad va Xarajatlar Hisoboti")
                        .generatedAt(Instant.now())
                        .startDate(startDate)
                        .endDate(endDate)
                        .build())
                .revenueItems(revenueItems)
                .expenseItems(expenseItems)
                .summary(IncomeStatementDTO.Summary.builder()
                        .totalRevenue(totalRevenue)
                        .totalExpenses(totalExpenses)
                        .netIncome(totalRevenue.subtract(totalExpenses))
                        .build())
                .build();
    }

    /**
     * Generate AR Aging Report.
     */
    public AgingReportDTO generateARAgingReport(GenerateReportRequest request) {
        Long tenantId = securityContextHelper.getRequiredTenantId();
        LocalDate asOfDate = request.getEndDate() != null ? request.getEndDate() : today(tenantId);

        log.info("Generating AR Aging report for tenant {} as of {}", tenantId, asOfDate);

        // Single query: open invoices (NOT paid/cancelled/draft/written-off) dated on or before asOfDate.
        // Includes OVERDUE and DISPUTED statuses, which were silently dropped by the previous filter.
        List<ARInvoice> invoices = arInvoiceRepository.findUnpaidInvoicesAsOf(
                tenantId, asOfDate, AR_EXCLUDED_STATUSES);

        // Group by customer driven from the invoice side so inactive customers with debt are still reported.
        Map<Long, List<ARInvoice>> invoicesByCustomer = new LinkedHashMap<>();
        for (ARInvoice invoice : invoices) {
            if (invoice.getCustomer() == null) continue;
            invoicesByCustomer
                    .computeIfAbsent(invoice.getCustomer().getId(), k -> new ArrayList<>())
                    .add(invoice);
        }

        Map<Long, Customer> customerById = customerRepository
                .findAllById(invoicesByCustomer.keySet())
                .stream()
                .collect(java.util.stream.Collectors.toMap(Customer::getId, c -> c));

        List<AgingReportDTO.AgingDetail> details = new ArrayList<>();
        BigDecimal totalOutstanding = BigDecimal.ZERO;
        BigDecimal totalCurrent = BigDecimal.ZERO;
        BigDecimal total1to30 = BigDecimal.ZERO;
        BigDecimal total31to60 = BigDecimal.ZERO;
        BigDecimal total61to90 = BigDecimal.ZERO;
        BigDecimal totalOver90 = BigDecimal.ZERO;
        int totalAccounts = 0;
        int overdueAccounts = 0;

        for (Map.Entry<Long, List<ARInvoice>> entry : invoicesByCustomer.entrySet()) {
            Long customerId = entry.getKey();
            List<ARInvoice> custInvoices = entry.getValue();

            BigDecimal current = BigDecimal.ZERO;
            BigDecimal days1to30 = BigDecimal.ZERO;
            BigDecimal days31to60 = BigDecimal.ZERO;
            BigDecimal days61to90 = BigDecimal.ZERO;
            BigDecimal over90Days = BigDecimal.ZERO;
            BigDecimal customerTotal = BigDecimal.ZERO;
            int overdueInvoices = 0;
            LocalDate oldestInvoiceDate = null;

            for (ARInvoice invoice : custInvoices) {
                BigDecimal balance = toBaseCurrency(invoice.getBalanceDue(), invoice.getExchangeRate());
                customerTotal = customerTotal.add(balance);

                LocalDate dueDate = invoice.getDueDate();
                long daysOverdue = dueDate != null
                        ? java.time.temporal.ChronoUnit.DAYS.between(dueDate, asOfDate)
                        : 0L;

                if (daysOverdue <= 0) {
                    current = current.add(balance);
                } else if (daysOverdue <= 30) {
                    days1to30 = days1to30.add(balance);
                    overdueInvoices++;
                } else if (daysOverdue <= 60) {
                    days31to60 = days31to60.add(balance);
                    overdueInvoices++;
                } else if (daysOverdue <= 90) {
                    days61to90 = days61to90.add(balance);
                    overdueInvoices++;
                } else {
                    over90Days = over90Days.add(balance);
                    overdueInvoices++;
                }

                if (invoice.getInvoiceDate() != null
                        && (oldestInvoiceDate == null || invoice.getInvoiceDate().isBefore(oldestInvoiceDate))) {
                    oldestInvoiceDate = invoice.getInvoiceDate();
                }
            }

            if (customerTotal.compareTo(BigDecimal.ZERO) > 0) {
                Customer customer = customerById.get(customerId);
                details.add(AgingReportDTO.AgingDetail.builder()
                        .entityId(customerId)
                        .entityCode(customer != null ? customer.getCode() : null)
                        .entityName(customer != null ? customer.getName() : custInvoices.get(0).getCustomerName())
                        .contactInfo(customer != null ? customer.getPhone() : null)
                        .creditLimit(customer != null ? customer.getCreditLimit() : null)
                        .totalOutstanding(customerTotal)
                        .current(current)
                        .days1to30(days1to30)
                        .days31to60(days31to60)
                        .days61to90(days61to90)
                        .over90Days(over90Days)
                        .overdueInvoices(overdueInvoices)
                        .oldestInvoiceDate(oldestInvoiceDate)
                        .build());

                totalOutstanding = totalOutstanding.add(customerTotal);
                totalCurrent = totalCurrent.add(current);
                total1to30 = total1to30.add(days1to30);
                total31to60 = total31to60.add(days31to60);
                total61to90 = total61to90.add(days61to90);
                totalOver90 = totalOver90.add(over90Days);
                totalAccounts++;
                if (overdueInvoices > 0) overdueAccounts++;
            }
        }

        // Sort by total outstanding descending
        details.sort((a, b) -> b.getTotalOutstanding().compareTo(a.getTotalOutstanding()));

        return AgingReportDTO.builder()
                .metadata(AgingReportDTO.ReportMetadata.builder()
                        .reportName("Accounts Receivable Aging Report")
                        .reportType("AR")
                        .generatedAt(Instant.now())
                        .asOfDate(asOfDate)
                        .build())
                .summary(AgingReportDTO.Summary.builder()
                        .totalOutstanding(totalOutstanding)
                        .current(totalCurrent)
                        .days1to30(total1to30)
                        .days31to60(total31to60)
                        .days61to90(total61to90)
                        .over90Days(totalOver90)
                        .totalAccounts(totalAccounts)
                        .overdueAccounts(overdueAccounts)
                        .build())
                .details(details)
                .build();
    }

    /**
     * Convert an invoice amount to base currency using its stored exchange rate.
     * Falls back to the raw amount when rate is missing or non-positive.
     */
    private BigDecimal toBaseCurrency(BigDecimal amount, BigDecimal exchangeRate) {
        if (amount == null) return BigDecimal.ZERO;
        if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) <= 0) return amount;
        return amount.multiply(exchangeRate);
    }

    /**
     * Generate AP Aging Report.
     */
    public AgingReportDTO generateAPAgingReport(GenerateReportRequest request) {
        Long tenantId = securityContextHelper.getRequiredTenantId();
        LocalDate asOfDate = request.getEndDate() != null ? request.getEndDate() : today(tenantId);

        log.info("Generating AP Aging report for tenant {} as of {}", tenantId, asOfDate);

        // Open invoices (NOT paid/cancelled/draft) dated on or before asOfDate.
        // Includes ON_HOLD which was previously dropped by the IN-list filter.
        List<APInvoice> allInvoices = apInvoiceRepository.findUnpaidInvoicesAsOf(
                tenantId, asOfDate, AP_EXCLUDED_STATUSES);

        // Group by vendor (preserve due-date ordering from the query)
        Map<Long, List<APInvoice>> invoicesByVendor = new LinkedHashMap<>();
        for (APInvoice invoice : allInvoices) {
            Long vendorId = invoice.getVendorId();
            if (vendorId == null) continue;
            invoicesByVendor.computeIfAbsent(vendorId, k -> new ArrayList<>()).add(invoice);
        }

        // Batch lookup of real vendor records — provides current code/name/phone/credit limit
        // instead of synthetic "V-{id}" and stale denormalized snapshots.
        Map<Long, Vendor> vendorById = vendorRepository
                .findAllById(invoicesByVendor.keySet())
                .stream()
                .collect(java.util.stream.Collectors.toMap(Vendor::getId, v -> v));

        List<AgingReportDTO.AgingDetail> details = new ArrayList<>();
        BigDecimal totalOutstanding = BigDecimal.ZERO;
        BigDecimal totalCurrent = BigDecimal.ZERO;
        BigDecimal total1to30 = BigDecimal.ZERO;
        BigDecimal total31to60 = BigDecimal.ZERO;
        BigDecimal total61to90 = BigDecimal.ZERO;
        BigDecimal totalOver90 = BigDecimal.ZERO;
        int totalAccounts = 0;
        int overdueAccounts = 0;

        for (Map.Entry<Long, List<APInvoice>> entry : invoicesByVendor.entrySet()) {
            Long vendorId = entry.getKey();
            List<APInvoice> invoices = entry.getValue();

            BigDecimal current = BigDecimal.ZERO;
            BigDecimal days1to30 = BigDecimal.ZERO;
            BigDecimal days31to60 = BigDecimal.ZERO;
            BigDecimal days61to90 = BigDecimal.ZERO;
            BigDecimal over90Days = BigDecimal.ZERO;
            BigDecimal vendorTotal = BigDecimal.ZERO;
            int overdueInvoices = 0;
            LocalDate oldestInvoiceDate = null;

            for (APInvoice invoice : invoices) {
                BigDecimal balance = toBaseCurrency(invoice.getBalanceDue(), invoice.getExchangeRate());
                vendorTotal = vendorTotal.add(balance);

                LocalDate dueDate = invoice.getDueDate();
                long daysOverdue = dueDate != null
                        ? java.time.temporal.ChronoUnit.DAYS.between(dueDate, asOfDate)
                        : 0L;

                if (daysOverdue <= 0) {
                    current = current.add(balance);
                } else if (daysOverdue <= 30) {
                    days1to30 = days1to30.add(balance);
                    overdueInvoices++;
                } else if (daysOverdue <= 60) {
                    days31to60 = days31to60.add(balance);
                    overdueInvoices++;
                } else if (daysOverdue <= 90) {
                    days61to90 = days61to90.add(balance);
                    overdueInvoices++;
                } else {
                    over90Days = over90Days.add(balance);
                    overdueInvoices++;
                }

                if (invoice.getInvoiceDate() != null
                        && (oldestInvoiceDate == null || invoice.getInvoiceDate().isBefore(oldestInvoiceDate))) {
                    oldestInvoiceDate = invoice.getInvoiceDate();
                }
            }

            if (vendorTotal.compareTo(BigDecimal.ZERO) > 0) {
                Vendor vendor = vendorById.get(vendorId);
                String entityCode = vendor != null ? vendor.getCode() : "V-" + vendorId;
                String entityName = vendor != null ? vendor.getName() : invoices.get(0).getVendorName();
                String contactInfo = vendor != null ? vendor.getPhone() : null;
                BigDecimal creditLimit = vendor != null ? vendor.getCreditLimit() : null;

                details.add(AgingReportDTO.AgingDetail.builder()
                        .entityId(vendorId)
                        .entityCode(entityCode)
                        .entityName(entityName)
                        .contactInfo(contactInfo)
                        .creditLimit(creditLimit)
                        .totalOutstanding(vendorTotal)
                        .current(current)
                        .days1to30(days1to30)
                        .days31to60(days31to60)
                        .days61to90(days61to90)
                        .over90Days(over90Days)
                        .overdueInvoices(overdueInvoices)
                        .oldestInvoiceDate(oldestInvoiceDate)
                        .build());

                totalOutstanding = totalOutstanding.add(vendorTotal);
                totalCurrent = totalCurrent.add(current);
                total1to30 = total1to30.add(days1to30);
                total31to60 = total31to60.add(days31to60);
                total61to90 = total61to90.add(days61to90);
                totalOver90 = totalOver90.add(over90Days);
                totalAccounts++;
                if (overdueInvoices > 0) overdueAccounts++;
            }
        }

        // Sort by total outstanding descending
        details.sort((a, b) -> b.getTotalOutstanding().compareTo(a.getTotalOutstanding()));

        return AgingReportDTO.builder()
                .metadata(AgingReportDTO.ReportMetadata.builder()
                        .reportName("Accounts Payable Aging Report")
                        .reportType("AP")
                        .generatedAt(Instant.now())
                        .asOfDate(asOfDate)
                        .build())
                .summary(AgingReportDTO.Summary.builder()
                        .totalOutstanding(totalOutstanding)
                        .current(totalCurrent)
                        .days1to30(total1to30)
                        .days31to60(total31to60)
                        .days61to90(total61to90)
                        .over90Days(totalOver90)
                        .totalAccounts(totalAccounts)
                        .overdueAccounts(overdueAccounts)
                        .build())
                .details(details)
                .build();
    }
}
