package org.example.adaptive.metrics;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConfigMetricsCollector {

    private final javax.sql.DataSource dataSource;

    public ConfigMetricsCollector(javax.sql.DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Double> collect(Connection conn) {

        List<Double> features = new ArrayList<>();

        try (Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery(
                    "SELECT value FROM v$parameter WHERE name IN (" +
                            "'optimizer_index_cost_adj'," +
                            "'optimizer_index_caching'" +
                            ")"
            );

            while (rs.next()) {
                try {
                    features.add(Double.parseDouble(rs.getString(1)));
                } catch (Exception e) {
                    features.add(0.0);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return features;
    }
}