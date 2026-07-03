package com.spam.financialaccounting.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spam.financialaccounting.domain.entity.JournalMaster;

@ExtendWith(MockitoExtension.class)
public class JournalMasterRowMapperTest {

    @Mock
    private ResultSet rs;

    private final JournalMasterRowMapper rowMapper = new JournalMasterRowMapper();

    // Valid ISO-8601 date string that LocalDateTime.parse() accepts
    private static final String VALID_DATE_STR = "2024-01-15T10:00:00";

    @Test
    @DisplayName("mapRow() should map all ResultSet columns to JournalMaster fields correctly")
    void mapRow_ShouldMapAllColumns() throws SQLException {
        when(rs.getString("J_ID")).thenReturn("JV00000001");
        when(rs.getString("J_DOC")).thenReturn("JV");
        when(rs.getString("J_DATE")).thenReturn(VALID_DATE_STR);
        when(rs.getBigDecimal("J_AMOUNT")).thenReturn(new BigDecimal("1000.00"));
        when(rs.getString("J_NARR")).thenReturn("Test narration");

        JournalMaster result = rowMapper.mapRow(rs, 1);

        assertThat(result).isNotNull();
        assertThat(result.getJId()).isEqualTo("JV00000001");
        assertThat(result.getJDoc()).isEqualTo("JV");
        assertThat(result.getJDate()).isEqualTo(LocalDateTime.parse(VALID_DATE_STR));
        assertThat(result.getJAmount()).isEqualByComparingTo("1000.00");
        assertThat(result.getJNarr()).isEqualTo("Test narration");
    }

    @Test
    @DisplayName("mapRow() should correctly map a null narration (optional field)")
    void mapRow_ShouldHandleNullNarration() throws SQLException {
        when(rs.getString("J_ID")).thenReturn("JV00000001");
        when(rs.getString("J_DOC")).thenReturn("PV");
        when(rs.getString("J_DATE")).thenReturn(VALID_DATE_STR);
        when(rs.getBigDecimal("J_AMOUNT")).thenReturn(BigDecimal.ZERO);
        when(rs.getString("J_NARR")).thenReturn(null);

        JournalMaster result = rowMapper.mapRow(rs, 1);

        assertThat(result.getJNarr()).isNull();
        assertThat(result.getJAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("mapRow() should use exact column names: J_ID, J_DOC, J_DATE, J_AMOUNT, J_NARR")
    void mapRow_ShouldReadCorrectColumnNames() throws SQLException {
        // If any column name is wrong, Mockito returns null → assertion below fails
        when(rs.getString("J_ID")).thenReturn("JV00000002");
        when(rs.getString("J_DOC")).thenReturn("JV");
        when(rs.getString("J_DATE")).thenReturn(VALID_DATE_STR);
        when(rs.getBigDecimal("J_AMOUNT")).thenReturn(new BigDecimal("500.00"));
        when(rs.getString("J_NARR")).thenReturn("Entry");

        JournalMaster result = rowMapper.mapRow(rs, 2);

        assertThat(result.getJId()).isNotNull();
        assertThat(result.getJDoc()).isNotNull();
        assertThat(result.getJDate()).isNotNull();
        assertThat(result.getJAmount()).isNotNull();
    }

    @Test
    @DisplayName("mapRow() should throw DateTimeParseException when J_DATE is not a valid ISO-8601 string")
    void mapRow_ShouldThrow_WhenDateIsInvalidFormat() throws SQLException {

        when(rs.getString("J_ID")).thenReturn("JV00000001");
        when(rs.getString("J_DOC")).thenReturn("JV");
        when(rs.getString("J_DATE")).thenReturn("15-01-2024 10:00");

        assertThatThrownBy(() -> rowMapper.mapRow(rs, 1))
                .isInstanceOf(RuntimeException.class);
    }
}
