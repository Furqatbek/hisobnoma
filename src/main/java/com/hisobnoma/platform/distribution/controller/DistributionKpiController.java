package com.hisobnoma.platform.distribution.controller;

import com.hisobnoma.platform.distribution.dto.AgentKpiDto;
import com.hisobnoma.platform.distribution.service.DistributionKpiService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/distribution/kpi")
@RequiredArgsConstructor
public class DistributionKpiController {

    private final DistributionKpiService kpiService;

    /** Per-agent KPI leaderboard for the {@code [from, to]} period (both inclusive). */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('DISTRIBUTION_KPI_VIEW')")
    public ResponseEntity<List<AgentKpiDto>> dashboard(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(kpiService.getDashboard(from, to));
    }

    /** Daily revenue/orders/visits/collections for the trend chart (zero-filled days). */
    @GetMapping("/trend")
    @PreAuthorize("hasAuthority('DISTRIBUTION_KPI_VIEW')")
    public ResponseEntity<List<com.hisobnoma.platform.distribution.dto.DailyTrendDto>> trend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(kpiService.getTrend(from, to));
    }
}
