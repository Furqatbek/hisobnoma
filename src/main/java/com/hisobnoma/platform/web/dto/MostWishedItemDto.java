package com.hisobnoma.platform.web.dto;

import com.hisobnoma.platform.web.entity.WebCatalogStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MostWishedItemDto {

    private Long catalogItemId;
    private String productName;
    private long likeCount;
    private BigDecimal effectivePrice;
    private BigDecimal salePrice;
    private WebCatalogStatus status;
}
