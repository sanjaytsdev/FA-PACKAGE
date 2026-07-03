package com.spam.financialaccounting.application.usecases.fasubgroup;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.spam.financialaccounting.domain.entity.FAGroup;
import com.spam.financialaccounting.presentation.exception.fasubgroup.InvalidNormalBalanceException;

/**
 * Checks {@link AccountTypeRules#assertNormalBalanceMatchesType} holds the right
 * natural balance side for each account category: Asset and Expense are DR,
 * Liability, Equity and Revenue are CR.
 */
class AccountTypeRulesTest {

    private static FAGroup group(String aType) {
        return new FAGroup("0" + aType, "Group", aType, BigDecimal.ZERO);
    }

    @ParameterizedTest(name = "A_TYPE {0} ({1}) accepts normal balance {2}")
    @CsvSource({
            "0, Asset, DR",
            "4, Expense, DR",
            "1, Liability, CR",
            "2, Equity, CR",
            "3, Revenue, CR"
    })
    @DisplayName("Should accept the natural balance side for each account category")
    void shouldAccept_NaturalBalanceSide(String aType, String name, String drCr) {
        assertThatCode(() -> AccountTypeRules.assertNormalBalanceMatchesType(drCr, group(aType)))
                .doesNotThrowAnyException();
    }

    // Mixed case should work here too, like every other DR/CR entry point.
    @ParameterizedTest(name = "A_TYPE {0} accepts normal balance \"{1}\"")
    @CsvSource({ "0, dr", "4, Dr", "1, cr", "2, cR", "3, Cr" })
    @DisplayName("Should accept the natural balance side case-insensitively")
    void shouldAccept_NaturalBalanceSide_CaseInsensitive(String aType, String drCr) {
        assertThatCode(() -> AccountTypeRules.assertNormalBalanceMatchesType(drCr, group(aType)))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "A_TYPE {0} ({1}) rejects wrong normal balance {2}")
    @CsvSource({
            "0, Asset, CR",
            "4, Expense, CR",
            "1, Liability, DR",
            "2, Equity, DR",
            "3, Revenue, DR"
    })
    @DisplayName("Should reject the wrong balance side for each account category")
    void shouldReject_WrongBalanceSide(String aType, String name, String drCr) {
        assertThatThrownBy(() -> AccountTypeRules.assertNormalBalanceMatchesType(drCr, group(aType)))
                .isInstanceOf(InvalidNormalBalanceException.class)
                .hasMessageContaining(name)
                .hasMessageContaining("normal balance");
    }
}
