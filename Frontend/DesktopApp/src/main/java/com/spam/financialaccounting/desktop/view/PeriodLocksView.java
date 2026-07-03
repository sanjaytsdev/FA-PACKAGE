package com.spam.financialaccounting.desktop.view;

import com.spam.financialaccounting.desktop.api.ApiClient;
import com.spam.financialaccounting.desktop.model.PeriodLock;
import com.spam.financialaccounting.desktop.ui.UiUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class PeriodLocksView extends VBox {

    private final ApiClient apiClient;
    private final DatePicker startPicker;
    private final DatePicker endPicker;
    private final TableView<PeriodLock> table;

    public PeriodLocksView(ApiClient apiClient) {
        this.apiClient = apiClient;
        this.getStyleClass().add("content-pane");
        this.setSpacing(20);

        Label title = new Label("Period Locks");
        title.getStyleClass().add("view-title");
        Label subtitle = new Label(
                "Close accounting periods. While a period is locked, no voucher can be posted or reversed into any date it covers.");
        subtitle.getStyleClass().add("view-subtitle");
        subtitle.setWrapText(true);
        VBox header = new VBox(5, title, subtitle);

        // Create form
        startPicker = new DatePicker();
        startPicker.setPromptText("Period Start");
        endPicker = new DatePicker();
        endPicker.setPromptText("Period End");
        Button lockBtn = new Button("🔒 Lock Period");
        lockBtn.getStyleClass().add("btn-primary");
        lockBtn.setOnAction(e -> createLock());

        HBox form = new HBox(15,
                new VBox(5, new Label("Period Start:"), startPicker),
                new VBox(5, new Label("Period End:"), endPicker),
                lockBtn);
        form.setAlignment(Pos.BOTTOM_LEFT);
        form.getStyleClass().add("card");

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No locked periods. All dates are currently open for posting."));
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<PeriodLock, String> colStart = new TableColumn<>("Period Start");
        colStart.setCellValueFactory(new PropertyValueFactory<>("periodStart"));
        TableColumn<PeriodLock, String> colEnd = new TableColumn<>("Period End");
        colEnd.setCellValueFactory(new PropertyValueFactory<>("periodEnd"));
        TableColumn<PeriodLock, String> colAt = new TableColumn<>("Locked At");
        colAt.setCellValueFactory(new PropertyValueFactory<>("lockedAt"));
        TableColumn<PeriodLock, String> colBy = new TableColumn<>("Locked By");
        colBy.setCellValueFactory(new PropertyValueFactory<>("lockedBy"));
        TableColumn<PeriodLock, Void> colAction = new TableColumn<>("Action");
        colAction.setCellFactory(c -> new TableCell<>() {
            private final Button unlockBtn = new Button("Unlock");
            {
                unlockBtn.getStyleClass().add("btn-danger");
                unlockBtn.setOnAction(e -> deleteLock(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : unlockBtn);
            }
        });
        table.getColumns().addAll(colStart, colEnd, colAt, colBy, colAction);

        this.getChildren().addAll(header, form, table);
        load();
    }

    private void load() {
        CompletableFuture.runAsync(() -> {
            try {
                List<PeriodLock> locks = apiClient.getPeriodLocks();
                Platform.runLater(() -> table.setItems(FXCollections.observableArrayList(locks)));
            } catch (Exception ex) {
                Platform.runLater(() -> UiUtils.showAlert(Alert.AlertType.ERROR, "Fetch Failure",
                        "Failed to load period locks", ex.getMessage()));
            }
        });
    }

    private void createLock() {
        LocalDate start = startPicker.getValue();
        LocalDate end = endPicker.getValue();
        if (start == null || end == null) {
            UiUtils.showAlert(Alert.AlertType.WARNING, "Missing Dates", "Invalid Range",
                    "Both period start and end dates are required.");
            return;
        }
        if (start.isAfter(end)) {
            UiUtils.showAlert(Alert.AlertType.WARNING, "Invalid Range", "Invalid Range",
                    "Period start must be on or before period end.");
            return;
        }

        Map<String, Object> request = new HashMap<>();
        request.put("periodStart", start.toString());
        request.put("periodEnd", end.toString());

        CompletableFuture.runAsync(() -> {
            try {
                apiClient.createPeriodLock(request);
                Platform.runLater(() -> {
                    UiUtils.showAlert(Alert.AlertType.INFORMATION, "Period Locked", "Success",
                            "Period " + start + " → " + end + " is now closed.");
                    startPicker.setValue(null);
                    endPicker.setValue(null);
                    load();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> UiUtils.showAlert(Alert.AlertType.ERROR, "Lock Failed",
                        "Failed to create period lock", ex.getMessage()));
            }
        });
    }

    private void deleteLock(PeriodLock lock) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Unlock");
        confirm.setHeaderText("Unlock period " + lock.getPeriodStart() + " → " + lock.getPeriodEnd() + "?");
        confirm.setContentText("Reopening the period allows vouchers to be posted into its dates again.");
        UiUtils.applyStylesheet(confirm.getDialogPane());

        confirm.showAndWait().ifPresent(btnType -> {
            if (btnType == ButtonType.OK) {
                CompletableFuture.runAsync(() -> {
                    try {
                        apiClient.deletePeriodLock(lock.getId());
                        Platform.runLater(() -> {
                            UiUtils.showAlert(Alert.AlertType.INFORMATION, "Period Unlocked", "Success",
                                    "Period " + lock.getPeriodStart() + " → " + lock.getPeriodEnd() + " reopened.");
                            load();
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> UiUtils.showAlert(Alert.AlertType.ERROR, "Unlock Failed",
                                "Failed to delete period lock", ex.getMessage()));
                    }
                });
            }
        });
    }
}
