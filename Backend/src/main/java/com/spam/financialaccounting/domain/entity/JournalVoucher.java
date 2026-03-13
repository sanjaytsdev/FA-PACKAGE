package com.spam.financialaccounting.domain.entity;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class JournalVoucher {
    private Long id;
    private LocalDate date;
    private List<JournalLine> lines;
}
