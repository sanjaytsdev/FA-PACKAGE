package com.spam.financialaccounting.desktop.view;

import com.spam.financialaccounting.desktop.api.ApiClient;
import com.spam.financialaccounting.desktop.model.FASubGroup;
import com.spam.financialaccounting.desktop.model.JournalDetail;
import com.spam.financialaccounting.desktop.model.JournalMaster;
import com.spam.financialaccounting.desktop.ui.UiUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class JournalEntriesView extends HBox {

    private final ApiClient apiClient;
    private TableView<JournalMaster> masterTable;
    private TableView<JournalDetail> detailTable;
    private Button newVoucherBtn;
    private Button deleteVoucherBtn;
    private JournalMaster selectedMaster = null;

    public JournalEntriesView(ApiClient apiClient) {
        this.apiClient = apiClient;
        this.getStyleClass().add("content-pane");
        this.setSpacing(20);

        // Left Pane: Journal Masters list
        VBox leftPane = new VBox(15);
        HBox.setHgrow(leftPane, Priority.ALWAYS);

        Label title = new Label("Journal Vouchers");
        title.getStyleClass().add("view-title");

        masterTable = new TableView<>();
        masterTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        masterTable.setPlaceholder(new Label("No Journal Vouchers found"));

        TableColumn<JournalMaster, String> colId = new TableColumn<>("Voucher ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("jId"));
        colId.setPrefWidth(100);

        TableColumn<JournalMaster, String> colDoc = new TableColumn<>("Doc");
        colDoc.setCellValueFactory(new PropertyValueFactory<>("jDoc"));
        colDoc.setPrefWidth(60);

        TableColumn<JournalMaster, LocalDateTime> colDate = new TableColumn<>("Date");
        colDate.setCellValueFactory(new PropertyValueFactory<>("jDate"));
        colDate.setPrefWidth(150);
        colDate.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                    setGraphic(null);
                }
            }
        });

        TableColumn<JournalMaster, BigDecimal> colAmt = new TableColumn<>("Total Amount");
        colAmt.setCellValueFactory(new PropertyValueFactory<>("jAmount"));
        colAmt.setPrefWidth(120);

        TableColumn<JournalMaster, String> colNarr = new TableColumn<>("Narration");
        colNarr.setCellValueFactory(new PropertyValueFactory<>("jNarr"));
        colNarr.setPrefWidth(180);

        masterTable.getColumns().addAll(colId, colDoc, colDate, colAmt, colNarr);

        HBox btnBox = new HBox(10);
        newVoucherBtn = new Button("📝 New Journal Voucher");
        newVoucherBtn.getStyleClass().add("btn-primary");
        newVoucherBtn.setOnAction(e -> openNewVoucherDialog());

        deleteVoucherBtn = new Button("❌ Delete Voucher");
        deleteVoucherBtn.getStyleClass().add("btn-danger");
        deleteVoucherBtn.setDisable(true);
        deleteVoucherBtn.setOnAction(e -> deleteSelectedVoucher());

        btnBox.getChildren().addAll(newVoucherBtn, deleteVoucherBtn);
        leftPane.getChildren().addAll(title, masterTable, btnBox);

        // Right Pane: Details breakdown
        VBox rightPane = new VBox(15);
        rightPane.getStyleClass().add("card");
        rightPane.setPrefWidth(400);

        Label detailsTitle = new Label("Journal Entry Line Details");
        detailsTitle.getStyleClass().add("card-title");

        detailTable = new TableView<>();
        detailTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        detailTable.setPlaceholder(new Label("Select a voucher to view lines"));

        TableColumn<JournalDetail, String> colCode = new TableColumn<>("Account");
        colCode.setCellValueFactory(new PropertyValueFactory<>("jCode"));
        colCode.setPrefWidth(120);

        TableColumn<JournalDetail, String> colSide = new TableColumn<>("DR/CR");
        colSide.setCellValueFactory(new PropertyValueFactory<>("jDrCr"));
        colSide.setPrefWidth(80);

        TableColumn<JournalDetail, BigDecimal> colDtlAmt = new TableColumn<>("Amount");
        colDtlAmt.setCellValueFactory(new PropertyValueFactory<>("jAmount"));
        colDtlAmt.setPrefWidth(150);

        detailTable.getColumns().addAll(colCode, colSide, colDtlAmt);

        rightPane.getChildren().addAll(detailsTitle, detailTable);
        this.getChildren().addAll(leftPane, rightPane);

        masterTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                selectedMaster = newSel;
                deleteVoucherBtn.setDisable(false);
                loadDetails(newSel.getJId());
            } else {
                selectedMaster = null;
                deleteVoucherBtn.setDisable(true);
                detailTable.getItems().clear();
            }
        });

        loadMasters();
    }

    private void loadMasters() {
        CompletableFuture.runAsync(() -> {
            try {
                List<JournalMaster> masters = apiClient.getJournalMasters();
                Platform.runLater(() -> masterTable.setItems(FXCollections.observableArrayList(masters)));
            } catch (Exception ex) {
                Platform.runLater(() -> UiUtils.showAlert(Alert.AlertType.ERROR, "Fetch Error",
                        "Failed to load vouchers", ex.getMessage()));
            }
        });
    }

    private void loadDetails(String jId) {
        CompletableFuture.runAsync(() -> {
            try {
                List<JournalDetail> details = apiClient.getJournalDetailsByJournalId(jId);
                Platform.runLater(() -> detailTable.setItems(FXCollections.observableArrayList(details)));
            } catch (Exception ex) {
                Platform.runLater(() -> UiUtils.showAlert(Alert.AlertType.ERROR, "Fetch Error",
                        "Failed to load transaction lines", ex.getMessage()));
            }
        });
    }

    private void deleteSelectedVoucher() {
        if (selectedMaster == null)
            return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete Voucher ID: " + selectedMaster.getJId());
        confirm.setContentText("This will delete the voucher header and all its sub-ledger line entries. Proceed?");
        UiUtils.applyStylesheet(confirm.getDialogPane());

        confirm.showAndWait().ifPresent(btnType -> {
            if (btnType == ButtonType.OK) {
                CompletableFuture.runAsync(() -> {
                    try {
                        // first delete children details
                        List<JournalDetail> details = apiClient.getJournalDetailsByJournalId(selectedMaster.getJId());
                        for (JournalDetail detail : details) {
                            apiClient.deleteJournalDetail(detail.getJId(), detail.getJCode(), detail.getJDrCr());
                        }
                        // Now delete master
                        apiClient.deleteJournalMaster(selectedMaster.getJId());
                        Platform.runLater(() -> {
                            UiUtils.showAlert(Alert.AlertType.INFORMATION, "Success", "Voucher Deleted",
                                    "Voucher and lines removed successfully.");
                            loadMasters();
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> UiUtils.showAlert(Alert.AlertType.ERROR, "Deletion Failed",
                                "API deletion pipeline failed", ex.getMessage()));
                    }
                });
            }
        });
    }

    // Modal view helper for creating double-entry transactions
    private void openNewVoucherDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Create Balanced Journal Voucher");

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.getStyleClass().add("root-layout");

        // Header Form fields
        GridPane headerGrid = new GridPane();
        headerGrid.setHgap(10);
        headerGrid.setVgap(10);

        TextField idInput = new TextField();
        idInput.setPromptText("JV12345678 (10 characters)");

        TextField docInput = new TextField("JV");
        docInput.setDisable(true); // default to JV

        DatePicker datePicker = new DatePicker(LocalDate.now());
        TextField narrInput = new TextField();

        narrInput.setPromptText("Voucher narrative");
        headerGrid.add(new Label("Voucher ID:"), 0, 0);
        headerGrid.add(idInput, 1, 0);
        headerGrid.add(new Label("Doc Type:"), 2, 0);
        headerGrid.add(docInput, 3, 0);
        headerGrid.add(new Label("Voucher Date:"), 0, 1);
        headerGrid.add(datePicker, 1, 1);
        headerGrid.add(new Label("Narration:"), 0, 2);
        headerGrid.add(narrInput, 1, 2, 3, 1);

        // Lines container
        VBox linesContainer = new VBox(10);
        Label linesTitle = new Label("Voucher Entry Lines (Double-Entry Ledger Grid)");
        linesTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        ScrollPane scroll = new ScrollPane(linesContainer);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(250);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: #1e293b;");

        // Double-entry validation bar
        HBox totalSummaryBox = new HBox(20);
        totalSummaryBox.setPadding(new Insets(10));
        totalSummaryBox.setAlignment(Pos.CENTER_LEFT);
        totalSummaryBox.getStyleClass().add("card");

        Label totalDrLabel = new Label("Total Debit: 0.00");
        totalDrLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
        Label totalCrLabel = new Label("Total Credit: 0.00");
        totalCrLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
        Label diffLabel = new Label("Net Difference: 0.00 (Balanced)");
        diffLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");

        totalSummaryBox.getChildren().addAll(totalDrLabel, totalCrLabel, diffLabel);

        Button saveVoucherBtn = new Button("Validation & Post Voucher");
        saveVoucherBtn.getStyleClass().add("btn-success");
        saveVoucherBtn.setDisable(true);

        List<VoucherLineRow> lineRows = new ArrayList<>();

        // Fetch accounts for combo-boxes
        CompletableFuture.supplyAsync(() -> {
            try {
                return apiClient.getLedgerAccounts();
            } catch (Exception ex) {
                return new ArrayList<FASubGroup>();
            }
        }).thenAccept(accounts -> Platform.runLater(() -> {
            // setup listener to recalculate totals dynamically
            Runnable recalculator = () -> {
                BigDecimal drSum = BigDecimal.ZERO;
                BigDecimal crSum = BigDecimal.ZERO;

                for (VoucherLineRow row : lineRows) {
                    BigDecimal rowAmt = row.getAmount();
                    String rowDrCr = row.getDrCr();
                    if (rowAmt != null) {
                        if ("DR".equals(rowDrCr)) {
                            drSum = drSum.add(rowAmt);
                        } else if ("CR".equals(rowDrCr)) {
                            crSum = crSum.add(rowAmt);
                        }
                    }
                }

                totalDrLabel.setText("Total Debit: " + drSum.toPlainString());
                totalCrLabel.setText("Total Credit: " + crSum.toPlainString());
                BigDecimal diff = drSum.subtract(crSum).abs();
                diffLabel.setText("Difference: " + diff.toPlainString());

                boolean isBalanced = drSum.compareTo(BigDecimal.ZERO) > 0 && drSum.compareTo(crSum) == 0;

                if (isBalanced) {
                    diffLabel.setText("Voucher Balanced!");
                    diffLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                    saveVoucherBtn.setDisable(false);
                } else {
                    diffLabel.setText("Difference: " + diff.toPlainString() + " (Unbalanced)");
                    diffLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                    saveVoucherBtn.setDisable(true);
                }
            };

            // Add initial 2 lines (standard journal voucher needs at least one debit and
            // credit)
            addLineRow(linesContainer, accounts, lineRows, recalculator);
            addLineRow(linesContainer, accounts, lineRows, recalculator);

            Button addLineBtn = new Button("➕ Add Debit/Credit Line");
            addLineBtn.getStyleClass().add("btn-secondary");
            addLineBtn.setOnAction(evt -> addLineRow(linesContainer, accounts, lineRows, recalculator));

            saveVoucherBtn.setOnAction(evt -> {
                // Post Master, then Post details
                String voucherId = idInput.getText().trim();
                String narration = narrInput.getText().trim();
                LocalDateTime voucherTime = datePicker.getValue().atStartOfDay();

                if (voucherId.length() != 10) {
                    UiUtils.showAlert(Alert.AlertType.WARNING, "Validation Error", "Invalid Voucher Id",
                            "Voucher ID must be exactly 10 characters.");
                    return;
                }

                // Fixed: Added critical validation to verify that every detail line is fully completed
                for (VoucherLineRow row : lineRows) {
                    if (row.getAccountCode() == null) {
                        UiUtils.showAlert(Alert.AlertType.WARNING, "Validation Error", "Incomplete Line Item", "Please select a valid ledger account for all entry lines.");
                        return;
                    }
                    BigDecimal amt = row.getAmount();
                    if (amt == null || amt.compareTo(BigDecimal.ZERO) <= 0) {
                        UiUtils.showAlert(Alert.AlertType.WARNING, "Validation Error", "Incomplete Line Item", "Please enter a valid positive decimal amount for all entry lines.");
                        return;
                    }
                }

                // compute total amount (sum of debits)
                BigDecimal totalAmount = BigDecimal.ZERO;
                for (VoucherLineRow row : lineRows) {
                    if ("DR".equals(row.getDrCr()) && row.getAmount() != null) {
                        totalAmount = totalAmount.add(row.getAmount());
                    }
                }

                JournalMaster master = new JournalMaster(voucherId, "JV", voucherTime, totalAmount, narration);

                CompletableFuture.runAsync(() -> {
                    try {
                        // 1. post master
                        apiClient.createJournalMaster(master);

                        // 2. post details
                        for (VoucherLineRow row : lineRows) {
                            JournalDetail detail = new JournalDetail(voucherId, row.getAccountCode(), row.getDrCr(),
                                    row.getAmount());
                            apiClient.createJournalDetail(detail);
                        }

                        Platform.runLater(() -> {
                            UiUtils.showAlert(Alert.AlertType.INFORMATION, "Voucher Posted", "Success",
                                    "Journal Voucher and lines created successfully.");
                            dialog.close();
                            loadMasters();
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> UiUtils.showAlert(Alert.AlertType.ERROR, "Posting Error",
                                "Failed to submit transaction", ex.getMessage()));
                    }
                });
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

    private void addLineRow(VBox container, List<FASubGroup> accounts, List<VoucherLineRow> rows, Runnable onRecalculate) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(5,0,5,0));

        ComboBox<FASubGroup> accSelect = new ComboBox<>(FXCollections.observableArrayList(accounts));
        accSelect.setPromptText("Select Ledger Account");
        accSelect.setPrefWidth(220);

        ComboBox<String> typeSelect = new ComboBox<>(FXCollections.observableArrayList("DR", "CR"));
        typeSelect.setValue("DR");
        typeSelect.setPrefWidth(80);

        TextField amountInput = new TextField();
        amountInput.setPromptText("Amount");
        amountInput.setPrefWidth(120);

        Button removeBtn = new Button("X");
        removeBtn.getStyleClass().add("btn-danger");

        VoucherLineRow rowData = new VoucherLineRow(accSelect,typeSelect,amountInput);
        rows.add(rowData);

        removeBtn.setOnAction(e-> {
            container.getChildren().remove(row);
            rows.remove(rowData);
            onRecalculate.run();
        });

       // Trigger dynamic calculation updates
accSelect.setOnAction(e->onRecalculate.run());
typeSelect.setOnAction(e->onRecalculate.run());
amountInput.textProperty().addListener((obs,o,n)->onRecalculate.run());

row.getChildren().addAll(accSelect,typeSelect,amountInput,removeBtn);
container.getChildren().add(row);
    }

    private static class VoucherLineRow {
        private final ComboBox<FASubGroup> accountSelector;
        private final ComboBox<String> drcrSelector;
        private final TextField amountField;

        public VoucherLineRow(ComboBox<FASubGroup> accountSelector, ComboBox<String> drcrSelector,
                TextField amountField) {
            this.accountSelector = accountSelector;
            this.drcrSelector = drcrSelector;
            this.amountField = amountField;
        }

        public String getAccountCode() {
            FASubGroup acc = accountSelector.getValue();
            return acc != null ? acc.getSCode() : null;
        }

        public String getDrCr() {
            return drcrSelector.getValue();
        }

        public BigDecimal getAmount() {
            try {
                return new BigDecimal(amountField.getText().trim());
            } catch (Exception e) {
                return null;
            }
        }
    }

}