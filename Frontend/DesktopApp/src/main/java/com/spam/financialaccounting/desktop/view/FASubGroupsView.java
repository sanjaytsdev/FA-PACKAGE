package com.spam.financialaccounting.desktop.view;

import com.spam.financialaccounting.desktop.api.ApiClient;
import com.spam.financialaccounting.desktop.model.FAGroup;
import com.spam.financialaccounting.desktop.model.FASubGroup;
import com.spam.financialaccounting.desktop.ui.UiUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class FASubGroupsView extends HBox {

    private final ApiClient apiClient;
    private TableView<FASubGroup> table;
    private TextField codeInput;
    private TextField descInput;
    private ComboBox<FAGroup> parentSelect;
    private TextField typeInput;
    private TextField balanceInput;
    private ComboBox<String> drcrSelect;
    private ComboBox<String> statusSelect;

    private Button saveBtn;
    private Button deleteBtn;
    private Button clearBtn;
    private FASubGroup selectedSubGroup = null;

    public FASubGroupsView(ApiClient apiClient) {
        this.apiClient = apiClient;
        this.getStyleClass().add("content-pane");
        this.setSpacing(20);

        VBox leftPane = new VBox();
        leftPane.setSpacing(15);
        HBox.setHgrow(leftPane, Priority.ALWAYS);

        Label title = new Label("Ledger Accounts");
        title.getStyleClass().add("view-title");

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No ledger accounts loaded"));

        TableColumn<FASubGroup, String> colCode = new TableColumn<>("Ledger Code");
        colCode.setCellValueFactory(new PropertyValueFactory<>("sCode"));
        colCode.setPrefWidth(90);

        TableColumn<FASubGroup, String> colDesc = new TableColumn<>("Description");
        colDesc.setCellValueFactory(new PropertyValueFactory<>("sDesc"));
        colDesc.setPrefWidth(180);

        TableColumn<FASubGroup, String> colParent = new TableColumn<>("Group");
        colParent.setCellValueFactory(new PropertyValueFactory<>("aCode"));
        colParent.setPrefWidth(60);

        TableColumn<FASubGroup, String> colType = new TableColumn<>("Sub-Type");
        colType.setCellValueFactory(new PropertyValueFactory<>("sType"));
        colType.setPrefWidth(80);

        TableColumn<FASubGroup, BigDecimal> colBal = new TableColumn<>("Opening Bal");
        colBal.setCellValueFactory(new PropertyValueFactory<>("sOpbal"));
        colBal.setPrefWidth(100);

        TableColumn<FASubGroup, String> colSide = new TableColumn<>("DR/CR");
        colSide.setCellValueFactory(new PropertyValueFactory<>("sDrCr"));
        colSide.setPrefWidth(60);

        TableColumn<FASubGroup, String> colFlag = new TableColumn<>("Status");
        colFlag.setCellValueFactory(new PropertyValueFactory<>("sFlag"));
        colFlag.setPrefWidth(60);

        table.getColumns().addAll(colCode, colDesc, colParent, colType, colBal, colSide, colFlag);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                selectedSubGroup = newSel;
                codeInput.setText(newSel.getSCode());
                codeInput.setDisable(true);
                descInput.setText(newSel.getSDesc());

                // pick the parent group whose code matches
                for (FAGroup g : parentSelect.getItems()) {
                    if (g.getAccountCode().equals(newSel.getACode())) {
                        parentSelect.setValue(g);
                        break;
                    }
                }

                typeInput.setText(newSel.getSType());
                balanceInput.setText(newSel.getSOpbal() != null ? newSel.getSOpbal().toPlainString() : "0.00");
                drcrSelect.setValue(newSel.getSDrCr());
                statusSelect.setValue("T".equalsIgnoreCase(newSel.getSFlag()) ? "Active" : "Inactive");
                saveBtn.setText("Update Account");
                deleteBtn.setDisable(false);
            }
        });

        leftPane.getChildren().addAll(title, table);

        // The editor form
        VBox rightPane = new VBox();
        rightPane.getStyleClass().add("card");
        rightPane.setSpacing(10);
        rightPane.setPrefWidth(350);

        Label formTitle = new Label("Manage Ledger Account");
        formTitle.getStyleClass().add("card-title");

        codeInput = new TextField();
        codeInput.setPromptText("5 chars (e.g. 10001)");

        descInput = new TextField();
        descInput.setPromptText("Account Title");

        parentSelect = new ComboBox<>();
        parentSelect.setPromptText("Select Parent Account Group");
        parentSelect.setOnAction(e -> {
            FAGroup selected = parentSelect.getValue();
            // only auto-fill sub-type and side when adding a new account, not editing
            if (selected != null && selectedSubGroup == null) {
                String type = selected.getAccountType();
                typeInput.setText(type + "0");
                drcrSelect.setValue("0".equals(type) || "4".equals(type) ? "DR" : "CR");
            }
        });

        typeInput = new TextField();
        typeInput.setPromptText("2 digit type (e.g. 00-49)");

        balanceInput = new TextField("0.00");
        drcrSelect = new ComboBox<>(FXCollections.observableArrayList("DR", "CR"));
        statusSelect = new ComboBox<>(FXCollections.observableArrayList("Active", "Inactive"));
        statusSelect.setValue("Active");

        saveBtn = new Button("Create Account");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.setOnAction(e -> saveSubGroup());

        deleteBtn = new Button("Delete Account");
        deleteBtn.getStyleClass().add("btn-danger");
        deleteBtn.setMaxWidth(Double.MAX_VALUE);
        deleteBtn.setDisable(true);
        deleteBtn.setOnAction(e -> deleteSubGroup());

        clearBtn = new Button("Clear Form");
        clearBtn.getStyleClass().add("btn-secondary");
        clearBtn.setMaxWidth(Double.MAX_VALUE);
        clearBtn.setOnAction(e -> clearForm());

        VBox form = new VBox(8);
        form.getChildren().addAll(
                new Label("Ledger Account Code:"), codeInput,
                new Label("Account Description:"), descInput,
                new Label("Parent Account Group:"), parentSelect,
                new Label("Sub-Type Code (e.g. 00):"), typeInput,
                new Label("Opening Balance:"), balanceInput,
                new Label("Normal Balance Side (DR/CR):"), drcrSelect,
                new Label("Status Flag:"), statusSelect);

        rightPane.getChildren().addAll(formTitle, form, saveBtn, deleteBtn, clearBtn);

        ScrollPane scrollPane = new ScrollPane(rightPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefWidth(370);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");

        this.getChildren().addAll(leftPane, scrollPane);
        loadInitialData();
    }

    private void loadInitialData() {
        CompletableFuture.runAsync(() -> {
            try {
                List<FAGroup> groups = apiClient.getGroups();
                List<FASubGroup> subGroups = apiClient.getLedgerAccounts();
                Platform.runLater(() -> {
                    parentSelect.setItems(FXCollections.observableArrayList(groups));
                    table.setItems(FXCollections.observableArrayList(subGroups));
                });
            } catch (Exception ex) {
                Platform.runLater(() -> UiUtils.showAlert(Alert.AlertType.ERROR, "Fetch Error",
                        "Failed to retrieve initial data", ex.getMessage()));
            }
        });
    }

    private void saveSubGroup() {
        String code = codeInput.getText().trim();
        String desc = descInput.getText().trim();
        FAGroup parent = parentSelect.getValue();
        String type = typeInput.getText().trim();
        String balStr = balanceInput.getText().trim();
        String drCr = drcrSelect.getValue();
        String status = "Active".equals(statusSelect.getValue()) ? "T" : "F";

        if (code.length() != 5) {
            UiUtils.showAlert(Alert.AlertType.WARNING, "Validation Error", "Invalid Ledger Code",
                    "Ledger Account code must be exactly 5 characters.");
            return;
        }

        if (desc.isEmpty()) {
            UiUtils.showAlert(Alert.AlertType.WARNING, "Validation Error", "Missing Description",
                    "Please enter description.");
            return;
        }

        if (parent == null) {
            UiUtils.showAlert(Alert.AlertType.WARNING, "Validation Error", "Missing Parent",
                    "Please select a parent account group.");
            return;
        }
        if (type.length() != 2) {
            UiUtils.showAlert(Alert.AlertType.WARNING, "Validation Error", "Invalid Sub-Type",
                    "Sub-type must be exactly 2-digit account code (00-49).");
            return;
        }
        if (drCr == null) {
            UiUtils.showAlert(Alert.AlertType.WARNING, "Validation Error", "Missing DR/CR",
                    "Please select debit or credit normal balance side.");
            return;
        }

        BigDecimal bal;
        try {
            bal = new BigDecimal(balStr);
        } catch (NumberFormatException e) {
            UiUtils.showAlert(Alert.AlertType.WARNING, "Validation Error", "Invalid Opening Balance",
                    "Please enter a valid numeric opening balance.");
            return;
        }

        FASubGroup sg = new FASubGroup(code, desc, parent.getAccountCode(), type, bal, drCr, status);

        CompletableFuture.runAsync(() -> {
            try {
                if (selectedSubGroup == null) {
                    apiClient.createLedgerAccount(sg);
                    Platform.runLater(() -> UiUtils.showAlert(Alert.AlertType.INFORMATION, "Success", "Ledger Created",
                            "Ledger account created successfully."));
                } else {
                    apiClient.updateLedgerAccount(code, sg);
                    Platform.runLater(() -> UiUtils.showAlert(Alert.AlertType.INFORMATION, "Success", "Ledger Updated",
                            "Ledger account details updated successfully."));
                }
                Platform.runLater(() -> {
                    clearForm();
                    loadInitialData();
                });
            } catch (Exception ex) {
                Platform.runLater(
                        () -> UiUtils.showAlert(Alert.AlertType.ERROR, "Save Failed", "API Error", ex.getMessage()));
            }
        });
    }

    private void deleteSubGroup() {
        if (selectedSubGroup == null)
            return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete Ledger Account: " + selectedSubGroup.getSDesc());
        confirm.setContentText("Are you sure you want to permanently delete this account? This cannot be undone.");

        UiUtils.applyStylesheet(confirm.getDialogPane());

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                CompletableFuture.runAsync(() -> {
                    try {
                        apiClient.deleteLedgerAccount(selectedSubGroup.getSCode());
                        Platform.runLater(() -> {
                            UiUtils.showAlert(Alert.AlertType.INFORMATION, "Success", "Deleted",
                                    "Ledger Account successfully removed.");
                            clearForm();
                            loadInitialData();
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> UiUtils.showAlert(Alert.AlertType.ERROR, "Delete Failed",
                                "API Core Error", ex.getMessage()));
                    }
                });
            }
        });
    }

    private void clearForm() {
        selectedSubGroup = null;
        codeInput.setDisable(false);
        codeInput.clear();
        descInput.clear();
        parentSelect.setValue(null);
        typeInput.clear();
        balanceInput.setText("0.00");
        drcrSelect.setValue(null);
        statusSelect.setValue("Active");
        saveBtn.setText("Create Account");
        deleteBtn.setDisable(true);
    }

}