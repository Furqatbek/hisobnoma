package com.hisobnoma.platform.reports.dto;

import com.hisobnoma.platform.reports.entity.ReportDefinition;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDefinitionDTO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private ReportDefinition.ReportCategory category;
    private List<ReportDefinition.ExportFormat> supportedFormats;
    private String parameterSchema;
    private String defaultParameters;
    private boolean systemReport;
    private boolean active;
    private Integer sortOrder;
}
