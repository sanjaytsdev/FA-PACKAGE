package com.spam.financialaccounting.desktop;

import com.spam.financialaccounting.desktop.api.ApiClient;

import com.spam.financialaccounting.desktop.ui.UiUtils;
import com.spam.financialaccounting.desktop.view.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * JavaFX application shell: builds the sidebar/top-bar chrome and swaps the center
 * content pane between the feature views on navigation. Owns the single shared
 * {@link ApiClient} that every view receives.
 */
public class DesktopApp extends Application {

    private BorderPane rootLayout;
    private ApiClient apiClient;
    private Label statusLabel;
    private Label activeIndicator;
    private Button activeNavBtn;

    @Override
    public void start(Stage primaryStage) {
        apiClient = new ApiClient();

        // Main layout container
        rootLayout = new BorderPane();
        rootLayout.getStyleClass().add("root-layout");

        // Build the UI pieces
        VBox sidebar = createSidebar();
        HBox topBar = createTopBar();

        rootLayout.setLeft(sidebar);
        rootLayout.setTop(topBar);

        // Show the dashboard by default
        showDashboard();

        // Set up the scene
        Scene scene = new Scene(rootLayout, 1100, 700);
        UiUtils.applyStylesheet(scene);

        primaryStage.setTitle("FA-PACKAGE | Desktop Financial Accounting");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Ping the server once on startup
        checkConnection();
    }

    private HBox createTopBar() {
        HBox topBar = new HBox();

        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER_RIGHT);
        topBar.setPadding(new Insets(10, 20, 10, 20));
        topBar.setSpacing(15);

        Label title = new Label("FINANCIAL ACCOUNTING SYSTEM");
        title.getStyleClass().add("topbar-title");
        HBox statusBox = new HBox();
        statusBox.setAlignment(Pos.CENTER_LEFT);
        statusBox.setSpacing(8);
        Label dot = new Label("●");
        dot.getStyleClass().add("status-dot");

        statusLabel = new Label("Checking Connection...");
        statusLabel.getStyleClass().add("status-text");
        statusBox.getChildren().addAll(dot, statusLabel);

        Button refreshBtn = new Button("🔄 Refresh Link");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> checkConnection());
        HBox leftSpacer = new HBox(title);
        leftSpacer.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(leftSpacer, javafx.scene.layout.Priority.ALWAYS);
        topBar.getChildren().addAll(leftSpacer, statusBox, refreshBtn);

        return topBar;
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(190);
        sidebar.setPadding(new Insets(20, 15, 20, 15));

        Label brandLabel = new Label("FA-PACKAGE");
        brandLabel.getStyleClass().add("brand-label");
        VBox.setMargin(brandLabel, new Insets(0, 0, 20, 0));

        Button dashBtn = createNavButton("📊 Dashboard", () -> showDashboard());
        Button groupBtn = createNavButton("📂 Account Groups", () -> showGroups());
        Button ledgerBtn = createNavButton("💳 Ledger Accounts", () -> showLedgers());
        Button openingBtn = createNavButton("💰 Opening Balances", () -> showOpeningBalances());
        Button journalBtn = createNavButton("📝 Journal Vouchers", () -> showJournals());
        Button trialBtn = createNavButton("⚖ Trial Balance", () -> showTrialBalance());
        Button plBtn = createNavButton("📈 Profit & Loss", () -> showProfitAndLoss());
        Button bsBtn = createNavButton("📖 Balance Sheet", () -> showBalanceSheet());
        Button lockBtn = createNavButton("🔒 Period Locks", () -> showPeriodLocks());

        sidebar.getChildren().addAll(brandLabel, dashBtn, groupBtn, ledgerBtn, openingBtn,
                journalBtn, trialBtn, plBtn, bsBtn, lockBtn);
        setActiveNav(dashBtn);
        return sidebar;
    }

    private Button createNavButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.getStyleClass().add("sidebar-item");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setOnAction(e -> {
            setActiveNav(btn);
            action.run();
        });
        return btn;
    }

    private void setActiveNav(Button btn) {
        if (activeNavBtn != null) {
            activeNavBtn.getStyleClass().remove("active");
        }
        activeNavBtn = btn;
        activeNavBtn.getStyleClass().add("active");
    }

    private void checkConnection() {
        statusLabel.setText("Connecting...");
        apiClient.pingAsync()
                .thenAccept(connected -> Platform.runLater(() -> {
                    if (connected) {
                        statusLabel.setText("Connected");
                        statusLabel.setStyle("-fx-text-fill: #10b981;");
                    } else {
                        statusLabel.setText("Disconnected");
                        statusLabel.setStyle("-fx-text-fill: #ef4444;");
                    }
                }));
    }

    private void showDashboard() {
        rootLayout.setCenter(new DashboardView(apiClient));
    }

    private void showGroups() {
        rootLayout.setCenter(new FAGroupsView(apiClient));
    }

    private void showLedgers() {
        rootLayout.setCenter(new FASubGroupsView(apiClient));
    }

    private void showOpeningBalances() {
        rootLayout.setCenter(new OpeningBalancesView(apiClient));
    }

    private void showJournals() {
        rootLayout.setCenter(new JournalEntriesView(apiClient));
    }

    private void showTrialBalance() {
        rootLayout.setCenter(new TrialBalanceView(apiClient));
    }

    private void showProfitAndLoss() {
        rootLayout.setCenter(new ProfitAndLossView(apiClient));
    }

    private void showBalanceSheet() {
        rootLayout.setCenter(new BalanceSheetView(apiClient));
    }

    private void showPeriodLocks() {
        rootLayout.setCenter(new PeriodLocksView(apiClient));
    }

}