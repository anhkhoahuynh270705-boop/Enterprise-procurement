package com.example.shopping.supplier.util;

import java.text.Normalizer;
import java.util.Locale;

public final class SupplierNameNormalizer {

    private SupplierNameNormalizer() {}

    public static String normalize(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFKC)
                .replaceAll("[\\p{Z}\\s]+", " ")
                .trim();
        return normalized.toLowerCase(Locale.ROOT);
    }
}
