package com.spam.financialaccounting.infrastructure.persistence.repository;

import java.util.List;

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
        this.jdbcTemplate=jdbcTemplate;
        this.rowMapper=rowMapper;
    }

    @Override
    public void save(FASubGroup subGroup) {
        String sql = "INSERT INTO FASubGroup(S_CODE,S_DESC,A_CODE,S_TYPE,S_OPBAL,S_DRCR,S_FLAG) VALUES(?,?,?,?,?,?,?)";
        jdbcTemplate.update(sql,
                subGroup.getSCode(),
                subGroup.getSDesc(),
                subGroup.getACode(),
                subGroup.getSType(),
                subGroup.getSOpbal(),
                subGroup.getSDrCr(),
                subGroup.getSFlag());
    }

    @Override
    public FASubGroup findByCode(String sCode) {
        String sql = "SELECT * FROM FASubGroup WHERE S_CODE = ?";
        List<FASubGroup> result = jdbcTemplate.query(sql, rowMapper, sCode);
        return result.isEmpty() ? null : result.get(0);
    }

    @Override
    public List<FASubGroup> findAll() {
        String sql = "SELECT * FROM FASubGroup";
        return jdbcTemplate.query(sql, rowMapper);
    }

    @Override
    public void update(FASubGroup subGroup) {
        String sql = "UPDATE FASubGroup SET S_DESC=?, A_CODE=?, S_TYPE=?, S_OPBAL=?, S_DRCR=?, S_FLAG=? WHERE S_CODE=?";
        jdbcTemplate.update(sql,
                subGroup.getSDesc(),
                subGroup.getACode(),
                subGroup.getSType(),
                subGroup.getSOpbal(),
                subGroup.getSDrCr(),
                subGroup.getSFlag(),
                subGroup.getSCode());
    }

    @Override
    public void delete(String sCode) {
        String sql = "DELETE FROM FASubGroup WHERE S_CODE = ?";
        jdbcTemplate.update(sql, sCode);
    }

}