package com.hisobnoma.platform.web.controller;

import com.hisobnoma.platform.auth.security.RequiresPermission;
import com.hisobnoma.platform.common.dto.ApiResponse;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.web.dto.WebCustomerDto;
import com.hisobnoma.platform.web.service.WebCustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Staff management of online (mobile app) customer accounts.
 */
@RestController
@RequestMapping("/api/v1/web-customers")
@RequiredArgsConstructor
public class WebCustomerAdminController {

    private final WebCustomerService customerService;

    @GetMapping
    @RequiresPermission("WEB_CUSTOMER_VIEW")
    public ResponseEntity<PageResponse<WebCustomerDto>> getCustomers(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<WebCustomerDto> page = customerService.getCustomers(search, pageable);
        return ResponseEntity.ok(PageResponse.of(page));
    }

    @GetMapping("/{id}")
    @RequiresPermission("WEB_CUSTOMER_VIEW")
    public ResponseEntity<ApiResponse<WebCustomerDto>> getCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(customerService.getCustomer(id)));
    }

    @PostMapping("/{id}/link-customer")
    @RequiresPermission("WEB_CUSTOMER_MANAGE")
    public ResponseEntity<ApiResponse<WebCustomerDto>> linkCustomer(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body) {
        return ResponseEntity.ok(ApiResponse.success(
                customerService.linkCustomer(id, body.get("customerId"))));
    }

    @PostMapping("/{id}/unlink-customer")
    @RequiresPermission("WEB_CUSTOMER_MANAGE")
    public ResponseEntity<ApiResponse<WebCustomerDto>> unlinkCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(customerService.unlinkCustomer(id)));
    }
}
