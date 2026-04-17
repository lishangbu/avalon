package io.github.lishangbu.avalon.catalog.infrastructure.type.sql

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
 * 负责 Catalog 中 type_definition / type_effectiveness 两个切片的 SQL 读写。
 */
internal class TypeSqlGateway(
    private val pool: Pool,
) {
    suspend fun listTypeDefinitions(): List<TypeDefinition> =
        pool.query("$TYPE_DEFINITION_SELECT_SQL ORDER BY td.sorting_order, td.id")
            .execute()
            .awaitSuspending()
            .toRows()
            .map(::mapTypeDefinition)

    suspend fun findTypeDefinition(id: TypeDefinitionId): TypeDefinition? =
        pool.preparedQuery("$TYPE_DEFINITION_SELECT_SQL WHERE td.id = $1")
            .execute(Tuple.of(id.value))
            .awaitSuspending()
            .firstOrNull()
            ?.let(::mapTypeDefinition)

    suspend fun createTypeDefinition(draft: TypeDefinitionDraft): TypeDefinition =
        pool.withSuspendingTransaction { connection ->
            val row =
                connection.preparedQuery(
                    """
                    INSERT INTO catalog.type_definition (
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

            requireTypeDefinition(connection, row.getUUID("id"))
        }

    suspend fun updateTypeDefinition(
        id: TypeDefinitionId,
        draft: TypeDefinitionDraft,
    ): TypeDefinition =
        pool.withSuspendingTransaction { connection ->
            val updatedCount =
                connection.preparedQuery(
                    """
                    UPDATE catalog.type_definition
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
                throw CatalogNotFound("type_definition", id.value.toString())
            }

            requireTypeDefinition(connection, id.value)
        }

    suspend fun deleteTypeDefinition(id: TypeDefinitionId) {
        val deletedCount =
            pool.preparedQuery("DELETE FROM catalog.type_definition WHERE id = $1")
                .execute(Tuple.of(id.value))
                .awaitSuspending()
                .rowCount()

        if (deletedCount == 0) {
            throw CatalogNotFound("type_definition", id.value.toString())
        }
    }

    suspend fun replaceTypeEffectiveness(entries: List<TypeEffectivenessDraft>) {
        pool.withSuspendingTransaction { connection ->
            connection.query("DELETE FROM catalog.type_effectiveness")
                .execute()
                .awaitSuspending()

            if (entries.isEmpty()) {
                return@withSuspendingTransaction
            }

            connection.preparedQuery(
                """
                INSERT INTO catalog.type_effectiveness (
                    attacking_type_id,
                    defending_type_id,
                    multiplier
                )
                VALUES ($1, $2, $3)
                """.trimIndent(),
            ).executeBatch(
                entries.map { draft ->
                    Tuple.tuple()
                        .addValue(draft.attackingTypeId.value)
                        .addValue(draft.defendingTypeId.value)
                        .addBigDecimal(draft.multiplier)
                },
            ).awaitSuspending()
        }
    }

    suspend fun listTypeEffectiveness(): List<TypeEffectiveness> =
        pool.query("$TYPE_EFFECTIVENESS_SELECT_SQL ORDER BY te.id")
            .execute()
            .awaitSuspending()
            .toRows()
            .map(::mapTypeEffectiveness)

    suspend fun findTypeEffectiveness(id: TypeEffectivenessId): TypeEffectiveness? =
        pool.preparedQuery("$TYPE_EFFECTIVENESS_SELECT_SQL WHERE te.id = $1")
            .execute(Tuple.of(id.value))
            .awaitSuspending()
            .firstOrNull()
            ?.let(::mapTypeEffectiveness)

    suspend fun createTypeEffectiveness(draft: TypeEffectivenessDraft): TypeEffectiveness =
        pool.withSuspendingTransaction { connection ->
            val row =
                connection.preparedQuery(
                    """
                    INSERT INTO catalog.type_effectiveness (
                        attacking_type_id,
                        defending_type_id,
                        multiplier
                    )
                    VALUES ($1, $2, $3)
                    RETURNING id
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addValue(draft.attackingTypeId.value)
                        .addValue(draft.defendingTypeId.value)
                        .addBigDecimal(draft.multiplier),
                ).awaitSuspending()
                    .first()

            requireTypeEffectiveness(connection, row.getUUID("id"))
        }

    suspend fun updateTypeEffectiveness(
        id: TypeEffectivenessId,
        draft: TypeEffectivenessDraft,
    ): TypeEffectiveness =
        pool.withSuspendingTransaction { connection ->
            val updatedCount =
                connection.preparedQuery(
                    """
                    UPDATE catalog.type_effectiveness
                    SET attacking_type_id = $1,
                        defending_type_id = $2,
                        multiplier = $3,
                        version = version + 1
                    WHERE id = $4
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addValue(draft.attackingTypeId.value)
                        .addValue(draft.defendingTypeId.value)
                        .addBigDecimal(draft.multiplier)
                        .addValue(id.value),
                ).awaitSuspending()
                    .rowCount()

            if (updatedCount == 0) {
                throw CatalogNotFound("type_effectiveness", id.value.toString())
            }

            requireTypeEffectiveness(connection, id.value)
        }

    suspend fun deleteTypeEffectiveness(id: TypeEffectivenessId) {
        val deletedCount =
            pool.preparedQuery("DELETE FROM catalog.type_effectiveness WHERE id = $1")
                .execute(Tuple.of(id.value))
                .awaitSuspending()
                .rowCount()

        if (deletedCount == 0) {
            throw CatalogNotFound("type_effectiveness", id.value.toString())
        }
    }
}
