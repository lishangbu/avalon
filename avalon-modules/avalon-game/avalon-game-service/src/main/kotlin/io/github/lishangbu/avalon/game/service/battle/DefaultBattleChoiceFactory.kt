package io.github.lishangbu.avalon.game.service.battle

import io.github.lishangbu.avalon.game.battle.engine.dsl.EffectDefinition
import io.github.lishangbu.avalon.game.battle.engine.model.UnitState
import io.github.lishangbu.avalon.game.battle.engine.repository.EffectDefinitionRepository
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionQuery
import io.github.lishangbu.avalon.game.battle.engine.session.ItemChoice
import io.github.lishangbu.avalon.game.battle.engine.session.MoveChoice
import io.github.lishangbu.avalon.game.battle.engine.session.target.BattleSessionTargetQuery
import io.github.lishangbu.avalon.game.battle.engine.session.target.BattleSessionTargetQueryService
import org.springframework.stereotype.Service
import kotlin.math.roundToInt

/**
 * 基于真实数据与当前快照自动补全行动输入。
 */
@Service
class DefaultBattleChoiceFactory(
    private val effectDefinitionRepository: EffectDefinitionRepository,
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
        val damage = request.damage ?: 0

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
                        "criticalRoll" to request.criticalRoll,
                        "damageRoll" to request.damageRoll,
                        "criticalHit" to request.criticalHit,
                        "computeDamage" to (request.damage == null),
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
