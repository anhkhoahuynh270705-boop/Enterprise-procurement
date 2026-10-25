package com.example.shopping.supplier.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
    @Size(min = 2, max = 200, message = "Tên nhà cung cấp phải có 2–200 ký tự")
    @Pattern(regexp = "(?:\\p{L}+|\\(\\p{L}+(?: \\p{L}+)*\\))(?: (?:\\p{L}+|\\(\\p{L}+(?: \\p{L}+)*\\)))*", message = "Tên nhà cung cấp chỉ được chứa chữ, dấu ngoặc đơn và khoảng trắng giữa các từ")
    private String name;

    private String contactPerson;

    @Email(message = "Email không hợp lệ")
    private String email;

    @Pattern(regexp = "^$|(?=(?:[^0-9]*[0-9]){8,15}[^0-9]*$)[0-9 ]+", message = "Điện thoại gồm 8–15 chữ số và chỉ được chứa khoảng trắng")
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
