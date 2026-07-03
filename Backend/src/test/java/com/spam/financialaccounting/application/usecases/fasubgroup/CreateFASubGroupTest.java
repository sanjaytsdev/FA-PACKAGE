package com.spam.financialaccounting.application.usecases.fasubgroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spam.financialaccounting.domain.entity.FAGroup;
import com.spam.financialaccounting.domain.entity.FASubGroup;
import com.spam.financialaccounting.domain.repository.FAGroupRepository;
import com.spam.financialaccounting.domain.repository.FASubGroupRepository;
import com.spam.financialaccounting.infrastructure.persistence.repository.AuditLogRepository;
import com.spam.financialaccounting.presentation.exception.fagroup.FAGroupNotFoundException;
import com.spam.financialaccounting.presentation.exception.fasubgroup.FASubGroupAlreadyExistsException;
import com.spam.financialaccounting.presentation.exception.fasubgroup.FASubGroupValidationException;
import com.spam.financialaccounting.presentation.exception.fasubgroup.InvalidNormalBalanceException;

@ExtendWith(MockitoExtension.class)
public class CreateFASubGroupTest {

    @Mock
    private FASubGroupRepository subGroupRepository;

    @Mock
    private FAGroupRepository groupRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private CreateFASubGroup createFASubGroup;

    private FAGroup parentGroup;
    private FASubGroup validSubGroup;

    @BeforeEach
    void setUp() {
        parentGroup = new FAGroup("01", "Asset", "0", BigDecimal.ZERO);
        validSubGroup = new FASubGroup("10001", "Cash", "01", "00", new BigDecimal("1000.00"), "DR", "T");
    }

