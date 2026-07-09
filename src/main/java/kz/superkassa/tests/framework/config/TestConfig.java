package ru.superkassa.tests.framework.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.time.Duration;

public final class TestConfig {
    private final Config config;

    private TestConfig(Config config) {
        this.config = config;
    }

    public static TestConfig load() {
        return new TestConfig(ConfigFactory.load().resolve());
    }

    public String baseUrl() {
        return config.getString("superkassa.base-url");
    }

    public String apiToken() {
        return config.getString("superkassa.api-token");
    }

    public String databaseJdbcUrl() {
        return config.getString("database.jdbc-url");
    }

    public String databaseUsername() {
        return config.getString("database.username");
    }

    public String databasePassword() {
        return config.getString("database.password");
    }

    public Duration defaultTimeout() {
        return config.getDuration("timeouts.default");
    }

    public Duration pollingInterval() {
        return config.getDuration("timeouts.polling-interval");
    }
}
