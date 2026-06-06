package com.spam.financialaccounting.application.usecases.fasubgroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spam.financialaccounting.domain.entity.FASubGroup;
import com.spam.financialaccounting.domain.repository.FASubGroupRepository;
import com.spam.financialaccounting.presentation.exception.fasubgroup.FASubGroupNotFoundException;

@ExtendWith(MockitoExtension.class)
public class GetFASubGroupByCodeTest {

    @Mock
    private FASubGroupRepository repository;

    @InjectMocks
    private GetFASubGroupByCode getFASubGroupByCode;

    @Test
    @DisplayName("Should successfully return FASubGroup when code exists")
    void shouldReturnFASubGroup_WhenCodeExists() {
        // ARRANGE
        FASubGroup subGroup = new FASubGroup("10001", "Cash", "01", "00", BigDecimal.ZERO, "DR", "T");
        when(repository.findByCode("10001")).thenReturn(Optional.of(subGroup));

        // ACT
        FASubGroup result = getFASubGroupByCode.execute("10001");

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getSCode()).isEqualTo("10001");
        assertThat(result.getSDesc()).isEqualTo("Cash");
        verify(repository, times(1)).findByCode("10001");
    }

    @Test
    @DisplayName("Should throw FASubGroupNotFoundException when code does not exist")
    void shouldThrow_WhenCodeDoesNotExist() {
        // ARRANGE
        when(repository.findByCode("99999")).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> getFASubGroupByCode.execute("99999"))
                .isInstanceOf(FASubGroupNotFoundException.class)
                .hasMessageContaining("FASubGroup not found with code: 99999");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when code is null or empty")
    void shouldThrow_WhenCodeIsNullOrEmpty() {
        // ACT & ASSERT for null
        assertThatThrownBy(() -> getFASubGroupByCode.execute(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sCode cannot be null or empty");

        // ACT & ASSERT for empty
        assertThatThrownBy(() -> getFASubGroupByCode.execute(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sCode cannot be null or empty");

        // ACT & ASSERT for whitespace
        assertThatThrownBy(() -> getFASubGroupByCode.execute("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sCode cannot be null or empty");

        verify(repository, never()).findByCode(anyString());
    }
}
