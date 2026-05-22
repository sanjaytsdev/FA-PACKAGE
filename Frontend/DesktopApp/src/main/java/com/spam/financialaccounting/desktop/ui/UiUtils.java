package com.spam.financialaccounting.desktop.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;

public class UiUtils {

    public static void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        DialogPane dialogPane = alert.getDialogPane();
        String css = UiUtils.class.getResource("/styles.css").toExternalForm();
        dialogPane.getStylesheets().add(css);
        dialogPane.getStyleClass().add("my-dialog");

        alert.showAndWait();
    }

}
