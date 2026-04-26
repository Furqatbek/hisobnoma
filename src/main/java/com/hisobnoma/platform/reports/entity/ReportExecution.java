package com.hisobnoma.platform.reports.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Entity logging report execution history.
 */
@Entity
@Table(name = "report_executions", indexes = {
        @Index(name = "idx_report_exec_tenant", columnList = "tenant_id"),
        @Index(name = "idx_report_exec_report", columnList = "report_definition_id"),
        @Index(name = "idx_report_exec_user", columnList = "executed_by_user_id"),
        @Index(name = "idx_report_exec_status", columnList = "status"),
        @Index(name = "idx_report_exec_started", columnList = "started_at")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ReportExecution extends TenantAwareEntity {

    public enum ExecutionStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    public enum ExecutionSource {
        MANUAL,
        SCHEDULED,
        API
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_definition_id", nullable = false)
    private ReportDefinition reportDefinition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_schedule_id")
    private ReportSchedule reportSchedule;

    @Column(name = "executed_by_user_id")
    private Long executedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private ExecutionSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ExecutionStatus status = ExecutionStatus.PENDING;

    @Column(name = "parameters", columnDefinition = "TEXT")
    private String parameters;

    @Enumerated(EnumType.STRING)
    @Column(name = "export_format", length = 10)
    private ReportDefinition.ExportFormat exportFormat;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "row_count")
    private Integer rowCount;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "error_stack_trace", columnDefinition = "TEXT")
    private String errorStackTrace;
}
