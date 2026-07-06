package com.hisobnoma.platform.distribution.controller;

import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.distribution.dto.DistributionVisitDto;
import com.hisobnoma.platform.distribution.dto.VisitCheckInRequest;
import com.hisobnoma.platform.distribution.dto.VisitCheckOutRequest;
import com.hisobnoma.platform.distribution.service.DistributionVisitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/distribution/visits")
@RequiredArgsConstructor
public class DistributionVisitController {

    private final DistributionVisitService visitService;

    @GetMapping
    @PreAuthorize("hasAuthority('DISTRIBUTION_VISIT_VIEW')")
    public ResponseEntity<PageResponse<DistributionVisitDto>> getVisits(
            @RequestParam(required = false) Long agentId,
            @RequestParam(required = false) Long routeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PageableDefault(sort = "checkInAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(visitService.getVisits(agentId, routeId, date, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DISTRIBUTION_VISIT_VIEW')")
    public ResponseEntity<DistributionVisitDto> getVisit(@PathVariable Long id) {
        return ResponseEntity.ok(visitService.getVisit(id));
    }

    @PostMapping("/check-in")
    @PreAuthorize("hasAuthority('DISTRIBUTION_VISIT_MANAGE')")
    public ResponseEntity<DistributionVisitDto> checkIn(@Valid @RequestBody VisitCheckInRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(visitService.checkIn(request));
    }

    @PostMapping("/{id}/check-out")
    @PreAuthorize("hasAuthority('DISTRIBUTION_VISIT_MANAGE')")
    public ResponseEntity<DistributionVisitDto> checkOut(
            @PathVariable Long id, @Valid @RequestBody VisitCheckOutRequest request) {
        return ResponseEntity.ok(visitService.checkOut(id, request));
    }
}
