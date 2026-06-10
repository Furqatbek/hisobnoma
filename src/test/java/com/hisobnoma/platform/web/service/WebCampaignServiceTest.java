package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.ValidationException;
import com.hisobnoma.platform.pos.dto.CouponDto;
import com.hisobnoma.platform.pos.entity.Promotion;
import com.hisobnoma.platform.pos.enums.PromotionChannel;
import com.hisobnoma.platform.pos.repository.PromotionRepository;
import com.hisobnoma.platform.pos.service.CouponService;
import com.hisobnoma.platform.sms.dto.SmsBulkSendRequest;
import com.hisobnoma.platform.sms.entity.SmsTemplate;
import com.hisobnoma.platform.sms.repository.SmsTemplateRepository;
import com.hisobnoma.platform.sms.service.SmsService;
import com.hisobnoma.platform.web.dto.CampaignPreviewDto;
import com.hisobnoma.platform.web.dto.WebCampaignDto;
import com.hisobnoma.platform.web.entity.WebCampaign;
import com.hisobnoma.platform.web.entity.WebCampaignStatus;
import com.hisobnoma.platform.web.entity.WebCustomer;
import com.hisobnoma.platform.web.entity.WebSegmentType;
import com.hisobnoma.platform.web.repository.WebCampaignRepository;
import com.hisobnoma.platform.web.repository.WebCustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebCampaignServiceTest {

    @Mock private WebCampaignRepository campaignRepository;
    @Mock private WebCustomerRepository webCustomerRepository;
    @Mock private PromotionRepository promotionRepository;
    @Mock private SmsService smsService;
    @Mock private SmsTemplateRepository templateRepository;
    @Mock private CouponService couponService;
    @Mock private WebCampaignDispatcher dispatcher;
    @Mock private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private WebCampaignService service;

    private static final Long TENANT = 1L;

    private WebCampaign campaign;

    @BeforeEach
    void setUp() {
        lenient().when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT);
        lenient().when(smsService.smsCost()).thenReturn(200.0);

        campaign = WebCampaign.builder()
                .name("Promo blast").segmentType(WebSegmentType.ALL_CUSTOMERS)
                .smsTemplateId(5L).status(WebCampaignStatus.DRAFT).tenantId(TENANT)
                .build();
        campaign.setId(10L);
        lenient().when(campaignRepository.findByIdAndTenantId(10L, TENANT)).thenReturn(Optional.of(campaign));
    }

    private SmsTemplate template(String text) {
        SmsTemplate t = SmsTemplate.builder().code("C").name("Tmpl").template(text).active(true).tenantId(TENANT).build();
        t.setId(5L);
        return t;
    }

    private List<WebCustomer> customers(int n) {
        return java.util.stream.IntStream.range(0, n)
                .mapToObj(i -> WebCustomer.builder().phone("99890000000" + i).name("C" + i).tenantId(TENANT).build())
                .map(c -> (WebCustomer) c).toList();
    }

    // ---- preview ----

    @Test
    void preview_computesCountCostAndBalanceSufficiency() {
        when(webCustomerRepository.segmentAll(TENANT)).thenReturn(customers(3));
        when(smsService.getBalanceAmount()).thenReturn(1000.0);

        CampaignPreviewDto preview = service.preview(10L);

        assertEquals(3, preview.getRecipientCount());
        assertEquals(0, new BigDecimal("600").compareTo(preview.getEstimatedCost())); // 3 x 200
        assertEquals(0, new BigDecimal("1000.0").compareTo(preview.getSmsBalance()));
        assertTrue(preview.isSufficientBalance());
    }

    @Test
    void preview_flagsInsufficientBalance() {
        when(webCustomerRepository.segmentAll(TENANT)).thenReturn(customers(10));
        when(smsService.getBalanceAmount()).thenReturn(500.0); // need 2000

        CampaignPreviewDto preview = service.preview(10L);

        assertFalse(preview.isSufficientBalance());
    }

    @Test
    void preview_unknownBalanceIsTreatedAsSufficient() {
        when(webCustomerRepository.segmentAll(TENANT)).thenReturn(customers(3));
        when(smsService.getBalanceAmount()).thenReturn(-1.0);

        CampaignPreviewDto preview = service.preview(10L);

        assertNull(preview.getSmsBalance());
        assertTrue(preview.isSufficientBalance());
    }

    // ---- send ----

    @Test
    void send_dispatchesToSegmentAndTransitionsToSending() {
        when(webCustomerRepository.segmentAll(TENANT)).thenReturn(customers(2));
        when(templateRepository.findByTenantIdAndId(TENANT, 5L)).thenReturn(Optional.of(template("Салом {name}")));
        when(smsService.getBalanceAmount()).thenReturn(10000.0);
        when(campaignRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WebCampaignDto dto = service.send(10L);

        assertEquals(WebCampaignStatus.SENDING, dto.getStatus());
        assertEquals(2, dto.getRecipientCount());
        assertEquals(0, new BigDecimal("400").compareTo(dto.getCostEstimate()));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SmsBulkSendRequest.Recipient>> captor = ArgumentCaptor.forClass(List.class);
        verify(dispatcher).dispatch(eq(10L), eq(TENANT), eq(5L), captor.capture(), isNull());
        assertEquals(2, captor.getValue().size());
        assertEquals("C0", captor.getValue().get(0).getVariables().get("name"));
        verifyNoInteractions(couponService); // no promotion attached
    }

    @Test
    void send_generatesOneCouponPerRecipientAndInjectsVariable() {
        campaign.setPromotionId(7L);
        when(webCustomerRepository.segmentAll(TENANT)).thenReturn(customers(2));
        when(templateRepository.findByTenantIdAndId(TENANT, 5L))
                .thenReturn(Optional.of(template("Сизга {coupon} купони")));
        when(smsService.getBalanceAmount()).thenReturn(10000.0);
        when(couponService.generateCoupons(eq(7L), eq(2), any())).thenReturn(List.of(
                CouponDto.builder().code("AAA").build(),
                CouponDto.builder().code("BBB").build()));
        when(campaignRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.send(10L);

        verify(couponService).generateCoupons(eq(7L), eq(2), any());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SmsBulkSendRequest.Recipient>> captor = ArgumentCaptor.forClass(List.class);
        verify(dispatcher).dispatch(eq(10L), eq(TENANT), eq(5L), captor.capture(), isNull());
        assertEquals("AAA", captor.getValue().get(0).getVariables().get("coupon"));
        assertEquals("BBB", captor.getValue().get(1).getVariables().get("coupon"));
    }

    @Test
    void send_couponTemplateWithoutPromotionIsRejected() {
        when(webCustomerRepository.segmentAll(TENANT)).thenReturn(customers(2));
        when(templateRepository.findByTenantIdAndId(TENANT, 5L))
                .thenReturn(Optional.of(template("Сизга {coupon} купони")));

        assertThrows(ValidationException.class, () -> service.send(10L));
        verifyNoInteractions(dispatcher);
    }

    @Test
    void send_insufficientBalanceBlocksBeforeDispatch() {
        when(webCustomerRepository.segmentAll(TENANT)).thenReturn(customers(10));
        when(templateRepository.findByTenantIdAndId(TENANT, 5L)).thenReturn(Optional.of(template("Салом")));
        when(smsService.getBalanceAmount()).thenReturn(500.0); // need 2000

        assertThrows(BusinessException.class, () -> service.send(10L));
        verifyNoInteractions(dispatcher);
        verifyNoInteractions(couponService);
    }

    @Test
    void send_emptySegmentIsRejected() {
        // The segment is resolved before the template is fetched
        when(webCustomerRepository.segmentAll(TENANT)).thenReturn(List.of());

        assertThrows(BusinessException.class, () -> service.send(10L));
        verifyNoInteractions(dispatcher);
    }

    @Test
    void send_nonDraftCampaignIsRejected() {
        campaign.setStatus(WebCampaignStatus.SENT);

        assertThrows(BusinessException.class, () -> service.send(10L));
        verifyNoInteractions(dispatcher);
    }

    // ---- segment resolution dispatch ----

    @Test
    void resolveSegment_routesEachTypeToItsQuery() {
        campaign.setSegmentType(WebSegmentType.NEVER_ORDERED);
        when(webCustomerRepository.segmentNeverOrdered(TENANT)).thenReturn(customers(1));
        assertEquals(1, service.resolveSegment(campaign).size());

        campaign.setSegmentType(WebSegmentType.MIN_TOTAL_SPENT);
        campaign.setSegmentParam(new BigDecimal("50000"));
        when(webCustomerRepository.segmentMinSpent(TENANT, new BigDecimal("50000"))).thenReturn(customers(2));
        assertEquals(2, service.resolveSegment(campaign).size());

        campaign.setSegmentType(WebSegmentType.ORDERED_LAST_N_DAYS);
        campaign.setSegmentParam(new BigDecimal("30"));
        when(webCustomerRepository.segmentOrderedSince(eq(TENANT), any())).thenReturn(customers(3));
        assertEquals(3, service.resolveSegment(campaign).size());
    }

    // ---- create validation ----

    @Test
    void create_paramRequiredSegmentRejectsMissingParam() {
        // Segment is validated before the template, so no template stub is needed
        var request = com.hisobnoma.platform.web.dto.CreateCampaignRequest.builder()
                .name("X").segmentType(WebSegmentType.NO_ORDER_IN_N_DAYS).smsTemplateId(5L).build();

        assertThrows(ValidationException.class, () -> service.create(request));
    }

    @Test
    void create_posChannelPromotionRejected() {
        when(templateRepository.findByTenantIdAndId(TENANT, 5L)).thenReturn(Optional.of(template("Салом")));
        Promotion pos = Promotion.builder().code("P").channel(PromotionChannel.POS).tenantId(TENANT).build();
        when(promotionRepository.findByIdAndTenantId(7L, TENANT)).thenReturn(Optional.of(pos));
        var request = com.hisobnoma.platform.web.dto.CreateCampaignRequest.builder()
                .name("X").segmentType(WebSegmentType.ALL_CUSTOMERS).smsTemplateId(5L).promotionId(7L).build();

        assertThrows(ValidationException.class, () -> service.create(request));
    }

    @Test
    void create_webChannelPromotionAccepted() {
        when(templateRepository.findByTenantIdAndId(TENANT, 5L)).thenReturn(Optional.of(template("Салом")));
        Promotion web = Promotion.builder().code("P").channel(PromotionChannel.WEB).tenantId(TENANT).build();
        when(promotionRepository.findByIdAndTenantId(7L, TENANT)).thenReturn(Optional.of(web));
        when(campaignRepository.save(any())).thenAnswer(inv -> {
            WebCampaign c = inv.getArgument(0);
            c.setId(99L);
            return c;
        });
        var request = com.hisobnoma.platform.web.dto.CreateCampaignRequest.builder()
                .name("X").segmentType(WebSegmentType.ALL_CUSTOMERS).smsTemplateId(5L).promotionId(7L).build();

        WebCampaignDto dto = service.create(request);
        assertEquals(WebCampaignStatus.DRAFT, dto.getStatus());
    }
}
