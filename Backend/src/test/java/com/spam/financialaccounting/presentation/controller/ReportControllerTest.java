package com.spam.financialaccounting.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.spam.financialaccounting.application.usecases.report.GetBalanceSheet;
import com.spam.financialaccounting.application.usecases.report.GetProfitAndLoss;
import com.spam.financialaccounting.application.usecases.report.GetTrialBalance;
import com.spam.financialaccounting.presentation.dto.BalanceSheetDTO;
import com.spam.financialaccounting.presentation.dto.ProfitAndLossDTO;
import com.spam.financialaccounting.presentation.dto.TrialBalanceDTO;

@WebMvcTest(ReportController.class)
public class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetTrialBalance getTrialBalance;

    @MockBean
    private GetProfitAndLoss getProfitAndLoss;

    @MockBean
    private GetBalanceSheet getBalanceSheet;

    private TrialBalanceDTO sample(LocalDate asOf) {
        return new TrialBalanceDTO(
                asOf,
                List.of(new TrialBalanceDTO.Row("10001", "Cash", new BigDecimal("1000"), BigDecimal.ZERO)),
                new BigDecimal("1000"), new BigDecimal("1000"), true);
    }

    @Test
    @DisplayName("GET /trial-balance?asOfDate=2026-03-31 → uses the supplied date")
    void usesSuppliedAsOfDate() throws Exception {
        LocalDate asOf = LocalDate.of(2026, 3, 31);
        when(getTrialBalance.execute(any())).thenReturn(sample(asOf));

        mockMvc.perform(get("/api/v1/reports/trial-balance").param("asOfDate", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asOfDate").value("2026-03-31"))
                .andExpect(jsonPath("$.balanced").value(true))
                .andExpect(jsonPath("$.rows[0].accountCode").value("10001"));

        ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);
        org.mockito.Mockito.verify(getTrialBalance).execute(captor.capture());
        assertThat(captor.getValue()).isEqualTo(asOf);
    }

    @Test
    @DisplayName("GET /trial-balance with no param → defaults to today")
    void defaultsToToday() throws Exception {
        when(getTrialBalance.execute(any())).thenReturn(sample(LocalDate.now()));

        mockMvc.perform(get("/api/v1/reports/trial-balance"))
                .andExpect(status().isOk());

        ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);
        org.mockito.Mockito.verify(getTrialBalance).execute(captor.capture());
        assertThat(captor.getValue()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("GET /profit-and-loss?asOfDate=... → 200 with revenue, expenses and net profit")
    void profitAndLoss() throws Exception {
        ProfitAndLossDTO pl = new ProfitAndLossDTO(
                LocalDate.of(2026, 3, 31),
                List.of(new ProfitAndLossDTO.LineItem("40001", "Sales Income", new BigDecimal("500"))),
                List.of(new ProfitAndLossDTO.LineItem("50001", "Rent Expense", new BigDecimal("200"))),
                new BigDecimal("500"), new BigDecimal("200"), new BigDecimal("300"));
        when(getProfitAndLoss.execute(any())).thenReturn(pl);

        mockMvc.perform(get("/api/v1/reports/profit-and-loss").param("asOfDate", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asOfDate").value("2026-03-31"))
                .andExpect(jsonPath("$.totalRevenue").value(500))
                .andExpect(jsonPath("$.totalExpenses").value(200))
                .andExpect(jsonPath("$.netProfit").value(300))
                .andExpect(jsonPath("$.revenue[0].accountCode").value("40001"));

        ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);
        org.mockito.Mockito.verify(getProfitAndLoss).execute(captor.capture());
        assertThat(captor.getValue()).isEqualTo(LocalDate.of(2026, 3, 31));
    }

    @Test
    @DisplayName("GET /balance-sheet → 200, balanced, with Net Income carried into equity")
    void balanceSheet() throws Exception {
        BalanceSheetDTO bs = new BalanceSheetDTO(
                LocalDate.of(2026, 3, 31),
                List.of(new BalanceSheetDTO.LineItem("10001", "Cash", new BigDecimal("2300"))),
                List.of(new BalanceSheetDTO.LineItem("20001", "Bank Loan", new BigDecimal("500"))),
                List.of(new BalanceSheetDTO.LineItem("30001", "Share Capital", new BigDecimal("1500")),
                        new BalanceSheetDTO.LineItem("NET_INCOME", "Net Income", new BigDecimal("300"))),
                new BigDecimal("2300"), new BigDecimal("500"), new BigDecimal("1800"), true);
        when(getBalanceSheet.execute(any())).thenReturn(bs);

        mockMvc.perform(get("/api/v1/reports/balance-sheet").param("asOfDate", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanced").value(true))
                .andExpect(jsonPath("$.totalAssets").value(2300))
                .andExpect(jsonPath("$.totalLiabilities").value(500))
                .andExpect(jsonPath("$.totalEquity").value(1800))
                .andExpect(jsonPath("$.equity[1].accountCode").value("NET_INCOME"))
                .andExpect(jsonPath("$.equity[1].description").value("Net Income"));
    }
}
