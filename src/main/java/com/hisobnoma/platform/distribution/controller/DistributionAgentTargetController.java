package com.hisobnoma.platform.distribution.controller;

import com.hisobnoma.platform.distribution.dto.CreateDistributionAgentTargetRequest;
import com.hisobnoma.platform.distribution.dto.DistributionAgentTargetDto;
import com.hisobnoma.platform.distribution.dto.UpdateDistributionAgentTargetRequest;
import com.hisobnoma.platform.distribution.service.DistributionAgentTargetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/distribution/agent-targets")
@RequiredArgsConstructor
public class DistributionAgentTargetController {

    private final DistributionAgentTargetService targetService;

    @GetMapping("/by-agent/{agentId}")
    @PreAuthorize("hasAuthority('DISTRIBUTION_KPI_VIEW')")
    public ResponseEntity<List<DistributionAgentTargetDto>> getTargetsForAgent(@PathVariable Long agentId) {
        return ResponseEntity.ok(targetService.getTargetsForAgent(agentId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DISTRIBUTION_KPI_VIEW')")
    public ResponseEntity<DistributionAgentTargetDto> getTarget(@PathVariable Long id) {
        return ResponseEntity.ok(targetService.getTarget(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('DISTRIBUTION_KPI_MANAGE')")
    public ResponseEntity<DistributionAgentTargetDto> createTarget(
            @Valid @RequestBody CreateDistributionAgentTargetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(targetService.createTarget(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('DISTRIBUTION_KPI_MANAGE')")
    public ResponseEntity<DistributionAgentTargetDto> updateTarget(
            @PathVariable Long id, @Valid @RequestBody UpdateDistributionAgentTargetRequest request) {
        return ResponseEntity.ok(targetService.updateTarget(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DISTRIBUTION_KPI_MANAGE')")
    public ResponseEntity<Void> deleteTarget(@PathVariable Long id) {
        targetService.deleteTarget(id);
        return ResponseEntity.noContent().build();
    }
}
