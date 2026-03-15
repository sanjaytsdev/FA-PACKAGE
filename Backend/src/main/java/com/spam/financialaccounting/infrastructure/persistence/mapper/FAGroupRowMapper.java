package com.spam.financialaccounting.infrastructure.persistence.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import com.spam.financialaccounting.domain.entity.FAGroup;

@Component
public class FAGroupRowMapper implements RowMapper<FAGroup> {
    @Override
    public FAGroup mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new FAGroup(
            rs.getString("A_CODE"),
            rs.getString("A_DESC"),
            rs.getString("A_TYPE"),
            rs.getBigDecimal("A_CURRB")
        );
    }
}
