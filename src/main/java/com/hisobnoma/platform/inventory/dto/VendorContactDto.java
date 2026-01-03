package com.hisobnoma.platform.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorContactDto {
    private Long id;
    private Long vendorId;
    private String name;
    private String title;
    private String department;
    private String email;
    private String phone;
    private String mobilePhone;
    private String fax;
    private boolean primary;
    private boolean billingContact;
    private boolean orderingContact;
    private String notes;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
