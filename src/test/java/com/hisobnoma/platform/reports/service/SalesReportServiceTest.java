package com.hisobnoma.platform.reports.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.inventory.entity.Category;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.pos.entity.*;
import com.hisobnoma.platform.pos.repository.POSTransactionRepository;
import com.hisobnoma.platform.reports.dto.GenerateReportRequest;
import com.hisobnoma.platform.reports.dto.SalesSummaryReportDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesReportServiceTest {

    @Mock
    private SecurityContextHelper securityContextHelper;

    @Mock
    private POSTransactionRepository transactionRepository;

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private SalesReportService salesReportService;

    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        UserPrincipal principal = new UserPrincipal(1L, "testuser", "password", TENANT_ID, true, true, List.of());
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getRequiredCurrentUser()).thenReturn(principal);
    }

    @Test
    void generateSalesSummaryReport_withSalesAndReturns_calculatesCorrectly() {
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 1);

        GenerateReportRequest request = GenerateReportRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build();

        Tenant tenant = Tenant.builder().timezone("UTC").build();
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));

        Category category = Category.builder().name("Electronics").build();
        category.setId(10L);

        Product product = Product.builder().sku("SKU-001").category(category).build();
        product.setId(100L);

        POSTransactionLine saleLine = POSTransactionLine.builder()
                .product(product)
                .productName("Test Product")
                .quantity(BigDecimal.valueOf(2))
                .unitPrice(BigDecimal.valueOf(50))
                .costPrice(BigDecimal.valueOf(30))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .lineTotal(BigDecimal.valueOf(100))
                .build();

        POSPayment cashPayment = POSPayment.builder()
                .paymentType(POSPaymentType.CASH)
                .amount(BigDecimal.valueOf(100))
                .status(POSPaymentStatus.APPROVED)
                .build();

        POSTransaction saleTx = POSTransaction.builder()
                .transactionType(TransactionType.SALE)
                .status(TransactionStatus.COMPLETED)
                .completedAt(startDate.atStartOfDay(ZoneOffset.UTC).toInstant())
                .discountAmount(BigDecimal.ZERO)
                .itemCount(2)
                .lines(new java.util.ArrayList<>(List.of(saleLine)))
                .payments(new java.util.ArrayList<>(List.of(cashPayment)))
                .build();

        POSTransactionLine returnLine = POSTransactionLine.builder()
                .product(product)
                .productName("Test Product")
                .quantity(BigDecimal.valueOf(-1))
                .unitPrice(BigDecimal.valueOf(50))
                .costPrice(BigDecimal.valueOf(30))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .lineTotal(BigDecimal.valueOf(-50))
                .isReturn(true)
                .build();

        POSTransaction returnTx = POSTransaction.builder()
                .transactionType(TransactionType.RETURN)
                .status(TransactionStatus.COMPLETED)
                .completedAt(startDate.atStartOfDay(ZoneOffset.UTC).toInstant())
                .discountAmount(BigDecimal.ZERO)
                .lines(new java.util.ArrayList<>(List.of(returnLine)))
                .payments(new java.util.ArrayList<>())
                .build();

        when(transactionRepository.findByTypeAndDateRangeAndTenantId(
                eq(TransactionType.SALE), any(Instant.class), any(Instant.class), eq(TENANT_ID)))
                .thenReturn(List.of(saleTx));
        when(transactionRepository.findByTypeAndDateRangeAndTenantId(
                eq(TransactionType.RETURN), any(Instant.class), any(Instant.class), eq(TENANT_ID)))
                .thenReturn(List.of(returnTx));

        SalesSummaryReportDTO result = salesReportService.generateSalesSummaryReport(request);

        assertThat(result).isNotNull();
        assertThat(result.getMetadata()).isNotNull();
        assertThat(result.getMetadata().getReportName()).isEqualTo("Sales Summary Report");
        assertThat(result.getMetadata().getStartDate()).isEqualTo(startDate);
        assertThat(result.getMetadata().getEndDate()).isEqualTo(endDate);

        SalesSummaryReportDTO.Summary summary = result.getSummary();
        assertThat(summary).isNotNull();
        assertThat(summary.getGrossSales()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(summary.getTransactionCount()).isEqualTo(1);
        assertThat(summary.getItemsSold()).isEqualTo(2);

        assertThat(result.getDailyBreakdown()).isNotEmpty();
        assertThat(result.getByCategory()).isNotEmpty();
        assertThat(result.getByCategory().get(0).getCategoryName()).isEqualTo("Electronics");
        assertThat(result.getTopProducts()).isNotEmpty();
        assertThat(result.getTopProducts().get(0).getProductName()).isEqualTo("Test Product");
        assertThat(result.getByPaymentMethod()).isNotEmpty();
        assertThat(result.getByPaymentMethod().get(0).getPaymentMethod()).isEqualTo("CASH");
    }

    @Test
    void generateSalesSummaryReport_noTransactions_returnsEmptySummary() {
        GenerateReportRequest request = GenerateReportRequest.builder()
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .build();

        Tenant tenant = Tenant.builder().timezone("UTC").build();
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
        when(transactionRepository.findByTypeAndDateRangeAndTenantId(
                eq(TransactionType.SALE), any(Instant.class), any(Instant.class), eq(TENANT_ID)))
                .thenReturn(Collections.emptyList());
        when(transactionRepository.findByTypeAndDateRangeAndTenantId(
                eq(TransactionType.RETURN), any(Instant.class), any(Instant.class), eq(TENANT_ID)))
                .thenReturn(Collections.emptyList());

        SalesSummaryReportDTO result = salesReportService.generateSalesSummaryReport(request);

        assertThat(result).isNotNull();
        SalesSummaryReportDTO.Summary summary = result.getSummary();
        assertThat(summary.getGrossSales()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getNetSales()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getCostOfGoods()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getGrossProfit()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getTransactionCount()).isEqualTo(0);
        assertThat(summary.getAverageTransactionValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getItemsSold()).isEqualTo(0);
        assertThat(result.getDailyBreakdown()).hasSize(31);
        assertThat(result.getByCategory()).isEmpty();
        assertThat(result.getTopProducts()).isEmpty();
        assertThat(result.getByPaymentMethod()).isEmpty();
    }

    @Test
    void generateSalesSummaryReport_nullDates_usesDefaults() {
        GenerateReportRequest request = GenerateReportRequest.builder().build();

        Tenant tenant = Tenant.builder().timezone("UTC").build();
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
        when(transactionRepository.findByTypeAndDateRangeAndTenantId(
                eq(TransactionType.SALE), any(Instant.class), any(Instant.class), eq(TENANT_ID)))
                .thenReturn(Collections.emptyList());
        when(transactionRepository.findByTypeAndDateRangeAndTenantId(
                eq(TransactionType.RETURN), any(Instant.class), any(Instant.class), eq(TENANT_ID)))
                .thenReturn(Collections.emptyList());

        SalesSummaryReportDTO result = salesReportService.generateSalesSummaryReport(request);

        assertThat(result).isNotNull();
        assertThat(result.getMetadata().getStartDate()).isEqualTo(LocalDate.now().minusDays(30));
        assertThat(result.getMetadata().getEndDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void generateSalesSummaryReport_tenantWithInvalidTimezone_fallsBackToUTC() {
        GenerateReportRequest request = GenerateReportRequest.builder()
                .startDate(LocalDate.of(2024, 6, 1))
                .endDate(LocalDate.of(2024, 6, 1))
                .build();

        Tenant tenant = Tenant.builder().timezone("Invalid/Zone").build();
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
        when(transactionRepository.findByTypeAndDateRangeAndTenantId(
                eq(TransactionType.SALE), any(Instant.class), any(Instant.class), eq(TENANT_ID)))
                .thenReturn(Collections.emptyList());
        when(transactionRepository.findByTypeAndDateRangeAndTenantId(
                eq(TransactionType.RETURN), any(Instant.class), any(Instant.class), eq(TENANT_ID)))
                .thenReturn(Collections.emptyList());

        SalesSummaryReportDTO result = salesReportService.generateSalesSummaryReport(request);

        assertThat(result).isNotNull();
        assertThat(result.getMetadata().getStartDate()).isEqualTo(LocalDate.of(2024, 6, 1));
    }

    @Test
    void generateSalesSummaryReport_tenantNotFound_usesUTC() {
        GenerateReportRequest request = GenerateReportRequest.builder()
                .startDate(LocalDate.of(2024, 3, 1))
                .endDate(LocalDate.of(2024, 3, 1))
                .build();

        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());
        when(transactionRepository.findByTypeAndDateRangeAndTenantId(
                eq(TransactionType.SALE), any(Instant.class), any(Instant.class), eq(TENANT_ID)))
                .thenReturn(Collections.emptyList());
        when(transactionRepository.findByTypeAndDateRangeAndTenantId(
                eq(TransactionType.RETURN), any(Instant.class), any(Instant.class), eq(TENANT_ID)))
                .thenReturn(Collections.emptyList());

        SalesSummaryReportDTO result = salesReportService.generateSalesSummaryReport(request);

        assertThat(result).isNotNull();
    }

    @Test
    void generateSalesSummaryReport_withLocationFilter_setsLocationInMetadata() {
        GenerateReportRequest request = GenerateReportRequest.builder()
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 1))
                .locationId(5L)
                .build();

        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());
        when(transactionRepository.findByTypeAndDateRangeAndTenantId(
                eq(TransactionType.SALE), any(Instant.class), any(Instant.class), eq(TENANT_ID)))
                .thenReturn(Collections.emptyList());
        when(transactionRepository.findByTypeAndDateRangeAndTenantId(
                eq(TransactionType.RETURN), any(Instant.class), any(Instant.class), eq(TENANT_ID)))
                .thenReturn(Collections.emptyList());

        SalesSummaryReportDTO result = salesReportService.generateSalesSummaryReport(request);

        assertThat(result.getMetadata().getLocationFilter()).isEqualTo("Location ID: 5");
    }

    @Test
    void generateSalesSummaryReport_withDiscounts_calculatesNetSalesCorrectly() {
        LocalDate date = LocalDate.of(2024, 2, 15);
        GenerateReportRequest request = GenerateReportRequest.builder()
                .startDate(date)
                .endDate(date)
                .build();

        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

        Product product = Product.builder().sku("SKU-002").build();
        product.setId(200L);

        POSTransactionLine line = POSTransactionLine.builder()
                .product(product)
                .productName("Discounted Product")
                .quantity(BigDecimal.valueOf(1))
                .unitPrice(BigDecimal.valueOf(200))
                .costPrice(BigDecimal.valueOf(100))
                .discountAmount(BigDecimal.valueOf(20))
                .taxAmount(BigDecimal.ZERO)
                .lineTotal(BigDecimal.valueOf(180))
                .build();

        POSTransaction saleTx = POSTransaction.builder()
                .transactionType(TransactionType.SALE)
                .status(TransactionStatus.COMPLETED)
                .completedAt(date.atStartOfDay(ZoneOffset.UTC).toInstant())
                .discountAmount(BigDecimal.valueOf(10))
                .itemCount(1)
                .lines(new java.util.ArrayList<>(List.of(line)))
                .payments(new java.util.ArrayList<>())
                .build();

        when(transactionRepository.findByTypeAndDateRangeAndTenantId(
                eq(TransactionType.SALE), any(Instant.class), any(Instant.class), eq(TENANT_ID)))
                .thenReturn(List.of(saleTx));
        when(transactionRepository.findByTypeAndDateRangeAndTenantId(
                eq(TransactionType.RETURN), any(Instant.class), any(Instant.class), eq(TENANT_ID)))
                .thenReturn(Collections.emptyList());

        SalesSummaryReportDTO result = salesReportService.generateSalesSummaryReport(request);

        SalesSummaryReportDTO.Summary summary = result.getSummary();
        assertThat(summary.getGrossSales()).isEqualByComparingTo(BigDecimal.valueOf(200));
        assertThat(summary.getDiscounts()).isEqualByComparingTo(BigDecimal.valueOf(30));
        assertThat(summary.getReturns()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getNetSales()).isEqualByComparingTo(BigDecimal.valueOf(170));
    }

    @Test
    void generateSalesSummaryReport_withNullCostPrice_treatsAsZero() {
        LocalDate date = LocalDate.of(2024, 4, 1);
        GenerateReportRequest request = GenerateReportRequest.builder()
                .startDate(date)
                .endDate(date)
                .build();

        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

        Product product = Product.builder().sku("SKU-003").build();
        product.setId(300L);

        POSTransactionLine line = POSTransactionLine.builder()
                .product(product)
                .productName("No Cost Product")
                .quantity(BigDecimal.valueOf(3))
                .unitPrice(BigDecimal.valueOf(10))
                .costPrice(null)
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .lineTotal(BigDecimal.valueOf(30))
                .build();

        POSTransaction saleTx = POSTransaction.builder()
                .transactionType(TransactionType.SALE)
                .status(TransactionStatus.COMPLETED)
                .completedAt(date.atStartOfDay(ZoneOffset.UTC).toInstant())
                .discountAmount(BigDecimal.ZERO)
                .itemCount(3)
                .lines(new java.util.ArrayList<>(List.of(line)))
                .payments(new java.util.ArrayList<>())
                .build();

        when(transactionRepository.findByTypeAndDateRangeAndTenantId(
                eq(TransactionType.SALE), any(Instant.class), any(Instant.class), eq(TENANT_ID)))
                .thenReturn(List.of(saleTx));
        when(transactionRepository.findByTypeAndDateRangeAndTenantId(
                eq(TransactionType.RETURN), any(Instant.class), any(Instant.class), eq(TENANT_ID)))
                .thenReturn(Collections.emptyList());

        SalesSummaryReportDTO result = salesReportService.generateSalesSummaryReport(request);

        assertThat(result.getSummary().getCostOfGoods()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void generateSalesSummaryReport_uncategorizedProduct_usesUncategorizedName() {
        LocalDate date = LocalDate.of(2024, 5, 1);
        GenerateReportRequest request = GenerateReportRequest.builder()
                .startDate(date)
                .endDate(date)
                .build();

        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

        Product product = Product.builder().sku("SKU-004").category(null).build();
        product.setId(400L);

        POSTransactionLine line = POSTransactionLine.builder()
                .product(product)
                .productName("Uncategorized Product")
                .quantity(BigDecimal.ONE)
                .unitPrice(BigDecimal.valueOf(25))
                .costPrice(BigDecimal.valueOf(10))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .lineTotal(BigDecimal.valueOf(25))
                .build();

        POSTransaction saleTx = POSTransaction.builder()
                .transactionType(TransactionType.SALE)
                .status(TransactionStatus.COMPLETED)
                .completedAt(date.atStartOfDay(ZoneOffset.UTC).toInstant())
                .discountAmount(BigDecimal.ZERO)
                .itemCount(1)
                .lines(new java.util.ArrayList<>(List.of(line)))
                .payments(new java.util.ArrayList<>())
                .build();

        when(transactionRepository.findByTypeAndDateRangeAndTenantId(
                eq(TransactionType.SALE), any(Instant.class), any(Instant.class), eq(TENANT_ID)))
                .thenReturn(List.of(saleTx));
        when(transactionRepository.findByTypeAndDateRangeAndTenantId(
                eq(TransactionType.RETURN), any(Instant.class), any(Instant.class), eq(TENANT_ID)))
                .thenReturn(Collections.emptyList());

        SalesSummaryReportDTO result = salesReportService.generateSalesSummaryReport(request);

        assertThat(result.getByCategory()).hasSize(1);
        assertThat(result.getByCategory().get(0).getCategoryName()).isEqualTo("Uncategorized");
        assertThat(result.getByCategory().get(0).getCategoryId()).isNull();
    }
}
