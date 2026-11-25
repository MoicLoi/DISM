package me.moiclathy.dism.e2e

/** A normalized observation produced by a natural-language front end. */
data class SemanticObservation(
    val observationId: String,
    val subject: String,
    val predicate: String,
    val objectValue: String,
    val confidence: Double = 1.0,
) {
    init {
        require(observationId.isNotBlank())
        require(subject.isNotBlank())
        require(predicate.isNotBlank())
        require(objectValue.isNotBlank())
        require(confidence in 0.0..1.0)
    }
}
