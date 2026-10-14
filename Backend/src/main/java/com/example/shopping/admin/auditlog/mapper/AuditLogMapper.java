package com.example.shopping.admin.auditlog.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.shopping.admin.auditlog.dto.AuditLogDto;
import com.example.shopping.admin.auditlog.entity.AuditLogEntry;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    @Mapping(target = "details", expression = "java(buildDetails(e))")
    AuditLogDto toDto(AuditLogEntry e);

    default AuditLogDto.Details buildDetails(AuditLogEntry e) {
        boolean hasDetails = e.getHttpMethod()     != null
                          || e.getHttpStatus()     != null
                          || e.getUserAgent()      != null
                          || e.getPayloadSummary() != null;

        if (!hasDetails) return null;

        return new AuditLogDto.Details(
                e.getUserAgent(),
                e.getHttpMethod(),
                e.getHttpStatus(),
                e.getResponseTimeMs(),
                e.getPayloadSummary()
        );
    }
}
