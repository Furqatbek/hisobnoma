package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.finance.dto.APAgingReportDto;
import com.hisobnoma.platform.finance.dto.APAgingReportDto.VendorAgingDto;
import com.hisobnoma.platform.finance.dto.VendorBalanceReportDto;
import com.hisobnoma.platform.finance.dto.VendorBalanceReportDto.VendorBalanceDto;
import com.hisobnoma.platform.finance.entity.APInvoice;
import com.hisobnoma.platform.finance.entity.APInvoiceStatus;
import com.hisobnoma.platform.finance.entity.APPaymentStatus;
import com.hisobnoma.platform.finance.repository.APInvoiceRepository;
import com.hisobnoma.platform.finance.repository.APPaymentRepository;
import com.hisobnoma.platform.inventory.entity.Vendor;
import com.hisobnoma.platform.inventory.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class APReportService {

    private final APInvoiceRepository apInvoiceRepository;
    private final APPaymentRepository apPaymentRepository;
    private final VendorRepository vendorRepository;
    private final SecurityContextHelper securityContextHelper;

    private static final List<APInvoiceStatus> EXCLUDED_STATUSES = Arrays.asList(
            APInvoiceStatus.CANCELLED, APInvoiceStatus.PAID
    );

    /**
     * Generates an AP Aging Report showing outstanding payables by aging buckets.
     */
    @Transactional(readOnly = true)
    public APAgingReportDto generateAgingReport() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        LocalDate today = LocalDate.now();

        // Get all unpaid invoices
        List<APInvoiceStatus> payableStatuses = Arrays.asList(
                APInvoiceStatus.APPROVED, APInvoiceStatus.PARTIAL
        );
        List<APInvoice> unpaidInvoices = apInvoiceRepository.findUnpaidInvoices(tenantId, payableStatuses);

        // Group invoices by vendor and calculate aging
        Map<Long, List<APInvoice>> invoicesByVendor = unpaidInvoices.stream()
                .collect(Collectors.groupingBy(APInvoice::getVendorId));

        // Get vendor details
        List<Vendor> vendors = vendorRepository.findAllByTenantId(tenantId);
        Map<Long, Vendor> vendorMap = vendors.stream()
                .collect(Collectors.toMap(Vendor::getId, v -> v));

        // Calculate aging buckets
        BigDecimal totalCurrent = BigDecimal.ZERO;
        BigDecimal total1To30 = BigDecimal.ZERO;
        BigDecimal total31To60 = BigDecimal.ZERO;
        BigDecimal total61To90 = BigDecimal.ZERO;
        BigDecimal totalOver90 = BigDecimal.ZERO;

        List<VendorAgingDto> vendorAgingList = new ArrayList<>();

        for (Map.Entry<Long, List<APInvoice>> entry : invoicesByVendor.entrySet()) {
            Long vendorId = entry.getKey();
            List<APInvoice> vendorInvoices = entry.getValue();
            Vendor vendor = vendorMap.get(vendorId);

            BigDecimal current = BigDecimal.ZERO;
            BigDecimal overdue1To30 = BigDecimal.ZERO;
            BigDecimal overdue31To60 = BigDecimal.ZERO;
            BigDecimal overdue61To90 = BigDecimal.ZERO;
            BigDecimal overdueOver90 = BigDecimal.ZERO;

            for (APInvoice invoice : vendorInvoices) {
                int daysOverdue = invoice.getDaysOverdue();
                BigDecimal balance = invoice.getBalanceDue();

                if (daysOverdue <= 0) {
                    current = current.add(balance);
                } else if (daysOverdue <= 30) {
                    overdue1To30 = overdue1To30.add(balance);
                } else if (daysOverdue <= 60) {
                    overdue31To60 = overdue31To60.add(balance);
                } else if (daysOverdue <= 90) {
                    overdue61To90 = overdue61To90.add(balance);
                } else {
                    overdueOver90 = overdueOver90.add(balance);
                }
            }

            BigDecimal vendorTotal = current.add(overdue1To30).add(overdue31To60)
                    .add(overdue61To90).add(overdueOver90);

            VendorAgingDto vendorAging = VendorAgingDto.builder()
                    .vendorId(vendorId)
                    .vendorCode(vendor != null ? vendor.getCode() : "")
                    .vendorName(vendor != null ? vendor.getName() : "Unknown")
                    .totalBalance(vendorTotal)
                    .current(current)
                    .overdue1To30(overdue1To30)
                    .overdue31To60(overdue31To60)
                    .overdue61To90(overdue61To90)
                    .overdueOver90(overdueOver90)
                    .invoiceCount(vendorInvoices.size())
                    .build();

            vendorAgingList.add(vendorAging);

            // Add to totals
            totalCurrent = totalCurrent.add(current);
            total1To30 = total1To30.add(overdue1To30);
            total31To60 = total31To60.add(overdue31To60);
            total61To90 = total61To90.add(overdue61To90);
            totalOver90 = totalOver90.add(overdueOver90);
        }

        // Sort by total balance descending
        vendorAgingList.sort((a, b) -> b.getTotalBalance().compareTo(a.getTotalBalance()));

        BigDecimal totalPayable = totalCurrent.add(total1To30).add(total31To60)
                .add(total61To90).add(totalOver90);

        return APAgingReportDto.builder()
                .reportDate(today)
                .tenantId(tenantId)
                .totalPayable(totalPayable)
                .current(totalCurrent)
                .overdue1To30(total1To30)
                .overdue31To60(total31To60)
                .overdue61To90(total61To90)
                .overdueOver90(totalOver90)
                .vendorAging(vendorAgingList)
                .build();
    }

    /**
     * Generates a Vendor Balance Report showing balances for all vendors.
     */
    @Transactional(readOnly = true)
    public VendorBalanceReportDto generateVendorBalanceReport() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        LocalDate today = LocalDate.now();

        // Get all active vendors
        List<Vendor> vendors = vendorRepository.findAllActiveByTenantId(tenantId);

        BigDecimal totalPayable = BigDecimal.ZERO;
        BigDecimal totalPayments = BigDecimal.ZERO;
        List<VendorBalanceDto> vendorBalances = new ArrayList<>();

        for (Vendor vendor : vendors) {
            // Get invoice total
            BigDecimal invoiceBalance = apInvoiceRepository.sumBalanceDueByVendor(
                    tenantId, vendor.getId(), EXCLUDED_STATUSES);
            if (invoiceBalance == null) invoiceBalance = BigDecimal.ZERO;

            // Get payment total
            BigDecimal paymentTotal = apPaymentRepository.sumPaymentsByVendorAndStatus(
                    tenantId, vendor.getId(), APPaymentStatus.COMPLETED);
            if (paymentTotal == null) paymentTotal = BigDecimal.ZERO;

            // Get open invoice count
            List<APInvoiceStatus> payableStatuses = Arrays.asList(
                    APInvoiceStatus.APPROVED, APInvoiceStatus.PARTIAL
            );
            List<APInvoice> openInvoices = apInvoiceRepository.findUnpaidInvoicesByVendor(
                    tenantId, vendor.getId(), payableStatuses);

            // Calculate available credit
            BigDecimal creditLimit = vendor.getCreditLimit() != null ? vendor.getCreditLimit() : BigDecimal.ZERO;
            BigDecimal availableCredit = creditLimit.subtract(invoiceBalance);
            if (availableCredit.compareTo(BigDecimal.ZERO) < 0) {
                availableCredit = BigDecimal.ZERO;
            }

            // Get last invoice and payment dates
            LocalDate lastInvoiceDate = openInvoices.stream()
                    .map(APInvoice::getInvoiceDate)
                    .max(LocalDate::compareTo)
                    .orElse(null);

            VendorBalanceDto vendorBalance = VendorBalanceDto.builder()
                    .vendorId(vendor.getId())
                    .vendorCode(vendor.getCode())
                    .vendorName(vendor.getName())
                    .contactPerson(vendor.getContactPerson())
                    .email(vendor.getEmail())
                    .phone(vendor.getPhone())
                    .creditLimit(creditLimit)
                    .totalInvoiced(invoiceBalance.add(paymentTotal))
                    .totalPaid(paymentTotal)
                    .currentBalance(invoiceBalance)
                    .availableCredit(availableCredit)
                    .openInvoiceCount(openInvoices.size())
                    .lastInvoiceDate(lastInvoiceDate)
                    .build();

            vendorBalances.add(vendorBalance);
            totalPayable = totalPayable.add(invoiceBalance);
            totalPayments = totalPayments.add(paymentTotal);
        }

        // Sort by current balance descending
        vendorBalances.sort((a, b) -> b.getCurrentBalance().compareTo(a.getCurrentBalance()));

        // Filter out vendors with zero balance if needed
        vendorBalances = vendorBalances.stream()
                .filter(v -> v.getCurrentBalance().compareTo(BigDecimal.ZERO) != 0 ||
                            v.getOpenInvoiceCount() > 0)
                .collect(Collectors.toList());

        return VendorBalanceReportDto.builder()
                .reportDate(today)
                .tenantId(tenantId)
                .totalPayable(totalPayable)
                .totalPayments(totalPayments)
                .netBalance(totalPayable)
                .vendorCount(vendorBalances.size())
                .vendorBalances(vendorBalances)
                .build();
    }

    /**
     * Gets vendor statement - all invoices and payments for a specific vendor.
     */
    @Transactional(readOnly = true)
    public VendorBalanceDto getVendorStatement(Long vendorId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Vendor vendor = vendorRepository.findByIdAndTenantId(vendorId, tenantId)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        BigDecimal invoiceBalance = apInvoiceRepository.sumBalanceDueByVendor(
                tenantId, vendorId, EXCLUDED_STATUSES);
        if (invoiceBalance == null) invoiceBalance = BigDecimal.ZERO;

        BigDecimal paymentTotal = apPaymentRepository.sumPaymentsByVendorAndStatus(
                tenantId, vendorId, APPaymentStatus.COMPLETED);
        if (paymentTotal == null) paymentTotal = BigDecimal.ZERO;

        List<APInvoiceStatus> payableStatuses = Arrays.asList(
                APInvoiceStatus.APPROVED, APInvoiceStatus.PARTIAL
        );
        List<APInvoice> openInvoices = apInvoiceRepository.findUnpaidInvoicesByVendor(
                tenantId, vendorId, payableStatuses);

        BigDecimal creditLimit = vendor.getCreditLimit() != null ? vendor.getCreditLimit() : BigDecimal.ZERO;
        BigDecimal availableCredit = creditLimit.subtract(invoiceBalance);
        if (availableCredit.compareTo(BigDecimal.ZERO) < 0) {
            availableCredit = BigDecimal.ZERO;
        }

        LocalDate lastInvoiceDate = openInvoices.stream()
                .map(APInvoice::getInvoiceDate)
                .max(LocalDate::compareTo)
                .orElse(null);

        return VendorBalanceDto.builder()
                .vendorId(vendor.getId())
                .vendorCode(vendor.getCode())
                .vendorName(vendor.getName())
                .contactPerson(vendor.getContactPerson())
                .email(vendor.getEmail())
                .phone(vendor.getPhone())
                .creditLimit(creditLimit)
                .totalInvoiced(invoiceBalance.add(paymentTotal))
                .totalPaid(paymentTotal)
                .currentBalance(invoiceBalance)
                .availableCredit(availableCredit)
                .openInvoiceCount(openInvoices.size())
                .lastInvoiceDate(lastInvoiceDate)
                .build();
    }
}
