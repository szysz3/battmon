package com.battmon.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.vendors.PostgreSQLDialect

object DatabaseFactory {
    private var dataSource: HikariDataSource? = null

    fun init(
        jdbcUrl: String,
        driverClassName: String,
        user: String,
        password: String,
        maxPoolSize: Int
    ) {
        Class.forName(driverClassName)

        dataSource = HikariDataSource(
            HikariConfig().apply {
                this.jdbcUrl = jdbcUrl
                this.driverClassName = driverClassName
                this.username = user
                this.password = password
                this.maximumPoolSize = maxPoolSize
            }
        )

        val database = Database.connect(
            datasource = dataSource!!,
            databaseConfig = DatabaseConfig {
                useNestedTransactions = true
                explicitDialect = PostgreSQLDialect()
            }
        )

        transaction(database) {
            // Create tables in dependency order (UpsDeviceTable first as it's referenced by UpsStatusTable)
            SchemaUtils.create(UpsDeviceTable)
            SchemaUtils.create(UpsStatusTable)
            SchemaUtils.create(DeviceTokenTable)

            // =====================================================================
            // MIGRATION NOTE: Legacy Data Handling
            // =====================================================================
            // The ups_device_id column is nullable to support migration from single-device
            // to multi-device mode. Pre-migration status records will have NULL device IDs.
            //
            // Important behavior:
            // - findLatestForAllDevices() excludes records with NULL ups_device_id
            // - Legacy records are still accessible via findByTimeRange() with deviceId=null
            // - New status records always have a device ID assigned
            //
            // To migrate legacy data, assign a device ID to orphaned records:
            //   UPDATE ups_status SET ups_device_id = 'legacy-ups' WHERE ups_device_id IS NULL;
            // =====================================================================

            // Ensure ups_status has the ups_device_id column (for existing databases)
            exec(
                """
                ALTER TABLE ups_status
                ADD COLUMN IF NOT EXISTS ups_device_id VARCHAR(255);
                """.trimIndent()
            )

            // Ensure foreign key constraint exists
            exec(
                """
                DO $$
                BEGIN
                    IF NOT EXISTS (
                        SELECT 1 FROM pg_constraint WHERE conname = 'ups_status_device_fk'
                    ) THEN
                        ALTER TABLE ups_status
                        ADD CONSTRAINT ups_status_device_fk
                        FOREIGN KEY (ups_device_id)
                        REFERENCES ups_devices(id)
                        ON DELETE CASCADE;
                    END IF;
                END $$;
                """.trimIndent()
            )

            // Index for time-based queries (existing)
            exec(
                """
                CREATE INDEX IF NOT EXISTS idx_ups_status_timestamp_desc
                ON ups_status (timestamp DESC);
                """.trimIndent()
            )

            // Composite index for per-device queries (most common query pattern)
            exec(
                """
                CREATE INDEX IF NOT EXISTS idx_ups_status_device_time
                ON ups_status (ups_device_id, timestamp DESC);
                """.trimIndent()
            )

            // Index for enabled devices (for monitoring startup)
            exec(
                """
                CREATE INDEX IF NOT EXISTS idx_ups_devices_enabled
                ON ups_devices (enabled) WHERE enabled = true;
                """.trimIndent()
            )

            // Unique constraint on host:port to prevent duplicate UPS registrations
            exec(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS idx_ups_devices_host_port
                ON ups_devices (host, port);
                """.trimIndent()
            )

            // Port range check constraint
            exec(
                """
                DO $$
                BEGIN
                    IF NOT EXISTS (
                        SELECT 1 FROM pg_constraint WHERE conname = 'ups_devices_port_range'
                    ) THEN
                        ALTER TABLE ups_devices ADD CONSTRAINT ups_devices_port_range
                        CHECK (port >= 1 AND port <= 65535);
                    END IF;
                END $$;
                """.trimIndent()
            )
        }
    }

    fun shutdown() {
        dataSource?.close()
        dataSource = null
    }
}
