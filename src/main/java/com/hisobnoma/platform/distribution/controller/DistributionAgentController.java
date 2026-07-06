package com.hisobnoma.platform.distribution.controller;

import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.distribution.dto.CreateDistributionAgentRequest;
import com.hisobnoma.platform.distribution.dto.DistributionAgentDto;
import com.hisobnoma.platform.distribution.dto.UpdateDistributionAgentRequest;
import com.hisobnoma.platform.distribution.service.DistributionAgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/distribution/agents")
@RequiredArgsConstructor
public class DistributionAgentController {

    private final DistributionAgentService agentService;

    @GetMapping
    @PreAuthorize("hasAuthority('DISTRIBUTION_AGENT_VIEW')")
    public ResponseEntity<List<DistributionAgentDto>> getAllAgents() {
        return ResponseEntity.ok(agentService.getAllAgents());
    }

    @GetMapping("/paginated")
    @PreAuthorize("hasAuthority('DISTRIBUTION_AGENT_VIEW')")
    public ResponseEntity<PageResponse<DistributionAgentDto>> getAgentsPaginated(
            @PageableDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(agentService.getAgents(pageable));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('DISTRIBUTION_AGENT_VIEW')")
    public ResponseEntity<List<DistributionAgentDto>> getActiveAgents() {
        return ResponseEntity.ok(agentService.getActiveAgents());
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('DISTRIBUTION_AGENT_VIEW')")
    public ResponseEntity<PageResponse<DistributionAgentDto>> searchAgents(
            @RequestParam String q,
            @PageableDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(agentService.searchAgents(q, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DISTRIBUTION_AGENT_VIEW')")
    public ResponseEntity<DistributionAgentDto> getAgent(@PathVariable Long id) {
        return ResponseEntity.ok(agentService.getAgent(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('DISTRIBUTION_AGENT_MANAGE')")
    public ResponseEntity<DistributionAgentDto> createAgent(
            @Valid @RequestBody CreateDistributionAgentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(agentService.createAgent(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('DISTRIBUTION_AGENT_MANAGE')")
    public ResponseEntity<DistributionAgentDto> updateAgent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDistributionAgentRequest request) {
        return ResponseEntity.ok(agentService.updateAgent(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DISTRIBUTION_AGENT_MANAGE')")
    public ResponseEntity<Void> deleteAgent(@PathVariable Long id) {
        agentService.deleteAgent(id);
        return ResponseEntity.noContent().build();
    }
}
