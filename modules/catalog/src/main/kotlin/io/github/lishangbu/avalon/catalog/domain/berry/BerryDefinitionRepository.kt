package io.github.lishangbu.avalon.catalog.domain.berry

interface BerryDefinitionRepository {
    suspend fun listBerryDefinitions(): List<BerryDefinition>

    suspend fun findBerryDefinition(id: BerryDefinitionId): BerryDefinition?

    suspend fun createBerryDefinition(draft: BerryDefinitionDraft): BerryDefinition

    suspend fun updateBerryDefinition(
        id: BerryDefinitionId,
        draft: BerryDefinitionDraft,
    ): BerryDefinition

    suspend fun deleteBerryDefinition(id: BerryDefinitionId)
}
