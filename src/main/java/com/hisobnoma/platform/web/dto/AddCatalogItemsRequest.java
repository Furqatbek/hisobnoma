package com.hisobnoma.platform.web.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddCatalogItemsRequest {

    @NotEmpty(message = "At least one product ID is required")
    private List<Long> productIds;
}
