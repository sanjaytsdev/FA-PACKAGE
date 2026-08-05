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
import java.time.LocalDate;

public class BalanceSheetView extends VBox {

    private final ApiClient apiClient;
    private final DatePicker asOfPicker;
    private final Label statusLabel;
    private final TableView<ReportLineItem> assetsTable;
    private final Label assetsTotal;
    private final TableView<ReportLineItem> liabilitiesTable;
    private final Label liabilitiesTotal;
    private final TableView<ReportLineItem> equityTable;
    private final Label equityTotal;

    public BalanceSheetView(ApiClient apiClient) {
        this.apiClient = apiClient;
        this.getStyleClass().add("content-pane");
        this.setSpacing(20);

        Label title = new Label("Balance Sheet");
        title.getStyleClass().add("view-title");
        Label subtitle = new Label(
                "Financial position as of the reporting date. Unclosed earnings appear in equity as \"Net Income\" so that Assets = Liabilities + Equity.");
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

        statusLabel = new Label("Loading...");
        statusLabel.setStyle("-fx-font-weight: bold;");

        assetsTable = new TableView<>();
        assetsTotal = new Label();
        VBox assetsSection = buildSection("Assets", assetsTable, assetsTotal);

        liabilitiesTable = new TableView<>();
        liabilitiesTotal = new Label();
        VBox liabilitiesSection = buildSection("Liabilities", liabilitiesTable, liabilitiesTotal);

        equityTable = new TableView<>();
        equityTotal = new Label();
        VBox equitySection = buildSection("Equity", equityTable, equityTotal);

        HBox.setHgrow(assetsSection, Priority.ALWAYS);
        VBox rightColumn = new VBox(20, liabilitiesSection, equitySection);
        HBox.setHgrow(rightColumn, Priority.ALWAYS);
        HBox columns = new HBox(20, assetsSection, rightColumn);
        VBox.setVgrow(columns, Priority.ALWAYS);

        this.getChildren().addAll(header, controls, statusLabel, columns);

        asOfPicker.valueProperty().addListener((obs, o, n) -> load());
        load();
    }

    private VBox buildSection(String name, TableView<ReportLineItem> tableView, Label totalLabel) {
        return ReportSection.build(name, tableView, totalLabel, 90, 200, 140);
    }

    private void load() {
        statusLabel.setText("Loading...");
        statusLabel.setStyle(UiConstants.STYLE_STATUS_MUTED);
        LocalDate d = asOfPicker.getValue();
        final String asOf = d != null ? d.toString() : null;
        AsyncUi.fetch(() -> apiClient.getBalanceSheet(asOf), report -> {
            assetsTable.setItems(FXCollections.observableArrayList(report.getAssets()));
            assetsTotal.setText("Total Assets: " + UiUtils.money(report.getTotalAssets()));
            liabilitiesTable.setItems(FXCollections.observableArrayList(report.getLiabilities()));
            liabilitiesTotal.setText("Total Liabilities: " + UiUtils.money(report.getTotalLiabilities()));
            equityTable.setItems(FXCollections.observableArrayList(report.getEquity()));
            equityTotal.setText("Total Equity: " + UiUtils.money(report.getTotalEquity()));

            if (report.isBalanced()) {
                statusLabel.setText("● BALANCED   (Assets " + UiUtils.money(report.getTotalAssets())
                        + " = Liabilities " + UiUtils.money(report.getTotalLiabilities())
                        + " + Equity " + UiUtils.money(report.getTotalEquity()) + ")");
                statusLabel.setStyle(UiConstants.STYLE_STATUS_SUCCESS);
            } else {
                statusLabel.setText("● OUT OF BALANCE");
                statusLabel.setStyle(UiConstants.STYLE_STATUS_DANGER);
            }
        }, ex -> {
            statusLabel.setText("Failed to load balance sheet");
            statusLabel.setStyle(UiConstants.STYLE_STATUS_DANGER);
            UiUtils.showAlert(Alert.AlertType.ERROR, "Fetch Error", "Failed to load balance sheet", ex.getMessage());
        });
    }
}
