package com.hisobnoma.platform.delivery.controller;

import com.hisobnoma.platform.auth.security.RequiresPermission;
import com.hisobnoma.platform.common.dto.ApiResponse;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.delivery.dto.DeliveryRegionDTO;
import com.hisobnoma.platform.delivery.service.DeliveryRegionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/delivery/regions")
@RequiredArgsConstructor
public class DeliveryRegionController {

    private final DeliveryRegionService regionService;

    @GetMapping
    @RequiresPermission("DELIVERY_REGION_READ")
    public ResponseEntity<PageResponse<DeliveryRegionDTO>> getAll(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "sortOrder", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<DeliveryRegionDTO> page = search != null && !search.isBlank()
                ? regionService.search(search, pageable)
                : regionService.findAll(pageable);

        return ResponseEntity.ok(PageResponse.of(page));
    }

    @GetMapping("/active")
    @RequiresPermission("DELIVERY_REGION_READ")
    public ResponseEntity<ApiResponse<List<DeliveryRegionDTO>>> getActive() {
        List<DeliveryRegionDTO> regions = regionService.findActive();
        return ResponseEntity.ok(ApiResponse.success(regions));
    }

    @GetMapping("/{id}")
    @RequiresPermission("DELIVERY_REGION_READ")
    public ResponseEntity<ApiResponse<DeliveryRegionDTO>> getById(@PathVariable Long id) {
        DeliveryRegionDTO region = regionService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(region));
    }

    @PostMapping
    @RequiresPermission("DELIVERY_REGION_CREATE")
    public ResponseEntity<ApiResponse<DeliveryRegionDTO>> create(@Valid @RequestBody DeliveryRegionDTO dto) {
        DeliveryRegionDTO created = regionService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @PutMapping("/{id}")
    @RequiresPermission("DELIVERY_REGION_UPDATE")
    public ResponseEntity<ApiResponse<DeliveryRegionDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody DeliveryRegionDTO dto) {
        DeliveryRegionDTO updated = regionService.update(id, dto);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("DELIVERY_REGION_DELETE")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        regionService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Region deleted successfully"));
    }
}
