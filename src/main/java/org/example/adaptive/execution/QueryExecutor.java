package org.example.adaptive.execution;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryExecutor {

    private static final Logger log = LoggerFactory.getLogger(QueryExecutor.class);

    private final DataSource dataSource;

    public QueryExecutor(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    
    public ExecutionResult execute(Connection conn, String sql) {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            long start = System.nanoTime();
            stmt.execute();
            long end = System.nanoTime();
            return new ExecutionResult((end - start) / 1_000_000.0, true);
        } catch (SQLException e) {
            log.error("Query execution failed", e);
            return new ExecutionResult(-1, false);
        }
    }

    
    public ExecutionResult execute(String sql) {
        try (Connection conn = dataSource.getConnection()) {
            return execute(conn, sql);
        } catch (SQLException e) {
            log.error("Failed to obtain connection", e);
            return new ExecutionResult(-1, false);
        }
    }

    
    public void applySessionSettings(Connection conn, Map<String, String> params) {
        if (params == null || params.isEmpty()) return;

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
                "plsql_optimize_level",
                "optimizer_adaptive_plans",
                "optimizer_adaptive_statistics"
        );

        for (Map.Entry<String, String> e : params.entrySet()) {
            if (!allowedSessionParams.contains(e.getKey())) continue;
            String sql = "ALTER SESSION SET " + e.getKey() + " = " + e.getValue();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.execute();
            } catch (SQLException ex) {
                throw new RuntimeException("Failed to apply session setting: " + sql, ex);
            }
        }
    }

    
    public String injectHints(String sql, Map<String, String> params) {
        if (params == null || params.isEmpty()) return sql;
        String hints = String.join(" ",
                params.entrySet().stream()
                        .map(e -> "OPT_PARAM('" + e.getKey() + "' " + e.getValue() + ")")
                        .toList()
        );
        return sql.replaceFirst("(?i)SELECT", "SELECT ");
    }
}