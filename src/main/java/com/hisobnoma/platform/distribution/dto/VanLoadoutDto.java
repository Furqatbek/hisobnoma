package com.hisobnoma.platform.distribution.dto;

import com.hisobnoma.platform.distribution.entity.VanLoadoutStatus;
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
public class VanLoadoutDto {
    private Long id;
    private String loadoutNumber;
    private VanLoadoutStatus status;
    private Long agentId;
    private Long vehicleLocationId;
    private Long sourceLocationId;
    private LocalDate loadoutDate;
    private Instant reconciledAt;
    private Long reconciledBy;
    private BigDecimal totalLoadedValue;
    private BigDecimal totalSoldValue;
    private BigDecimal totalReturnedValue;
    private BigDecimal totalDamagedValue;
    private BigDecimal expectedCash;
    private BigDecimal actualCash;
    private BigDecimal cashDifference;
    private String currency;
    private String notes;
    private List<VanLoadoutLineDto> lines;
}
