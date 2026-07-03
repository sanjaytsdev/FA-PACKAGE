package com.spam.financialaccounting.application.usecases.journaldetail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.spam.financialaccounting.domain.entity.JournalDetail;
import com.spam.financialaccounting.domain.repository.JournalDetailRepository;
import com.spam.financialaccounting.presentation.exception.journaldetail.JournalDetailNotFoundException;

@ExtendWith(MockitoExtension.class)
public class GetJournalDetailTest {

    @Mock
    private JournalDetailRepository journalDetailRepository;

    @InjectMocks
    private GetJournalDetail getJournalDetail;

    @Test
    @DisplayName("Should return JournalDetail when composite key exists")
    void shouldReturnDetail_WhenCompositeKeyExists() {
        // ARRANGE
        JournalDetail detail = new JournalDetail("V001", "SG01", "DR", new BigDecimal("500.00"));
        when(journalDetailRepository.findByCompositeKey("V001", "SG01", "DR"))
                .thenReturn(Optional.of(detail));

        // ACT
        JournalDetail result = getJournalDetail.execute("V001", "SG01", "DR");

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getJId()).isEqualTo("V001");
        assertThat(result.getJCode()).isEqualTo("SG01");
        assertThat(result.getJDrCr()).isEqualTo("DR");
        assertThat(result.getJAmount()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("Should throw JournalDetailNotFoundException when composite key does not exist")
    void shouldThrow_WhenCompositeKeyNotFound() {
        // ARRANGE
        when(journalDetailRepository.findByCompositeKey("V999", "SG99", "DR"))
                .thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> getJournalDetail.execute("V999", "SG99", "DR"))
                .isInstanceOf(JournalDetailNotFoundException.class)
                .hasMessageContaining("Journal detail not found for voucher V999")
                .hasMessageContaining("account SG99")
                .hasMessageContaining("type DR");
    }

    @Test
    @DisplayName("Should call findByCompositeKey exactly once with the correct arguments")
    void shouldCallRepositoryOnce_WithCorrectArguments() {
        // ARRANGE
        JournalDetail detail = new JournalDetail("V001", "SG01", "CR", new BigDecimal("300.00"));
        when(journalDetailRepository.findByCompositeKey("V001", "SG01", "CR"))
                .thenReturn(Optional.of(detail));

        // ACT
        getJournalDetail.execute("V001", "SG01", "CR");

        // ASSERT
        verify(journalDetailRepository, times(1)).findByCompositeKey("V001", "SG01", "CR");
    }
}
