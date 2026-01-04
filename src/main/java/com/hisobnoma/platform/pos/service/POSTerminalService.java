package com.hisobnoma.platform.pos.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.DuplicateResourceException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.inventory.entity.Location;
import com.hisobnoma.platform.inventory.repository.LocationRepository;
import com.hisobnoma.platform.pos.dto.CreateTerminalRequest;
import com.hisobnoma.platform.pos.dto.POSTerminalDto;
import com.hisobnoma.platform.pos.entity.POSTerminal;
import com.hisobnoma.platform.pos.mapper.POSTerminalMapper;
import com.hisobnoma.platform.pos.repository.POSTerminalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class POSTerminalService {

    private final POSTerminalRepository terminalRepository;
    private final LocationRepository locationRepository;
    private final POSTerminalMapper terminalMapper;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public Page<POSTerminalDto> findAll(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return terminalRepository.findByTenantId(tenantId, pageable)
                .map(terminalMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<POSTerminalDto> search(String query, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return terminalRepository.searchByTenantId(query, tenantId, pageable)
                .map(terminalMapper::toDto);
    }

    @Transactional(readOnly = true)
    public POSTerminalDto findById(Long id) {
        return terminalMapper.toDto(getTerminalById(id));
    }

    @Transactional(readOnly = true)
    public POSTerminalDto findByCode(String code) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return terminalRepository.findByTerminalCodeAndTenantId(code, tenantId)
                .map(terminalMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Terminal not found with code: " + code));
    }

    @Transactional(readOnly = true)
    public List<POSTerminalDto> findByLocation(Long locationId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return terminalMapper.toDtoList(
                terminalRepository.findByLocationIdAndTenantId(locationId, tenantId));
    }

    @Transactional(readOnly = true)
    public List<POSTerminalDto> findActiveTerminals() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return terminalMapper.toDtoList(
                terminalRepository.findActiveByTenantId(tenantId));
    }

    @Transactional
    public POSTerminalDto create(CreateTerminalRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        // Check for duplicate code
        if (terminalRepository.existsByTerminalCodeAndTenantId(request.getTerminalCode(), tenantId, null)) {
            throw new DuplicateResourceException("Terminal with code " + request.getTerminalCode() + " already exists");
        }

        // Get location
        Location location = locationRepository.findByIdAndTenantId(request.getLocationId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Location not found: " + request.getLocationId()));

        POSTerminal terminal = terminalMapper.toEntity(request);
        terminal.setTenantId(tenantId);
        terminal.setLocation(location);
        terminal.setActive(true);

        terminal = terminalRepository.save(terminal);
        log.info("Created POS terminal: {} at location {}", terminal.getTerminalCode(), location.getName());

        return terminalMapper.toDto(terminal);
    }

    @Transactional
    public POSTerminalDto update(Long id, CreateTerminalRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        POSTerminal terminal = getTerminalById(id);

        // Check for duplicate code (excluding current)
        if (terminalRepository.existsByTerminalCodeAndTenantId(request.getTerminalCode(), tenantId, id)) {
            throw new DuplicateResourceException("Terminal with code " + request.getTerminalCode() + " already exists");
        }

        // Get location if changed
        if (!terminal.getLocation().getId().equals(request.getLocationId())) {
            Location location = locationRepository.findByIdAndTenantId(request.getLocationId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Location not found: " + request.getLocationId()));
            terminal.setLocation(location);
        }

        terminalMapper.updateEntity(request, terminal);
        terminal = terminalRepository.save(terminal);

        log.info("Updated POS terminal: {}", terminal.getTerminalCode());
        return terminalMapper.toDto(terminal);
    }

    @Transactional
    public void activate(Long id) {
        POSTerminal terminal = getTerminalById(id);
        terminal.setActive(true);
        terminalRepository.save(terminal);
        log.info("Activated POS terminal: {}", terminal.getTerminalCode());
    }

    @Transactional
    public void deactivate(Long id) {
        POSTerminal terminal = getTerminalById(id);
        terminal.setActive(false);
        terminalRepository.save(terminal);
        log.info("Deactivated POS terminal: {}", terminal.getTerminalCode());
    }

    @Transactional
    public void updateLastSync(Long id) {
        POSTerminal terminal = getTerminalById(id);
        terminal.setLastSyncAt(Instant.now());
        terminalRepository.save(terminal);
    }

    @Transactional
    public void updateCurrentShift(Long terminalId, Long shiftId) {
        POSTerminal terminal = getTerminalById(terminalId);
        terminal.setCurrentShiftId(shiftId);
        terminalRepository.save(terminal);
    }

    @Transactional
    public void delete(Long id) {
        POSTerminal terminal = getTerminalById(id);

        // Check if there's an active shift
        if (terminal.getCurrentShiftId() != null) {
            throw new IllegalStateException("Cannot delete terminal with active shift");
        }

        terminalRepository.delete(terminal);
        log.info("Deleted POS terminal: {}", terminal.getTerminalCode());
    }

    private POSTerminal getTerminalById(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return terminalRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Terminal not found: " + id));
    }
}
