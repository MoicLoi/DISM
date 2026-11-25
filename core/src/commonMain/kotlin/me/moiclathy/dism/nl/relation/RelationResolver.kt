package me.moiclathy.dism.nl.relation

interface RelationResolver {
    fun resolve(mention: RelationMention): RelationResolutionResult
}
