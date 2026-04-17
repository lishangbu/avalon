package io.github.lishangbu.avalon.catalog.infrastructure.berry.sql

internal val BERRY_DEFINITION_SELECT_SQL =
    """
    SELECT bd.id,
           bd.code,
           bd.name,
           bd.description,
           bd.icon,
           bd.color_code,
           bd.firmness_code,
           bd.size_cm,
           bd.smoothness,
           bd.spicy,
           bd.dry,
           bd.sweet,
           bd.bitter,
           bd.sour,
           bd.natural_gift_type_id,
           ngt.code AS natural_gift_type_code,
           ngt.name AS natural_gift_type_name,
           bd.natural_gift_power,
           bd.sorting_order,
           bd.enabled,
           bd.version
    FROM catalog.berry_definition bd
    LEFT JOIN catalog.type_definition ngt ON ngt.id = bd.natural_gift_type_id
    """.trimIndent()

internal val BERRY_BATTLE_EFFECT_SELECT_SQL =
    """
    SELECT berry_id,
           hold_effect_summary,
           direct_use_effect_summary,
           fling_power,
           fling_effect_summary,
           pluck_effect_summary,
           bug_bite_effect_summary,
           version
    FROM catalog.berry_battle_effect
    """.trimIndent()

internal val BERRY_CULTIVATION_SELECT_SQL =
    """
    SELECT berry_id,
           growth_hours_min,
           growth_hours_max,
           yield_min,
           yield_max,
           cultivation_summary,
           version
    FROM catalog.berry_cultivation_profile
    """.trimIndent()

internal val BERRY_ACQUISITION_SELECT_SQL =
    """
    SELECT id,
           berry_id,
           source_type,
           condition_note,
           sorting_order,
           enabled,
           version
    FROM catalog.berry_acquisition
    """.trimIndent()

internal val BERRY_MOVE_RELATION_SELECT_SQL =
    """
    SELECT id,
           berry_id,
           move_code,
           move_name,
           relation_kind,
           note,
           sorting_order,
           enabled,
           version
    FROM catalog.berry_move_relation
    """.trimIndent()

internal val BERRY_ABILITY_RELATION_SELECT_SQL =
    """
    SELECT id,
           berry_id,
           ability_code,
           ability_name,
           relation_kind,
           note,
           sorting_order,
           enabled,
           version
    FROM catalog.berry_ability_relation
    """.trimIndent()
