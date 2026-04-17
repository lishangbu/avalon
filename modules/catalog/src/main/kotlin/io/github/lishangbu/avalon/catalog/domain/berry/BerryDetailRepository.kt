package io.github.lishangbu.avalon.catalog.domain.berry

interface BerryDetailRepository {
    suspend fun findBattleEffect(berryId: BerryDefinitionId): BerryBattleEffect?

    suspend fun saveBattleEffect(
        berryId: BerryDefinitionId,
        draft: BerryBattleEffectDraft,
    ): BerryBattleEffect

    suspend fun findCultivationProfile(berryId: BerryDefinitionId): BerryCultivationProfile?

    suspend fun saveCultivationProfile(
        berryId: BerryDefinitionId,
        draft: BerryCultivationProfileDraft,
    ): BerryCultivationProfile

    suspend fun listAcquisitions(berryId: BerryDefinitionId): List<BerryAcquisition>

    suspend fun createAcquisition(
        berryId: BerryDefinitionId,
        draft: BerryAcquisitionDraft,
    ): BerryAcquisition

    suspend fun updateAcquisition(
        berryId: BerryDefinitionId,
        acquisitionId: BerryAcquisitionId,
        draft: BerryAcquisitionDraft,
    ): BerryAcquisition

    suspend fun deleteAcquisition(
        berryId: BerryDefinitionId,
        acquisitionId: BerryAcquisitionId,
    )

    suspend fun listMoveRelations(berryId: BerryDefinitionId): List<BerryMoveRelation>

    suspend fun createMoveRelation(
        berryId: BerryDefinitionId,
        draft: BerryMoveRelationDraft,
    ): BerryMoveRelation

    suspend fun deleteMoveRelation(
        berryId: BerryDefinitionId,
        relationId: BerryMoveRelationId,
    )

    suspend fun listAbilityRelations(berryId: BerryDefinitionId): List<BerryAbilityRelation>

    suspend fun createAbilityRelation(
        berryId: BerryDefinitionId,
        draft: BerryAbilityRelationDraft,
    ): BerryAbilityRelation

    suspend fun deleteAbilityRelation(
        berryId: BerryDefinitionId,
        relationId: BerryAbilityRelationId,
    )
}
