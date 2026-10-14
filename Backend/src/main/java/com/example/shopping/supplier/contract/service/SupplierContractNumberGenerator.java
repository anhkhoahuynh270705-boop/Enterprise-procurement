package com.example.shopping.supplier.contract.service;

import com.example.shopping.common.enums.ErrorEnum;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.example.shopping.supplier.contract.repository.SupplierContractNumberCounterRepository;
import com.example.shopping.supplier.contract.repository.SupplierContractRepository;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.RequiredArgsConstructor;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class SupplierContractNumberGenerator {

    SupplierContractNumberCounterRepository counterRepository;
    SupplierContractRepository contracts;

    public String nextFor(int year) {
        String contractNumber;
        do {
            Long nextNumber = counterRepository.nextNumberForYear(year);
            if (nextNumber == null) {
                throw ErrorEnum.SUPPLIER_CONTRACT_NUMBER_FAILED.ex();
            }
            contractNumber = String.format(Locale.ROOT, "HĐMB-%d-%04d", year, nextNumber);
        } while (contracts.existsByContractNumberIgnoreCase(contractNumber));
        return contractNumber;
    }
}
