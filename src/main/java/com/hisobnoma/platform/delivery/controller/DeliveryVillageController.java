package com.hisobnoma.platform.delivery.controller;

import com.hisobnoma.platform.auth.security.RequiresPermission;
import com.hisobnoma.platform.common.dto.ApiResponse;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.delivery.dto.DeliveryVillageDTO;
import com.hisobnoma.platform.delivery.service.DeliveryVillageService;
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
@RequestMapping("/api/v1/delivery/villages")
@RequiredArgsConstructor
public class DeliveryVillageController {

    private final DeliveryVillageService villageService;

    @GetMapping
    @RequiresPermission("DELIVERY_VILLAGE_READ")
    public ResponseEntity<PageResponse<DeliveryVillageDTO>> getAll(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "sortOrder", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<DeliveryVillageDTO> page = search != null && !search.isBlank()
                ? villageService.search(search, pageable)
                : villageService.findAll(pageable);

        return ResponseEntity.ok(PageResponse.of(page));
    }

    @GetMapping("/active")
    @RequiresPermission("DELIVERY_VILLAGE_READ")
    public ResponseEntity<ApiResponse<List<DeliveryVillageDTO>>> getActive() {
        List<DeliveryVillageDTO> villages = villageService.findActive();
        return ResponseEntity.ok(ApiResponse.success(villages));
    }

    @GetMapping("/region/{regionId}")
    @RequiresPermission("DELIVERY_VILLAGE_READ")
    public ResponseEntity<ApiResponse<List<DeliveryVillageDTO>>> getByRegion(@PathVariable Long regionId) {
        List<DeliveryVillageDTO> villages = villageService.findByRegion(regionId);
        return ResponseEntity.ok(ApiResponse.success(villages));
    }

    @GetMapping("/{id}")
    @RequiresPermission("DELIVERY_VILLAGE_READ")
    public ResponseEntity<ApiResponse<DeliveryVillageDTO>> getById(@PathVariable Long id) {
        DeliveryVillageDTO village = villageService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(village));
    }

    @PostMapping
    @RequiresPermission("DELIVERY_VILLAGE_CREATE")
    public ResponseEntity<ApiResponse<DeliveryVillageDTO>> create(@Valid @RequestBody DeliveryVillageDTO dto) {
        DeliveryVillageDTO created = villageService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @PutMapping("/{id}")
    @RequiresPermission("DELIVERY_VILLAGE_UPDATE")
    public ResponseEntity<ApiResponse<DeliveryVillageDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody DeliveryVillageDTO dto) {
        DeliveryVillageDTO updated = villageService.update(id, dto);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("DELIVERY_VILLAGE_DELETE")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        villageService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Village deleted successfully"));
    }
}
