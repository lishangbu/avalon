package io.github.lishangbu.avalon.catalog.infrastructure.sql

/**
 * Catalog 的 canonical select SQL 统一放在这里，避免每个 gateway 各自复制一份。
 *
 * 这些 SQL 仍然属于 Catalog 上下文自己的读模型装配规则，因此不下沉到 shared-infra。
 */
internal const val TYPE_DEFINITION_SELECT_SQL =
    """
    SELECT
        td.id,
        td.code,
        td.name,
        td.description,
        td.icon,
        td.sorting_order,
        td.enabled,
        td.version
    FROM catalog.type_definition td
    """

internal const val TYPE_EFFECTIVENESS_SELECT_SQL =
    """
    SELECT
        te.id,
        te.attacking_type_id,
        te.defending_type_id,
        te.multiplier,
        te.version,
        attacking.code AS attacking_type_code,
        attacking.name AS attacking_type_name,
        defending.code AS defending_type_code,
        defending.name AS defending_type_name
    FROM catalog.type_effectiveness te
    INNER JOIN catalog.type_definition attacking ON attacking.id = te.attacking_type_id
    INNER JOIN catalog.type_definition defending ON defending.id = te.defending_type_id
    """

internal const val NATURE_SELECT_SQL =
    """
    SELECT
        n.id,
        n.code,
        n.name,
        n.description,
        n.increased_stat_code,
        n.decreased_stat_code,
        n.sorting_order,
        n.enabled,
        n.version
    FROM catalog.nature n
    """

internal const val ITEM_SELECT_SQL =
    """
    SELECT
        i.id,
        i.code,
        i.name,
        i.category_code,
        i.description,
        i.icon,
        i.max_stack_size,
        i.consumable,
        i.sorting_order,
        i.enabled,
        i.version
    FROM catalog.item i
    """

internal const val ABILITY_SELECT_SQL =
    """
    SELECT
        a.id,
        a.code,
        a.name,
        a.description,
        a.icon,
        a.sorting_order,
        a.enabled,
        a.version
    FROM catalog.ability a
    """

internal const val MOVE_CATEGORY_SELECT_SQL =
    """
    SELECT
        mc.id,
        mc.code,
        mc.name,
        mc.description,
        mc.sorting_order,
        mc.enabled,
        mc.version
    FROM catalog.move_category mc
    """

internal const val MOVE_AILMENT_SELECT_SQL =
    """
    SELECT
        ma.id,
        ma.code,
        ma.name,
        ma.description,
        ma.sorting_order,
        ma.enabled,
        ma.version
    FROM catalog.move_ailment ma
    """

internal const val MOVE_TARGET_SELECT_SQL =
    """
    SELECT
        mt.id,
        mt.code,
        mt.name,
        mt.description,
        mt.sorting_order,
        mt.enabled,
        mt.version
    FROM catalog.move_target mt
    """

internal const val MOVE_LEARN_METHOD_SELECT_SQL =
    """
    SELECT
        mlm.id,
        mlm.code,
        mlm.name,
        mlm.description,
        mlm.sorting_order,
        mlm.enabled,
        mlm.version
    FROM catalog.move_learn_method mlm
    """

internal const val MOVE_SELECT_SQL =
    """
    SELECT
        m.id,
        m.code,
        m.name,
        m.type_definition_id,
        m.category_code,
        m.move_category_id,
        m.move_ailment_id,
        m.move_target_id,
        m.description,
        m.effect_chance,
        m.power,
        m.accuracy,
        m.power_points,
        m.priority,
        m.text,
        m.short_effect,
        m.effect,
        m.sorting_order,
        m.enabled,
        m.version,
        td.code AS type_definition_code,
        td.name AS type_definition_name,
        mc.code AS move_category_code,
        mc.name AS move_category_name,
        ma.code AS move_ailment_code,
        ma.name AS move_ailment_name,
        mt.code AS move_target_code,
        mt.name AS move_target_name
    FROM catalog.move m
    INNER JOIN catalog.type_definition td ON td.id = m.type_definition_id
    LEFT JOIN catalog.move_category mc ON mc.id = m.move_category_id
    LEFT JOIN catalog.move_ailment ma ON ma.id = m.move_ailment_id
    LEFT JOIN catalog.move_target mt ON mt.id = m.move_target_id
    """

