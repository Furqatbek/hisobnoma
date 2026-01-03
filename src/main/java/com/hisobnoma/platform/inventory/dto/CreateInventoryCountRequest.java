package com.hisobnoma.platform.inventory.dto;

import com.hisobnoma.platform.inventory.entity.CountType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInventoryCountRequest {

    @NotNull(message = "Location ID is required")
    private Long locationId;

    @NotNull(message = "Count type is required")
    private CountType countType;

    @NotNull(message = "Count date is required")
    private LocalDate countDate;

    @Size(max = 200, message = "Description must not exceed 200 characters")
    private String description;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    private boolean freezeStock;

    private Long categoryId;

    private List<Long> productIds;
}
