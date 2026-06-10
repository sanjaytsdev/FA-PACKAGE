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
import com.spam.financialaccounting.domain.repository.JournalDetailRepository;
import com.spam.financialaccounting.presentation.exception.fagroup.FAGroupNotFoundException;
import com.spam.financialaccounting.presentation.exception.fasubgroup.FASubGroupHasTransactionsException;
import com.spam.financialaccounting.presentation.exception.fasubgroup.FASubGroupNotFoundException;
import com.spam.financialaccounting.presentation.exception.fasubgroup.FASubGroupValidationException;
import com.spam.financialaccounting.presentation.exception.fasubgroup.InvalidNormalBalanceException;

@ExtendWith(MockitoExtension.class)
public class UpdateFASubGroupTest {

    @Mock
    private FASubGroupRepository subGroupRepository;

    @Mock
    private FAGroupRepository groupRepository;

    @Mock
    private JournalDetailRepository journalDetailRepository;

    @InjectMocks
    private UpdateFASubGroup updateFASubGroup;

    private FAGroup parentGroup;
    private FASubGroup existingSubGroup;
    private FASubGroup updatedSubGroup;

    @BeforeEach
    void setUp() {
        parentGroup = new FAGroup("01", "Asset", "0", BigDecimal.ZERO);
        existingSubGroup = new FASubGroup("10001", "Cash", "01", "00", new BigDecimal("1000.00"), "DR", "T");
        updatedSubGroup = new FASubGroup("10001", "Petty Cash", "01", "00", new BigDecimal("1200.00"), "DR", "T");
    }

