package com.hisobnoma.platform.web.controller;

import com.hisobnoma.platform.auth.security.RequiresPermission;
import com.hisobnoma.platform.common.dto.ApiResponse;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.web.dto.CampaignPreviewDto;
import com.hisobnoma.platform.web.dto.CreateCampaignRequest;
import com.hisobnoma.platform.web.dto.WebCampaignDto;
import com.hisobnoma.platform.web.service.WebCampaignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Staff management of online-shop SMS marketing campaigns.
 */
@RestController
@RequestMapping("/api/v1/web-campaigns")
@RequiredArgsConstructor
public class WebCampaignAdminController {

    private final WebCampaignService campaignService;

    @GetMapping
    @RequiresPermission("WEB_CAMPAIGN_VIEW")
    public ResponseEntity<PageResponse<WebCampaignDto>> list(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<WebCampaignDto> page = campaignService.getCampaigns(pageable);
        return ResponseEntity.ok(PageResponse.of(page));
    }

    @GetMapping("/{id}")
    @RequiresPermission("WEB_CAMPAIGN_VIEW")
    public ResponseEntity<ApiResponse<WebCampaignDto>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(campaignService.getCampaign(id)));
    }

    @PostMapping
    @RequiresPermission("WEB_CAMPAIGN_MANAGE")
    public ResponseEntity<ApiResponse<WebCampaignDto>> create(
            @Valid @RequestBody CreateCampaignRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(campaignService.create(request)));
    }

    @PutMapping("/{id}")
    @RequiresPermission("WEB_CAMPAIGN_MANAGE")
    public ResponseEntity<ApiResponse<WebCampaignDto>> update(
            @PathVariable Long id, @Valid @RequestBody CreateCampaignRequest request) {
        return ResponseEntity.ok(ApiResponse.success(campaignService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("WEB_CAMPAIGN_MANAGE")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        campaignService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/preview")
    @RequiresPermission("WEB_CAMPAIGN_MANAGE")
    public ResponseEntity<ApiResponse<CampaignPreviewDto>> preview(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(campaignService.preview(id)));
    }

    @PostMapping("/{id}/send")
    @RequiresPermission("WEB_CAMPAIGN_MANAGE")
    public ResponseEntity<ApiResponse<WebCampaignDto>> send(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(campaignService.send(id)));
    }
}
