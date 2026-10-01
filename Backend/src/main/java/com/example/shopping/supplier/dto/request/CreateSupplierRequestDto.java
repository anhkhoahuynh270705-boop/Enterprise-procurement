package com.example.shopping.supplier.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSupplierRequestDto {

    @NotBlank(message = "Tên nhà cung cấp không được để trống")
    @Size(min = 2, max = 200, message = "Tên nhà cung cấp phải có 2–200 ký tự")
    @Pattern(regexp = "\\p{L}+(?: \\p{L}+)*", message = "Tên nhà cung cấp chỉ được chứa chữ và một khoảng trắng giữa các từ")
    private String name;

    @Pattern(regexp = "^$|\\p{L}+(?: \\p{L}+)*", message = "Người liên hệ chỉ được chứa chữ và một khoảng trắng giữa các từ")
    @Size(max = 200, message = "Người liên hệ tối đa 200 ký tự")
    private String contactPerson;

    @Email(message = "Email không hợp lệ")
    @Size(max = 254, message = "Email tối đa 254 ký tự")
    private String email;

    @Pattern(regexp = "^$|[0-9]{9,15}", message = "Điện thoại gồm 9–15 chữ số, không có chữ hoặc khoảng trắng")
    private String phone;

    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
    private String address;

    @Pattern(regexp = "^$|\\p{L}+(?: \\p{L}+)*", message = "Tỉnh / Thành phố chỉ được chứa chữ và một khoảng trắng giữa các từ")
    @Size(max = 255, message = "Tỉnh / Thành phố tối đa 255 ký tự")
    private String city;

    @Pattern(regexp = "^$|\\p{L}+(?: \\p{L}+)*", message = "Quốc gia chỉ được chứa chữ và một khoảng trắng giữa các từ")
    @Size(max = 255, message = "Quốc gia tối đa 255 ký tự")
    private String country;

    @NotBlank(message = "Mã số thuế không được để trống")
    @Pattern(regexp = "[0-9]{10}([0-9]{3})?", message = "Mã số thuế phải có 10 hoặc 13 chữ số")
    private String taxCode;

    @Pattern(regexp = "^$|[0-9]{6,50}", message = "Số tài khoản gồm 6–50 chữ số, không có chữ hoặc khoảng trắng")
    private String bankAccount;

    @Pattern(regexp = "^$|\\p{L}+(?: \\p{L}+)*", message = "Tên ngân hàng chỉ được chứa chữ và một khoảng trắng giữa các từ")
    @Size(max = 50, message = "Tên ngân hàng tối đa 50 ký tự")
    private String bankName;

    @Size(max = 2000, message = "Ghi chú tối đa 2000 ký tự")
    private String notes;

    @JsonIgnore
    @AssertTrue(message = "Cần ít nhất email hoặc số điện thoại liên hệ")
    public boolean isContactProvided() {
        return hasText(email) || hasText(phone);
    }

    @JsonIgnore
    @AssertTrue(message = "Tên ngân hàng và số tài khoản phải được nhập cùng nhau")
    public boolean isBankDetailsComplete() {
        return hasText(bankName) == hasText(bankAccount);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
