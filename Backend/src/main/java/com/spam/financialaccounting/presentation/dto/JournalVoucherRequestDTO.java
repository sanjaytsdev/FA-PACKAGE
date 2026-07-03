package com.spam.financialaccounting.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request body for posting a whole journal voucher (header + lines) in one call. */
public class JournalVoucherRequestDTO {

    @JsonProperty("jDoc")
    @Size(max = 2, message = "Document type must be at most 2 characters")
    private String jDoc;

    @JsonProperty("jDate")
    private LocalDateTime jDate;

    @JsonProperty("jNarr")
    @NotBlank(message = "Narration is required")
    @Size(min = 5, max = 100, message = "Narration must be between 5 and 100 characters")
    private String jNarr;

    @NotNull(message = "A voucher must include lines")
    @Size(min = 2, message = "A voucher must have at least two lines")
    @Valid
    private List<Line> lines;

    public String getJDoc() {
        return jDoc;
    }

    public void setJDoc(String jDoc) {
        this.jDoc = jDoc;
    }

    public LocalDateTime getJDate() {
        return jDate;
    }

    public void setJDate(LocalDateTime jDate) {
        this.jDate = jDate;
    }

    public String getJNarr() {
        return jNarr;
    }

    public void setJNarr(String jNarr) {
        this.jNarr = jNarr;
    }

    public List<Line> getLines() {
        return lines;
    }

    public void setLines(List<Line> lines) {
        this.lines = lines;
    }

    public static class Line {

        @JsonProperty("jCode")
        @NotBlank(message = "Account code is required")
        @Pattern(regexp = "^.{5}$", message = "Account code must be exactly 5 characters")
        private String jCode;

        @JsonProperty("jDrCr")
        @NotBlank(message = "Debit/Credit indicator is required")
        @Pattern(regexp = "^(?i)(DR|CR)$", message = "Debit/Credit indicator must be either 'DR' or 'CR'")
        private String jDrCr;

        @JsonProperty("jAmount")
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.001", message = "Amount must be greater than zero")
        private BigDecimal jAmount;

        public String getJCode() {
            return jCode;
        }

        public void setJCode(String jCode) {
            this.jCode = jCode;
        }

        public String getJDrCr() {
            return jDrCr;
        }

        public void setJDrCr(String jDrCr) {
            this.jDrCr = jDrCr;
        }

        public BigDecimal getJAmount() {
            return jAmount;
        }

        public void setJAmount(BigDecimal jAmount) {
            this.jAmount = jAmount;
        }
    }
}
