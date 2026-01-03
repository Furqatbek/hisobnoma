package com.hisobnoma.platform.inventory.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.DuplicateResourceException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.inventory.dto.*;
import com.hisobnoma.platform.inventory.entity.Brand;
import com.hisobnoma.platform.inventory.mapper.BrandMapper;
import com.hisobnoma.platform.inventory.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;
    private final BrandMapper brandMapper;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public List<BrandDto> getAllBrands() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<Brand> brands = brandRepository.findAllByTenantId(tenantId);
        return brandMapper.toDtoList(brands);
    }

    @Transactional(readOnly = true)
    public PageResponse<BrandDto> getBrands(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<Brand> page = brandRepository.findAllByTenantId(tenantId, pageable);
        return PageResponse.of(page.map(brandMapper::toDto));
    }

    @Transactional(readOnly = true)
    public List<BrandDto> getActiveBrands() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<Brand> brands = brandRepository.findAllActiveByTenantId(tenantId);
        return brandMapper.toDtoList(brands);
    }

    @Transactional(readOnly = true)
    public BrandDto getBrand(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Brand", id));
        return brandMapper.toDto(brand);
    }

    @Transactional(readOnly = true)
    public PageResponse<BrandDto> searchBrands(String search, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<Brand> page = brandRepository.searchByTenantId(tenantId, search, pageable);
        return PageResponse.of(page.map(brandMapper::toDto));
    }

    @Transactional
    public BrandDto createBrand(CreateBrandRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        // Check for duplicate code
        if (brandRepository.existsByCodeAndTenantId(request.getCode(), tenantId) ||
            (tenantId == null && brandRepository.existsByCodeAndTenantIdIsNull(request.getCode()))) {
            throw new DuplicateResourceException("Brand", "code", request.getCode());
        }

        Brand brand = brandMapper.toEntity(request);
        brand.setTenantId(tenantId);

        if (request.getSortOrder() == null) {
            brand.setSortOrder(0);
        }

        brand = brandRepository.save(brand);
        log.info("Created brand: {} ({})", brand.getName(), brand.getCode());
        return brandMapper.toDto(brand);
    }

    @Transactional
    public BrandDto updateBrand(Long id, UpdateBrandRequest request) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Brand", id));

        if (request.getName() != null) {
            brand.setName(request.getName());
        }

        if (request.getDescription() != null) {
            brand.setDescription(request.getDescription());
        }

        if (request.getLogoUrl() != null) {
            brand.setLogoUrl(request.getLogoUrl());
        }

        if (request.getWebsite() != null) {
            brand.setWebsite(request.getWebsite());
        }

        if (request.getSortOrder() != null) {
            brand.setSortOrder(request.getSortOrder());
        }

        if (request.getActive() != null) {
            brand.setActive(request.getActive());
        }

        brand = brandRepository.save(brand);
        log.info("Updated brand: {}", brand.getId());
        return brandMapper.toDto(brand);
    }

    @Transactional
    public void deleteBrand(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Brand", id));

        brandRepository.delete(brand);
        log.info("Deleted brand: {}", id);
    }
}
