package com.hisobnoma.platform.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserListItem {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String fullName;
    private String initials;
    private boolean hasPin;
}
