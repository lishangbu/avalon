package io.github.lishangbu.avalon.game.service.battle

import io.github.lishangbu.avalon.dataset.service.TypeEffectivenessService
import io.github.lishangbu.avalon.game.battle.engine.dsl.EffectDefinition
import io.github.lishangbu.avalon.game.battle.engine.model.UnitState
import io.github.lishangbu.avalon.game.battle.engine.repository.EffectDefinitionRepository
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionQuery
import io.github.lishangbu.avalon.game.battle.engine.session.ItemChoice
import io.github.lishangbu.avalon.game.battle.engine.session.MoveChoice
import io.github.lishangbu.avalon.game.battle.engine.session.target.BattleSessionTargetQuery
import io.github.lishangbu.avalon.game.battle.engine.session.target.BattleSessionTargetQueryService
import org.springframework.stereotype.Service
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * 基于真实数据与当前快照自动补全行动输入。
 */
@Service
class DefaultBattleChoiceFactory(
    private val effectDefinitionRepository: EffectDefinitionRepository,
    private val typeEffectivenessService: TypeEffectivenessService,
    private val targetQueryService: BattleSessionTargetQueryService,
) : BattleChoiceFactory {
    override fun queryTargets(
        session: BattleSessionQuery,
        effectId: String,
        actorUnitId: String,
    ): BattleSessionTargetQuery =
        targetQueryService.resolve(
            snapshot = session.snapshot,
            effectId = effectId,
            actorUnitId = actorUnitId,
        )

    override fun createMoveChoice(
        session: BattleSessionQuery,
        request: SmartMoveChoiceRequest,
    ): MoveChoice {
        val effect = effectDefinitionRepository.get(request.moveId)
        val attacker = requireUnit(session, request.attackerId)
        val targetQuery = queryTargets(session, request.moveId, request.attackerId)
        val targetId = resolveTargetId(request.targetId, targetQuery, request.attackerId)
        val target = requireUnit(session, targetId)
        val basePower = request.basePower ?: effect.intData("basePower") ?: 0
        val moveAccuracy = request.accuracy ?: defaultAccuracy(effect, attacker)
        val targetEvasion = request.evasion ?: defaultEvasion(target)
        val priority = request.priority ?: effect.intData("priority") ?: 0
        val speed = request.speed ?: effectiveStat(attacker, "speed")
        val damage = request.damage ?: estimateDamage(effect, attacker, target, basePower)

        return MoveChoice(
            moveId = request.moveId,
            attackerId = request.attackerId,
            targetId = targetId,
            priority = priority,
            speed = speed,
            accuracy = moveAccuracy,
            evasion = targetEvasion,
            basePower = basePower,
            damage = damage,
            attributes =
                request.attributes +
                    mapOfNotNull(
                        "accuracyRoll" to request.accuracyRoll,
                        "chanceRoll" to request.chanceRoll,
                    ),
        )
    }

    override fun createItemChoice(
        session: BattleSessionQuery,
        request: SmartItemChoiceRequest,
    ): ItemChoice {
        val actor = requireUnit(session, request.actorUnitId)
        val targetQuery = queryTargets(session, request.itemId, request.actorUnitId)
        val targetId = resolveTargetId(request.targetId, targetQuery, request.actorUnitId)
        val effect = effectDefinitionRepository.get(request.itemId)
        return ItemChoice(
            itemId = request.itemId,
            actorUnitId = request.actorUnitId,
            targetId = targetId,
            priority = request.priority ?: effect.intData("priority") ?: 0,
            speed = request.speed ?: effectiveStat(actor, "speed"),
            attributes = request.attributes + mapOfNotNull("chanceRoll" to request.chanceRoll),
        )
    }

    private fun resolveTargetId(
        requestedTargetId: String?,
        targetQuery: BattleSessionTargetQuery,
        actorUnitId: String,
    ): String {
        if (requestedTargetId != null) {
            return requestedTargetId
        }
        if (!targetQuery.requiresExplicitTarget) {
            return targetQuery.availableTargetUnitIds.firstOrNull() ?: actorUnitId
        }
        if (targetQuery.availableTargetUnitIds.size == 1) {
            return targetQuery.availableTargetUnitIds.single()
        }
        error("Effect '${targetQuery.effectId}' requires an explicit target for actor '${targetQuery.actorUnitId}'.")
    }

    private fun estimateDamage(
        effect: EffectDefinition,
        attacker: UnitState,
        target: UnitState,
        basePower: Int,
    ): Int {
        if (basePower <= 0) {
            return 0
        }
        val damageClass = effect.data["damageClass"]?.toString()
        if (damageClass == null || damageClass == "status") {
            return 0
        }
        val attackStatName = if (damageClass == "special") "special-attack" else "attack"
        val defenseStatName = if (damageClass == "special") "special-defense" else "defense"
        val level = attacker.flags["level"]?.toIntOrNull() ?: 50
        val attack = effectiveStat(attacker, attackStatName).coerceAtLeast(1)
        val defense = effectiveStat(target, defenseStatName).coerceAtLeast(1)
        val typeId = effect.data["type"]?.toString()
        val stab = if (typeId != null && typeId in attacker.typeIds) 1.5 else 1.0
        val typeMultiplier =
            if (typeId != null && target.typeIds.isNotEmpty()) {
                typeEffectivenessService.calculate(typeId, target.typeIds.toList()).finalMultiplier?.toDouble() ?: 1.0
            } else {
                1.0
            }
        val rawDamage = (((((2.0 * level) / 5.0) + 2.0) * basePower * attack / defense) / 50.0) + 2.0
        val finalDamage = floor(rawDamage * stab * typeMultiplier).toInt()
        return when {
            typeMultiplier <= 0.0 -> 0
            finalDamage <= 0 -> 1
            else -> finalDamage
        }
    }

    private fun defaultAccuracy(
        effect: EffectDefinition,
        attacker: UnitState,
    ): Int? {
        val baseAccuracy = effect.intData("accuracy") ?: return null
        return (baseAccuracy * stageMultiplier(attacker.boosts["accuracy"] ?: 0)).roundToInt()
    }

    private fun defaultEvasion(target: UnitState): Int = (100.0 * stageMultiplier(target.boosts["evasion"] ?: 0)).roundToInt()

    private fun effectiveStat(
        unit: UnitState,
        statName: String,
    ): Int {
        val baseStat = unit.stats[statName] ?: return 0
        val stage = unit.boosts[statName] ?: 0
        return (baseStat * stageMultiplier(stage)).roundToInt()
    }

    private fun stageMultiplier(stage: Int): Double {
        val normalized = stage.coerceIn(-6, 6)
        return if (normalized >= 0) {
            (2.0 + normalized) / 2.0
        } else {
            2.0 / (2.0 - normalized)
        }
    }

    private fun requireUnit(
        session: BattleSessionQuery,
        unitId: String,
    ): UnitState =
        requireNotNull(session.snapshot.units[unitId]) {
            "Unit '$unitId' was not found in battle snapshot."
        }

    private fun EffectDefinition.intData(key: String): Int? =
        when (val value = data[key]) {
            is Int -> value
            is Long -> value.toInt()
            is Double -> value.roundToInt()
            is Float -> value.roundToInt()
            is Number -> value.toInt()
            is String -> value.toIntOrNull()
            else -> null
        }

    private fun mapOfNotNull(vararg entries: Pair<String, Any?>): Map<String, Any> = entries.mapNotNull { (key, value) -> value?.let { key to it } }.toMap()
}
