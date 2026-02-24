package com.hisobnoma.platform.sms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SmsSendRequest {

    @NotBlank(message = "Телефон рақами киритилиши шарт")
    @Pattern(regexp = "^998\\d{9}$", message = "Телефон рақами 998XXXXXXXXX форматида бўлиши керак")
    private String phone;

    @NotBlank(message = "Хабар матни киритилиши шарт")
    private String message;

    private String from;
}
