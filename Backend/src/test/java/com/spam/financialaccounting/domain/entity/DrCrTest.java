package com.spam.financialaccounting.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for the single canonical DR/CR normalizer.
 *
 * Every write path (FASubGroup create/update, journal-voucher posting, opening
 * balance import) funnels its debit/credit input through
 * {@link DrCr#normalizeOrNull(String)}, so proving the five required mixed-case
 * inputs here proves the behaviour shared by all of those endpoints.
 */
class DrCrTest {

    // The exact mixed-case inputs called out by the task, plus the already-canonical forms.
    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @CsvSource({
            "dr, DR",
            "Dr, DR",
            "dR, DR",
            "DR, DR",
            "cr, CR",
            "Cr, CR",
            "cR, CR",
            "CR, CR",
    })
    @DisplayName("normalizeOrNull canonicalizes any-case DR/CR to uppercase")
    void normalizeOrNull_canonicalizesAnyCase(String input, String expected) {
        assertThat(DrCr.normalizeOrNull(input)).isEqualTo(expected);
        assertThat(DrCr.isValid(input)).isTrue();
    }

    @Test
    @DisplayName("normalizeOrNull trims surrounding whitespace before canonicalizing")
    void normalizeOrNull_trimsWhitespace() {
        assertThat(DrCr.normalizeOrNull("  dr  ")).isEqualTo("DR");
        assertThat(DrCr.normalizeOrNull("\tCr\n")).isEqualTo("CR");
    }

    @ParameterizedTest
    @ValueSource(strings = { "XX", "D", "DRR", "debit", "credit", "0", "drcr", "" })
    @NullSource
    @DisplayName("normalizeOrNull returns null and isValid is false for non-DR/CR input")
    void normalizeOrNull_rejectsInvalid(String input) {
        assertThat(DrCr.normalizeOrNull(input)).isNull();
        assertThat(DrCr.isValid(input)).isFalse();
    }

    @Test
    @DisplayName("Canonical constants are the uppercase DR/CR literals")
    void canonicalConstants() {
        assertThat(DrCr.DEBIT).isEqualTo("DR");
        assertThat(DrCr.CREDIT).isEqualTo("CR");
    }
}
