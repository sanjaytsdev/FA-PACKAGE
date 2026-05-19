package com.spam.financialaccounting.infrastructure.persistence.mapper;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spam.financialaccounting.domain.entity.FAGroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FAGroupRowMapperTest {

    @Mock
    private ResultSet rs;

    private final FAGroupRowMapper rowMapper = new FAGroupRowMapper();

    @Test
    @DisplayName("mapRow() should map all ResultSet columns to FAGroup fields correctly")
    void mapRow_ShouldMapAllColumns() throws SQLException {
        // ARRANGE - mock each column the mapper reads
        when(rs.getString("A_CODE")).thenReturn("01");
        when(rs.getString("A_DESC")).thenReturn("Asset");
        when(rs.getString("A_TYPE")).thenReturn("0");
        when(rs.getBigDecimal("A_CURRB")).thenReturn(BigDecimal.ZERO);

        // ACT
        FAGroup result = rowMapper.mapRow(rs, 1);

        // ASSERT — every field is correctly mapped
        assertThat(result).isNotNull();
        assertThat(result.getAccountCode()).isEqualTo("01");
        assertThat(result.getAccountDescription()).isEqualTo("Asset");
        assertThat(result.getAccountType()).isEqualTo("0");
        assertThat(result.getAccountCurrentBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("mapRow() should map a non-zero balance correctly")
    void mapRow_ShouldMapNonZeroBalance() throws SQLException {
        // ARRANGE
        when(rs.getString("A_CODE")).thenReturn("02");
        when(rs.getString("A_DESC")).thenReturn("Liability");
        when(rs.getString("A_TYPE")).thenReturn("1");
        when(rs.getBigDecimal("A_CURRB")).thenReturn(new BigDecimal("9999.99"));
        // ACT
        FAGroup result = rowMapper.mapRow(rs, 1);
        // ASSERT
        assertThat(result.getAccountCurrentBalance()).isEqualByComparingTo("9999.99");
    }
}
