package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.finance.dto.ARAgingReportDto;
import com.hisobnoma.platform.finance.dto.CustomerBalanceReportDto;
import com.hisobnoma.platform.finance.entity.ARInvoice;
import com.hisobnoma.platform.finance.entity.ARInvoiceStatus;
import com.hisobnoma.platform.finance.entity.CreditNote;
import com.hisobnoma.platform.finance.entity.CreditNoteStatus;
import com.hisobnoma.platform.finance.entity.Customer;
import com.hisobnoma.platform.finance.repository.ARInvoiceRepository;
import com.hisobnoma.platform.finance.repository.ARPaymentRepository;
import com.hisobnoma.platform.finance.repository.CreditNoteRepository;
import com.hisobnoma.platform.finance.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ARReportService {

    private final ARInvoiceRepository arInvoiceRepository;
    private final ARPaymentRepository arPaymentRepository;
    private final CreditNoteRepository creditNoteRepository;
    private final CustomerRepository customerRepository;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public ARAgingReportDto generateAgingReport(LocalDate asOfDate) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        List<ARInvoiceStatus> unpaidStatuses = List.of(
                ARInvoiceStatus.PENDING, ARInvoiceStatus.SENT,
                ARInvoiceStatus.PARTIAL, ARInvoiceStatus.OVERDUE
        );

        // Get all unpaid invoices
        List<ARInvoice> unpaidInvoices = arInvoiceRepository.findByTenantIdAndStatusIn(tenantId, unpaidStatuses);

        // Initialize totals
        BigDecimal totalCurrent = BigDecimal.ZERO;
        BigDecimal total1To30 = BigDecimal.ZERO;
        BigDecimal total31To60 = BigDecimal.ZERO;
        BigDecimal total61To90 = BigDecimal.ZERO;
        BigDecimal totalOver90 = BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;

        // Group by customer
        Map<Long, ARAgingReportDto.CustomerAgingDto> customerAgingMap = new HashMap<>();

        for (ARInvoice invoice : unpaidInvoices) {
            Long customerId = invoice.getCustomer().getId();
            Customer customer = invoice.getCustomer();

            ARAgingReportDto.CustomerAgingDto customerAging = customerAgingMap.computeIfAbsent(
                    customerId, k -> ARAgingReportDto.CustomerAgingDto.builder()
                            .customerId(customerId)
                            .customerCode(customer.getCode())
                            .customerName(customer.getName())
                            .currentAmount(BigDecimal.ZERO)
                            .days1To30(BigDecimal.ZERO)
                            .days31To60(BigDecimal.ZERO)
                            .days61To90(BigDecimal.ZERO)
                            .over90Days(BigDecimal.ZERO)
                            .totalBalance(BigDecimal.ZERO)
                            .creditLimit(customer.getCreditLimit())
                            .availableCredit(calculateAvailableCredit(customer))
                            .build()
            );

            BigDecimal balance = invoice.getBalanceDue();
            long daysOverdue = ChronoUnit.DAYS.between(invoice.getDueDate(), asOfDate);

            if (daysOverdue <= 0) {
                // Current (not yet due)
                customerAging.setCurrentAmount(customerAging.getCurrentAmount().add(balance));
                totalCurrent = totalCurrent.add(balance);
            } else if (daysOverdue <= 30) {
                customerAging.setDays1To30(customerAging.getDays1To30().add(balance));
                total1To30 = total1To30.add(balance);
            } else if (daysOverdue <= 60) {
                customerAging.setDays31To60(customerAging.getDays31To60().add(balance));
                total31To60 = total31To60.add(balance);
            } else if (daysOverdue <= 90) {
                customerAging.setDays61To90(customerAging.getDays61To90().add(balance));
                total61To90 = total61To90.add(balance);
            } else {
                customerAging.setOver90Days(customerAging.getOver90Days().add(balance));
                totalOver90 = totalOver90.add(balance);
            }

            customerAging.setTotalBalance(customerAging.getTotalBalance().add(balance));
            grandTotal = grandTotal.add(balance);
        }

        // Sort customers by total balance descending
        List<ARAgingReportDto.CustomerAgingDto> customerAgingList = new ArrayList<>(customerAgingMap.values());
        customerAgingList.sort((a, b) -> b.getTotalBalance().compareTo(a.getTotalBalance()));

        return ARAgingReportDto.builder()
                .reportDate(asOfDate)
                .tenantId(tenantId)
                .customerAgingList(customerAgingList)
                .totalCurrent(totalCurrent)
                .total1To30Days(total1To30)
                .total31To60Days(total31To60)
                .total61To90Days(total61To90)
                .totalOver90Days(totalOver90)
                .grandTotal(grandTotal)
                .customerCount(customerAgingList.size())
                .build();
    }

    @Transactional(readOnly = true)
    public CustomerBalanceReportDto generateCustomerBalanceReport() {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        // Get all active customers
        List<Customer> customers = customerRepository.findAllActiveByTenantId(tenantId);

        List<CustomerBalanceReportDto.CustomerBalanceDto> customerBalances = new ArrayList<>();

        BigDecimal totalReceivables = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        BigDecimal totalNetBalance = BigDecimal.ZERO;
        int customersOverLimit = 0;
        int customersOnHold = 0;

        List<ARInvoiceStatus> unpaidStatuses = List.of(
                ARInvoiceStatus.PENDING, ARInvoiceStatus.SENT,
                ARInvoiceStatus.PARTIAL, ARInvoiceStatus.OVERDUE
        );

        List<CreditNoteStatus> availableCreditStatuses = List.of(
                CreditNoteStatus.APPROVED, CreditNoteStatus.PARTIAL
        );

        for (Customer customer : customers) {
            // Calculate outstanding invoices
            BigDecimal outstandingInvoices = arInvoiceRepository.sumBalanceDueByCustomer(
                    tenantId, customer.getId(), unpaidStatuses);
            if (outstandingInvoices == null) {
                outstandingInvoices = BigDecimal.ZERO;
            }

            // Calculate available credits
            BigDecimal availableCredits = creditNoteRepository.sumAvailableCreditByCustomer(
                    tenantId, customer.getId(), availableCreditStatuses);
            if (availableCredits == null) {
                availableCredits = BigDecimal.ZERO;
            }

            BigDecimal netBalance = outstandingInvoices.subtract(availableCredits);

            // Calculate available credit limit
            BigDecimal availableCreditLimit = BigDecimal.ZERO;
            boolean overCreditLimit = false;

            if (customer.getCreditLimit() != null) {
                availableCreditLimit = customer.getCreditLimit().subtract(netBalance);
                if (availableCreditLimit.compareTo(BigDecimal.ZERO) < 0) {
                    overCreditLimit = true;
                    availableCreditLimit = BigDecimal.ZERO;
                    customersOverLimit++;
                }
            }

            if (customer.isCreditHold()) {
                customersOnHold++;
            }

            // Get last invoice and payment dates
            LocalDate lastInvoiceDate = arInvoiceRepository.findLatestInvoiceDateByCustomer(tenantId, customer.getId());
            LocalDate lastPaymentDate = null; // Would need to add this query

            CustomerBalanceReportDto.CustomerBalanceDto balance = CustomerBalanceReportDto.CustomerBalanceDto.builder()
                    .customerId(customer.getId())
                    .customerCode(customer.getCode())
                    .customerName(customer.getName())
                    .outstandingInvoices(outstandingInvoices)
                    .availableCredits(availableCredits)
                    .netBalance(netBalance)
                    .creditLimit(customer.getCreditLimit())
                    .availableCreditLimit(availableCreditLimit)
                    .overCreditLimit(overCreditLimit)
                    .onCreditHold(customer.isCreditHold())
                    .lastInvoiceDate(lastInvoiceDate)
                    .lastPaymentDate(lastPaymentDate)
                    .paymentTerms(customer.getPaymentTermsDays())
                    .build();

            customerBalances.add(balance);

            totalReceivables = totalReceivables.add(outstandingInvoices);
            totalCredits = totalCredits.add(availableCredits);
            totalNetBalance = totalNetBalance.add(netBalance);
        }

        // Sort by net balance descending
        customerBalances.sort((a, b) -> b.getNetBalance().compareTo(a.getNetBalance()));

        return CustomerBalanceReportDto.builder()
                .reportDate(LocalDate.now())
                .tenantId(tenantId)
                .customerBalances(customerBalances)
                .totalReceivables(totalReceivables)
                .totalCredits(totalCredits)
                .totalNetBalance(totalNetBalance)
                .customerCount(customers.size())
                .customersOverCreditLimit(customersOverLimit)
                .customersOnCreditHold(customersOnHold)
                .build();
    }

    @Transactional(readOnly = true)
    public CustomerBalanceReportDto.CustomerBalanceDto getCustomerBalance(Long customerId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Customer customer = customerRepository.findByIdAndTenantId(customerId, tenantId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        List<ARInvoiceStatus> unpaidStatuses = List.of(
                ARInvoiceStatus.PENDING, ARInvoiceStatus.SENT,
                ARInvoiceStatus.PARTIAL, ARInvoiceStatus.OVERDUE
        );

        List<CreditNoteStatus> availableCreditStatuses = List.of(
                CreditNoteStatus.APPROVED, CreditNoteStatus.PARTIAL
        );

        BigDecimal outstandingInvoices = arInvoiceRepository.sumBalanceDueByCustomer(
                tenantId, customer.getId(), unpaidStatuses);
        if (outstandingInvoices == null) {
            outstandingInvoices = BigDecimal.ZERO;
        }

        BigDecimal availableCredits = creditNoteRepository.sumAvailableCreditByCustomer(
                tenantId, customer.getId(), availableCreditStatuses);
        if (availableCredits == null) {
            availableCredits = BigDecimal.ZERO;
        }

        BigDecimal netBalance = outstandingInvoices.subtract(availableCredits);

        BigDecimal availableCreditLimit = BigDecimal.ZERO;
        boolean overCreditLimit = false;

        if (customer.getCreditLimit() != null) {
            availableCreditLimit = customer.getCreditLimit().subtract(netBalance);
            if (availableCreditLimit.compareTo(BigDecimal.ZERO) < 0) {
                overCreditLimit = true;
                availableCreditLimit = BigDecimal.ZERO;
            }
        }

        return CustomerBalanceReportDto.CustomerBalanceDto.builder()
                .customerId(customer.getId())
                .customerCode(customer.getCode())
                .customerName(customer.getName())
                .outstandingInvoices(outstandingInvoices)
                .availableCredits(availableCredits)
                .netBalance(netBalance)
                .creditLimit(customer.getCreditLimit())
                .availableCreditLimit(availableCreditLimit)
                .overCreditLimit(overCreditLimit)
                .onCreditHold(customer.isCreditHold())
                .paymentTerms(customer.getPaymentTermsDays())
                .build();
    }

    private BigDecimal calculateAvailableCredit(Customer customer) {
        if (customer.getCreditLimit() == null) {
            return null;
        }

        BigDecimal currentBalance = customer.getCurrentBalance() != null ? customer.getCurrentBalance() : BigDecimal.ZERO;
        BigDecimal available = customer.getCreditLimit().subtract(currentBalance);

        return available.compareTo(BigDecimal.ZERO) > 0 ? available : BigDecimal.ZERO;
    }
}
