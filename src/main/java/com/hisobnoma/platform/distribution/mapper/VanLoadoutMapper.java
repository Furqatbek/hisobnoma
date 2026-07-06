package com.hisobnoma.platform.distribution.mapper;

import com.hisobnoma.platform.distribution.dto.VanLoadoutDto;
import com.hisobnoma.platform.distribution.dto.VanLoadoutLineDto;
import com.hisobnoma.platform.distribution.entity.VanLoadout;
import com.hisobnoma.platform.distribution.entity.VanLoadoutLine;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VanLoadoutMapper {

    VanLoadoutDto toDto(VanLoadout loadout);

    VanLoadoutLineDto toDto(VanLoadoutLine line);

    List<VanLoadoutDto> toDtoList(List<VanLoadout> loadouts);
}
