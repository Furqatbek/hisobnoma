package com.hisobnoma.platform.web.controller;

import com.hisobnoma.platform.auth.security.RequiresPermission;
import com.hisobnoma.platform.common.dto.ApiResponse;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.web.dto.UpdateOrderStatusRequest;
import com.hisobnoma.platform.web.dto.WebOrderDto;
import com.hisobnoma.platform.web.entity.WebOrderStatus;
import com.hisobnoma.platform.web.service.WebOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Staff inbox for online orders.
 */
@RestController
@RequestMapping("/api/v1/web-orders")
@RequiredArgsConstructor
public class WebOrderAdminController {

    private final WebOrderService orderService;

    @GetMapping
    @RequiresPermission("WEB_ORDER_VIEW")
    public ResponseEntity<PageResponse<WebOrderDto>> getOrders(
            @RequestParam(required = false) WebOrderStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<WebOrderDto> page = orderService.getOrders(status, pageable);
        return ResponseEntity.ok(PageResponse.of(page));
    }

    @GetMapping("/counts/new")
    @RequiresPermission("WEB_ORDER_VIEW")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getNewCount() {
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("newOrders", orderService.getNewOrderCount())));
    }

    @GetMapping("/{id}")
    @RequiresPermission("WEB_ORDER_VIEW")
    public ResponseEntity<ApiResponse<WebOrderDto>> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrder(id)));
    }

    @PostMapping("/{id}/status")
    @RequiresPermission("WEB_ORDER_MANAGE")
    public ResponseEntity<ApiResponse<WebOrderDto>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(orderService.updateStatus(id, request)));
    }

    @PostMapping("/{id}/convert-to-invoice")
    @RequiresPermission("WEB_ORDER_MANAGE")
    public ResponseEntity<ApiResponse<WebOrderDto>> convertToInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(orderService.convertToInvoice(id)));
    }
}
