package com.spam.financialaccounting.application.usecases.journaldetail;

import static org.assertj.core.api.Assertions.assertThat;
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

@ExtendWith(MockitoExtension.class)
public class GetAllJournalDetailsTest {

    @Mock
    private JournalDetailRepository journalDetailRepository;

    @InjectMocks
    private GetAllJournalDetails getAllJournalDetails;

    @Test
    @DisplayName("Should return all JournalDetails when records exist")
    void shouldReturnAllDetails_WhenRecordsExist() {
        // ARRANGE
        List<JournalDetail> fakeDetails = Arrays.asList(
                new JournalDetail("V001", "SG01", "DR", new BigDecimal("500.00")),
                new JournalDetail("V001", "SG01", "CR", new BigDecimal("500.00")),
                new JournalDetail("V002", "SG02", "DR", new BigDecimal("1000.00")));
        when(journalDetailRepository.findAll()).thenReturn(fakeDetails);

        // ACT
        List<JournalDetail> result = getAllJournalDetails.execute();

        // ASSERT
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getJId()).isEqualTo("V001");
        assertThat(result.get(2).getJId()).isEqualTo("V002");

        verify(journalDetailRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no JournalDetails exist")
    void shouldReturnEmptyList_WhenNoDetailsExist() {
        // ARRANGE
        when(journalDetailRepository.findAll()).thenReturn(Collections.emptyList());

        // ACT
        List<JournalDetail> result = getAllJournalDetails.execute();

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(journalDetailRepository, times(1)).findAll();
    }
}
