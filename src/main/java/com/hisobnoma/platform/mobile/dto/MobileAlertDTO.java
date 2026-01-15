package com.hisobnoma.platform.mobile.dto;

import com.hisobnoma.platform.mobile.entity.MobileAlert;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Mobile alert DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MobileAlertDTO {

    private Long id;
    private MobileAlert.AlertType alertType;
    private String title;
    private String message;
    private MobileAlert.AlertPriority priority;
    private String entityType;
    private Long entityId;
    private String actionUrl;
    private String data;
    private boolean read;
    private Instant readAt;
    private Instant expiresAt;
    private Instant createdAt;
}
