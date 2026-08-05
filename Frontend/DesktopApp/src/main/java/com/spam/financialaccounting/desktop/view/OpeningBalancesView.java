package com.spam.financialaccounting.desktop.view;

import com.spam.financialaccounting.desktop.api.ApiClient;
import com.spam.financialaccounting.desktop.config.UiConstants;
import com.spam.financialaccounting.desktop.model.FASubGroup;
import com.spam.financialaccounting.desktop.ui.AsyncUi;
import com.spam.financialaccounting.desktop.ui.LedgerLineEditor;
import com.spam.financialaccounting.desktop.ui.UiUtils;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class OpeningBalancesView extends VBox {

    private final ApiClient apiClient;
    private final DatePicker datePicker;
    private final TextField narrInput;
    private final LedgerLineEditor lineEditor = new LedgerLineEditor(LedgerLineEditor.openingStyle());
    private final Label totalDrLabel;
    private final Label totalCrLabel;
    private final Label diffLabel;
    private final Button importBtn;

    public OpeningBalancesView(ApiClient apiClient) {
        this.apiClient = apiClient;
        this.getStyleClass().add("content-pane");
        this.setSpacing(20);

        Label title = new Label("Opening Balances");
        title.getStyleClass().add("view-title");
        Label subtitle = new Label(
                "Initialize the books. The import is accepted only when total opening debits equal total opening credits, and is recorded as a single Opening Balance voucher.");
        subtitle.getStyleClass().add("view-subtitle");
        subtitle.setWrapText(true);
        VBox header = new VBox(5, title, subtitle);

        datePicker = new DatePicker(LocalDate.now());
        narrInput = new TextField();
        narrInput.setPromptText("e.g. Opening balances FY2026");
        HBox.setHgrow(narrInput, Priority.ALWAYS);
        HBox headerFields = new HBox(15,
                new VBox(5, new Label("Opening Date:"), datePicker),
                new VBox(5, new Label("Narration:"), narrInput));
        HBox.setHgrow(headerFields.getChildren().get(1), Priority.ALWAYS);

        lineEditor.setOnChange(this::recalculate);
        ScrollPane scroll = new ScrollPane(lineEditor);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(260);

        Button addLineBtn = new Button("➕ Add Line");
        addLineBtn.getStyleClass().add("btn-secondary");
        addLineBtn.setOnAction(e -> lineEditor.addLine());

        totalDrLabel = new Label("Total Debit: 0.00");
        totalDrLabel.setStyle("-fx-text-fill: #6366f1; -fx-font-weight: bold;");
        totalCrLabel = new Label("Total Credit: 0.00");
        totalCrLabel.setStyle("-fx-text-fill: #a855f7; -fx-font-weight: bold;");
        diffLabel = new Label("Difference: 0.00");
        diffLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
        HBox totalsBox = new HBox(25, totalDrLabel, totalCrLabel, diffLabel);
        totalsBox.setAlignment(Pos.CENTER_LEFT);
        totalsBox.setPadding(new Insets(10));
        totalsBox.getStyleClass().add("card");

        importBtn = new Button("✔ Import Opening Balances");
        importBtn.getStyleClass().add("btn-success");
        importBtn.setDisable(true);
        importBtn.setOnAction(e -> doImport());

        HBox actionBox = new HBox(15, addLineBtn, importBtn);
        actionBox.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(15, headerFields,
                new Label("Opening Balance Lines"), scroll, totalsBox, actionBox);
        card.getStyleClass().add("card");

        this.getChildren().addAll(header, card);

        loadAccounts();
    }

    private void loadAccounts() {
        CompletableFuture.supplyAsync(() -> {
            try {
                return apiClient.getLedgerAccounts();
            } catch (Exception ex) {
                return new ArrayList<FASubGroup>();
            }
        }).thenAccept(list -> Platform.runLater(() -> {
            lineEditor.setAccounts(list);
            lineEditor.addLine();
            lineEditor.addLine();
        }));
    }

    private void recalculate() {
        LedgerLineEditor.Totals totals = lineEditor.totals();
        totalDrLabel.setText("Total Debit: " + UiUtils.money(totals.debit()));
        totalCrLabel.setText("Total Credit: " + UiUtils.money(totals.credit()));
        boolean balanced = totals.isBalanced();
        if (balanced) {
            diffLabel.setText("Balanced!");
            diffLabel.setStyle(UiConstants.STYLE_STATUS_SUCCESS);
        } else {
            diffLabel.setText("Difference: " + UiUtils.money(totals.difference()));
            diffLabel.setStyle(UiConstants.STYLE_STATUS_DANGER);
        }
        importBtn.setDisable(!balanced);
    }

    private void doImport() {
        List<LedgerLineEditor.LineData> editorLines = lineEditor.lines();
        for (LedgerLineEditor.LineData line : editorLines) {
            if (line.accountCode() == null) {
                UiUtils.showAlert(Alert.AlertType.WARNING, "Missing Account", "Incomplete Line",
                        "Select a ledger account for every line item.");
                return;
            }
            if (line.amount() == null || line.amount().compareTo(BigDecimal.ZERO) <= 0) {
                UiUtils.showAlert(Alert.AlertType.WARNING, "Invalid Amount", "Incomplete Line",
                        "Enter a positive amount for every line item.");
                return;
            }
        }

        List<Map<String, Object>> lines = new ArrayList<>();
        for (LedgerLineEditor.LineData line : editorLines) {
            Map<String, Object> lineMap = new HashMap<>();
            lineMap.put("accountCode", line.accountCode());
            lineMap.put("drCr", line.drCr());
            lineMap.put("amount", line.amount());
            lines.add(lineMap);
        }

        Map<String, Object> request = new HashMap<>();
        // Skip it if blank; the backend just uses today's date then.
        if (datePicker.getValue() != null) request.put("openingDate", datePicker.getValue().toString());
        String narration = narrInput.getText().trim();
        if (!narration.isEmpty()) request.put("narration", narration);
        request.put("lines", lines);

        importBtn.setDisable(true);
        AsyncUi.fetch(() -> apiClient.importOpeningBalances(request), voucher -> {
            UiUtils.showAlert(Alert.AlertType.INFORMATION, "Opening Balances Imported", "Success",
                    "Recorded as Opening Balance voucher \"" + voucher.getJId() + "\".");
            resetForm();
        }, ex -> {
            importBtn.setDisable(false);
            UiUtils.showAlert(Alert.AlertType.ERROR, "Import Failed",
                    "Failed to import opening balances", ex.getMessage());
        });
    }

    private void resetForm() {
        datePicker.setValue(LocalDate.now());
        narrInput.clear();
        lineEditor.clearLines();
        lineEditor.addLine();
        lineEditor.addLine();
        recalculate();
    }
}
