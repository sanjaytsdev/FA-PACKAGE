package com.spam.financialaccounting.application.usecases.journaldetail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.spam.financialaccounting.domain.repository.JournalDetailRepository;
import com.spam.financialaccounting.domain.repository.JournalMasterRepository;
import com.spam.financialaccounting.presentation.exception.journalmaster.JournalMasterNotFoundException;

@ExtendWith(MockitoExtension.class)
public class GetJournalDetailsByJournalIdTest {

    @Mock
    private JournalDetailRepository journalDetailRepository;

    @Mock
    private JournalMasterRepository journalMasterRepository;

    @InjectMocks
    private GetJournalDetailsByJournalId getJournalDetailsByJournalId;

    @Test
    @DisplayName("Should return all JournalDetails for a valid journal voucher ID")
    void shouldReturnDetails_WhenJournalIdExists() {
        // ARRANGE
        List<JournalDetail> fakeDetails = Arrays.asList(
                new JournalDetail("V001", "SG01", "DR", new BigDecimal("500.00")),
                new JournalDetail("V001", "SG02", "CR", new BigDecimal("500.00")));
        when(journalMasterRepository.existsById("V001")).thenReturn(true);
        when(journalDetailRepository.findByJournalId("V001")).thenReturn(fakeDetails);

        // ACT
        List<JournalDetail> result = getJournalDetailsByJournalId.execute("V001");

        // ASSERT
        assertThat(result).hasSize(2);
        assertThat(result).extracting(JournalDetail::getJId).containsOnly("V001");
        verify(journalDetailRepository, times(1)).findByJournalId("V001");
    }

    @Test
    @DisplayName("Should return empty list when journal voucher has no detail lines")
    void shouldReturnEmptyList_WhenNoDetailsForJournalId() {
        // ARRANGE
        when(journalMasterRepository.existsById("V001")).thenReturn(true);
        when(journalDetailRepository.findByJournalId("V001")).thenReturn(Collections.emptyList());

        // ACT
        List<JournalDetail> result = getJournalDetailsByJournalId.execute("V001");

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should throw NotFoundException when journal voucher ID does not exist")
    void shouldThrow_WhenJournalMasterNotFound() {
        // ARRANGE
        when(journalMasterRepository.existsById("V999")).thenReturn(false);

        // ACT + ASSERT
        assertThatThrownBy(() -> getJournalDetailsByJournalId.execute("V999"))
                .isInstanceOf(JournalMasterNotFoundException.class)
                .hasMessageContaining("Journal voucher with ID V999 does not exist");

        // Repository query must not be called if journal voucher doesn't exist
        verify(journalDetailRepository, never()).findByJournalId(any());
    }

    @Test
    @DisplayName("Should check JournalMaster existence before querying JournalDetails")
    void shouldValidateFirst_BeforeQueryingRepository() {
        when(journalMasterRepository.existsById("V999")).thenReturn(false);

        assertThatThrownBy(() -> getJournalDetailsByJournalId.execute("V999"))
                .isInstanceOf(JournalMasterNotFoundException.class);

        verify(journalMasterRepository, times(1)).existsById("V999");
        verify(journalDetailRepository, never()).findByJournalId(any());
    }
}
