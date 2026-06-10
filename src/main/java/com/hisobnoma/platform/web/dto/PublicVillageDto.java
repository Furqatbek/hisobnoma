package com.hisobnoma.platform.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicVillageDto {

    private Long id;
    private String name;
    private Long regionId;
}
