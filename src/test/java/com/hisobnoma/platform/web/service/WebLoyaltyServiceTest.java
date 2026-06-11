package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.admin.service.TenantSettingService;
import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.web.dto.LoyaltyAdjustRequest;
import com.hisobnoma.platform.web.dto.LoyaltyBalanceDto;
import com.hisobnoma.platform.web.entity.*;
import com.hisobnoma.platform.web.repository.WebCustomerRepository;
import com.hisobnoma.platform.web.repository.WebLoyaltyTransactionRepository;
import com.hisobnoma.platform.web.repository.WebOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebLoyaltyServiceTest {

    @Mock private WebLoyaltyTransactionRepository loyaltyRepository;
    @Mock private WebCustomerRepository webCustomerRepository;
    @Mock private WebOrderRepository orderRepository;
    @Mock private TenantSettingService tenantSettingService;
    @Mock private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private WebLoyaltyService service;

    private static final Long TENANT_ID = 1L;
    private static final Long CUSTOMER_ID = 10L;
    private static final Long ORDER_ID = 100L;

    @BeforeEach
    void setUp() {
        lenient().when(tenantSettingService.getSettingValue(TENANT_ID, WebLoyaltyService.SETTING_ENABLED))
                .thenReturn("true");
        lenient().when(tenantSettingService.getSettingValue(TENANT_ID, WebLoyaltyService.SETTING_EARN_PERCENT))
                .thenReturn("5");
        lenient().when(tenantSettingService.getSettingValue(TENANT_ID, WebLoyaltyService.SETTING_EXPIRY_DAYS))
                .thenReturn("180");
        lenient().when(tenantSettingService.getSettingValue(TENANT_ID, WebLoyaltyService.SETTING_MIN_REDEEM))
                .thenReturn("5000");
        lenient().when(tenantSettingService.getSettingValue(TENANT_ID, WebLoyaltyService.SETTING_MAX_REDEEM_PERCENT))
                .thenReturn("50");
    }

    // ---- getBalance ----

    @Test
    void getBalance_enabled_returnsBalance() {
        when(loyaltyRepository.balanceByCustomer(eq(TENANT_ID), eq(CUSTOMER_ID), any()))
                .thenReturn(new BigDecimal("15000"));
        when(loyaltyRepository.findByCustomer(eq(TENANT_ID), eq(CUSTOMER_ID), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        LoyaltyBalanceDto result = service.getBalance(TENANT_ID, CUSTOMER_ID);

        assertTrue(result.isEnabled());
        assertEquals(new BigDecimal("15000"), result.getBalance());
        assertEquals(new BigDecimal("5000"), result.getMinRedeem());
        assertEquals(50, result.getMaxRedeemPercent());
    }

    @Test
    void getBalance_disabled_returnsZero() {
        when(tenantSettingService.getSettingValue(TENANT_ID, WebLoyaltyService.SETTING_ENABLED))
                .thenReturn("false");
        when(loyaltyRepository.findByCustomer(eq(TENANT_ID), eq(CUSTOMER_ID), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        LoyaltyBalanceDto result = service.getBalance(TENANT_ID, CUSTOMER_ID);

        assertFalse(result.isEnabled());
        assertEquals(BigDecimal.ZERO, result.getBalance());
    }

    // ---- spend ----

    @Test
    void spend_clampsToBalanceAndPercent() {
        when(loyaltyRepository.balanceByCustomer(eq(TENANT_ID), eq(CUSTOMER_ID), any()))
                .thenReturn(new BigDecimal("30000"));
        when(loyaltyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BigDecimal spent = service.spend(TENANT_ID, CUSTOMER_ID, ORDER_ID,
                new BigDecimal("30000"), new BigDecimal("40000"));

        assertEquals(new BigDecimal("20000.0000"), spent);

        ArgumentCaptor<WebLoyaltyTransaction> captor = ArgumentCaptor.forClass(WebLoyaltyTransaction.class);
        verify(loyaltyRepository).save(captor.capture());
        WebLoyaltyTransaction tx = captor.getValue();
        assertEquals(WebLoyaltyTransactionType.SPEND, tx.getType());
        assertTrue(tx.getAmount().compareTo(BigDecimal.ZERO) < 0);
    }

    @Test
    void spend_belowMinRedeem_returnsZero() {
        when(loyaltyRepository.balanceByCustomer(eq(TENANT_ID), eq(CUSTOMER_ID), any()))
                .thenReturn(new BigDecimal("3000"));

        BigDecimal spent = service.spend(TENANT_ID, CUSTOMER_ID, ORDER_ID,
                new BigDecimal("3000"), new BigDecimal("50000"));

        assertEquals(BigDecimal.ZERO, spent);
        verify(loyaltyRepository, never()).save(any());
    }

    @Test
    void spend_disabled_returnsZero() {
        when(tenantSettingService.getSettingValue(TENANT_ID, WebLoyaltyService.SETTING_ENABLED))
                .thenReturn("false");

        BigDecimal spent = service.spend(TENANT_ID, CUSTOMER_ID, ORDER_ID,
                new BigDecimal("10000"), new BigDecimal("50000"));

        assertEquals(BigDecimal.ZERO, spent);
        verify(loyaltyRepository, never()).save(any());
    }

    @Test
    void spend_zeroRequested_returnsZero() {
        BigDecimal spent = service.spend(TENANT_ID, CUSTOMER_ID, ORDER_ID,
                BigDecimal.ZERO, new BigDecimal("50000"));

        assertEquals(BigDecimal.ZERO, spent);
        verify(loyaltyRepository, never()).save(any());
    }

    // ---- earn ----

    @Test
    void earn_completedOrder_createsEarnTransaction() {
        WebOrder order = buildOrder(new BigDecimal("100000"), new BigDecimal("5000"), BigDecimal.ZERO);
        WebCustomer customer = WebCustomer.builder().id(CUSTOMER_ID).phone("998901234567").build();
        when(webCustomerRepository.findByTenantIdAndPhone(TENANT_ID, "998901234567"))
                .thenReturn(Optional.of(customer));
        when(loyaltyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.earn(order);

        ArgumentCaptor<WebLoyaltyTransaction> captor = ArgumentCaptor.forClass(WebLoyaltyTransaction.class);
        verify(loyaltyRepository).save(captor.capture());
        WebLoyaltyTransaction tx = captor.getValue();
        assertEquals(WebLoyaltyTransactionType.EARN, tx.getType());
        assertEquals(new BigDecimal("4750.0000"), tx.getAmount());
        assertNotNull(tx.getExpiresAt());
    }

    @Test
    void earn_disabled_skips() {
        when(tenantSettingService.getSettingValue(TENANT_ID, WebLoyaltyService.SETTING_ENABLED))
                .thenReturn("false");

        service.earn(buildOrder(new BigDecimal("100000"), BigDecimal.ZERO, BigDecimal.ZERO));

        verify(loyaltyRepository, never()).save(any());
    }

    @Test
    void earn_idempotent_duplicateIgnored() {
        WebOrder order = buildOrder(new BigDecimal("100000"), BigDecimal.ZERO, BigDecimal.ZERO);
        WebCustomer customer = WebCustomer.builder().id(CUSTOMER_ID).phone("998901234567").build();
        when(webCustomerRepository.findByTenantIdAndPhone(TENANT_ID, "998901234567"))
                .thenReturn(Optional.of(customer));
        when(loyaltyRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertDoesNotThrow(() -> service.earn(order));
    }

    @Test
    void earn_noWebCustomer_skips() {
        WebOrder order = buildOrder(new BigDecimal("100000"), BigDecimal.ZERO, BigDecimal.ZERO);
        when(webCustomerRepository.findByTenantIdAndPhone(TENANT_ID, "998901234567"))
                .thenReturn(Optional.empty());

        service.earn(order);

        verify(loyaltyRepository, never()).save(any());
    }

    @Test
    void earn_excludesDeliveryFee() {
        WebOrder order = buildOrder(new BigDecimal("100000"), new BigDecimal("10000"), BigDecimal.ZERO);
        WebCustomer customer = WebCustomer.builder().id(CUSTOMER_ID).phone("998901234567").build();
        when(webCustomerRepository.findByTenantIdAndPhone(TENANT_ID, "998901234567"))
                .thenReturn(Optional.of(customer));
        when(loyaltyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.earn(order);

        ArgumentCaptor<WebLoyaltyTransaction> captor = ArgumentCaptor.forClass(WebLoyaltyTransaction.class);
        verify(loyaltyRepository).save(captor.capture());
        assertEquals(new BigDecimal("4500.0000"), captor.getValue().getAmount());
    }

    // ---- reverseOrder ----

    @Test
    void reverseOrder_reversesBothEarnAndSpend() {
        WebLoyaltyTransaction earn = WebLoyaltyTransaction.builder()
                .tenantId(TENANT_ID).webCustomerId(CUSTOMER_ID).type(WebLoyaltyTransactionType.EARN)
                .amount(new BigDecimal("5000")).build();
        WebLoyaltyTransaction spend = WebLoyaltyTransaction.builder()
                .tenantId(TENANT_ID).webCustomerId(CUSTOMER_ID).type(WebLoyaltyTransactionType.SPEND)
                .amount(new BigDecimal("-10000")).build();
        when(loyaltyRepository.findByTenantIdAndWebOrderId(TENANT_ID, ORDER_ID))
                .thenReturn(List.of(earn, spend));
        when(loyaltyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WebOrder order = buildOrder(new BigDecimal("100000"), BigDecimal.ZERO, new BigDecimal("10000"));
        service.reverseOrder(order);

        ArgumentCaptor<WebLoyaltyTransaction> captor = ArgumentCaptor.forClass(WebLoyaltyTransaction.class);
        verify(loyaltyRepository, times(2)).save(captor.capture());
        List<WebLoyaltyTransaction> adjusts = captor.getAllValues().stream()
                .filter(t -> t.getType() == WebLoyaltyTransactionType.ADJUST)
                .toList();
        assertEquals(2, adjusts.size());
    }

    // ---- adjust ----

    @Test
    void adjust_createsAdjustTransaction() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(99L);
        WebCustomer customer = WebCustomer.builder().id(CUSTOMER_ID).phone("998901234567").tenantId(TENANT_ID).build();
        when(webCustomerRepository.findByIdAndTenantId(CUSTOMER_ID, TENANT_ID))
                .thenReturn(Optional.of(customer));
        when(loyaltyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(loyaltyRepository.balanceByCustomer(eq(TENANT_ID), eq(CUSTOMER_ID), any()))
                .thenReturn(new BigDecimal("5000"));
        when(loyaltyRepository.findByCustomer(eq(TENANT_ID), eq(CUSTOMER_ID), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        LoyaltyBalanceDto result = service.adjust(CUSTOMER_ID,
                LoyaltyAdjustRequest.builder().amount(new BigDecimal("5000")).reason("Компенсация").build());

        assertEquals(new BigDecimal("5000"), result.getBalance());
        ArgumentCaptor<WebLoyaltyTransaction> captor = ArgumentCaptor.forClass(WebLoyaltyTransaction.class);
        verify(loyaltyRepository, atLeastOnce()).save(captor.capture());
        WebLoyaltyTransaction tx = captor.getAllValues().stream()
                .filter(t -> t.getType() == WebLoyaltyTransactionType.ADJUST)
                .findFirst().orElseThrow();
        assertEquals(new BigDecimal("5000"), tx.getAmount());
        assertEquals("Компенсация", tx.getNote());
        assertEquals(99L, tx.getCreatedBy());
    }

    // ---- helpers ----

    private WebOrder buildOrder(BigDecimal total, BigDecimal deliveryFee, BigDecimal pointsSpent) {
        return WebOrder.builder()
                .id(ORDER_ID)
                .tenantId(TENANT_ID)
                .orderNumber("WO-000001")
                .phoneNormalized("998901234567")
                .totalAmount(total)
                .deliveryFee(deliveryFee)
                .pointsSpent(pointsSpent)
                .build();
    }
}
