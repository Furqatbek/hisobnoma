package com.hisobnoma.platform.inventory.dto;

import com.hisobnoma.platform.inventory.entity.SerialNumber;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SerialNumberDto {

    private Long id;
    private Long productId;
    private String productSku;
    private String productName;
    private Long productVariantId;
    private String variantName;
    private Long locationId;
    private String locationCode;
    private String locationName;
    private String serialNumber;
    private SerialNumber.SerialStatus status;
    private Long batchId;
    private String batchNumber;
    private BigDecimal unitCost;
    private LocalDate manufactureDate;
    private LocalDate expiryDate;
    private LocalDate warrantyStartDate;
    private LocalDate warrantyEndDate;
    private LocalDateTime receivedDate;
    private LocalDateTime soldDate;
    private Long supplierId;
    private Long customerId;
    private Long receivingOrderId;
    private Long salesTransactionId;
    private String manufacturerSerial;
    private String imei;
    private String macAddress;
    private String notes;
    private boolean reserved;
    private Long reservedForOrderId;
    private boolean underWarranty;
    private Long warrantyDaysRemaining;
}
