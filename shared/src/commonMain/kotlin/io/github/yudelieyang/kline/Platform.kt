package io.github.yudelieyang.kline

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform