package com.spam.financialaccounting.desktop.view;

import com.spam.financialaccounting.desktop.api.ApiClient;
import com.spam.financialaccounting.desktop.config.UiConstants;
import com.spam.financialaccounting.desktop.model.FAGroup;
import com.spam.financialaccounting.desktop.model.FASubGroup;
import com.spam.financialaccounting.desktop.model.JournalMaster;
import com.spam.financialaccounting.desktop.ui.AsyncUi;

import javafx.scene.control.Label;
import javafx.scene.layout.*;
import java.math.BigDecimal;
import java.util.List;

public class DashboardView extends VBox {

    private final ApiClient apiClient;
    private Label totalGroupsVal;
    private Label totalLedgersVal;
    private Label totalJournalsVal;
    private Label tbStatusVal;

    public DashboardView(ApiClient apiClient) {
        this.apiClient = apiClient;
        this.getStyleClass().add("content-pane");
        this.setSpacing(20);

        Label title = new Label("Dashboard Overview");
        title.getStyleClass().add("view-title");

        Label subtitle = new Label("Real-time snapshot of double-entry ledger database stats.");
        subtitle.getStyleClass().add("view-subtitle");

        VBox header = new VBox(title, subtitle);
        header.setSpacing(5);

        // Stats grid
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);

        VBox cardGroups = createStatCard("Total Account Groups", totalGroupsVal = new Label("..."));
        VBox cardLedgers = createStatCard("Total Ledger Accounts", totalLedgersVal = new Label("..."));
        VBox cardJournals = createStatCard("Total Journal Entries", totalJournalsVal = new Label("..."));
        VBox cardTb = createStatCard("Trial Balance Check", tbStatusVal = new Label("Checking..."));

        grid.add(cardGroups, 0, 0);
        grid.add(cardLedgers, 1, 0);
        grid.add(cardJournals, 0, 1);
        grid.add(cardTb, 1, 1);

        // split the width evenly between the two columns
        ColumnConstraints cc = new ColumnConstraints();
        cc.setPercentWidth(50);
        grid.getColumnConstraints().addAll(cc, cc);

        this.getChildren().addAll(header, grid);

        loadDashboardData();
    }

    private VBox createStatCard(String title, Label valLabel) {
        VBox card = new VBox();
        card.getStyleClass().add("card");
        card.setSpacing(8);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-title");

        valLabel.getStyleClass().add("card-value");

        card.getChildren().addAll(titleLabel, valLabel);
        return card;
    }

    private record DashboardStats(int groups, int ledgers, int journals, BigDecimal tbBalance) {
    }

    private void loadDashboardData() {
        AsyncUi.fetch(() -> {
            List<FAGroup> groups = apiClient.getGroups();
            List<FASubGroup> ledgers = apiClient.getLedgerAccounts();
            List<JournalMaster> journals = apiClient.getJournalMasters();

            // Check the books balance: in double-entry, total debits should equal total credits.
            // Each ledger has an opening balance and a side, so sum them up and see if it nets to zero.
            BigDecimal totalBalance = BigDecimal.ZERO;
            for (FASubGroup ledger : ledgers) {
                BigDecimal balance = ledger.getSOpbal() != null ? ledger.getSOpbal() : BigDecimal.ZERO;

                if ("CR".equalsIgnoreCase(ledger.getSDrCr())) {
                    totalBalance = totalBalance.subtract(balance);
                } else {
                    totalBalance = totalBalance.add(balance);
                }
            }

            return new DashboardStats(groups.size(), ledgers.size(), journals.size(), totalBalance);
        }, stats -> {
            totalGroupsVal.setText(String.valueOf(stats.groups()));
            totalLedgersVal.setText(String.valueOf(stats.ledgers()));
            totalJournalsVal.setText(String.valueOf(stats.journals()));

            if (stats.tbBalance().compareTo(BigDecimal.ZERO) == 0) {
                tbStatusVal.setText("BALANCED (0.00)");
                tbStatusVal.setStyle("-fx-text-fill: " + UiConstants.COLOR_SUCCESS + ";");
            } else {
                tbStatusVal.setText("MISMATCH (" + stats.tbBalance().toPlainString() + ")");
                tbStatusVal.setStyle("-fx-text-fill: " + UiConstants.COLOR_DANGER + ";");
            }
        }, ex -> {
            totalGroupsVal.setText("Error");
            totalLedgersVal.setText("Error");
            totalJournalsVal.setText("Error");
            tbStatusVal.setText("Unknown");
            tbStatusVal.setStyle("-fx-text-fill: " + UiConstants.COLOR_DANGER + ";");
        });
    }

}