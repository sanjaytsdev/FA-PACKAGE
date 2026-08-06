package com.spam.financialaccounting.desktop.view;

import com.spam.financialaccounting.desktop.api.ApiClient;
import com.spam.financialaccounting.desktop.config.UiConstants;
import com.spam.financialaccounting.desktop.model.TrialBalanceRow;
import com.spam.financialaccounting.desktop.ui.AsyncUi;
import com.spam.financialaccounting.desktop.ui.UiUtils;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import java.time.LocalDate;

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
        statusLabel.setStyle(UiConstants.STYLE_STATUS_MUTED);
        LocalDate d = asOfPicker.getValue();
        final String asOf = d != null ? d.toString() : null;
        AsyncUi.fetch(() -> apiClient.getTrialBalance(asOf), report -> {
            table.setItems(FXCollections.observableArrayList(report.getRows()));
            if (report.isBalanced()) {
                statusLabel.setText("● IN BALANCE");
                statusLabel.setStyle(UiConstants.STYLE_STATUS_SUCCESS);
            } else {
                statusLabel.setText("● OUT OF BALANCE");
                statusLabel.setStyle(UiConstants.STYLE_STATUS_DANGER);
            }
            totalsLabel.setText("Total Debit: " + UiUtils.money(report.getTotalDebit())
                    + "      Total Credit: " + UiUtils.money(report.getTotalCredit()));
        }, ex -> {
            statusLabel.setText("Failed to load trial balance");
            statusLabel.setStyle(UiConstants.STYLE_STATUS_DANGER);
            UiUtils.showAlert(Alert.AlertType.ERROR, "Fetch Error", "Failed to load trial balance", ex.getMessage());
        });
    }
}
