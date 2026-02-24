package com.hisobnoma.platform.sms.service;

import com.hisobnoma.platform.sms.dto.SmsTemplateDto;
import com.hisobnoma.platform.sms.dto.SmsTemplateRequest;
import com.hisobnoma.platform.sms.entity.SmsTemplate;
import com.hisobnoma.platform.sms.repository.SmsTemplateRepository;
import com.hisobnoma.platform.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsTemplateService {

    private static final Long DEFAULT_TENANT_ID = 1L;
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{(\\w+)}");

    private final SmsTemplateRepository templateRepository;

    private Long resolveTenantId() {
        Long tenantId = TenantContext.getCurrentTenant();
        return tenantId != null ? tenantId : DEFAULT_TENANT_ID;
    }

    public List<SmsTemplateDto> getAllTemplates() {
        return templateRepository.findByTenantIdOrderByNameAsc(resolveTenantId())
                .stream()
                .map(SmsTemplateDto::from)
                .collect(Collectors.toList());
    }

    public List<SmsTemplateDto> getActiveTemplates() {
        return templateRepository.findByTenantIdAndActiveTrueOrderByNameAsc(resolveTenantId())
                .stream()
                .map(SmsTemplateDto::from)
                .collect(Collectors.toList());
    }

    public SmsTemplateDto getTemplate(Long id) {
        SmsTemplate template = templateRepository.findByTenantIdAndId(resolveTenantId(), id)
                .orElseThrow(() -> new IllegalArgumentException("Shablon topilmadi: " + id));
        return SmsTemplateDto.from(template);
    }

    @Transactional
    public SmsTemplateDto createTemplate(SmsTemplateRequest request) {
        Long tenantId = resolveTenantId();
        if (templateRepository.existsByTenantIdAndCode(tenantId, request.getCode())) {
            throw new IllegalArgumentException("Bu kodli shablon allaqachon mavjud: " + request.getCode());
        }

        SmsTemplate template = SmsTemplate.builder()
                .tenantId(tenantId)
                .code(request.getCode())
                .name(request.getName())
                .template(request.getTemplate())
                .active(request.isActive())
                .build();

        template = templateRepository.save(template);
        log.info("SMS template created: code={}, tenant={}", template.getCode(), tenantId);
        return SmsTemplateDto.from(template);
    }

    @Transactional
    public SmsTemplateDto updateTemplate(Long id, SmsTemplateRequest request) {
        Long tenantId = resolveTenantId();
        SmsTemplate template = templateRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new IllegalArgumentException("Shablon topilmadi: " + id));

        // Check code uniqueness if changed
        if (!template.getCode().equals(request.getCode())
                && templateRepository.existsByTenantIdAndCode(tenantId, request.getCode())) {
            throw new IllegalArgumentException("Bu kodli shablon allaqachon mavjud: " + request.getCode());
        }

        template.setCode(request.getCode());
        template.setName(request.getName());
        template.setTemplate(request.getTemplate());
        template.setActive(request.isActive());

        template = templateRepository.save(template);
        log.info("SMS template updated: id={}, code={}", id, template.getCode());
        return SmsTemplateDto.from(template);
    }

    @Transactional
    public void deleteTemplate(Long id) {
        SmsTemplate template = templateRepository.findByTenantIdAndId(resolveTenantId(), id)
                .orElseThrow(() -> new IllegalArgumentException("Shablon topilmadi: " + id));
        templateRepository.delete(template);
        log.info("SMS template deleted: id={}, code={}", id, template.getCode());
    }

    /**
     * Resolve a template with the given variables to produce the final SMS text.
     */
    public String resolveTemplate(Long templateId, Map<String, String> variables) {
        SmsTemplate template = templateRepository.findByTenantIdAndId(resolveTenantId(), templateId)
                .orElseThrow(() -> new IllegalArgumentException("Shablon topilmadi: " + templateId));

        if (!template.isActive()) {
            throw new IllegalArgumentException("Shablon faol emas: " + template.getCode());
        }

        String text = template.getTemplate();
        if (variables != null) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                text = text.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }

        // Check for unresolved variables
        Matcher matcher = VARIABLE_PATTERN.matcher(text);
        if (matcher.find()) {
            throw new IllegalArgumentException("O'zgaruvchi kiritilmagan: {" + matcher.group(1) + "}");
        }

        return text;
    }
}
