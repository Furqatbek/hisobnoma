package com.hisobnoma.platform.mobile.mapper;

import com.hisobnoma.platform.mobile.dto.MobileAlertDTO;
import com.hisobnoma.platform.mobile.entity.MobileAlert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class MobileAlertMapperTest {

    @Autowired
    private MobileAlertMapper mobileAlertMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        Instant readAt = Instant.now();
        Instant expiresAt = Instant.now().plusSeconds(86400);

        MobileAlert entity = MobileAlert.builder()
                .id(1L)
                .tenantId(1L)
                .userId(10L)
                .alertType(MobileAlert.AlertType.LOW_STOCK)
                .title("Low Stock Alert")
                .message("Product SKU-001 is below minimum stock level")
                .priority(MobileAlert.AlertPriority.HIGH)
                .entityType("Product")
                .entityId(50L)
                .actionUrl("/inventory/products/50")
                .data("{\"sku\":\"SKU-001\",\"currentStock\":5}")
                .read(true)
                .readAt(readAt)
                .expiresAt(expiresAt)
                .sentViaPush(true)
                .pushSentAt(Instant.now())
                .build();

        // When
        MobileAlertDTO dto = mobileAlertMapper.toDto(entity);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(MobileAlert.AlertType.LOW_STOCK, dto.getAlertType());
        assertEquals("Low Stock Alert", dto.getTitle());
        assertEquals("Product SKU-001 is below minimum stock level", dto.getMessage());
        assertEquals(MobileAlert.AlertPriority.HIGH, dto.getPriority());
        assertEquals("Product", dto.getEntityType());
        assertEquals(50L, dto.getEntityId());
        assertEquals("/inventory/products/50", dto.getActionUrl());
        assertEquals("{\"sku\":\"SKU-001\",\"currentStock\":5}", dto.getData());
        assertTrue(dto.isRead());
        assertEquals(readAt, dto.getReadAt());
        assertEquals(expiresAt, dto.getExpiresAt());
    }

    @Test
    void toEntity_fromDto_mapsBasicFields() {
        // Given
        Instant expiresAt = Instant.now().plusSeconds(86400);

        MobileAlertDTO dto = MobileAlertDTO.builder()
                .alertType(MobileAlert.AlertType.PAYMENT_RECEIVED)
                .title("Payment Received")
                .message("Payment of 500,000 UZS received")
                .priority(MobileAlert.AlertPriority.NORMAL)
                .entityType("Payment")
                .entityId(100L)
                .actionUrl("/payments/100")
                .data("{\"amount\":500000}")
                .read(false)
                .expiresAt(expiresAt)
                .build();

        // When
        MobileAlert entity = mobileAlertMapper.toEntity(dto);

        // Then
        assertNotNull(entity);
        assertEquals(MobileAlert.AlertType.PAYMENT_RECEIVED, entity.getAlertType());
        assertEquals("Payment Received", entity.getTitle());
        assertEquals("Payment of 500,000 UZS received", entity.getMessage());
        assertEquals(MobileAlert.AlertPriority.NORMAL, entity.getPriority());
        assertEquals("Payment", entity.getEntityType());
        assertEquals(100L, entity.getEntityId());
        assertEquals("/payments/100", entity.getActionUrl());
        assertEquals("{\"amount\":500000}", entity.getData());
        assertFalse(entity.isRead());
        assertEquals(expiresAt, entity.getExpiresAt());
        // Ignored fields
        assertNull(entity.getId());
        assertNull(entity.getTenantId());
        assertNull(entity.getReadAt());
        assertFalse(entity.isSentViaPush());
        assertNull(entity.getPushSentAt());
    }

    @Test
    void toDtoList_mapsAllAlerts() {
        // Given
        MobileAlert a1 = MobileAlert.builder()
                .id(1L).alertType(MobileAlert.AlertType.LOW_STOCK)
                .title("Alert 1").message("Msg 1")
                .priority(MobileAlert.AlertPriority.NORMAL).build();
        MobileAlert a2 = MobileAlert.builder()
                .id(2L).alertType(MobileAlert.AlertType.SYSTEM_ALERT)
                .title("Alert 2").message("Msg 2")
                .priority(MobileAlert.AlertPriority.HIGH).build();

        // When
        List<MobileAlertDTO> dtos = mobileAlertMapper.toDtoList(List.of(a1, a2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals("Alert 1", dtos.get(0).getTitle());
        assertEquals("Alert 2", dtos.get(1).getTitle());
    }
}
