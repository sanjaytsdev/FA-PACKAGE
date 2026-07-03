package com.spam.financialaccounting.application.usecases.journalmaster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spam.financialaccounting.domain.entity.JournalMaster;
import com.spam.financialaccounting.domain.repository.JournalMasterRepository;
import com.spam.financialaccounting.presentation.exception.journalmaster.JournalMasterNotFoundException;

@ExtendWith(MockitoExtension.class)
public class GetJournalMasterByIdTest {

    @Mock
    private JournalMasterRepository journalMasterRepository;

    @InjectMocks
    private GetJournalMasterById getJournalMasterById;

    private static final LocalDateTime FIXED_DATE = LocalDateTime.of(2024, 1, 15, 10, 0);

    @Test
    @DisplayName("Should return JournalMaster when ID exists")
    void shouldReturn_WhenIdExists() {
        JournalMaster master = new JournalMaster("JV00000001", "JV", FIXED_DATE, new BigDecimal("1000.00"), "Test");
        when(journalMasterRepository.findById("JV00000001")).thenReturn(Optional.of(master));

        JournalMaster result = getJournalMasterById.execute("JV00000001");

        assertThat(result).isNotNull();
        assertThat(result.getJId()).isEqualTo("JV00000001");
        assertThat(result.getJDoc()).isEqualTo("JV");
        assertThat(result.getJAmount()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("Should throw JournalMasterNotFoundException when ID does not exist")
    void shouldThrow_WhenIdNotFound() {
        when(journalMasterRepository.findById("JV99999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getJournalMasterById.execute("JV99999999"))
                .isInstanceOf(JournalMasterNotFoundException.class)
                .hasMessageContaining("Journal voucher with ID JV99999999 not found");
    }

    @Test
    @DisplayName("Should call findById exactly once with the correct ID")
    void shouldCallRepositoryOnce() {
        JournalMaster master = new JournalMaster("JV00000001", "JV", FIXED_DATE, new BigDecimal("500.00"), null);
        when(journalMasterRepository.findById("JV00000001")).thenReturn(Optional.of(master));

        getJournalMasterById.execute("JV00000001");

        verify(journalMasterRepository, times(1)).findById("JV00000001");
    }
}
