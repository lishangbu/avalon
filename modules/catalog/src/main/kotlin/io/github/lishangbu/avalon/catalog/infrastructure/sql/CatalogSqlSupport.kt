package io.github.lishangbu.avalon.catalog.infrastructure.sql

import io.github.lishangbu.avalon.catalog.domain.*
import io.github.lishangbu.avalon.shared.infra.mutiny.awaitSuspending
import io.github.lishangbu.avalon.shared.infra.sql.first
import io.vertx.mutiny.sqlclient.SqlConnection
import io.vertx.mutiny.sqlclient.Tuple
import java.util.UUID

internal const val LEVEL_UP_MOVE_LEARN_METHOD_CODE = "LEVEL-UP"

/**
 * Catalog 的 SQL 支撑函数只服务本上下文。
 *
 * shared-infra 只提供事务模板、RowSet 扩展和异常包装壳；这里保留的仍是
 * Catalog 自己的数据库错误翻译、读回校验和 canonical query 组合逻辑，
 * 避免把上下文语义抬进共享层。
 */
internal fun mapCatalogDatabaseError(exception: Throwable): Throwable {
    val message = exception.message ?: "Catalog data access failure"
    return when {
        message.contains("duplicate key", ignoreCase = true) ->
            CatalogConflict("The submitted catalog data conflicts with an existing record.")

        message.contains("foreign key", ignoreCase = true) ->
            CatalogConflict("Referenced catalog data does not exist, or the record is still referenced by other catalog data.")

        message.contains("check constraint", ignoreCase = true) ->
            CatalogConflict("The submitted catalog data violates a database validation rule.")

        else -> exception
    }
}

internal suspend fun requireTypeDefinition(
    connection: SqlConnection,
    id: UUID,
): TypeDefinition =
    connection.preparedQuery("$TYPE_DEFINITION_SELECT_SQL WHERE td.id = $1")
        .execute(Tuple.of(id))
        .awaitSuspending()
        .first()
        .let(::mapTypeDefinition)

internal suspend fun requireTypeEffectiveness(
    connection: SqlConnection,
    id: UUID,
): TypeEffectiveness =
    connection.preparedQuery("$TYPE_EFFECTIVENESS_SELECT_SQL WHERE te.id = $1")
        .execute(Tuple.of(id))
        .awaitSuspending()
        .first()
        .let(::mapTypeEffectiveness)

internal suspend fun requireNature(
    connection: SqlConnection,
    id: UUID,
): Nature =
    connection.preparedQuery("$NATURE_SELECT_SQL WHERE n.id = $1")
        .execute(Tuple.of(id))
        .awaitSuspending()
        .first()
        .let(::mapNature)

internal suspend fun requireItem(
    connection: SqlConnection,
    id: UUID,
): Item =
    connection.preparedQuery("$ITEM_SELECT_SQL WHERE i.id = $1")
        .execute(Tuple.of(id))
        .awaitSuspending()
        .first()
        .let(::mapItem)

internal suspend fun requireAbility(
    connection: SqlConnection,
    id: UUID,
): Ability =
    connection.preparedQuery("$ABILITY_SELECT_SQL WHERE a.id = $1")
        .execute(Tuple.of(id))
        .awaitSuspending()
        .first()
        .let(::mapAbility)

internal suspend fun requireMove(
    connection: SqlConnection,
    id: UUID,
): Move =
    connection.preparedQuery("$MOVE_SELECT_SQL WHERE m.id = $1")
        .execute(Tuple.of(id))
        .awaitSuspending()
        .first()
        .let(::mapMove)

internal suspend fun requireMoveTarget(
    connection: SqlConnection,
    id: UUID,
): MoveTarget =
    connection.preparedQuery("$MOVE_TARGET_SELECT_SQL WHERE mt.id = $1")
        .execute(Tuple.of(id))
        .awaitSuspending()
        .first()
        .let(::mapMoveTarget)

