package com.spam.financialaccounting.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class JournalMaster {
    private String jId;  // Journal voucher ID (e.g., 'JV123456789')
    private String jDoc;  // Document type (e.g., 'JV' for Journal Voucher)
    private LocalDateTime jDate;  // Voucher date
    private BigDecimal jAmount;  // Total voucher amount (calculated from lines)
    private String jNarr;  // Voucher narration/description (optional)

    // Constructor
    public JournalMaster(String jId, String jDoc, LocalDateTime jDate, BigDecimal jAmount, String jNarr) {
        this.jId = jId;
        this.jDoc = jDoc;
        this.jDate = jDate;
        this.jAmount = jAmount;
        this.jNarr = jNarr;
    }

    // Getters and setters
    public String getJId() {
        return jId;
    }

    public void setJId(String jId) {
        this.jId = jId;
    }

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

    public BigDecimal getJAmount() {
        return jAmount;
    }

    public void setJAmount(BigDecimal jAmount) {
        this.jAmount = jAmount;
    }

    public String getJNarr() {
        return jNarr;
    }

    public void setJNarr(String jNarr) {
        this.jNarr = jNarr;
    }
}
