package com.hisobnoma.platform.pos.mapper;

import com.hisobnoma.platform.inventory.entity.Location;
import com.hisobnoma.platform.pos.dto.CreateTerminalRequest;
import com.hisobnoma.platform.pos.dto.POSTerminalDto;
import com.hisobnoma.platform.pos.entity.POSTerminal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class POSTerminalMapperTest {

    @Autowired
    private POSTerminalMapper posTerminalMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        Location location = Location.builder()
                .id(100L)
                .name("Main Store")
                .build();

        Instant lastSync = Instant.now();
        POSTerminal terminal = POSTerminal.builder()
                .id(1L)
                .tenantId(1L)
                .terminalCode("POS-001")
                .name("Register 1")
                .description("Main register")
                .location(location)
                .active(true)
                .ipAddress("192.168.1.100")
                .macAddress("AA:BB:CC:DD:EE:FF")
                .deviceType("Tablet")
                .printerName("Epson TM-T88")
                .cashDrawerEnabled(true)
                .allowOffline(false)
                .lastSyncAt(lastSync)
                .currentShiftId(5L)
                .build();

        // When
        POSTerminalDto dto = posTerminalMapper.toDto(terminal);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("POS-001", dto.getTerminalCode());
        assertEquals("Register 1", dto.getName());
        assertEquals("Main register", dto.getDescription());
        assertEquals(100L, dto.getLocationId());
        assertEquals("Main Store", dto.getLocationName());
        assertTrue(dto.isActive());
        assertEquals("192.168.1.100", dto.getIpAddress());
        assertEquals("AA:BB:CC:DD:EE:FF", dto.getMacAddress());
        assertEquals("Tablet", dto.getDeviceType());
        assertEquals("Epson TM-T88", dto.getPrinterName());
        assertTrue(dto.isCashDrawerEnabled());
        assertFalse(dto.isAllowOffline());
        assertEquals(lastSync, dto.getLastSyncAt());
        assertEquals(5L, dto.getCurrentShiftId());
    }

    @Test
    void toEntity_fromCreateRequest_mapsBasicFields() {
        // Given
        CreateTerminalRequest request = CreateTerminalRequest.builder()
                .terminalCode("POS-002")
                .name("Register 2")
                .description("Second register")
                .locationId(100L)
                .ipAddress("192.168.1.101")
                .macAddress("11:22:33:44:55:66")
                .deviceType("Desktop")
                .printerName("Star TSP100")
                .cashDrawerEnabled(true)
                .allowOffline(true)
                .build();

        // When
        POSTerminal entity = posTerminalMapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertEquals("POS-002", entity.getTerminalCode());
        assertEquals("Register 2", entity.getName());
        assertEquals("Second register", entity.getDescription());
        assertEquals("192.168.1.101", entity.getIpAddress());
        assertEquals("11:22:33:44:55:66", entity.getMacAddress());
        assertEquals("Desktop", entity.getDeviceType());
        assertEquals("Star TSP100", entity.getPrinterName());
        assertTrue(entity.isCashDrawerEnabled());
        assertTrue(entity.isAllowOffline());
        assertTrue(entity.isActive()); // constant = "true"
        // Ignored fields
        assertNull(entity.getId());
        assertNull(entity.getTenantId());
        assertNull(entity.getLocation());
    }

    @Test
    void updateEntity_updatesFieldsOnExisting() {
        // Given
        POSTerminal existing = POSTerminal.builder()
                .id(1L)
                .tenantId(1L)
                .terminalCode("POS-001")
                .name("Old Name")
                .description("Old description")
                .active(true)
                .build();

        CreateTerminalRequest request = CreateTerminalRequest.builder()
                .terminalCode("POS-001")
                .name("New Name")
                .description("New description")
                .ipAddress("10.0.0.1")
                .build();

        // When
        posTerminalMapper.updateEntity(request, existing);

        // Then
        assertEquals("New Name", existing.getName());
        assertEquals("New description", existing.getDescription());
        assertEquals("10.0.0.1", existing.getIpAddress());
        // ID and tenantId should remain unchanged
        assertEquals(1L, existing.getId());
        assertEquals(1L, existing.getTenantId());
    }

    @Test
    void toDtoList_mapsAllTerminals() {
        // Given
        Location location = Location.builder().id(100L).name("Store").build();

        POSTerminal t1 = POSTerminal.builder()
                .id(1L).terminalCode("POS-001").name("Reg 1").location(location).build();
        POSTerminal t2 = POSTerminal.builder()
                .id(2L).terminalCode("POS-002").name("Reg 2").location(location).build();

        // When
        List<POSTerminalDto> dtos = posTerminalMapper.toDtoList(List.of(t1, t2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals("POS-001", dtos.get(0).getTerminalCode());
        assertEquals("POS-002", dtos.get(1).getTerminalCode());
    }
}
