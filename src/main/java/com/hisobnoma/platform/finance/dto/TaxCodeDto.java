package com.hisobnoma.platform.finance.dto;

import com.hisobnoma.platform.finance.entity.TaxType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxCodeDto {
    private Long id;
    private String code;
    private String name;
    private String description;
    private TaxType taxType;
    private boolean inclusive;
    private boolean compound;
    private boolean appliesToSales;
    private boolean appliesToPurchases;
    private Long salesAccountId;
    private String salesAccountCode;
    private String salesAccountName;
    private Long purchaseAccountId;
    private String purchaseAccountCode;
    private String purchaseAccountName;
    private String taxAuthority;
    private String taxRegistrationNumber;
    private String reportCode;
    private boolean active;
    private boolean systemCode;
    private Integer sortOrder;
    private String notes;
    private List<TaxRateDto> rates;
    private Instant createdAt;
    private Instant updatedAt;
}
