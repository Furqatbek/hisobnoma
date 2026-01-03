package com.hisobnoma.platform.inventory.service;

import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.inventory.dto.CreateLocationRequest;
import com.hisobnoma.platform.inventory.dto.LocationDto;
import com.hisobnoma.platform.inventory.entity.Location;
import com.hisobnoma.platform.inventory.entity.LocationType;
import com.hisobnoma.platform.inventory.mapper.LocationMapper;
import com.hisobnoma.platform.inventory.repository.LocationRepository;
import com.hisobnoma.platform.inventory.repository.StockRepository;
import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;
    private final StockRepository stockRepository;
    private final LocationMapper locationMapper;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public List<LocationDto> getAllLocations() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return locationRepository.findByTenantIdOrderBySortOrderAsc(tenantId)
                .stream()
                .map(locationMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<LocationDto> getLocations(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<Location> page = locationRepository.findByTenantId(tenantId, pageable);
        return PageResponse.of(page.map(locationMapper::toDto));
    }

    @Transactional(readOnly = true)
    public List<LocationDto> getActiveLocations() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return locationRepository.findByTenantIdAndActiveOrderBySortOrderAsc(tenantId, true)
                .stream()
                .map(locationMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LocationDto> getLocationsByType(LocationType locationType) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return locationRepository.findByTenantIdAndLocationTypeOrderBySortOrderAsc(tenantId, locationType)
                .stream()
                .map(locationMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LocationDto> getRootLocations() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return locationRepository.findRootLocationsByTenantId(tenantId)
                .stream()
                .map(locationMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LocationDto> getLocationTree() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<Location> rootLocations = locationRepository.findRootLocationsByTenantId(tenantId);
        return rootLocations.stream()
                .map(this::mapWithChildren)
                .collect(Collectors.toList());
    }

    private LocationDto mapWithChildren(Location location) {
        LocationDto dto = locationMapper.toDto(location);
        if (location.getChildLocations() != null && !location.getChildLocations().isEmpty()) {
            dto.setChildren(location.getChildLocations().stream()
                    .map(this::mapWithChildren)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    @Transactional(readOnly = true)
    public LocationDto getLocation(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Location location = locationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Location not found with id: " + id));
        return locationMapper.toDto(location);
    }

    @Transactional(readOnly = true)
    public LocationDto getLocationByCode(String code) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Location location = locationRepository.findByCodeAndTenantId(code, tenantId)
                .orElseThrow(() -> new NotFoundException("Location not found with code: " + code));
        return locationMapper.toDto(location);
    }

    @Transactional(readOnly = true)
    public List<LocationDto> getChildLocations(Long parentId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        // Verify parent exists
        if (!locationRepository.existsById(parentId)) {
            throw new NotFoundException("Parent location not found with id: " + parentId);
        }
        return locationRepository.findByParentLocationIdAndTenantIdOrderBySortOrderAsc(parentId, tenantId)
                .stream()
                .map(locationMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<LocationDto> searchLocations(String query, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<Location> page = locationRepository.searchByNameOrCode(tenantId, query, pageable);
        return PageResponse.of(page.map(locationMapper::toDto));
    }

    @Transactional
    public LocationDto createLocation(CreateLocationRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        // Check for duplicate code
        if (locationRepository.existsByCodeAndTenantId(request.getCode(), tenantId)) {
            throw new BusinessException("Location code already exists: " + request.getCode());
        }

        Location location = locationMapper.toEntity(request);
        location.setTenantId(tenantId);

        // Set parent if specified
        if (request.getParentLocationId() != null) {
            Location parent = locationRepository.findByIdAndTenantId(request.getParentLocationId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Parent location not found"));
            location.setParentLocation(parent);
        }

        // Handle default location
        if (request.isDefault()) {
            clearDefaultLocation(tenantId);
        }

        Location saved = locationRepository.save(location);
        log.info("Created location: {} ({})", saved.getName(), saved.getCode());
        return locationMapper.toDto(saved);
    }

    @Transactional
    public LocationDto updateLocation(Long id, CreateLocationRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Location location = locationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Location not found with id: " + id));

        // Check for duplicate code (excluding current location)
        if (locationRepository.existsByCodeAndTenantIdAndIdNot(request.getCode(), tenantId, id)) {
            throw new BusinessException("Location code already exists: " + request.getCode());
        }

        // Prevent setting self as parent
        if (request.getParentLocationId() != null && request.getParentLocationId().equals(id)) {
            throw new BusinessException("Location cannot be its own parent");
        }

        locationMapper.updateEntity(location, request);

        // Update parent
        if (request.getParentLocationId() != null) {
            Location parent = locationRepository.findByIdAndTenantId(request.getParentLocationId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Parent location not found"));

            // Check for circular reference
            if (isDescendant(parent, location)) {
                throw new BusinessException("Cannot set a descendant as parent (circular reference)");
            }
            location.setParentLocation(parent);
        } else {
            location.setParentLocation(null);
        }

        // Handle default location
        if (request.isDefault() && !location.isDefault()) {
            clearDefaultLocation(tenantId);
            location.setDefault(true);
        }

        Location saved = locationRepository.save(location);
        log.info("Updated location: {} ({})", saved.getName(), saved.getCode());
        return locationMapper.toDto(saved);
    }

    private boolean isDescendant(Location potentialParent, Location ancestor) {
        if (potentialParent == null) {
            return false;
        }
        if (potentialParent.getId().equals(ancestor.getId())) {
            return true;
        }
        return isDescendant(potentialParent.getParentLocation(), ancestor);
    }

    private void clearDefaultLocation(Long tenantId) {
        locationRepository.findByTenantIdAndIsDefaultTrue(tenantId)
                .ifPresent(loc -> {
                    loc.setDefault(false);
                    locationRepository.save(loc);
                });
    }

    @Transactional
    public void deleteLocation(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Location location = locationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Location not found with id: " + id));

        // Check for child locations
        if (locationRepository.hasChildren(id, tenantId)) {
            throw new BusinessException("Cannot delete location with child locations");
        }

        // Check for stock at this location
        if (!stockRepository.findByLocationIdAndTenantId(id, tenantId).isEmpty()) {
            throw new BusinessException("Cannot delete location with existing stock. Please transfer or adjust stock first.");
        }

        locationRepository.delete(location);
        log.info("Deleted location: {} ({})", location.getName(), location.getCode());
    }

    @Transactional(readOnly = true)
    public LocationDto getDefaultLocation() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return locationRepository.findByTenantIdAndIsDefaultTrue(tenantId)
                .map(locationMapper::toDto)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<LocationDto> getWarehouseAndStoreLocations() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return locationRepository.findActiveByTypes(tenantId, List.of(LocationType.WAREHOUSE, LocationType.STORE))
                .stream()
                .map(locationMapper::toDto)
                .collect(Collectors.toList());
    }
}
