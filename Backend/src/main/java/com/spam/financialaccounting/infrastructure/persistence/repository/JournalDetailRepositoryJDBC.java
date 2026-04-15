package com.spam.financialaccounting.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.DataAccessException;

import com.spam.financialaccounting.domain.entity.JournalDetail;
import com.spam.financialaccounting.domain.repository.JournalDetailRepository;
import com.spam.financialaccounting.infrastructure.persistence.mapper.JournalDetailRowMapper;
import com.spam.financialaccounting.presentation.exception.journaldetail.JournalDetailAlreadyExistsException;
import com.spam.financialaccounting.presentation.exception.journaldetail.JournalDetailNotFoundException;

@Repository
public class JournalDetailRepositoryJDBC implements JournalDetailRepository {

    private final JdbcTemplate jdbcTemplate;
    private final JournalDetailRowMapper rowMapper;

    public JournalDetailRepositoryJDBC(JdbcTemplate jdbcTemplate, JournalDetailRowMapper rowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.rowMapper = rowMapper;
    }

    @Override
    @Transactional
    public void save(JournalDetail detail) {
        if (existsByCompositeKey(detail.getJId(), detail.getJCode(), detail.getJDrCr())) {
            throw new JournalDetailAlreadyExistsException("JournalDetail already exists");
        }
        String sql = "INSERT INTO JournalDetail(J_ID,J_CODE,J_DRCR,J_AMOUNT) VALUES(?,?,?,?)";
        jdbcTemplate.update(sql, detail.getJId(), detail.getJCode(), detail.getJDrCr(), detail.getJAmount());
    }

    @Override
    public Optional<JournalDetail> findByCompositeKey(String jId, String jCode, String jDrCr) {
        String sql = "SELECT * FROM JournalDetail WHERE J_ID=? AND J_CODE=? AND J_DRCR=?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, rowMapper, jId, jCode, jDrCr));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<JournalDetail> findByJournalId(String jId) {
        String sql = "SELECT * FROM JournalDetail WHERE J_ID=?";
        return jdbcTemplate.query(sql, rowMapper, jId);
    }

    @Override
    public List<JournalDetail> findByAccountCode(String jCode) {
        String sql = "SELECT * FROM JournalDetail WHERE J_CODE = ?";
        return jdbcTemplate.query(sql, rowMapper, jCode);
    }

    @Override
    public List<JournalDetail> findAll() {
        String sql = "SELECT * FROM JournalDetail";
        return jdbcTemplate.query(sql, rowMapper);
    }

    @Override
    public JournalDetail update(JournalDetail detail) {
        String sql = "UPDATE JournalDetail SET J_AMOUNT=? WHERE J_ID=? AND J_CODE=? AND J_DRCR=?";
        int rowsAffected = jdbcTemplate.update(sql, detail.getJAmount(), detail.getJId(), detail.getJCode(),
                detail.getJDrCr());
        if (rowsAffected == 0) {
            throw new JournalDetailNotFoundException("No JournalDetail found to update");
        }
        return detail;
    }

    @Override
    public boolean deleteByCompositeKey(String jId, String jCode, String jDrCr) {
        String sql = "DELETE FROM JournalDetail WHERE J_ID=? AND J_CODE=? AND J_DRCR=?";
        int rowsAffected = jdbcTemplate.update(sql, jId, jCode, jDrCr);
        return rowsAffected > 0;
    }

    @Override
    public boolean deleteByJournalId(String jId) {
        String sql = "DELETE FROM JournalDetail WHERE J_ID = ?";
        int rowsAffected = jdbcTemplate.update(sql, jId);
        return rowsAffected > 0;
    }

    @Override
    public boolean existsByCompositeKey(String jId, String jCode, String jDrCr) {
        String sql = "SELECT COUNT(*) FROM JournalDetail WHERE J_ID=? AND J_CODE=? AND J_DRCR=?";
        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, jId, jCode, jDrCr);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            throw new RuntimeException("Database error checking existence", e);
        }
    }

    @Override
    public boolean existsByAccountCode(String jCode) {
        String sql = "SELECT COUNT(*) FROM JournalDetail WHERE J_CODE=?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, jCode);
        return count != null && count > 0;
    }
}
