package com.hisobnoma.platform.inventory.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.inventory.dto.CreateVendorRequest;
import com.hisobnoma.platform.inventory.dto.VendorDto;
import com.hisobnoma.platform.inventory.entity.Vendor;
import com.hisobnoma.platform.inventory.mapper.VendorMapper;
import com.hisobnoma.platform.inventory.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorRepository vendorRepository;
    private final VendorMapper vendorMapper;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public PageResponse<VendorDto> getVendors(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<Vendor> page = vendorRepository.findAllByTenantId(tenantId, pageable);
        return PageResponse.of(page.map(vendorMapper::toDto));
    }

    @Transactional(readOnly = true)
    public List<VendorDto> getActiveVendors() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<Vendor> vendors = vendorRepository.findAllActiveByTenantId(tenantId);
        return vendorMapper.toDtoList(vendors);
    }

    @Transactional(readOnly = true)
    public VendorDto getVendor(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Vendor vendor = vendorRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Vendor not found with id: " + id));
        return vendorMapper.toDto(vendor);
    }

    @Transactional(readOnly = true)
    public VendorDto getVendorByCode(String code) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Vendor vendor = vendorRepository.findByCodeAndTenantId(code, tenantId)
                .orElseThrow(() -> new NotFoundException("Vendor not found with code: " + code));
        return vendorMapper.toDto(vendor);
    }

    @Transactional(readOnly = true)
    public PageResponse<VendorDto> searchVendors(String search, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<Vendor> page = vendorRepository.searchByTenantId(tenantId, search, pageable);
        return PageResponse.of(page.map(vendorMapper::toDto));
    }

    @Transactional(readOnly = true)
    public List<VendorDto> getPreferredVendors() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<Vendor> vendors = vendorRepository.findPreferredVendorsByTenantId(tenantId);
        return vendorMapper.toDtoList(vendors);
    }

    @Transactional
    public VendorDto createVendor(CreateVendorRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        // Check for duplicate code
        if (vendorRepository.existsByCodeAndTenantId(request.getCode(), tenantId)) {
            throw new BusinessException("Vendor with code '" + request.getCode() + "' already exists");
        }

        Vendor vendor = vendorMapper.toEntity(request);
        vendor.setTenantId(tenantId);

        vendor = vendorRepository.save(vendor);
        return vendorMapper.toDto(vendor);
    }

    @Transactional
    public VendorDto updateVendor(Long id, CreateVendorRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Vendor vendor = vendorRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Vendor not found with id: " + id));

        // Check for duplicate code if changed
        if (!vendor.getCode().equals(request.getCode()) &&
            vendorRepository.existsByCodeAndTenantId(request.getCode(), tenantId)) {
            throw new BusinessException("Vendor with code '" + request.getCode() + "' already exists");
        }

        vendorMapper.updateEntity(request, vendor);
        vendor = vendorRepository.save(vendor);
        return vendorMapper.toDto(vendor);
    }

    @Transactional
    public void deleteVendor(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Vendor vendor = vendorRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Vendor not found with id: " + id));

        // Soft delete by deactivating
        vendor.setActive(false);
        vendorRepository.save(vendor);
    }

    @Transactional
    public VendorDto activateVendor(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Vendor vendor = vendorRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Vendor not found with id: " + id));

        vendor.setActive(true);
        vendor = vendorRepository.save(vendor);
        return vendorMapper.toDto(vendor);
    }
}
