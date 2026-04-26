package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.ARPaymentDto;
import com.hisobnoma.platform.finance.dto.CreateARPaymentRequest;
import com.hisobnoma.platform.finance.entity.ARPayment;
import com.hisobnoma.platform.finance.entity.ARPaymentMethod;
import com.hisobnoma.platform.finance.entity.ARPaymentStatus;
import com.hisobnoma.platform.finance.entity.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class ARPaymentMapperTest {

    private ARPaymentMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(ARPaymentMapper.class);
        // The ARPaymentMapperImpl has an @Autowired ARPaymentAllocationMapper dependency;
        // inject it manually for unit testing.
        ARPaymentAllocationMapper allocationMapper = Mappers.getMapper(ARPaymentAllocationMapper.class);
        ReflectionTestUtils.setField(mapper, "aRPaymentAllocationMapper", allocationMapper);
    }

    @Test
    void toDto_mapsAllFields() {
        // Given
        Customer customer = Customer.builder()
                .id(1L)
                .code("CUST-001")
                .name("Test Customer")
                .build();

        ARPayment payment = ARPayment.builder()
                .paymentNumber("REC-000001")
                .customer(customer)
                .customerName("Test Customer")
                .paymentDate(LocalDate.of(2025, 3, 15))
                .status(ARPaymentStatus.COMPLETED)
                .paymentMethod(ARPaymentMethod.BANK_TRANSFER)
                .paymentAmount(new BigDecimal("1000.00"))
                .allocatedAmount(new BigDecimal("800.00"))
                .unallocatedAmount(new BigDecimal("200.00"))
                .currency("UZS")
                .referenceNumber("REF-12345")
                .memo("Payment for INV-000001")
                .notes("Some notes")
                .deposited(false)
                .allocations(new ArrayList<>())
                .build();
        payment.setId(1L);
        payment.setTenantId(1L);

        // When
        ARPaymentDto dto = mapper.toDto(payment);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("REC-000001", dto.getPaymentNumber());
        assertEquals("Test Customer", dto.getCustomerName());
        assertEquals(LocalDate.of(2025, 3, 15), dto.getPaymentDate());
        assertEquals(ARPaymentStatus.COMPLETED, dto.getStatus());
        assertEquals(ARPaymentMethod.BANK_TRANSFER, dto.getPaymentMethod());
        assertEquals(new BigDecimal("1000.00"), dto.getPaymentAmount());
        assertEquals(new BigDecimal("800.00"), dto.getAllocatedAmount());
        assertEquals(new BigDecimal("200.00"), dto.getUnallocatedAmount());
        assertEquals("UZS", dto.getCurrency());
        assertEquals("REF-12345", dto.getReferenceNumber());
        assertEquals("Payment for INV-000001", dto.getMemo());
        assertFalse(dto.isDeposited());
    }

    @Test
    void toDto_mapsComputedFieldsCorrectly() {
        // Given - fully allocated payment
        ARPayment fullyAllocated = ARPayment.builder()
                .paymentNumber("REC-000002")
                .paymentDate(LocalDate.now())
                .status(ARPaymentStatus.COMPLETED)
                .paymentMethod(ARPaymentMethod.CASH)
                .paymentAmount(new BigDecimal("500.00"))
                .allocatedAmount(new BigDecimal("500.00"))
                .unallocatedAmount(BigDecimal.ZERO)
                .allocations(new ArrayList<>())
                .build();
        fullyAllocated.setId(2L);

        // When
        ARPaymentDto dto = mapper.toDto(fullyAllocated);

        // Then
        assertNotNull(dto);
        assertTrue(dto.isFullyAllocated());
        assertEquals(BigDecimal.ZERO, dto.getAvailableAmount());
    }

    @Test
    void toDto_mapsPartiallyAllocatedPayment() {
        // Given
        ARPayment partiallyAllocated = ARPayment.builder()
                .paymentNumber("REC-000003")
                .paymentDate(LocalDate.now())
                .status(ARPaymentStatus.PENDING)
                .paymentMethod(ARPaymentMethod.CHECK)
                .paymentAmount(new BigDecimal("1000.00"))
                .allocatedAmount(new BigDecimal("600.00"))
                .unallocatedAmount(new BigDecimal("400.00"))
                .checkNumber("CHK-001")
                .checkDate(LocalDate.now())
                .allocations(new ArrayList<>())
                .build();
        partiallyAllocated.setId(3L);

        // When
        ARPaymentDto dto = mapper.toDto(partiallyAllocated);

        // Then
        assertNotNull(dto);
        assertFalse(dto.isFullyAllocated());
        assertEquals(new BigDecimal("400.00"), dto.getAvailableAmount());
        assertEquals("CHK-001", dto.getCheckNumber());
    }

    @Test
    void toEntity_mapsRequestFields() {
        // Given
        CreateARPaymentRequest request = CreateARPaymentRequest.builder()
                .customerId(1L)
                .paymentDate(LocalDate.of(2025, 3, 20))
                .paymentMethod(ARPaymentMethod.BANK_TRANSFER)
                .paymentAmount(new BigDecimal("750.00"))
                .currency("UZS")
                .referenceNumber("REF-99999")
                .bankAccountId(10L)
                .memo("Test payment")
                .notes("Test notes")
                .build();

        // When
        ARPayment entity = mapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertEquals(LocalDate.of(2025, 3, 20), entity.getPaymentDate());
        assertEquals(ARPaymentMethod.BANK_TRANSFER, entity.getPaymentMethod());
        assertEquals(new BigDecimal("750.00"), entity.getPaymentAmount());
        assertEquals("UZS", entity.getCurrency());
        assertEquals("REF-99999", entity.getReferenceNumber());
        assertEquals(10L, entity.getBankAccountId());
        assertEquals("Test payment", entity.getMemo());
        assertEquals("Test notes", entity.getNotes());
        // Ignored fields
        assertNull(entity.getId());
        assertNull(entity.getTenantId());
        assertNull(entity.getPaymentNumber());
        // Note: status defaults to PENDING via @Builder.Default in entity but mapper
        // uses disableBuilder=true, so the Impl sets fields via setters.
        // The @Mapping(target = "status", ignore = true) means it won't be set by the mapper.
    }

    @Test
    void toEntity_handlesCardPaymentFields() {
        // Given
        CreateARPaymentRequest request = CreateARPaymentRequest.builder()
                .customerId(1L)
                .paymentDate(LocalDate.now())
                .paymentMethod(ARPaymentMethod.CREDIT_CARD)
                .paymentAmount(new BigDecimal("300.00"))
                .cardLastFour("4321")
                .cardType("VISA")
                .transactionId("TXN-ABC123")
                .build();

        // When
        ARPayment entity = mapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertEquals(ARPaymentMethod.CREDIT_CARD, entity.getPaymentMethod());
        assertEquals("4321", entity.getCardLastFour());
        assertEquals("VISA", entity.getCardType());
        assertEquals("TXN-ABC123", entity.getTransactionId());
    }
}
