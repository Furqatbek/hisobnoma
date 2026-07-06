package com.hisobnoma.platform.distribution.b2b.controller;

import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.distribution.b2b.dto.B2bCatalogProductDto;
import com.hisobnoma.platform.distribution.b2b.service.B2bAuthService;
import com.hisobnoma.platform.distribution.b2b.service.B2bCatalogService;
import com.hisobnoma.platform.finance.entity.Customer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/b2b/catalog")
@RequiredArgsConstructor
public class B2bCatalogController {

    private final B2bAuthService authService;
    private final B2bCatalogService catalogService;

    @GetMapping
    public ResponseEntity<PageResponse<B2bCatalogProductDto>> getCatalog(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @PageableDefault(size = 30, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        Customer buyer = authService.requireCustomer(authorization);
        return ResponseEntity.ok(catalogService.getCatalog(buyer, search, categoryId, pageable));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<B2bCatalogProductDto> getProduct(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable Long productId) {
        Customer buyer = authService.requireCustomer(authorization);
        return ResponseEntity.ok(catalogService.getProduct(buyer, productId));
    }
}
