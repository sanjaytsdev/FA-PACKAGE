package com.spam.financialaccounting.desktop.view;

import com.spam.financialaccounting.desktop.api.ApiClient;
import com.spam.financialaccounting.desktop.model.FASubGroup;
import com.spam.financialaccounting.desktop.model.JournalMaster;
import com.spam.financialaccounting.desktop.ui.UiUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
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
    private final VBox linesContainer;
    private final Label totalDrLabel;
    private final Label totalCrLabel;
    private final Label diffLabel;
    private final Button importBtn;
    private final List<LineRow> lineRows = new ArrayList<>();
    private List<FASubGroup> accounts = new ArrayList<>();

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

        linesContainer = new VBox(10);
        ScrollPane scroll = new ScrollPane(linesContainer);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(260);

        Button addLineBtn = new Button("➕ Add Line");
        addLineBtn.getStyleClass().add("btn-secondary");
        addLineBtn.setOnAction(e -> addLineRow());

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
            this.accounts = list;
            addLineRow();
            addLineRow();
        }));
    }

    private void recalculate() {
        BigDecimal drSum = BigDecimal.ZERO;
        BigDecimal crSum = BigDecimal.ZERO;
        for (LineRow row : lineRows) {
            BigDecimal amt = row.getAmount();
            if (amt != null) {
                if ("DR".equals(row.getDrCr())) drSum = drSum.add(amt);
                else if ("CR".equals(row.getDrCr())) crSum = crSum.add(amt);
            }
        }
        totalDrLabel.setText("Total Debit: " + UiUtils.money(drSum));
        totalCrLabel.setText("Total Credit: " + UiUtils.money(crSum));
        BigDecimal diff = drSum.subtract(crSum).abs();
        boolean balanced = drSum.compareTo(BigDecimal.ZERO) > 0 && drSum.compareTo(crSum) == 0;
        if (balanced) {
            diffLabel.setText("Balanced!");
            diffLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
        } else {
            diffLabel.setText("Difference: " + UiUtils.money(diff));
            diffLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
        }
        importBtn.setDisable(!balanced);
    }

    private void addLineRow() {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        ComboBox<FASubGroup> accSelect = new ComboBox<>(FXCollections.observableArrayList(accounts));
        accSelect.setPromptText("Select Ledger Account");
        accSelect.setPrefWidth(260);
        ComboBox<String> typeSelect = new ComboBox<>(FXCollections.observableArrayList("DR", "CR"));
        typeSelect.setValue("DR");
        typeSelect.setPrefWidth(80);
        TextField amountInput = new TextField();
        amountInput.setPromptText("Amount");
        amountInput.setPrefWidth(140);
        Button removeBtn = new Button("X");
        removeBtn.getStyleClass().add("btn-danger");

        LineRow rowData = new LineRow(accSelect, typeSelect, amountInput);
        lineRows.add(rowData);

        removeBtn.setOnAction(e -> {
            if (lineRows.size() <= 2) return;
            linesContainer.getChildren().remove(row);
            lineRows.remove(rowData);
            recalculate();
        });
        accSelect.setOnAction(e -> {
            FASubGroup chosen = accSelect.getValue();
            if (chosen != null) typeSelect.setValue(chosen.getSDrCr());
            recalculate();
        });
        typeSelect.setOnAction(e -> recalculate());
        amountInput.textProperty().addListener((obs, o, n) -> recalculate());

        row.getChildren().addAll(accSelect, typeSelect, amountInput, removeBtn);
        linesContainer.getChildren().add(row);
    }

    private void doImport() {
        for (LineRow row : lineRows) {
            if (row.getAccountCode() == null) {
                UiUtils.showAlert(Alert.AlertType.WARNING, "Missing Account", "Incomplete Line",
                        "Select a ledger account for every line item.");
                return;
            }
            BigDecimal amt = row.getAmount();
            if (amt == null || amt.compareTo(BigDecimal.ZERO) <= 0) {
                UiUtils.showAlert(Alert.AlertType.WARNING, "Invalid Amount", "Incomplete Line",
                        "Enter a positive amount for every line item.");
                return;
            }
        }

        List<Map<String, Object>> lines = new ArrayList<>();
        for (LineRow row : lineRows) {
            Map<String, Object> line = new HashMap<>();
            line.put("accountCode", row.getAccountCode());
            line.put("drCr", row.getDrCr());
            line.put("amount", row.getAmount());
            lines.add(line);
        }

        Map<String, Object> request = new HashMap<>();
        // Skip it if blank; the backend just uses today's date then.
        if (datePicker.getValue() != null) request.put("openingDate", datePicker.getValue().toString());
        String narration = narrInput.getText().trim();
        if (!narration.isEmpty()) request.put("narration", narration);
        request.put("lines", lines);

        importBtn.setDisable(true);
        CompletableFuture.runAsync(() -> {
            try {
                JournalMaster voucher = apiClient.importOpeningBalances(request);
                Platform.runLater(() -> {
                    UiUtils.showAlert(Alert.AlertType.INFORMATION, "Opening Balances Imported", "Success",
                            "Recorded as Opening Balance voucher \"" + voucher.getJId() + "\".");
                    resetForm();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    importBtn.setDisable(false);
                    UiUtils.showAlert(Alert.AlertType.ERROR, "Import Failed",
                            "Failed to import opening balances", ex.getMessage());
                });
            }
        });
    }

    private void resetForm() {
        datePicker.setValue(LocalDate.now());
        narrInput.clear();
        linesContainer.getChildren().clear();
        lineRows.clear();
        addLineRow();
        addLineRow();
        recalculate();
    }

    private static class LineRow {
        private final ComboBox<FASubGroup> accountSelector;
        private final ComboBox<String> drcrSelector;
        private final TextField amountField;

        LineRow(ComboBox<FASubGroup> accountSelector, ComboBox<String> drcrSelector, TextField amountField) {
            this.accountSelector = accountSelector;
            this.drcrSelector = drcrSelector;
            this.amountField = amountField;
        }

        String getAccountCode() {
            FASubGroup acc = accountSelector.getValue();
            return acc != null ? acc.getSCode() : null;
        }

        String getDrCr() {
            return drcrSelector.getValue();
        }

        BigDecimal getAmount() {
            try {
                return new BigDecimal(amountField.getText().trim());
            } catch (Exception e) {
                return null;
            }
        }
    }
}
