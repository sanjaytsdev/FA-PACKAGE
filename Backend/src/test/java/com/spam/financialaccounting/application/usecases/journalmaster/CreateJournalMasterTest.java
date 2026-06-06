package com.spam.financialaccounting.application.usecases.journalmaster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spam.financialaccounting.domain.entity.JournalMaster;
import com.spam.financialaccounting.domain.repository.JournalMasterRepository;
import com.spam.financialaccounting.presentation.exception.journalmaster.JournalMasterAlreadyExistsException;
import com.spam.financialaccounting.presentation.exception.journalmaster.JournalMasterValidationException;

@ExtendWith(MockitoExtension.class)
public class CreateJournalMasterTest {

    @Mock
    private JournalMasterRepository journalMasterRepository;

    @InjectMocks
    private CreateJournalMaster createJournalMaster;

    private static final LocalDateTime FIXED_DATE = LocalDateTime.of(2024, 1, 15, 10, 0);
    private JournalMaster validMaster;

    @BeforeEach
    void setUp() {
        // jId: exactly 10 chars, jDoc: exactly 2 chars
        validMaster = new JournalMaster("JV00000001", "JV", FIXED_DATE, new BigDecimal("1000.00"), "Test entry");
    }

    // ─────────────────────────────────────────────────────────
    // Happy Path
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should successfully create JournalMaster when all inputs are valid")
    void shouldCreate_WhenAllInputsAreValid() {
        when(journalMasterRepository.existsById("JV00000001")).thenReturn(false);
        when(journalMasterRepository.save(validMaster)).thenReturn(validMaster);

        JournalMaster result = createJournalMaster.execute(validMaster);

        assertThat(result).isNotNull();
        assertThat(result.getJId()).isEqualTo("JV00000001");
        assertThat(result.getJDoc()).isEqualTo("JV");
        assertThat(result.getJAmount()).isEqualByComparingTo("1000.00");
        verify(journalMasterRepository).save(validMaster);
    }

    @Test
    @DisplayName("Should default jDate to now when jDate is null")
    void shouldDefaultDate_WhenDateIsNull() {
        JournalMaster master = new JournalMaster("JV00000001", "JV", null, new BigDecimal("500.00"), "Entry");
        when(journalMasterRepository.existsById("JV00000001")).thenReturn(false);
        when(journalMasterRepository.save(master)).thenReturn(master);

        createJournalMaster.execute(master);

        // jDate must have been set before save() was called
        assertThat(master.getJDate()).isNotNull();
    }

