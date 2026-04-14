package com.spam.financialaccounting.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.spam.financialaccounting.domain.entity.JournalDetail;
import com.spam.financialaccounting.domain.repository.JournalDetailRepository;
import com.spam.financialaccounting.infrastructure.persistence.mapper.JournalDetailRowMapper;

@Repository
public class JournalDetailRepositoryJDBC implements JournalDetailRepository {

    private final JdbcTemplate jdbcTemplate;
    private final JournalDetailRowMapper rowMapper = new JournalDetailRowMapper();

    public JournalDetailRepositoryJDBC(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(JournalDetail detail) {
        String sql = "INSERT INTO JournalDetail(J_ID,J_CODE,J_DRCR,J_AMOUNT) VALUES(?,?,?,?)";
        jdbcTemplate.update(sql, detail.getJId(), detail.getJCode(), detail.getJDrCr(), detail.getJAmount());
    }

    @Override
    public Optional<JournalDetail> findByCompositeKey(String jId, String jCode, String jDrCr) {
        String sql = "SELECT * FROM JournalDetail WHERE J_ID=? AND J_CODE=? AND J_DRCR=?";
        List<JournalDetail> results = jdbcTemplate.query(sql, rowMapper, jId, jCode, jDrCr);
        return results.stream().findFirst();
    }

    @Override
    public List<JournalDetail> findByJournalId(String jId) {
        String sql = "SELECT * FROM JournalDetail WHERE J_ID=?";
        return jdbcTemplate.query(sql, rowMapper, jId);
    }

    @Override
    public List<JournalDetail> findByAccountCode(String jCode) {
        String sql = "SELECT * FROM JournalDetail WHERE J_CODE = ?";
        return jdbcTemplate.query(sql,rowMapper, jCode);
    }

    @Override
    public List<JournalDetail> findAll() {
        String sql = "SELECT * FROM JournalDetail";
        return jdbcTemplate.query(sql, rowMapper);
    }

    @Override
    public JournalDetail update(JournalDetail detail) {
        String sql = "UPDATE JournalDetail SET J_AMOUNT=? WHERE J_ID=? AND J_CODE=? AND J_DRCR=?";
        jdbcTemplate.update(sql, detail.getJAmount(), detail.getJId(), detail.getJCode(), detail.getJDrCr());
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
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, jId, jCode, jDrCr);
        return count != null && count > 0;
    }

    @Override
    public boolean existsByAccountCode(String jCode) {
        String sql = "SELECT COUNT(*) FROM JournalDetail WHERE J_CODE=?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, jCode);
        return count != null && count > 0;
    }
}
