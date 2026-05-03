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
    public Map<String, Double> collectSessionParameters() {

        Map<String, Double> result = new LinkedHashMap<>();

        try (Connection conn = dataSource.getConnection()) {

            for (Metric m : METRICS) {
                double value = fetchParameter(conn, m.oracleName());
                result.put(m.name(), value);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to collect optimizer session metrics", e);
        }

        return result;
    }

    private double fetchParameter(Connection conn, String name) {

        try {
            if (name.startsWith("SYS:")) {
                return fetchSystemMetric(conn, name);
            } else {
                return fetchSessionParameter(conn, name);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch: " + name, e);
        }
    }
    private double fetchSessionParameter(Connection conn, String paramName) throws Exception {

        String sql = """
        SELECT value
        FROM v$parameter
        WHERE name = ?
    """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, paramName);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return parseValue(rs.getString(1));
                }
            }
        }

        return 0.0;
    }
    private double fetchSystemMetric(Connection conn, String metric) throws Exception {

        switch (metric) {

            case "SYS:CPU_USAGE":
                return querySingle(conn,
                        "SELECT value FROM v$sysstat WHERE name = 'CPU used by this session'");

            case "SYS:ACTIVE_SESSIONS":
                return querySingle(conn,
                        "SELECT COUNT(*) FROM v$session WHERE status = 'ACTIVE'");

            case "SYS:LOGICAL_READS":
                return querySingle(conn,
                        "SELECT value FROM v$sysstat WHERE name = 'session logical reads'");

            case "SYS:PHYSICAL_READS":
                return querySingle(conn,
                        "SELECT value FROM v$sysstat WHERE name = 'physical reads'");

            case "SYS:PHYSICAL_WRITES":
                return querySingle(conn,
                        "SELECT value FROM v$sysstat WHERE name = 'physical writes'");

            case "SYS:DB_BLOCK_GETS":
                return querySingle(conn,
                        "SELECT value FROM v$sysstat WHERE name = 'db block gets'");

            case "SYS:CONSISTENT_GETS":
                return querySingle(conn,
                        "SELECT value FROM v$sysstat WHERE name = 'consistent gets'");

            case "SYS:BUFFER_HIT":
                return querySingle(conn,
                        "SELECT (1 - (phy.value / (cur.value + con.value))) * 100 " +
                                "FROM v$sysstat phy, v$sysstat cur, v$sysstat con " +
                                "WHERE phy.name='physical reads' " +
                                "AND cur.name='db block gets' " +
                                "AND con.name='consistent gets'");

            case "SYS:LIB_HIT":
                return querySingle(conn,
                        "SELECT COALESCE((1 - SUM(reloads) / NULLIF(SUM(pins), 0)) * 100, 100) FROM v$librarycache");

            case "SYS:LATCH_WAITS":
                return querySingle(conn,
                        "SELECT SUM(misses) FROM v$latch");

            case "SYS:ENQUEUE":
                return querySingle(conn,
                        "SELECT COUNT(*) FROM v$lock");

            case "SYS:PGA_USED":
                return querySingle(conn,
                        "SELECT value FROM v$pgastat WHERE name='total PGA allocated'");

            case "SYS:TEMP_USED":
                return querySingle(conn,
                        "SELECT SUM(blocks)*8192 FROM v$tempseg_usage");

            case "SYS:IO_WAIT":
                return querySingle(conn,
                        "SELECT time_waited FROM v$system_event WHERE event LIKE 'db file%'");

            case "SYS:LOGONS":
                return querySingle(conn,
                        "SELECT value FROM v$sysstat WHERE name='logons cumulative'");

            case "SYS:PARSE_CALLS":
                return querySingle(conn,
                        "SELECT value FROM v$sysstat WHERE name='parse count (total)'");

            default:
                return 0.0;
        }
    }
    private double querySingle(Connection conn, String sql) throws Exception {

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getDouble(1);
            }
        }

        return 0.0;
    }

    private double parseValue(String value) {

        if (value == null) return 0.0;

        value = value.trim();

        if ("AUTO".equalsIgnoreCase(value)) return 1.0;
        if ("MANUAL".equalsIgnoreCase(value)) return 0.0;

        if ("TRUE".equalsIgnoreCase(value)) return 1.0;
        if ("FALSE".equalsIgnoreCase(value)) return 0.0;

        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return value.hashCode();
        }
    }
    public record Metric(
            String name,
            String oracleName
    ) {}

    public static final java.util.List<Metric> METRICS = java.util.List.of(

            // ===== KNOBS (управляемые) =====
            new Metric("optimizer_index_cost_adj", "optimizer_index_cost_adj"),
            new Metric("optimizer_index_caching", "optimizer_index_caching"),
            new Metric("db_file_multiblock_read_count", "db_file_multiblock_read_count"),
            new Metric("parallel_degree_limit", "parallel_degree_limit"),
            new Metric("parallel_degree_policy", "parallel_degree_policy"),
            new Metric("parallel_min_time_threshold", "parallel_min_time_threshold"),
            new Metric("optimizer_dynamic_sampling", "optimizer_dynamic_sampling"),

            new Metric("pga_aggregate_target", "pga_aggregate_target"),
            new Metric("sga_target", "sga_target"),
            new Metric("db_cache_size", "db_cache_size"),
            new Metric("shared_pool_size", "shared_pool_size"),

            new Metric("result_cache_max_size", "result_cache_max_size"),
            new Metric("hash_area_size", "hash_area_size"),
            new Metric("sort_area_size", "sort_area_size"),

            new Metric("workarea_size_policy", "workarea_size_policy"),
            new Metric("plsql_optimize_level", "plsql_optimize_level"),

            // ===== SYSTEM (НЕ управляемые) =====
            new Metric("cpu_usage", "SYS:CPU_USAGE"),
            new Metric("active_sessions", "SYS:ACTIVE_SESSIONS"),
            new Metric("logical_reads", "SYS:LOGICAL_READS"),
            new Metric("physical_reads", "SYS:PHYSICAL_READS"),
            new Metric("physical_writes", "SYS:PHYSICAL_WRITES"),
            new Metric("db_block_gets", "SYS:DB_BLOCK_GETS"),
            new Metric("consistent_gets", "SYS:CONSISTENT_GETS"),
            new Metric("buffer_cache_hit_ratio", "SYS:BUFFER_HIT"),
            new Metric("library_cache_hit_ratio", "SYS:LIB_HIT"),
            new Metric("latch_waits", "SYS:LATCH_WAITS"),
            new Metric("enqueue_waits", "SYS:ENQUEUE"),
            new Metric("pga_used", "SYS:PGA_USED"),
            new Metric("temp_used", "SYS:TEMP_USED"),
            new Metric("io_wait_time", "SYS:IO_WAIT"),
            new Metric("logons", "SYS:LOGONS"),
            new Metric("parse_calls", "SYS:PARSE_CALLS")
    );
}