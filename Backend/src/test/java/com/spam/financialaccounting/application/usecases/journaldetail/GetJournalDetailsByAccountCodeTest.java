package com.spam.financialaccounting.application.usecases.journaldetail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spam.financialaccounting.domain.entity.JournalDetail;
import com.spam.financialaccounting.domain.repository.FASubGroupRepository;
import com.spam.financialaccounting.domain.repository.JournalDetailRepository;
import com.spam.financialaccounting.presentation.exception.journaldetail.JournalDetailValidationException;

@ExtendWith(MockitoExtension.class)
public class GetJournalDetailsByAccountCodeTest {

    @Mock
    private JournalDetailRepository journalDetailRepository;

    @Mock
    private FASubGroupRepository faSubGroupRepository;

    @InjectMocks
    private GetJournalDetailsByAccountCode getJournalDetailsByAccountCode;

    @Test
    @DisplayName("Should return list of JournalDetails when account code exists")
    void shouldReturnDetails_WhenAccountCodeExists() {
        // ARRANGE
        List<JournalDetail> fakeDetails = Arrays.asList(
                new JournalDetail("V001", "SG01", "DR", new BigDecimal("500.00")),
                new JournalDetail("V002", "SG01", "CR", new BigDecimal("200.00")));
        when(faSubGroupRepository.existsByCode("SG01")).thenReturn(true);
        when(journalDetailRepository.findByAccountCode("SG01")).thenReturn(fakeDetails);

        // ACT
        List<JournalDetail> result = getJournalDetailsByAccountCode.execute("SG01");

        // ASSERT
        assertThat(result).hasSize(2);
        assertThat(result).extracting(JournalDetail::getJCode).containsOnly("SG01");
        verify(journalDetailRepository, times(1)).findByAccountCode("SG01");
    }

    @Test
    @DisplayName("Should return empty list when account code exists but has no details")
    void shouldReturnEmptyList_WhenNoDetailsForCode() {
        // ARRANGE
        when(faSubGroupRepository.existsByCode("SG01")).thenReturn(true);
        when(journalDetailRepository.findByAccountCode("SG01")).thenReturn(Collections.emptyList());

        // ACT
        List<JournalDetail> result = getJournalDetailsByAccountCode.execute("SG01");

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should throw ValidationException when account code does not exist in FASubGroup")
    void shouldThrow_WhenAccountCodeNotFound() {
        // ARRANGE
        when(faSubGroupRepository.existsByCode("INVALID")).thenReturn(false);

        // ACT + ASSERT
        assertThatThrownBy(() -> getJournalDetailsByAccountCode.execute("INVALID"))
                .isInstanceOf(JournalDetailValidationException.class)
                .hasMessageContaining("Ledger account with code INVALID does not exist");

        // Repository query should never be called if the code doesn't exist
        verify(journalDetailRepository, never()).findByAccountCode(any());
    }

    @Test
    @DisplayName("Should validate account code before querying JournalDetails")
    void shouldValidateFirst_BeforeQueryingRepository() {
        when(faSubGroupRepository.existsByCode("SG99")).thenReturn(false);

        assertThatThrownBy(() -> getJournalDetailsByAccountCode.execute("SG99"))
                .isInstanceOf(JournalDetailValidationException.class);

        // confirm the guard runs BEFORE the query
        verify(faSubGroupRepository, times(1)).existsByCode("SG99");
        verify(journalDetailRepository, never()).findByAccountCode(any());
    }
}
