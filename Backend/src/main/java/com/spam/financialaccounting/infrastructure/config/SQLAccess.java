package com.spam.financialaccounting.infrastructure.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class SQLAccess {
    private final String connectionString;

    public SQLAccess(String driverName, String connectionString) throws ClassNotFoundException {
        this.connectionString = connectionString;
        Class.forName(driverName);
    }

    // For SELECT single object
    public <T> T queryForObject(String sql, Object[] params, Function<ResultSet, T> mapper) {
        try (Connection con = DriverManager.getConnection(connectionString);
             PreparedStatement stmt = con.prepareStatement(sql)) {

            if (params != null) {
                for (int i = 0; i < params.length; i++)
                    stmt.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapper.apply(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    // For SELECT multiple rows
    public <T> List<T> query(String sql, Object[] params, Function<ResultSet, T> mapper) {
        List<T> result = new ArrayList<>();
        try (Connection con = DriverManager.getConnection(connectionString);
             PreparedStatement stmt = con.prepareStatement(sql)) {

            if (params != null) {
                for (int i = 0; i < params.length; i++)
                    stmt.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapper.apply(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    // For INSERT / UPDATE / DELETE
    public int executeUpdate(String sql, Object... params) {
        try (Connection con = DriverManager.getConnection(connectionString);
             PreparedStatement stmt = con.prepareStatement(sql)) {

            if (params != null) {
                for (int i = 0; i < params.length; i++)
                    stmt.setObject(i + 1, params[i]);
            }

            return stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
