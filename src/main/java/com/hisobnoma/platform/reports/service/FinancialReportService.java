package com.hisobnoma.platform.reports.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.finance.entity.*;
import com.hisobnoma.platform.finance.repository.*;
import com.hisobnoma.platform.reports.dto.AgingReportDTO;
import com.hisobnoma.platform.reports.dto.GenerateReportRequest;
import com.hisobnoma.platform.reports.dto.TrialBalanceReportDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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

    private static final List<ARInvoiceStatus> AR_EXCLUDED_STATUSES = Arrays.asList(
            ARInvoiceStatus.CANCELLED, ARInvoiceStatus.PAID);
    private static final List<APInvoiceStatus> AP_EXCLUDED_STATUSES = Arrays.asList(
            APInvoiceStatus.CANCELLED, APInvoiceStatus.PAID);

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
     * Generate AR Aging Report.
     */
    public AgingReportDTO generateARAgingReport(GenerateReportRequest request) {
        Long tenantId = securityContextHelper.getRequiredTenantId();
        LocalDate asOfDate = request.getEndDate() != null ? request.getEndDate() : LocalDate.now();

        log.info("Generating AR Aging report for tenant {} as of {}", tenantId, asOfDate);

        List<Customer> customers = customerRepository.findAllActiveByTenantId(tenantId);

        List<AgingReportDTO.AgingDetail> details = new ArrayList<>();
        BigDecimal totalOutstanding = BigDecimal.ZERO;
        BigDecimal totalCurrent = BigDecimal.ZERO;
        BigDecimal total1to30 = BigDecimal.ZERO;
        BigDecimal total31to60 = BigDecimal.ZERO;
        BigDecimal total61to90 = BigDecimal.ZERO;
        BigDecimal totalOver90 = BigDecimal.ZERO;
        int totalAccounts = 0;
        int overdueAccounts = 0;

        for (Customer customer : customers) {
            List<ARInvoice> invoices = arInvoiceRepository.findByTenantIdAndCustomer_IdAndStatusIn(
                    tenantId, customer.getId(), Arrays.asList(ARInvoiceStatus.PENDING, ARInvoiceStatus.SENT, ARInvoiceStatus.PARTIAL));

            if (invoices.isEmpty()) continue;

            BigDecimal current = BigDecimal.ZERO;
            BigDecimal days1to30 = BigDecimal.ZERO;
            BigDecimal days31to60 = BigDecimal.ZERO;
            BigDecimal days61to90 = BigDecimal.ZERO;
            BigDecimal over90Days = BigDecimal.ZERO;
            BigDecimal customerTotal = BigDecimal.ZERO;
            int overdueInvoices = 0;
            LocalDate oldestInvoiceDate = null;

            for (ARInvoice invoice : invoices) {
                if (invoice.getBalanceDue() == null || invoice.getBalanceDue().compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                BigDecimal balance = invoice.getBalanceDue();
                customerTotal = customerTotal.add(balance);

                LocalDate dueDate = invoice.getDueDate();
                long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(dueDate, asOfDate);

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

                if (oldestInvoiceDate == null || invoice.getInvoiceDate().isBefore(oldestInvoiceDate)) {
                    oldestInvoiceDate = invoice.getInvoiceDate();
                }
            }

            if (customerTotal.compareTo(BigDecimal.ZERO) > 0) {
                details.add(AgingReportDTO.AgingDetail.builder()
                        .entityId(customer.getId())
                        .entityCode(customer.getCode())
                        .entityName(customer.getName())
                        .contactInfo(customer.getPhone())
                        .creditLimit(customer.getCreditLimit())
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
     * Generate AP Aging Report.
     */
    public AgingReportDTO generateAPAgingReport(GenerateReportRequest request) {
        Long tenantId = securityContextHelper.getRequiredTenantId();
        LocalDate asOfDate = request.getEndDate() != null ? request.getEndDate() : LocalDate.now();

        log.info("Generating AP Aging report for tenant {} as of {}", tenantId, asOfDate);

        // Get all unpaid AP invoices
        List<APInvoice> allInvoices = apInvoiceRepository.findUnpaidInvoices(tenantId,
                Arrays.asList(APInvoiceStatus.PENDING_APPROVAL, APInvoiceStatus.APPROVED, APInvoiceStatus.PARTIAL));

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
                if (invoice.getBalanceDue() == null || invoice.getBalanceDue().compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                BigDecimal balance = invoice.getBalanceDue();
                vendorTotal = vendorTotal.add(balance);

                LocalDate dueDate = invoice.getDueDate();
                long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(dueDate, asOfDate);

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

                if (oldestInvoiceDate == null || invoice.getInvoiceDate().isBefore(oldestInvoiceDate)) {
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
