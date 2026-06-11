package com.hisobnoma.platform.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebNotificationDto {
    private Long id;
    private String title;
    private String body;
    private String type;
    private String referenceType;
    private String referenceId;
    private boolean read;
    private Instant createdAt;
}
