package com.spam.financialaccounting.presentation.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.spam.financialaccounting.application.usecases.report.GetBalanceSheet;
import com.spam.financialaccounting.application.usecases.report.GetProfitAndLoss;
import com.spam.financialaccounting.application.usecases.report.GetTrialBalance;
import com.spam.financialaccounting.presentation.dto.BalanceSheetDTO;
import com.spam.financialaccounting.presentation.dto.ProfitAndLossDTO;
import com.spam.financialaccounting.presentation.dto.TrialBalanceDTO;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final GetTrialBalance getTrialBalance;
    private final GetProfitAndLoss getProfitAndLoss;
    private final GetBalanceSheet getBalanceSheet;

    public ReportController(GetTrialBalance getTrialBalance, GetProfitAndLoss getProfitAndLoss,
            GetBalanceSheet getBalanceSheet) {
        this.getTrialBalance = getTrialBalance;
        this.getProfitAndLoss = getProfitAndLoss;
        this.getBalanceSheet = getBalanceSheet;
    }

    /**
     * Trial balance as of a reporting date.
     *
     * <p>Examples:
     * <pre>
     *   GET /api/v1/reports/trial-balance                       (defaults to today)
     *   GET /api/v1/reports/trial-balance?asOfDate=2026-03-31   (historical close)
     * </pre>
     *
     * Only opening balances and vouchers dated on or before {@code asOfDate} count,
     * so future-dated entries can't skew a historical report.
     */
    @GetMapping("/trial-balance")
    public ResponseEntity<TrialBalanceDTO> trialBalance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        LocalDate reportDate = asOfDate != null ? asOfDate : LocalDate.now();
        return ResponseEntity.ok(getTrialBalance.execute(reportDate));
    }

    /**
     * Profit &amp; Loss (income statement) as of a reporting date. Runs cumulative to
     * date: revenue minus expenses for every voucher dated on or before the date.
     *
     * <pre>
     *   GET /api/v1/reports/profit-and-loss                     (defaults to today)
     *   GET /api/v1/reports/profit-and-loss?asOfDate=2026-03-31
     * </pre>
     */
    @GetMapping("/profit-and-loss")
    public ResponseEntity<ProfitAndLossDTO> profitAndLoss(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        LocalDate reportDate = asOfDate != null ? asOfDate : LocalDate.now();
        return ResponseEntity.ok(getProfitAndLoss.execute(reportDate));
    }

    /**
     * Balance Sheet as of a reporting date. Assets, liabilities and equity sit on
     * their natural sides. Unclosed earnings get carried into equity as a
     * "Net Income" line, so assets = liabilities + equity.
     *
     * <pre>
     *   GET /api/v1/reports/balance-sheet                     (defaults to today)
     *   GET /api/v1/reports/balance-sheet?asOfDate=2026-03-31
     * </pre>
     */
    @GetMapping("/balance-sheet")
    public ResponseEntity<BalanceSheetDTO> balanceSheet(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        LocalDate reportDate = asOfDate != null ? asOfDate : LocalDate.now();
        return ResponseEntity.ok(getBalanceSheet.execute(reportDate));
    }
}
