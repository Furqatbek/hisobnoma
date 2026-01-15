package com.hisobnoma.platform.reports.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgingReportDTO {
    private ReportMetadata metadata;
    private Summary summary;
    private List<AgingDetail> details;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportMetadata {
        private String reportName;
        private String reportType; // AR or AP
        private Instant generatedAt;
        private LocalDate asOfDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private BigDecimal totalOutstanding;
        private BigDecimal current;
        private BigDecimal days1to30;
        private BigDecimal days31to60;
        private BigDecimal days61to90;
        private BigDecimal over90Days;
        private int totalAccounts;
        private int overdueAccounts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgingDetail {
        private Long entityId;
        private String entityCode;
        private String entityName;
        private String contactInfo;
        private BigDecimal creditLimit;
        private BigDecimal totalOutstanding;
        private BigDecimal current;
        private BigDecimal days1to30;
        private BigDecimal days31to60;
        private BigDecimal days61to90;
        private BigDecimal over90Days;
        private int overdueInvoices;
        private LocalDate oldestInvoiceDate;
    }
}
