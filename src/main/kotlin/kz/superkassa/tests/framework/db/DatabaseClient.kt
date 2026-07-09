package kz.superkassa.tests.framework.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kz.superkassa.tests.framework.config.TestConfig
import java.sql.Connection
import javax.sql.DataSource

@Suppress("unused")
class DatabaseClient(config: TestConfig) : AutoCloseable {
    private val dataSource: HikariDataSource = HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = config.databaseJdbcUrl
            username = config.databaseUsername
            password = config.databasePassword
            isReadOnly = true
            maximumPoolSize = 3
        },
    )

    fun dataSource(): DataSource = dataSource

    fun connection(): Connection = dataSource.connection

    override fun close() {
        dataSource.close()
    }
}
