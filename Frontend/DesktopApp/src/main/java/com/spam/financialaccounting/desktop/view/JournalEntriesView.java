package com.spam.financialaccounting.desktop.view;

import com.spam.financialaccounting.desktop.api.ApiClient;
import com.spam.financialaccounting.desktop.model.JournalDetail;
import com.spam.financialaccounting.desktop.model.JournalMaster;
import com.spam.financialaccounting.desktop.ui.AsyncUi;
import com.spam.financialaccounting.desktop.ui.UiUtils;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class JournalEntriesView extends HBox {

    private final ApiClient apiClient;
    private TableView<JournalMaster> masterTable;
    private TableView<JournalDetail> detailTable;
    private Button newVoucherBtn;
    private Button reverseVoucherBtn;
    private Button deleteVoucherBtn;
    private JournalMaster selectedMaster = null;

    public JournalEntriesView(ApiClient apiClient) {
        this.apiClient = apiClient;
        this.getStyleClass().add("content-pane");
        this.setSpacing(20);

        // Left pane: list of journal vouchers
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

        reverseVoucherBtn = new Button("↩ Reverse Voucher");
        reverseVoucherBtn.getStyleClass().add("btn-secondary");
        reverseVoucherBtn.setDisable(true);
        reverseVoucherBtn.setOnAction(e -> reverseSelectedVoucher());

        deleteVoucherBtn = new Button("❌ Delete Voucher");
        deleteVoucherBtn.getStyleClass().add("btn-danger");
        deleteVoucherBtn.setDisable(true);
        deleteVoucherBtn.setOnAction(e -> deleteSelectedVoucher());

        btnBox.getChildren().addAll(newVoucherBtn, reverseVoucherBtn, deleteVoucherBtn);
        leftPane.getChildren().addAll(title, masterTable, btnBox);

        // Right pane: the line details
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
                reverseVoucherBtn.setDisable(false);
                loadDetails(newSel.getJId());
            } else {
                selectedMaster = null;
                deleteVoucherBtn.setDisable(true);
                reverseVoucherBtn.setDisable(true);
                detailTable.getItems().clear();
            }
        });

        loadMasters();
    }

    private void loadMasters() {
        AsyncUi.fetch(apiClient::getJournalMasters,
                masters -> masterTable.setItems(FXCollections.observableArrayList(masters)),
                "Fetch Error", "Failed to load vouchers");
    }

    private void loadDetails(String jId) {
        AsyncUi.fetch(() -> apiClient.getJournalDetailsByJournalId(jId),
                details -> detailTable.setItems(FXCollections.observableArrayList(details)),
                "Fetch Error", "Failed to load transaction lines");
    }

    private void deleteSelectedVoucher() {
        if (selectedMaster == null)
            return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete Voucher ID: " + selectedMaster.getJId());
        confirm.setContentText("This will delete the voucher header and all its sub-ledger line entries. Proceed?");
        UiUtils.applyStylesheet(confirm.getDialogPane());

        String jId = selectedMaster.getJId();
        confirm.showAndWait().ifPresent(btnType -> {
            if (btnType == ButtonType.OK) {
                // Server drops the header and all its lines in one atomic call.
                AsyncUi.run(() -> apiClient.deleteJournalVoucher(jId), () -> {
                    UiUtils.showAlert(Alert.AlertType.INFORMATION, "Success", "Voucher Deleted",
                            "Voucher and lines removed successfully.");
                    detailTable.getItems().clear();
                    loadMasters();
                }, "Deletion Failed", "Failed to delete journal voucher");
            }
        });
    }

    private void reverseSelectedVoucher() {
        if (selectedMaster == null)
            return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Reversal");
        confirm.setHeaderText("Reverse Voucher ID: " + selectedMaster.getJId());
        confirm.setContentText("This posts a new balanced voucher with the opposite DR/CR of each line. Proceed?");
        UiUtils.applyStylesheet(confirm.getDialogPane());

        String jId = selectedMaster.getJId();
        confirm.showAndWait().ifPresent(btnType -> {
            if (btnType == ButtonType.OK) {
                AsyncUi.fetch(() -> apiClient.reverseJournalVoucher(jId), reversal -> {
                    UiUtils.showAlert(Alert.AlertType.INFORMATION, "Voucher Reversed", "Success",
                            "Reversing voucher \"" + reversal.getJId() + "\" posted.");
                    loadMasters();
                }, "Reversal Failed", "Failed to reverse journal voucher");
            }
        });
    }

    // Pops the modal for entering a new double-entry voucher
    private void openNewVoucherDialog() {
        new NewVoucherDialog(apiClient, this::loadMasters).show();
    }

}