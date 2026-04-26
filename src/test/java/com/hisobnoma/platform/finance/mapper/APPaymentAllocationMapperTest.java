package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.APPaymentAllocationDto;
import com.hisobnoma.platform.finance.dto.CreateAPPaymentAllocationRequest;
import com.hisobnoma.platform.finance.entity.APInvoice;
import com.hisobnoma.platform.finance.entity.APPayment;
import com.hisobnoma.platform.finance.entity.APPaymentAllocation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class APPaymentAllocationMapperTest {

    @Autowired
    private APPaymentAllocationMapper apPaymentAllocationMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        APPayment apPayment = APPayment.builder().id(10L).build();
        APInvoice apInvoice = APInvoice.builder().id(20L).build();

        APPaymentAllocation allocation = APPaymentAllocation.builder()
                .id(1L)
                .apPayment(apPayment)
                .apInvoice(apInvoice)
                .invoiceNumber("INV-001")
                .invoiceAmount(new BigDecimal("500000.0000"))
                .invoiceBalanceBefore(new BigDecimal("300000.0000"))
                .allocatedAmount(new BigDecimal("200000.0000"))
                .discountTaken(new BigDecimal("10000.0000"))
                .writeOffAmount(new BigDecimal("5000.0000"))
                .invoiceBalanceAfter(new BigDecimal("85000.0000"))
                .notes("Payment allocation notes")
                .build();

        // When
        APPaymentAllocationDto dto = apPaymentAllocationMapper.toDto(allocation);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getApPaymentId());
        assertEquals(20L, dto.getApInvoiceId());
        assertEquals("INV-001", dto.getInvoiceNumber());
        assertEquals(new BigDecimal("500000.0000"), dto.getInvoiceAmount());
        assertEquals(new BigDecimal("300000.0000"), dto.getInvoiceBalanceBefore());
        assertEquals(new BigDecimal("200000.0000"), dto.getAllocatedAmount());
        assertEquals(new BigDecimal("10000.0000"), dto.getDiscountTaken());
        assertEquals(new BigDecimal("5000.0000"), dto.getWriteOffAmount());
        assertEquals(new BigDecimal("85000.0000"), dto.getInvoiceBalanceAfter());
        assertEquals("Payment allocation notes", dto.getNotes());
        // totalApplied is computed via expression
        assertNotNull(dto.getTotalApplied());
        assertEquals(new BigDecimal("215000.0000"), dto.getTotalApplied());
    }

    @Test
    void toEntity_fromCreateRequest_mapsBasicFields() {
        // Given
        CreateAPPaymentAllocationRequest request = CreateAPPaymentAllocationRequest.builder()
                .apInvoiceId(20L)
                .allocatedAmount(new BigDecimal("100000.0000"))
                .discountTaken(new BigDecimal("5000.0000"))
                .writeOffAmount(new BigDecimal("2000.0000"))
                .notes("New allocation")
                .build();

        // When
        APPaymentAllocation entity = apPaymentAllocationMapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertEquals(new BigDecimal("100000.0000"), entity.getAllocatedAmount());
        assertEquals(new BigDecimal("5000.0000"), entity.getDiscountTaken());
        assertEquals(new BigDecimal("2000.0000"), entity.getWriteOffAmount());
        assertEquals("New allocation", entity.getNotes());
        // Ignored fields
        assertNull(entity.getId());
        assertNull(entity.getApPayment());
        assertNull(entity.getApInvoice());
        assertNull(entity.getInvoiceNumber());
        assertNull(entity.getInvoiceAmount());
        assertNull(entity.getInvoiceBalanceBefore());
        assertNull(entity.getInvoiceBalanceAfter());
    }

    @Test
    void toDtoList_mapsAllAllocations() {
        // Given
        APPayment payment = APPayment.builder().id(10L).build();
        APInvoice invoice = APInvoice.builder().id(20L).build();

        APPaymentAllocation a1 = APPaymentAllocation.builder()
                .id(1L).apPayment(payment).apInvoice(invoice)
                .allocatedAmount(new BigDecimal("100000.0000"))
                .discountTaken(BigDecimal.ZERO).writeOffAmount(BigDecimal.ZERO).build();
        APPaymentAllocation a2 = APPaymentAllocation.builder()
                .id(2L).apPayment(payment).apInvoice(invoice)
                .allocatedAmount(new BigDecimal("50000.0000"))
                .discountTaken(BigDecimal.ZERO).writeOffAmount(BigDecimal.ZERO).build();

        // When
        List<APPaymentAllocationDto> dtos = apPaymentAllocationMapper.toDtoList(List.of(a1, a2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
    }
}
