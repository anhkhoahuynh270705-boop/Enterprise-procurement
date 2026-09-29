package com.example.shopping.supplier.proposal.dto.request;

import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

public record SupplierProposalRequest(
    @NotBlank(message = "Tên nhà cung cấp không được để trống")
    @Size(min = 2, max = 200, message = "Tên nhà cung cấp phải có 2–200 ký tự")

    @Pattern(regexp = "\\p{L}+(?: \\p{L}+)*", message = "Tên nhà cung cấp chỉ được chứa chữ và một khoảng trắng giữa các từ") String name,
    @NotBlank(message = "Mã số thuế không được để trống")

    @Pattern(regexp = "[0-9]{10}([0-9]{3})?", message = "Mã số thuế phải có 10 hoặc 13 chữ số") String taxCode,

    @Pattern(regexp = "^$|\\p{L}+(?: \\p{L}+)*", message = "Người liên hệ chỉ được chứa chữ và một khoảng trắng giữa các từ")
    @Size(max = 200, message = "Người liên hệ tối đa 200 ký tự") String contactPerson,

    @Email(message = "Email không hợp lệ") @Size(max = 254, message = "Email tối đa 254 ký tự") String email,

    @Pattern(regexp = "^$|[0-9]{9,15}", message = "Điện thoại gồm 9–15 chữ số, không có chữ hoặc khoảng trắng") String phone,

    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự") String address,

    @Pattern(regexp = "^$|\\p{L}+(?: \\p{L}+)*", message = "Tỉnh / Thành phố chỉ được chứa chữ và một khoảng trắng giữa các từ")
    @Size(max = 255, message = "Tỉnh / Thành phố tối đa 255 ký tự") String city,

    @Pattern(regexp = "^$|\\p{L}+(?: \\p{L}+)*", message = "Quốc gia chỉ được chứa chữ và một khoảng trắng giữa các từ")
    @Size(max = 255, message = "Quốc gia tối đa 255 ký tự") String country,

    @Pattern(regexp = "^$|\\p{L}+(?: \\p{L}+)*", message = "Tên ngân hàng chỉ được chứa chữ và một khoảng trắng giữa các từ")
    @Size(max = 255, message = "Tên ngân hàng tối đa 255 ký tự") String bankName,

    @Pattern(regexp = "^$|[0-9]{6,50}", message = "Số tài khoản gồm 6–50 chữ số, không có chữ hoặc khoảng trắng") String bankAccount,
    @Size(max = 2000, message = "Ghi chú tối đa 2000 ký tự") String notes,

    @NotBlank(message = "Vui lòng nhập lý do đề xuất")
    @Size(min = 10, max = 2000, message = "Lý do phải có 10–2000 ký tự") String reason
) {
    public SupplierProposalRequest {
        name = clean(name); 
        taxCode = clean(taxCode); 
        contactPerson = clean(contactPerson);
        email = clean(email); 
        phone = clean(phone); 
        address = clean(address); 
        reason = clean(reason);
        city = clean(city); 
        country = clean(country); 
        bankName = clean(bankName);
        bankAccount = clean(bankAccount);
        notes = clean(notes);
    }

    static String clean(String value) { 
        return value == null ? "" : value.strip(); 
    }

    @JsonIgnore
    @AssertTrue(message = "Cần ít nhất email hoặc số điện thoại liên hệ")
    public boolean isContactProvided() { 
        return !email.isBlank() || !phone.isBlank(); 
    }

    @JsonIgnore
    @AssertTrue(message = "Tên ngân hàng và số tài khoản phải được nhập cùng nhau")
    public boolean isBankDetailsComplete() {
        return bankName.isBlank() == bankAccount.isBlank();
    }
}
