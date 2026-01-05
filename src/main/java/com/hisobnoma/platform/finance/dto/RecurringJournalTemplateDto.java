package com.hisobnoma.platform.finance.dto;

import com.hisobnoma.platform.finance.entity.RecurringJournalTemplate.RecurrenceFrequency;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringJournalTemplateDto {

    private Long id;

    @NotBlank(message = "Name is required")
    @Size(max = 50)
    private String name;

    @Size(max = 500)
    private String description;

    @NotNull(message = "Frequency is required")
    private RecurrenceFrequency frequency;

    private Integer frequencyDay;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate endDate;
    private LocalDate nextRunDate;
    private LocalDate lastRunDate;
    private Integer runCount;
    private boolean active;
    private boolean autoPost;

    @NotEmpty(message = "At least one line is required")
    @Valid
    private List<LineDto> lines;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineDto {
        private Long id;

        @NotNull(message = "Account ID is required")
        private Long accountId;
        private String accountCode;
        private String accountName;

        @Size(max = 500)
        private String description;

        private BigDecimal debitAmount;
        private BigDecimal creditAmount;
        private Integer sortOrder;
    }
}
