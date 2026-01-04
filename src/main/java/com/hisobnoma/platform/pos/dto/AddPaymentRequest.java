package com.hisobnoma.platform.pos.dto;

import com.hisobnoma.platform.pos.entity.POSPaymentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddPaymentRequest {

    @NotNull(message = "Payment type is required")
    private POSPaymentType paymentType;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    /**
     * For cash payments - actual amount tendered
     */
    private BigDecimal tenderedAmount;

    /**
     * For card payments
     */
    @Size(max = 50, message = "Card type must not exceed 50 characters")
    private String cardType;

    @Size(max = 4, message = "Card last four must not exceed 4 characters")
    private String cardLastFour;

    @Size(max = 50, message = "Auth code must not exceed 50 characters")
    private String authCode;

    @Size(max = 100, message = "Gateway reference must not exceed 100 characters")
    private String gatewayReference;

    /**
     * For gift card payments
     */
    @Size(max = 50, message = "Gift card number must not exceed 50 characters")
    private String giftCardNumber;

    /**
     * For check payments
     */
    @Size(max = 50, message = "Check number must not exceed 50 characters")
    private String checkNumber;

    /**
     * For mobile payments
     */
    @Size(max = 100, message = "Mobile reference must not exceed 100 characters")
    private String mobileReference;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
