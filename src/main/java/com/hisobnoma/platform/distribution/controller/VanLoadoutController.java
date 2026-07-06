package com.hisobnoma.platform.distribution.controller;

import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.distribution.dto.CreateVanLoadoutRequest;
import com.hisobnoma.platform.distribution.dto.ReconcileVanLoadoutRequest;
import com.hisobnoma.platform.distribution.dto.VanLoadoutDto;
import com.hisobnoma.platform.distribution.entity.VanLoadoutStatus;
import com.hisobnoma.platform.distribution.service.VanLoadoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/distribution/van-loadouts")
@RequiredArgsConstructor
public class VanLoadoutController {

    private final VanLoadoutService loadoutService;

    @GetMapping
    @PreAuthorize("hasAuthority('DISTRIBUTION_VAN_VIEW')")
    public ResponseEntity<PageResponse<VanLoadoutDto>> getLoadouts(
            @RequestParam(required = false) VanLoadoutStatus status,
            @RequestParam(required = false) Long agentId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        if (status != null) {
            return ResponseEntity.ok(loadoutService.getLoadoutsByStatus(status, pageable));
        }
        if (agentId != null) {
            return ResponseEntity.ok(loadoutService.getLoadoutsByAgent(agentId, pageable));
        }
        return ResponseEntity.ok(loadoutService.getLoadouts(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DISTRIBUTION_VAN_VIEW')")
    public ResponseEntity<VanLoadoutDto> getLoadout(@PathVariable Long id) {
        return ResponseEntity.ok(loadoutService.getLoadout(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('DISTRIBUTION_VAN_MANAGE')")
    public ResponseEntity<VanLoadoutDto> createLoadout(@Valid @RequestBody CreateVanLoadoutRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loadoutService.createLoadout(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DISTRIBUTION_VAN_MANAGE')")
    public ResponseEntity<Void> deleteLoadout(@PathVariable Long id) {
        loadoutService.deleteLoadout(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/load")
    @PreAuthorize("hasAuthority('DISTRIBUTION_VAN_MANAGE')")
    public ResponseEntity<VanLoadoutDto> load(@PathVariable Long id) {
        return ResponseEntity.ok(loadoutService.load(id));
    }

    @PostMapping("/{id}/reconcile")
    @PreAuthorize("hasAuthority('DISTRIBUTION_VAN_MANAGE')")
    public ResponseEntity<VanLoadoutDto> reconcile(
            @PathVariable Long id, @Valid @RequestBody ReconcileVanLoadoutRequest request) {
        return ResponseEntity.ok(loadoutService.reconcile(id, request));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('DISTRIBUTION_VAN_MANAGE')")
    public ResponseEntity<VanLoadoutDto> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(loadoutService.cancel(id));
    }
}