    @Test
    @DisplayName("Should successfully create FASubGroup when input is valid")
    void shouldCreateFASubGroup_WhenValidInput() {
        // ARRANGE
        when(groupRepository.findByCode("01")).thenReturn(Optional.of(parentGroup));
        when(subGroupRepository.existsByCode("10001")).thenReturn(false);
        when(subGroupRepository.save(validSubGroup)).thenReturn(validSubGroup);

        // ACT
        FASubGroup result = createFASubGroup.execute(validSubGroup);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getSCode()).isEqualTo("10001");
        assertThat(result.getSDesc()).isEqualTo("Cash");
        assertThat(result.getACode()).isEqualTo("01");
        verify(subGroupRepository, times(1)).save(validSubGroup);
    }

    @Test
    @DisplayName("Should throw FAGroupNotFoundException when parent group does not exist")
    void shouldThrow_WhenParentGroupNotFound() {
        // ARRANGE
        when(groupRepository.findByCode("01")).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> createFASubGroup.execute(validSubGroup))
                .isInstanceOf(FAGroupNotFoundException.class)
                .hasMessageContaining("Parent Group with code 01 does not exist.");

        verify(subGroupRepository, never()).existsByCode(any());
        verify(subGroupRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should set default balance to ZERO when balance is null")
    void shouldDefaultBalance_WhenBalanceIsNull() {
        // ARRANGE
        FASubGroup subGroupWithNullBalance = new FASubGroup("10001", "Cash", "01", "00", null, "DR", "T");
        when(groupRepository.findByCode("01")).thenReturn(Optional.of(parentGroup));
        when(subGroupRepository.existsByCode("10001")).thenReturn(false);
        when(subGroupRepository.save(subGroupWithNullBalance)).thenReturn(subGroupWithNullBalance);

        // ACT
        createFASubGroup.execute(subGroupWithNullBalance);

        // ASSERT
        assertThat(subGroupWithNullBalance.getSOpbal()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should set default flag to 'T' when flag is null")
    void shouldDefaultFlag_WhenFlagIsNull() {
        // ARRANGE
        FASubGroup subGroupWithNullFlag = new FASubGroup("10001", "Cash", "01", "00", BigDecimal.ZERO, "DR", null);
        when(groupRepository.findByCode("01")).thenReturn(Optional.of(parentGroup));
        when(subGroupRepository.existsByCode("10001")).thenReturn(false);
        when(subGroupRepository.save(subGroupWithNullFlag)).thenReturn(subGroupWithNullFlag);

        // ACT
        createFASubGroup.execute(subGroupWithNullFlag);

        // ASSERT
        assertThat(subGroupWithNullFlag.getSFlag()).isEqualTo("T");
    }

    @Test
    @DisplayName("Should throw FASubGroupAlreadyExistsException when ledger code already exists")
    void shouldThrow_WhenCodeAlreadyExists() {
        // ARRANGE
        when(groupRepository.findByCode("01")).thenReturn(Optional.of(parentGroup));
        when(subGroupRepository.existsByCode("10001")).thenReturn(true);

        // ACT & ASSERT
        assertThatThrownBy(() -> createFASubGroup.execute(validSubGroup))
                .isInstanceOf(FASubGroupAlreadyExistsException.class)
                .hasMessageContaining("A ledger account with code '10001' already exists.");

        verify(subGroupRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────
    // Gap D — S_DRCR validation
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should throw FASubGroupValidationException when S_DRCR is not 'DR' or 'CR'")
    void shouldThrow_WhenDrCrInvalid() {
        FASubGroup bad = new FASubGroup("10001", "Cash", "01", "00", BigDecimal.ZERO, "XX", "T");

        // Same exception/status (400) as UpdateFASubGroup, so create and update match.
        assertThatThrownBy(() -> createFASubGroup.execute(bad))
                .isInstanceOf(FASubGroupValidationException.class)
                .hasMessageContaining("Normal balance side must be either 'DR' or 'CR'");

        // Validation happens before any repository interaction
        verify(subGroupRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should accept S_DRCR value 'CR'")
    void shouldCreate_WhenDrCrIsCR() {
        // CR only fits credit-natured classes, so put the account under a
        // Liability group (A_TYPE '1'), which is naturally CR.
        FAGroup liabilityGroup = new FAGroup("02", "Liability", "1", BigDecimal.ZERO);
        FASubGroup credit = new FASubGroup("20001", "Loan", "02", "10", BigDecimal.ZERO, "CR", "T");
        when(groupRepository.findByCode("02")).thenReturn(Optional.of(liabilityGroup));
        when(subGroupRepository.existsByCode("20001")).thenReturn(false);
        when(subGroupRepository.save(credit)).thenReturn(credit);

        FASubGroup result = createFASubGroup.execute(credit);

        assertThat(result.getSDrCr()).isEqualTo("CR");
        verify(subGroupRepository, times(1)).save(credit);
    }

    // Mixed-case input should be accepted and uppercased, same as every other
    // DR/CR endpoint. Each row puts the account under a class that's naturally
    // that side (DR => Asset, CR => Liability) so we test the normalization, not
    // the natural-balance rule.
    @ParameterizedTest(name = "S_DRCR \"{0}\" is normalized to \"{1}\"")
    @CsvSource({
            "dr, DR, 01, 0, 00",
            "Dr, DR, 01, 0, 00",
            "dR, DR, 01, 0, 00",
            "cr, CR, 02, 1, 10",
            "CR, CR, 02, 1, 10"
    })
    @DisplayName("Should normalize mixed-case S_DRCR to canonical uppercase on create")
    void shouldNormalizeDrCr_OnCreate(String input, String expected, String aCode, String aType, String sType) {
        FAGroup parent = new FAGroup(aCode, "Group", aType, BigDecimal.ZERO);
        FASubGroup subGroup = new FASubGroup("10001", "Cash", aCode, sType, BigDecimal.ZERO, input, "T");
        when(groupRepository.findByCode(aCode)).thenReturn(Optional.of(parent));
        when(subGroupRepository.existsByCode("10001")).thenReturn(false);
        when(subGroupRepository.save(subGroup)).thenReturn(subGroup);

        FASubGroup result = createFASubGroup.execute(subGroup);

        assertThat(result.getSDrCr()).isEqualTo(expected);
    }

    // ─────────────────────────────────────────────────────────
    // Natural balance side enforcement (Asset/Expense => DR, Liability/Equity/Revenue => CR)
    // ─────────────────────────────────────────────────────────

    @ParameterizedTest(name = "A_TYPE {0} ({1}) accepts its natural balance {3}")
    @CsvSource({
            "0, Asset, 00, DR",
            "4, Expense, 40, DR",
            "1, Liability, 10, CR",
            "2, Equity, 20, CR",
            "3, Revenue, 30, CR"
    })
    @DisplayName("Should create account when S_DRCR matches the category's natural balance")
    void shouldCreate_WhenNormalBalanceMatchesCategory(String aType, String name, String sType, String drCr) {
        FAGroup parent = new FAGroup("0" + aType, name, aType, BigDecimal.ZERO);
        FASubGroup subGroup = new FASubGroup("10001", name, "0" + aType, sType, BigDecimal.ZERO, drCr, "T");
        when(groupRepository.findByCode("0" + aType)).thenReturn(Optional.of(parent));
        when(subGroupRepository.existsByCode("10001")).thenReturn(false);
        when(subGroupRepository.save(subGroup)).thenReturn(subGroup);

        FASubGroup result = createFASubGroup.execute(subGroup);

        assertThat(result.getSDrCr()).isEqualTo(drCr);
        verify(subGroupRepository, times(1)).save(subGroup);
    }

    @ParameterizedTest(name = "A_TYPE {0} ({1}) rejects wrong balance {3}")
    @CsvSource({
            "0, Asset, 00, CR",
            "4, Expense, 40, CR",
            "1, Liability, 10, DR",
            "2, Equity, 20, DR",
            "3, Revenue, 30, DR"
    })
    @DisplayName("Should reject account when S_DRCR violates the category's natural balance")
    void shouldThrow_WhenNormalBalanceViolatesCategory(String aType, String name, String sType, String drCr) {
        FAGroup parent = new FAGroup("0" + aType, name, aType, BigDecimal.ZERO);
        FASubGroup subGroup = new FASubGroup("10001", name, "0" + aType, sType, BigDecimal.ZERO, drCr, "T");
        when(groupRepository.findByCode("0" + aType)).thenReturn(Optional.of(parent));

        assertThatThrownBy(() -> createFASubGroup.execute(subGroup))
                .isInstanceOf(InvalidNormalBalanceException.class)
                .hasMessageContaining(name);

        verify(subGroupRepository, never()).save(any());
    }
}
