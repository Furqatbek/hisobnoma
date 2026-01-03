package com.hisobnoma.platform.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVendorContactRequest {

    @NotNull(message = "Vendor ID is required")
    private Long vendorId;

    @NotBlank(message = "Contact name is required")
    @Size(max = 100, message = "Contact name cannot exceed 100 characters")
    private String name;

    @Size(max = 100, message = "Title cannot exceed 100 characters")
    private String title;

    @Size(max = 100, message = "Department cannot exceed 100 characters")
    private String department;

    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    @Size(max = 50, message = "Phone cannot exceed 50 characters")
    private String phone;

    @Size(max = 50, message = "Mobile phone cannot exceed 50 characters")
    private String mobilePhone;

    @Size(max = 50, message = "Fax cannot exceed 50 characters")
    private String fax;

    @Builder.Default
    private boolean primary = false;

    @Builder.Default
    private boolean billingContact = false;

    @Builder.Default
    private boolean orderingContact = false;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;
}
