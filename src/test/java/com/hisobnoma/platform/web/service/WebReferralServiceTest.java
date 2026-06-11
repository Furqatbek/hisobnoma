package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.admin.service.TenantSettingService;
import com.hisobnoma.platform.web.entity.WebCustomer;
import com.hisobnoma.platform.web.entity.WebLoyaltyTransaction;
import com.hisobnoma.platform.web.entity.WebLoyaltyTransactionType;
import com.hisobnoma.platform.web.entity.WebOrder;
import com.hisobnoma.platform.web.entity.WebOrderStatus;
import com.hisobnoma.platform.web.repository.WebCustomerRepository;
import com.hisobnoma.platform.web.repository.WebLoyaltyTransactionRepository;
import com.hisobnoma.platform.web.repository.WebOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebReferralServiceTest {

    @Mock private WebCustomerRepository customerRepository;
    @Mock private WebLoyaltyTransactionRepository loyaltyRepository;
    @Mock private WebOrderRepository orderRepository;
    @Mock private TenantSettingService tenantSettingService;

    @InjectMocks
    private WebReferralService service;

    private static final Long TENANT = 1L;

    @BeforeEach
    void setUp() {
        lenient().when(tenantSettingService.getSettingValue(TENANT, "referral.enabled")).thenReturn("true");
        lenient().when(tenantSettingService.getSettingValue(TENANT, "referral.reward_referrer")).thenReturn("10000");
        lenient().when(tenantSettingService.getSettingValue(TENANT, "referral.reward_referred")).thenReturn("5000");
        lenient().when(tenantSettingService.getSettingValue(TENANT, "referral.monthly_cap")).thenReturn("0");
    }

    // ---- getOrCreateCode ----

    @Test
    void getOrCreateCode_generatesCodeForNewCustomer() {
        WebCustomer customer = WebCustomer.builder().phone("998901234567").tenantId(TENANT).build();
        customer.setId(10L);
        when(customerRepository.findByIdAndTenantId(10L, TENANT)).thenReturn(Optional.of(customer));
        when(customerRepository.existsByReferralCode(anyString())).thenReturn(false);
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String code = service.getOrCreateCode(TENANT, 10L);

        assertNotNull(code);
        assertEquals(8, code.length());
        assertEquals(code, customer.getReferralCode());
    }

    @Test
    void getOrCreateCode_returnsExistingCode() {
        WebCustomer customer = WebCustomer.builder().phone("998901234567").tenantId(TENANT).referralCode("ABC12345").build();
        customer.setId(10L);
        when(customerRepository.findByIdAndTenantId(10L, TENANT)).thenReturn(Optional.of(customer));

        String code = service.getOrCreateCode(TENANT, 10L);

        assertEquals("ABC12345", code);
        verify(customerRepository, never()).save(any());
    }

    // ---- applyCode ----

    @Test
    void applyCode_setsReferredBy() {
        WebCustomer referrer = WebCustomer.builder().phone("998901111111").tenantId(TENANT).referralCode("REF123").build();
        referrer.setId(1L);
        WebCustomer newCustomer = WebCustomer.builder().phone("998902222222").tenantId(TENANT).build();
        newCustomer.setId(2L);

        when(customerRepository.findByReferralCode("REF123")).thenReturn(Optional.of(referrer));
        when(customerRepository.findByIdAndTenantId(2L, TENANT)).thenReturn(Optional.of(newCustomer));
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.applyCode(TENANT, 2L, "REF123");

        assertEquals(1L, newCustomer.getReferredBy());
    }

    @Test
    void applyCode_selfReferralRejected() {
        WebCustomer customer = WebCustomer.builder().phone("998901111111").tenantId(TENANT).referralCode("REF123").build();
        customer.setId(1L);

        when(customerRepository.findByReferralCode("REF123")).thenReturn(Optional.of(customer));
        when(customerRepository.findByIdAndTenantId(1L, TENANT)).thenReturn(Optional.of(customer));

        service.applyCode(TENANT, 1L, "REF123");

        assertNull(customer.getReferredBy());
        verify(customerRepository, never()).save(any());
    }

    @Test
    void applyCode_alreadyReferredIsIgnored() {
        WebCustomer referrer = WebCustomer.builder().phone("998901111111").tenantId(TENANT).referralCode("REF123").build();
        referrer.setId(1L);
        WebCustomer newCustomer = WebCustomer.builder().phone("998902222222").tenantId(TENANT).referredBy(99L).build();
        newCustomer.setId(2L);

        when(customerRepository.findByReferralCode("REF123")).thenReturn(Optional.of(referrer));
        when(customerRepository.findByIdAndTenantId(2L, TENANT)).thenReturn(Optional.of(newCustomer));

        service.applyCode(TENANT, 2L, "REF123");

        assertEquals(99L, newCustomer.getReferredBy());
        verify(customerRepository, never()).save(any());
    }

    @Test
    void applyCode_unknownCodeIgnored() {
        when(customerRepository.findByReferralCode("UNKNOWN")).thenReturn(Optional.empty());

        service.applyCode(TENANT, 2L, "UNKNOWN");

        verify(customerRepository, never()).save(any());
    }

    @Test
    void applyCode_disabledIsNoop() {
        when(tenantSettingService.getSettingValue(TENANT, "referral.enabled")).thenReturn("false");

        service.applyCode(TENANT, 2L, "REF123");

        verify(customerRepository, never()).findByReferralCode(any());
    }

    // ---- rewardIfFirstCompletion ----

    @Test
    void rewardIfFirstCompletion_rewardsBothSidesOnFirstOrder() {
        WebCustomer referred = WebCustomer.builder().phone("998902222222").tenantId(TENANT).referredBy(1L).build();
        referred.setId(2L);
        WebCustomer referrer = WebCustomer.builder().phone("998901111111").tenantId(TENANT).build();
        referrer.setId(1L);

        WebOrder order = WebOrder.builder()
                .orderNumber("WO-001").phoneNormalized("998902222222")
                .tenantId(TENANT).totalAmount(new BigDecimal("50000")).build();
        order.setId(100L);

        when(customerRepository.findByTenantIdAndPhone(TENANT, "998902222222"))
                .thenReturn(Optional.of(referred));
        when(orderRepository.countByTenantIdAndPhoneNormalizedAndStatus(TENANT, "998902222222", WebOrderStatus.COMPLETED))
                .thenReturn(1L);
        when(customerRepository.findByIdAndTenantId(1L, TENANT)).thenReturn(Optional.of(referrer));
        when(loyaltyRepository.existsByTenantIdAndWebCustomerIdAndTypeAndNote(
                eq(TENANT), eq(1L), eq(WebLoyaltyTransactionType.ADJUST), anyString()))
                .thenReturn(false);
        when(loyaltyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.rewardIfFirstCompletion(order);

        verify(loyaltyRepository, times(2)).save(argThat(t ->
                t instanceof WebLoyaltyTransaction && ((WebLoyaltyTransaction) t).getType() == WebLoyaltyTransactionType.ADJUST));
    }

    @Test
    void rewardIfFirstCompletion_noRewardOnSecondOrder() {
        WebCustomer referred = WebCustomer.builder().phone("998902222222").tenantId(TENANT).referredBy(1L).build();
        referred.setId(2L);

        WebOrder order = WebOrder.builder()
                .orderNumber("WO-002").phoneNormalized("998902222222")
                .tenantId(TENANT).totalAmount(new BigDecimal("50000")).build();
        order.setId(200L);

        when(customerRepository.findByTenantIdAndPhone(TENANT, "998902222222"))
                .thenReturn(Optional.of(referred));
        when(orderRepository.countByTenantIdAndPhoneNormalizedAndStatus(TENANT, "998902222222", WebOrderStatus.COMPLETED))
                .thenReturn(2L);

        service.rewardIfFirstCompletion(order);

        verify(loyaltyRepository, never()).save(any());
    }

    @Test
    void rewardIfFirstCompletion_idempotentOnDoubleCall() {
        WebCustomer referred = WebCustomer.builder().phone("998902222222").tenantId(TENANT).referredBy(1L).build();
        referred.setId(2L);
        WebCustomer referrer = WebCustomer.builder().phone("998901111111").tenantId(TENANT).build();
        referrer.setId(1L);

        WebOrder order = WebOrder.builder()
                .orderNumber("WO-001").phoneNormalized("998902222222")
                .tenantId(TENANT).totalAmount(new BigDecimal("50000")).build();
        order.setId(100L);

        when(customerRepository.findByTenantIdAndPhone(TENANT, "998902222222"))
                .thenReturn(Optional.of(referred));
        when(orderRepository.countByTenantIdAndPhoneNormalizedAndStatus(TENANT, "998902222222", WebOrderStatus.COMPLETED))
                .thenReturn(1L);
        when(customerRepository.findByIdAndTenantId(1L, TENANT)).thenReturn(Optional.of(referrer));
        when(loyaltyRepository.existsByTenantIdAndWebCustomerIdAndTypeAndNote(
                eq(TENANT), eq(1L), eq(WebLoyaltyTransactionType.ADJUST), anyString()))
                .thenReturn(true);

        service.rewardIfFirstCompletion(order);

        verify(loyaltyRepository, never()).save(any());
    }

    @Test
    void rewardIfFirstCompletion_noReferredBySkips() {
        WebCustomer customer = WebCustomer.builder().phone("998902222222").tenantId(TENANT).build();
        customer.setId(2L);

        WebOrder order = WebOrder.builder()
                .orderNumber("WO-001").phoneNormalized("998902222222")
                .tenantId(TENANT).totalAmount(new BigDecimal("50000")).build();

        when(customerRepository.findByTenantIdAndPhone(TENANT, "998902222222"))
                .thenReturn(Optional.of(customer));

        service.rewardIfFirstCompletion(order);

        verify(loyaltyRepository, never()).save(any());
    }

    @Test
    void rewardIfFirstCompletion_disabledIsNoop() {
        when(tenantSettingService.getSettingValue(TENANT, "referral.enabled")).thenReturn("false");

        WebOrder order = WebOrder.builder()
                .orderNumber("WO-001").phoneNormalized("998902222222")
                .tenantId(TENANT).totalAmount(new BigDecimal("50000")).build();

        service.rewardIfFirstCompletion(order);

        verify(loyaltyRepository, never()).save(any());
    }

    @Test
    void rewardIfFirstCompletion_monthlyCapEnforced() {
        WebCustomer referred = WebCustomer.builder().phone("998902222222").tenantId(TENANT).referredBy(1L).build();
        referred.setId(2L);
        WebCustomer referrer = WebCustomer.builder().phone("998901111111").tenantId(TENANT).build();
        referrer.setId(1L);

        WebOrder order = WebOrder.builder()
                .orderNumber("WO-001").phoneNormalized("998902222222")
                .tenantId(TENANT).totalAmount(new BigDecimal("50000")).build();
        order.setId(100L);

        when(tenantSettingService.getSettingValue(TENANT, "referral.monthly_cap")).thenReturn("3");
        when(customerRepository.findByTenantIdAndPhone(TENANT, "998902222222"))
                .thenReturn(Optional.of(referred));
        when(orderRepository.countByTenantIdAndPhoneNormalizedAndStatus(TENANT, "998902222222", WebOrderStatus.COMPLETED))
                .thenReturn(1L);
        when(customerRepository.findByIdAndTenantId(1L, TENANT)).thenReturn(Optional.of(referrer));
        when(loyaltyRepository.existsByTenantIdAndWebCustomerIdAndTypeAndNote(
                eq(TENANT), eq(1L), eq(WebLoyaltyTransactionType.ADJUST), anyString()))
                .thenReturn(false);
        when(loyaltyRepository.countByTenantIdAndWebCustomerIdAndTypeAndNoteStartingWithAndCreatedAtBetween(
                eq(TENANT), eq(1L), eq(WebLoyaltyTransactionType.ADJUST), anyString(), any(), any()))
                .thenReturn(3L);

        service.rewardIfFirstCompletion(order);

        verify(loyaltyRepository, never()).save(any());
    }
}
