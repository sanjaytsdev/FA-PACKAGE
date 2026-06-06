package com.spam.financialaccounting.application.usecases.fasubgroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.spam.financialaccounting.domain.entity.FASubGroup;
import com.spam.financialaccounting.domain.repository.FASubGroupRepository;
import com.spam.financialaccounting.domain.repository.JournalDetailRepository;
import com.spam.financialaccounting.presentation.exception.fasubgroup.FASubGroupNotFoundException;
import com.spam.financialaccounting.presentation.exception.fasubgroup.FASubGroupValidationException;

@ExtendWith(MockitoExtension.class)
public class DeleteFASubGroupTest {

    @Mock
    private FASubGroupRepository subGroupRepository;

    @Mock
    private JournalDetailRepository journalDetailRepository;

    @InjectMocks
    private DeleteFASubGroup deleteFASubGroup;

    private FASubGroup sampleSubGroup;

    @BeforeEach
    void setUp() {
        sampleSubGroup = new FASubGroup("10001", "Cash", "01", "00", BigDecimal.ZERO, "DR", "T");
    }

    @Test
    @DisplayName("Should successfully delete FASubGroup when it exists and has no journal entries")
    void shouldDeleteSubGroup_WhenExistsAndNoJournalEntries() {
        // ARRANGE
        when(subGroupRepository.findByCode("10001")).thenReturn(Optional.of(sampleSubGroup));
        when(journalDetailRepository.existsByAccountCode("10001")).thenReturn(false);
        when(subGroupRepository.delete("10001")).thenReturn(true);

        // ACT
        boolean result = deleteFASubGroup.execute("10001");

        // ASSERT
        assertThat(result).isTrue();
        verify(subGroupRepository, times(1)).delete("10001");
    }

    @Test
    @DisplayName("Should throw FASubGroupNotFoundException when ledger account does not exist")
    void shouldThrow_WhenSubGroupNotFound() {
        // ARRANGE
        when(subGroupRepository.findByCode("99999")).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> deleteFASubGroup.execute("99999"))
                .isInstanceOf(FASubGroupNotFoundException.class)
                .hasMessageContaining("Ledger Account with code 99999 not found");

        verify(journalDetailRepository, never()).existsByAccountCode("99999");
        verify(subGroupRepository, never()).delete("99999");
    }

    @Test
    @DisplayName("Should throw FASubGroupValidationException when ledger account has associated journal entries")
    void shouldThrow_WhenSubGroupHasJournalEntries() {
        // ARRANGE
        when(subGroupRepository.findByCode("10001")).thenReturn(Optional.of(sampleSubGroup));
        when(journalDetailRepository.existsByAccountCode("10001")).thenReturn(true);

        // ACT & ASSERT
        assertThatThrownBy(() -> deleteFASubGroup.execute("10001"))
                .isInstanceOf(FASubGroupValidationException.class)
                .hasMessageContaining("Cannot delete ledger account with existing journal entries.");

        verify(subGroupRepository, never()).delete("10001");
    }
}
