package com.hisobnoma.platform.delivery.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.DuplicateResourceException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.delivery.dto.DeliveryRegionDTO;
import com.hisobnoma.platform.delivery.entity.DeliveryRegion;
import com.hisobnoma.platform.delivery.mapper.DeliveryRegionMapper;
import com.hisobnoma.platform.delivery.repository.DeliveryRegionRepository;
import com.hisobnoma.platform.delivery.repository.DeliveryVillageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryRegionService {

    private final DeliveryRegionRepository regionRepository;
    private final DeliveryVillageRepository villageRepository;
    private final DeliveryRegionMapper regionMapper;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public Page<DeliveryRegionDTO> findAll(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return regionRepository.findByTenantId(tenantId, pageable)
                .map(regionMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<DeliveryRegionDTO> search(String query, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return regionRepository.searchByTenantId(query, tenantId, pageable)
                .map(regionMapper::toDto);
    }

    @Transactional(readOnly = true)
    public DeliveryRegionDTO findById(Long id) {
        return regionMapper.toDto(getRegionById(id));
    }

    @Transactional(readOnly = true)
    public List<DeliveryRegionDTO> findActive() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return regionMapper.toDtoList(regionRepository.findActiveByTenantId(tenantId));
    }

    @Transactional
    public DeliveryRegionDTO create(DeliveryRegionDTO dto) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        if (dto.getCode() != null && !dto.getCode().isBlank()) {
            if (regionRepository.existsByCodeAndTenantId(dto.getCode(), tenantId, null)) {
                throw new DuplicateResourceException("Region with code " + dto.getCode() + " already exists");
            }
        }

        DeliveryRegion region = regionMapper.toEntity(dto);
        region.setTenantId(tenantId);
        region = regionRepository.save(region);

        log.info("Created delivery region: {}", region.getName());
        return regionMapper.toDto(region);
    }

    @Transactional
    public DeliveryRegionDTO update(Long id, DeliveryRegionDTO dto) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        DeliveryRegion region = getRegionById(id);

        if (dto.getCode() != null && !dto.getCode().isBlank()) {
            if (regionRepository.existsByCodeAndTenantId(dto.getCode(), tenantId, id)) {
                throw new DuplicateResourceException("Region with code " + dto.getCode() + " already exists");
            }
        }

        regionMapper.updateEntity(dto, region);
        region = regionRepository.save(region);

        log.info("Updated delivery region: {}", region.getName());
        return regionMapper.toDto(region);
    }

    @Transactional
    public void delete(Long id) {
        DeliveryRegion region = getRegionById(id);
        long villageCount = villageRepository.countByRegionId(id);

        if (villageCount > 0) {
            throw new IllegalStateException("Cannot delete region with " + villageCount + " villages. Remove villages first.");
        }

        regionRepository.delete(region);
        log.info("Deleted delivery region: {}", region.getName());
    }

    private DeliveryRegion getRegionById(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return regionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Delivery region not found: " + id));
    }
}
