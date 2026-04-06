package com.spam.financialaccounting.infrastructure.persistence.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.spam.financialaccounting.domain.entity.JournalDetail;
import com.spam.financialaccounting.domain.repository.JournalDetailRepository;

@Repository
public class JournalDetailRepositoryJDBC implements JournalDetailRepository {

    private final JdbcTemplate jdbcTemplate;

    public JournalDetailRepositoryJDBC(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(JournalDetail detail) {
        String sql = "INSERT INTO JournalDetail(J_ID,J_CODE,J_DRCR,J_AMOUNT) VALUES(?,?,?,?)";
        jdbcTemplate.update(sql, detail.getJId(), detail.getJCode(), detail.getJDrCr(), detail.getJAmount());
    }
}
