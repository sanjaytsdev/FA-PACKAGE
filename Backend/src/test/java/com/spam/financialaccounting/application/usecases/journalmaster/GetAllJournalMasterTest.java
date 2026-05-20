package com.spam.financialaccounting.application.usecases.journalmaster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spam.financialaccounting.domain.entity.JournalMaster;
import com.spam.financialaccounting.domain.repository.JournalMasterRepository;

@ExtendWith(MockitoExtension.class)
public class GetAllJournalMasterTest {

    @Mock
    private JournalMasterRepository journalMasterRepository;

    @InjectMocks
    private GetAllJournalMaster getAllJournalMaster;

    private static final LocalDateTime FIXED_DATE = LocalDateTime.of(2024, 1, 15, 10, 0);

    @Test
    @DisplayName("Should return all JournalMasters when records exist")
    void shouldReturnAll_WhenRecordsExist() {
        List<JournalMaster> data = Arrays.asList(
                new JournalMaster("JV00000001", "JV", FIXED_DATE, new BigDecimal("1000.00"), "Entry 1"),
                new JournalMaster("JV00000002", "PV", FIXED_DATE, new BigDecimal("500.00"), "Entry 2"));
        when(journalMasterRepository.findAll()).thenReturn(data);

        List<JournalMaster> result = getAllJournalMaster.execute();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getJId()).isEqualTo("JV00000001");
        assertThat(result.get(1).getJId()).isEqualTo("JV00000002");
        verify(journalMasterRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no JournalMasters exist")
    void shouldReturnEmptyList_WhenNoRecords() {
        when(journalMasterRepository.findAll()).thenReturn(Collections.emptyList());

        List<JournalMaster> result = getAllJournalMaster.execute();

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(journalMasterRepository, times(1)).findAll();
    }
}
