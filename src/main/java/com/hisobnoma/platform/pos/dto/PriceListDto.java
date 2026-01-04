package com.hisobnoma.platform.pos.dto;

import com.hisobnoma.platform.pos.enums.PriceListType;
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
public class PriceListDto {
    private Long id;
    private String code;
    private String name;
    private String description;
    private PriceListType type;
    private String currency;
    private Integer priority;
    private BigDecimal defaultMarkupPercent;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active;
    private boolean defaultPriceList;
    private Long locationId;
    private String locationName;
    private String notes;
    private List<PriceListItemDto> items;
    private Integer itemCount;
    private Integer customerCount;
}
