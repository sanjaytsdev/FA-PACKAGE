package com.spam.financialaccounting.infrastructure.persistence.mapper;

import com.spam.financialaccounting.domain.entity.JournalDetail;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.stereotype.Component;

@Component
public class JournalDetailRowMapper implements RowMapper<JournalDetail> {

    @Override
    public JournalDetail mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new JournalDetail(rs.getString("J_ID"),
                rs.getString("J_CODE"),
                rs.getString("J_DRCR"),
                rs.getBigDecimal("J_AMOUNT"));
    }
}

