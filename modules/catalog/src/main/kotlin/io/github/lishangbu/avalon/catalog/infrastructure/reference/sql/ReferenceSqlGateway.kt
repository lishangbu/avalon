package io.github.lishangbu.avalon.catalog.infrastructure.reference.sql

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
 * 负责 nature / item / ability / growth_rate 这些参考数据切片的 SQL 读写。
 */
internal class ReferenceSqlGateway(
    private val pool: Pool,
) {
    suspend fun listNatures(): List<Nature> =
        pool.query("$NATURE_SELECT_SQL ORDER BY n.sorting_order, n.id")
            .execute()
            .awaitSuspending()
            .toRows()
            .map(::mapNature)

    suspend fun findNature(id: NatureId): Nature? =
        pool.preparedQuery("$NATURE_SELECT_SQL WHERE n.id = $1")
            .execute(Tuple.of(id.value))
            .awaitSuspending()
            .firstOrNull()
            ?.let(::mapNature)

    suspend fun createNature(draft: NatureDraft): Nature =
        pool.withSuspendingTransaction { connection ->
            val row =
                connection.preparedQuery(
                    """
                    INSERT INTO catalog.nature (
                        code,
                        name,
                        description,
                        increased_stat_code,
                        decreased_stat_code,
                        sorting_order,
                        enabled
                    )
                    VALUES ($1, $2, $3, $4, $5, $6, $7)
                    RETURNING id
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addString(draft.code)
                        .addString(draft.name)
                        .addValue(draft.description)
                        .addValue(draft.increasedStatCode?.name)
                        .addValue(draft.decreasedStatCode?.name)
                        .addInteger(draft.sortingOrder)
                        .addBoolean(draft.enabled),
                ).awaitSuspending()
                    .first()

            requireNature(connection, row.getUUID("id"))
        }

    suspend fun updateNature(
        id: NatureId,
        draft: NatureDraft,
    ): Nature =
        pool.withSuspendingTransaction { connection ->
            val updatedCount =
                connection.preparedQuery(
                    """
                    UPDATE catalog.nature
                    SET code = $1,
                        name = $2,
                        description = $3,
                        increased_stat_code = $4,
                        decreased_stat_code = $5,
                        sorting_order = $6,
                        enabled = $7,
                        version = version + 1
                    WHERE id = $8
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addString(draft.code)
                        .addString(draft.name)
                        .addValue(draft.description)
                        .addValue(draft.increasedStatCode?.name)
                        .addValue(draft.decreasedStatCode?.name)
                        .addInteger(draft.sortingOrder)
                        .addBoolean(draft.enabled)
                        .addValue(id.value),
                ).awaitSuspending()
                    .rowCount()

            if (updatedCount == 0) {
                throw CatalogNotFound("nature", id.value.toString())
            }

            requireNature(connection, id.value)
        }

    suspend fun deleteNature(id: NatureId) {
        val deletedCount =
            pool.preparedQuery("DELETE FROM catalog.nature WHERE id = $1")
                .execute(Tuple.of(id.value))
                .awaitSuspending()
                .rowCount()

        if (deletedCount == 0) {
            throw CatalogNotFound("nature", id.value.toString())
        }
    }

    suspend fun listItems(): List<Item> =
        pool.query("$ITEM_SELECT_SQL ORDER BY i.sorting_order, i.id")
            .execute()
            .awaitSuspending()
            .toRows()
            .map(::mapItem)

    suspend fun findItem(id: ItemId): Item? =
        pool.preparedQuery("$ITEM_SELECT_SQL WHERE i.id = $1")
            .execute(Tuple.of(id.value))
            .awaitSuspending()
            .firstOrNull()
            ?.let(::mapItem)

    suspend fun createItem(draft: ItemDraft): Item =
        pool.withSuspendingTransaction { connection ->
            val row =
                connection.preparedQuery(
                    """
                    INSERT INTO catalog.item (
                        code,
                        name,
                        category_code,
                        description,
                        icon,
                        max_stack_size,
                        consumable,
                        sorting_order,
                        enabled
                    )
                    VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
                    RETURNING id
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addString(draft.code)
                        .addString(draft.name)
                        .addString(draft.categoryCode)
                        .addValue(draft.description)
                        .addValue(draft.icon)
                        .addInteger(draft.maxStackSize)
                        .addBoolean(draft.consumable)
                        .addInteger(draft.sortingOrder)
                        .addBoolean(draft.enabled),
                ).awaitSuspending()
                    .first()

            requireItem(connection, row.getUUID("id"))
        }

    suspend fun updateItem(
        id: ItemId,
        draft: ItemDraft,
    ): Item =
        pool.withSuspendingTransaction { connection ->
            val updatedCount =
                connection.preparedQuery(
                    """
                    UPDATE catalog.item
                    SET code = $1,
                        name = $2,
                        category_code = $3,
                        description = $4,
                        icon = $5,
                        max_stack_size = $6,
                        consumable = $7,
                        sorting_order = $8,
                        enabled = $9,
                        version = version + 1
                    WHERE id = $10
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addString(draft.code)
                        .addString(draft.name)
                        .addString(draft.categoryCode)
                        .addValue(draft.description)
                        .addValue(draft.icon)
                        .addInteger(draft.maxStackSize)
                        .addBoolean(draft.consumable)
                        .addInteger(draft.sortingOrder)
                        .addBoolean(draft.enabled)
                        .addValue(id.value),
                ).awaitSuspending()
                    .rowCount()

            if (updatedCount == 0) {
                throw CatalogNotFound("item", id.value.toString())
            }

            requireItem(connection, id.value)
        }

    suspend fun deleteItem(id: ItemId) {
        val deletedCount =
            pool.preparedQuery("DELETE FROM catalog.item WHERE id = $1")
                .execute(Tuple.of(id.value))
                .awaitSuspending()
                .rowCount()

        if (deletedCount == 0) {
            throw CatalogNotFound("item", id.value.toString())
        }
    }

    suspend fun listAbilities(): List<Ability> =
        pool.query("$ABILITY_SELECT_SQL ORDER BY a.sorting_order, a.id")
            .execute()
            .awaitSuspending()
            .toRows()
            .map(::mapAbility)

    suspend fun findAbility(id: AbilityId): Ability? =
        pool.preparedQuery("$ABILITY_SELECT_SQL WHERE a.id = $1")
            .execute(Tuple.of(id.value))
            .awaitSuspending()
            .firstOrNull()
            ?.let(::mapAbility)

    suspend fun createAbility(draft: AbilityDraft): Ability =
        pool.withSuspendingTransaction { connection ->
            val row =
                connection.preparedQuery(
                    """
                    INSERT INTO catalog.ability (
                        code,
                        name,
                        description,
                        icon,
                        sorting_order,
                        enabled
                    )
                    VALUES ($1, $2, $3, $4, $5, $6)
                    RETURNING id
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addString(draft.code)
                        .addString(draft.name)
                        .addValue(draft.description)
                        .addValue(draft.icon)
                        .addInteger(draft.sortingOrder)
                        .addBoolean(draft.enabled),
                ).awaitSuspending()
                    .first()

            requireAbility(connection, row.getUUID("id"))
        }

    suspend fun updateAbility(
        id: AbilityId,
        draft: AbilityDraft,
    ): Ability =
        pool.withSuspendingTransaction { connection ->
            val updatedCount =
                connection.preparedQuery(
                    """
                    UPDATE catalog.ability
                    SET code = $1,
                        name = $2,
                        description = $3,
                        icon = $4,
                        sorting_order = $5,
                        enabled = $6,
                        version = version + 1
                    WHERE id = $7
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addString(draft.code)
                        .addString(draft.name)
                        .addValue(draft.description)
                        .addValue(draft.icon)
                        .addInteger(draft.sortingOrder)
                        .addBoolean(draft.enabled)
                        .addValue(id.value),
                ).awaitSuspending()
                    .rowCount()

            if (updatedCount == 0) {
                throw CatalogNotFound("ability", id.value.toString())
            }

            requireAbility(connection, id.value)
        }

    suspend fun deleteAbility(id: AbilityId) {
        val deletedCount =
            pool.preparedQuery("DELETE FROM catalog.ability WHERE id = $1")
                .execute(Tuple.of(id.value))
                .awaitSuspending()
                .rowCount()

        if (deletedCount == 0) {
            throw CatalogNotFound("ability", id.value.toString())
        }
    }

    suspend fun listGrowthRates(): List<GrowthRate> =
        pool.query("$GROWTH_RATE_SELECT_SQL ORDER BY gr.sorting_order, gr.id")
            .execute()
            .awaitSuspending()
            .toRows()
            .map(::mapGrowthRate)

    suspend fun findGrowthRate(id: GrowthRateId): GrowthRate? =
        pool.preparedQuery("$GROWTH_RATE_SELECT_SQL WHERE gr.id = $1")
            .execute(Tuple.of(id.value))
            .awaitSuspending()
            .firstOrNull()
            ?.let(::mapGrowthRate)

    suspend fun createGrowthRate(draft: GrowthRateDraft): GrowthRate =
        pool.withSuspendingTransaction { connection ->
            val row =
                connection.preparedQuery(
                    """
                    INSERT INTO catalog.growth_rate (
                        code,
                        name,
                        formula_code,
                        description,
                        sorting_order,
                        enabled
                    )
                    VALUES ($1, $2, $3, $4, $5, $6)
                    RETURNING id
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addString(draft.code)
                        .addString(draft.name)
                        .addString(draft.formulaCode.name)
                        .addValue(draft.description)
                        .addInteger(draft.sortingOrder)
                        .addBoolean(draft.enabled),
                ).awaitSuspending()
                    .first()

            requireGrowthRate(connection, row.getUUID("id"))
        }

    suspend fun updateGrowthRate(
        id: GrowthRateId,
        draft: GrowthRateDraft,
    ): GrowthRate =
        pool.withSuspendingTransaction { connection ->
            val updatedCount =
                connection.preparedQuery(
                    """
                    UPDATE catalog.growth_rate
                    SET code = $1,
                        name = $2,
                        formula_code = $3,
                        description = $4,
                        sorting_order = $5,
                        enabled = $6,
                        version = version + 1
                    WHERE id = $7
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addString(draft.code)
                        .addString(draft.name)
                        .addString(draft.formulaCode.name)
                        .addValue(draft.description)
                        .addInteger(draft.sortingOrder)
                        .addBoolean(draft.enabled)
                        .addValue(id.value),
                ).awaitSuspending()
                    .rowCount()

            if (updatedCount == 0) {
                throw CatalogNotFound("growth_rate", id.value.toString())
            }

            requireGrowthRate(connection, id.value)
        }

    suspend fun deleteGrowthRate(id: GrowthRateId) {
        val deletedCount =
            pool.preparedQuery("DELETE FROM catalog.growth_rate WHERE id = $1")
                .execute(Tuple.of(id.value))
                .awaitSuspending()
                .rowCount()

        if (deletedCount == 0) {
            throw CatalogNotFound("growth_rate", id.value.toString())
        }
    }
}