package com.hisobnoma.platform.distribution.dto;

import com.hisobnoma.platform.distribution.entity.AgentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class CreateDistributionAgentRequest {

    @NotBlank(message = "Code is required")
    @Size(max = 50, message = "Code must not exceed 50 characters")
    private String code;

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    @Size(max = 50, message = "Phone must not exceed 50 characters")
    private String phone;

    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @Size(max = 500, message = "Photo URL must not exceed 500 characters")
    private String photoUrl;

    private Long userId;

    private Long employeeId;

    @Size(max = 50, message = "Vehicle plate must not exceed 50 characters")
    private String vehiclePlate;

    @Size(max = 200, message = "Vehicle name must not exceed 200 characters")
    private String vehicleName;

    @DecimalMin(value = "0", message = "Commission must be non-negative")
    @DecimalMax(value = "100", message = "Commission must not exceed 100")
    private BigDecimal commissionPercent;

    private AgentStatus status;

    private LocalDate hiredAt;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    @Valid
    private List<DistributionTerritoryRequest> territories;
}
