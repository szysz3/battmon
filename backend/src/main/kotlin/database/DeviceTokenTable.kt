package com.battmon.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object DeviceTokenTable : Table("device_tokens") {
    val id = long("id").autoIncrement()
    val fcmToken = varchar("fcm_token", 512).uniqueIndex()
    val deviceName = varchar("device_name", 255).nullable()
    val platform = varchar("platform", 50).default("ios")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val lastSeen = timestamp("last_seen")

    override val primaryKey = PrimaryKey(id)
}
