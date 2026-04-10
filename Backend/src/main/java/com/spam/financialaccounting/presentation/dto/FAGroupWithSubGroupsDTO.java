package com.spam.financialaccounting.presentation.dto;

import java.math.BigDecimal;
import java.util.List;

public class FAGroupWithSubGroupsDTO {

    private String accountCode;
    private String accountDescription;
    private String accountType;
    private BigDecimal accountCurrentBalance;

    private List<FASubGroupDTO> subGroups;

    public FAGroupWithSubGroupsDTO(String accountCode, String accountDescription, String accountType,
            BigDecimal accountCurrentBalance, List<FASubGroupDTO> subGroups) {
        this.accountCode = accountCode;
        this.accountDescription = accountDescription;
        this.accountType = accountType;
        this.accountCurrentBalance = accountCurrentBalance;
        this.subGroups = subGroups;
    }

    public String getAccountCode() {
        return accountCode;
    }

    public String getAccountDescription() {
        return accountDescription;
    }

    public String getAccountType() {
        return accountType;
    }

    public BigDecimal getAccountCurrentBalance() {
        return accountCurrentBalance;
    }

    public List<FASubGroupDTO> getSubGroups() {
        return subGroups;
    }

    
    
}
