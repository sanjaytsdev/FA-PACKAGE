package com.spam.financialaccounting.desktop.view;

import com.spam.financialaccounting.desktop.api.ApiClient;
import com.spam.financialaccounting.desktop.config.UiConstants;
import com.spam.financialaccounting.desktop.model.ReportLineItem;
import com.spam.financialaccounting.desktop.ui.AsyncUi;
import com.spam.financialaccounting.desktop.ui.ReportSection;
import com.spam.financialaccounting.desktop.ui.UiUtils;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ProfitAndLossView extends VBox {

    private final ApiClient apiClient;
    private final DatePicker asOfPicker;
    private final TableView<ReportLineItem> revenueTable;
    private final Label revenueTotal;
    private final TableView<ReportLineItem> expensesTable;
    private final Label expensesTotal;
    private final Label netLabel;

    public ProfitAndLossView(ApiClient apiClient) {
        this.apiClient = apiClient;
        this.getStyleClass().add("content-pane");
        this.setSpacing(20);

        Label title = new Label("Profit & Loss");
        title.getStyleClass().add("view-title");
        Label subtitle = new Label(
                "Income statement cumulative to the reporting date: revenue less expenses for every voucher dated on or before it.");
        subtitle.getStyleClass().add("view-subtitle");
        subtitle.setWrapText(true);
        VBox header = new VBox(5, title, subtitle);

        asOfPicker = new DatePicker();
        asOfPicker.setPromptText("As of (today)");
        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> load());
        HBox controls = new HBox(10, new Label("As of date:"), asOfPicker, refreshBtn);
        controls.setAlignment(Pos.CENTER_LEFT);

        revenueTable = new TableView<>();
        revenueTotal = new Label();
        VBox revenueSection = buildSection("Revenue", revenueTable, revenueTotal);

        expensesTable = new TableView<>();
        expensesTotal = new Label();
        VBox expensesSection = buildSection("Expenses", expensesTable, expensesTotal);

        netLabel = new Label();
        netLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        VBox netBox = new VBox(netLabel);
        netBox.getStyleClass().add("card");

        this.getChildren().addAll(header, controls, revenueSection, expensesSection, netBox);

        asOfPicker.valueProperty().addListener((obs, o, n) -> load());
        load();
    }

    private VBox buildSection(String name, TableView<ReportLineItem> tableView, Label totalLabel) {
        return ReportSection.build(name, tableView, totalLabel, 120, 280, 160);
    }

    private void load() {
        LocalDate d = asOfPicker.getValue();
        final String asOf = d != null ? d.toString() : null;
        AsyncUi.fetch(() -> apiClient.getProfitAndLoss(asOf), report -> {
            revenueTable.setItems(FXCollections.observableArrayList(report.getRevenue()));
            revenueTotal.setText("Total Revenue: " + UiUtils.money(report.getTotalRevenue()));
            expensesTable.setItems(FXCollections.observableArrayList(report.getExpenses()));
            expensesTotal.setText("Total Expenses: " + UiUtils.money(report.getTotalExpenses()));

            BigDecimal net = report.getNetProfit() != null ? report.getNetProfit() : BigDecimal.ZERO;
            boolean profitable = net.compareTo(BigDecimal.ZERO) >= 0;
            netLabel.setText((profitable ? "Net Profit: " : "Net Loss: ") + UiUtils.money(net));
            netLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: "
                    + (profitable ? UiConstants.COLOR_SUCCESS : UiConstants.COLOR_DANGER) + ";");
        }, "Fetch Error", "Failed to load profit & loss");
    }
}
