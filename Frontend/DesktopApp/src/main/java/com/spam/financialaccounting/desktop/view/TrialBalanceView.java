package com.spam.financialaccounting.desktop.view;

import com.spam.financialaccounting.desktop.api.ApiClient;
import com.spam.financialaccounting.desktop.model.TrialBalance;
import com.spam.financialaccounting.desktop.model.TrialBalanceRow;
import com.spam.financialaccounting.desktop.ui.UiUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

public class TrialBalanceView extends VBox {

    private final ApiClient apiClient;
    private final DatePicker asOfPicker;
    private final Label statusLabel;
    private final TableView<TrialBalanceRow> table;
    private final Label totalsLabel;

    public TrialBalanceView(ApiClient apiClient) {
        this.apiClient = apiClient;
        this.getStyleClass().add("content-pane");
        this.setSpacing(20);

        Label title = new Label("Trial Balance");
        title.getStyleClass().add("view-title");
        Label subtitle = new Label(
                "Account balances derived from opening balances and posted journals. The books balance when total debits equal total credits.");
        subtitle.getStyleClass().add("view-subtitle");
        subtitle.setWrapText(true);
        VBox header = new VBox(5, title, subtitle);

        asOfPicker = new DatePicker();
        asOfPicker.setPromptText("As of (today)");
        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> load());
        Label asOfLabel = new Label("As of date:");
        HBox controls = new HBox(10, asOfLabel, asOfPicker, refreshBtn);
        controls.setAlignment(Pos.CENTER_LEFT);

        statusLabel = new Label("Loading...");
        statusLabel.setStyle("-fx-font-weight: bold;");

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No ledger accounts found"));
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<TrialBalanceRow, String> colCode = new TableColumn<>("Account");
        colCode.setCellValueFactory(new PropertyValueFactory<>("accountCode"));
        colCode.setPrefWidth(120);
        TableColumn<TrialBalanceRow, String> colDesc = new TableColumn<>("Description");
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        colDesc.setPrefWidth(260);
        TableColumn<TrialBalanceRow, java.math.BigDecimal> colDebit = UiUtils.moneyColumn("Debit", "debit");
        colDebit.setPrefWidth(140);
        TableColumn<TrialBalanceRow, java.math.BigDecimal> colCredit = UiUtils.moneyColumn("Credit", "credit");
        colCredit.setPrefWidth(140);
        table.getColumns().addAll(colCode, colDesc, colDebit, colCredit);

        totalsLabel = new Label();
        totalsLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        this.getChildren().addAll(header, controls, statusLabel, table, totalsLabel);

        asOfPicker.valueProperty().addListener((obs, o, n) -> load());
        load();
    }

    private void load() {
        statusLabel.setText("Loading...");
        statusLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold;");
        LocalDate d = asOfPicker.getValue();
        final String asOf = d != null ? d.toString() : null;
        CompletableFuture.runAsync(() -> {
            try {
                TrialBalance report = apiClient.getTrialBalance(asOf);
                Platform.runLater(() -> {
                    table.setItems(FXCollections.observableArrayList(report.getRows()));
                    if (report.isBalanced()) {
                        statusLabel.setText("● IN BALANCE");
                        statusLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                    } else {
                        statusLabel.setText("● OUT OF BALANCE");
                        statusLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                    }
                    totalsLabel.setText("Total Debit: " + UiUtils.money(report.getTotalDebit())
                            + "      Total Credit: " + UiUtils.money(report.getTotalCredit()));
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    statusLabel.setText("Failed to load trial balance");
                    statusLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                    UiUtils.showAlert(Alert.AlertType.ERROR, "Fetch Error", "Failed to load trial balance",
                            ex.getMessage());
                });
            }
        });
    }
}
