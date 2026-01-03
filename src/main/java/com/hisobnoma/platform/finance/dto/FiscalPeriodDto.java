package com.hisobnoma.platform.finance.dto;

import com.hisobnoma.platform.finance.entity.PeriodStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FiscalPeriodDto {
    private Long id;
    private Long fiscalYearId;
    private Integer fiscalYear;
    private Integer periodNumber;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private PeriodStatus status;
    private boolean adjustmentPeriod;
    private LocalDateTime closedAt;
    private Long closedBy;
    private LocalDateTime reopenedAt;
    private Long reopenedBy;
    private String reopenReason;
    private String notes;
    private String displayName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
