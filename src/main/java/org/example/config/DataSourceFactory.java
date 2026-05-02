package org.example.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public class DataSourceFactory {

    public static DataSource create() {

        HikariConfig config = new HikariConfig();

        // =========================
        // Oracle XE Docker (SERVICE NAME)
        // =========================
        config.setJdbcUrl("jdbc:oracle:thin:@//localhost:1521/XEPDB1");

        // =========================
        // TPC-H USER (из init скрипта)
        // =========================
        config.setUsername("tpch");
        config.setPassword("tpch");

        // =========================
        // POOL SETTINGS
        // =========================
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        config.setAutoCommit(true);
        config.setPoolName("OracleTPCHEnginePool");

        return new HikariDataSource(config);
    }
}