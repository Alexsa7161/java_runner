package org.example.adaptive.plan;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.stream.Collectors;

public class OptimizerApplier {

    private final DataSource dataSource;

    public OptimizerApplier(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    public void applySessionSettings(OptimizerSettings settings) {

        if (settings == null || settings.isEmpty()) {
            return;
        }

        try (Connection conn = dataSource.getConnection()) {

            for (Map.Entry<String, String> entry : settings.getAll().entrySet()) {

                String sql = "ALTER SESSION SET " +
                        entry.getKey() + " = " + entry.getValue();

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.execute();
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to apply session settings", e);
        }
    }

    /**
     * Универсальное внедрение параметров в SQL через OPT_PARAM
     */
    public String applyHints(String sql, OptimizerSettings settings) {

        if (sql == null || settings == null || settings.isEmpty()) {
            return sql;
        }

        String trimmed = sql.trim().toUpperCase();
        if (!trimmed.startsWith("SELECT")) {
            return sql;
        }

        String hints = settings.getAll().entrySet().stream()
                .map(e -> "OPT_PARAM('" + e.getKey() + "' " + e.getValue() + ")")
                .collect(Collectors.joining(" "));

        return sql.replaceFirst(
                "(?i)SELECT",
                "SELECT /*+ " + hints + " */"
        );
    }
}