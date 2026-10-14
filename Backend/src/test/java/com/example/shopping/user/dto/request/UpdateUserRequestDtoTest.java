package com.example.shopping.user.dto.request;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class UpdateUserRequestDtoTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void optionalPasswordAcceptsBlankOrAtLeastSixCharacters() {
        assertPasswordValid(null);
        assertPasswordValid("");
        assertPasswordValid("      ");
        assertPasswordValid("123456");
    }

    @Test
    void optionalPasswordRejectsNonBlankValuesShorterThanSixCharacters() {
        UpdateUserRequestDto request = validRequest();
        request.setPassword("12345");

        Set<String> invalidProperties = validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());

        assertFalse(invalidProperties.isEmpty());
        assertTrue(invalidProperties.contains("password"));
    }

    private static void assertPasswordValid(String password) {
        UpdateUserRequestDto request = validRequest();
        request.setPassword(password);

        Set<String> invalidProperties = validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());

        assertFalse(invalidProperties.contains("password"));
    }

    private static UpdateUserRequestDto validRequest() {
        UpdateUserRequestDto request = new UpdateUserRequestDto();
        request.setUsername("admin");
        request.setEmail("admin@example.com");
        return request;
    }
}
