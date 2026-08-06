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
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Modal dialog for entering and posting a balanced double-entry journal voucher.
 *
 * <p>Extracted from {@link JournalEntriesView} so that view keeps only the master/
 * detail list concern. The dialog validates the header, collects lines from a
 * {@link LedgerLineEditor}, and posts the voucher; {@code onPosted} is run after a
 * successful post so the caller can refresh its list.
 */
public class NewVoucherDialog {

    private final ApiClient apiClient;
    private final Runnable onPosted;

    public NewVoucherDialog(ApiClient apiClient, Runnable onPosted) {
        this.apiClient = apiClient;
        this.onPosted = onPosted;
    }

    /** Build and show the modal dialog, blocking until it is dismissed. */
    public void show() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Create Balanced Journal Voucher");

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.getStyleClass().add("root-layout");

        // Header fields
        GridPane headerGrid = new GridPane();
        headerGrid.setHgap(10);
        headerGrid.setVgap(10);

        DatePicker datePicker = new DatePicker(LocalDate.now());
        TextField narrInput = new TextField();

        narrInput.setPromptText("Voucher narrative");
        narrInput.setPrefWidth(560);
        narrInput.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(narrInput, Priority.ALWAYS);
        headerGrid.add(new Label("Voucher Date:"), 0, 0);
        headerGrid.add(datePicker, 1, 0);
        headerGrid.add(new Label("Narration:"), 0, 1);
        headerGrid.add(narrInput, 1, 1, 3, 1);

        // Holds the entry lines
        LedgerLineEditor editor = new LedgerLineEditor(LedgerLineEditor.voucherStyle());
        Label linesTitle = new Label("Voucher Entry Lines (Double-Entry Ledger Grid)");
        linesTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        ScrollPane scroll = new ScrollPane(editor);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(250);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: #1e293b;");

        // Running totals / balance check bar
        HBox totalSummaryBox = new HBox(20);
        totalSummaryBox.setPadding(new Insets(10));
        totalSummaryBox.setAlignment(Pos.CENTER_LEFT);
        totalSummaryBox.getStyleClass().add("card");

        Label totalDrLabel = new Label("Total Debit: 0.00");
        totalDrLabel.setStyle(UiConstants.STYLE_STATUS_SUCCESS);
        Label totalCrLabel = new Label("Total Credit: 0.00");
        totalCrLabel.setStyle(UiConstants.STYLE_STATUS_SUCCESS);
        Label diffLabel = new Label("Net Difference: 0.00 (Balanced)");
        diffLabel.setStyle(UiConstants.STYLE_STATUS_SUCCESS);

        totalSummaryBox.getChildren().addAll(totalDrLabel, totalCrLabel, diffLabel);

        Button saveVoucherBtn = new Button("Validation & Post Voucher");
        saveVoucherBtn.getStyleClass().add("btn-success");
        saveVoucherBtn.setDisable(true);

        // Load the accounts that fill the combo-boxes
        CompletableFuture.supplyAsync(() -> {
            try {
                return apiClient.getLedgerAccounts();
            } catch (Exception ex) {
                return new ArrayList<FASubGroup>();
            }
        }).thenAccept(accounts -> Platform.runLater(() -> {
            // recomputes the totals whenever a line changes
            Runnable recalculator = () -> {
                LedgerLineEditor.Totals totals = editor.totals();
                totalDrLabel.setText("Total Debit: " + totals.debit().toPlainString());
                totalCrLabel.setText("Total Credit: " + totals.credit().toPlainString());

                if (totals.isBalanced()) {
                    diffLabel.setText("Voucher Balanced!");
                    diffLabel.setStyle(UiConstants.STYLE_STATUS_SUCCESS);
                    saveVoucherBtn.setDisable(false);
                } else {
                    diffLabel.setText("Difference: " + totals.difference().toPlainString() + " (Unbalanced)");
                    diffLabel.setStyle(UiConstants.STYLE_STATUS_DANGER);
                    saveVoucherBtn.setDisable(true);
                }
            };
            editor.setOnChange(recalculator);
            editor.setAccounts(accounts);

            // Start with two lines, since a voucher needs at least one debit and one credit
            editor.addLine();
            editor.addLine();

            Button addLineBtn = new Button("➕ Add Debit/Credit Line");
            addLineBtn.getStyleClass().add("btn-secondary");
            addLineBtn.setOnAction(evt -> editor.addLine());

            saveVoucherBtn.setOnAction(evt -> {
                String narration = narrInput.getText().trim();

                if (datePicker.getValue() == null) {
                    UiUtils.showAlert(Alert.AlertType.WARNING, "Validation Error", "Missing Date",
                            "Please select a voucher date.");
                    return;
                }
                if (narration.length() < 5 || narration.length() > 100) {
                    UiUtils.showAlert(Alert.AlertType.WARNING, "Validation Error", "Invalid Narration",
                            "Narration is required and must be between 5 and 100 characters.");
                    return;
                }

                LocalDateTime voucherTime = datePicker.getValue().atStartOfDay();

                // Make sure every line is filled in
                List<LedgerLineEditor.LineData> lines = editor.lines();
                for (LedgerLineEditor.LineData line : lines) {
                    if (line.accountCode() == null) {
                        UiUtils.showAlert(Alert.AlertType.WARNING, "Validation Error", "Incomplete Line Item", "Please select a valid ledger account for all entry lines.");
                        return;
                    }
                    if (line.amount() == null || line.amount().compareTo(BigDecimal.ZERO) <= 0) {
                        UiUtils.showAlert(Alert.AlertType.WARNING, "Validation Error", "Incomplete Line Item", "Please enter a valid positive decimal amount for all entry lines.");
                        return;
                    }
                }

                // Build the request. The backend re-checks the debit/credit balance,
                // works out the total, and generates the ID.
                List<Map<String, Object>> requestLines = new ArrayList<>();
                for (LedgerLineEditor.LineData line : lines) {
                    Map<String, Object> lineMap = new HashMap<>();
                    lineMap.put("jCode", line.accountCode());
                    lineMap.put("jDrCr", line.drCr());
                    lineMap.put("jAmount", line.amount());
                    requestLines.add(lineMap);
                }
                Map<String, Object> request = new HashMap<>();
                request.put("jDoc", "JV");
                request.put("jDate", voucherTime);
                request.put("jNarr", narration);
                request.put("lines", requestLines);

                // One call posts the whole voucher; the server validates it.
                AsyncUi.fetch(() -> apiClient.postJournalVoucher(request), created -> {
                    UiUtils.showAlert(Alert.AlertType.INFORMATION, "Voucher Posted", "Success",
                            "Journal Voucher posted successfully.\nGenerated ID: " + created.getJId());
                    dialog.close();
                    onPosted.run();
                }, "Posting Error", "Failed to submit transaction");
            });

            HBox actionBox = new HBox(15, addLineBtn, saveVoucherBtn);
            actionBox.setAlignment(Pos.CENTER_RIGHT);
            layout.getChildren().addAll(headerGrid, linesTitle, scroll, totalSummaryBox, actionBox);
        }));

        Scene scene = new Scene(layout, 750, 600);
        UiUtils.applyStylesheet(scene);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
