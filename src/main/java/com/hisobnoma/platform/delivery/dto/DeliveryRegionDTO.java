package com.hisobnoma.platform.delivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryRegionDTO {
    private Long id;
    private String name;
    private String code;
    private String description;
    private boolean active;
    private Integer sortOrder;
    private java.math.BigDecimal deliveryFee;
    private int villageCount;
    private Instant createdAt;
    private Instant updatedAt;
}
