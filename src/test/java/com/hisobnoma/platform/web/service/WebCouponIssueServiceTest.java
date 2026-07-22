package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.ValidationException;
import com.hisobnoma.platform.pos.dto.CouponDto;
import com.hisobnoma.platform.pos.dto.CreateCouponRequest;
import com.hisobnoma.platform.pos.entity.Coupon;
import com.hisobnoma.platform.pos.repository.CouponRepository;
import com.hisobnoma.platform.pos.service.CouponService;
import com.hisobnoma.platform.sms.service.SmsService;
import com.hisobnoma.platform.web.dto.IssueCouponRequest;
import com.hisobnoma.platform.web.entity.WebCustomer;
import com.hisobnoma.platform.web.entity.WebSegmentType;
import com.hisobnoma.platform.web.repository.WebCustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebCouponIssueServiceTest {

    @Mock private WebCustomerRepository webCustomerRepository;
    @Mock private CouponService couponService;
    @Mock private CouponRepository couponRepository;
    @Mock private SmsService smsService;
    @Mock private SecurityContextHelper securityContextHelper;
    @InjectMocks private WebCouponIssueService service;

    private static final Long TENANT = 1L;

    private WebCustomer customer(Long id, String phone, Long arId) {
        WebCustomer c = WebCustomer.builder().tenantId(TENANT).phone(phone).customerId(arId).build();
        c.setId(id);
        return c;
    }

    private void stubMint(long couponId, String code) {
        when(couponService.generateCoupons(eq(5L), eq(1), any(CreateCouponRequest.class)))
                .thenReturn(List.of(CouponDto.builder().id(couponId).code(code).build()));
        when(couponRepository.findByIdAndTenantId(eq(couponId), eq(TENANT))).thenReturn(Optional.of(new Coupon()));
        when(couponRepository.save(any(Coupon.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void issueToCustomer_mintsSingleUseBoundCouponAndSendsSms() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT);
        when(webCustomerRepository.findByIdAndTenantId(10L, TENANT))
                .thenReturn(Optional.of(customer(10L, "998901234567", 77L)));
        stubMint(500L, "ABC123");

        CouponDto dto = service.issueToCustomer(10L, IssueCouponRequest.builder()
                .promotionId(5L).validityDays(14).sendSms(true).build());

        assertEquals("ABC123", dto.getCode());
        // Mint request: single-use, AR-bound when linked
        ArgumentCaptor<CreateCouponRequest> create = ArgumentCaptor.forClass(CreateCouponRequest.class);
        verify(couponService).generateCoupons(eq(5L), eq(1), create.capture());
        assertEquals(1, create.getValue().getMaxUses());
        assertEquals(77L, create.getValue().getCustomerId());
        // Web binding persisted on the minted coupon
        ArgumentCaptor<Coupon> saved = ArgumentCaptor.forClass(Coupon.class);
        verify(couponRepository).save(saved.capture());
        assertEquals(10L, saved.getValue().getWebCustomerId());
        // SMS with the code
        verify(smsService).sendSmsAsync(eq("+998901234567"), contains("ABC123"));
    }

    @Test
    void issueToSegment_issuesOnePerCustomer_noSmsWhenDisabled() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT);
        when(webCustomerRepository.segmentNoOrderSince(eq(TENANT), any()))
                .thenReturn(List.of(customer(10L, "998901111111", null),
                                    customer(11L, "998902222222", null)));
        stubMint(500L, "CODE");

        int issued = service.issueToSegment(WebSegmentType.NO_ORDER_IN_N_DAYS, 60,
                IssueCouponRequest.builder().promotionId(5L).sendSms(false).build());

        assertEquals(2, issued);
        verify(couponService, times(2)).generateCoupons(eq(5L), eq(1), any());
        verify(smsService, never()).sendSmsAsync(any(), any());
    }

    @Test
    void issueToSegment_daysSegmentRequiresParam() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT);
        assertThrows(ValidationException.class, () ->
                service.issueToSegment(WebSegmentType.NO_ORDER_IN_N_DAYS, null,
                        IssueCouponRequest.builder().promotionId(5L).build()));
    }
}
