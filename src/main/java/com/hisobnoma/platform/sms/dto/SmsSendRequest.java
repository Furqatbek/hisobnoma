package com.hisobnoma.platform.sms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.Map;

@Data
public class SmsSendRequest {

    @NotBlank(message = "Телефон рақами киритилиши шарт")
    @Pattern(regexp = "^\\+?998\\d{9}$", message = "Телефон рақами 998XXXXXXXXX ёки +998XXXXXXXXX форматида бўлиши керак")
    private String phone;

    @NotNull(message = "Шаблон танланиши шарт")
    private Long templateId;

    private Map<String, String> variables;

    private String from;
}
