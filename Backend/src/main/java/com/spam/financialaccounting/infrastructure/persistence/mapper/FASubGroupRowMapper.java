package com.spam.financialaccounting.infrastructure.persistence.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import org.springframework.stereotype.Component;

import com.spam.financialaccounting.domain.entity.FASubGroup;

@Component
public class FASubGroupRowMapper implements RowMapper<FASubGroup> {

    @Override
    public FASubGroup mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new FASubGroup(
                rs.getString("S_CODE"),
                rs.getString("S_DESC"),
                rs.getString("A_CODE"),
                rs.getString("S_TYPE"),
                rs.getBigDecimal("S_OPBAL"),
                rs.getString("S_DRCR"),
                rs.getString("S_FLAG"));
    }
}
