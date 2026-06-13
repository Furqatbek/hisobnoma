package com.hisobnoma.platform.web.controller;

import com.hisobnoma.platform.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mobile-shop-facing error responses. Scoped to the anonymous public
 * controllers only (the staff API keeps the standard {@code ErrorResponse}
 * shape the admin frontend relies on), and given the highest precedence so it
 * wins over the global handler for these endpoints.
 *
 * <p>Returns exactly what the app parses:
 * <pre>{ "success": false, "message": "&lt;localized, user-safe&gt;", "error": { "code": "..." } }</pre>
 * Messages are Uzbek (matching the app's UI and the OTP SMS). Internal/English
 * exception text and stack traces are never exposed — unknown errors collapse
 * to a generic localized message by HTTP status.
 */
@RestControllerAdvice(assignableTypes = {
        WebAuthPublicController.class,
        WebOrderPublicController.class,
        WebCartPublicController.class,
        WebCatalogPublicController.class,
        WebPaymentPublicController.class
})
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class WebApiExceptionHandler {

    /**
     * Specific scenarios → {refined error code, Uzbek message}, matched by the
     * (stable, server-controlled) exception text. Keeps the app's `error.code`
     * meaningful and the `message` user-friendly without coupling every throw
     * site to a code. Anything not matched falls back to code/status.
     */
    private record Localized(String code, String message) {}

    private static final Map<String, Localized> EXACT = Map.ofEntries(
            Map.entry("Coupon is invalid or expired",
                    new Localized("COUPON_INVALID", "Купон яроқсиз ёки муддати ўтган")),
            Map.entry("Coupon is invalid",
                    new Localized("COUPON_INVALID", "Купон яроқсиз ёки муддати ўтган")),
            Map.entry("Order is already paid",
                    new Localized("ORDER_ALREADY_PAID", "Буюртма аллақачон тўланган")),
            Map.entry("Cancelled orders cannot be paid",
                    new Localized("ORDER_NOT_PAYABLE", "Бекор қилинган буюртмани тўлаб бўлмайди")),
            Map.entry("Invalid code",
                    new Localized("OTP_INVALID", "Тасдиқлаш коди нотўғри")),
            Map.entry("Code expired or not requested",
                    new Localized("OTP_EXPIRED", "Код муддати ўтган ёки сўралмаган")),
            Map.entry("Invalid phone number",
                    new Localized("INVALID_PHONE", "Телефон рақами нотўғри"))
    );

    /** Prefix matches for messages that carry dynamic detail (ids, names). */
    private static final Map<String, Localized> PREFIX = Map.of(
            "Product is not available",
                    new Localized("PRODUCT_UNAVAILABLE", "Маҳсулот ҳозирда мавжуд эмас"),
            "Unknown delivery region",
                    new Localized("INVALID_DELIVERY", "Етказиб бериш ҳудуди нотўғри"),
            "Unknown delivery village",
                    new Localized("INVALID_DELIVERY", "Етказиб бериш ҳудуди нотўғри")
    );

    /** Fallback by error code when no specific message matched. */
    private static final Map<String, String> BY_CODE = Map.of(
            "TOO_MANY_REQUESTS", "Жуда кўп уринишлар. Бироздан сўнг қайта уриниб кўринг",
            "PAYMENT_NOT_CONFIGURED", "Онлайн тўлов ҳозирча мавжуд эмас",
            "NOT_FOUND", "Топилмади",
            "UNAUTHORIZED", "Сессия муддати тугаган. Қайтадан киринг",
            "VALIDATION_FAILED", "Киритилган маълумотлар нотўғри ёки тўлиқ эмас"
    );

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(BusinessException ex) {
        Localized hit = match(ex.getMessage());
        String code = hit != null ? hit.code() : ex.getCode();
        String message = hit != null ? hit.message() : byCodeOrStatus(ex.getCode(), ex.getStatus());
        return body(ex.getStatus(), code, message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleBeanValidation(MethodArgumentNotValidException ex) {
        return body(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", BY_CODE.get("VALIDATION_FAILED"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        // Never leak internal detail to the shop app.
        log.error("Unhandled error on public shop endpoint", ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Хатолик юз берди");
    }

    // ---- helpers ----

    private Localized match(String rawMessage) {
        if (rawMessage == null) {
            return null;
        }
        Localized exact = EXACT.get(rawMessage);
        if (exact != null) {
            return exact;
        }
        for (Map.Entry<String, Localized> e : PREFIX.entrySet()) {
            if (rawMessage.startsWith(e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }

    private String byCodeOrStatus(String code, HttpStatus status) {
        String byCode = BY_CODE.get(code);
        if (byCode != null) {
            return byCode;
        }
        return status.is4xxClientError()
                ? "Сўров бажарилмади"      // generic 4xx
                : "Хатолик юз берди";       // generic 5xx
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String code, String message) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", code);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", false);
        resp.put("message", message);
        resp.put("error", error);
        resp.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(status).body(resp);
    }
}
