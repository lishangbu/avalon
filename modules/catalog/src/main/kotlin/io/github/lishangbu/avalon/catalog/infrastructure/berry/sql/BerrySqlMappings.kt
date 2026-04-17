package io.github.lishangbu.avalon.catalog.infrastructure.berry.sql

import io.github.lishangbu.avalon.catalog.domain.TypeDefinitionId
import io.github.lishangbu.avalon.catalog.domain.TypeDefinitionSummary
import io.github.lishangbu.avalon.catalog.domain.berry.*
import io.vertx.mutiny.sqlclient.Row

internal fun mapBerryDefinition(row: Row): BerryDefinition =
    BerryDefinition(
        id = BerryDefinitionId(row.getUUID("id")),
        code = row.getString("code"),
        name = row.getString("name"),
        description = row.getString("description"),
        icon = row.getString("icon"),
        colorCode = row.getString("color_code"),
        firmnessCode = row.getString("firmness_code"),
        sizeCm = row.getBigDecimal("size_cm"),
        smoothness = row.getInteger("smoothness"),
        spicy = row.getInteger("spicy"),
        dry = row.getInteger("dry"),
        sweet = row.getInteger("sweet"),
        bitter = row.getInteger("bitter"),
        sour = row.getInteger("sour"),
        naturalGiftType =
            row.getUUID("natural_gift_type_id")?.let {
                TypeDefinitionSummary(
                    id = TypeDefinitionId(it),
                    code = row.getString("natural_gift_type_code"),
                    name = row.getString("natural_gift_type_name"),
                )
            },
        naturalGiftPower = row.getInteger("natural_gift_power"),
        sortingOrder = row.getInteger("sorting_order"),
        enabled = row.getBoolean("enabled"),
        version = row.getLong("version"),
    )

internal fun mapBerryBattleEffect(row: Row): BerryBattleEffect =
    BerryBattleEffect(
        berryId = BerryDefinitionId(row.getUUID("berry_id")),
        holdEffectSummary = row.getString("hold_effect_summary"),
        directUseEffectSummary = row.getString("direct_use_effect_summary"),
        flingPower = row.getInteger("fling_power"),
        flingEffectSummary = row.getString("fling_effect_summary"),
        pluckEffectSummary = row.getString("pluck_effect_summary"),
        bugBiteEffectSummary = row.getString("bug_bite_effect_summary"),
        version = row.getLong("version"),
    )

internal fun mapBerryCultivationProfile(row: Row): BerryCultivationProfile =
    BerryCultivationProfile(
        berryId = BerryDefinitionId(row.getUUID("berry_id")),
        growthHoursMin = row.getInteger("growth_hours_min"),
        growthHoursMax = row.getInteger("growth_hours_max"),
        yieldMin = row.getInteger("yield_min"),
        yieldMax = row.getInteger("yield_max"),
        cultivationSummary = row.getString("cultivation_summary"),
        version = row.getLong("version"),
    )

internal fun mapBerryAcquisition(row: Row): BerryAcquisition =
    BerryAcquisition(
        id = BerryAcquisitionId(row.getUUID("id")),
        berryId = BerryDefinitionId(row.getUUID("berry_id")),
        sourceType = row.getString("source_type"),
        conditionNote = row.getString("condition_note"),
        sortingOrder = row.getInteger("sorting_order"),
        enabled = row.getBoolean("enabled"),
        version = row.getLong("version"),
    )

internal fun mapBerryMoveRelation(row: Row): BerryMoveRelation =
    BerryMoveRelation(
        id = BerryMoveRelationId(row.getUUID("id")),
        berryId = BerryDefinitionId(row.getUUID("berry_id")),
        moveCode = row.getString("move_code"),
        moveName = row.getString("move_name"),
        relationKind = row.getString("relation_kind"),
        note = row.getString("note"),
        sortingOrder = row.getInteger("sorting_order"),
        enabled = row.getBoolean("enabled"),
        version = row.getLong("version"),
    )

internal fun mapBerryAbilityRelation(row: Row): BerryAbilityRelation =
    BerryAbilityRelation(
        id = BerryAbilityRelationId(row.getUUID("id")),
        berryId = BerryDefinitionId(row.getUUID("berry_id")),
        abilityCode = row.getString("ability_code"),
        abilityName = row.getString("ability_name"),
        relationKind = row.getString("relation_kind"),
        note = row.getString("note"),
        sortingOrder = row.getInteger("sorting_order"),
        enabled = row.getBoolean("enabled"),
        version = row.getLong("version"),
    )
