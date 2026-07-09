package kz.superkassa.tests.framework.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import java.time.Duration

class TestConfig private constructor(
    private val config: Config,
) {
    val baseUrl: String
        get() = config.getString("superkassa.base-url")

    val apiToken: String
        get() = config.getString("superkassa.api-token")

    val databaseJdbcUrl: String
        get() = config.getString("database.jdbc-url")

    val databaseUsername: String
        get() = config.getString("database.username")

    val databasePassword: String
        get() = config.getString("database.password")

    val defaultTimeout: Duration
        get() = config.getDuration("timeouts.default")

    val pollingInterval: Duration
        get() = config.getDuration("timeouts.polling-interval")

    companion object {
        fun load(): TestConfig = TestConfig(ConfigFactory.load().resolve())
    }
}
