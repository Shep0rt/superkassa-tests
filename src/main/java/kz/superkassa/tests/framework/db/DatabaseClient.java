package ru.superkassa.tests.framework.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import ru.superkassa.tests.framework.config.TestConfig;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public final class DatabaseClient implements AutoCloseable {
    private final HikariDataSource dataSource;

    public DatabaseClient(TestConfig config) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.databaseJdbcUrl());
        hikariConfig.setUsername(config.databaseUsername());
        hikariConfig.setPassword(config.databasePassword());
        hikariConfig.setReadOnly(true);
        hikariConfig.setMaximumPoolSize(3);
        this.dataSource = new HikariDataSource(hikariConfig);
    }

    public DataSource dataSource() {
        return dataSource;
    }

    public Connection connection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
