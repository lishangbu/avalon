package io.github.lishangbu.avalon.catalog.infrastructure.move.sql

import io.github.lishangbu.avalon.catalog.domain.*
import io.github.lishangbu.avalon.catalog.infrastructure.sql.*
import io.github.lishangbu.avalon.shared.infra.mutiny.awaitSuspending
import io.github.lishangbu.avalon.shared.infra.sql.first
import io.github.lishangbu.avalon.shared.infra.sql.firstOrNull
import io.github.lishangbu.avalon.shared.infra.sql.toRows
import io.github.lishangbu.avalon.shared.infra.sql.withSuspendingTransaction
import io.vertx.mutiny.sqlclient.Pool
import io.vertx.mutiny.sqlclient.Tuple

/**
 * 负责 move 及其参考切片 move_category / move_ailment / move_target / move_learn_method 的 SQL 读写。
 */
internal class MoveSqlGateway(
    private val pool: Pool,
) {
    suspend fun listMoves(): List<Move> =
        pool.query("$MOVE_SELECT_SQL ORDER BY m.sorting_order, m.id")
            .execute()
            .awaitSuspending()
            .toRows()
            .map(::mapMove)

    suspend fun findMove(id: MoveId): Move? =
        pool.preparedQuery("$MOVE_SELECT_SQL WHERE m.id = $1")
            .execute(Tuple.of(id.value))
            .awaitSuspending()
            .firstOrNull()
            ?.let(::mapMove)

    suspend fun createMove(draft: MoveDraft): Move =
        pool.withSuspendingTransaction { connection ->
            val row =
                connection.preparedQuery(
                    """
                    INSERT INTO catalog.move (
                        code,
                        name,
                        type_definition_id,
                        category_code,
                        move_category_id,
                        move_ailment_id,
                        move_target_id,
                        description,
                        effect_chance,
                        power,
                        accuracy,
                        power_points,
                        priority,
                        text,
                        short_effect,
                        effect,
                        sorting_order,
                        enabled
                    )
                    VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17, $18)
                    RETURNING id
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addString(draft.code)
                        .addString(draft.name)
                        .addValue(draft.typeDefinitionId.value)
                        .addString(draft.categoryCode.name)
                        .addValue(draft.moveCategoryId?.value)
                        .addValue(draft.moveAilmentId?.value)
                        .addValue(draft.moveTargetId?.value)
                        .addValue(draft.description)
                        .addValue(draft.effectChance)
                        .addValue(draft.power)
                        .addValue(draft.accuracy)
                        .addInteger(draft.powerPoints)
                        .addInteger(draft.priority)
                        .addValue(draft.text)
                        .addValue(draft.shortEffect)
                        .addValue(draft.effect)
                        .addInteger(draft.sortingOrder)
                        .addBoolean(draft.enabled),
                ).awaitSuspending()
                    .first()

            requireMove(connection, row.getUUID("id"))
        }

    suspend fun updateMove(
        id: MoveId,
        draft: MoveDraft,
    ): Move =
        pool.withSuspendingTransaction { connection ->
            val updatedCount =
                connection.preparedQuery(
                    """
                    UPDATE catalog.move
                    SET code = $1,
                        name = $2,
                        type_definition_id = $3,
                        category_code = $4,
                        move_category_id = $5,
                        move_ailment_id = $6,
                        move_target_id = $7,
                        description = $8,
                        effect_chance = $9,
                        power = $10,
                        accuracy = $11,
                        power_points = $12,
                        priority = $13,
                        text = $14,
                        short_effect = $15,
                        effect = $16,
                        sorting_order = $17,
                        enabled = $18,
                        version = version + 1
                    WHERE id = $19
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addString(draft.code)
                        .addString(draft.name)
                        .addValue(draft.typeDefinitionId.value)
                        .addString(draft.categoryCode.name)
                        .addValue(draft.moveCategoryId?.value)
                        .addValue(draft.moveAilmentId?.value)
                        .addValue(draft.moveTargetId?.value)
                        .addValue(draft.description)
                        .addValue(draft.effectChance)
                        .addValue(draft.power)
                        .addValue(draft.accuracy)
                        .addInteger(draft.powerPoints)
                        .addInteger(draft.priority)
                        .addValue(draft.text)
                        .addValue(draft.shortEffect)
                        .addValue(draft.effect)
                        .addInteger(draft.sortingOrder)
                        .addBoolean(draft.enabled)
                        .addValue(id.value),
                ).awaitSuspending()
                    .rowCount()

            if (updatedCount == 0) {
                throw CatalogNotFound("move", id.value.toString())
            }

            requireMove(connection, id.value)
        }

    suspend fun deleteMove(id: MoveId) {
        val deletedCount =
            pool.preparedQuery("DELETE FROM catalog.move WHERE id = $1")
                .execute(Tuple.of(id.value))
                .awaitSuspending()
                .rowCount()

        if (deletedCount == 0) {
            throw CatalogNotFound("move", id.value.toString())
        }
    }

    suspend fun listMoveCategories(): List<MoveCategory> =
        pool.query("$MOVE_CATEGORY_SELECT_SQL ORDER BY mc.sorting_order, mc.id")
            .execute()
            .awaitSuspending()
            .toRows()
            .map(::mapMoveCategory)

    suspend fun findMoveCategory(id: MoveCategoryId): MoveCategory? =
        pool.preparedQuery("$MOVE_CATEGORY_SELECT_SQL WHERE mc.id = $1")
            .execute(Tuple.of(id.value))
            .awaitSuspending()
            .firstOrNull()
            ?.let(::mapMoveCategory)

    suspend fun createMoveCategory(draft: MoveCategoryDraft): MoveCategory =
        pool.withSuspendingTransaction { connection ->
            val row =
                connection.preparedQuery(
                    """
                    INSERT INTO catalog.move_category (
                        code,
                        name,
                        description,
                        sorting_order,
                        enabled
                    )
                    VALUES ($1, $2, $3, $4, $5)
                    RETURNING id
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addString(draft.code)
                        .addString(draft.name)
                        .addValue(draft.description)
                        .addInteger(draft.sortingOrder)
                        .addBoolean(draft.enabled),
                ).awaitSuspending()
                    .first()

            requireMoveCategory(connection, row.getUUID("id"))
        }

    suspend fun updateMoveCategory(
        id: MoveCategoryId,
        draft: MoveCategoryDraft,
    ): MoveCategory =
        pool.withSuspendingTransaction { connection ->
            val updatedCount =
                connection.preparedQuery(
                    """
                    UPDATE catalog.move_category
                    SET code = $1,
                        name = $2,
                        description = $3,
                        sorting_order = $4,
                        enabled = $5,
                        version = version + 1
                    WHERE id = $6
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addString(draft.code)
                        .addString(draft.name)
                        .addValue(draft.description)
                        .addInteger(draft.sortingOrder)
                        .addBoolean(draft.enabled)
                        .addValue(id.value),
                ).awaitSuspending()
                    .rowCount()

            if (updatedCount == 0) {
                throw CatalogNotFound("move_category", id.value.toString())
            }

            requireMoveCategory(connection, id.value)
        }

    suspend fun deleteMoveCategory(id: MoveCategoryId) {
        val deletedCount =
            pool.preparedQuery("DELETE FROM catalog.move_category WHERE id = $1")
                .execute(Tuple.of(id.value))
                .awaitSuspending()
                .rowCount()

        if (deletedCount == 0) {
            throw CatalogNotFound("move_category", id.value.toString())
        }
    }

    suspend fun listMoveAilments(): List<MoveAilment> =
        pool.query("$MOVE_AILMENT_SELECT_SQL ORDER BY ma.sorting_order, ma.id")
            .execute()
            .awaitSuspending()
            .toRows()
            .map(::mapMoveAilment)

    suspend fun findMoveAilment(id: MoveAilmentId): MoveAilment? =
        pool.preparedQuery("$MOVE_AILMENT_SELECT_SQL WHERE ma.id = $1")
            .execute(Tuple.of(id.value))
            .awaitSuspending()
            .firstOrNull()
            ?.let(::mapMoveAilment)

    suspend fun createMoveAilment(draft: MoveAilmentDraft): MoveAilment =
        pool.withSuspendingTransaction { connection ->
            val row =
                connection.preparedQuery(
                    """
                    INSERT INTO catalog.move_ailment (
                        code,
                        name,
                        description,
                        sorting_order,
                        enabled
                    )
                    VALUES ($1, $2, $3, $4, $5)
                    RETURNING id
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addString(draft.code)
                        .addString(draft.name)
                        .addValue(draft.description)
                        .addInteger(draft.sortingOrder)
                        .addBoolean(draft.enabled),
                ).awaitSuspending()
                    .first()

            requireMoveAilment(connection, row.getUUID("id"))
        }

    suspend fun updateMoveAilment(
        id: MoveAilmentId,
        draft: MoveAilmentDraft,
    ): MoveAilment =
        pool.withSuspendingTransaction { connection ->
            val updatedCount =
                connection.preparedQuery(
                    """
                    UPDATE catalog.move_ailment
                    SET code = $1,
                        name = $2,
                        description = $3,
                        sorting_order = $4,
                        enabled = $5,
                        version = version + 1
                    WHERE id = $6
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addString(draft.code)
                        .addString(draft.name)
                        .addValue(draft.description)
                        .addInteger(draft.sortingOrder)
                        .addBoolean(draft.enabled)
                        .addValue(id.value),
                ).awaitSuspending()
                    .rowCount()

            if (updatedCount == 0) {
                throw CatalogNotFound("move_ailment", id.value.toString())
            }

            requireMoveAilment(connection, id.value)
        }

    suspend fun deleteMoveAilment(id: MoveAilmentId) {
        val deletedCount =
            pool.preparedQuery("DELETE FROM catalog.move_ailment WHERE id = $1")
                .execute(Tuple.of(id.value))
                .awaitSuspending()
                .rowCount()

        if (deletedCount == 0) {
            throw CatalogNotFound("move_ailment", id.value.toString())
        }
    }

    suspend fun listMoveTargets(): List<MoveTarget> =
        pool.query("$MOVE_TARGET_SELECT_SQL ORDER BY mt.sorting_order, mt.id")
            .execute()
            .awaitSuspending()
            .toRows()
            .map(::mapMoveTarget)

    suspend fun findMoveTarget(id: MoveTargetId): MoveTarget? =
        pool.preparedQuery("$MOVE_TARGET_SELECT_SQL WHERE mt.id = $1")
            .execute(Tuple.of(id.value))
            .awaitSuspending()
            .firstOrNull()
            ?.let(::mapMoveTarget)

    suspend fun createMoveTarget(draft: MoveTargetDraft): MoveTarget =
        pool.withSuspendingTransaction { connection ->
            val row =
                connection.preparedQuery(
                    """
                    INSERT INTO catalog.move_target (
                        code,
                        name,
                        description,
                        sorting_order,
                        enabled
                    )
                    VALUES ($1, $2, $3, $4, $5)
                    RETURNING id
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addString(draft.code)
                        .addString(draft.name)
                        .addValue(draft.description)
                        .addInteger(draft.sortingOrder)
                        .addBoolean(draft.enabled),
                ).awaitSuspending()
                    .first()

            requireMoveTarget(connection, row.getUUID("id"))
        }

    suspend fun updateMoveTarget(
        id: MoveTargetId,
        draft: MoveTargetDraft,
    ): MoveTarget =
        pool.withSuspendingTransaction { connection ->
            val updatedCount =
                connection.preparedQuery(
                    """
                    UPDATE catalog.move_target
                    SET code = $1,
                        name = $2,
                        description = $3,
                        sorting_order = $4,
                        enabled = $5,
                        version = version + 1
                    WHERE id = $6
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addString(draft.code)
                        .addString(draft.name)
                        .addValue(draft.description)
                        .addInteger(draft.sortingOrder)
                        .addBoolean(draft.enabled)
                        .addValue(id.value),
                ).awaitSuspending()
                    .rowCount()

            if (updatedCount == 0) {
                throw CatalogNotFound("move_target", id.value.toString())
            }

            requireMoveTarget(connection, id.value)
        }

    suspend fun deleteMoveTarget(id: MoveTargetId) {
        val deletedCount =
            pool.preparedQuery("DELETE FROM catalog.move_target WHERE id = $1")
                .execute(Tuple.of(id.value))
                .awaitSuspending()
                .rowCount()

        if (deletedCount == 0) {
            throw CatalogNotFound("move_target", id.value.toString())
        }
    }

    suspend fun listMoveLearnMethods(): List<MoveLearnMethod> =
        pool.query("$MOVE_LEARN_METHOD_SELECT_SQL ORDER BY mlm.sorting_order, mlm.id")
            .execute()
            .awaitSuspending()
            .toRows()
            .map(::mapMoveLearnMethod)

    suspend fun findMoveLearnMethod(id: MoveLearnMethodId): MoveLearnMethod? =
        pool.preparedQuery("$MOVE_LEARN_METHOD_SELECT_SQL WHERE mlm.id = $1")
            .execute(Tuple.of(id.value))
            .awaitSuspending()
            .firstOrNull()
            ?.let(::mapMoveLearnMethod)

    suspend fun createMoveLearnMethod(draft: MoveLearnMethodDraft): MoveLearnMethod =
        pool.withSuspendingTransaction { connection ->
            val row =
                connection.preparedQuery(
                    """
                    INSERT INTO catalog.move_learn_method (
                        code,
                        name,
                        description,
                        sorting_order,
                        enabled
                    )
                    VALUES ($1, $2, $3, $4, $5)
                    RETURNING id
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addString(draft.code)
                        .addString(draft.name)
                        .addValue(draft.description)
                        .addInteger(draft.sortingOrder)
                        .addBoolean(draft.enabled),
                ).awaitSuspending()
                    .first()

            requireMoveLearnMethod(connection, row.getUUID("id"))
        }

    suspend fun updateMoveLearnMethod(
        id: MoveLearnMethodId,
        draft: MoveLearnMethodDraft,
    ): MoveLearnMethod =
        pool.withSuspendingTransaction { connection ->
            val updatedCount =
                connection.preparedQuery(
                    """
                    UPDATE catalog.move_learn_method
                    SET code = $1,
                        name = $2,
                        description = $3,
                        sorting_order = $4,
                        enabled = $5,
                        version = version + 1
                    WHERE id = $6
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addString(draft.code)
                        .addString(draft.name)
                        .addValue(draft.description)
                        .addInteger(draft.sortingOrder)
                        .addBoolean(draft.enabled)
                        .addValue(id.value),
                ).awaitSuspending()
                    .rowCount()

            if (updatedCount == 0) {
                throw CatalogNotFound("move_learn_method", id.value.toString())
            }

            requireMoveLearnMethod(connection, id.value)
        }

    suspend fun deleteMoveLearnMethod(id: MoveLearnMethodId) {
        val deletedCount =
            pool.preparedQuery("DELETE FROM catalog.move_learn_method WHERE id = $1")
                .execute(Tuple.of(id.value))
                .awaitSuspending()
                .rowCount()

        if (deletedCount == 0) {
            throw CatalogNotFound("move_learn_method", id.value.toString())
        }
    }
}