internal suspend fun requireMoveCategory(
    connection: SqlConnection,
    id: UUID,
): MoveCategory =
    connection.preparedQuery("$MOVE_CATEGORY_SELECT_SQL WHERE mc.id = $1")
        .execute(Tuple.of(id))
        .awaitSuspending()
        .first()
        .let(::mapMoveCategory)

internal suspend fun requireMoveAilment(
    connection: SqlConnection,
    id: UUID,
): MoveAilment =
    connection.preparedQuery("$MOVE_AILMENT_SELECT_SQL WHERE ma.id = $1")
        .execute(Tuple.of(id))
        .awaitSuspending()
        .first()
        .let(::mapMoveAilment)

internal suspend fun requireMoveLearnMethod(
    connection: SqlConnection,
    id: UUID,
): MoveLearnMethod =
    connection.preparedQuery("$MOVE_LEARN_METHOD_SELECT_SQL WHERE mlm.id = $1")
        .execute(Tuple.of(id))
        .awaitSuspending()
        .first()
        .let(::mapMoveLearnMethod)

internal suspend fun requireGrowthRate(
    connection: SqlConnection,
    id: UUID,
): GrowthRate =
    connection.preparedQuery("$GROWTH_RATE_SELECT_SQL WHERE gr.id = $1")
        .execute(Tuple.of(id))
        .awaitSuspending()
        .first()
        .let(::mapGrowthRate)

internal suspend fun requireSpeciesEvolution(
    connection: SqlConnection,
    id: UUID,
): SpeciesEvolution =
    connection.preparedQuery("$SPECIES_EVOLUTION_SELECT_SQL WHERE se.id = $1")
        .execute(Tuple.of(id))
        .awaitSuspending()
        .first()
        .let(::mapSpeciesEvolution)

internal suspend fun requireSpeciesAbility(
    connection: SqlConnection,
    id: UUID,
): SpeciesAbility =
    connection.preparedQuery("$SPECIES_ABILITY_SELECT_SQL WHERE sa.id = $1")
        .execute(Tuple.of(id))
        .awaitSuspending()
        .first()
        .let(::mapSpeciesAbility)

internal suspend fun requireSpeciesMoveLearnset(
    connection: SqlConnection,
    id: UUID,
): SpeciesMoveLearnset =
    connection.preparedQuery("$SPECIES_MOVE_LEARNSET_SELECT_SQL WHERE sml.id = $1")
        .execute(Tuple.of(id))
        .awaitSuspending()
        .first()
        .let(::mapSpeciesMoveLearnset)

// 先让数据库兜住外键和唯一约束，再在同一事务内校验学习方法与 level 的组合规则。
internal suspend fun requireValidatedSpeciesMoveLearnset(
    connection: SqlConnection,
    id: UUID,
): SpeciesMoveLearnset {
    val learnset = requireSpeciesMoveLearnset(connection, id)
    validateSpeciesMoveLearnsetLevelRule(learnset)
    return learnset
}

internal fun validateSpeciesMoveLearnsetLevelRule(learnset: SpeciesMoveLearnset) {
    when {
        learnset.learnMethod.code == LEVEL_UP_MOVE_LEARN_METHOD_CODE && learnset.level == null ->
            throw CatalogBadRequest("level is required when learn method code is LEVEL-UP.")

        learnset.learnMethod.code != LEVEL_UP_MOVE_LEARN_METHOD_CODE && learnset.level != null ->
            throw CatalogBadRequest("level must be null unless learn method code is LEVEL-UP.")
    }
}

internal suspend fun requireSpecies(
    connection: SqlConnection,
    id: UUID,
): Species =
    connection.preparedQuery("$SPECIES_SELECT_SQL WHERE cs.id = $1")
        .execute(Tuple.of(id))
        .awaitSuspending()
        .first()
        .let(::mapSpecies)

