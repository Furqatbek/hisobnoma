package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.ARPaymentAllocationDto;
import com.hisobnoma.platform.finance.dto.CreateARPaymentAllocationRequest;
import com.hisobnoma.platform.finance.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ARPaymentAllocationMapperTest {

    @Autowired
    private ARPaymentAllocationMapper arPaymentAllocationMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        ARPayment arPayment = ARPayment.builder().id(10L).build();
        ARInvoice arInvoice = ARInvoice.builder().id(20L).build();
        CreditNote creditNote = CreditNote.builder().id(30L).build();

        ARPaymentAllocation allocation = ARPaymentAllocation.builder()
                .id(1L)
                .arPayment(arPayment)
                .arInvoice(arInvoice)
                .creditNote(creditNote)
                .invoiceNumber("INV-AR-001")
                .invoiceAmount(new BigDecimal("800000.0000"))
                .invoiceBalanceBefore(new BigDecimal("500000.0000"))
                .allocatedAmount(new BigDecimal("300000.0000"))
                .discountTaken(new BigDecimal("15000.0000"))
                .writeOffAmount(new BigDecimal("5000.0000"))
                .invoiceBalanceAfter(new BigDecimal("180000.0000"))
                .notes("AR allocation notes")
                .build();

        // When
        ARPaymentAllocationDto dto = arPaymentAllocationMapper.toDto(allocation);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getArPaymentId());
        assertEquals(20L, dto.getArInvoiceId());
        assertEquals(30L, dto.getCreditNoteId());
        assertEquals("INV-AR-001", dto.getInvoiceNumber());
        assertEquals(new BigDecimal("800000.0000"), dto.getInvoiceAmount());
        assertEquals(new BigDecimal("500000.0000"), dto.getInvoiceBalanceBefore());
        assertEquals(new BigDecimal("300000.0000"), dto.getAllocatedAmount());
        assertEquals(new BigDecimal("15000.0000"), dto.getDiscountTaken());
        assertEquals(new BigDecimal("5000.0000"), dto.getWriteOffAmount());
        assertEquals(new BigDecimal("180000.0000"), dto.getInvoiceBalanceAfter());
        assertEquals("AR allocation notes", dto.getNotes());
        // totalApplied is computed via expression
        assertNotNull(dto.getTotalApplied());
        assertEquals(new BigDecimal("320000.0000"), dto.getTotalApplied());
    }

    @Test
    void toDto_withNullCreditNote_mapsCreditNoteIdToNull() {
        // Given
        ARPayment arPayment = ARPayment.builder().id(10L).build();
        ARInvoice arInvoice = ARInvoice.builder().id(20L).build();

        ARPaymentAllocation allocation = ARPaymentAllocation.builder()
                .id(1L)
                .arPayment(arPayment)
                .arInvoice(arInvoice)
                .creditNote(null)
                .allocatedAmount(new BigDecimal("100000.0000"))
                .discountTaken(BigDecimal.ZERO)
                .writeOffAmount(BigDecimal.ZERO)
                .build();

        // When
        ARPaymentAllocationDto dto = arPaymentAllocationMapper.toDto(allocation);

        // Then
        assertNotNull(dto);
        assertNull(dto.getCreditNoteId());
    }

    @Test
    void toEntity_fromCreateRequest_mapsBasicFields() {
        // Given
        CreateARPaymentAllocationRequest request = CreateARPaymentAllocationRequest.builder()
                .arInvoiceId(20L)
                .creditNoteId(30L)
                .allocatedAmount(new BigDecimal("200000.0000"))
                .discountTaken(new BigDecimal("10000.0000"))
                .writeOffAmount(new BigDecimal("3000.0000"))
                .notes("New AR allocation")
                .build();

        // When
        ARPaymentAllocation entity = arPaymentAllocationMapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertEquals(new BigDecimal("200000.0000"), entity.getAllocatedAmount());
        assertEquals(new BigDecimal("10000.0000"), entity.getDiscountTaken());
        assertEquals(new BigDecimal("3000.0000"), entity.getWriteOffAmount());
        assertEquals("New AR allocation", entity.getNotes());
        // Ignored fields
        assertNull(entity.getId());
        assertNull(entity.getArPayment());
        assertNull(entity.getArInvoice());
        assertNull(entity.getCreditNote());
        assertNull(entity.getInvoiceNumber());
        assertNull(entity.getInvoiceAmount());
        assertNull(entity.getInvoiceBalanceBefore());
        assertNull(entity.getInvoiceBalanceAfter());
    }

    @Test
    void toDtoList_mapsAllAllocations() {
        // Given
        ARPayment payment = ARPayment.builder().id(10L).build();
        ARInvoice invoice = ARInvoice.builder().id(20L).build();

        ARPaymentAllocation a1 = ARPaymentAllocation.builder()
                .id(1L).arPayment(payment).arInvoice(invoice)
                .allocatedAmount(new BigDecimal("100000.0000"))
                .discountTaken(BigDecimal.ZERO).writeOffAmount(BigDecimal.ZERO).build();
        ARPaymentAllocation a2 = ARPaymentAllocation.builder()
                .id(2L).arPayment(payment).arInvoice(invoice)
                .allocatedAmount(new BigDecimal("50000.0000"))
                .discountTaken(BigDecimal.ZERO).writeOffAmount(BigDecimal.ZERO).build();

        // When
        List<ARPaymentAllocationDto> dtos = arPaymentAllocationMapper.toDtoList(List.of(a1, a2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
    }
}
