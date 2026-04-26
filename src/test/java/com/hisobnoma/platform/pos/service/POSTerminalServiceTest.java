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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class POSTerminalServiceTest {

    @Mock
    private POSTerminalRepository terminalRepository;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private POSTerminalMapper terminalMapper;
    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private POSTerminalService terminalService;

    private POSTerminal terminal;
    private Location location;
    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        location = Location.builder()
                .id(1L)
                .name("Main Store")
                .build();

        terminal = POSTerminal.builder()
                .id(1L)
                .terminalCode("TERM-001")
                .name("Terminal 1")
                .location(location)
                .tenantId(TENANT_ID)
                .active(true)
                .build();
    }

    // ==================== findAll ====================

    @Test
    void getTerminals_returnsPaged() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<POSTerminal> page = new PageImpl<>(List.of(terminal), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(terminalRepository.findByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(terminalMapper.toDto(any())).thenReturn(
                POSTerminalDto.builder().id(1L).terminalCode("TERM-001").build());

        // When
        Page<POSTerminalDto> result = terminalService.findAll(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    // ==================== findActiveTerminals ====================

    @Test
    void getActiveTerminals_returnsOnlyActive() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(terminalRepository.findActiveByTenantId(TENANT_ID)).thenReturn(List.of(terminal));
        when(terminalMapper.toDtoList(any())).thenReturn(
                List.of(POSTerminalDto.builder().id(1L).active(true).build()));

        // When
        List<POSTerminalDto> result = terminalService.findActiveTerminals();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ==================== findById ====================

    @Test
    void getTerminal_found_returnsDto() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(terminalRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(terminal));
        when(terminalMapper.toDto(terminal)).thenReturn(
                POSTerminalDto.builder().id(1L).terminalCode("TERM-001").build());

        // When
        POSTerminalDto result = terminalService.findById(1L);

        // Then
        assertNotNull(result);
        assertEquals("TERM-001", result.getTerminalCode());
    }

    @Test
    void getTerminal_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(terminalRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> terminalService.findById(999L));
    }

    // ==================== create ====================

    @Test
    void createTerminal_success() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(terminalRepository.existsByTerminalCodeAndTenantId("TERM-002", TENANT_ID, null)).thenReturn(false);
        when(locationRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(location));
        when(terminalMapper.toEntity(any())).thenReturn(POSTerminal.builder()
                .terminalCode("TERM-002").name("Terminal 2").build());
        when(terminalRepository.save(any())).thenAnswer(inv -> {
            POSTerminal saved = inv.getArgument(0);
            saved.setId(2L);
            return saved;
        });
        when(terminalMapper.toDto(any())).thenReturn(
                POSTerminalDto.builder().id(2L).terminalCode("TERM-002").build());

        CreateTerminalRequest request = CreateTerminalRequest.builder()
                .terminalCode("TERM-002")
                .name("Terminal 2")
                .locationId(1L)
                .build();

        // When
        POSTerminalDto result = terminalService.create(request);

        // Then
        assertNotNull(result);
        assertEquals("TERM-002", result.getTerminalCode());
        verify(terminalRepository).save(any());
    }

    @Test
    void createTerminal_duplicateCode_throwsDuplicateResourceException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(terminalRepository.existsByTerminalCodeAndTenantId("TERM-001", TENANT_ID, null)).thenReturn(true);

        CreateTerminalRequest request = CreateTerminalRequest.builder()
                .terminalCode("TERM-001")
                .name("Terminal Dup")
                .locationId(1L)
                .build();

        // When/Then
        assertThrows(DuplicateResourceException.class, () -> terminalService.create(request));
        verify(terminalRepository, never()).save(any());
    }

    // ==================== update ====================

    @Test
    void updateTerminal_success() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(terminalRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(terminal));
        when(terminalRepository.existsByTerminalCodeAndTenantId("TERM-001-UPD", TENANT_ID, 1L)).thenReturn(false);
        when(terminalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(terminalMapper.toDto(any())).thenReturn(
                POSTerminalDto.builder().id(1L).terminalCode("TERM-001-UPD").build());

        CreateTerminalRequest request = CreateTerminalRequest.builder()
                .terminalCode("TERM-001-UPD")
                .name("Terminal 1 Updated")
                .locationId(1L)
                .build();

        // When
        POSTerminalDto result = terminalService.update(1L, request);

        // Then
        assertNotNull(result);
        verify(terminalRepository).save(any());
    }

    @Test
    void updateTerminal_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(terminalRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        CreateTerminalRequest request = CreateTerminalRequest.builder()
                .terminalCode("TERM-999")
                .name("Not found")
                .locationId(1L)
                .build();

        // When/Then
        assertThrows(NotFoundException.class, () -> terminalService.update(999L, request));
    }

    // ==================== activate ====================

    @Test
    void activateTerminal_inactive_becomesActive() {
        // Given
        terminal.setActive(false);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(terminalRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(terminal));
        when(terminalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        terminalService.activate(1L);

        // Then
        assertTrue(terminal.isActive());
        verify(terminalRepository).save(terminal);
    }

    @Test
    void activateTerminal_alreadyActive_idempotent() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(terminalRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(terminal));
        when(terminalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        terminalService.activate(1L);

        // Then
        assertTrue(terminal.isActive());
        verify(terminalRepository).save(terminal);
    }

    @Test
    void activateTerminal_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(terminalRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> terminalService.activate(999L));
    }

    // ==================== deactivate ====================

    @Test
    void deactivateTerminal_active_becomesInactive() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(terminalRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(terminal));
        when(terminalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        terminalService.deactivate(1L);

        // Then
        assertFalse(terminal.isActive());
        verify(terminalRepository).save(terminal);
    }

    @Test
    void deactivateTerminal_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(terminalRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> terminalService.deactivate(999L));
    }

    // ==================== findByCode ====================

    @Test
    void findByCode_found_returnsDto() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(terminalRepository.findByTerminalCodeAndTenantId("TERM-001", TENANT_ID))
                .thenReturn(Optional.of(terminal));
        when(terminalMapper.toDto(terminal)).thenReturn(
                POSTerminalDto.builder().id(1L).terminalCode("TERM-001").build());

        // When
        POSTerminalDto result = terminalService.findByCode("TERM-001");

        // Then
        assertNotNull(result);
        assertEquals("TERM-001", result.getTerminalCode());
    }

    @Test
    void findByCode_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(terminalRepository.findByTerminalCodeAndTenantId("UNKNOWN", TENANT_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> terminalService.findByCode("UNKNOWN"));
    }

    // ==================== findByLocation ====================

    @Test
    void findByLocation_returnsTerminals() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(terminalRepository.findByLocationIdAndTenantId(1L, TENANT_ID)).thenReturn(List.of(terminal));
        when(terminalMapper.toDtoList(any())).thenReturn(
                List.of(POSTerminalDto.builder().id(1L).build()));

        // When
        List<POSTerminalDto> result = terminalService.findByLocation(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ==================== delete ====================

    @Test
    void delete_noActiveShift_deletesSuccessfully() {
        // Given
        terminal.setCurrentShiftId(null);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(terminalRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(terminal));

        // When
        terminalService.delete(1L);

        // Then
        verify(terminalRepository).delete(terminal);
    }

    @Test
    void delete_hasActiveShift_throwsIllegalStateException() {
        // Given
        terminal.setCurrentShiftId(99L);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(terminalRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(terminal));

        // When/Then
        assertThrows(IllegalStateException.class, () -> terminalService.delete(1L));
        verify(terminalRepository, never()).delete(any());
    }
}
