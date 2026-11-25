package me.moiclathy.dism.context.model

data class ContextId(val value: String) {
    init { require(value.isNotBlank()) { "ContextId must not be blank" } }
}
