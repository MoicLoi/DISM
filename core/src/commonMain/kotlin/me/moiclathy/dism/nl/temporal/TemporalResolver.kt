package me.moiclathy.dism.nl.temporal

fun interface TemporalResolver {
    fun resolve(text: String): TemporalResolution
}
