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

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {

                long start = System.nanoTime();

                stmt.execute();

                return (System.nanoTime() - start) / 1_000_000;

            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}