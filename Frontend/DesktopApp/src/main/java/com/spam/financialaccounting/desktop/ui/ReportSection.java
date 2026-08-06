package com.spam.financialaccounting.desktop.ui;

import java.math.BigDecimal;

import com.spam.financialaccounting.desktop.model.ReportLineItem;

import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

/**
 * Factory for a titled report section: a card holding an account/description/amount
 * table with a bold total label beneath it.
 *
 * <p>Shared by the Balance Sheet and Profit &amp; Loss screens, which each build the
 * same three-column {@link ReportLineItem} table; only the column widths differ.
 */
public final class ReportSection {

    private ReportSection() {
    }

    /**
     * Build a report section card. The supplied {@code table} and {@code totalLabel}
     * are configured in place and returned inside a styled {@code card} container.
     */
    public static VBox build(String name, TableView<ReportLineItem> table, Label totalLabel,
            double codeWidth, double descWidth, double amountWidth) {
        Label sectionTitle = new Label(name);
        sectionTitle.getStyleClass().add("card-title");

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No accounts"));
        table.setPrefHeight(180);

        TableColumn<ReportLineItem, String> colCode = new TableColumn<>("Account");
        colCode.setCellValueFactory(new PropertyValueFactory<>("accountCode"));
        colCode.setPrefWidth(codeWidth);
        TableColumn<ReportLineItem, String> colDesc = new TableColumn<>("Description");
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        colDesc.setPrefWidth(descWidth);
        TableColumn<ReportLineItem, BigDecimal> colAmt = UiUtils.moneyColumn("Amount", "amount");
        colAmt.setPrefWidth(amountWidth);
        table.getColumns().addAll(colCode, colDesc, colAmt);

        totalLabel.setStyle("-fx-font-weight: bold;");

        VBox section = new VBox(10, sectionTitle, table, totalLabel);
        section.getStyleClass().add("card");
        return section;
    }
}
