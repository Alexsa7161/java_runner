package org.example.adaptive.metrics;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

public class MetricsCollector {

    private final DataSource dataSource;

    public MetricsCollector(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // =========================
    // MAIN ENTRY (НОВЫЙ МЕТОД)
    // =========================
    public Map<String, Double> collectAll() {

        Map<String, Double> result = new LinkedHashMap<>();

        try (Connection conn = dataSource.getConnection()) {

            // ---- STATIC ----
            for (Metric m : STATIC_METRICS) {
                result.put(m.name(), fetchParameter(conn, m.oracleName()));
            }

            // ---- DYNAMIC ----
            for (Metric m : DYNAMIC_METRICS) {
                result.put(m.name(), fetchDynamic(conn, m.oracleName()));
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to collect all metrics", e);
        }

        return result;
    }

    // =========================
    // STATIC (v$parameter)
    // =========================
    private double fetchParameter(Connection conn, String name) {

        try (PreparedStatement stmt = conn.prepareStatement("""
                SELECT value
                FROM v$parameter
                WHERE name = ?
        """)) {

            stmt.setString(1, name);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return parse(rs.getString(1));
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch param: " + name, e);
        }

        return 0.0;
    }

    // =========================
    // DYNAMIC (v$sysstat / v$ views)
    // =========================
    private double fetchDynamic(Connection conn, String key) {

        try {

            return switch (key) {

                case "cpu_usage" -> single(conn,
                        "SELECT value FROM v$sysstat WHERE name='CPU used by this session'");

                case "logical_reads" -> single(conn,
                        "SELECT value FROM v$sysstat WHERE name='session logical reads'");

                case "physical_reads" -> single(conn,
                        "SELECT value FROM v$sysstat WHERE name='physical reads'");

                case "physical_writes" -> single(conn,
                        "SELECT value FROM v$sysstat WHERE name='physical writes'");

                case "db_block_gets" -> single(conn,
                        "SELECT value FROM v$sysstat WHERE name='db block gets'");

                case "consistent_gets" -> single(conn,
                        "SELECT value FROM v$sysstat WHERE name='consistent gets'");

                case "parse_calls" -> single(conn,
                        "SELECT value FROM v$sysstat WHERE name='parse count (total)'");

                case "hard_parses" -> single(conn,
                        "SELECT value FROM v$sysstat WHERE name='parse count (hard)'");

                case "active_sessions" -> single(conn,
                        "SELECT COUNT(*) FROM v$session WHERE status='ACTIVE'");

                case "pga_used" -> single(conn,
                        "SELECT value FROM v$pgastat WHERE name='total PGA allocated'");

                case "temp_used" -> single(conn,
                        "SELECT NVL(SUM(blocks)*8192,0) FROM v$tempseg_usage");

                case "io_wait_time" -> single(conn,
                        "SELECT NVL(SUM(time_waited),0) FROM v$system_event WHERE event LIKE 'db file%'");

                case "buffer_cache_hit_ratio" -> single(conn,
                        "SELECT (1 - (phy.value / (cur.value + con.value))) * 100 " +
                                "FROM v$sysstat phy, v$sysstat cur, v$sysstat con " +
                                "WHERE phy.name='physical reads' " +
                                "AND cur.name='db block gets' " +
                                "AND con.name='consistent gets'");

                case "rows_processed" -> single(conn,
                        "SELECT value FROM v$sysstat WHERE name='table scan rows gotten'");

                case "table_scans" -> single(conn,
                        "SELECT value FROM v$sysstat WHERE name='table scans (long tables)'");

                case "index_scans" -> single(conn,
                        "SELECT value FROM v$sysstat WHERE name='index scans kdiixs1'");

                case "join_operations" -> single(conn,
                        "SELECT value FROM v$sysstat WHERE name='sorts (rows)'");

                default -> 0.0;
            };

        } catch (Exception e) {
            throw new RuntimeException("Failed dynamic metric: " + key, e);
        }
    }

    private double single(Connection conn, String sql) throws Exception {

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getDouble(1);
            }
        }

        return 0.0;
    }

    // =========================
    // PARSE
    // =========================
    private double parse(String value) {

        if (value == null) return 0.0;

        value = value.trim();

        switch (value.toUpperCase()) {
            case "AUTO": return 1.0;
            case "MANUAL": return 0.0;
            case "TRUE": return 1.0;
            case "FALSE": return 0.0;
        }

        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return value.hashCode() % 1000;
        }
    }

    // =========================
    // METRIC DEFINITIONS
    // =========================
    public record Metric(String name, String oracleName) {}

    // ---- STATIC (16)
    public static final java.util.List<Metric> STATIC_METRICS = java.util.List.of(

            new Metric("optimizer_index_cost_adj", "optimizer_index_cost_adj"),
            new Metric("optimizer_index_caching", "optimizer_index_caching"),
            new Metric("optimizer_dynamic_sampling", "optimizer_dynamic_sampling"),
            new Metric("optimizer_features_enable", "optimizer_features_enable"),

            new Metric("pga_aggregate_target", "pga_aggregate_target"),
            new Metric("workarea_size_policy", "workarea_size_policy"),
            new Metric("db_file_multiblock_read_count", "db_file_multiblock_read_count"),
            new Metric("query_rewrite_enabled", "query_rewrite_enabled")
    );

    // ---- DYNAMIC (16)
    public static final java.util.List<Metric> DYNAMIC_METRICS = java.util.List.of(

            new Metric("cpu_usage", "cpu_usage"),
            new Metric("logical_reads", "logical_reads"),
            new Metric("physical_reads", "physical_reads"),
            new Metric("physical_writes", "physical_writes"),
            new Metric("db_block_gets", "db_block_gets"),
            new Metric("consistent_gets", "consistent_gets"),
            new Metric("parse_calls", "parse_calls"),
            new Metric("hard_parses", "hard_parses"),

            new Metric("active_sessions", "active_sessions"),
            new Metric("pga_used", "pga_used"),
            new Metric("temp_used", "temp_used"),
            new Metric("io_wait_time", "io_wait_time"),
            new Metric("buffer_cache_hit_ratio", "buffer_cache_hit_ratio"),
            new Metric("rows_processed", "rows_processed"),
            new Metric("table_scans", "table_scans"),
            new Metric("index_scans", "index_scans"),
            new Metric("join_operations", "join_operations")
    );
}