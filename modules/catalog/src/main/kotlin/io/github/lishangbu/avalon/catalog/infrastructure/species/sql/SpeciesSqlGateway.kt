package io.github.lishangbu.avalon.catalog.infrastructure.species.sql

import io.github.lishangbu.avalon.catalog.domain.*
import io.github.lishangbu.avalon.catalog.infrastructure.sql.*
import io.github.lishangbu.avalon.shared.application.query.Page
import io.github.lishangbu.avalon.shared.application.query.PageRequest
import io.github.lishangbu.avalon.shared.infra.mutiny.awaitSuspending
import io.github.lishangbu.avalon.shared.infra.sql.first
import io.github.lishangbu.avalon.shared.infra.sql.firstOrNull
import io.github.lishangbu.avalon.shared.infra.sql.pagination.addLimitThenOffset
import io.github.lishangbu.avalon.shared.infra.sql.pagination.PostgresPaginationDialect
import io.github.lishangbu.avalon.shared.infra.sql.toRows
import io.github.lishangbu.avalon.shared.infra.sql.withSuspendingTransaction
import io.vertx.mutiny.sqlclient.Pool
import io.vertx.mutiny.sqlclient.Tuple

/**
 * 负责 species 主体及其 evolution / ability / move_learnset 关系切片的 SQL 读写。
 */
