package com.hisobnoma.platform.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class POSTerminalDto {
    private Long id;
    private String terminalCode;
    private String name;
    private String description;
    private Long locationId;
    private String locationName;
    private boolean active;
    private String ipAddress;
    private String macAddress;
    private String deviceType;
    private String printerName;
    private boolean cashDrawerEnabled;
    private boolean allowOffline;
    private Instant lastSyncAt;
    private Long currentShiftId;
    private Instant createdAt;
    private Instant updatedAt;
}
