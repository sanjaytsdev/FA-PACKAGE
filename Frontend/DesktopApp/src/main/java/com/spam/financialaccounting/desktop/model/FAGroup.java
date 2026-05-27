package com.spam.financialaccounting.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FAGroup {
    private String accountCode;
    private String accountDescription;
    private String accountType;
    private BigDecimal accountCurrentBalance;

    public FAGroup() {
    }

    public FAGroup(String accountCode, String accountDescription, String accountType,
            BigDecimal accountCurrentBalance) {
        this.accountCode = accountCode;
        this.accountDescription = accountDescription;
        this.accountType = accountType;
        this.accountCurrentBalance = accountCurrentBalance;
    }

    public String getAccountCode() {
        return accountCode;
    }

    public void setAccountCode(String accountCode) {
        this.accountCode = accountCode;
    }

    public String getAccountDescription() {
        return accountDescription;
    }

    public void setAccountDescription(String accountDescription) {
        this.accountDescription = accountDescription;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public BigDecimal getAccountCurrentBalance() {
        return accountCurrentBalance;
    }

    public void setAccountCurrentBalance(BigDecimal accountCurrentBalance) {
        this.accountCurrentBalance = accountCurrentBalance;
    }

    @Override
    public String toString() {
        return accountCode + " - " + accountDescription;
    }
}