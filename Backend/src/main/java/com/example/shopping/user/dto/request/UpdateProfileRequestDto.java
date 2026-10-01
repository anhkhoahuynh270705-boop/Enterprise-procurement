package com.example.shopping.user.dto.request;

import org.hibernate.validator.constraints.URL;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequestDto {
    @Size(max = 2048)
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

    @JsonAnySetter
    public void rejectUnsupportedField(String name, Object value) {
        throw new IllegalArgumentException("Field is not editable in your profile: " + name);
    }
}
