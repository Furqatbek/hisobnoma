package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.APPaymentDto;
import com.hisobnoma.platform.finance.dto.CreateAPPaymentRequest;
import com.hisobnoma.platform.finance.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class APPaymentMapperTest {

    private APPaymentMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(APPaymentMapper.class);
        APPaymentAllocationMapper allocationMapper = Mappers.getMapper(APPaymentAllocationMapper.class);
        ReflectionTestUtils.setField(mapper, "aPPaymentAllocationMapper", allocationMapper);
    }

    // ========== toDto ==========

    @Test
    void toDto_mapsAllFields() {
        // Given
        APPayment payment = APPayment.builder()
                .paymentNumber("PAY-000001")
                .vendorId(1L)
                .vendorName("Test Vendor")
                .paymentDate(LocalDate.of(2025, 3, 15))
                .status(APPaymentStatus.COMPLETED)
                .paymentMethod(APPaymentMethod.BANK_TRANSFER)
                .paymentAmount(new BigDecimal("1000.0000"))
                .allocatedAmount(new BigDecimal("800.0000"))
                .unallocatedAmount(new BigDecimal("200.0000"))
                .currency("UZS")
                .exchangeRate(BigDecimal.ONE)
                .bankAccountId(5L)
                .bankAccountName("Main Bank Account")
                .cashAccountId(10L)
                .apAccountId(20L)
                .referenceNumber("REF-12345")
                .checkNumber("CHK-001")
                .checkDate(LocalDate.of(2025, 3, 15))
                .memo("Payment for AP-000001")
                .notes("Some notes")
                .glPosted(true)
                .glJournalEntryId(100L)
                .glPostedAt(Instant.parse("2025-03-15T10:00:00Z"))
                .reconciled(false)
                .allocations(new ArrayList<>())
                .build();
        payment.setId(1L);
        payment.setTenantId(1L);

        // When
        APPaymentDto dto = mapper.toDto(payment);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(1L, dto.getTenantId());
        assertEquals("PAY-000001", dto.getPaymentNumber());
        assertEquals(1L, dto.getVendorId());
        assertEquals("Test Vendor", dto.getVendorName());
        assertEquals(LocalDate.of(2025, 3, 15), dto.getPaymentDate());
        assertEquals(APPaymentStatus.COMPLETED, dto.getStatus());
        assertEquals(APPaymentMethod.BANK_TRANSFER, dto.getPaymentMethod());
        assertEquals(new BigDecimal("1000.0000"), dto.getPaymentAmount());
        assertEquals(new BigDecimal("800.0000"), dto.getAllocatedAmount());
        assertEquals(new BigDecimal("200.0000"), dto.getUnallocatedAmount());
        assertEquals("UZS", dto.getCurrency());
        assertEquals(BigDecimal.ONE, dto.getExchangeRate());
        assertEquals(5L, dto.getBankAccountId());
        assertEquals("Main Bank Account", dto.getBankAccountName());
        assertEquals(10L, dto.getCashAccountId());
        assertEquals(20L, dto.getApAccountId());
        assertEquals("REF-12345", dto.getReferenceNumber());
        assertEquals("CHK-001", dto.getCheckNumber());
        assertEquals(LocalDate.of(2025, 3, 15), dto.getCheckDate());
        assertEquals("Payment for AP-000001", dto.getMemo());
        assertEquals("Some notes", dto.getNotes());
        assertTrue(dto.isGlPosted());
        assertEquals(100L, dto.getGlJournalEntryId());
        assertFalse(dto.isReconciled());
        // fullyAllocated and availableAmount are calculated
        assertFalse(dto.isFullyAllocated());
        assertEquals(new BigDecimal("200.0000"), dto.getAvailableAmount());
    }

    @Test
    void toDto_mapsFullyAllocatedPayment() {
        // Given
        APPayment payment = APPayment.builder()
                .paymentNumber("PAY-000002")
                .paymentDate(LocalDate.now())
                .status(APPaymentStatus.COMPLETED)
                .paymentMethod(APPaymentMethod.CHECK)
                .paymentAmount(new BigDecimal("500.0000"))
                .allocatedAmount(new BigDecimal("500.0000"))
                .unallocatedAmount(BigDecimal.ZERO)
                .currency("UZS")
                .exchangeRate(BigDecimal.ONE)
                .glPosted(false)
                .reconciled(false)
                .allocations(new ArrayList<>())
                .build();
        payment.setId(2L);

        // When
        APPaymentDto dto = mapper.toDto(payment);

        // Then
        assertNotNull(dto);
        assertTrue(dto.isFullyAllocated());
        assertEquals(BigDecimal.ZERO, dto.getAvailableAmount());
    }

    // ========== toDtoWithoutAllocations ==========

    @Test
    void toDtoWithoutAllocations_excludesAllocations() {
        // Given
        APPaymentAllocation allocation = APPaymentAllocation.builder()
                .allocatedAmount(new BigDecimal("500.0000"))
                .discountTaken(BigDecimal.ZERO)
                .writeOffAmount(BigDecimal.ZERO)
                .build();

        APPayment payment = APPayment.builder()
                .paymentNumber("PAY-000003")
                .vendorId(1L)
                .vendorName("Test Vendor")
                .paymentDate(LocalDate.now())
                .status(APPaymentStatus.DRAFT)
                .paymentMethod(APPaymentMethod.BANK_TRANSFER)
                .paymentAmount(new BigDecimal("500.0000"))
                .allocatedAmount(new BigDecimal("500.0000"))
                .unallocatedAmount(BigDecimal.ZERO)
                .currency("UZS")
                .exchangeRate(BigDecimal.ONE)
                .glPosted(false)
                .reconciled(false)
                .allocations(new ArrayList<>(List.of(allocation)))
                .build();
        payment.setId(3L);

        // When
        APPaymentDto dto = mapper.toDtoWithoutAllocations(payment);

        // Then
        assertNotNull(dto);
        assertEquals(3L, dto.getId());
        assertEquals("PAY-000003", dto.getPaymentNumber());
        assertNull(dto.getAllocations());
    }

    // ========== toEntity ==========

    @Test
    void toEntity_fromCreateRequest() {
        // Given
        CreateAPPaymentRequest request = CreateAPPaymentRequest.builder()
                .vendorId(1L)
                .paymentDate(LocalDate.of(2025, 3, 15))
                .paymentMethod(APPaymentMethod.BANK_TRANSFER)
                .paymentAmount(new BigDecimal("1000.0000"))
                .currency("USD")
                .exchangeRate(new BigDecimal("12500.0000"))
                .bankAccountId(5L)
                .cashAccountId(10L)
                .apAccountId(20L)
                .referenceNumber("REF-12345")
                .checkNumber("CHK-001")
                .checkDate(LocalDate.of(2025, 3, 15))
                .memo("Test payment memo")
                .notes("Test notes")
                .allocations(new ArrayList<>())
                .build();

        // When
        APPayment entity = mapper.toEntity(request);

        // Then
        assertNotNull(entity);
        // ignored fields should be null/default
        assertNull(entity.getId());
        assertNull(entity.getTenantId());
        assertNull(entity.getPaymentNumber());
        assertNull(entity.getVendorName());
        // mapped fields
        assertEquals(1L, entity.getVendorId());
        assertEquals(LocalDate.of(2025, 3, 15), entity.getPaymentDate());
        assertEquals(APPaymentMethod.BANK_TRANSFER, entity.getPaymentMethod());
        assertEquals(new BigDecimal("1000.0000"), entity.getPaymentAmount());
        assertEquals("USD", entity.getCurrency());
        assertEquals(new BigDecimal("12500.0000"), entity.getExchangeRate());
        assertEquals(5L, entity.getBankAccountId());
        assertEquals(10L, entity.getCashAccountId());
        assertEquals(20L, entity.getApAccountId());
        assertEquals("REF-12345", entity.getReferenceNumber());
        assertEquals("CHK-001", entity.getCheckNumber());
        assertEquals(LocalDate.of(2025, 3, 15), entity.getCheckDate());
        assertEquals("Test payment memo", entity.getMemo());
        assertEquals("Test notes", entity.getNotes());
    }

    // ========== toDtoList ==========

    @Test
    void toDtoList_mapsMultiplePayments() {
        // Given
        APPayment payment1 = APPayment.builder()
                .paymentNumber("PAY-000001")
                .paymentDate(LocalDate.now())
                .status(APPaymentStatus.DRAFT)
                .paymentMethod(APPaymentMethod.CASH)
                .paymentAmount(new BigDecimal("100.0000"))
                .allocatedAmount(new BigDecimal("100.0000"))
                .unallocatedAmount(BigDecimal.ZERO)
                .currency("UZS")
                .exchangeRate(BigDecimal.ONE)
                .glPosted(false)
                .reconciled(false)
                .allocations(new ArrayList<>())
                .build();
        payment1.setId(1L);

        APPayment payment2 = APPayment.builder()
                .paymentNumber("PAY-000002")
                .paymentDate(LocalDate.now())
                .status(APPaymentStatus.COMPLETED)
                .paymentMethod(APPaymentMethod.BANK_TRANSFER)
                .paymentAmount(new BigDecimal("200.0000"))
                .allocatedAmount(new BigDecimal("200.0000"))
                .unallocatedAmount(BigDecimal.ZERO)
                .currency("UZS")
                .exchangeRate(BigDecimal.ONE)
                .glPosted(false)
                .reconciled(false)
                .allocations(new ArrayList<>())
                .build();
        payment2.setId(2L);

        // When
        List<APPaymentDto> dtos = mapper.toDtoList(List.of(payment1, payment2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals("PAY-000001", dtos.get(0).getPaymentNumber());
        assertEquals("PAY-000002", dtos.get(1).getPaymentNumber());
        // toDtoList uses toDtoWithoutAllocations, so allocations should be null
        assertNull(dtos.get(0).getAllocations());
        assertNull(dtos.get(1).getAllocations());
    }
}
