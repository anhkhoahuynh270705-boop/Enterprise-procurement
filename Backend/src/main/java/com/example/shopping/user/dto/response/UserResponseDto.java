package com.example.shopping.user.dto.response;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {
    private UUID id;
    private String username;
    private String email;
    private String firstName;
    private String fullName;
    private String lastName;
    private String role;
    private Boolean enabled;
    private String avatarUrl;
    private String phone;
    private LocalDate dateOfBirth;
    private String gender;
    private String address;
    private String employeeId;
    private String department;
    private String jobTitle;
    private String officeLocation;
    private String manager;
    private String employeeStatus;

}