internal class SpeciesSqlGateway(
    private val pool: Pool,
) {
    private val pageSpeciesSql =
        buildString {
            append(SPECIES_SELECT_SQL)
            appendLine()
            append(SPECIES_DEFAULT_ORDER_BY_SQL)
            appendLine()
            append(PostgresPaginationDialect.renderLimitOffsetClause(firstParameterIndex = 1))
        }

    suspend fun listSpeciesEvolutions(): List<SpeciesEvolution> =
        pool.query("$SPECIES_EVOLUTION_SELECT_SQL ORDER BY se.from_species_id, se.sorting_order, se.id")
            .execute()
            .awaitSuspending()
            .toRows()
            .map(::mapSpeciesEvolution)

    suspend fun findSpeciesEvolution(id: SpeciesEvolutionId): SpeciesEvolution? =
        pool.preparedQuery("$SPECIES_EVOLUTION_SELECT_SQL WHERE se.id = $1")
            .execute(Tuple.of(id.value))
            .awaitSuspending()
            .firstOrNull()
            ?.let(::mapSpeciesEvolution)

    suspend fun createSpeciesEvolution(draft: SpeciesEvolutionDraft): SpeciesEvolution =
        pool.withSuspendingTransaction { connection ->
            val row =
                connection.preparedQuery(
                    """
                    INSERT INTO catalog.species_evolution (
                        from_species_id,
                        to_species_id,
                        trigger_code,
                        min_level,
                        description,
                        sorting_order,
                        enabled
                    )
                    VALUES ($1, $2, $3, $4, $5, $6, $7)
                    RETURNING id
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addValue(draft.fromSpeciesId.value)
                        .addValue(draft.toSpeciesId.value)
                        .addString(draft.triggerCode.name)
                        .addValue(draft.minLevel)
                        .addValue(draft.description)
                        .addInteger(draft.sortingOrder)
                        .addBoolean(draft.enabled),
                ).awaitSuspending()
                    .first()

            requireSpeciesEvolution(connection, row.getUUID("id"))
        }

    suspend fun updateSpeciesEvolution(
        id: SpeciesEvolutionId,
        draft: SpeciesEvolutionDraft,
    ): SpeciesEvolution =
        pool.withSuspendingTransaction { connection ->
            val updatedCount =
                connection.preparedQuery(
                    """
                    UPDATE catalog.species_evolution
                    SET from_species_id = $1,
                        to_species_id = $2,
                        trigger_code = $3,
                        min_level = $4,
                        description = $5,
                        sorting_order = $6,
                        enabled = $7,
                        version = version + 1
                    WHERE id = $8
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addValue(draft.fromSpeciesId.value)
                        .addValue(draft.toSpeciesId.value)
                        .addString(draft.triggerCode.name)
                        .addValue(draft.minLevel)
                        .addValue(draft.description)
                        .addInteger(draft.sortingOrder)
                        .addBoolean(draft.enabled)
                        .addValue(id.value),
                ).awaitSuspending()
                    .rowCount()

            if (updatedCount == 0) {
                throw CatalogNotFound("species_evolution", id.value.toString())
            }

            requireSpeciesEvolution(connection, id.value)
        }

    suspend fun deleteSpeciesEvolution(id: SpeciesEvolutionId) {
        val deletedCount =
            pool.preparedQuery("DELETE FROM catalog.species_evolution WHERE id = $1")
                .execute(Tuple.of(id.value))
                .awaitSuspending()
                .rowCount()

        if (deletedCount == 0) {
            throw CatalogNotFound("species_evolution", id.value.toString())
        }
    }

    suspend fun listSpeciesAbilities(): List<SpeciesAbility> =
        pool.query("$SPECIES_ABILITY_SELECT_SQL ORDER BY sa.species_id, sa.sorting_order, sa.id")
            .execute()
            .awaitSuspending()
            .toRows()
            .map(::mapSpeciesAbility)

    suspend fun findSpeciesAbility(id: SpeciesAbilityId): SpeciesAbility? =
        pool.preparedQuery("$SPECIES_ABILITY_SELECT_SQL WHERE sa.id = $1")
            .execute(Tuple.of(id.value))
            .awaitSuspending()
            .firstOrNull()
            ?.let(::mapSpeciesAbility)

    suspend fun createSpeciesAbility(draft: SpeciesAbilityDraft): SpeciesAbility =
        pool.withSuspendingTransaction { connection ->
            val row =
                connection.preparedQuery(
                    """
                    INSERT INTO catalog.species_ability (
                        species_id,
                        ability_id,
                        slot_code,
                        sorting_order,
                        enabled
                    )
                    VALUES ($1, $2, $3, $4, $5)
                    RETURNING id
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addValue(draft.speciesId.value)
                        .addValue(draft.abilityId.value)
                        .addString(draft.slotCode.name)
                        .addInteger(draft.sortingOrder)
                        .addBoolean(draft.enabled),
                ).awaitSuspending()
                    .first()

            requireSpeciesAbility(connection, row.getUUID("id"))
        }

    suspend fun updateSpeciesAbility(
        id: SpeciesAbilityId,
        draft: SpeciesAbilityDraft,
    ): SpeciesAbility =
        pool.withSuspendingTransaction { connection ->
            val updatedCount =
                connection.preparedQuery(
                    """
                    UPDATE catalog.species_ability
                    SET species_id = $1,
                        ability_id = $2,
                        slot_code = $3,
                        sorting_order = $4,
                        enabled = $5,
                        version = version + 1
                    WHERE id = $6
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addValue(draft.speciesId.value)
                        .addValue(draft.abilityId.value)
                        .addString(draft.slotCode.name)
                        .addInteger(draft.sortingOrder)
                        .addBoolean(draft.enabled)
                        .addValue(id.value),
                ).awaitSuspending()
                    .rowCount()

            if (updatedCount == 0) {
                throw CatalogNotFound("species_ability", id.value.toString())
            }

            requireSpeciesAbility(connection, id.value)
        }

    suspend fun deleteSpeciesAbility(id: SpeciesAbilityId) {
        val deletedCount =
            pool.preparedQuery("DELETE FROM catalog.species_ability WHERE id = $1")
                .execute(Tuple.of(id.value))
                .awaitSuspending()
                .rowCount()

        if (deletedCount == 0) {
            throw CatalogNotFound("species_ability", id.value.toString())
        }
    }

    suspend fun listSpeciesMoveLearnsets(): List<SpeciesMoveLearnset> =
        pool.query("$SPECIES_MOVE_LEARNSET_SELECT_SQL ORDER BY sml.species_id, sml.sorting_order, sml.id")
            .execute()
            .awaitSuspending()
            .toRows()
            .map(::mapSpeciesMoveLearnset)

    suspend fun findSpeciesMoveLearnset(id: SpeciesMoveLearnsetId): SpeciesMoveLearnset? =
        pool.preparedQuery("$SPECIES_MOVE_LEARNSET_SELECT_SQL WHERE sml.id = $1")
            .execute(Tuple.of(id.value))
            .awaitSuspending()
            .firstOrNull()
            ?.let(::mapSpeciesMoveLearnset)

    suspend fun createSpeciesMoveLearnset(draft: SpeciesMoveLearnsetDraft): SpeciesMoveLearnset =
        pool.withSuspendingTransaction { connection ->
            val row =
                connection.preparedQuery(
                    """
                    INSERT INTO catalog.species_move_learnset (
                        species_id,
                        move_id,
                        learn_method_id,
                        level,
                        sorting_order,
                        enabled
                    )
                    VALUES ($1, $2, $3, $4, $5, $6)
                    RETURNING id
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addValue(draft.speciesId.value)
                        .addValue(draft.moveId.value)
                        .addValue(draft.learnMethodId.value)
                        .addInteger(draft.level)
                        .addInteger(draft.sortingOrder)
                        .addBoolean(draft.enabled),
                ).awaitSuspending()
                    .first()

            requireValidatedSpeciesMoveLearnset(connection, row.getUUID("id"))
        }

    suspend fun updateSpeciesMoveLearnset(
        id: SpeciesMoveLearnsetId,
        draft: SpeciesMoveLearnsetDraft,
    ): SpeciesMoveLearnset =
        pool.withSuspendingTransaction { connection ->
            val updatedCount =
                connection.preparedQuery(
                    """
                    UPDATE catalog.species_move_learnset
                    SET species_id = $1,
                        move_id = $2,
                        learn_method_id = $3,
                        level = $4,
                        sorting_order = $5,
                        enabled = $6,
                        version = version + 1
                    WHERE id = $7
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addValue(draft.speciesId.value)
                        .addValue(draft.moveId.value)
                        .addValue(draft.learnMethodId.value)
                        .addInteger(draft.level)
                        .addInteger(draft.sortingOrder)
                        .addBoolean(draft.enabled)
                        .addValue(id.value),
                ).awaitSuspending()
                    .rowCount()

            if (updatedCount == 0) {
                throw CatalogNotFound("species_move_learnset", id.value.toString())
            }

            requireValidatedSpeciesMoveLearnset(connection, id.value)
        }

    suspend fun deleteSpeciesMoveLearnset(id: SpeciesMoveLearnsetId) {
        val deletedCount =
            pool.preparedQuery("DELETE FROM catalog.species_move_learnset WHERE id = $1")
                .execute(Tuple.of(id.value))
                .awaitSuspending()
                .rowCount()

        if (deletedCount == 0) {
            throw CatalogNotFound("species_move_learnset", id.value.toString())
        }
    }

    suspend fun pageSpecies(request: PageRequest): Page<Species> {
        val totalItems =
            pool.query(SPECIES_COUNT_SQL)
                .execute()
                .awaitSuspending()
                .first()
                .getLong("total_items")

        val items =
            pool.preparedQuery(pageSpeciesSql)
                .execute(
                    Tuple.tuple().addLimitThenOffset(request),
                ).awaitSuspending()
                .toRows()
                .map(::mapSpecies)

        return Page.of(
            items = items,
            request = request,
            totalItems = totalItems,
        )
    }

    suspend fun findSpecies(id: SpeciesId): Species? =
        pool.preparedQuery("$SPECIES_SELECT_SQL WHERE cs.id = $1")
            .execute(Tuple.of(id.value))
            .awaitSuspending()
            .firstOrNull()
            ?.let(::mapSpecies)

    suspend fun createSpecies(draft: SpeciesDraft): Species =
        pool.withSuspendingTransaction { connection ->
            val row =
                connection.preparedQuery(
                    """
                    INSERT INTO catalog.creature_species (
                        code,
                        dex_number,
                        name,
                        description,
                        primary_type_id,
                        secondary_type_id,
                        growth_rate_id,
                        base_hp,
                        base_attack,
                        base_defense,
                        base_special_attack,
                        base_special_defense,
                        base_speed,
                        sorting_order,
                        enabled
                    )
                    VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15)
                    RETURNING id
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addString(draft.code)
                        .addInteger(draft.dexNumber)
                        .addString(draft.name)
                        .addValue(draft.description)
                        .addValue(draft.primaryTypeId.value)
                        .addValue(draft.secondaryTypeId?.value)
                        .addValue(draft.growthRateId?.value)
                        .addInteger(draft.baseStats.hp)
                        .addInteger(draft.baseStats.attack)
                        .addInteger(draft.baseStats.defense)
                        .addInteger(draft.baseStats.specialAttack)
                        .addInteger(draft.baseStats.specialDefense)
                        .addInteger(draft.baseStats.speed)
                        .addInteger(draft.sortingOrder)
                        .addBoolean(draft.enabled),
                ).awaitSuspending()
                    .first()

            requireSpecies(connection, row.getUUID("id"))
        }

    suspend fun updateSpecies(
        id: SpeciesId,
        draft: SpeciesDraft,
    ): Species =
        pool.withSuspendingTransaction { connection ->
            val updatedCount =
                connection.preparedQuery(
                    """
                    UPDATE catalog.creature_species
                    SET code = $1,
                        dex_number = $2,
                        name = $3,
                        description = $4,
                        primary_type_id = $5,
                        secondary_type_id = $6,
                        growth_rate_id = $7,
                        base_hp = $8,
                        base_attack = $9,
                        base_defense = $10,
                        base_special_attack = $11,
                        base_special_defense = $12,
                        base_speed = $13,
                        sorting_order = $14,
                        enabled = $15,
                        version = version + 1
                    WHERE id = $16
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addString(draft.code)
                        .addInteger(draft.dexNumber)
                        .addString(draft.name)
                        .addValue(draft.description)
                        .addValue(draft.primaryTypeId.value)
                        .addValue(draft.secondaryTypeId?.value)
                        .addValue(draft.growthRateId?.value)
                        .addInteger(draft.baseStats.hp)
                        .addInteger(draft.baseStats.attack)
                        .addInteger(draft.baseStats.defense)
                        .addInteger(draft.baseStats.specialAttack)
                        .addInteger(draft.baseStats.specialDefense)
                        .addInteger(draft.baseStats.speed)
                        .addInteger(draft.sortingOrder)
                        .addBoolean(draft.enabled)
                        .addValue(id.value),
                ).awaitSuspending()
                    .rowCount()

            if (updatedCount == 0) {
                throw CatalogNotFound("creature_species", id.value.toString())
            }

            requireSpecies(connection, id.value)
        }

    suspend fun deleteSpecies(id: SpeciesId) {
        val deletedCount =
            pool.preparedQuery("DELETE FROM catalog.creature_species WHERE id = $1")
                .execute(Tuple.of(id.value))
                .awaitSuspending()
                .rowCount()

        if (deletedCount == 0) {
            throw CatalogNotFound("creature_species", id.value.toString())
        }
    }
}