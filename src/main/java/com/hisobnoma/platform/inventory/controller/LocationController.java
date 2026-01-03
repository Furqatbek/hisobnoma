package com.hisobnoma.platform.inventory.controller;

import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.inventory.dto.CreateLocationRequest;
import com.hisobnoma.platform.inventory.dto.LocationDto;
import com.hisobnoma.platform.inventory.entity.LocationType;
import com.hisobnoma.platform.inventory.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping
    @PreAuthorize("hasAuthority('INVENTORY_STOCK_VIEW')")
    public ResponseEntity<List<LocationDto>> getAllLocations() {
        return ResponseEntity.ok(locationService.getAllLocations());
    }

    @GetMapping("/paginated")
    @PreAuthorize("hasAuthority('INVENTORY_STOCK_VIEW')")
    public ResponseEntity<PageResponse<LocationDto>> getLocationsPaginated(
            @PageableDefault(sort = "sortOrder", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(locationService.getLocations(pageable));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('INVENTORY_STOCK_VIEW')")
    public ResponseEntity<List<LocationDto>> getActiveLocations() {
        return ResponseEntity.ok(locationService.getActiveLocations());
    }

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('INVENTORY_STOCK_VIEW')")
    public ResponseEntity<List<LocationDto>> getLocationTree() {
        return ResponseEntity.ok(locationService.getLocationTree());
    }

    @GetMapping("/roots")
    @PreAuthorize("hasAuthority('INVENTORY_STOCK_VIEW')")
    public ResponseEntity<List<LocationDto>> getRootLocations() {
        return ResponseEntity.ok(locationService.getRootLocations());
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("hasAuthority('INVENTORY_STOCK_VIEW')")
    public ResponseEntity<List<LocationDto>> getLocationsByType(@PathVariable LocationType type) {
        return ResponseEntity.ok(locationService.getLocationsByType(type));
    }

    @GetMapping("/warehouse-store")
    @PreAuthorize("hasAuthority('INVENTORY_STOCK_VIEW')")
    public ResponseEntity<List<LocationDto>> getWarehouseAndStoreLocations() {
        return ResponseEntity.ok(locationService.getWarehouseAndStoreLocations());
    }

    @GetMapping("/default")
    @PreAuthorize("hasAuthority('INVENTORY_STOCK_VIEW')")
    public ResponseEntity<LocationDto> getDefaultLocation() {
        LocationDto defaultLocation = locationService.getDefaultLocation();
        if (defaultLocation == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(defaultLocation);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_STOCK_VIEW')")
    public ResponseEntity<LocationDto> getLocation(@PathVariable Long id) {
        return ResponseEntity.ok(locationService.getLocation(id));
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAuthority('INVENTORY_STOCK_VIEW')")
    public ResponseEntity<LocationDto> getLocationByCode(@PathVariable String code) {
        return ResponseEntity.ok(locationService.getLocationByCode(code));
    }

    @GetMapping("/{id}/children")
    @PreAuthorize("hasAuthority('INVENTORY_STOCK_VIEW')")
    public ResponseEntity<List<LocationDto>> getChildLocations(@PathVariable Long id) {
        return ResponseEntity.ok(locationService.getChildLocations(id));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('INVENTORY_STOCK_VIEW')")
    public ResponseEntity<PageResponse<LocationDto>> searchLocations(
            @RequestParam String q,
            @PageableDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(locationService.searchLocations(q, pageable));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('INVENTORY_LOCATION_MANAGE')")
    public ResponseEntity<LocationDto> createLocation(@Valid @RequestBody CreateLocationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(locationService.createLocation(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_LOCATION_MANAGE')")
    public ResponseEntity<LocationDto> updateLocation(
            @PathVariable Long id,
            @Valid @RequestBody CreateLocationRequest request) {
        return ResponseEntity.ok(locationService.updateLocation(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_LOCATION_MANAGE')")
    public ResponseEntity<Void> deleteLocation(@PathVariable Long id) {
        locationService.deleteLocation(id);
        return ResponseEntity.noContent().build();
    }
}
