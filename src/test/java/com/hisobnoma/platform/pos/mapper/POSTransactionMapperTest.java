package com.hisobnoma.platform.pos.mapper;

import com.hisobnoma.platform.finance.entity.Customer;
import com.hisobnoma.platform.inventory.entity.Location;
import com.hisobnoma.platform.pos.dto.POSTransactionDto;
import com.hisobnoma.platform.pos.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class POSTransactionMapperTest {

    @Autowired
    private POSTransactionMapper posTransactionMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        Location location = Location.builder()
                .id(100L)
                .name("Main Store")
                .build();

        POSTerminal terminal = POSTerminal.builder()
                .id(10L)
                .terminalCode("POS-001")
                .name("Register 1")
                .location(location)
                .build();

        Shift shift = Shift.builder()
                .id(20L)
                .shiftNumber("SH-2024-001")
                .build();

        Customer customer = Customer.builder()
                .id(30L)
                .build();

        POSTransaction transaction = POSTransaction.builder()
                .id(1L)
                .tenantId(1L)
                .transactionNumber("TXN-001")
                .terminal(terminal)
                .shift(shift)
                .customer(customer)
                .customerName("John Doe")
                .customerPhone("+998901234567")
                .cashierId(5L)
                .cashierName("Jane Smith")
                .transactionType(TransactionType.SALE)
                .status(TransactionStatus.COMPLETED)
                .subtotal(new BigDecimal("100000.0000"))
                .discountAmount(new BigDecimal("5000.0000"))
                .discountPercent(new BigDecimal("5.00"))
                .discountReason("Loyal customer")
                .taxAmount(new BigDecimal("12000.0000"))
                .totalAmount(new BigDecimal("95000.0000"))
                .paidAmount(new BigDecimal("100000.0000"))
                .changeAmount(new BigDecimal("5000.0000"))
                .currency("UZS")
                .itemCount(3)
                .lineCount(2)
                .completedAt(Instant.now())
                .completedBy(5L)
                .glPosted(true)
                .stockDeducted(true)
                .deliveryRegionId(50L)
                .deliveryRegionName("Tashkent")
                .deliveryVillageId(51L)
                .deliveryVillageName("Chilanzar")
                .notes("Test transaction")
                .lines(new ArrayList<>())
                .payments(new ArrayList<>())
                .build();

        // When
        POSTransactionDto dto = posTransactionMapper.toDto(transaction);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("TXN-001", dto.getTransactionNumber());
        assertEquals(10L, dto.getTerminalId());
        assertEquals("POS-001", dto.getTerminalCode());
        assertEquals("Register 1", dto.getTerminalName());
        assertEquals(100L, dto.getLocationId());
        assertEquals("Main Store", dto.getLocationName());
        assertEquals(20L, dto.getShiftId());
        assertEquals("SH-2024-001", dto.getShiftNumber());
        assertEquals(30L, dto.getCustomerId());
        assertEquals("John Doe", dto.getCustomerName());
        assertEquals("+998901234567", dto.getCustomerPhone());
        assertEquals(5L, dto.getCashierId());
        assertEquals("Jane Smith", dto.getCashierName());
        assertEquals(TransactionType.SALE, dto.getTransactionType());
        assertEquals(TransactionStatus.COMPLETED, dto.getStatus());
        assertEquals(new BigDecimal("100000.0000"), dto.getSubtotal());
        assertEquals(new BigDecimal("5000.0000"), dto.getDiscountAmount());
        assertEquals(new BigDecimal("5.00"), dto.getDiscountPercent());
        assertEquals("Loyal customer", dto.getDiscountReason());
        assertEquals(new BigDecimal("12000.0000"), dto.getTaxAmount());
        assertEquals(new BigDecimal("95000.0000"), dto.getTotalAmount());
        assertEquals(new BigDecimal("100000.0000"), dto.getPaidAmount());
        assertEquals(new BigDecimal("5000.0000"), dto.getChangeAmount());
        assertEquals("UZS", dto.getCurrency());
        assertEquals(3, dto.getItemCount());
        assertEquals(2, dto.getLineCount());
        assertTrue(dto.isGlPosted());
        assertTrue(dto.isStockDeducted());
        assertEquals(50L, dto.getDeliveryRegionId());
        assertEquals("Tashkent", dto.getDeliveryRegionName());
        assertEquals(51L, dto.getDeliveryVillageId());
        assertEquals("Chilanzar", dto.getDeliveryVillageName());
        assertEquals("Test transaction", dto.getNotes());
        // balanceDue is computed via expression
        assertNotNull(dto.getBalanceDue());
    }

    @Test
    void toDtoWithoutDetails_doesNotMapLines() {
        // Given
        Location location = Location.builder()
                .id(100L)
                .name("Main Store")
                .build();

        POSTerminal terminal = POSTerminal.builder()
                .id(10L)
                .terminalCode("POS-001")
                .name("Register 1")
                .location(location)
                .build();

        Shift shift = Shift.builder()
                .id(20L)
                .shiftNumber("SH-2024-001")
                .build();

        POSTransaction transaction = POSTransaction.builder()
                .id(1L)
                .transactionNumber("TXN-001")
                .terminal(terminal)
                .shift(shift)
                .transactionType(TransactionType.SALE)
                .status(TransactionStatus.COMPLETED)
                .subtotal(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .paidAmount(BigDecimal.ZERO)
                .lines(new ArrayList<>())
                .payments(new ArrayList<>())
                .build();

        // When
        POSTransactionDto dto = posTransactionMapper.toDtoWithoutDetails(transaction);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertNull(dto.getLines());
    }

    @Test
    void toDtoList_mapsAllTransactions() {
        // Given
        Location location = Location.builder()
                .id(100L)
                .name("Main Store")
                .build();

        POSTerminal terminal = POSTerminal.builder()
                .id(10L)
                .terminalCode("POS-001")
                .name("Register 1")
                .location(location)
                .build();

        Shift shift = Shift.builder()
                .id(20L)
                .shiftNumber("SH-001")
                .build();

        POSTransaction tx1 = POSTransaction.builder()
                .id(1L)
                .transactionNumber("TXN-001")
                .terminal(terminal)
                .shift(shift)
                .transactionType(TransactionType.SALE)
                .status(TransactionStatus.COMPLETED)
                .subtotal(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .paidAmount(BigDecimal.ZERO)
                .lines(new ArrayList<>())
                .payments(new ArrayList<>())
                .build();

        POSTransaction tx2 = POSTransaction.builder()
                .id(2L)
                .transactionNumber("TXN-002")
                .terminal(terminal)
                .shift(shift)
                .transactionType(TransactionType.RETURN)
                .status(TransactionStatus.COMPLETED)
                .subtotal(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .paidAmount(BigDecimal.ZERO)
                .lines(new ArrayList<>())
                .payments(new ArrayList<>())
                .build();

        // When
        List<POSTransactionDto> dtos = posTransactionMapper.toDtoList(List.of(tx1, tx2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals("TXN-001", dtos.get(0).getTransactionNumber());
        assertEquals("TXN-002", dtos.get(1).getTransactionNumber());
        // toDtoList uses toDtoWithoutDetails, so lines should be null
        assertNull(dtos.get(0).getLines());
    }

    @Test
    void toDto_withNullCustomer_mapsCustomerIdToNull() {
        // Given
        Location location = Location.builder()
                .id(100L)
                .name("Main Store")
                .build();

        POSTerminal terminal = POSTerminal.builder()
                .id(10L)
                .terminalCode("POS-001")
                .name("Register 1")
                .location(location)
                .build();

        Shift shift = Shift.builder()
                .id(20L)
                .shiftNumber("SH-001")
                .build();

        POSTransaction transaction = POSTransaction.builder()
                .id(1L)
                .transactionNumber("TXN-001")
                .terminal(terminal)
                .shift(shift)
                .customer(null)
                .transactionType(TransactionType.SALE)
                .status(TransactionStatus.PENDING)
                .subtotal(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .paidAmount(BigDecimal.ZERO)
                .lines(new ArrayList<>())
                .payments(new ArrayList<>())
                .build();

        // When
        POSTransactionDto dto = posTransactionMapper.toDto(transaction);

        // Then
        assertNotNull(dto);
        assertNull(dto.getCustomerId());
    }
}
