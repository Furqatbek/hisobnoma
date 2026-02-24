package com.hisobnoma.platform.sms.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SmsBulkSendRequest {

    @NotNull(message = "Шаблон танланиши шарт")
    private Long templateId;

    @NotEmpty(message = "Камида битта қабул қилувчи киритилиши шарт")
    private List<Recipient> recipients;

    private String from;

    @Data
    public static class Recipient {
        private String phone;
        private Map<String, String> variables;
    }
}
