package com.hisobnoma.platform.mobile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Lightweight employee entry for the mobile salary/advance payee picker. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileEmployeeOption {
    private Long id;
    private String name;
    private String position;
    private String code;
}
