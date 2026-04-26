package com.hisobnoma.platform.pos.mapper;

import com.hisobnoma.platform.pos.dto.ShiftDto;
import com.hisobnoma.platform.pos.entity.POSTerminal;
import com.hisobnoma.platform.pos.entity.Shift;
import com.hisobnoma.platform.pos.entity.ShiftStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ShiftMapperTest {

    @Autowired
    private ShiftMapper shiftMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        POSTerminal terminal = POSTerminal.builder()
                .id(10L)
                .terminalCode("POS-001")
                .name("Register 1")
                .build();

        Instant openedAt = Instant.now().minusSeconds(3600);
        Instant closedAt = Instant.now();

        Shift shift = Shift.builder()
                .id(1L)
                .tenantId(1L)
                .shiftNumber("SH-2024-001")
                .terminal(terminal)
                .cashierId(5L)
                .cashierName("Jane Smith")
                .status(ShiftStatus.CLOSED)
                .openedAt(openedAt)
                .closedAt(closedAt)
                .openingCash(new BigDecimal("500000.0000"))
                .closingCash(new BigDecimal("1200000.0000"))
                .expectedCash(new BigDecimal("1150000.0000"))
                .cashDifference(new BigDecimal("50000.0000"))
                .totalSales(new BigDecimal("800000.0000"))
                .totalReturns(new BigDecimal("50000.0000"))
                .totalDiscounts(new BigDecimal("30000.0000"))
                .totalTaxes(new BigDecimal("96000.0000"))
                .cashPayments(new BigDecimal("600000.0000"))
                .cardPayments(new BigDecimal("200000.0000"))
                .otherPayments(BigDecimal.ZERO)
                .cashRefunds(new BigDecimal("10000.0000"))
                .cardRefunds(BigDecimal.ZERO)
                .otherRefunds(BigDecimal.ZERO)
                .transactionCount(15)
                .voidedCount(1)
                .returnCount(2)
                .cashIn(new BigDecimal("100000.0000"))
                .cashOut(new BigDecimal("50000.0000"))
                .notes("Shift notes")
                .closingNotes("Shift closed normally")
                .build();

        // When
        ShiftDto dto = shiftMapper.toDto(shift);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("SH-2024-001", dto.getShiftNumber());
        assertEquals(10L, dto.getTerminalId());
        assertEquals("POS-001", dto.getTerminalCode());
        assertEquals("Register 1", dto.getTerminalName());
        assertEquals(5L, dto.getCashierId());
        assertEquals("Jane Smith", dto.getCashierName());
        assertEquals(ShiftStatus.CLOSED, dto.getStatus());
        assertEquals(openedAt, dto.getOpenedAt());
        assertEquals(closedAt, dto.getClosedAt());
        assertEquals(new BigDecimal("500000.0000"), dto.getOpeningCash());
        assertEquals(new BigDecimal("1200000.0000"), dto.getClosingCash());
        assertEquals(new BigDecimal("1150000.0000"), dto.getExpectedCash());
        assertEquals(new BigDecimal("50000.0000"), dto.getCashDifference());
        assertEquals(new BigDecimal("800000.0000"), dto.getTotalSales());
        assertEquals(new BigDecimal("50000.0000"), dto.getTotalReturns());
        assertEquals(new BigDecimal("30000.0000"), dto.getTotalDiscounts());
        assertEquals(new BigDecimal("96000.0000"), dto.getTotalTaxes());
        assertEquals(new BigDecimal("600000.0000"), dto.getCashPayments());
        assertEquals(new BigDecimal("200000.0000"), dto.getCardPayments());
        assertEquals(15, dto.getTransactionCount());
        assertEquals(1, dto.getVoidedCount());
        assertEquals(2, dto.getReturnCount());
        assertEquals(new BigDecimal("100000.0000"), dto.getCashIn());
        assertEquals(new BigDecimal("50000.0000"), dto.getCashOut());
        assertEquals("Shift notes", dto.getNotes());
        assertEquals("Shift closed normally", dto.getClosingNotes());
    }

    @Test
    void toDtoList_mapsAllShifts() {
        // Given
        POSTerminal terminal = POSTerminal.builder()
                .id(10L).terminalCode("POS-001").name("Reg 1").build();

        Shift s1 = Shift.builder()
                .id(1L).shiftNumber("SH-001").terminal(terminal)
                .cashierId(5L).openedAt(Instant.now())
                .openingCash(BigDecimal.ZERO).build();
        Shift s2 = Shift.builder()
                .id(2L).shiftNumber("SH-002").terminal(terminal)
                .cashierId(6L).openedAt(Instant.now())
                .openingCash(BigDecimal.ZERO).build();

        // When
        List<ShiftDto> dtos = shiftMapper.toDtoList(List.of(s1, s2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals("SH-001", dtos.get(0).getShiftNumber());
        assertEquals("SH-002", dtos.get(1).getShiftNumber());
    }
}
