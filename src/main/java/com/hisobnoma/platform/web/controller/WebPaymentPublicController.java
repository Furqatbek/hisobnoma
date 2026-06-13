package com.hisobnoma.platform.web.controller;

import com.hisobnoma.platform.common.dto.ApiResponse;
import com.hisobnoma.platform.web.dto.CreatePaymentRequest;
import com.hisobnoma.platform.web.dto.PaymentDto;
import com.hisobnoma.platform.web.service.WebPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Online card payment for the shop mobile app. Anonymous at the security
 * layer (whitelisted /api/v1/web/**); the create call authorizes by matching
 * the phone to the order, and status polling uses an opaque payment id.
 */
@RestController
@RequestMapping("/api/v1/web")
@RequiredArgsConstructor
public class WebPaymentPublicController {

    private final WebPaymentService paymentService;

    /**
     * Start a payment for an order. Returns an opaque {@code id}, an HTTPS
     * {@code paymentUrl} to open, and the initial {@code status} (PENDING).
     * 503 if the chosen provider isn't configured in this environment.
     */
    @PostMapping("/orders/{orderNumber}/payment")
    public ResponseEntity<ApiResponse<PaymentDto>> createPayment(
            @PathVariable String orderNumber,
            @Valid @RequestBody CreatePaymentRequest request) {
        PaymentDto payment = paymentService.createPayment(orderNumber, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(payment));
    }

    /** Poll a payment's status by its opaque id (no PII in the URL). */
    @GetMapping("/payments/{id}")
    public ResponseEntity<ApiResponse<PaymentDto>> getPayment(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getStatus(id)));
    }
}
