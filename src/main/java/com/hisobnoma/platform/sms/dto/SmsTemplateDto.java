package com.hisobnoma.platform.sms.dto;

import com.hisobnoma.platform.sms.entity.SmsTemplate;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Data
@Builder
public class SmsTemplateDto {

    private Long id;
    private String code;
    private String name;
    private String template;
    private boolean active;
    private List<String> variables;
    private Instant createdAt;

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{(\\w+)}");

    public static SmsTemplateDto from(SmsTemplate entity) {
        Matcher matcher = VARIABLE_PATTERN.matcher(entity.getTemplate());
        List<String> vars = matcher.results()
                .map(m -> m.group(1))
                .distinct()
                .collect(Collectors.toList());

        return SmsTemplateDto.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .template(entity.getTemplate())
                .active(entity.isActive())
                .variables(vars)
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
