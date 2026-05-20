package com.spam.financialaccounting.application.usecases.journaldetail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spam.financialaccounting.domain.entity.JournalDetail;
import com.spam.financialaccounting.domain.repository.FASubGroupRepository;
import com.spam.financialaccounting.domain.repository.JournalDetailRepository;
import com.spam.financialaccounting.domain.repository.JournalMasterRepository;
import com.spam.financialaccounting.presentation.exception.journaldetail.JournalDetailAlreadyExistsException;
import com.spam.financialaccounting.presentation.exception.journaldetail.JournalDetailValidationException;

@ExtendWith(MockitoExtension.class)
public class CreateJournalDetailTest {

    @Mock
    private JournalDetailRepository journalDetailRepository;

    @Mock
    private JournalMasterRepository journalMasterRepository;

    @Mock
    private FASubGroupRepository faSubGroupRepository;

    @InjectMocks
    private CreateJournalDetail createJournalDetail;

    private JournalDetail validDetail;

    @BeforeEach
    void setUp() {
        validDetail = new JournalDetail("V001", "SG01", "DR", new BigDecimal("500.00"));
    }

    // Happy Path
    @Test
    @DisplayName("Should successfully create JournalDetail when all inputs are valid")
    void shouldCreate_WhenAllInputsAreValid() {
        when(journalMasterRepository.existsById("V001")).thenReturn(true);
        when(faSubGroupRepository.existsByCode("SG01")).thenReturn(true);
        when(journalDetailRepository.existsByCompositeKey("V001", "SG01", "DR")).thenReturn(false);

        JournalDetail result = createJournalDetail.execute(validDetail);

        assertThat(result).isNotNull();
        assertThat(result.getJId()).isEqualTo("V001");
        assertThat(result.getJCode()).isEqualTo("SG01");
        assertThat(result.getJDrCr()).isEqualTo("DR");
        assertThat(result.getJAmount()).isEqualByComparingTo("500.00");

        verify(journalDetailRepository).save(validDetail);
    }

    @Test
    @DisplayName("Should successfully create JournalDetail with CR indicator")
    void shouldCreate_WhenDrCrIsCR() {
        JournalDetail crDetail = new JournalDetail("V001", "SG01", "CR", new BigDecimal("250.00"));
        when(journalMasterRepository.existsById("V001")).thenReturn(true);
        when(faSubGroupRepository.existsByCode("SG01")).thenReturn(true);
        when(journalDetailRepository.existsByCompositeKey("V001", "SG01", "CR")).thenReturn(false);

        JournalDetail result = createJournalDetail.execute(crDetail);

        assertThat(result.getJDrCr()).isEqualTo("CR");
        verify(journalDetailRepository).save(crDetail);
    }

    // Amount Validation
    @Test
    @DisplayName("Should throw IllegalArgumentException when amount is null")
    void shouldThrow_WhenAmountIsNull() {
        JournalDetail detail = new JournalDetail("V001", "SG01", "DR", null);

        assertThatThrownBy(() -> createJournalDetail.execute(detail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount must be greater than zero");

        verify(journalDetailRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when amount is zero")
    void shouldThrow_WhenAmountIsZero() {
        JournalDetail detail = new JournalDetail("V001", "SG01", "DR", BigDecimal.ZERO);

        assertThatThrownBy(() -> createJournalDetail.execute(detail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount must be greater than zero");

        verify(journalDetailRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when amount is negative")
    void shouldThrow_WhenAmountIsNegative() {
        JournalDetail detail = new JournalDetail("V001", "SG01", "DR", new BigDecimal("-100.00"));

        assertThatThrownBy(() -> createJournalDetail.execute(detail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount must be greater than zero");

        verify(journalDetailRepository, never()).save(any());
    }

    // DR/CR Validation
    @Test
    @DisplayName("Should throw IllegalArgumentException when jDrCr is null")
    void shouldThrow_WhenDrCrIsNull() {
        JournalDetail detail = new JournalDetail("V001", "SG01", null, new BigDecimal("100.00"));

        assertThatThrownBy(() -> createJournalDetail.execute(detail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Debit/Credit indicator must be either 'DR' or 'CR'");

        verify(journalDetailRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when jDrCr is an invalid value")
    void shouldThrow_WhenDrCrIsInvalid() {
        JournalDetail detail = new JournalDetail("V001", "SG01", "XX", new BigDecimal("100.00"));

        assertThatThrownBy(() -> createJournalDetail.execute(detail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Debit/Credit indicator must be either 'DR' or 'CR'");

        verify(journalDetailRepository, never()).save(any());
    }

    // Referential Integrity — JournalMaster
    @Test
    @DisplayName("Should throw ValidationException when JournalMaster ID does not exist")
    void shouldThrow_WhenJournalMasterNotFound() {
        when(journalMasterRepository.existsById("V001")).thenReturn(false);

        assertThatThrownBy(() -> createJournalDetail.execute(validDetail))
                .isInstanceOf(JournalDetailValidationException.class)
                .hasMessageContaining("Journal voucher with ID V001 does not exist");

        verify(journalDetailRepository, never()).save(any());
    }

    // Referential Integrity — FASubGroup
    @Test
    @DisplayName("Should throw ValidationException when FASubGroup account code does not exist")
    void shouldThrow_WhenFASubGroupNotFound() {
        when(journalMasterRepository.existsById("V001")).thenReturn(true);
        when(faSubGroupRepository.existsByCode("SG01")).thenReturn(false);

        assertThatThrownBy(() -> createJournalDetail.execute(validDetail))
                .isInstanceOf(JournalDetailValidationException.class)
                .hasMessageContaining("Ledger account with code SG01 does not exist");

        verify(journalDetailRepository, never()).save(any());
    }

    
    // Duplicate Check
    @Test
    @DisplayName("Should throw AlreadyExistsException when composite key already exists")
    void shouldThrow_WhenDuplicateCompositeKey() {
        when(journalMasterRepository.existsById("V001")).thenReturn(true);
        when(faSubGroupRepository.existsByCode("SG01")).thenReturn(true);
        when(journalDetailRepository.existsByCompositeKey("V001", "SG01", "DR")).thenReturn(true);

        assertThatThrownBy(() -> createJournalDetail.execute(validDetail))
                .isInstanceOf(JournalDetailAlreadyExistsException.class)
                .hasMessageContaining("Journal detail already exists for voucher V001")
                .hasMessageContaining("account SG01")
                .hasMessageContaining("type DR");

        verify(journalDetailRepository, never()).save(any());
    }

    // Validation Order: Amount is checked BEFORE referential integrity
    @Test
    @DisplayName("Should throw amount error first, before checking JournalMaster existence")
    void shouldThrowAmountError_BeforeReferentialCheck() {
        JournalDetail detail = new JournalDetail("V001", "SG01", "DR", BigDecimal.ZERO);

        // Even if repo methods are not set up at all, it should still throw on amount first
        assertThatThrownBy(() -> createJournalDetail.execute(detail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount must be greater than zero");

        // JournalMaster and FASubGroup repos should NEVER be called if amount fails
        verify(journalMasterRepository, never()).existsById(any());
        verify(faSubGroupRepository, never()).existsByCode(any());
        verify(journalDetailRepository, never()).save(any());
    }
}
