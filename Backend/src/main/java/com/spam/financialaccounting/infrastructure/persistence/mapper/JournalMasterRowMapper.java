package com.spam.financialaccounting.infrastructure.persistence.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.math.BigDecimal;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import com.spam.financialaccounting.domain.entity.JournalMaster;

@Component
public class JournalMasterRowMapper implements RowMapper<JournalMaster> {

    @Override
    public JournalMaster mapRow(ResultSet rs, int rowNum) throws SQLException {
        String jId = rs.getString("J_ID");
        String jDoc = rs.getString("J_DOC");
        LocalDateTime jDate = rs.getTimestamp("J_DATE").toLocalDateTime();
        BigDecimal jAmount = rs.getBigDecimal("J_AMOUNT");
        String jNarr = rs.getString("J_NARR");

        return new JournalMaster(jId, jDoc, jDate, jAmount, jNarr);
    }
}
