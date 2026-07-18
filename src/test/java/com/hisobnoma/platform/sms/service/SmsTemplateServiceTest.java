package com.hisobnoma.platform.sms.service;

import com.hisobnoma.platform.common.tenant.TenantContext;
import com.hisobnoma.platform.sms.dto.SmsTemplateDto;
import com.hisobnoma.platform.sms.dto.SmsTemplateRequest;
import com.hisobnoma.platform.sms.entity.SmsTemplate;
import com.hisobnoma.platform.sms.repository.SmsTemplateRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmsTemplateServiceTest {

    @Mock
    private SmsTemplateRepository templateRepository;

    @InjectMocks
    private SmsTemplateService smsTemplateService;

    private SmsTemplate template;
    private SmsTemplateRequest templateRequest;
    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenant(TENANT_ID);

        template = SmsTemplate.builder()
                .id(1L)
                .tenantId(TENANT_ID)
                .code("ORDER_CONFIRM")
                .name("Order Confirmation")
                .template("Hurmatli {name}, buyurtmangiz #{orderId} qabul qilindi.")
                .active(true)
                .build();

        templateRequest = new SmsTemplateRequest();
        templateRequest.setCode("ORDER_CONFIRM");
        templateRequest.setName("Order Confirmation");
        templateRequest.setTemplate("Hurmatli {name}, buyurtmangiz #{orderId} qabul qilindi.");
        templateRequest.setActive(true);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ====== getAllTemplates ======

    @Test
    void getAllTemplates_returnsAllTemplates() {
        // Given
        when(templateRepository.findByTenantIdOrderByNameAsc(TENANT_ID)).thenReturn(List.of(template));

        // When
        List<SmsTemplateDto> result = smsTemplateService.getAllTemplates();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ORDER_CONFIRM", result.get(0).getCode());
    }

    // ====== getActiveTemplates ======

    @Test
    void getActiveTemplates_returnsOnlyActiveTemplates() {
        // Given
        when(templateRepository.findByTenantIdAndActiveTrueOrderByNameAsc(TENANT_ID)).thenReturn(List.of(template));

        // When
        List<SmsTemplateDto> result = smsTemplateService.getActiveTemplates();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).isActive());
    }

    // ====== getTemplate ======

    @Test
    void getTemplate_found_returnsDto() {
        // Given
        when(templateRepository.findByTenantIdAndId(TENANT_ID, 1L)).thenReturn(Optional.of(template));

        // When
        SmsTemplateDto result = smsTemplateService.getTemplate(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("ORDER_CONFIRM", result.getCode());
    }

    @Test
    void getTemplate_notFound_throwsIllegalArgumentException() {
        // Given
        when(templateRepository.findByTenantIdAndId(TENANT_ID, 999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> smsTemplateService.getTemplate(999L));
    }

    // ====== createTemplate ======

    @Test
    void createTemplate_success() {
        // Given
        when(templateRepository.existsByTenantIdAndCode(TENANT_ID, "ORDER_CONFIRM")).thenReturn(false);
        when(templateRepository.save(any(SmsTemplate.class))).thenReturn(template);

        // When
        SmsTemplateDto result = smsTemplateService.createTemplate(templateRequest);

        // Then
        assertNotNull(result);
        assertEquals("ORDER_CONFIRM", result.getCode());
        verify(templateRepository).save(any(SmsTemplate.class));
    }

    @Test
    void createTemplate_duplicateCode_throwsIllegalArgumentException() {
        // Given
        when(templateRepository.existsByTenantIdAndCode(TENANT_ID, "ORDER_CONFIRM")).thenReturn(true);

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> smsTemplateService.createTemplate(templateRequest));
        verify(templateRepository, never()).save(any());
    }

    // ====== updateTemplate ======

    @Test
    void updateTemplate_success() {
        // Given
        when(templateRepository.findByTenantIdAndId(TENANT_ID, 1L)).thenReturn(Optional.of(template));
        when(templateRepository.save(any(SmsTemplate.class))).thenReturn(template);

        // When
        SmsTemplateDto result = smsTemplateService.updateTemplate(1L, templateRequest);

        // Then
        assertNotNull(result);
        assertEquals("ORDER_CONFIRM", result.getCode());
        verify(templateRepository).save(template);
    }

    @Test
    void updateTemplate_notFound_throwsIllegalArgumentException() {
        // Given
        when(templateRepository.findByTenantIdAndId(TENANT_ID, 999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> smsTemplateService.updateTemplate(999L, templateRequest));
    }

    @Test
    void updateTemplate_codeChangedToDuplicate_throwsIllegalArgumentException() {
        // Given
        SmsTemplateRequest updateRequest = new SmsTemplateRequest();
        updateRequest.setCode("NEW_CODE");
        updateRequest.setName("New Name");
        updateRequest.setTemplate("New template");
        updateRequest.setActive(true);

        when(templateRepository.findByTenantIdAndId(TENANT_ID, 1L)).thenReturn(Optional.of(template));
        when(templateRepository.existsByTenantIdAndCode(TENANT_ID, "NEW_CODE")).thenReturn(true);

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> smsTemplateService.updateTemplate(1L, updateRequest));
        verify(templateRepository, never()).save(any());
    }

    @Test
    void updateTemplate_sameCode_skipsUniquenessCheck() {
        // Given - same code, should not check uniqueness
        when(templateRepository.findByTenantIdAndId(TENANT_ID, 1L)).thenReturn(Optional.of(template));
        when(templateRepository.save(any(SmsTemplate.class))).thenReturn(template);

        // When
        SmsTemplateDto result = smsTemplateService.updateTemplate(1L, templateRequest);

        // Then
        assertNotNull(result);
        verify(templateRepository, never()).existsByTenantIdAndCode(any(), any());
    }

    // ====== deleteTemplate ======

    @Test
    void deleteTemplate_found_deletesTemplate() {
        // Given
        when(templateRepository.findByTenantIdAndId(TENANT_ID, 1L)).thenReturn(Optional.of(template));

        // When
        smsTemplateService.deleteTemplate(1L);

        // Then
        verify(templateRepository).delete(template);
    }

    @Test
    void deleteTemplate_notFound_throwsIllegalArgumentException() {
        // Given
        when(templateRepository.findByTenantIdAndId(TENANT_ID, 999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> smsTemplateService.deleteTemplate(999L));
        verify(templateRepository, never()).delete(any());
    }

    // ====== resolveTemplate ======

    @Test
    void resolveTemplate_allVariablesProvided_returnsResolvedText() {
        // Given
        when(templateRepository.findByTenantIdAndId(TENANT_ID, 1L)).thenReturn(Optional.of(template));

        // When
        String result = smsTemplateService.resolveTemplate(1L, Map.of("name", "Sardor", "orderId", "12345"));

        // Then
        assertEquals("Hurmatli Sardor, buyurtmangiz #12345 qabul qilindi.", result);
    }

    @Test
    void resolveTemplate_missingVariable_throwsIllegalArgumentException() {
        // Given
        when(templateRepository.findByTenantIdAndId(TENANT_ID, 1L)).thenReturn(Optional.of(template));

        // When/Then - only providing "name", missing "orderId"
        assertThrows(IllegalArgumentException.class,
                () -> smsTemplateService.resolveTemplate(1L, Map.of("name", "Sardor")));
    }

    @Test
    void resolveTemplate_inactiveTemplate_throwsIllegalArgumentException() {
        // Given
        template.setActive(false);
        when(templateRepository.findByTenantIdAndId(TENANT_ID, 1L)).thenReturn(Optional.of(template));

        // When/Then
        assertThrows(IllegalArgumentException.class,
                () -> smsTemplateService.resolveTemplate(1L, Map.of("name", "Sardor")));
    }

    @Test
    void resolveTemplate_templateNotFound_throwsIllegalArgumentException() {
        // Given
        when(templateRepository.findByTenantIdAndId(TENANT_ID, 999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class,
                () -> smsTemplateService.resolveTemplate(999L, Map.of()));
    }

    @Test
    void resolveTemplate_nullVariables_resolvesWithoutReplacement() {
        // Given
        SmsTemplate simpleTemplate = SmsTemplate.builder()
                .id(2L)
                .tenantId(TENANT_ID)
                .code("SIMPLE")
                .name("Simple")
                .template("Welcome to Hisobnoma!")
                .active(true)
                .build();
        when(templateRepository.findByTenantIdAndId(TENANT_ID, 2L)).thenReturn(Optional.of(simpleTemplate));

        // When
        String result = smsTemplateService.resolveTemplate(2L, null);

        // Then
        assertEquals("Welcome to Hisobnoma!", result);
    }

    // ====== TenantContext: fail closed ======

    @Test
    void getAllTemplates_noTenantContext_failsClosed() {
        // No tenant in context → must reject, never read tenant 1's templates.
        TenantContext.clear();
        com.hisobnoma.platform.common.exception.BusinessException ex =
                assertThrows(com.hisobnoma.platform.common.exception.BusinessException.class,
                        () -> smsTemplateService.getAllTemplates());
        assertEquals("TENANT_REQUIRED", ex.getCode());
        verify(templateRepository, never()).findByTenantIdOrderByNameAsc(any());
    }
}
