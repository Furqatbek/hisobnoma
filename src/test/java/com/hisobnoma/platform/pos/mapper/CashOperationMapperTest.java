package com.hisobnoma.platform.pos.mapper;

import com.hisobnoma.platform.pos.dto.CashOperationDto;
import com.hisobnoma.platform.pos.dto.CashOperationRequest;
import com.hisobnoma.platform.pos.entity.CashOperation;
import com.hisobnoma.platform.pos.entity.Shift;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class CashOperationMapperTest {

    @Autowired
    private CashOperationMapper cashOperationMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        Shift shift = Shift.builder().id(10L).build();

        CashOperation cashOperation = CashOperation.builder()
                .id(1L)
                .tenantId(1L)
                .shift(shift)
                .operationType(CashOperationRequest.OperationType.CASH_IN)
                .amount(new BigDecimal("100000.0000"))
                .reason("Petty cash deposit")
                .cashierId(5L)
                .cashierName("Jane Smith")
                .build();

        // When
        CashOperationDto dto = cashOperationMapper.toDto(cashOperation);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getShiftId());
        assertEquals(new BigDecimal("100000.0000"), dto.getAmount());
        assertEquals("Petty cash deposit", dto.getReason());
        assertEquals(5L, dto.getCashierId());
        assertEquals("Jane Smith", dto.getCashierName());
    }

    @Test
    void toDtoList_mapsAllOperations() {
        // Given
        Shift shift = Shift.builder().id(10L).build();

        CashOperation op1 = CashOperation.builder()
                .id(1L).shift(shift).operationType(CashOperationRequest.OperationType.CASH_IN)
                .amount(new BigDecimal("100000.0000")).cashierId(5L).build();
        CashOperation op2 = CashOperation.builder()
                .id(2L).shift(shift).operationType(CashOperationRequest.OperationType.CASH_OUT)
                .amount(new BigDecimal("50000.0000")).cashierId(5L).build();

        // When
        List<CashOperationDto> dtos = cashOperationMapper.toDtoList(List.of(op1, op2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
    }
}
