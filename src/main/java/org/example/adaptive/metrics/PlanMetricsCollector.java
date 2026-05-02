package org.example.adaptive.metrics;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PlanMetricsCollector {

    private final javax.sql.DataSource dataSource;

    public PlanMetricsCollector(javax.sql.DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Double> collect(String sql, Connection conn) {

        List<Double> features = new ArrayList<>();

        try (Statement stmt = conn.createStatement()) {

            // 1. Построить план
            stmt.execute("EXPLAIN PLAN FOR " + sql);

            // 2. Забрать план
            ResultSet rs = stmt.executeQuery(
                    "SELECT operation, options, cost, cardinality " +
                            "FROM v$sql_plan WHERE sql_id = (SELECT prev_sql_id FROM v$session WHERE audsid = USERENV('SESSIONID'))"
            );

            int operations = 0;
            int fullScan = 0;
            int indexScan = 0;
            int hashJoin = 0;
            int nestedLoop = 0;

            double totalCost = 0;
            double totalCardinality = 0;

            while (rs.next()) {

                operations++;

                String op = rs.getString("operation");
                String options = rs.getString("options");

                totalCost += rs.getDouble("cost");
                totalCardinality += rs.getDouble("cardinality");

                if ("TABLE ACCESS".equals(op) && "FULL".equals(options)) {
                    fullScan++;
                }

                if (op != null && op.contains("INDEX")) {
                    indexScan++;
                }

                if ("HASH JOIN".equals(op)) {
                    hashJoin++;
                }

                if ("NESTED LOOPS".equals(op)) {
                    nestedLoop++;
                }
            }

            features.add(totalCost);
            features.add(totalCardinality);
            features.add((double) operations);
            features.add((double) fullScan);
            features.add((double) indexScan);
            features.add((double) hashJoin);
            features.add((double) nestedLoop);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return features;
    }
}