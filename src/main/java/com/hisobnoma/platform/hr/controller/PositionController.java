package com.hisobnoma.platform.hr.controller;

import com.hisobnoma.platform.hr.dto.PositionDto;
import com.hisobnoma.platform.hr.service.PositionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hr/positions")
@RequiredArgsConstructor
public class PositionController {

    private final PositionService positionService;

    @GetMapping
    @PreAuthorize("hasAuthority('HR_POSITION_READ')")
    public ResponseEntity<List<PositionDto>> getAll() {
        return ResponseEntity.ok(positionService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('HR_POSITION_READ')")
    public ResponseEntity<PositionDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(positionService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('HR_POSITION_WRITE')")
    public ResponseEntity<PositionDto> create(@RequestBody PositionDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(positionService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('HR_POSITION_WRITE')")
    public ResponseEntity<PositionDto> update(@PathVariable Long id, @RequestBody PositionDto request) {
        return ResponseEntity.ok(positionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('HR_POSITION_WRITE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        positionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
