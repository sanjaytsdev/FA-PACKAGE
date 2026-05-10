package com.spam.financialaccounting.application.usecases.fagroup;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spam.financialaccounting.domain.entity.FAGroup;
import com.spam.financialaccounting.domain.repository.FAGroupRepository;
import com.spam.financialaccounting.presentation.exception.fagroup.FAGroupAlreadyExistsException;
import com.spam.financialaccounting.presentation.exception.fagroup.FAGroupValidationException;

@ExtendWith(MockitoExtension.class)
public class CreateFAGroupTest {

    @Mock
    private FAGroupRepository faGroupRepository;

    @InjectMocks
    private CreateFAGroup createFAGroup;

    private FAGroup validGroup;

    @BeforeEach
    void setUp() {
        validGroup = new FAGroup("01", "Asset", "0", BigDecimal.ZERO);
    }

    @Test
    @DisplayName("should successfully create FAGroup when all inputs are valid")
    void shouldCreateFAGroup_WhenValidInput() {
        when(faGroupRepository.existsByCode("01")).thenReturn(false);
        when(faGroupRepository.save(validGroup)).thenReturn(validGroup);

        FAGroup result = createFAGroup.execute(validGroup);

        assertThat(result).isNotNull();
        assertThat(result.getAccountCode()).isEqualTo("01");
        assertThat(result.getAccountDescription()).isEqualTo("Asset");

        verify(faGroupRepository, times(1)).save(validGroup);
    }

    @Test
    @DisplayName("Should set balance to ZERO when balance is null")
    void shouldDefaultBalance_WhenBalanceIsNull() {
        FAGroup groupWithNullBalance = new FAGroup("02", "Liability", "1", null);
        when(faGroupRepository.existsByCode("02")).thenReturn(false);
        when(faGroupRepository.save(groupWithNullBalance)).thenReturn(groupWithNullBalance);

        createFAGroup.execute(groupWithNullBalance);

        assertThat(groupWithNullBalance.getAccountCurrentBalance()).isEqualTo(BigDecimal.ZERO);
    }

    // Account code validation
    @Test
    @DisplayName("Should throw ValidationException when account code is null")
    void shouldThrow_WhenAccountCodeIsNull() {
        FAGroup group = new FAGroup(null, "Asset", "0", BigDecimal.ZERO);

        assertThatThrownBy(() -> createFAGroup.execute(group)).isInstanceOf(FAGroupValidationException.class)
                .hasMessageContaining("Account Code cannot be null or empty");
    }

    @Test
    @DisplayName("Should throw ValidationException when account code is not 2 characters")
    void shouldThrow_WhenAccountCodeIsWrongLength() {
        FAGroup group = new FAGroup("ABC", "Asset", "0", BigDecimal.ZERO);
        assertThatThrownBy(() -> createFAGroup.execute(group)).isInstanceOf(FAGroupValidationException.class)
                .hasMessageContaining("Account Code must be exactly 2 characters");
    }

    // uniqueness check
    @Test
    @DisplayName("Should throw AlreadyExistsException when code already exists")
    void shouldThrow_WhenCodeAlreadyExists() {
        when(faGroupRepository.existsByCode("01")).thenReturn(true);

        assertThatThrownBy(() -> createFAGroup.execute(validGroup))
                .isInstanceOf(FAGroupAlreadyExistsException.class)
                .hasMessageContaining("FA Group already exists with code: 01");

        verify(faGroupRepository, never()).save(any());
    }

    // description validation
    @Test
    @DisplayName("Should throw ValidationException when description is null")
    void shouldThrow_WhenDescriptionIsNull() {
        FAGroup group = new FAGroup("01", null, "0", BigDecimal.ZERO);
        when(faGroupRepository.existsByCode("01")).thenReturn(false);
        assertThatThrownBy(() -> createFAGroup.execute(group))
                .isInstanceOf(FAGroupValidationException.class)
                .hasMessageContaining("Account Description cannot be null or empty");
    }

    @Test
    @DisplayName("Should throw ValidationException when description exceeds 50 characters")
    void shouldThrow_WhenDescriptionTooLong() {
        String longDescription = "A".repeat(51); // 51 characters
        FAGroup group = new FAGroup("01", longDescription, "0", BigDecimal.ZERO);
        when(faGroupRepository.existsByCode("01")).thenReturn(false);
        assertThatThrownBy(() -> createFAGroup.execute(group))
                .isInstanceOf(FAGroupValidationException.class)
                .hasMessageContaining("Account Description cannot exceed 50 characters");
    }

    // Account Type Validation
    @Test
    @DisplayName("Should throw ValidationException when account type is null")
    void shouldThow_WhenAccountTypeIsNull() {
        FAGroup group = new FAGroup("01", "Asset", null, BigDecimal.ZERO);
        when(faGroupRepository.existsByCode("01")).thenReturn(false);

        assertThatThrownBy(() -> createFAGroup.execute(group))
                .isInstanceOf(FAGroupValidationException.class)
                .hasMessageContaining("Account Type cannot be null");
    }

    @Test
    @DisplayName("Should throw ValidationException when account type is invalid")
    void shouldThrow_WhenAccountTypeIsInvalid() {
        FAGroup group = new FAGroup("01", "Asset", "9", BigDecimal.ZERO);
        when(faGroupRepository.existsByCode("01")).thenReturn(false);

        assertThatThrownBy(() -> createFAGroup.execute(group))
                .isInstanceOf(FAGroupValidationException.class)
                .hasMessageContaining("Invalid Account Type");
    }

    @Test
    @DisplayName("Should accept all valid account types: 0, 1, 2, 3, 4")
    void shouldAcceptAllValidAccountTypes() {
        String[] validTypes = { "0", "1", "2", "3", "4" };

        for (String type : validTypes) {
            FAGroup group = new FAGroup("0" + type.charAt(0), "Test", type, BigDecimal.ZERO);
            when(faGroupRepository.existsByCode(anyString())).thenReturn(false);
            when(faGroupRepository.save(any())).thenReturn(group);

            assertThatCode(() -> createFAGroup.execute(group))
                    .doesNotThrowAnyException();
        }
    }

}
