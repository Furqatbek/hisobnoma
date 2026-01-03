package com.hisobnoma.platform.finance.dto;

import com.hisobnoma.platform.finance.entity.PeriodStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FiscalYearDto {
    private Long id;
    private Integer year;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private PeriodStatus status;
    private boolean current;
    private boolean closed;
    private LocalDateTime closedAt;
    private Long closedBy;
    private Long retainedEarningsAccountId;
    private String retainedEarningsAccountName;
    private Long yearEndClosingEntryId;
    private String notes;
    private List<FiscalPeriodDto> periods;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
