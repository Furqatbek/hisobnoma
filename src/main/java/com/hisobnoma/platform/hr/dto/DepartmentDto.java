package com.hisobnoma.platform.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentDto {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Long parentId;
    private String parentName;
    private Long managerId;
    private String managerName;
    private boolean active;
}
