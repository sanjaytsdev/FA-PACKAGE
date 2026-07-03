package com.spam.financialaccounting.infrastructure.persistence.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.spam.financialaccounting.domain.entity.PeriodLock;

/**
 * Stores closed accounting periods. Dates are kept as ISO-8601 strings
 * (yyyy-MM-dd), same as the J_DATE TEXT convention, so comparing them as
 * strings gives the same order as comparing them as dates.
 */
@Repository
public class PeriodLockRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<PeriodLock> rowMapper = (rs, rowNum) -> new PeriodLock(
            rs.getLong("ID"),
            LocalDate.parse(rs.getString("PERIOD_START")),
            LocalDate.parse(rs.getString("PERIOD_END")),
            LocalDateTime.parse(rs.getString("LOCKED_AT")),
            rs.getString("LOCKED_BY"));

    public PeriodLockRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** True if any lock covers {@code date} (PERIOD_START <= date <= PERIOD_END). */
    public boolean isLocked(LocalDate date) {
        String iso = date.toString();
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM period_lock WHERE PERIOD_START <= ? AND PERIOD_END >= ?",
                Integer.class, iso, iso);
        return count != null && count > 0;
    }

    public PeriodLock save(PeriodLock lock) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement(
                    "INSERT INTO period_lock (PERIOD_START, PERIOD_END, LOCKED_AT, LOCKED_BY) VALUES (?, ?, ?, ?)",
                    new String[] { "ID" });
            ps.setString(1, lock.getPeriodStart().toString());
            ps.setString(2, lock.getPeriodEnd().toString());
            ps.setString(3, lock.getLockedAt().toString());
            ps.setString(4, lock.getLockedBy());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            lock.setId(key.longValue());
        }
        return lock;
    }

    public List<PeriodLock> findAll() {
        return jdbcTemplate.query("SELECT * FROM period_lock ORDER BY PERIOD_START", rowMapper);
    }

    public boolean delete(Long id) {
        return jdbcTemplate.update("DELETE FROM period_lock WHERE ID = ?", id) > 0;
    }
}
