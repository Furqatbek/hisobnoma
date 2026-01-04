package com.hisobnoma.platform.pos.controller;

import com.hisobnoma.platform.auth.security.RequiresPermission;
import com.hisobnoma.platform.common.dto.ApiResponse;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.pos.dto.CreateTerminalRequest;
import com.hisobnoma.platform.pos.dto.POSTerminalDto;
import com.hisobnoma.platform.pos.service.POSTerminalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pos/terminals")
@RequiredArgsConstructor
public class POSTerminalController {

    private final POSTerminalService terminalService;

    @GetMapping
    @RequiresPermission("POS_TERMINAL_READ")
    public ResponseEntity<PageResponse<POSTerminalDto>> getAll(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<POSTerminalDto> page = search != null && !search.isBlank()
                ? terminalService.search(search, pageable)
                : terminalService.findAll(pageable);

        return ResponseEntity.ok(PageResponse.of(page));
    }

    @GetMapping("/{id}")
    @RequiresPermission("POS_TERMINAL_READ")
    public ResponseEntity<ApiResponse<POSTerminalDto>> getById(@PathVariable Long id) {
        POSTerminalDto terminal = terminalService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(terminal));
    }

    @GetMapping("/code/{code}")
    @RequiresPermission("POS_TERMINAL_READ")
    public ResponseEntity<ApiResponse<POSTerminalDto>> getByCode(@PathVariable String code) {
        POSTerminalDto terminal = terminalService.findByCode(code);
        return ResponseEntity.ok(ApiResponse.success(terminal));
    }

    @GetMapping("/location/{locationId}")
    @RequiresPermission("POS_TERMINAL_READ")
    public ResponseEntity<ApiResponse<List<POSTerminalDto>>> getByLocation(@PathVariable Long locationId) {
        List<POSTerminalDto> terminals = terminalService.findByLocation(locationId);
        return ResponseEntity.ok(ApiResponse.success(terminals));
    }

    @GetMapping("/active")
    @RequiresPermission("POS_TERMINAL_READ")
    public ResponseEntity<ApiResponse<List<POSTerminalDto>>> getActiveTerminals() {
        List<POSTerminalDto> terminals = terminalService.findActiveTerminals();
        return ResponseEntity.ok(ApiResponse.success(terminals));
    }

    @PostMapping
    @RequiresPermission("POS_TERMINAL_CREATE")
    public ResponseEntity<ApiResponse<POSTerminalDto>> create(@Valid @RequestBody CreateTerminalRequest request) {
        POSTerminalDto terminal = terminalService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(terminal));
    }

    @PutMapping("/{id}")
    @RequiresPermission("POS_TERMINAL_UPDATE")
    public ResponseEntity<ApiResponse<POSTerminalDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateTerminalRequest request) {
        POSTerminalDto terminal = terminalService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(terminal));
    }

    @PutMapping("/{id}/activate")
    @RequiresPermission("POS_TERMINAL_UPDATE")
    public ResponseEntity<ApiResponse<Void>> activate(@PathVariable Long id) {
        terminalService.activate(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Terminal activated"));
    }

    @PutMapping("/{id}/deactivate")
    @RequiresPermission("POS_TERMINAL_UPDATE")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        terminalService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Terminal deactivated"));
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("POS_TERMINAL_DELETE")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        terminalService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Terminal deleted"));
    }
}
