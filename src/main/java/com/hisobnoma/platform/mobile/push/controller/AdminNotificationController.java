package com.hisobnoma.platform.mobile.push.controller;

import com.hisobnoma.platform.common.dto.ApiResponse;
import com.hisobnoma.platform.mobile.push.dto.PushSendResult;
import com.hisobnoma.platform.mobile.push.dto.SendNotificationRequest;
import com.hisobnoma.platform.mobile.push.service.PushSendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Staff-facing broadcast of push notifications to the tenant's app users (or a single user).
 */
@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final PushSendService pushSendService;

    @PostMapping("/send")
    @PreAuthorize("hasAuthority('MOBILE_PUSH_SEND')")
    public ResponseEntity<ApiResponse<PushSendResult>> send(@Valid @RequestBody SendNotificationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(pushSendService.send(request)));
    }
}
