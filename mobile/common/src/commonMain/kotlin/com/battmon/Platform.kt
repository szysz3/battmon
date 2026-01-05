package com.battmon

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform