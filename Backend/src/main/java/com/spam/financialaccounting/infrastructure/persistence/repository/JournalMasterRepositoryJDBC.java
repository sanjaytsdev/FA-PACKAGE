package com.spam.financialaccounting.infrastructure.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.spam.financialaccounting.domain.entity.JournalMaster;
import com.spam.financialaccounting.domain.repository.JournalMasterRepository;
import com.spam.financialaccounting.presentation.exception.journalmaster.JournalMasterAlreadyExistsException;
import com.spam.financialaccounting.presentation.exception.journalmaster.JournalMasterNotFoundException;

@Repository
public class JournalMasterRepositoryJDBC implements JournalMasterRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<JournalMaster> rowMapper;

    /**
     * Voucher id pattern: {@code JV} + 4-digit year + 6-digit zero-padded sequence,
     * e.g. {@code JV2026000001}. Six digits gives us up to 999,999 vouchers a year;
     * the old 4-digit width overflowed past 9,999 and broke the fixed-width J_ID and
     * its primary key. Total width is 12 chars, which fits the VARCHAR(12) J_ID
     * column. Legacy 10-char ids ({@code JV} + year + 4-digit sequence) still work
     * and won't collide with the wider ones since the lengths differ.
     */
    private static final String ID_FORMAT = "JV%d%06d";

    public JournalMasterRepositoryJDBC(JdbcTemplate jdbcTemplate, RowMapper<JournalMaster> rowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.rowMapper = rowMapper;
    }

    @Override
    @Transactional
    public JournalMaster save(JournalMaster journalMaster) {
        if(existsById(journalMaster.getJId())) {
            throw new JournalMasterAlreadyExistsException("A journal voucher with ID '" + journalMaster.getJId() + "' already exists.");
        }
        String sql = "INSERT INTO JournalMaster(J_ID,J_DOC,J_DATE,J_AMOUNT,J_NARR) VALUES (?,?,?,?,?)";
        // Convert to ISO-8601 String — SQLite stores DATETIME as TEXT and the RowMapper reads it with getString()
        String dateStr = journalMaster.getJDate() != null ? journalMaster.getJDate().toString() : null;
        jdbcTemplate.update(sql, journalMaster.getJId(), journalMaster.getJDoc(), dateStr,
                journalMaster.getJAmount(), journalMaster.getJNarr());

        return journalMaster;
    }

    @Override
    public Optional<JournalMaster> findById(String jId) {
        String sql = "SELECT * FROM JournalMaster WHERE J_ID=?";
        try {
            JournalMaster result = jdbcTemplate.queryForObject(sql, rowMapper, jId);
            return Optional.ofNullable(result);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<JournalMaster> findAll() {
        String sql = "SELECT * FROM JournalMaster";
        return jdbcTemplate.query(sql, rowMapper);
    }

    @Override
    @Transactional
    public JournalMaster update(JournalMaster journalMaster) {
        String sql = "UPDATE JournalMaster SET J_DOC = ?, J_DATE = ?, J_AMOUNT = ?, J_NARR = ? WHERE J_ID = ?";
        // Convert to ISO-8601 String — SQLite stores DATETIME as TEXT and the RowMapper reads it with getString()
        String dateStr = journalMaster.getJDate() != null ? journalMaster.getJDate().toString() : null;
        int rowsAffected = jdbcTemplate.update(sql, journalMaster.getJDoc(), dateStr,
                journalMaster.getJAmount(),
                journalMaster.getJNarr(),
                journalMaster.getJId());

        if (rowsAffected == 0) {
            throw new JournalMasterNotFoundException("No journal voucher found with ID '" + journalMaster.getJId() + "' to update.");
        }

        return journalMaster;
    }

    @Override
    @Transactional
    public boolean delete(String jId) {
        String sql = "DELETE FROM JournalMaster WHERE J_ID=?";
        int rowsAffected = jdbcTemplate.update(sql, jId);
        return rowsAffected > 0;
    }

    @Override
    public boolean existsById(String jId) {
        String sql = "SELECT COUNT(*) FROM JournalMaster WHERE J_ID = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, jId);
        return count != null && count > 0;
    }

    @Override
    public boolean existsByDoc(String jDoc) {
        String sql = "SELECT COUNT(*) FROM JournalMaster WHERE J_DOC = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, jDoc);
        return count != null && count > 0;
    }

    /**
     * Hands out the next voucher id from the JournalSequence counter table.
     *
     * Replaces the old MAX(J_ID)+1 scan, which wasn't atomic: two concurrent
     * posts could read the same max and produce the same id. The
     * {@code UPDATE ... SET LAST_VAL = LAST_VAL + 1} write-locks the year's row
     * until the surrounding transaction commits, so concurrent callers line up
     * and never get the same number.
     *
     * Has to run inside the caller's transaction (e.g. PostJournalVoucher),
     * which it does via PROPAGATION_REQUIRED.
     */
    @Override
    @Transactional
    public String generateNextId() {
        int year = LocalDateTime.now().getYear();

        int updated = jdbcTemplate.update(
                "UPDATE JournalSequence SET LAST_VAL = LAST_VAL + 1 WHERE SEQ_YEAR = ?", year);
        if (updated == 0) {
            // First voucher of the year — create the counter starting at 1.
            try {
                jdbcTemplate.update(
                        "INSERT INTO JournalSequence (SEQ_YEAR, LAST_VAL) VALUES (?, 1)", year);
                return String.format(ID_FORMAT, year, 1);
            } catch (DataIntegrityViolationException raceLost) {
                // Another transaction beat us to creating the row; just increment it.
                jdbcTemplate.update(
                        "UPDATE JournalSequence SET LAST_VAL = LAST_VAL + 1 WHERE SEQ_YEAR = ?", year);
            }
        }

        Integer sequence = jdbcTemplate.queryForObject(
                "SELECT LAST_VAL FROM JournalSequence WHERE SEQ_YEAR = ?", Integer.class, year);
        return String.format(ID_FORMAT, year, sequence);
    }

    @Override
    public String getReversedBy(String jId) {
        return jdbcTemplate.queryForObject(
                "SELECT REVERSED_BY FROM JournalMaster WHERE J_ID = ?", String.class, jId);
    }

    @Override
    public String getReverses(String jId) {
        return jdbcTemplate.queryForObject(
                "SELECT REVERSES FROM JournalMaster WHERE J_ID = ?", String.class, jId);
    }

    @Override
    @Transactional
    public void linkReversal(String originalId, String reversalId) {
        jdbcTemplate.update("UPDATE JournalMaster SET REVERSED_BY = ? WHERE J_ID = ?", reversalId, originalId);
        jdbcTemplate.update("UPDATE JournalMaster SET REVERSES = ? WHERE J_ID = ?", originalId, reversalId);
    }

}
