package com.hisobnoma.platform.reports.dto;

import com.hisobnoma.platform.reports.entity.ReportDefinition;
import com.hisobnoma.platform.reports.entity.ReportSchedule;
import lombok.*;

import java.time.Instant;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportScheduleDTO {
    private Long id;
    private Long reportDefinitionId;
    private String reportCode;
    private String reportName;
    private String name;
    private ReportSchedule.ScheduleFrequency frequency;
    private String cronExpression;
    private LocalTime runTime;
    private Integer dayOfWeek;
    private Integer dayOfMonth;
    private String parameters;
    private ReportDefinition.ExportFormat exportFormat;
    private ReportSchedule.DeliveryMethod deliveryMethod;
    private String emailRecipients;
    private Instant lastRunAt;
    private Instant nextRunAt;
    private String lastRunStatus;
    private boolean active;
}
