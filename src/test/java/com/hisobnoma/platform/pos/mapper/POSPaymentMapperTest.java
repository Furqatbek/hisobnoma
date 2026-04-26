package com.hisobnoma.platform.pos.mapper;

import com.hisobnoma.platform.pos.dto.POSPaymentDto;
import com.hisobnoma.platform.pos.entity.POSPayment;
import com.hisobnoma.platform.pos.entity.POSPaymentStatus;
import com.hisobnoma.platform.pos.entity.POSPaymentType;
import com.hisobnoma.platform.pos.entity.POSTransaction;
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
class POSPaymentMapperTest {

    @Autowired
    private POSPaymentMapper posPaymentMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        POSTransaction transaction = POSTransaction.builder().id(1L).build();

        Instant processedAt = Instant.now();
        POSPayment payment = POSPayment.builder()
                .id(100L)
                .transaction(transaction)
                .paymentNumber(1)
                .paymentType(POSPaymentType.CASH)
                .status(POSPaymentStatus.APPROVED)
                .amount(new BigDecimal("100000.0000"))
                .tenderedAmount(new BigDecimal("120000.0000"))
                .changeAmount(new BigDecimal("20000.0000"))
                .currency("UZS")
                .cardType("VISA")
                .cardLastFour("1234")
                .authCode("AUTH001")
                .gatewayReference("GW-REF-001")
                .giftCardNumber("GC-001")
                .checkNumber("CHK-001")
                .mobileReference("MOB-REF-001")
                .processedAt(processedAt)
                .processedBy(5L)
                .notes("Payment notes")
                .build();

        // When
        POSPaymentDto dto = posPaymentMapper.toDto(payment);

        // Then
        assertNotNull(dto);
        assertEquals(100L, dto.getId());
        assertEquals(1L, dto.getTransactionId());
        assertEquals(1, dto.getPaymentNumber());
        assertEquals(POSPaymentType.CASH, dto.getPaymentType());
        assertEquals(POSPaymentStatus.APPROVED, dto.getStatus());
        assertEquals(new BigDecimal("100000.0000"), dto.getAmount());
        assertEquals(new BigDecimal("120000.0000"), dto.getTenderedAmount());
        assertEquals(new BigDecimal("20000.0000"), dto.getChangeAmount());
        assertEquals("UZS", dto.getCurrency());
        assertEquals("VISA", dto.getCardType());
        assertEquals("1234", dto.getCardLastFour());
        assertEquals("AUTH001", dto.getAuthCode());
        assertEquals("GW-REF-001", dto.getGatewayReference());
        assertEquals("GC-001", dto.getGiftCardNumber());
        assertEquals("CHK-001", dto.getCheckNumber());
        assertEquals("MOB-REF-001", dto.getMobileReference());
        assertEquals(processedAt, dto.getProcessedAt());
        assertEquals(5L, dto.getProcessedBy());
        assertEquals("Payment notes", dto.getNotes());
    }

    @Test
    void toDto_withVoidedPayment_mapsVoidFields() {
        // Given
        POSTransaction transaction = POSTransaction.builder().id(1L).build();
        Instant voidedAt = Instant.now();

        POSPayment payment = POSPayment.builder()
                .id(100L)
                .transaction(transaction)
                .paymentNumber(1)
                .paymentType(POSPaymentType.CASH)
                .status(POSPaymentStatus.VOIDED)
                .amount(new BigDecimal("50000.0000"))
                .voidedAt(voidedAt)
                .voidedBy(10L)
                .voidReason("Customer changed mind")
                .build();

        // When
        POSPaymentDto dto = posPaymentMapper.toDto(payment);

        // Then
        assertNotNull(dto);
        assertEquals(POSPaymentStatus.VOIDED, dto.getStatus());
        assertEquals(voidedAt, dto.getVoidedAt());
        assertEquals(10L, dto.getVoidedBy());
        assertEquals("Customer changed mind", dto.getVoidReason());
    }

    @Test
    void toDtoList_mapsAllPayments() {
        // Given
        POSTransaction transaction = POSTransaction.builder().id(1L).build();

        POSPayment p1 = POSPayment.builder()
                .id(1L).transaction(transaction).paymentNumber(1)
                .paymentType(POSPaymentType.CASH).status(POSPaymentStatus.APPROVED)
                .amount(new BigDecimal("50000.0000")).build();

        POSPayment p2 = POSPayment.builder()
                .id(2L).transaction(transaction).paymentNumber(2)
                .paymentType(POSPaymentType.CARD).status(POSPaymentStatus.APPROVED)
                .amount(new BigDecimal("50000.0000")).build();

        // When
        List<POSPaymentDto> dtos = posPaymentMapper.toDtoList(List.of(p1, p2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
    }
}
