package com.example.shopping.user.normalization;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class UserNameNormalizer {
    private UserNameNormalizer() { }

    public static String resolveFullName(String fullName, String firstName, String lastName) {
        if (fullName != null && !fullName.isBlank()) return fullName.trim();
        String name = Stream.of(lastName, firstName)
            .filter(part -> part != null && !part.isBlank())
            .map(String::trim).collect(Collectors.joining(" "));
        return name.isBlank() ? "Chưa cập nhật họ tên" : name;
    }
}
