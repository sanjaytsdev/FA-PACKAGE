package com.spam.financialaccounting.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

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

    public JournalMasterRepositoryJDBC(JdbcTemplate jdbcTemplate, RowMapper<JournalMaster> rowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.rowMapper = rowMapper;
    }

    @Override
    @Transactional
    public JournalMaster save(JournalMaster journalMaster) {
        if(existsById(journalMaster.getJId())) {
            throw new JournalMasterAlreadyExistsException("JournalMaster with ID " + journalMaster.getJId() + " already exists");
        }
        String sql = "INSERT INTO JournalMaster(J_ID,J_DOC,J_DATE,J_AMOUNT,J_NARR) VALUES (?,?,?,?,?)";
        jdbcTemplate.update(sql, journalMaster.getJId(),journalMaster.getJDoc(),journalMaster.getJDate(),journalMaster.getJAmount(),journalMaster.getJNarr());

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
        int rowsAffected = jdbcTemplate.update(sql, journalMaster.getJDoc(), journalMaster.getJDate(),
                journalMaster.getJAmount(),
                journalMaster.getJNarr(),
                journalMaster.getJId());

        if (rowsAffected == 0) {
            throw new JournalMasterNotFoundException("JournalMaster with ID " + journalMaster.getJId() + " not found");
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
        Integer count = jdbcTemplate.queryForObject(sql,Integer.class,jId);
        return count!=null && count>0;
    }

}
