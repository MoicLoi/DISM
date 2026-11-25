package me.moiclathy.dism.schedule.dependency

enum class DynamicScheduleDependencyType {
    REQUIRES_COMPLETION,
    REQUIRES_START,
    PRECEDES,
    ASSOCIATED,
}