    @Test
    @DisplayName("Should successfully update FASubGroup when all inputs are valid")
    void shouldUpdateFASubGroup_WhenValidInput() {
        // ARRANGE
        when(subGroupRepository.findByCode("10001")).thenReturn(Optional.of(existingSubGroup));
        when(groupRepository.findByCode("01")).thenReturn(Optional.of(parentGroup));
        when(subGroupRepository.update(updatedSubGroup)).thenReturn(updatedSubGroup);

        // ACT
        FASubGroup result = updateFASubGroup.execute(updatedSubGroup);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getSDesc()).isEqualTo("Petty Cash");
        assertThat(result.getSOpbal()).isEqualByComparingTo("1200.00");
        verify(subGroupRepository, times(1)).update(updatedSubGroup);
    }

    @Test
    @DisplayName("Should throw FASubGroupNotFoundException when ledger account does not exist")
    void shouldThrow_WhenLedgerAccountNotFound() {
        // ARRANGE
        when(subGroupRepository.findByCode("10001")).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> updateFASubGroup.execute(updatedSubGroup))
                .isInstanceOf(FASubGroupNotFoundException.class)
                .hasMessageContaining("Ledger Account with code 10001 not found.");

        verify(subGroupRepository, never()).update(any());
    }

    @Test
    @DisplayName("Should throw FAGroupNotFoundException when parent group does not exist")
    void shouldThrow_WhenParentGroupNotFound() {
        // ARRANGE
        when(subGroupRepository.findByCode("10001")).thenReturn(Optional.of(existingSubGroup));
        when(groupRepository.findByCode("01")).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> updateFASubGroup.execute(updatedSubGroup))
                .isInstanceOf(FAGroupNotFoundException.class)
                .hasMessageContaining("Parent Group with code 01 does not exist.");

        verify(subGroupRepository, never()).update(any());
    }

    @Test
    @DisplayName("Should throw FASubGroupValidationException when sCode length is not 5")
    void shouldThrow_WhenCodeLengthIsWrong() {
        // ARRANGE
        FASubGroup badCodeSubGroup = new FASubGroup("101", "Cash", "01", "00", BigDecimal.ZERO, "DR", "T");
        when(subGroupRepository.findByCode("101")).thenReturn(Optional.of(existingSubGroup));
        when(groupRepository.findByCode("01")).thenReturn(Optional.of(parentGroup));

        // ACT & ASSERT
        assertThatThrownBy(() -> updateFASubGroup.execute(badCodeSubGroup))
                .isInstanceOf(FASubGroupValidationException.class)
                .hasMessageContaining("Ledger account code must be exactly 5 characters.");
    }

    @Test
    @DisplayName("Should throw FASubGroupValidationException when description is empty or null")
    void shouldThrow_WhenDescriptionIsEmpty() {
        // ARRANGE
        FASubGroup emptyDescSubGroup = new FASubGroup("10001", "   ", "01", "00", BigDecimal.ZERO, "DR", "T");
        when(subGroupRepository.findByCode("10001")).thenReturn(Optional.of(existingSubGroup));
        when(groupRepository.findByCode("01")).thenReturn(Optional.of(parentGroup));

        // ACT & ASSERT
        assertThatThrownBy(() -> updateFASubGroup.execute(emptyDescSubGroup))
                .isInstanceOf(FASubGroupValidationException.class)
                .hasMessageContaining("Description cannot be null or empty.");
    }

    @Test
    @DisplayName("Should throw FASubGroupValidationException when description exceeds 50 characters")
    void shouldThrow_WhenDescriptionTooLong() {
        // ARRANGE
        String longDesc = "A".repeat(51);
        FASubGroup longDescSubGroup = new FASubGroup("10001", longDesc, "01", "00", BigDecimal.ZERO, "DR", "T");
        when(subGroupRepository.findByCode("10001")).thenReturn(Optional.of(existingSubGroup));
        when(groupRepository.findByCode("01")).thenReturn(Optional.of(parentGroup));

        // ACT & ASSERT
        assertThatThrownBy(() -> updateFASubGroup.execute(longDescSubGroup))
                .isInstanceOf(FASubGroupValidationException.class)
                .hasMessageContaining("Description cannot exceed 50 characters.");
    }

    @Test
    @DisplayName("Should throw FASubGroupValidationException when S_DRCR is invalid")
    void shouldThrow_WhenDrCrIsInvalid() {
        // ARRANGE
        FASubGroup invalidDrCrSubGroup = new FASubGroup("10001", "Cash", "01", "00", BigDecimal.ZERO, "INVALID", "T");
        when(subGroupRepository.findByCode("10001")).thenReturn(Optional.of(existingSubGroup));
        when(groupRepository.findByCode("01")).thenReturn(Optional.of(parentGroup));

        // ACT & ASSERT
        assertThatThrownBy(() -> updateFASubGroup.execute(invalidDrCrSubGroup))
                .isInstanceOf(FASubGroupValidationException.class)
                .hasMessageContaining("Normal balance side must be either 'DR' or 'CR'");
    }

    @Test
    @DisplayName("Should convert S_DRCR to uppercase when it is valid lowercase")
    void shouldConvertDrCrToUppercase_WhenValidLowercase() {
        // ARRANGE
        FASubGroup lowercaseDrCr = new FASubGroup("10001", "Cash", "01", "00", BigDecimal.ZERO, "dr", "T");
        when(subGroupRepository.findByCode("10001")).thenReturn(Optional.of(existingSubGroup));
        when(groupRepository.findByCode("01")).thenReturn(Optional.of(parentGroup));
        when(subGroupRepository.update(lowercaseDrCr)).thenReturn(lowercaseDrCr);

        // ACT
        FASubGroup result = updateFASubGroup.execute(lowercaseDrCr);

        // ASSERT
        assertThat(result.getSDrCr()).isEqualTo("DR");
    }

    @Test
    @DisplayName("Should throw FASubGroupValidationException when S_FLAG is invalid")
    void shouldThrow_WhenFlagIsInvalid() {
        // ARRANGE
        FASubGroup invalidFlagSubGroup = new FASubGroup("10001", "Cash", "01", "00", BigDecimal.ZERO, "DR", "X");
        when(subGroupRepository.findByCode("10001")).thenReturn(Optional.of(existingSubGroup));
        when(groupRepository.findByCode("01")).thenReturn(Optional.of(parentGroup));

        // ACT & ASSERT
        assertThatThrownBy(() -> updateFASubGroup.execute(invalidFlagSubGroup))
                .isInstanceOf(FASubGroupValidationException.class)
                .hasMessageContaining("Status must be either 'T' (active) or 'F' (inactive)");
    }

    @Test
    @DisplayName("Should convert S_FLAG to uppercase when it is valid lowercase")
    void shouldConvertFlagToUppercase_WhenValidLowercase() {
        // ARRANGE
        FASubGroup lowercaseFlag = new FASubGroup("10001", "Cash", "01", "00", BigDecimal.ZERO, "DR", "f");
        when(subGroupRepository.findByCode("10001")).thenReturn(Optional.of(existingSubGroup));
        when(groupRepository.findByCode("01")).thenReturn(Optional.of(parentGroup));
        when(subGroupRepository.update(lowercaseFlag)).thenReturn(lowercaseFlag);

        // ACT
        FASubGroup result = updateFASubGroup.execute(lowercaseFlag);

        // ASSERT
        assertThat(result.getSFlag()).isEqualTo("F");
    }

    @Test
    @DisplayName("Should set default balance to ZERO when balance is null")
    void shouldDefaultBalanceToZero_WhenNull() {
        // ARRANGE
        FASubGroup nullBalanceSubGroup = new FASubGroup("10001", "Cash", "01", "00", null, "DR", "T");
        when(subGroupRepository.findByCode("10001")).thenReturn(Optional.of(existingSubGroup));
        when(groupRepository.findByCode("01")).thenReturn(Optional.of(parentGroup));
        when(subGroupRepository.update(nullBalanceSubGroup)).thenReturn(nullBalanceSubGroup);

        // ACT
        FASubGroup result = updateFASubGroup.execute(nullBalanceSubGroup);

        // ASSERT
        assertThat(result.getSOpbal()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should set default flag to 'T' when flag is null")
    void shouldDefaultFlagToT_WhenNull() {
        // ARRANGE
        FASubGroup nullFlagSubGroup = new FASubGroup("10001", "Cash", "01", "00", BigDecimal.ZERO, "DR", null);
        when(subGroupRepository.findByCode("10001")).thenReturn(Optional.of(existingSubGroup));
        when(groupRepository.findByCode("01")).thenReturn(Optional.of(parentGroup));
        when(subGroupRepository.update(nullFlagSubGroup)).thenReturn(nullFlagSubGroup);

        // ACT
        FASubGroup result = updateFASubGroup.execute(nullFlagSubGroup);

        // ASSERT
        assertThat(result.getSFlag()).isEqualTo("T");
    }

    // --- Immutability guard: critical fields locked once transactions exist ---

    @Test
    @DisplayName("Should allow changing a critical field when the account has NO journal transactions")
    void shouldAllowCriticalFieldChange_WhenNoTransactions() {
        // ARRANGE: reclassify account type 00 -> 01 while no postings exist
        FASubGroup reclassified = new FASubGroup("10001", "Cash", "01", "01", new BigDecimal("1000.00"), "DR", "T");
        when(subGroupRepository.findByCode("10001")).thenReturn(Optional.of(existingSubGroup));
        when(groupRepository.findByCode("01")).thenReturn(Optional.of(parentGroup));
        when(journalDetailRepository.existsByAccountCode("10001")).thenReturn(false);
        when(subGroupRepository.update(reclassified)).thenReturn(reclassified);

        // ACT
        FASubGroup result = updateFASubGroup.execute(reclassified);

        // ASSERT
        assertThat(result.getSType()).isEqualTo("01");
        verify(subGroupRepository, times(1)).update(reclassified);
    }

    @Test
    @DisplayName("Should allow editing description even when the account has journal transactions")
    void shouldAllowDescriptionChange_WhenTransactionsExist() {
        // ARRANGE: only the (non-critical) description changes, all critical fields untouched
        FASubGroup renamed = new FASubGroup("10001", "Cash in Hand", "01", "00", new BigDecimal("1000.00"), "DR", "T");
        when(subGroupRepository.findByCode("10001")).thenReturn(Optional.of(existingSubGroup));
        when(groupRepository.findByCode("01")).thenReturn(Optional.of(parentGroup));
        when(journalDetailRepository.existsByAccountCode("10001")).thenReturn(true);
        when(subGroupRepository.update(renamed)).thenReturn(renamed);

        // ACT
        FASubGroup result = updateFASubGroup.execute(renamed);

        // ASSERT
        assertThat(result.getSDesc()).isEqualTo("Cash in Hand");
        verify(subGroupRepository, times(1)).update(renamed);
    }

    @Test
    @DisplayName("Should block changing a critical field (S_TYPE) when journal transactions exist")
    void shouldBlock_WhenCriticalFieldChangedAndTransactionsExist() {
        // ARRANGE: attempt to reclassify account type after postings exist
        FASubGroup reclassified = new FASubGroup("10001", "Cash", "01", "01", new BigDecimal("1000.00"), "DR", "T");
        when(subGroupRepository.findByCode("10001")).thenReturn(Optional.of(existingSubGroup));
        when(groupRepository.findByCode("01")).thenReturn(Optional.of(parentGroup));
        when(journalDetailRepository.existsByAccountCode("10001")).thenReturn(true);

        // ACT & ASSERT
        assertThatThrownBy(() -> updateFASubGroup.execute(reclassified))
                .isInstanceOf(FASubGroupHasTransactionsException.class)
                .hasMessageContaining("account type")
                .hasMessageContaining("journal transactions already exist");

        verify(subGroupRepository, never()).update(any());
    }

    @Test
    @DisplayName("Should block changing normal balance side (S_DRCR) after postings exist")
    void shouldBlock_WhenDrCrChangedAfterPosting() {
        // ARRANGE: flip DR -> CR after the account has been posted to
        FASubGroup flippedDrCr = new FASubGroup("10001", "Cash", "01", "00", new BigDecimal("1000.00"), "CR", "T");
        when(subGroupRepository.findByCode("10001")).thenReturn(Optional.of(existingSubGroup));
        when(groupRepository.findByCode("01")).thenReturn(Optional.of(parentGroup));
        when(journalDetailRepository.existsByAccountCode("10001")).thenReturn(true);

        // ACT & ASSERT
        assertThatThrownBy(() -> updateFASubGroup.execute(flippedDrCr))
                .isInstanceOf(FASubGroupHasTransactionsException.class)
                .hasMessageContaining("normal balance side");

        verify(subGroupRepository, never()).update(any());
    }

    @Test
    @DisplayName("Should block changing opening balance (S_OPBAL) after postings exist")
    void shouldBlock_WhenOpeningBalanceChangedAfterPosting() {
        // ARRANGE: existing opening balance is 1000.00, attempt to change it to 5000.00
        FASubGroup changedOpbal = new FASubGroup("10001", "Cash", "01", "00", new BigDecimal("5000.00"), "DR", "T");
        when(subGroupRepository.findByCode("10001")).thenReturn(Optional.of(existingSubGroup));
        when(groupRepository.findByCode("01")).thenReturn(Optional.of(parentGroup));
        when(journalDetailRepository.existsByAccountCode("10001")).thenReturn(true);

        // ACT & ASSERT
        assertThatThrownBy(() -> updateFASubGroup.execute(changedOpbal))
                .isInstanceOf(FASubGroupHasTransactionsException.class)
                .hasMessageContaining("opening balance");

        verify(subGroupRepository, never()).update(any());
    }

    // --- Natural balance side enforcement (Asset/Expense => DR, Liability/Equity/Revenue => CR) ---

    @ParameterizedTest(name = "A_TYPE {0} ({1}) accepts its natural balance {3}")
    @CsvSource({
            "0, Asset, 00, DR",
            "4, Expense, 40, DR",
            "1, Liability, 10, CR",
            "2, Equity, 20, CR",
            "3, Revenue, 30, CR"
    })
    @DisplayName("Should update account when S_DRCR matches the category's natural balance")
    void shouldUpdate_WhenNormalBalanceMatchesCategory(String aType, String name, String sType, String drCr) {
        String aCode = "0" + aType;
        FAGroup parent = new FAGroup(aCode, name, aType, BigDecimal.ZERO);
        FASubGroup existing = new FASubGroup("10001", name, aCode, sType, BigDecimal.ZERO, drCr, "T");
        FASubGroup updated = new FASubGroup("10001", name + " Updated", aCode, sType, BigDecimal.ZERO, drCr, "T");
        when(subGroupRepository.findByCode("10001")).thenReturn(Optional.of(existing));
        when(groupRepository.findByCode(aCode)).thenReturn(Optional.of(parent));
        when(journalDetailRepository.existsByAccountCode("10001")).thenReturn(false);
        when(subGroupRepository.update(updated)).thenReturn(updated);

        FASubGroup result = updateFASubGroup.execute(updated);

        assertThat(result.getSDrCr()).isEqualTo(drCr);
        verify(subGroupRepository, times(1)).update(updated);
    }

    @ParameterizedTest(name = "A_TYPE {0} ({1}) rejects wrong balance {3} (no postings)")
    @CsvSource({
            "0, Asset, 00, CR",
            "4, Expense, 40, CR",
            "1, Liability, 10, DR",
            "2, Equity, 20, DR",
            "3, Revenue, 30, DR"
    })
    @DisplayName("Should reject update when S_DRCR violates the category's natural balance")
    void shouldThrow_WhenUpdateViolatesNormalBalance(String aType, String name, String sType, String drCr) {
        String aCode = "0" + aType;
        FAGroup parent = new FAGroup(aCode, name, aType, BigDecimal.ZERO);
        FASubGroup existing = new FASubGroup("10001", name, aCode, sType, BigDecimal.ZERO, drCr, "T");
        FASubGroup updated = new FASubGroup("10001", name, aCode, sType, BigDecimal.ZERO, drCr, "T");
        when(subGroupRepository.findByCode("10001")).thenReturn(Optional.of(existing));
        when(groupRepository.findByCode(aCode)).thenReturn(Optional.of(parent));
        when(journalDetailRepository.existsByAccountCode("10001")).thenReturn(false);

        assertThatThrownBy(() -> updateFASubGroup.execute(updated))
                .isInstanceOf(InvalidNormalBalanceException.class)
                .hasMessageContaining(name);

        verify(subGroupRepository, never()).update(any());
    }
}
