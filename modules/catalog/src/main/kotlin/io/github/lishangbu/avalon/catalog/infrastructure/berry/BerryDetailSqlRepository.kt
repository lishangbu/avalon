package io.github.lishangbu.avalon.catalog.infrastructure.berry

import io.github.lishangbu.avalon.catalog.domain.berry.*
import io.github.lishangbu.avalon.catalog.infrastructure.berry.sql.BerrySqlGateway
import io.github.lishangbu.avalon.catalog.infrastructure.sql.mapCatalogDatabaseError
import io.github.lishangbu.avalon.shared.infra.sql.translateSqlErrors
import io.vertx.mutiny.sqlclient.Pool
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class BerryDetailSqlRepository(
    pool: Pool,
) : BerryDetailRepository {
    private val gateway = BerrySqlGateway(pool)

    override suspend fun findBattleEffect(berryId: BerryDefinitionId): BerryBattleEffect? =
        translateSqlErrors(::mapCatalogDatabaseError) { gateway.findBattleEffect(berryId) }

    override suspend fun saveBattleEffect(
        berryId: BerryDefinitionId,
        draft: BerryBattleEffectDraft,
    ): BerryBattleEffect =
        translateSqlErrors(::mapCatalogDatabaseError) { gateway.saveBattleEffect(berryId, draft) }

    override suspend fun findCultivationProfile(berryId: BerryDefinitionId): BerryCultivationProfile? =
        translateSqlErrors(::mapCatalogDatabaseError) { gateway.findCultivationProfile(berryId) }

    override suspend fun saveCultivationProfile(
        berryId: BerryDefinitionId,
        draft: BerryCultivationProfileDraft,
    ): BerryCultivationProfile =
        translateSqlErrors(::mapCatalogDatabaseError) { gateway.saveCultivationProfile(berryId, draft) }

    override suspend fun listAcquisitions(berryId: BerryDefinitionId): List<BerryAcquisition> =
        translateSqlErrors(::mapCatalogDatabaseError) { gateway.listAcquisitions(berryId) }

    override suspend fun createAcquisition(
        berryId: BerryDefinitionId,
        draft: BerryAcquisitionDraft,
    ): BerryAcquisition =
        translateSqlErrors(::mapCatalogDatabaseError) { gateway.createAcquisition(berryId, draft) }

    override suspend fun updateAcquisition(
        berryId: BerryDefinitionId,
        acquisitionId: BerryAcquisitionId,
        draft: BerryAcquisitionDraft,
    ): BerryAcquisition =
        translateSqlErrors(::mapCatalogDatabaseError) { gateway.updateAcquisition(berryId, acquisitionId, draft) }

    override suspend fun deleteAcquisition(
        berryId: BerryDefinitionId,
        acquisitionId: BerryAcquisitionId,
    ) {
        translateSqlErrors(::mapCatalogDatabaseError) { gateway.deleteAcquisition(berryId, acquisitionId) }
    }

    override suspend fun listMoveRelations(berryId: BerryDefinitionId): List<BerryMoveRelation> =
        translateSqlErrors(::mapCatalogDatabaseError) { gateway.listMoveRelations(berryId) }

    override suspend fun createMoveRelation(
        berryId: BerryDefinitionId,
        draft: BerryMoveRelationDraft,
    ): BerryMoveRelation =
        translateSqlErrors(::mapCatalogDatabaseError) { gateway.createMoveRelation(berryId, draft) }

    override suspend fun deleteMoveRelation(
        berryId: BerryDefinitionId,
        relationId: BerryMoveRelationId,
    ) {
        translateSqlErrors(::mapCatalogDatabaseError) { gateway.deleteMoveRelation(berryId, relationId) }
    }

    override suspend fun listAbilityRelations(berryId: BerryDefinitionId): List<BerryAbilityRelation> =
        translateSqlErrors(::mapCatalogDatabaseError) { gateway.listAbilityRelations(berryId) }

    override suspend fun createAbilityRelation(
        berryId: BerryDefinitionId,
        draft: BerryAbilityRelationDraft,
    ): BerryAbilityRelation =
        translateSqlErrors(::mapCatalogDatabaseError) { gateway.createAbilityRelation(berryId, draft) }

    override suspend fun deleteAbilityRelation(
        berryId: BerryDefinitionId,
        relationId: BerryAbilityRelationId,
    ) {
        translateSqlErrors(::mapCatalogDatabaseError) { gateway.deleteAbilityRelation(berryId, relationId) }
    }
}
