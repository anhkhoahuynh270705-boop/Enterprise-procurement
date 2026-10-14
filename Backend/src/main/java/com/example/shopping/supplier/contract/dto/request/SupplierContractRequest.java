package com.example.shopping.supplier.contract.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.example.shopping.supplier.contract.enums.SupplierContractStatus;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupplierContractRequest {

    @NotBlank
    @Size(min = 2, max = 255, message = "Tên hợp đồng phải có từ 2 đến 255 ký tự.")
    private String title;

    @NotNull
    private UUID supplierId;

    private LocalDate signedDate;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @NotNull
    @DecimalMin(value = "0.00", message = "Giá trị hợp đồng không được âm.")
    @Digits(integer = 13, fraction = 2, message = "Giá trị hợp đồng tối đa 13 chữ số nguyên và 2 chữ số thập phân.")
    private BigDecimal amount;

    @NotBlank
    @Pattern(regexp = "[A-Z]{3}", message = "Mã tiền tệ phải gồm đúng 3 chữ cái in hoa (ví dụ: VND, USD).")
    private String currency;

    @NotNull
    private SupplierContractStatus status;

    @Size(max = 5000)
    private String notes;
}
