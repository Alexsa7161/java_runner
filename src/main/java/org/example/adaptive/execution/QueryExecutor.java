package org.example.adaptive.execution;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

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
        if (params == null || params.isEmpty()) return;

        // Параметры, которые разрешено менять на уровне сессии
        Set<String> allowedSessionParams = Set.of(
                "optimizer_index_cost_adj",
                "optimizer_index_caching",
                "db_file_multiblock_read_count",
                "parallel_degree_limit",
                "parallel_degree_policy",
                "parallel_min_time_threshold",
                "optimizer_dynamic_sampling",
                "hash_area_size",
                "sort_area_size",
                "workarea_size_policy",
                "plsql_optimize_level"
                // result_cache_max_size – спорно, но может работать,
                // shared_pool_size, db_cache_size, pga_aggregate_target, sga_target – обычно системные, не трогаем
        );

        try (Connection conn = dataSource.getConnection()) {
            for (Map.Entry<String, String> e : params.entrySet()) {
                if (!allowedSessionParams.contains(e.getKey())) continue;

                String sql = "ALTER SESSION SET " + e.getKey() + " = " + e.getValue();
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