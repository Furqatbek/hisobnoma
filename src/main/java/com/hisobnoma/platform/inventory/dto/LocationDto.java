package com.hisobnoma.platform.inventory.dto;

import com.hisobnoma.platform.inventory.entity.LocationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationDto {

    private Long id;
    private String code;
    private String name;
    private String description;
    private LocationType locationType;
    private Long parentLocationId;
    private String parentLocationName;
    private String address;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private String phone;
    private String email;
    private String managerName;
    private boolean active;
    private boolean isDefault;
    private boolean allowNegativeStock;
    private Integer sortOrder;
    private String fullPath;
    private int level;
    private List<LocationDto> children;
}
