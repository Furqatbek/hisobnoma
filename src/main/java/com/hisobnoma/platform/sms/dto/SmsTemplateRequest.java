package com.hisobnoma.platform.sms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SmsTemplateRequest {

    @NotBlank(message = "Shablon kodi kiritilishi shart")
    @Size(max = 50)
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "Kod faqat katta lotin harflari va pastki chiziq bo'lishi kerak")
    private String code;

    @NotBlank(message = "Shablon nomi kiritilishi shart")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Shablon matni kiritilishi shart")
    @Size(max = 500)
    private String template;

    private boolean active = true;
}
