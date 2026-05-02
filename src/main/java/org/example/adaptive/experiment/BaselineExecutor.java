package org.example.adaptive.experiment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.sql.DataSource;

public class BaselineExecutor {

    private final DataSource dataSource;

    public BaselineExecutor(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public long execute(String sql) {

        try (Connection conn = dataSource.getConnection()) {

            long start = System.nanoTime();

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.execute();
            }

            long end = System.nanoTime();

            return (end - start) / 1_000_000;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}