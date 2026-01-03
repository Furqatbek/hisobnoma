package com.hisobnoma.platform.auth.mapper;

import com.hisobnoma.platform.auth.dto.RoleDto;
import com.hisobnoma.platform.auth.entity.Permission;
import com.hisobnoma.platform.auth.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    @Mapping(target = "permissions", source = "permissions", qualifiedByName = "permissionsToCodes")
    RoleDto toDto(Role role);

    @Named("permissionsToCodes")
    default Set<String> permissionsToCodes(Set<Permission> permissions) {
        if (permissions == null) {
            return Set.of();
        }
        return permissions.stream()
                .map(Permission::getCode)
                .collect(Collectors.toSet());
    }
}
