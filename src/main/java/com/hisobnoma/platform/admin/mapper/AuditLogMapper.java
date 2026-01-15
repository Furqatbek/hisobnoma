package com.hisobnoma.platform.admin.mapper;

import com.hisobnoma.platform.admin.dto.AuditLogDTO;
import com.hisobnoma.platform.admin.entity.AuditLog;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    AuditLogDTO toDto(AuditLog entity);

    List<AuditLogDTO> toDtoList(List<AuditLog> entities);
}
