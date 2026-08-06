package com.spam.financialaccounting.desktop.ui;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.spam.financialaccounting.desktop.model.FASubGroup;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Reusable editable grid of double-entry ledger lines (account / DR-CR / amount).
 *
 * <p>Shared by the journal-voucher dialog and the opening-balances screen, which
 * previously each carried a private line-row class plus an {@code addLineRow} method
 * and DR/CR summation. Cosmetic differences (column widths, row padding, and the
 * minimum number of rows the user may keep) are captured by {@link Style}.
 */
public class LedgerLineEditor extends VBox {

    /** A single edited line's current values ({@code accountCode}/{@code amount} may be null). */
    public record LineData(String accountCode, String drCr, BigDecimal amount) {
    }

    /** Running debit/credit totals across all lines. */
    public record Totals(BigDecimal debit, BigDecimal credit) {
        /** Absolute difference between debit and credit totals. */
        public BigDecimal difference() {
            return debit.subtract(credit).abs();
        }

        /** True when there is a positive debit total that exactly equals the credit total. */
        public boolean isBalanced() {
            return debit.compareTo(BigDecimal.ZERO) > 0 && debit.compareTo(credit) == 0;
        }
    }

    /** Per-screen look/behavior: minimum retained rows and control widths/padding. */
    public record Style(int minRows, double accountWidth, double amountWidth, Insets rowPadding) {
    }

    /** Journal-voucher preset: rows may be removed freely, wider-padded rows. */
    public static Style voucherStyle() {
        return new Style(0, 220, 120, new Insets(5, 0, 5, 0));
    }

    /** Opening-balances preset: keep at least two rows, no row padding. */
    public static Style openingStyle() {
        return new Style(2, 260, 140, Insets.EMPTY);
    }

    private final Style style;
    private final List<Row> rows = new ArrayList<>();
    private List<FASubGroup> accounts = new ArrayList<>();
    private Runnable onChange = () -> {
    };

    public LedgerLineEditor(Style style) {
        this.style = style;
        setSpacing(10);
    }

    /** Accounts offered in each line's dropdown; applied to rows added afterwards. */
    public void setAccounts(List<FASubGroup> accounts) {
        this.accounts = accounts;
    }

    /** Callback fired whenever a line's account, side, amount, or count changes. */
    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    /** Append a blank line. Does not fire {@link #setOnChange} (an empty line adds nothing). */
    public void addLine() {
        Row row = new Row();
        rows.add(row);
        getChildren().add(row.container);
    }

    /** Remove every line (e.g. before re-seeding a form). */
    public void clearLines() {
        rows.clear();
        getChildren().clear();
    }

    /** Snapshot of every line's current values. */
    public List<LineData> lines() {
        List<LineData> result = new ArrayList<>();
        for (Row row : rows) {
            result.add(row.data());
        }
        return result;
    }

    /** Sum of debit and credit amounts across all lines. */
    public Totals totals() {
        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;
        for (Row row : rows) {
            LineData data = row.data();
            if (data.amount() != null) {
                if ("DR".equals(data.drCr())) {
                    debit = debit.add(data.amount());
                } else if ("CR".equals(data.drCr())) {
                    credit = credit.add(data.amount());
                }
            }
        }
        return new Totals(debit, credit);
    }

    private void removeRow(Row row) {
        if (rows.size() <= style.minRows()) {
            return;
        }
        rows.remove(row);
        getChildren().remove(row.container);
        onChange.run();
    }

    private final class Row {
        private final HBox container = new HBox(10);
        private final ComboBox<FASubGroup> account = new ComboBox<>(FXCollections.observableArrayList(accounts));
        private final ComboBox<String> drcr = new ComboBox<>(FXCollections.observableArrayList("DR", "CR"));
        private final TextField amount = new TextField();

        private Row() {
            container.setAlignment(Pos.CENTER_LEFT);
            container.setPadding(style.rowPadding());

            account.setPromptText("Select Ledger Account");
            account.setPrefWidth(style.accountWidth());

            drcr.setValue("DR");
            drcr.setPrefWidth(80);

            amount.setPromptText("Amount");
            amount.setPrefWidth(style.amountWidth());

            Button removeBtn = new Button("X");
            removeBtn.getStyleClass().add("btn-danger");
            removeBtn.setOnAction(e -> removeRow(this));

            // Default DR/CR to the account's normal side, then recalculate.
            account.setOnAction(e -> {
                FASubGroup chosen = account.getValue();
                if (chosen != null) {
                    drcr.setValue(chosen.getSDrCr());
                }
                onChange.run();
            });
            drcr.setOnAction(e -> onChange.run());
            amount.textProperty().addListener((obs, o, n) -> onChange.run());

            container.getChildren().addAll(account, drcr, amount, removeBtn);
        }

        private LineData data() {
            FASubGroup acc = account.getValue();
            String code = acc != null ? acc.getSCode() : null;
            BigDecimal amt;
            try {
                amt = new BigDecimal(amount.getText().trim());
            } catch (Exception e) {
                amt = null;
            }
            return new LineData(code, drcr.getValue(), amt);
        }
    }
}
