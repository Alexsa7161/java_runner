package org.example.adaptive.metrics;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class MetricsCollector {

    public enum ParameterScope {
        FEATURE,
        SESSION,
        HINT
    }

    public enum ParameterSource {
        PLAN,
        SYSTEM,
        CONFIG,
        DATA
    }

    public record MetricDefinition(
            String name,
            ParameterSource source,
            ParameterScope scope,
            String oracleSource
    ) {}

    public static final List<MetricDefinition> METRICS = List.of(

            // ================= PLAN =================
            new MetricDefinition("cost", ParameterSource.PLAN, ParameterScope.FEATURE, "V$SQL_PLAN"),
            new MetricDefinition("cardinality", ParameterSource.PLAN, ParameterScope.FEATURE, "V$SQL_PLAN"),
            new MetricDefinition("full_scans", ParameterSource.PLAN, ParameterScope.FEATURE, "V$SQL_PLAN"),
            new MetricDefinition("hash_joins", ParameterSource.PLAN, ParameterScope.FEATURE, "V$SQL_PLAN"),
            new MetricDefinition("nested_loops", ParameterSource.PLAN, ParameterScope.FEATURE, "V$SQL_PLAN"),
            new MetricDefinition("merge_joins", ParameterSource.PLAN, ParameterScope.FEATURE, "V$SQL_PLAN"),
            new MetricDefinition("temp_usage", ParameterSource.PLAN, ParameterScope.FEATURE, "V$SQL_PLAN"),
            new MetricDefinition("plan_depth", ParameterSource.PLAN, ParameterScope.FEATURE, "V$SQL_PLAN"),

            // ================= SYSTEM =================
            new MetricDefinition("cpu_usage", ParameterSource.SYSTEM, ParameterScope.FEATURE, "V$OSSTAT"),
            new MetricDefinition("active_sessions", ParameterSource.SYSTEM, ParameterScope.FEATURE, "V$SESSION"),
            new MetricDefinition("io_read", ParameterSource.SYSTEM, ParameterScope.FEATURE, "V$SYSSTAT"),
            new MetricDefinition("io_write", ParameterSource.SYSTEM, ParameterScope.FEATURE, "V$SYSSTAT"),
            new MetricDefinition("pga_usage", ParameterSource.SYSTEM, ParameterScope.FEATURE, "V$PGASTAT"),
            new MetricDefinition("sga_usage", ParameterSource.SYSTEM, ParameterScope.FEATURE, "V$SGA"),

            // ================= CONFIG =================
            new MetricDefinition("optimizer_mode", ParameterSource.CONFIG, ParameterScope.HINT, "V$PARAMETER"),
            new MetricDefinition("optimizer_index_cost_adj", ParameterSource.CONFIG, ParameterScope.HINT, "V$PARAMETER"),
            new MetricDefinition("optimizer_index_caching", ParameterSource.CONFIG, ParameterScope.HINT, "V$PARAMETER"),
            new MetricDefinition("parallel_degree_policy", ParameterSource.CONFIG, ParameterScope.SESSION, "V$PARAMETER"),
            new MetricDefinition("pga_aggregate_target", ParameterSource.CONFIG, ParameterScope.SESSION, "V$PARAMETER"),

            // ================= DATA =================
            new MetricDefinition("table_rows", ParameterSource.DATA, ParameterScope.FEATURE, "DBA_TABLES"),
            new MetricDefinition("table_size_mb", ParameterSource.DATA, ParameterScope.FEATURE, "DBA_SEGMENTS"),
            new MetricDefinition("index_distinct_keys", ParameterSource.DATA, ParameterScope.FEATURE, "DBA_INDEXES"),
            new MetricDefinition("ndv", ParameterSource.DATA, ParameterScope.FEATURE, "DBA_TAB_COL_STATISTICS"),
            new MetricDefinition("null_percent", ParameterSource.DATA, ParameterScope.FEATURE, "DBA_TAB_COL_STATISTICS")
    );
    private final DataSource dataSource;

    private final PlanMetricsCollector planCollector;
    private final SystemMetricsCollector systemCollector;
    private final ConfigMetricsCollector configCollector;
    private final DataMetricsCollector dataCollector;

    public MetricsCollector(DataSource dataSource) {
        this.dataSource = dataSource;

        this.planCollector = new PlanMetricsCollector(dataSource);
        this.systemCollector = new SystemMetricsCollector(dataSource);
        this.configCollector = new ConfigMetricsCollector(dataSource);
        this.dataCollector = new DataMetricsCollector(dataSource);
    }

    public double[] collectFeatures(String sql) {

        double[] vector = new double[METRICS.size()];

        try (Connection conn = dataSource.getConnection()) {

            Map<ParameterSource, Integer> indexOffset = buildOffsets();

            for (int i = 0; i < METRICS.size(); i++) {

                MetricDefinition def = METRICS.get(i);

                double value = switch (def.source()) {

                    case PLAN -> planCollector.collectOne(def.name(), sql, conn);

                    case SYSTEM -> systemCollector.collectOne(def.name(), conn);

                    case CONFIG -> configCollector.collectOne(def.name(), conn);

                    case DATA -> dataCollector.collectOne(def.name(), sql, conn);
                };

                vector[i] = value;
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to collect metrics", e);
        }

        return vector;
    }
    private Map<ParameterSource, Integer> buildOffsets() {
        return new EnumMap<>(ParameterSource.class);
    }
}