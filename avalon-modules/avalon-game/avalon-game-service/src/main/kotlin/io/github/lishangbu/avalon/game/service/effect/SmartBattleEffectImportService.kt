package io.github.lishangbu.avalon.game.service.effect

import io.github.lishangbu.avalon.dataset.entity.dto.AbilitySpecification
import io.github.lishangbu.avalon.dataset.entity.dto.AbilityView
import io.github.lishangbu.avalon.dataset.entity.dto.ItemSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.ItemView
import io.github.lishangbu.avalon.dataset.entity.dto.MoveSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveView
import io.github.lishangbu.avalon.dataset.repository.AbilityRepository
import io.github.lishangbu.avalon.dataset.repository.ItemRepository
import io.github.lishangbu.avalon.dataset.repository.MoveRepository
import io.github.lishangbu.avalon.game.battle.engine.dsl.EffectDefinition
import org.springframework.stereotype.Service

/**
 * 基于真实数据源的战斗 effect 智能导入服务。
 */
@Service
class SmartBattleEffectImportService(
    private val moveRepository: MoveRepository,
    private val abilityRepository: AbilityRepository,
    private val itemRepository: ItemRepository,
) {
    fun importEffect(effectId: String): EffectDefinition? =
        loadMove(effectId)?.let(::toMoveRecord)?.let(SmartBattleEffectAssembler::fromMove)
            ?: loadAbility(effectId)?.let(::toAbilityRecord)?.let(SmartBattleEffectAssembler::fromAbility)
            ?: loadItem(effectId)?.let(::toItemRecord)?.let(SmartBattleEffectAssembler::fromItem)

    private fun loadMove(internalName: String): MoveView? =
        moveRepository
            .listViews(MoveSpecification(internalName = internalName))
            .firstOrNull { move -> move.internalName == internalName }

    private fun loadAbility(internalName: String): AbilityView? =
        abilityRepository
            .listViews(AbilitySpecification(internalName = internalName))
            .firstOrNull { ability -> ability.internalName == internalName }

    private fun loadItem(internalName: String): ItemView? =
        itemRepository
            .listViews(ItemSpecification(internalName = internalName))
            .firstOrNull { item -> item.internalName == internalName }

    private fun toMoveRecord(move: MoveView): MoveImportRecord =
        MoveImportRecord(
            internalName = requireNotNull(move.internalName) { "Move internalName must not be null." },
            name = move.name ?: requireNotNull(move.internalName),
            typeInternalName = move.type?.internalName,
            damageClassInternalName = move.moveDamageClass?.internalName,
            targetInternalName = move.moveTarget?.internalName,
            accuracy = move.accuracy,
            effectChance = move.effectChance,
            pp = move.pp,
            priority = move.priority,
            power = move.power,
            shortEffect = move.shortEffect,
            effect = move.effect,
            ailmentInternalName = move.moveAilment?.internalName,
            ailmentChance = move.ailmentChance,
            healing = move.healing,
            drain = move.drain,
        )

    private fun toAbilityRecord(ability: AbilityView): AbilityImportRecord =
        AbilityImportRecord(
            internalName = requireNotNull(ability.internalName) { "Ability internalName must not be null." },
            name = ability.name ?: requireNotNull(ability.internalName),
            effect = ability.effect,
            introduction = ability.introduction,
        )

    private fun toItemRecord(item: ItemView): ItemImportRecord =
        ItemImportRecord(
            internalName = requireNotNull(item.internalName) { "Item internalName must not be null." },
            name = item.name ?: requireNotNull(item.internalName),
            shortEffect = item.shortEffect,
            effect = item.effect,
            text = item.text,
            attributeInternalNames = item.itemAttributes.mapNotNull { attribute -> attribute.internalName }.toSet(),
            flingEffectInternalName = item.itemFlingEffect?.internalName,
        )
}
