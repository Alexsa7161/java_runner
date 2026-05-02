package org.example.adaptive.metrics;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SystemMetricsCollector {

    private final javax.sql.DataSource dataSource;

    public SystemMetricsCollector(javax.sql.DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Double> collect(Connection conn) {

        List<Double> features = new ArrayList<>();

        try (Statement stmt = conn.createStatement()) {

            // CPU usage
            ResultSet cpu = stmt.executeQuery(
                    "SELECT value FROM v$osstat WHERE stat_name = 'BUSY_TIME'"
            );

            features.add(cpu.next() ? cpu.getDouble(1) : 0.0);

            // active sessions
            ResultSet sessions = stmt.executeQuery(
                    "SELECT COUNT(*) FROM v$session WHERE status = 'ACTIVE'"
            );

            features.add(sessions.next() ? sessions.getDouble(1) : 0.0);

            // physical reads
            ResultSet io = stmt.executeQuery(
                    "SELECT value FROM v$sysstat WHERE name = 'physical reads'"
            );

            features.add(io.next() ? io.getDouble(1) : 0.0);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return features;
    }
}