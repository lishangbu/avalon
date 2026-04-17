package io.github.lishangbu.avalon.catalog.application.berry

import io.github.lishangbu.avalon.catalog.domain.CatalogNotFound
import io.github.lishangbu.avalon.catalog.domain.berry.*
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

/**
 * 树果从属明细应用服务。
 */
@ApplicationScoped
class BerryDetailApplicationService(
    private val definitionRepository: BerryDefinitionRepository,
    private val detailRepository: BerryDetailRepository,
) {
    suspend fun getBattleEffect(berryId: UUID): BerryBattleEffect =
        detailRepository.findBattleEffect(requireBerryId(berryId))
            ?: throw CatalogNotFound("berry_battle_effect", berryId.toString())

    suspend fun saveBattleEffect(
        berryId: UUID,
        draft: BerryBattleEffectDraft,
    ): BerryBattleEffect = detailRepository.saveBattleEffect(requireBerryId(berryId), draft)

    suspend fun getCultivationProfile(berryId: UUID): BerryCultivationProfile =
        detailRepository.findCultivationProfile(requireBerryId(berryId))
            ?: throw CatalogNotFound("berry_cultivation_profile", berryId.toString())

    suspend fun saveCultivationProfile(
        berryId: UUID,
        draft: BerryCultivationProfileDraft,
    ): BerryCultivationProfile = detailRepository.saveCultivationProfile(requireBerryId(berryId), draft)

    suspend fun listAcquisitions(berryId: UUID): List<BerryAcquisition> =
        detailRepository.listAcquisitions(requireBerryId(berryId))

    suspend fun createAcquisition(
        berryId: UUID,
        draft: BerryAcquisitionDraft,
    ): BerryAcquisition = detailRepository.createAcquisition(requireBerryId(berryId), draft)

    suspend fun updateAcquisition(
        berryId: UUID,
        acquisitionId: UUID,
        draft: BerryAcquisitionDraft,
    ): BerryAcquisition =
        detailRepository.updateAcquisition(requireBerryId(berryId), BerryAcquisitionId(acquisitionId), draft)

    suspend fun deleteAcquisition(
        berryId: UUID,
        acquisitionId: UUID,
    ) {
        detailRepository.deleteAcquisition(requireBerryId(berryId), BerryAcquisitionId(acquisitionId))
    }

    suspend fun listMoveRelations(berryId: UUID): List<BerryMoveRelation> =
        detailRepository.listMoveRelations(requireBerryId(berryId))

    suspend fun createMoveRelation(
        berryId: UUID,
        draft: BerryMoveRelationDraft,
    ): BerryMoveRelation = detailRepository.createMoveRelation(requireBerryId(berryId), draft)

    suspend fun deleteMoveRelation(
        berryId: UUID,
        relationId: UUID,
    ) {
        detailRepository.deleteMoveRelation(requireBerryId(berryId), BerryMoveRelationId(relationId))
    }

    suspend fun listAbilityRelations(berryId: UUID): List<BerryAbilityRelation> =
        detailRepository.listAbilityRelations(requireBerryId(berryId))

    suspend fun createAbilityRelation(
        berryId: UUID,
        draft: BerryAbilityRelationDraft,
    ): BerryAbilityRelation = detailRepository.createAbilityRelation(requireBerryId(berryId), draft)

    suspend fun deleteAbilityRelation(
        berryId: UUID,
        relationId: UUID,
    ) {
        detailRepository.deleteAbilityRelation(requireBerryId(berryId), BerryAbilityRelationId(relationId))
    }

    private suspend fun requireBerryId(id: UUID): BerryDefinitionId =
        definitionRepository.findBerryDefinition(BerryDefinitionId(id))
            ?.id
            ?: throw CatalogNotFound("berry_definition", id.toString())
}
