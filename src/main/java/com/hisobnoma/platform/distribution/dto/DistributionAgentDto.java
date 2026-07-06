package com.hisobnoma.platform.distribution.dto;

import com.hisobnoma.platform.distribution.entity.AgentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistributionAgentDto {
    private Long id;
    private String code;
    private String name;
    private String phone;
    private String email;
    private String photoUrl;
    private Long userId;
    private Long employeeId;
    private String vehiclePlate;
    private String vehicleName;
    private BigDecimal commissionPercent;
    private AgentStatus status;
    private LocalDate hiredAt;
    private LocalDate terminatedAt;
    private String notes;
    private List<DistributionTerritoryDto> territories;
}
