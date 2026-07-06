package com.hisobnoma.platform.distribution.b2b.controller;

import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.distribution.b2b.dto.B2bPlaceOrderRequest;
import com.hisobnoma.platform.distribution.b2b.service.B2bAuthService;
import com.hisobnoma.platform.distribution.b2b.service.B2bOrderService;
import com.hisobnoma.platform.distribution.dto.DistributionOrderDto;
import com.hisobnoma.platform.finance.entity.Customer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/b2b/orders")
@RequiredArgsConstructor
public class B2bOrderController {

    private final B2bAuthService authService;
    private final B2bOrderService orderService;

    @GetMapping
    public ResponseEntity<PageResponse<DistributionOrderDto>> myOrders(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Customer buyer = authService.requireCustomer(authorization);
        return ResponseEntity.ok(orderService.listOrders(buyer, pageable));
    }

    @GetMapping("/{orderNumber}")
    public ResponseEntity<DistributionOrderDto> getOrder(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable String orderNumber) {
        Customer buyer = authService.requireCustomer(authorization);
        return ResponseEntity.ok(orderService.getOrder(buyer, orderNumber));
    }

    @PostMapping
    public ResponseEntity<DistributionOrderDto> placeOrder(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody B2bPlaceOrderRequest request) {
        Customer buyer = authService.requireCustomer(authorization);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder(buyer, request));
    }
}
