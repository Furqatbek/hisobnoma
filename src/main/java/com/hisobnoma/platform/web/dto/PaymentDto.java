package com.hisobnoma.platform.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Payment view for the mobile app. {@code id} is the opaque token used to poll
 * {@code GET /web/payments/{id}}; {@code paymentUrl} is always HTTPS.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDto {

    private String id;
    private String paymentUrl;
    private String status;
    private String provider;
    private BigDecimal amount;
}
