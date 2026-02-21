package com.hisobnoma.platform.delivery.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.DuplicateResourceException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.delivery.dto.DeliveryVillageDTO;
import com.hisobnoma.platform.delivery.entity.DeliveryRegion;
import com.hisobnoma.platform.delivery.entity.DeliveryVillage;
import com.hisobnoma.platform.delivery.mapper.DeliveryVillageMapper;
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
public class DeliveryVillageService {

    private final DeliveryVillageRepository villageRepository;
    private final DeliveryRegionRepository regionRepository;
    private final DeliveryVillageMapper villageMapper;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public Page<DeliveryVillageDTO> findAll(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return villageRepository.findByTenantId(tenantId, pageable)
                .map(villageMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<DeliveryVillageDTO> search(String query, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return villageRepository.searchByTenantId(query, tenantId, pageable)
                .map(villageMapper::toDto);
    }

    @Transactional(readOnly = true)
    public DeliveryVillageDTO findById(Long id) {
        return villageMapper.toDto(getVillageById(id));
    }

    @Transactional(readOnly = true)
    public List<DeliveryVillageDTO> findByRegion(Long regionId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return villageMapper.toDtoList(
                villageRepository.findActiveByRegionIdAndTenantId(regionId, tenantId));
    }

    @Transactional(readOnly = true)
    public List<DeliveryVillageDTO> findActive() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return villageMapper.toDtoList(villageRepository.findActiveByTenantId(tenantId));
    }

    @Transactional
    public DeliveryVillageDTO create(DeliveryVillageDTO dto) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        DeliveryRegion region = regionRepository.findByIdAndTenantId(dto.getRegionId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Delivery region not found: " + dto.getRegionId()));

        if (dto.getCode() != null && !dto.getCode().isBlank()) {
            if (villageRepository.existsByCodeAndTenantId(dto.getCode(), tenantId, null)) {
                throw new DuplicateResourceException("Village with code " + dto.getCode() + " already exists");
            }
        }

        DeliveryVillage village = villageMapper.toEntity(dto);
        village.setTenantId(tenantId);
        village.setRegion(region);
        village = villageRepository.save(village);

        log.info("Created delivery village: {} in region: {}", village.getName(), region.getName());
        return villageMapper.toDto(village);
    }

    @Transactional
    public DeliveryVillageDTO update(Long id, DeliveryVillageDTO dto) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        DeliveryVillage village = getVillageById(id);

        if (dto.getRegionId() != null && !dto.getRegionId().equals(village.getRegion().getId())) {
            DeliveryRegion region = regionRepository.findByIdAndTenantId(dto.getRegionId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Delivery region not found: " + dto.getRegionId()));
            village.setRegion(region);
        }

        if (dto.getCode() != null && !dto.getCode().isBlank()) {
            if (villageRepository.existsByCodeAndTenantId(dto.getCode(), tenantId, id)) {
                throw new DuplicateResourceException("Village with code " + dto.getCode() + " already exists");
            }
        }

        villageMapper.updateEntity(dto, village);
        village = villageRepository.save(village);

        log.info("Updated delivery village: {}", village.getName());
        return villageMapper.toDto(village);
    }

    @Transactional
    public void delete(Long id) {
        DeliveryVillage village = getVillageById(id);
        villageRepository.delete(village);
        log.info("Deleted delivery village: {}", village.getName());
    }

    private DeliveryVillage getVillageById(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return villageRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Delivery village not found: " + id));
    }
}