internal const val GROWTH_RATE_SELECT_SQL =
    """
    SELECT
        gr.id,
        gr.code,
        gr.name,
        gr.formula_code,
        gr.description,
        gr.sorting_order,
        gr.enabled,
        gr.version
    FROM catalog.growth_rate gr
    """

internal const val SPECIES_EVOLUTION_SELECT_SQL =
    """
    SELECT
        se.id,
        se.from_species_id,
        se.to_species_id,
        se.trigger_code,
        se.min_level,
        se.description,
        se.sorting_order,
        se.enabled,
        se.version,
        from_species.code AS from_species_code,
        from_species.dex_number AS from_species_dex_number,
        from_species.name AS from_species_name,
        to_species.code AS to_species_code,
        to_species.dex_number AS to_species_dex_number,
        to_species.name AS to_species_name
    FROM catalog.species_evolution se
    INNER JOIN catalog.creature_species from_species ON from_species.id = se.from_species_id
    INNER JOIN catalog.creature_species to_species ON to_species.id = se.to_species_id
    """

internal const val SPECIES_ABILITY_SELECT_SQL =
    """
    SELECT
        sa.id,
        sa.species_id,
        sa.ability_id,
        sa.slot_code,
        sa.sorting_order,
        sa.enabled,
        sa.version,
        species.code AS species_code,
        species.dex_number AS species_dex_number,
        species.name AS species_name,
        ability.code AS ability_code,
        ability.name AS ability_name
    FROM catalog.species_ability sa
    INNER JOIN catalog.creature_species species ON species.id = sa.species_id
    INNER JOIN catalog.ability ability ON ability.id = sa.ability_id
    """

internal const val SPECIES_MOVE_LEARNSET_SELECT_SQL =
    """
    SELECT
        sml.id,
        sml.species_id,
        sml.move_id,
        sml.learn_method_id,
        sml.level,
        sml.sorting_order,
        sml.enabled,
        sml.version,
        species.code AS species_code,
        species.dex_number AS species_dex_number,
        species.name AS species_name,
        move.code AS move_code,
        move.name AS move_name,
        learn_method.code AS learn_method_code,
        learn_method.name AS learn_method_name
    FROM catalog.species_move_learnset sml
    INNER JOIN catalog.creature_species species ON species.id = sml.species_id
    INNER JOIN catalog.move move ON move.id = sml.move_id
    INNER JOIN catalog.move_learn_method learn_method ON learn_method.id = sml.learn_method_id
    """

internal const val SPECIES_SELECT_SQL =
    """
    SELECT
        cs.id,
        cs.code,
        cs.dex_number,
        cs.name,
        cs.description,
        cs.primary_type_id,
        cs.secondary_type_id,
        cs.growth_rate_id,
        cs.base_hp,
        cs.base_attack,
        cs.base_defense,
        cs.base_special_attack,
        cs.base_special_defense,
        cs.base_speed,
        cs.sorting_order,
        cs.enabled,
        cs.version,
        primary_type.code AS primary_type_code,
        primary_type.name AS primary_type_name,
        secondary_type.code AS secondary_type_code,
        secondary_type.name AS secondary_type_name,
        growth_rate.code AS growth_rate_code,
        growth_rate.name AS growth_rate_name,
        growth_rate.formula_code AS growth_rate_formula_code
    FROM catalog.creature_species cs
    INNER JOIN catalog.type_definition primary_type ON primary_type.id = cs.primary_type_id
    LEFT JOIN catalog.type_definition secondary_type ON secondary_type.id = cs.secondary_type_id
    LEFT JOIN catalog.growth_rate growth_rate ON growth_rate.id = cs.growth_rate_id
    """

internal const val SPECIES_DEFAULT_ORDER_BY_SQL = "ORDER BY cs.sorting_order, cs.id"

internal const val SPECIES_COUNT_SQL = "SELECT COUNT(*) AS total_items FROM catalog.creature_species cs"