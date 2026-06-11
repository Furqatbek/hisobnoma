package com.hisobnoma.platform.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Staff-facing view of an online customer account.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebCustomerDto {

    private Long id;
    private String phone;
    private String name;
    private Instant verifiedAt;
    private Instant lastLoginAt;
    private Long customerId;
    private String customerCode;
    private String customerName;
    private long orderCount;
    private Instant lastOrderAt;
    private boolean smsOptOut;
    private String referralCode;
    private Long referredBy;
    private String referredByName;
    private long referredCount;
    private boolean pushReachable;
}
