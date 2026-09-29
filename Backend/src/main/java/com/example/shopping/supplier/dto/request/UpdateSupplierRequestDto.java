package com.example.shopping.supplier.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.example.shopping.supplier.enums.SupplierStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSupplierRequestDto {

    @NotBlank(message = "Tên nhà cung cấp không được để trống")
    @Size(max = 200, message = "Tên không được vượt quá 200 ký tự")
    private String name;

    private String contactPerson;

    @Email(message = "Email không hợp lệ")
    private String email;

    private String phone;
    private String address;
    private String city;
    private String country;
    private String taxCode;
    private String bankAccount;
    private String bankName;
    private String notes;
    private SupplierStatus status;
}
