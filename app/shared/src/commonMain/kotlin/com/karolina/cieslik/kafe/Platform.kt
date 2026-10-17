package com.karolina.cieslik.kafe

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform