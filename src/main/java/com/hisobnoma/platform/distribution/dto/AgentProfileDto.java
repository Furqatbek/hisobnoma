package com.hisobnoma.platform.distribution.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** The agent's own profile for the mobile app's "me" screen. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentProfileDto {

    private Long agentId;
    private String code;
    private String name;
    private String phone;
    private String vehiclePlate;
    private String vehicleName;
    private String status;
}
