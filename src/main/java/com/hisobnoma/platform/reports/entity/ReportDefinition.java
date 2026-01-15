package com.hisobnoma.platform.reports.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity storing report definitions and metadata.
 */
@Entity
@Table(name = "report_definitions", indexes = {
        @Index(name = "idx_report_def_tenant", columnList = "tenant_id"),
        @Index(name = "idx_report_def_code", columnList = "code"),
        @Index(name = "idx_report_def_category", columnList = "category")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDefinition extends TenantAwareEntity {

    public enum ReportCategory {
        INVENTORY,
        FINANCIAL,
        SALES,
        PURCHASING,
        CUSTOM
    }

    public enum ExportFormat {
        PDF,
        EXCEL,
        CSV,
        JSON
    }

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private ReportCategory category;

    @Column(name = "query_class", length = 255)
    private String queryClass;

    @Column(name = "template_path", length = 255)
    private String templatePath;

    @ElementCollection
    @CollectionTable(name = "report_export_formats",
                    joinColumns = @JoinColumn(name = "report_definition_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "format")
    @Builder.Default
    private List<ExportFormat> supportedFormats = new ArrayList<>();

    @Column(name = "parameter_schema", columnDefinition = "JSONB")
    private String parameterSchema;

    @Column(name = "default_parameters", columnDefinition = "JSONB")
    private String defaultParameters;

    @Column(name = "is_system_report")
    @Builder.Default
    private boolean systemReport = false;

    @Column(name = "active")
    @Builder.Default
    private boolean active = true;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @OneToMany(mappedBy = "reportDefinition", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReportSchedule> schedules = new ArrayList<>();
}
