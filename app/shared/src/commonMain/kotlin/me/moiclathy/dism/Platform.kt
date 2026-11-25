package me.moiclathy.dism

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform