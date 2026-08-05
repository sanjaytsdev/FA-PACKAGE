package com.spam.financialaccounting.desktop.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Application configuration resolved once at startup.
 *
 * <p>The REST base URL is read from a {@code config.properties} file in the working
 * directory. When the file is absent it is created with the default URL so the value
 * can be edited without recompiling.
 */
public final class AppConfig {

    private static final Logger LOG = Logger.getLogger(AppConfig.class.getName());

    /** Property key for the backend REST base URL. */
    private static final String BASE_URL_KEY = "api.base.url";

    /** Fallback base URL used when no configuration file overrides it. */
    private static final String DEFAULT_BASE_URL = "http://localhost:8080/api/v1";

    private static final String CONFIG_FILE = "config.properties";

    private static final String BASE_URL = resolveBaseUrl();

    private AppConfig() {
    }

    /** REST base URL (e.g. {@code http://localhost:8080/api/v1}). */
    public static String getBaseUrl() {
        return BASE_URL;
    }

    private static String resolveBaseUrl() {
        Properties props = new Properties();
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (FileInputStream in = new FileInputStream(configFile)) {
                props.load(in);
                String url = props.getProperty(BASE_URL_KEY);
                if (url != null && !url.trim().isEmpty()) {
                    return url.trim();
                }
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Error loading " + CONFIG_FILE, ex);
            }
        } else {
            try (FileOutputStream out = new FileOutputStream(configFile)) {
                props.setProperty(BASE_URL_KEY, DEFAULT_BASE_URL);
                props.store(out, "FA-PACKAGE Desktop Client Settings");
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Error creating default " + CONFIG_FILE, ex);
            }
        }
        return DEFAULT_BASE_URL;
    }
}
