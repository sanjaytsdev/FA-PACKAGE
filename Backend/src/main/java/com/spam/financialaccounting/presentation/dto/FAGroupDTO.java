package com.spam.financialaccounting.presentation.dto;

import java.math.BigDecimal;

public class FAGroupDTO {
    private String accountCode;  // Account group code (e.g., '01' for Asset)
    private String accountDescription;  // Account group description (e.g., 'Asset')
    private String accountType;  // Type (e.g., '0' for Asset, '1' for Liability)
    private BigDecimal accountCurrentBalance;  // Current balance (for aggregations)

    public FAGroupDTO() {
    }

    //Constructor
    public FAGroupDTO(String accountCode, String accountDescription, String accountType, BigDecimal accountCurrentBalance) {
        this.accountCode = accountCode;
        this.accountDescription = accountDescription;
        this.accountType = accountType;
        this.accountCurrentBalance = accountCurrentBalance;
    }

    //Getters and Setters
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
}
