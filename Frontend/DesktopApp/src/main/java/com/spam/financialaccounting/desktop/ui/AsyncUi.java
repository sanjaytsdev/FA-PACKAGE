package com.spam.financialaccounting.desktop.ui;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.scene.control.Alert;

/**
 * Helpers for running blocking API calls off the JavaFX Application Thread and
 * delivering the result (or failure) back on it.
 *
 * <p>They replace the repeated
 * {@code CompletableFuture.runAsync(() -> { try { ...; Platform.runLater(...) }
 * catch (Exception e) { Platform.runLater(...) } })} boilerplate that appeared in
 * every view. Both the success and error callbacks run on the FX thread.
 */
public final class AsyncUi {

    /** A supplier whose body may throw a checked exception (e.g. an API call). */
    @FunctionalInterface
    public interface IoSupplier<T> {
        T get() throws Exception;
    }

    /** An action whose body may throw a checked exception (e.g. a delete call). */
    @FunctionalInterface
    public interface IoRunnable {
        void run() throws Exception;
    }

    private AsyncUi() {
    }

    /**
     * Run {@code task} on a background thread; on success invoke {@code onSuccess}
     * with the result on the FX thread, otherwise invoke {@code onError} on the FX
     * thread. Use this overload when a failure needs custom UI handling.
     */
    public static <T> void fetch(IoSupplier<T> task, Consumer<T> onSuccess, Consumer<Exception> onError) {
        CompletableFuture.runAsync(() -> {
            try {
                T result = task.get();
                Platform.runLater(() -> onSuccess.accept(result));
            } catch (Exception ex) {
                Platform.runLater(() -> onError.accept(ex));
            }
        });
    }

    /**
     * Run {@code task} on a background thread; on success invoke {@code onSuccess},
     * otherwise show an error alert with the given title/header and the exception
     * message.
     */
    public static <T> void fetch(IoSupplier<T> task, Consumer<T> onSuccess, String errorTitle, String errorHeader) {
        fetch(task, onSuccess, alertHandler(errorTitle, errorHeader));
    }

    /**
     * Run a result-less {@code task} on a background thread; on success invoke
     * {@code onSuccess} on the FX thread, otherwise invoke {@code onError}.
     */
    public static void run(IoRunnable task, Runnable onSuccess, Consumer<Exception> onError) {
        fetch(() -> {
            task.run();
            return null;
        }, ignored -> onSuccess.run(), onError);
    }

    /**
     * Run a result-less {@code task} on a background thread; on success invoke
     * {@code onSuccess}, otherwise show an error alert.
     */
    public static void run(IoRunnable task, Runnable onSuccess, String errorTitle, String errorHeader) {
        run(task, onSuccess, alertHandler(errorTitle, errorHeader));
    }

    private static Consumer<Exception> alertHandler(String title, String header) {
        return ex -> UiUtils.showAlert(Alert.AlertType.ERROR, title, header, ex.getMessage());
    }
}
