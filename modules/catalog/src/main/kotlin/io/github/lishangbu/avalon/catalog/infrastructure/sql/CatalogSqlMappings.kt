package io.github.lishangbu.avalon.catalog.infrastructure.sql

import io.github.lishangbu.avalon.catalog.domain.*
import io.vertx.mutiny.sqlclient.Row

internal fun mapTypeDefinition(row: Row): TypeDefinition =
    TypeDefinition(
        id = TypeDefinitionId(row.getUUID("id")),
        code = row.getString("code"),
        name = row.getString("name"),
        description = row.getString("description"),
        icon = row.getString("icon"),
        sortingOrder = row.getInteger("sorting_order"),
        enabled = row.getBoolean("enabled"),
        version = row.getLong("version"),
    )

internal fun mapTypeEffectiveness(row: Row): TypeEffectiveness =
    TypeEffectiveness(
        id = TypeEffectivenessId(row.getUUID("id")),
        attackingType =
            TypeDefinitionSummary(
                id = TypeDefinitionId(row.getUUID("attacking_type_id")),
                code = row.getString("attacking_type_code"),
                name = row.getString("attacking_type_name"),
            ),
        defendingType =
            TypeDefinitionSummary(
                id = TypeDefinitionId(row.getUUID("defending_type_id")),
                code = row.getString("defending_type_code"),
                name = row.getString("defending_type_name"),
            ),
        multiplier = row.getBigDecimal("multiplier"),
        version = row.getLong("version"),
    )

internal fun mapNature(row: Row): Nature =
    Nature(
        id = NatureId(row.getUUID("id")),
        code = row.getString("code"),
        name = row.getString("name"),
        description = row.getString("description"),
        increasedStatCode = row.getNullableNatureModifierStatCode("increased_stat_code"),
        decreasedStatCode = row.getNullableNatureModifierStatCode("decreased_stat_code"),
        sortingOrder = row.getInteger("sorting_order"),
        enabled = row.getBoolean("enabled"),
        version = row.getLong("version"),
    )

internal fun mapAbility(row: Row): Ability =
    Ability(
        id = AbilityId(row.getUUID("id")),
        code = row.getString("code"),
        name = row.getString("name"),
        description = row.getString("description"),
        icon = row.getString("icon"),
        sortingOrder = row.getInteger("sorting_order"),
        enabled = row.getBoolean("enabled"),
        version = row.getLong("version"),
    )

internal fun mapItem(row: Row): Item =
    Item(
        id = ItemId(row.getUUID("id")),
        code = row.getString("code"),
        name = row.getString("name"),
        categoryCode = row.getString("category_code"),
        description = row.getString("description"),
        icon = row.getString("icon"),
        maxStackSize = row.getInteger("max_stack_size"),
        consumable = row.getBoolean("consumable"),
        sortingOrder = row.getInteger("sorting_order"),
        enabled = row.getBoolean("enabled"),
        version = row.getLong("version"),
    )

internal fun mapMoveCategory(row: Row): MoveCategory =
    MoveCategory(
        id = MoveCategoryId(row.getUUID("id")),
        code = row.getString("code"),
        name = row.getString("name"),
        description = row.getString("description"),
        sortingOrder = row.getInteger("sorting_order"),
        enabled = row.getBoolean("enabled"),
        version = row.getLong("version"),
    )

internal fun mapMoveAilment(row: Row): MoveAilment =
    MoveAilment(
        id = MoveAilmentId(row.getUUID("id")),
        code = row.getString("code"),
        name = row.getString("name"),
        description = row.getString("description"),
        sortingOrder = row.getInteger("sorting_order"),
        enabled = row.getBoolean("enabled"),
        version = row.getLong("version"),
    )

internal fun mapMove(row: Row): Move =
    Move(
        id = MoveId(row.getUUID("id")),
        code = row.getString("code"),
        name = row.getString("name"),
        type =
            TypeDefinitionSummary(
                id = TypeDefinitionId(row.getUUID("type_definition_id")),
                code = row.getString("type_definition_code"),
                name = row.getString("type_definition_name"),
            ),
        categoryCode = MoveCategoryCode.valueOf(row.getString("category_code")),
        moveCategory =
            row.getUUID("move_category_id")?.let {
                MoveCategorySummary(
                    id = MoveCategoryId(it),
                    code = row.getString("move_category_code"),
                    name = row.getString("move_category_name"),
                )
            },
        moveAilment =
            row.getUUID("move_ailment_id")?.let {
                MoveAilmentSummary(
                    id = MoveAilmentId(it),
                    code = row.getString("move_ailment_code"),
                    name = row.getString("move_ailment_name"),
                )
            },
        moveTarget =
            row.getUUID("move_target_id")?.let {
                MoveTargetSummary(
                    id = MoveTargetId(it),
                    code = row.getString("move_target_code"),
                    name = row.getString("move_target_name"),
                )
            },
        description = row.getString("description"),
        effectChance = row.getInteger("effect_chance"),
        power = row.getInteger("power"),
        accuracy = row.getInteger("accuracy"),
        powerPoints = row.getInteger("power_points"),
        priority = row.getInteger("priority"),
        text = row.getString("text"),
        shortEffect = row.getString("short_effect"),
        effect = row.getString("effect"),
        sortingOrder = row.getInteger("sorting_order"),
        enabled = row.getBoolean("enabled"),
        version = row.getLong("version"),
    )

internal fun mapMoveTarget(row: Row): MoveTarget =
    MoveTarget(
        id = MoveTargetId(row.getUUID("id")),
        code = row.getString("code"),
        name = row.getString("name"),
        description = row.getString("description"),
        sortingOrder = row.getInteger("sorting_order"),
        enabled = row.getBoolean("enabled"),
        version = row.getLong("version"),
    )

