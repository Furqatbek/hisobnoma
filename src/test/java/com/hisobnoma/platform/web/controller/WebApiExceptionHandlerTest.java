package com.hisobnoma.platform.web.controller;

import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.common.exception.ValidationException;
import com.hisobnoma.platform.web.exception.PaymentNotConfiguredException;
import com.hisobnoma.platform.web.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WebApiExceptionHandlerTest {

    private final WebApiExceptionHandler handler = new WebApiExceptionHandler();

    @SuppressWarnings("unchecked")
    private Map<String, Object> error(ResponseEntity<Map<String, Object>> resp) {
        return (Map<String, Object>) resp.getBody().get("error");
    }

    @Test
    void couponInvalid_localizedMessageAndRefinedCode() {
        var resp = handler.handleBusiness(new ValidationException("Coupon is invalid or expired"));

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals(false, resp.getBody().get("success"));
        assertEquals("Купон яроқсиз ёки муддати ўтган", resp.getBody().get("message"));
        assertEquals("COUPON_INVALID", error(resp).get("code"));
    }

    @Test
    void productUnavailable_matchesByPrefix_andHidesId() {
        var resp = handler.handleBusiness(new ValidationException("Product is not available: 42"));

        assertEquals("PRODUCT_UNAVAILABLE", error(resp).get("code"));
        assertEquals("Маҳсулот ҳозирда мавжуд эмас", resp.getBody().get("message"));
        // The internal id must not leak into the user message.
        assertFalse(resp.getBody().get("message").toString().contains("42"));
    }

    @Test
    void tooManyRequests_keepsStatusAndCode() {
        var resp = handler.handleBusiness(new TooManyRequestsException("Too many checkout attempts"));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, resp.getStatusCode());
        assertEquals("TOO_MANY_REQUESTS", error(resp).get("code"));
        assertEquals("Жуда кўп уринишлар. Бироздан сўнг қайта уриниб кўринг",
                resp.getBody().get("message"));
    }

    @Test
    void paymentNotConfigured_returns503Localized() {
        var resp = handler.handleBusiness(new PaymentNotConfiguredException("Payme not configured"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, resp.getStatusCode());
        assertEquals("PAYMENT_NOT_CONFIGURED", error(resp).get("code"));
        assertEquals("Онлайн тўлов ҳозирча мавжуд эмас", resp.getBody().get("message"));
    }

    @Test
    void notFound_genericLocalized() {
        var resp = handler.handleBusiness(new NotFoundException("Order not found: WO-000001"));

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertEquals("NOT_FOUND", error(resp).get("code"));
        assertEquals("Топилмади", resp.getBody().get("message"));
    }

    @Test
    void unexpectedException_neverLeaksInternalDetail() {
        var resp = handler.handleUnexpected(new RuntimeException("NullPointerException at line 42"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertEquals("INTERNAL_ERROR", error(resp).get("code"));
        assertEquals("Хатолик юз берди", resp.getBody().get("message"));
        assertFalse(resp.getBody().get("message").toString().contains("NullPointer"));
    }

    @Test
    void unknownBusinessCode_fallsBackToGenericByStatus() {
        var resp = handler.handleBusiness(
                new BusinessException("some internal english text", "WEIRD_CODE", HttpStatus.CONFLICT));

        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
        assertEquals("WEIRD_CODE", error(resp).get("code"));
        assertEquals("Сўров бажарилмади", resp.getBody().get("message"));
        assertFalse(resp.getBody().get("message").toString().contains("english"));
    }
}
