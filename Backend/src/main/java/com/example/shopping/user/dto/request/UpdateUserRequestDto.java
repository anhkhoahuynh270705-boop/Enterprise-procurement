package com.example.shopping.user.dto.request;

import org.hibernate.validator.constraints.URL;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequestDto {
    @NotBlank(message = "Username không được để trống")
    @Size(min = 3, max = 50, message = "Username phải từ 3 đến 50 ký tự")
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    private String firstName;
    
    @Size(max = 255)
    private String fullName;

    private String lastName;

    @Pattern(regexp = "ADMIN|USER|CHECKER", message = "Role must be ADMIN, USER or CHECKER")
    private String role;

    private Boolean enabled;

    private String password;

    @Size(max = 2048)
    @URL(protocol = "https")
    private String avatarUrl;

    @Size(max = 30)
    @Pattern(regexp = "[+0-9().\\s-]*", message = "Invalid phone number")
    private String phone;

    @PastOrPresent(message = "Date of birth cannot be in the future")
    private java.time.LocalDate dateOfBirth;

    @Pattern(regexp = "MALE|FEMALE|OTHER|UNDISCLOSED", message = "Invalid gender")
    private String gender;

    @Size(max = 500)
    private String address;

    @Size(max = 50)
    private String employeeId;

    @Size(max = 255)
    private String department;

    @Size(max = 255)
    private String jobTitle;

    @Size(max = 255)
    private String officeLocation;

    @Size(max = 255)
    private String manager;

    @Pattern(regexp = "ACTIVE|INACTIVE", message = "Invalid employee status")
    private String employeeStatus;


   
}
