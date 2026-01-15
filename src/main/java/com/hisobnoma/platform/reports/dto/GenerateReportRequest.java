package com.hisobnoma.platform.reports.dto;

import com.hisobnoma.platform.reports.entity.ReportDefinition;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateReportRequest {

    // Export format - required only for export endpoints
    private ReportDefinition.ExportFormat exportFormat;

    private LocalDate startDate;
    private LocalDate endDate;
    private Long locationId;
    private Long categoryId;
    private Long customerId;
    private Long supplierId;
    private Long productId;
    private Long accountId;

    // Additional dynamic parameters
    private Map<String, Object> additionalParams;
}