internal fun mapMoveLearnMethod(row: Row): MoveLearnMethod =
    MoveLearnMethod(
        id = MoveLearnMethodId(row.getUUID("id")),
        code = row.getString("code"),
        name = row.getString("name"),
        description = row.getString("description"),
        sortingOrder = row.getInteger("sorting_order"),
        enabled = row.getBoolean("enabled"),
        version = row.getLong("version"),
    )

internal fun mapGrowthRate(row: Row): GrowthRate =
    GrowthRate(
        id = GrowthRateId(row.getUUID("id")),
        code = row.getString("code"),
        name = row.getString("name"),
        formulaCode = GrowthRateFormulaCode.valueOf(row.getString("formula_code")),
        description = row.getString("description"),
        sortingOrder = row.getInteger("sorting_order"),
        enabled = row.getBoolean("enabled"),
        version = row.getLong("version"),
    )

internal fun mapSpeciesEvolution(row: Row): SpeciesEvolution =
    SpeciesEvolution(
        id = SpeciesEvolutionId(row.getUUID("id")),
        fromSpecies =
            SpeciesSummary(
                id = SpeciesId(row.getUUID("from_species_id")),
                code = row.getString("from_species_code"),
                dexNumber = row.getInteger("from_species_dex_number"),
                name = row.getString("from_species_name"),
            ),
        toSpecies =
            SpeciesSummary(
                id = SpeciesId(row.getUUID("to_species_id")),
                code = row.getString("to_species_code"),
                dexNumber = row.getInteger("to_species_dex_number"),
                name = row.getString("to_species_name"),
            ),
        triggerCode = EvolutionTriggerCode.valueOf(row.getString("trigger_code")),
        minLevel = row.getInteger("min_level"),
        description = row.getString("description"),
        sortingOrder = row.getInteger("sorting_order"),
        enabled = row.getBoolean("enabled"),
        version = row.getLong("version"),
    )

internal fun mapSpeciesMoveLearnset(row: Row): SpeciesMoveLearnset =
    SpeciesMoveLearnset(
        id = SpeciesMoveLearnsetId(row.getUUID("id")),
        species =
            SpeciesSummary(
                id = SpeciesId(row.getUUID("species_id")),
                code = row.getString("species_code"),
                dexNumber = row.getInteger("species_dex_number"),
                name = row.getString("species_name"),
            ),
        move =
            MoveSummary(
                id = MoveId(row.getUUID("move_id")),
                code = row.getString("move_code"),
                name = row.getString("move_name"),
            ),
        learnMethod =
            MoveLearnMethodSummary(
                id = MoveLearnMethodId(row.getUUID("learn_method_id")),
                code = row.getString("learn_method_code"),
                name = row.getString("learn_method_name"),
            ),
        level = row.getInteger("level"),
        sortingOrder = row.getInteger("sorting_order"),
        enabled = row.getBoolean("enabled"),
        version = row.getLong("version"),
    )

internal fun mapSpeciesAbility(row: Row): SpeciesAbility =
    SpeciesAbility(
        id = SpeciesAbilityId(row.getUUID("id")),
        species =
            SpeciesSummary(
                id = SpeciesId(row.getUUID("species_id")),
                code = row.getString("species_code"),
                dexNumber = row.getInteger("species_dex_number"),
                name = row.getString("species_name"),
            ),
        ability =
            AbilitySummary(
                id = AbilityId(row.getUUID("ability_id")),
                code = row.getString("ability_code"),
                name = row.getString("ability_name"),
            ),
        slotCode = AbilitySlotCode.valueOf(row.getString("slot_code")),
        sortingOrder = row.getInteger("sorting_order"),
        enabled = row.getBoolean("enabled"),
        version = row.getLong("version"),
    )

internal fun mapSpecies(row: Row): Species =
    Species(
        id = SpeciesId(row.getUUID("id")),
        code = row.getString("code"),
        dexNumber = row.getInteger("dex_number"),
        name = row.getString("name"),
        description = row.getString("description"),
        primaryType =
            TypeDefinitionSummary(
                id = TypeDefinitionId(row.getUUID("primary_type_id")),
                code = row.getString("primary_type_code"),
                name = row.getString("primary_type_name"),
            ),
        secondaryType =
            row.getUUID("secondary_type_id")?.let {
                TypeDefinitionSummary(
                    id = TypeDefinitionId(it),
                    code = row.getString("secondary_type_code"),
                    name = row.getString("secondary_type_name"),
                )
            },
        growthRate =
            row.getUUID("growth_rate_id")?.let {
                GrowthRateSummary(
                    id = GrowthRateId(it),
                    code = row.getString("growth_rate_code"),
                    name = row.getString("growth_rate_name"),
                    formulaCode = GrowthRateFormulaCode.valueOf(row.getString("growth_rate_formula_code")),
                )
            },
        baseStats =
            SpeciesBaseStats(
                hp = row.getInteger("base_hp"),
                attack = row.getInteger("base_attack"),
                defense = row.getInteger("base_defense"),
                specialAttack = row.getInteger("base_special_attack"),
                specialDefense = row.getInteger("base_special_defense"),
                speed = row.getInteger("base_speed"),
            ),
        sortingOrder = row.getInteger("sorting_order"),
        enabled = row.getBoolean("enabled"),
        version = row.getLong("version"),
    )

internal fun Row.getNullableNatureModifierStatCode(column: String): NatureModifierStatCode? =
    getString(column)?.let(NatureModifierStatCode::valueOf)