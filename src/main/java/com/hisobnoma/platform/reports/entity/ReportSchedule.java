package com.hisobnoma.platform.reports.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.time.LocalTime;

/**
 * Entity for scheduling report generation.
 */
@Entity
@Table(name = "report_schedules", indexes = {
        @Index(name = "idx_report_schedule_tenant", columnList = "tenant_id"),
        @Index(name = "idx_report_schedule_active", columnList = "active"),
        @Index(name = "idx_report_schedule_next_run", columnList = "next_run_at")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class ReportSchedule extends TenantAwareEntity {

    public enum ScheduleFrequency {
        DAILY,
        WEEKLY,
        MONTHLY,
        QUARTERLY,
        YEARLY,
        CUSTOM_CRON
    }

    public enum DeliveryMethod {
        EMAIL,
        STORE_ONLY,
        WEBHOOK
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_definition_id", nullable = false)
    private ReportDefinition reportDefinition;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 20)
    private ScheduleFrequency frequency;

    @Column(name = "cron_expression", length = 50)
    private String cronExpression;

    @Column(name = "run_time")
    private LocalTime runTime;

    @Column(name = "day_of_week")
    private Integer dayOfWeek;

    @Column(name = "day_of_month")
    private Integer dayOfMonth;

    @Column(name = "parameters", columnDefinition = "JSONB")
    private String parameters;

    @Enumerated(EnumType.STRING)
    @Column(name = "export_format", nullable = false, length = 10)
    private ReportDefinition.ExportFormat exportFormat;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_method", nullable = false, length = 20)
    @Builder.Default
    private DeliveryMethod deliveryMethod = DeliveryMethod.STORE_ONLY;

    @Column(name = "email_recipients", length = 500)
    private String emailRecipients;

    @Column(name = "webhook_url", length = 500)
    private String webhookUrl;

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @Column(name = "next_run_at")
    private Instant nextRunAt;

    @Column(name = "last_run_status", length = 20)
    private String lastRunStatus;

    @Column(name = "active")
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;
}
