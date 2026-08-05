package com.spam.financialaccounting.desktop.config;

/**
 * Shared UI constants (semantic colors and reusable inline-style fragments).
 *
 * <p>These centralize the hex values that were previously repeated inline across the
 * view classes so status styling lives in one place.
 */
public final class UiConstants {

    /** Positive / balanced / connected state (emerald). */
    public static final String COLOR_SUCCESS = "#10b981";

    /** Error / out-of-balance / disconnected state (red). */
    public static final String COLOR_DANGER = "#ef4444";

    /** Neutral / loading state (slate). */
    public static final String COLOR_MUTED = "#94a3b8";

    /** Bold status label in the success color. */
    public static final String STYLE_STATUS_SUCCESS = "-fx-text-fill: " + COLOR_SUCCESS + "; -fx-font-weight: bold;";

    /** Bold status label in the danger color. */
    public static final String STYLE_STATUS_DANGER = "-fx-text-fill: " + COLOR_DANGER + "; -fx-font-weight: bold;";

    /** Bold status label in the muted/loading color. */
    public static final String STYLE_STATUS_MUTED = "-fx-text-fill: " + COLOR_MUTED + "; -fx-font-weight: bold;";

    private UiConstants() {
    }
}
