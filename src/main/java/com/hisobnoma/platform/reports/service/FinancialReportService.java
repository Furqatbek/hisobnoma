package com.hisobnoma.platform.reports.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.finance.entity.*;
import com.hisobnoma.platform.finance.repository.*;
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
    private final TenantRepository tenantRepository;

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
     */
    public TrialBalanceReportDTO generateTrialBalanceReport(GenerateReportRequest request) {
        Long tenantId = securityContextHelper.getRequiredTenantId();

        LocalDate asOfDate = request.getEndDate() != null ? request.getEndDate() : LocalDate.now();
        log.info("Generating Trial Balance report for tenant {} as of {}", tenantId, asOfDate);

        List<Account> accounts = accountRepository.findAllActiveByTenantId(tenantId);
        accounts.sort(Comparator.comparing(Account::getCode));

        List<TrialBalanceReportDTO.AccountBalance> balances = new ArrayList<>();
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (Account account : accounts) {
            BigDecimal debitBalance = BigDecimal.ZERO;
            BigDecimal creditBalance = BigDecimal.ZERO;

            // Get opening balance
            BigDecimal openingBalance = account.getOpeningBalance() != null ? account.getOpeningBalance() : BigDecimal.ZERO;

            // Get YTD debits and credits
            BigDecimal ytdDebit = account.getYtdDebit() != null ? account.getYtdDebit() : BigDecimal.ZERO;
            BigDecimal ytdCredit = account.getYtdCredit() != null ? account.getYtdCredit() : BigDecimal.ZERO;

            // Calculate net balance
            BigDecimal netBalance = openingBalance.add(ytdDebit).subtract(ytdCredit);

            // Determine if it's a debit or credit balance based on account type
            if (account.getNormalBalance() == NormalBalance.DEBIT) {
                if (netBalance.compareTo(BigDecimal.ZERO) >= 0) {
                    debitBalance = netBalance;
                } else {
                    creditBalance = netBalance.abs();
                }
            } else {
                netBalance = openingBalance.add(ytdCredit).subtract(ytdDebit);
                if (netBalance.compareTo(BigDecimal.ZERO) >= 0) {
                    creditBalance = netBalance;
                } else {
                    debitBalance = netBalance.abs();
                }
            }

            // Only include accounts with balances
            if (debitBalance.compareTo(BigDecimal.ZERO) != 0 || creditBalance.compareTo(BigDecimal.ZERO) != 0) {
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

        BigDecimal difference = totalDebits.subtract(totalCredits).abs();
        boolean isBalanced = difference.compareTo(new BigDecimal("0.01")) < 0;

        return TrialBalanceReportDTO.builder()
                .metadata(TrialBalanceReportDTO.ReportMetadata.builder()
                        .reportName("Trial Balance")
                        .generatedAt(Instant.now())
                        .asOfDate(asOfDate)
                        .fiscalYear(String.valueOf(asOfDate.getYear()))
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

        // Group by vendor
        Map<Long, List<APInvoice>> invoicesByVendor = new HashMap<>();
        Map<Long, String> vendorNames = new HashMap<>();
        Map<Long, String> vendorCodes = new HashMap<>();

        for (APInvoice invoice : allInvoices) {
            Long vendorId = invoice.getVendorId();
            invoicesByVendor.computeIfAbsent(vendorId, k -> new ArrayList<>()).add(invoice);
            if (!vendorNames.containsKey(vendorId)) {
                vendorNames.put(vendorId, invoice.getVendorName());
                vendorCodes.put(vendorId, "V-" + vendorId);
            }
        }

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
                details.add(AgingReportDTO.AgingDetail.builder()
                        .entityId(vendorId)
                        .entityCode(vendorCodes.get(vendorId))
                        .entityName(vendorNames.get(vendorId))
                        .contactInfo(null)
                        .creditLimit(null)
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
