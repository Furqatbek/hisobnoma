package com.hisobnoma.platform.pos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTerminalRequest {

    @NotBlank(message = "Terminal code is required")
    @Size(max = 50, message = "Terminal code must not exceed 50 characters")
    private String terminalCode;

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Location ID is required")
    private Long locationId;

    @Size(max = 50, message = "IP address must not exceed 50 characters")
    private String ipAddress;

    @Size(max = 50, message = "MAC address must not exceed 50 characters")
    private String macAddress;

    @Size(max = 50, message = "Device type must not exceed 50 characters")
    private String deviceType;

    @Size(max = 100, message = "Printer name must not exceed 100 characters")
    private String printerName;

    @Builder.Default
    private boolean cashDrawerEnabled = true;

    @Builder.Default
    private boolean allowOffline = false;
}
