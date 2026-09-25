package com.bank.transaction;

import com.bank.common.dto.MutationRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ValidationAndBoundaryTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Boundary Test: Amount <= 0 should fail @Positive validation")
    void testNegativeOrZeroAmountFails() {
        MutationRequest request = new MutationRequest(
                1L, null, new BigDecimal("-50.00"), "PHP", "DEBIT", "DEBIT", "IDEM-001");

        Set<ConstraintViolation<MutationRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Violations should exist for negative amount");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("mutationAmount")));
    }

    @Test
    @DisplayName("Boundary Test: Fraction places > 4 should fail @Digits validation")
    void testFractionPlacesExceedingFourFails() {
        MutationRequest request = new MutationRequest(
                1L, null, new BigDecimal("100.12345"), "PHP", "DEBIT", "DEBIT", "IDEM-002");

        Set<ConstraintViolation<MutationRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Violations should exist for 5 decimal places");
    }

    @Test
    @DisplayName("Boundary Test: Valid 4-decimal place positive balance succeeds validation")
    void testValidPrecisionSucceeds() {
        MutationRequest request = new MutationRequest(
                1L, null, new BigDecimal("125450.5000"), "PHP", "CREDIT", "CREDIT", "IDEM-003");

        Set<ConstraintViolation<MutationRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "No violations for valid precision");
    }
}
