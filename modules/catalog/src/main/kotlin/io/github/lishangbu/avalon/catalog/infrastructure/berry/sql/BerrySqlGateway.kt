package io.github.lishangbu.avalon.catalog.infrastructure.berry.sql

import io.github.lishangbu.avalon.catalog.domain.CatalogNotFound
import io.github.lishangbu.avalon.catalog.domain.berry.*
import io.github.lishangbu.avalon.shared.infra.mutiny.awaitSuspending
import io.github.lishangbu.avalon.shared.infra.sql.first
import io.github.lishangbu.avalon.shared.infra.sql.firstOrNull
import io.github.lishangbu.avalon.shared.infra.sql.toRows
import io.github.lishangbu.avalon.shared.infra.sql.withSuspendingTransaction
import io.vertx.mutiny.sqlclient.Pool
import io.vertx.mutiny.sqlclient.Tuple

internal class BerrySqlGateway(
    private val pool: Pool,
) {
    suspend fun listBerryDefinitions(): List<BerryDefinition> =
        pool.query("$BERRY_DEFINITION_SELECT_SQL ORDER BY bd.sorting_order, bd.id")
            .execute()
            .awaitSuspending()
            .toRows()
            .map(::mapBerryDefinition)

    suspend fun findBerryDefinition(id: BerryDefinitionId): BerryDefinition? =
        pool.preparedQuery("$BERRY_DEFINITION_SELECT_SQL WHERE bd.id = $1")
            .execute(Tuple.of(id.value))
            .awaitSuspending()
            .firstOrNull()
            ?.let(::mapBerryDefinition)

    suspend fun createBerryDefinition(draft: BerryDefinitionDraft): BerryDefinition =
        pool.withSuspendingTransaction { connection ->
            val row =
                connection.preparedQuery(
                    """
                    INSERT INTO catalog.berry_definition (
                        code, name, description, icon, color_code, firmness_code, size_cm, smoothness,
                        spicy, dry, sweet, bitter, sour, natural_gift_type_id, natural_gift_power,
                        sorting_order, enabled
                    )
                    VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17)
                    RETURNING id
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addString(draft.code)
                        .addString(draft.name)
                        .addValue(draft.description)
                        .addValue(draft.icon)
                        .addValue(draft.colorCode)
                        .addValue(draft.firmnessCode)
                        .addValue(draft.sizeCm)
                        .addValue(draft.smoothness)
                        .addInteger(draft.spicy)
                        .addInteger(draft.dry)
                        .addInteger(draft.sweet)
                        .addInteger(draft.bitter)
                        .addInteger(draft.sour)
                        .addValue(draft.naturalGiftTypeId)
                        .addValue(draft.naturalGiftPower)
                        .addInteger(draft.sortingOrder)
                        .addBoolean(draft.enabled),
                ).awaitSuspending()
                    .first()
            requireBerryDefinition(connection, row.getUUID("id"))
        }

    suspend fun updateBerryDefinition(
        id: BerryDefinitionId,
        draft: BerryDefinitionDraft,
    ): BerryDefinition =
        pool.withSuspendingTransaction { connection ->
            val updated =
                connection.preparedQuery(
                    """
                    UPDATE catalog.berry_definition
                    SET code = $1,
                        name = $2,
                        description = $3,
                        icon = $4,
                        color_code = $5,
                        firmness_code = $6,
                        size_cm = $7,
                        smoothness = $8,
                        spicy = $9,
                        dry = $10,
                        sweet = $11,
                        bitter = $12,
                        sour = $13,
                        natural_gift_type_id = $14,
                        natural_gift_power = $15,
                        sorting_order = $16,
                        enabled = $17,
                        version = version + 1
                    WHERE id = $18
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addString(draft.code)
                        .addString(draft.name)
                        .addValue(draft.description)
                        .addValue(draft.icon)
                        .addValue(draft.colorCode)
                        .addValue(draft.firmnessCode)
                        .addValue(draft.sizeCm)
                        .addValue(draft.smoothness)
                        .addInteger(draft.spicy)
                        .addInteger(draft.dry)
                        .addInteger(draft.sweet)
                        .addInteger(draft.bitter)
                        .addInteger(draft.sour)
                        .addValue(draft.naturalGiftTypeId)
                        .addValue(draft.naturalGiftPower)
                        .addInteger(draft.sortingOrder)
                        .addBoolean(draft.enabled)
                        .addValue(id.value),
                ).awaitSuspending()
                    .rowCount()
            if (updated == 0) {
                throw CatalogNotFound("berry_definition", id.value.toString())
            }
            requireBerryDefinition(connection, id.value)
        }

    suspend fun deleteBerryDefinition(id: BerryDefinitionId) {
        val deleted =
            pool.preparedQuery("DELETE FROM catalog.berry_definition WHERE id = $1")
                .execute(Tuple.of(id.value))
                .awaitSuspending()
                .rowCount()
        if (deleted == 0) {
            throw CatalogNotFound("berry_definition", id.value.toString())
        }
    }

    suspend fun findBattleEffect(berryId: BerryDefinitionId): BerryBattleEffect? =
        pool.preparedQuery("$BERRY_BATTLE_EFFECT_SELECT_SQL WHERE berry_id = $1")
            .execute(Tuple.of(berryId.value))
            .awaitSuspending()
            .firstOrNull()
            ?.let(::mapBerryBattleEffect)

    suspend fun saveBattleEffect(
        berryId: BerryDefinitionId,
        draft: BerryBattleEffectDraft,
    ): BerryBattleEffect =
        pool.withSuspendingTransaction { connection ->
            connection.preparedQuery(
                """
                INSERT INTO catalog.berry_battle_effect (
                    berry_id, hold_effect_summary, direct_use_effect_summary, fling_power,
                    fling_effect_summary, pluck_effect_summary, bug_bite_effect_summary
                )
                VALUES ($1, $2, $3, $4, $5, $6, $7)
                ON CONFLICT (berry_id) DO UPDATE
                SET hold_effect_summary = EXCLUDED.hold_effect_summary,
                    direct_use_effect_summary = EXCLUDED.direct_use_effect_summary,
                    fling_power = EXCLUDED.fling_power,
                    fling_effect_summary = EXCLUDED.fling_effect_summary,
                    pluck_effect_summary = EXCLUDED.pluck_effect_summary,
                    bug_bite_effect_summary = EXCLUDED.bug_bite_effect_summary,
                    version = catalog.berry_battle_effect.version + 1
                """.trimIndent(),
            ).execute(
                Tuple.tuple()
                    .addValue(berryId.value)
                    .addValue(draft.holdEffectSummary)
                    .addValue(draft.directUseEffectSummary)
                    .addValue(draft.flingPower)
                    .addValue(draft.flingEffectSummary)
                    .addValue(draft.pluckEffectSummary)
                    .addValue(draft.bugBiteEffectSummary),
            ).awaitSuspending()
            requireBattleEffect(connection, berryId.value)
        }

    suspend fun findCultivationProfile(berryId: BerryDefinitionId): BerryCultivationProfile? =
        pool.preparedQuery("$BERRY_CULTIVATION_SELECT_SQL WHERE berry_id = $1")
            .execute(Tuple.of(berryId.value))
            .awaitSuspending()
            .firstOrNull()
            ?.let(::mapBerryCultivationProfile)

    suspend fun saveCultivationProfile(
        berryId: BerryDefinitionId,
        draft: BerryCultivationProfileDraft,
    ): BerryCultivationProfile =
        pool.withSuspendingTransaction { connection ->
            connection.preparedQuery(
                """
                INSERT INTO catalog.berry_cultivation_profile (
                    berry_id, growth_hours_min, growth_hours_max, yield_min, yield_max, cultivation_summary
                )
                VALUES ($1, $2, $3, $4, $5, $6)
                ON CONFLICT (berry_id) DO UPDATE
                SET growth_hours_min = EXCLUDED.growth_hours_min,
                    growth_hours_max = EXCLUDED.growth_hours_max,
                    yield_min = EXCLUDED.yield_min,
                    yield_max = EXCLUDED.yield_max,
                    cultivation_summary = EXCLUDED.cultivation_summary,
                    version = catalog.berry_cultivation_profile.version + 1
                """.trimIndent(),
            ).execute(
                Tuple.tuple()
                    .addValue(berryId.value)
                    .addValue(draft.growthHoursMin)
                    .addValue(draft.growthHoursMax)
                    .addValue(draft.yieldMin)
                    .addValue(draft.yieldMax)
                    .addValue(draft.cultivationSummary),
            ).awaitSuspending()
            requireCultivationProfile(connection, berryId.value)
        }

    suspend fun listAcquisitions(berryId: BerryDefinitionId): List<BerryAcquisition> =
        pool.preparedQuery("$BERRY_ACQUISITION_SELECT_SQL WHERE berry_id = $1 ORDER BY sorting_order, id")
            .execute(Tuple.of(berryId.value))
            .awaitSuspending()
            .toRows()
            .map(::mapBerryAcquisition)

    suspend fun createAcquisition(
        berryId: BerryDefinitionId,
        draft: BerryAcquisitionDraft,
    ): BerryAcquisition =
        pool.withSuspendingTransaction { connection ->
            val row =
                connection.preparedQuery(
                    """
                    INSERT INTO catalog.berry_acquisition (berry_id, source_type, condition_note, sorting_order, enabled)
                    VALUES ($1, $2, $3, $4, $5)
                    RETURNING id
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addValue(berryId.value)
                        .addString(draft.sourceType)
                        .addString(draft.conditionNote)
                        .addInteger(draft.sortingOrder)
                        .addBoolean(draft.enabled),
                ).awaitSuspending()
                    .first()
            requireAcquisition(connection, row.getUUID("id"))
        }

    suspend fun updateAcquisition(
        berryId: BerryDefinitionId,
        acquisitionId: BerryAcquisitionId,
        draft: BerryAcquisitionDraft,
    ): BerryAcquisition =
        pool.withSuspendingTransaction { connection ->
            val updated =
                connection.preparedQuery(
                    """
                    UPDATE catalog.berry_acquisition
                    SET source_type = $1,
                        condition_note = $2,
                        sorting_order = $3,
                        enabled = $4,
                        version = version + 1
                    WHERE id = $5 AND berry_id = $6
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addString(draft.sourceType)
                        .addString(draft.conditionNote)
                        .addInteger(draft.sortingOrder)
                        .addBoolean(draft.enabled)
                        .addValue(acquisitionId.value)
                        .addValue(berryId.value),
                ).awaitSuspending()
                    .rowCount()
            if (updated == 0) throw CatalogNotFound("berry_acquisition", acquisitionId.value.toString())
            requireAcquisition(connection, acquisitionId.value)
        }

    suspend fun deleteAcquisition(
        berryId: BerryDefinitionId,
        acquisitionId: BerryAcquisitionId,
    ) {
        val deleted =
            pool.preparedQuery("DELETE FROM catalog.berry_acquisition WHERE id = $1 AND berry_id = $2")
                .execute(Tuple.of(acquisitionId.value, berryId.value))
                .awaitSuspending()
                .rowCount()
        if (deleted == 0) throw CatalogNotFound("berry_acquisition", acquisitionId.value.toString())
    }

    suspend fun listMoveRelations(berryId: BerryDefinitionId): List<BerryMoveRelation> =
        pool.preparedQuery("$BERRY_MOVE_RELATION_SELECT_SQL WHERE berry_id = $1 ORDER BY sorting_order, id")
            .execute(Tuple.of(berryId.value))
            .awaitSuspending()
            .toRows()
            .map(::mapBerryMoveRelation)

    suspend fun createMoveRelation(
        berryId: BerryDefinitionId,
        draft: BerryMoveRelationDraft,
    ): BerryMoveRelation =
        pool.withSuspendingTransaction { connection ->
            val row =
                connection.preparedQuery(
                    """
                    INSERT INTO catalog.berry_move_relation (berry_id, move_code, move_name, relation_kind, note, sorting_order, enabled)
                    VALUES ($1, $2, $3, $4, $5, $6, $7)
                    RETURNING id
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addValue(berryId.value)
                        .addString(draft.moveCode)
                        .addString(draft.moveName)
                        .addString(draft.relationKind)
                        .addValue(draft.note)
                        .addInteger(draft.sortingOrder)
                        .addBoolean(draft.enabled),
                ).awaitSuspending()
                    .first()
            requireMoveRelation(connection, row.getUUID("id"))
        }

    suspend fun deleteMoveRelation(
        berryId: BerryDefinitionId,
        relationId: BerryMoveRelationId,
    ) {
        val deleted =
            pool.preparedQuery("DELETE FROM catalog.berry_move_relation WHERE id = $1 AND berry_id = $2")
                .execute(Tuple.of(relationId.value, berryId.value))
                .awaitSuspending()
                .rowCount()
        if (deleted == 0) throw CatalogNotFound("berry_move_relation", relationId.value.toString())
    }

    suspend fun listAbilityRelations(berryId: BerryDefinitionId): List<BerryAbilityRelation> =
        pool.preparedQuery("$BERRY_ABILITY_RELATION_SELECT_SQL WHERE berry_id = $1 ORDER BY sorting_order, id")
            .execute(Tuple.of(berryId.value))
            .awaitSuspending()
            .toRows()
            .map(::mapBerryAbilityRelation)

    suspend fun createAbilityRelation(
        berryId: BerryDefinitionId,
        draft: BerryAbilityRelationDraft,
    ): BerryAbilityRelation =
        pool.withSuspendingTransaction { connection ->
            val row =
                connection.preparedQuery(
                    """
                    INSERT INTO catalog.berry_ability_relation (berry_id, ability_code, ability_name, relation_kind, note, sorting_order, enabled)
                    VALUES ($1, $2, $3, $4, $5, $6, $7)
                    RETURNING id
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addValue(berryId.value)
                        .addString(draft.abilityCode)
                        .addString(draft.abilityName)
                        .addString(draft.relationKind)
                        .addValue(draft.note)
                        .addInteger(draft.sortingOrder)
                        .addBoolean(draft.enabled),
                ).awaitSuspending()
                    .first()
            requireAbilityRelation(connection, row.getUUID("id"))
        }

    suspend fun deleteAbilityRelation(
        berryId: BerryDefinitionId,
        relationId: BerryAbilityRelationId,
    ) {
        val deleted =
            pool.preparedQuery("DELETE FROM catalog.berry_ability_relation WHERE id = $1 AND berry_id = $2")
                .execute(Tuple.of(relationId.value, berryId.value))
                .awaitSuspending()
                .rowCount()
        if (deleted == 0) throw CatalogNotFound("berry_ability_relation", relationId.value.toString())
    }

    private suspend fun requireBerryDefinition(connection: io.vertx.mutiny.sqlclient.SqlConnection, id: java.util.UUID): BerryDefinition =
        connection.preparedQuery("$BERRY_DEFINITION_SELECT_SQL WHERE bd.id = $1")
            .execute(Tuple.of(id))
            .awaitSuspending()
            .first()
            .let(::mapBerryDefinition)

    private suspend fun requireBattleEffect(connection: io.vertx.mutiny.sqlclient.SqlConnection, berryId: java.util.UUID): BerryBattleEffect =
        connection.preparedQuery("$BERRY_BATTLE_EFFECT_SELECT_SQL WHERE berry_id = $1")
            .execute(Tuple.of(berryId))
            .awaitSuspending()
            .first()
            .let(::mapBerryBattleEffect)

    private suspend fun requireCultivationProfile(connection: io.vertx.mutiny.sqlclient.SqlConnection, berryId: java.util.UUID): BerryCultivationProfile =
        connection.preparedQuery("$BERRY_CULTIVATION_SELECT_SQL WHERE berry_id = $1")
            .execute(Tuple.of(berryId))
            .awaitSuspending()
            .first()
            .let(::mapBerryCultivationProfile)

    private suspend fun requireAcquisition(connection: io.vertx.mutiny.sqlclient.SqlConnection, id: java.util.UUID): BerryAcquisition =
        connection.preparedQuery("$BERRY_ACQUISITION_SELECT_SQL WHERE id = $1")
            .execute(Tuple.of(id))
            .awaitSuspending()
            .first()
            .let(::mapBerryAcquisition)

    private suspend fun requireMoveRelation(connection: io.vertx.mutiny.sqlclient.SqlConnection, id: java.util.UUID): BerryMoveRelation =
        connection.preparedQuery("$BERRY_MOVE_RELATION_SELECT_SQL WHERE id = $1")
            .execute(Tuple.of(id))
            .awaitSuspending()
            .first()
            .let(::mapBerryMoveRelation)

    private suspend fun requireAbilityRelation(connection: io.vertx.mutiny.sqlclient.SqlConnection, id: java.util.UUID): BerryAbilityRelation =
        connection.preparedQuery("$BERRY_ABILITY_RELATION_SELECT_SQL WHERE id = $1")
            .execute(Tuple.of(id))
            .awaitSuspending()
            .first()
            .let(::mapBerryAbilityRelation)
}
