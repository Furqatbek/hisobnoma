package com.hisobnoma.platform.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerContactDto {
    private Long id;
    private Long customerId;
    private String name;
    private String title;
    private String department;
    private String email;
    private String phone;
    private String mobilePhone;
    private String fax;
    private boolean primary;
    private boolean billingContact;
    private boolean shippingContact;
    private String notes;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
