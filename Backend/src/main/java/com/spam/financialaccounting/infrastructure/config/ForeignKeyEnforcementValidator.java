package com.spam.financialaccounting.infrastructure.config;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Fails startup fast if SQLite isn't enforcing foreign keys.
 *
 * SQLite turns foreign keys off per connection by default, so the FK
 * constraints in schema.sql do nothing unless every connection runs
 * {@code PRAGMA foreign_keys = ON} (set via
 * {@code spring.datasource.hikari.connection-init-sql}). This grabs a pooled
 * connection after startup, checks the pragma actually stuck, and aborts boot
 * if it didn't.
 *
 * Does nothing for non-SQLite datasources — MySQL/H2 enforce FKs on their own
 * and don't know the SQLite pragma.
 */
@Component
public class ForeignKeyEnforcementValidator {

    private static final Logger log = LoggerFactory.getLogger(ForeignKeyEnforcementValidator.class);

    private final DataSource dataSource;
    private final String datasourceUrl;

    public ForeignKeyEnforcementValidator(DataSource dataSource,
            @Value("${spring.datasource.url:}") String datasourceUrl) {
        this.dataSource = dataSource;
        this.datasourceUrl = datasourceUrl;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void verifyForeignKeysEnabled() {
        if (datasourceUrl == null || !datasourceUrl.startsWith("jdbc:sqlite")) {
            // Only SQLite needs the per-connection pragma; other engines enforce FKs on their own.
            return;
        }

        Integer enabled = new JdbcTemplate(dataSource)
                .queryForObject("PRAGMA foreign_keys", Integer.class);

        if (enabled == null || enabled != 1) {
            throw new IllegalStateException(
                    "SQLite foreign key enforcement is OFF (PRAGMA foreign_keys=" + enabled + "). "
                            + "Referential integrity is NOT guaranteed. Ensure "
                            + "'spring.datasource.hikari.connection-init-sql=PRAGMA foreign_keys=ON' is configured.");
        }

        log.info("SQLite foreign key enforcement is ACTIVE (PRAGMA foreign_keys=1).");
    }
}
