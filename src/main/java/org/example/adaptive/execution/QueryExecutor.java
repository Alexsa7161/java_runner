package org.example.adaptive.execution;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

public class QueryExecutor {

    private final DataSource dataSource;

    public QueryExecutor(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public ExecutionResult execute(String sql) {

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            long start = System.nanoTime();

            stmt.execute();

            long end = System.nanoTime();

            double timeMs = (end - start) / 1_000_000.0;

            return new ExecutionResult(timeMs, true);

        } catch (SQLException e) {
            e.printStackTrace();
            return new ExecutionResult(-1, false);
        }
    }

    /**
     * ⚡ ГЛАВНОЕ ИЗМЕНЕНИЕ — теперь применяем ВСЕ параметры
     */
    public void applySessionSettings(Map<String, String> params) {

        if (params == null || params.isEmpty()) {
            return;
        }

        try (Connection conn = dataSource.getConnection()) {

            for (Map.Entry<String, String> e : params.entrySet()) {

                String sql = "ALTER SESSION SET " +
                        e.getKey() + " = " + e.getValue();

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.execute();
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to apply session settings", e);
        }
    }

    /**
     * Hint injection (расширенная версия)
     */
    public String injectHints(String sql, Map<String, String> params) {

        if (params == null || params.isEmpty()) {
            return sql;
        }

        String hints = String.join(" ",
                params.entrySet().stream()
                        .map(e -> "OPT_PARAM('" + e.getKey() + "' " + e.getValue() + ")")
                        .toList()
        );

        return sql.replaceFirst(
                "(?i)SELECT",
                "SELECT /*+ " + hints + " */"
        );
    }
}