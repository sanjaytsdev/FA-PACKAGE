package com.spam.financialaccounting.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spam.financialaccounting.domain.entity.JournalDetail;

@ExtendWith(MockitoExtension.class)
public class JournalDetailRowMapperTest {

    @Mock
    private ResultSet rs;

    private final JournalDetailRowMapper rowMapper = new JournalDetailRowMapper();

    @Test
    @DisplayName("mapRow() should map all ResultSet columns to JournalDetail fields correctly")
    void mapRow_ShouldMapAllColumns() throws SQLException {
        // ARRANGE — mock every column the mapper reads
        when(rs.getString("J_ID")).thenReturn("V001000001");
        when(rs.getString("J_CODE")).thenReturn("SG001");
        when(rs.getString("J_DRCR")).thenReturn("DR");
        when(rs.getBigDecimal("J_AMOUNT")).thenReturn(new BigDecimal("500.00"));

        // ACT
        JournalDetail result = rowMapper.mapRow(rs, 1);

        // ASSERT — every field is correctly mapped
        assertThat(result).isNotNull();
        assertThat(result.getJId()).isEqualTo("V001000001");
        assertThat(result.getJCode()).isEqualTo("SG001");
        assertThat(result.getJDrCr()).isEqualTo("DR");
        assertThat(result.getJAmount()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("mapRow() should map a CR entry with a large amount correctly")
    void mapRow_ShouldMapCREntryCorrectly() throws SQLException {
        // ARRANGE
        when(rs.getString("J_ID")).thenReturn("V002000001");
        when(rs.getString("J_CODE")).thenReturn("SG002");
        when(rs.getString("J_DRCR")).thenReturn("CR");
        when(rs.getBigDecimal("J_AMOUNT")).thenReturn(new BigDecimal("99999.99"));

        // ACT
        JournalDetail result = rowMapper.mapRow(rs, 2);

        // ASSERT
        assertThat(result.getJDrCr()).isEqualTo("CR");
        assertThat(result.getJAmount()).isEqualByComparingTo("99999.99");
    }

    @Test
    @DisplayName("mapRow() should use the correct column names J_ID, J_CODE, J_DRCR, J_AMOUNT")
    void mapRow_ShouldReadCorrectColumnNames() throws SQLException {
        // ARRANGE — only set up the mocks; if the mapper reads a wrong column name
        // Mockito will return null instead of throwing, making the assertion below fail.
        when(rs.getString("J_ID")).thenReturn("V003000001");
        when(rs.getString("J_CODE")).thenReturn("SG003");
        when(rs.getString("J_DRCR")).thenReturn("DR");
        when(rs.getBigDecimal("J_AMOUNT")).thenReturn(new BigDecimal("1.00"));

        // ACT
        JournalDetail result = rowMapper.mapRow(rs, 1);

        // ASSERT — if any column name was wrong, the field would be null
        assertThat(result.getJId()).isNotNull();
        assertThat(result.getJCode()).isNotNull();
        assertThat(result.getJDrCr()).isNotNull();
        assertThat(result.getJAmount()).isNotNull();
    }
}
