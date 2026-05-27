package com.spam.financialaccounting.desktop.ui;

import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;

public class UiUtils {

    public static void applyStylesheet(Scene scene) {
        if (scene == null) return;
        var resource = UiUtils.class.getResource("/styles.css");
        if (resource != null) {
            scene.getStylesheets().add(resource.toExternalForm());
        } else {
            System.err.println("Warning: styles.css resource not found on classpath.");
        }
    }

    public static void applyStylesheet(DialogPane dialogPane) {
        if (dialogPane == null) return;
        var resource = UiUtils.class.getResource("/styles.css");
        if (resource != null) {
            dialogPane.getStylesheets().add(resource.toExternalForm());
        } else {
            System.err.println("Warning: styles.css resource not found on classpath.");
        }
    }

    public static void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        DialogPane dialogPane = alert.getDialogPane();
        applyStylesheet(dialogPane);
        dialogPane.getStyleClass().add("my-dialog");

        alert.showAndWait();
    }

}

