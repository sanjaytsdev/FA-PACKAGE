package com.spam.financialaccounting.infrastructure.persistence.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.spam.financialaccounting.domain.entity.FAGroup;
import com.spam.financialaccounting.domain.repository.FAGroupRepository;
import com.spam.financialaccounting.infrastructure.persistence.mapper.FAGroupRowMapper;

@Repository
public class FAGroupRepositoryJDBC implements FAGroupRepository{
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<FAGroup> rowMapper;

   public FAGroupRepositoryJDBC(JdbcTemplate jdbcTemplate, FAGroupRowMapper rowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.rowMapper = rowMapper;
    }

    @Override
    public void save(FAGroup faGroup) {
        String sql = "INSERT INTO FAGroup (A_CODE, A_DESC, A_TYPE, A_CURRB) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                faGroup.getAccountCode(),
                faGroup.getAccountDescription(),
                faGroup.getAccountType(),
                faGroup.getAccountCurrentBalance());
    }

    @Override
    public FAGroup findByCode(String aCode) {
        String sql = "SELECT * FROM FAGroup WHERE A_CODE = ?";

        List<FAGroup> result = jdbcTemplate.query(sql, rowMapper, aCode);

        return result.isEmpty() ? null : result.get(0);
    }

    @Override
    public List<FAGroup> findAll() {
        String sql = "SELECT * FROM FAGroup";
        return jdbcTemplate.query(sql, rowMapper);
    }

    @Override
    public void update(FAGroup faGroup) {
        String sql = "UPDATE FAGroup SET A_DESC = ?, A_TYPE = ?, A_CURRB = ? WHERE A_CODE = ?";

        jdbcTemplate.update(sql,
                faGroup.getAccountDescription(),
                faGroup.getAccountType(),
                faGroup.getAccountCurrentBalance(),
                faGroup.getAccountCode());
    }

    @Override
    public void delete(String aCode) {
        String sql = "DELETE FROM FAGroup WHERE A_CODE = ?";
        jdbcTemplate.update(sql, aCode);
    }
}
