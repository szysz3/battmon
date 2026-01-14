package com.battmon.model

import kotlinx.serialization.Serializable

@Serializable
data class UpsStatusHistory(
    val data: List<UpsStatus>,
    val count: Int,
    val from: String?,
    val to: String?,
    val totalCount: Long? = null,
    val limit: Int? = null,
    val offset: Long? = null
)
