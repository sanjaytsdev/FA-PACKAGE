package com.spam.financialaccounting.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.spam.financialaccounting.domain.entity.FASubGroup;
import com.spam.financialaccounting.domain.repository.FASubGroupRepository;
import com.spam.financialaccounting.infrastructure.persistence.mapper.FASubGroupRowMapper;

@Repository
class FASubGroupRepositoryJDBC implements FASubGroupRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<FASubGroup> rowMapper;

    public FASubGroupRepositoryJDBC(JdbcTemplate jdbcTemplate, FASubGroupRowMapper rowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.rowMapper = rowMapper;
    }

    @Override
    public FASubGroup save(FASubGroup subGroup) {
        String sql = "INSERT INTO FASubGroup(S_CODE,S_DESC,A_CODE,S_TYPE,S_OPBAL,S_DRCR,S_FLAG) VALUES(?,?,?,?,?,?,?)";
        jdbcTemplate.update(sql,
                subGroup.getSCode(),
                subGroup.getSDesc(),
                subGroup.getACode(),
                subGroup.getSType(),
                subGroup.getSOpbal(),
                subGroup.getSDrCr(),
                subGroup.getSFlag());
        return subGroup;
    }

    @Override
    public Optional<FASubGroup> findByCode(String sCode) {
        String sql = "SELECT * FROM FASubGroup WHERE S_CODE = ?";
        List<FASubGroup> result = jdbcTemplate.query(sql, rowMapper, sCode);
        return result.stream().findFirst();
    }

    @Override
    public List<FASubGroup> findAll() {
        String sql = "SELECT * FROM FASubGroup";
        return jdbcTemplate.query(sql, rowMapper);
    }

    @Override
    public List<FASubGroup> findByACode(String aCode) {
        String sql = "SELECT * FROM FASubGroup WHERE A_CODE=?";
        return jdbcTemplate.query(sql, rowMapper, aCode);
    }

    @Override
    public FASubGroup update(FASubGroup subGroup) {
        String sql = "UPDATE FASubGroup SET S_DESC=?, A_CODE=?, S_TYPE=?, S_OPBAL=?, S_DRCR=?, S_FLAG=? WHERE S_CODE=?";
        jdbcTemplate.update(sql,
                subGroup.getSDesc(),
                subGroup.getACode(),
                subGroup.getSType(),
                subGroup.getSOpbal(),
                subGroup.getSDrCr(),
                subGroup.getSFlag(),
                subGroup.getSCode());
        return subGroup;
    }

    @Override
    public boolean delete(String sCode) {
        String sql = "DELETE FROM FASubGroup WHERE S_CODE = ?";
        int rowsAffected = jdbcTemplate.update(sql, sCode);
        return rowsAffected > 0;
    }

}