package com.spam.financialaccounting.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request body for importing opening balances as one Opening Balance voucher. */
public class OpeningBalanceImportDTO {

    /** Date the opening balances kick in; defaults to today if left out. */
    private LocalDate openingDate;

    @Size(max = 100, message = "Narration must not exceed 100 characters")
    private String narration;

    @NotNull(message = "Opening balances must include lines")
    @Size(min = 2, message = "Opening balances need at least two lines (one debit and one credit)")
    @Valid
    private List<Line> lines;

    public LocalDate getOpeningDate() {
        return openingDate;
    }

    public void setOpeningDate(LocalDate openingDate) {
        this.openingDate = openingDate;
    }

    public String getNarration() {
        return narration;
    }

    public void setNarration(String narration) {
        this.narration = narration;
    }

    public List<Line> getLines() {
        return lines;
    }

    public void setLines(List<Line> lines) {
        this.lines = lines;
    }

    public static class Line {

        @NotBlank(message = "Account code is required")
        @Pattern(regexp = "^.{5}$", message = "Account code must be exactly 5 characters")
        private String accountCode;

        @NotBlank(message = "Debit/Credit indicator is required")
        @Pattern(regexp = "^(?i)(DR|CR)$", message = "Debit/Credit indicator must be either 'DR' or 'CR'")
        private String drCr;

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.001", message = "Amount must be greater than zero")
        private BigDecimal amount;

        public String getAccountCode() {
            return accountCode;
        }

        public void setAccountCode(String accountCode) {
            this.accountCode = accountCode;
        }

        public String getDrCr() {
            return drCr;
        }

        public void setDrCr(String drCr) {
            this.drCr = drCr;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }
}
