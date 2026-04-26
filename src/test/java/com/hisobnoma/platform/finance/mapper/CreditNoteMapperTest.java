package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.CreateCreditNoteRequest;
import com.hisobnoma.platform.finance.dto.CreditNoteDto;
import com.hisobnoma.platform.finance.entity.CreditNote;
import com.hisobnoma.platform.finance.entity.CreditNoteStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CreditNoteMapperTest {

    private CreditNoteMapper creditNoteMapper;

    @BeforeEach
    void setUp() {
        creditNoteMapper = Mappers.getMapper(CreditNoteMapper.class);
    }

    @Test
    void toDto_allFields_mappedCorrectly() {
        // Given
        CreditNote entity = CreditNote.builder()
                .id(1L)
                .creditNoteNumber("CN-000001")
                .creditNoteDate(LocalDate.of(2024, 6, 15))
                .status(CreditNoteStatus.DRAFT)
                .creditAmount(new BigDecimal("500.00"))
                .subtotal(new BigDecimal("450.00"))
                .taxAmount(new BigDecimal("50.00"))
                .totalAmount(new BigDecimal("500.00"))
                .appliedAmount(BigDecimal.ZERO)
                .balance(new BigDecimal("500.00"))
                .refundedAmount(BigDecimal.ZERO)
                .currency("UZS")
                .exchangeRate(BigDecimal.ONE)
                .reason("Defective goods")
                .description("Return of defective items")
                .build();
        entity.setTenantId(1L);

        // When
        CreditNoteDto dto = creditNoteMapper.toDto(entity);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("CN-000001", dto.getCreditNoteNumber());
        assertEquals(CreditNoteStatus.DRAFT, dto.getStatus());
        assertEquals(new BigDecimal("500.00"), dto.getBalance());
        assertEquals("Defective goods", dto.getReason());
    }

    @Test
    void toEntity_mapsRequestToEntity() {
        // Given
        CreateCreditNoteRequest request = CreateCreditNoteRequest.builder()
                .customerId(1L)
                .creditNoteDate(LocalDate.now())
                .creditAmount(new BigDecimal("500.00"))
                .subtotal(new BigDecimal("450.00"))
                .taxAmount(new BigDecimal("50.00"))
                .reason("Returned goods")
                .description("Customer returned items")
                .build();

        // When
        CreditNote entity = creditNoteMapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertEquals(new BigDecimal("500.00"), entity.getCreditAmount());
        assertEquals("Returned goods", entity.getReason());
        assertNull(entity.getId());
        assertNull(entity.getCreditNoteNumber()); // Ignored in mapping
    }

    @Test
    void toDtoList_mapsList() {
        // Given
        CreditNote cn1 = CreditNote.builder()
                .id(1L).creditNoteNumber("CN-000001")
                .status(CreditNoteStatus.DRAFT).creditAmount(new BigDecimal("100"))
                .build();
        CreditNote cn2 = CreditNote.builder()
                .id(2L).creditNoteNumber("CN-000002")
                .status(CreditNoteStatus.APPROVED).creditAmount(new BigDecimal("200"))
                .build();

        // When
        List<CreditNoteDto> dtos = creditNoteMapper.toDtoList(List.of(cn1, cn2));

        // Then
        assertEquals(2, dtos.size());
        assertEquals("CN-000001", dtos.get(0).getCreditNoteNumber());
        assertEquals("CN-000002", dtos.get(1).getCreditNoteNumber());
    }

    @Test
    void updateEntity_updatesExistingEntity() {
        // Given
        CreditNote entity = CreditNote.builder()
                .id(1L)
                .creditNoteNumber("CN-000001")
                .creditAmount(new BigDecimal("500.00"))
                .reason("Original reason")
                .build();

        CreateCreditNoteRequest request = CreateCreditNoteRequest.builder()
                .customerId(1L)
                .creditNoteDate(LocalDate.now())
                .creditAmount(new BigDecimal("600.00"))
                .reason("Updated reason")
                .build();

        // When
        creditNoteMapper.updateEntity(request, entity);

        // Then
        assertEquals(new BigDecimal("600.00"), entity.getCreditAmount());
        assertEquals("Updated reason", entity.getReason());
        assertEquals(1L, entity.getId()); // id should not change
        assertEquals("CN-000001", entity.getCreditNoteNumber()); // number should not change
    }
}
