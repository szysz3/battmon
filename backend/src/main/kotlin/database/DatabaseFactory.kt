package com.battmon.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.vendors.PostgreSQLDialect

object DatabaseFactory {
    fun init(
        jdbcUrl: String,
        driverClassName: String,
        user: String,
        password: String,
        maxPoolSize: Int
    ) {
        // Explicitly load the PostgreSQL driver
        Class.forName(driverClassName)

        val dataSource = HikariDataSource(
            HikariConfig().apply {
                this.jdbcUrl = jdbcUrl
                this.driverClassName = driverClassName
                this.username = user
                this.password = password
                this.maximumPoolSize = maxPoolSize
            }
        )

        val database = Database.connect(
            datasource = dataSource,
            databaseConfig = DatabaseConfig {
                useNestedTransactions = true
                explicitDialect = PostgreSQLDialect()
            }
        )

        transaction(database) {
            // Create table
            SchemaUtils.create(UpsStatusTable)

            // Create indexes for common queries
            exec(
                """
                CREATE INDEX IF NOT EXISTS idx_ups_status_timestamp_desc
                ON ups_status (timestamp DESC);
            """.trimIndent()
            )
        }
    }
}
