package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.web.entity.WebCustomer;
import com.hisobnoma.platform.web.entity.WebOrder;
import com.hisobnoma.platform.web.entity.WebOrderStatus;
import com.hisobnoma.platform.web.repository.WebCustomerRepository;
import com.hisobnoma.platform.web.repository.WebDeviceTokenRepository;
import com.hisobnoma.platform.web.repository.WebLoyaltyTransactionRepository;
import com.hisobnoma.platform.web.repository.WebNotificationRepository;
import com.hisobnoma.platform.web.repository.WebOrderRepository;
import com.hisobnoma.platform.web.repository.WebOtpCodeRepository;
import com.hisobnoma.platform.web.repository.WebWishlistItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebAccountDeletionServiceTest {

    @Mock private WebAuthService authService;
    @Mock private WebCustomerRepository customerRepository;
    @Mock private WebOrderRepository orderRepository;
    @Mock private WebDeviceTokenRepository deviceTokenRepository;
    @Mock private WebWishlistItemRepository wishlistRepository;
    @Mock private WebNotificationRepository notificationRepository;
    @Mock private WebLoyaltyTransactionRepository loyaltyRepository;
    @Mock private WebOtpCodeRepository otpRepository;

    @InjectMocks
    private WebAccountDeletionService service;

    private static final Long TENANT = 1L;
    private static final Long CUSTOMER_ID = 10L;
    private static final String PHONE = "998901234567";
    private static final String TOKEN = "Bearer t";

    private WebCustomer customer() {
        WebCustomer c = WebCustomer.builder().tenantId(TENANT).phone(PHONE).name("Ali").build();
        c.setId(CUSTOMER_ID);
        return c;
    }

    @Test
    void deleteOwnAccount_purgesPersonalDataAndAnonymisesOrders() {
        when(authService.requireCustomer(TOKEN)).thenReturn(customer());
        when(orderRepository.countByTenantIdAndPhoneNormalizedAndStatusIn(eq(TENANT), eq(PHONE), any()))
                .thenReturn(0L);
        WebOrder completed = WebOrder.builder()
                .orderNumber("WO-1").status(WebOrderStatus.COMPLETED)
                .customerName("Ali").phone(PHONE).phoneNormalized(PHONE)
                .deliveryAddress("Chilonzor 5").customerId(99L).build();
        when(orderRepository.findByTenantIdAndPhoneNormalized(TENANT, PHONE))
                .thenReturn(List.of(completed));

        service.deleteOwnAccount(TOKEN);

        // Order kept but stripped + detached.
        assertEquals("Deleted user", completed.getCustomerName());
        assertNull(completed.getPhoneNormalized());
        assertNull(completed.getDeliveryAddress());
        assertNull(completed.getCustomerId());
        verify(orderRepository).saveAll(anyList());

        // Personal data removed, customer row deleted (invalidates tokens).
        verify(deviceTokenRepository).deleteAllByTenantIdAndWebCustomerId(TENANT, CUSTOMER_ID);
        verify(wishlistRepository).deleteAllByTenantIdAndWebCustomerId(TENANT, CUSTOMER_ID);
        verify(notificationRepository).deleteAllByTenantIdAndWebCustomerId(TENANT, CUSTOMER_ID);
        verify(loyaltyRepository).deleteAllByTenantIdAndWebCustomerId(TENANT, CUSTOMER_ID);
        verify(otpRepository).deleteAllByTenantIdAndPhone(TENANT, PHONE);
        verify(customerRepository).delete(any(WebCustomer.class));
    }

    @Test
    void deleteOwnAccount_withActiveOrder_blocksWith409() {
        when(authService.requireCustomer(TOKEN)).thenReturn(customer());
        when(orderRepository.countByTenantIdAndPhoneNormalizedAndStatusIn(eq(TENANT), eq(PHONE), any()))
                .thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.deleteOwnAccount(TOKEN));
        assertEquals("ACCOUNT_HAS_ACTIVE_ORDERS", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.CONFLICT, ex.getStatus());

        // Nothing is deleted when blocked.
        verify(customerRepository, never()).delete(any());
        verify(loyaltyRepository, never()).deleteAllByTenantIdAndWebCustomerId(any(), any());
    }

    @Test
    void deleteOwnAccount_invalidToken_propagates() {
        when(authService.requireCustomer(TOKEN))
                .thenThrow(new com.hisobnoma.platform.common.exception.UnauthorizedException("bad"));

        assertThrows(com.hisobnoma.platform.common.exception.UnauthorizedException.class,
                () -> service.deleteOwnAccount(TOKEN));
        verifyNoInteractions(customerRepository, loyaltyRepository);
    }
}
