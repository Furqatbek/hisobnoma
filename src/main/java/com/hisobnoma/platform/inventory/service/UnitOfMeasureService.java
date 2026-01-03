package com.hisobnoma.platform.inventory.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.DuplicateResourceException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.common.exception.ValidationException;
import com.hisobnoma.platform.inventory.dto.*;
import com.hisobnoma.platform.inventory.entity.UnitOfMeasure;
import com.hisobnoma.platform.inventory.mapper.UnitOfMeasureMapper;
import com.hisobnoma.platform.inventory.repository.UnitOfMeasureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnitOfMeasureService {

    private final UnitOfMeasureRepository uomRepository;
    private final UnitOfMeasureMapper uomMapper;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public List<UnitOfMeasureDto> getAllUoms() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<UnitOfMeasure> uoms = uomRepository.findAllByTenantId(tenantId);
        return uomMapper.toDtoList(uoms);
    }

    @Transactional(readOnly = true)
    public PageResponse<UnitOfMeasureDto> getUoms(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<UnitOfMeasure> page = uomRepository.findAllByTenantId(tenantId, pageable);
        return PageResponse.of(page.map(uomMapper::toDto));
    }

    @Transactional(readOnly = true)
    public List<UnitOfMeasureDto> getActiveUoms() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<UnitOfMeasure> uoms = uomRepository.findAllActiveByTenantId(tenantId);
        return uomMapper.toDtoList(uoms);
    }

    @Transactional(readOnly = true)
    public List<UnitOfMeasureDto> getBaseUoms() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<UnitOfMeasure> uoms = uomRepository.findAllBaseUnitsByTenantId(tenantId);
        return uomMapper.toDtoList(uoms);
    }

    @Transactional(readOnly = true)
    public UnitOfMeasureDto getUom(Long id) {
        UnitOfMeasure uom = uomRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Unit of Measure", id));
        return uomMapper.toDto(uom);
    }

    @Transactional(readOnly = true)
    public UnitOfMeasureDto getUomByCode(String code) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        UnitOfMeasure uom = uomRepository.findByCodeAndTenantId(code, tenantId)
                .or(() -> uomRepository.findByCode(code))
                .orElseThrow(() -> new NotFoundException("Unit of Measure", "code", code));
        return uomMapper.toDto(uom);
    }

    @Transactional(readOnly = true)
    public PageResponse<UnitOfMeasureDto> searchUoms(String search, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<UnitOfMeasure> page = uomRepository.searchByTenantId(tenantId, search, pageable);
        return PageResponse.of(page.map(uomMapper::toDto));
    }

    @Transactional
    public UnitOfMeasureDto createUom(CreateUomRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        // Check for duplicate code
        if (uomRepository.existsByCodeAndTenantId(request.getCode(), tenantId) ||
            (tenantId == null && uomRepository.existsByCodeAndTenantIdIsNull(request.getCode()))) {
            throw new DuplicateResourceException("Unit of Measure", "code", request.getCode());
        }

        UnitOfMeasure uom = uomMapper.toEntity(request);
        uom.setTenantId(tenantId);

        // Set base UOM if specified
        if (request.getBaseUomId() != null) {
            UnitOfMeasure baseUom = uomRepository.findById(request.getBaseUomId())
                    .orElseThrow(() -> new NotFoundException("Base Unit of Measure", request.getBaseUomId()));

            if (!baseUom.isBaseUnit()) {
                throw new ValidationException("Base UOM must be a base unit");
            }

            uom.setBaseUom(baseUom);
            uom.setBaseUnit(false);
        }

        if (request.getConversionFactor() == null || request.getConversionFactor().compareTo(BigDecimal.ZERO) <= 0) {
            uom.setConversionFactor(BigDecimal.ONE);
        }

        uom = uomRepository.save(uom);
        log.info("Created UOM: {} ({})", uom.getName(), uom.getCode());
        return uomMapper.toDto(uom);
    }

    @Transactional
    public UnitOfMeasureDto updateUom(Long id, CreateUomRequest request) {
        UnitOfMeasure uom = uomRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Unit of Measure", id));

        if (request.getName() != null) {
            uom.setName(request.getName());
        }

        if (request.getSymbol() != null) {
            uom.setSymbol(request.getSymbol());
        }

        if (request.getDescription() != null) {
            uom.setDescription(request.getDescription());
        }

        if (request.getConversionFactor() != null) {
            uom.setConversionFactor(request.getConversionFactor());
        }

        uom.setActive(request.isActive());

        uom = uomRepository.save(uom);
        log.info("Updated UOM: {}", uom.getId());
        return uomMapper.toDto(uom);
    }

    @Transactional
    public void deleteUom(Long id) {
        UnitOfMeasure uom = uomRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Unit of Measure", id));

        // Check if used as base for other UOMs
        List<UnitOfMeasure> dependentUoms = uomRepository.findByBaseUomId(id);
        if (!dependentUoms.isEmpty()) {
            throw new ValidationException("Cannot delete UOM that is used as base for other units");
        }

        uomRepository.delete(uom);
        log.info("Deleted UOM: {}", id);
    }

    @Transactional(readOnly = true)
    public BigDecimal convert(BigDecimal quantity, Long fromUomId, Long toUomId) {
        if (fromUomId.equals(toUomId)) {
            return quantity;
        }

        UnitOfMeasure fromUom = uomRepository.findById(fromUomId)
                .orElseThrow(() -> new NotFoundException("From Unit of Measure", fromUomId));
        UnitOfMeasure toUom = uomRepository.findById(toUomId)
                .orElseThrow(() -> new NotFoundException("To Unit of Measure", toUomId));

        // Convert to base first, then to target
        BigDecimal baseQuantity = fromUom.convertToBase(quantity);
        return toUom.convertFromBase(baseQuantity);
    }
}
