package com.spam.financialaccounting.desktop.view;

import com.spam.financialaccounting.desktop.api.ApiClient;
import com.spam.financialaccounting.desktop.model.FAGroup;
import com.spam.financialaccounting.desktop.ui.AsyncUi;
import com.spam.financialaccounting.desktop.ui.UiUtils;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import java.math.BigDecimal;

public class FAGroupsView extends HBox {

    private final ApiClient apiClient;
    private TableView<FAGroup> table;
    private TextField codeInput;
    private TextField descInput;
    private ComboBox<String> typeSelect;
    private TextField balanceInput;
    private Button saveBtn;
    private Button clearBtn;
    private FAGroup selectedGroup = null;

    public FAGroupsView(ApiClient apiClient) {
        this.apiClient = apiClient;
        this.getStyleClass().add("content-pane");
        this.setSpacing(20);

        // Left side: header plus the list
        VBox leftPane = new VBox();
        leftPane.setSpacing(15);
        HBox.setHgrow(leftPane, Priority.ALWAYS);

        Label title = new Label("Account Groups");
        title.getStyleClass().add("view-title");

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No account groups loaded"));

        TableColumn<FAGroup, String> colCode = new TableColumn<>("Group Code");
        colCode.setCellValueFactory(new PropertyValueFactory<>("accountCode"));
        colCode.setPrefWidth(100);

        TableColumn<FAGroup, String> colDesc = new TableColumn<>("Description");
        colDesc.setCellValueFactory(new PropertyValueFactory<>("accountDescription"));
        colDesc.setPrefWidth(220);

        TableColumn<FAGroup, String> colType = new TableColumn<>("Type Code");
        colType.setCellValueFactory(new PropertyValueFactory<>("accountType"));
        colType.setPrefWidth(100);

        TableColumn<FAGroup, BigDecimal> colBal = new TableColumn<>("Aggregate Balance");
        colBal.setCellValueFactory(new PropertyValueFactory<>("accountCurrentBalance"));
        colBal.setPrefWidth(150);

        table.getColumns().addAll(colCode, colDesc, colType, colBal);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                selectedGroup = newSel;
                codeInput.setText(newSel.getAccountCode());
                codeInput.setDisable(true);
                descInput.setText(newSel.getAccountDescription());
                typeSelect.setValue(newSel.getAccountType());
                balanceInput.setText(
                        newSel.getAccountCurrentBalance() != null ? newSel.getAccountCurrentBalance().toPlainString()
                                : "0.00");
                saveBtn.setText("Update Group");
            }
        });

        leftPane.getChildren().addAll(title, table);

        // Right side: the create/edit form
        VBox rightPane = new VBox();
        rightPane.getStyleClass().add("card");
        rightPane.setSpacing(15);
        rightPane.setPrefWidth(350);

        Label formTitle = new Label("Manage Account Group");
        formTitle.getStyleClass().add("card-title");

        codeInput = new TextField();
        codeInput.setPromptText("e.g. 01 (Exactly 2 Chars)");

        descInput = new TextField();
        descInput.setPromptText("e.g. Assets");

        typeSelect = new ComboBox<>(FXCollections.observableArrayList("0", "1", "2", "3", "4"));
        typeSelect.setPromptText("Select Group Class");
        typeSelect.setConverter(new javafx.util.StringConverter<String>() {
            @Override
            public String toString(String item) {
                if (item == null) return null;
                return switch (item) {
                    case "0" -> "0 - Asset";
                    case "1" -> "1 - Liability";
                    case "2" -> "2 - Equity";
                    case "3" -> "3 - Income";
                    case "4" -> "4 - Expense";
                    default -> item;
                };
            }

            @Override
            public String fromString(String string) {
                if (string == null || string.trim().isEmpty()) return null;
                if (string.startsWith("0")) return "0";
                if (string.startsWith("1")) return "1";
                if (string.startsWith("2")) return "2";
                if (string.startsWith("3")) return "3";
                if (string.startsWith("4")) return "4";
                return string;
            }
        });
        // Custom button cell so the prompt text reappears after the value is
        // reset to null (JavaFX does not restore promptText once a value has
        // been set, which blanks the dropdown title after a submission).
        typeSelect.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(typeSelect.getPromptText());
                } else {
                    setText(typeSelect.getConverter().toString(item));
                }
            }
        });

        balanceInput = new TextField("0.00");

        saveBtn = new Button("Create Group");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.setOnAction(e -> saveGroup());

        clearBtn = new Button("Clear Form");
        clearBtn.getStyleClass().add("btn-secondary");
        clearBtn.setMaxWidth(Double.MAX_VALUE);
        clearBtn.setOnAction(e -> clearForm());

        VBox form = new VBox();
        form.setSpacing(10);
        form.getChildren().addAll(
                new Label("Account Group Code:"), codeInput,
                new Label("Description:"), descInput,
                new Label("Type (0=Asset, 1=Liab, 2=Eq, 3=Inc, 4=Exp):"), typeSelect,
                new Label("Aggregated Balance:"), balanceInput);
        rightPane.getChildren().addAll(formTitle, form, saveBtn, clearBtn);
        this.getChildren().addAll(leftPane, rightPane);
        loadGroups();
    }

    private void loadGroups() {
        AsyncUi.fetch(apiClient::getGroups,
                groups -> table.setItems(FXCollections.observableArrayList(groups)),
                "Fetch Error", "Failed to load Groups");
    }

    private void saveGroup() {
        String code = codeInput.getText().trim();
        String desc = descInput.getText().trim();
        String type = typeSelect.getValue();
        String balStr = balanceInput.getText().trim();

        if (code.length() != 2) {
            UiUtils.showAlert(Alert.AlertType.WARNING, "Validation Error", "Invalid Code",
                    "Group code must be exactly 2 characters");
            return;
        }
        if (desc.isEmpty()) {
            UiUtils.showAlert(Alert.AlertType.WARNING, "Validation Error", "Missing Description",
                    "Please enter a description.");
            return;
        }
        if (type == null) {
            UiUtils.showAlert(Alert.AlertType.WARNING, "Validation Error", "Missing Type",
                    "Please choose an account group type.");
            return;
        }
        BigDecimal bal;
        try {
            bal = new BigDecimal(balStr);
        } catch (NumberFormatException e) {
            UiUtils.showAlert(Alert.AlertType.WARNING, "Validation Error", "Invalid Balance",
                    "Please enter a valid numeric decimal for balance.");
            return;
        }

        FAGroup group = new FAGroup(code, desc, type, bal);
        boolean isNew = selectedGroup == null;

        AsyncUi.run(() -> {
            if (isNew) {
                apiClient.createGroup(group);
            } else {
                apiClient.updateGroup(code, group);
            }
        }, () -> {
            UiUtils.showAlert(Alert.AlertType.INFORMATION, "Success",
                    isNew ? "Group Created" : "Group Updated",
                    isNew ? "Account Group was successfully added." : "Account Group details were updated.");
            clearForm();
            loadGroups();
        }, "Save Failed", "API Submission Error");
    }

    private void clearForm() {
        selectedGroup = null;
        codeInput.setDisable(false);
        codeInput.clear();
        descInput.clear();
        typeSelect.setValue(null);
        balanceInput.setText("0.00");
        saveBtn.setText("Create Group");
    }
}