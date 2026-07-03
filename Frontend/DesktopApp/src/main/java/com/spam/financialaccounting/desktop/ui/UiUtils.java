package com.spam.financialaccounting.desktop.ui;

import java.math.BigDecimal;

import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

public class UiUtils {

    /** Format money with thousands separators and two decimals (e.g. 1,250.00). */
    public static String money(BigDecimal value) {
        if (value == null) value = BigDecimal.ZERO;
        return String.format("%,.2f", value);
    }

    /** Right-aligned, money-formatted table column bound to a BigDecimal property. */
    public static <S> TableColumn<S, BigDecimal> moneyColumn(String title, String property) {
        TableColumn<S, BigDecimal> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(money(item));
                    setStyle("-fx-alignment: CENTER-RIGHT;");
                }
            }
        });
        return col;
    }

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

