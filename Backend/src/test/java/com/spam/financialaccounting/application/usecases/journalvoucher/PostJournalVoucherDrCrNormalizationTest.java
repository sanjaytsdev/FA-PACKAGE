package com.spam.financialaccounting.application.usecases.journalvoucher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spam.financialaccounting.domain.entity.FASubGroup;
import com.spam.financialaccounting.domain.entity.JournalDetail;
import com.spam.financialaccounting.domain.entity.JournalMaster;
import com.spam.financialaccounting.domain.repository.FASubGroupRepository;
import com.spam.financialaccounting.domain.repository.JournalDetailRepository;
import com.spam.financialaccounting.domain.repository.JournalMasterRepository;
import com.spam.financialaccounting.infrastructure.persistence.repository.AuditLogRepository;
import com.spam.financialaccounting.infrastructure.persistence.repository.PeriodLockRepository;

/**
 * Checks the journal-voucher posting path (the main write path for journal
 * lines) takes a debit/credit flag in any case and stores it uppercased, so
 * DR/CR validation works the same whether the client sends "dr", "Dr", "dR",
 * "cr" or "CR".
 */
@ExtendWith(MockitoExtension.class)
class PostJournalVoucherDrCrNormalizationTest {

    @Mock
    private JournalMasterRepository journalMasterRepository;
    @Mock
    private JournalDetailRepository journalDetailRepository;
    @Mock
    private FASubGroupRepository faSubGroupRepository;
    @Mock
    private PeriodLockRepository periodLockRepository;
    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private PostJournalVoucher postJournalVoucher;

    private FASubGroup activeAccount(String code) {
        return new FASubGroup(code, code + " desc", "A1", "00", BigDecimal.ZERO, "DR", "T");
    }

    @ParameterizedTest(name = "debit line sent as \"{0}\" is persisted as \"{1}\"")
    @CsvSource({
            "dr, DR",
            "Dr, DR",
            "dR, DR",
            "cr, CR",
            "CR, CR",
    })
    @DisplayName("Posting normalizes a mixed-case debit/credit indicator to canonical uppercase")
    void post_normalizesDrCrIndicator(String input, String expectedCanonical) {
        // First line is the case we're testing; the second balances it on the
        // opposite side so the voucher posts. If the first line ends up DR, the
        // balancing line is CR, and the other way round.
        String balancing = "DR".equals(expectedCanonical) ? "CR" : "DR";
        JournalMaster header = new JournalMaster(null, "JV", null, null, "Mixed-case DR/CR");
        List<JournalDetail> lines = List.of(
                new JournalDetail(null, "SG01", input, new BigDecimal("100.00")),
                new JournalDetail(null, "SG02", balancing, new BigDecimal("100.00")));

        when(faSubGroupRepository.findByCode(anyString()))
                .thenAnswer(inv -> Optional.of(activeAccount(inv.getArgument(0))));
        when(journalMasterRepository.generateNextId()).thenReturn("JV20260001");
        when(periodLockRepository.isLocked(any(LocalDate.class))).thenReturn(false);

        postJournalVoucher.execute(header, lines);

        // The line objects get updated in place and passed to save() uppercased.
        ArgumentCaptor<JournalDetail> saved = ArgumentCaptor.forClass(JournalDetail.class);
        verify(journalDetailRepository, times(2)).save(saved.capture());

        JournalDetail firstLine = saved.getAllValues().stream()
                .filter(l -> "SG01".equals(l.getJCode()))
                .findFirst()
                .orElseThrow();
        assertThat(firstLine.getJDrCr()).isEqualTo(expectedCanonical);
    }
}
