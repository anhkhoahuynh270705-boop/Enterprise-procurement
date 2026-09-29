package com.example.shopping.user.mapper;

import org.mapstruct.*;
import com.example.shopping.common.enums.Role;
import com.example.shopping.user.dto.request.CreateUserRequestDto;
import com.example.shopping.user.dto.response.UserResponseDto;
import com.example.shopping.user.entity.UserEntity;
import com.example.shopping.user.normalization.UserNameNormalizer;
@Mapper(componentModel = "spring", imports = {Role.class, UserNameNormalizer.class},
    builder = @Builder(disableBuilder = true), unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "username", source = "dto.username")
    @Mapping(target = "email", source = "dto.email")
    @Mapping(target = "firstName", source = "dto.firstName")
    @Mapping(target = "fullName", expression = "java(UserNameNormalizer.resolveFullName(dto.getFullName(), dto.getFirstName(), dto.getLastName()))" )
    @Mapping(target = "lastName", source = "dto.lastName")
    @Mapping(target = "password", source = "encodedPassword")
    @Mapping(target = "enabled", source = "dto.enabled", defaultValue = "true")
    @Mapping(target = "role", expression = "java(Role.parse(dto.getRole()).name())")
    @Mapping(target = "emailVerificationRequired", constant = "true")
    @Mapping(target = "employeeStatus", source = "dto.employeeStatus", defaultValue = "ACTIVE")
    UserEntity toEntity(CreateUserRequestDto dto, String encodedPassword);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "username", source = "username")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "firstName", source = "firstName")
    @Mapping(target = "fullName", source = "fullName")
    @Mapping(target = "lastName", source = "lastName")
    @Mapping(target = "enabled", source = "enabled")
    @Mapping(target = "role", expression = "java(Role.parse(entity.getRole()).name())")
    UserResponseDto toDto(UserEntity entity);
}
