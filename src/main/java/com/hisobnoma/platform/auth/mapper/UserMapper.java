package com.hisobnoma.platform.auth.mapper;

import com.hisobnoma.platform.auth.dto.UserDto;
import com.hisobnoma.platform.auth.entity.Role;
import com.hisobnoma.platform.auth.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "fullName", expression = "java(user.getFullName())")
    @Mapping(target = "roles", source = "roles", qualifiedByName = "rolesToCodes")
    UserDto toDto(User user);

    @Named("rolesToCodes")
    default Set<String> rolesToCodes(Set<Role> roles) {
        if (roles == null) {
            return Set.of();
        }
        return roles.stream()
                .map(Role::getCode)
                .collect(Collectors.toSet());
    }
}
