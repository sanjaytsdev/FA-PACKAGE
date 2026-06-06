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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spam.financialaccounting.domain.entity.FAGroup;
import com.spam.financialaccounting.domain.entity.FASubGroup;
import com.spam.financialaccounting.domain.repository.FAGroupRepository;
import com.spam.financialaccounting.domain.repository.FASubGroupRepository;
import com.spam.financialaccounting.presentation.exception.fagroup.FAGroupNotFoundException;
import com.spam.financialaccounting.presentation.exception.fasubgroup.FASubGroupNotFoundException;
import com.spam.financialaccounting.presentation.exception.fasubgroup.FASubGroupValidationException;

@ExtendWith(MockitoExtension.class)
public class UpdateFASubGroupTest {

    @Mock
    private FASubGroupRepository subGroupRepository;

    @Mock
    private FAGroupRepository groupRepository;

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
                .hasMessageContaining("sCode must be 5 characters.");
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
                .hasMessageContaining("S_DRCR must be either 'DR' or 'CR'");
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
                .hasMessageContaining("S_FLAG must be either 'T'(active) or 'F'(inactive)");
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
}
