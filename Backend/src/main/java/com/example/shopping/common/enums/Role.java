package com.example.shopping.common.enums;
import java.util.Locale;
public enum Role {
    ADMIN,
    USER,
    CHECKER;

    public static Role parse(String value) {
        if (value == null || value.isBlank()) 
            return USER;
        String normalized = value.trim().toUpperCase(Locale.ROOT).replaceFirst("^ROLE_", "");
        if ("MAKER".equals(normalized)) 
            normalized = "USER";
        return Role.valueOf(normalized);
    }
}
