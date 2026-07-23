package com.hisobnoma.platform.distribution.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Token + minimal identity returned after a successful agent OTP verify. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentAuthResponse {

    private String token;
    private Long agentId;
    private String code;
    private String name;
    private String phone;
}
