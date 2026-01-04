package com.hisobnoma.platform.pos.dto;

import com.hisobnoma.platform.pos.enums.PriceListType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePriceListRequest {

    @NotBlank(message = "Code is required")
    @Size(max = 50, message = "Code cannot exceed 50 characters")
    private String code;

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Type is required")
    private PriceListType type;

    private String currency;
    private Integer priority;
    private BigDecimal defaultMarkupPercent;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean defaultPriceList;
    private Long locationId;
    private String notes;
}
