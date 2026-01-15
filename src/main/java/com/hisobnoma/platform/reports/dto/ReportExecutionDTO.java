package com.hisobnoma.platform.reports.dto;

import com.hisobnoma.platform.reports.entity.ReportDefinition;
import com.hisobnoma.platform.reports.entity.ReportExecution;
import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportExecutionDTO {
    private Long id;
    private Long reportDefinitionId;
    private String reportCode;
    private String reportName;
    private Long reportScheduleId;
    private Long executedByUserId;
    private ReportExecution.ExecutionSource source;
    private ReportExecution.ExecutionStatus status;
    private String parameters;
    private ReportDefinition.ExportFormat exportFormat;
    private Instant startedAt;
    private Instant completedAt;
    private Long durationMs;
    private Integer rowCount;
    private String filePath;
    private Long fileSize;
    private String errorMessage;
}