    @Test
    @DisplayName("Should default jAmount to ZERO when jAmount is null")
    void shouldDefaultAmount_WhenAmountIsNull() {
        JournalMaster master = new JournalMaster("JV00000001", "JV", FIXED_DATE, null, "Entry");
        when(journalMasterRepository.existsById("JV00000001")).thenReturn(false);
        when(journalMasterRepository.save(master)).thenReturn(master);

        createJournalMaster.execute(master);

        assertThat(master.getJAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should allow null narration (narration is optional)")
    void shouldCreate_WhenNarrationIsNull() {
        JournalMaster master = new JournalMaster("JV00000001", "JV", FIXED_DATE, BigDecimal.ZERO, null);
        when(journalMasterRepository.existsById("JV00000001")).thenReturn(false);
        when(journalMasterRepository.save(master)).thenReturn(master);

        JournalMaster result = createJournalMaster.execute(master);

        assertThat(result).isNotNull();
        verify(journalMasterRepository).save(master);
    }

    // ─────────────────────────────────────────────────────────
    // Journal ID Auto-Generation
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should auto-generate jId when jId is null")
    void shouldAutoGenerateId_WhenJIdIsNull() {
        JournalMaster master = new JournalMaster(null, "JV", FIXED_DATE, BigDecimal.ZERO, null);
        when(journalMasterRepository.generateNextId()).thenReturn("JV20260001");
        when(journalMasterRepository.existsById("JV20260001")).thenReturn(false);
        when(journalMasterRepository.save(master)).thenReturn(master);

        createJournalMaster.execute(master);

        assertThat(master.getJId()).isEqualTo("JV20260001");
        verify(journalMasterRepository).generateNextId();
    }

    @Test
    @DisplayName("Should auto-generate jId when jId is blank")
    void shouldAutoGenerateId_WhenJIdIsBlank() {
        JournalMaster master = new JournalMaster("   ", "JV", FIXED_DATE, BigDecimal.ZERO, null);
        when(journalMasterRepository.generateNextId()).thenReturn("JV20260001");
        when(journalMasterRepository.existsById("JV20260001")).thenReturn(false);
        when(journalMasterRepository.save(master)).thenReturn(master);

        createJournalMaster.execute(master);

        assertThat(master.getJId()).isEqualTo("JV20260001");
        verify(journalMasterRepository).generateNextId();
    }

    @Test
    @DisplayName("Should skip auto-generation and use caller-supplied jId")
    void shouldUseProvidedId_WhenJIdIsSupplied() {
        JournalMaster master = new JournalMaster("JV00000001", "JV", FIXED_DATE, BigDecimal.ZERO, null);
        when(journalMasterRepository.existsById("JV00000001")).thenReturn(false);
        when(journalMasterRepository.save(master)).thenReturn(master);

        createJournalMaster.execute(master);

        verify(journalMasterRepository, never()).generateNextId();
        verify(journalMasterRepository).save(master);
    }

    // ─────────────────────────────────────────────────────────
    // Document Type Validation
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should throw ValidationException when jDoc is null")
    void shouldThrow_WhenJDocIsNull() {
        // existsById is NOT stubbed — jDoc validation fires before the existence check
        JournalMaster master = new JournalMaster("JV00000001", null, FIXED_DATE, BigDecimal.ZERO, null);

        assertThatThrownBy(() -> createJournalMaster.execute(master))
                .isInstanceOf(JournalMasterValidationException.class)
                .hasMessageContaining("Document type must be exactly 2 characters");

        verify(journalMasterRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ValidationException when jDoc is not exactly 2 characters")
    void shouldThrow_WhenJDocIsWrongLength() {
        // existsById is NOT stubbed — jDoc validation fires before the existence check
        JournalMaster master = new JournalMaster("JV00000001", "JVX", FIXED_DATE, BigDecimal.ZERO, null);

        assertThatThrownBy(() -> createJournalMaster.execute(master))
                .isInstanceOf(JournalMasterValidationException.class)
                .hasMessageContaining("Document type must be exactly 2 characters");

        verify(journalMasterRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────
    // Narration Validation
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should throw ValidationException when narration exceeds 100 characters")
    void shouldThrow_WhenNarrationTooLong() {
        // existsById is NOT stubbed — narration validation fires before the existence check
        String longNarr = "A".repeat(101);
        JournalMaster master = new JournalMaster("JV00000001", "JV", FIXED_DATE, BigDecimal.ZERO, longNarr);

        assertThatThrownBy(() -> createJournalMaster.execute(master))
                .isInstanceOf(JournalMasterValidationException.class)
                .hasMessageContaining("Narration must not exceed 100 characters");

        verify(journalMasterRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should accept narration of exactly 100 characters")
    void shouldCreate_WhenNarrationIsExactly100Chars() {
        String narr = "A".repeat(100);
        JournalMaster master = new JournalMaster("JV00000001", "JV", FIXED_DATE, BigDecimal.ZERO, narr);
        when(journalMasterRepository.existsById("JV00000001")).thenReturn(false);
        when(journalMasterRepository.save(master)).thenReturn(master);

        JournalMaster result = createJournalMaster.execute(master);

        assertThat(result).isNotNull();
        verify(journalMasterRepository).save(master);
    }

    // ─────────────────────────────────────────────────────────
    // Duplicate Check
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should throw AlreadyExistsException when journal ID already exists")
    void shouldThrow_WhenDuplicateId() {
        when(journalMasterRepository.existsById("JV00000001")).thenReturn(true);

        assertThatThrownBy(() -> createJournalMaster.execute(validMaster))
                .isInstanceOf(JournalMasterAlreadyExistsException.class)
                .hasMessageContaining("Journal voucher with ID JV00000001 already exists");

        verify(journalMasterRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────
    // Execution Order: generateNextId → jDoc → existsById → save
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should call generateNextId() when jId is null, then proceed through the full pipeline")
    void shouldCallGenerateNextId_ThenProceedToSave_WhenJIdIsNull() {
        JournalMaster master = new JournalMaster(null, "JV", FIXED_DATE, BigDecimal.ZERO, null);
        when(journalMasterRepository.generateNextId()).thenReturn("JV20260001");
        when(journalMasterRepository.existsById("JV20260001")).thenReturn(false);
        when(journalMasterRepository.save(master)).thenReturn(master);

        createJournalMaster.execute(master);

        verify(journalMasterRepository).generateNextId();
        verify(journalMasterRepository).existsById("JV20260001");
        verify(journalMasterRepository).save(master);
    }
}
