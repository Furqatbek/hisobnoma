package com.hisobnoma.platform.inventory.dto;

import com.hisobnoma.platform.inventory.entity.CountStatus;
import com.hisobnoma.platform.inventory.entity.CountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCountDto {

    private Long id;
    private String countNumber;
    private Long locationId;
    private String locationCode;
    private String locationName;
    private CountStatus status;
    private CountType countType;
    private LocalDate countDate;
    private String description;
    private String notes;
    private Integer totalItems;
    private Integer countedItems;
    private Integer varianceItems;
    private BigDecimal totalVarianceValue;
    private BigDecimal positiveVarianceValue;
    private BigDecimal negativeVarianceValue;
    private boolean freezeStock;
    private Instant startedAt;
    private Instant completedAt;
    private Long approvedBy;
    private Instant approvedAt;
    private Long cancelledBy;
    private Instant cancelledAt;
    private String cancellationReason;
    private boolean adjustmentsPosted;
    private Long categoryId;
    private String categoryName;
    private List<InventoryCountLineDto> lines;
    private BigDecimal completionPercentage;
    private boolean complete;
    private Instant createdAt;
    private Long createdBy;
}
