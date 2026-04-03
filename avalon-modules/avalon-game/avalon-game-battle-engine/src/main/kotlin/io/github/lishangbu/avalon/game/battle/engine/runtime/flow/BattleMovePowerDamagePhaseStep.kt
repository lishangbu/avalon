package io.github.lishangbu.avalon.game.battle.engine.runtime.flow

import io.github.lishangbu.avalon.game.battle.engine.event.StandardHookNames
import io.github.lishangbu.avalon.game.battle.engine.model.UnitState
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * 威力与伤害修正 phase step。
 *
 * @property phaseProcessor battle hook phase 处理器。
 */
class BattleMovePowerDamagePhaseStep(
    private val phaseProcessor: BattleFlowPhaseProcessor,
    private val typeEffectivenessResolver: BattleTypeEffectivenessResolver = NoopBattleTypeEffectivenessResolver,
) : BattleMoveResolutionStep {
    /**
     * 当前步骤在 pipeline 中的执行顺序。
     */
    override val order: Int = 300

    /**
     * 执行威力与伤害修正阶段。
     */
    override fun execute(context: BattleMoveResolutionContext) {
        val basePowerResult =
            phaseProcessor.processPhase(
                snapshot = context.snapshot,
                hookName = StandardHookNames.ON_MODIFY_BASE_POWER.value,
                moveEffect = context.moveEffect,
                selfId = context.attackerId,
                targetId = context.targetId,
                sourceId = context.sourceId,
                relay = context.basePower.toDouble(),
                attributes = context.attributes,
            )
        context.snapshot = basePowerResult.snapshot
        context.basePower = (basePowerResult.relay as? Number)?.toInt() ?: context.basePower

        val moveType = context.moveEffect.data["type"]?.toString()
        val damageClass = context.moveEffect.data["damageClass"]?.toString()
        val attacker = context.snapshot.units[context.attackerId]
        val target = context.snapshot.units[context.targetId]
        val typeMultiplier = typeEffectivenessResolver.resolve(moveType, attacker, target)
        val shouldComputeNativeDamage = shouldComputeDamage(context, damageClass)
        if (shouldComputeNativeDamage) {
            computeBaseDamage(
                context = context,
                moveType = moveType,
                damageClass = damageClass,
                attacker = attacker,
                target = target,
            )?.let { computedDamage ->
                context.damage = computedDamage
            }
        }

        val damageResult =
            phaseProcessor.processPhase(
                snapshot = context.snapshot,
                hookName = StandardHookNames.ON_MODIFY_DAMAGE.value,
                moveEffect = context.moveEffect,
                selfId = context.attackerId,
                targetId = context.targetId,
                sourceId = context.sourceId,
                relay = context.damage.toDouble(),
                attributes =
                    context.attributes +
                        mapOf(
                            "criticalHit" to context.criticalHit,
                            "moveType" to moveType,
                            "damageClass" to damageClass,
                            "typeMultiplier" to typeMultiplier,
                        ),
            )
        context.snapshot = damageResult.snapshot
        context.damage = (damageResult.relay as? Number)?.toInt() ?: context.damage

        if (context.criticalHit) {
            context.damage = floor(context.damage * CRITICAL_DAMAGE_MULTIPLIER).toInt()
        }
        if (shouldApplyDamageVariance(context, damageClass, shouldComputeNativeDamage) && typeMultiplier > 0.0) {
            val damageRoll = resolveDamageRoll(context)
            context.damageRoll = damageRoll
            context.damage = floor(context.damage * damageRoll / DAMAGE_ROLL_DENOMINATOR).toInt()
        }

        val stabMatched =
            context.basePower > 0 &&
                damageClass != null &&
                damageClass != "status" &&
                moveType != null &&
                attacker != null &&
                moveType in attacker.typeIds
        val baseStab = if (stabMatched) 1.5 else 1.0
        val stabResult =
            phaseProcessor.processPhase(
                snapshot = context.snapshot,
                hookName = StandardHookNames.ON_MODIFY_STAB.value,
                moveEffect = context.moveEffect,
                selfId = context.attackerId,
                targetId = context.targetId,
                sourceId = context.sourceId,
                relay = baseStab,
                attributes =
                    context.attributes +
                        mapOf(
                            "moveType" to moveType,
                            "stabMatched" to stabMatched,
                        ),
            )
        context.snapshot = stabResult.snapshot
        val stab = (stabResult.relay as? Number)?.toDouble() ?: baseStab
        context.damage = floor(context.damage * stab).toInt()

        if (typeMultiplier <= 0.0) {
            context.damage = 0
            context.hitSuccessful = false
            return
        }
        context.damage = floor(context.damage * typeMultiplier).toInt()
        if (context.basePower > 0 && context.damage <= 0) {
            context.damage = 1
        }
    }

    private fun shouldComputeDamage(
        context: BattleMoveResolutionContext,
        damageClass: String?,
    ): Boolean {
        if (context.basePower <= 0 || damageClass == null || damageClass == "status") {
            return false
        }
        if (context.attributes["computeDamage"] == true) {
            return true
        }
        return context.damage <= 0
    }

    private fun computeBaseDamage(
        context: BattleMoveResolutionContext,
        moveType: String?,
        damageClass: String?,
        attacker: UnitState?,
        target: UnitState?,
    ): Int? {
        if (attacker == null || target == null || damageClass == null) {
            return null
        }
        val attackStatKey = if (damageClass == SPECIAL_DAMAGE_CLASS) SPECIAL_ATTACK_STAT else ATTACK_STAT
        val defenseStatKey = if (damageClass == SPECIAL_DAMAGE_CLASS) SPECIAL_DEFENSE_STAT else DEFENSE_STAT
        val attackStat =
            resolveModifiedBattleStat(
                context = context,
                unit = attacker,
                statKey = attackStatKey,
                hookName = StandardHookNames.ON_MODIFY_ATTACK.value,
                moveType = moveType,
                damageClass = damageClass,
                stage =
                    normalizedOffensiveStage(
                        unit = attacker,
                        statKey = attackStatKey,
                        criticalHit = context.criticalHit,
                    ),
            ) ?: return null
        val defenseStat =
            resolveModifiedBattleStat(
                context = context,
                unit = target,
                statKey = defenseStatKey,
                hookName = StandardHookNames.ON_MODIFY_DEFENSE.value,
                moveType = moveType,
                damageClass = damageClass,
                stage =
                    normalizedDefensiveStage(
                        unit = target,
                        statKey = defenseStatKey,
                        criticalHit = context.criticalHit,
                    ),
            ) ?: return null
        val burnedAttack =
            if (damageClass == PHYSICAL_DAMAGE_CLASS && attacker.statusId == BURN_STATUS && attacker.abilityId != GUTS_ABILITY) {
                attackStat * BURN_ATTACK_MULTIPLIER
            } else {
                attackStat
            }
        val level = attacker.flags["level"]?.toIntOrNull() ?: DEFAULT_LEVEL
        val rawDamage = (((((2.0 * level) / 5.0) + 2.0) * context.basePower * burnedAttack / defenseStat) / 50.0) + 2.0
        return floor(rawDamage).toInt().coerceAtLeast(1)
    }

    private fun shouldApplyDamageVariance(
        context: BattleMoveResolutionContext,
        damageClass: String?,
        shouldComputeNativeDamage: Boolean,
    ): Boolean {
        if (context.basePower <= 0 || damageClass == null || damageClass == "status") {
            return false
        }
        if (context.attributes["damageRoll"] is Number) {
            return true
        }
        return shouldComputeNativeDamage
    }

    private fun resolveDamageRoll(context: BattleMoveResolutionContext): Int {
        val explicitRoll = (context.attributes["damageRoll"] as? Number)?.toInt()
        if (explicitRoll != null) {
            return explicitRoll.coerceIn(MIN_DAMAGE_ROLL, MAX_DAMAGE_ROLL)
        }
        val randomResult =
            context.snapshot.battle.randomState
                .nextInt(DAMAGE_ROLL_BOUND)
        context.snapshot =
            context.snapshot.copy(
                battle =
                    context.snapshot.battle.copy(
                        randomState = randomResult.nextState,
                    ),
            )
        return MIN_DAMAGE_ROLL + randomResult.value
    }

    private fun resolveModifiedBattleStat(
        context: BattleMoveResolutionContext,
        unit: UnitState,
        statKey: String,
        hookName: String,
        moveType: String?,
        damageClass: String,
        stage: Int,
    ): Double? {
        val baseStat = resolveStat(unit, statKey) ?: return null
        val stagedStat = baseStat * stageMultiplier(stage)
        val hookResult =
            phaseProcessor.processPhase(
                snapshot = context.snapshot,
                hookName = hookName,
                moveEffect = context.moveEffect,
                selfId = unit.id,
                targetId = context.targetId,
                sourceId = context.sourceId,
                relay = stagedStat,
                attributes =
                    context.attributes +
                        mapOf(
                            "moveType" to moveType,
                            "damageClass" to damageClass,
                            "criticalHit" to context.criticalHit,
                            "statKey" to statKey,
                        ),
            )
        context.snapshot = hookResult.snapshot
        return ((hookResult.relay as? Number)?.toDouble() ?: stagedStat).coerceAtLeast(1.0)
    }

    private fun normalizedOffensiveStage(
        unit: UnitState,
        statKey: String,
        criticalHit: Boolean,
    ): Int {
        val stage = resolveBoost(unit, statKey)
        return if (criticalHit && stage < 0) 0 else stage
    }

    private fun normalizedDefensiveStage(
        unit: UnitState,
        statKey: String,
        criticalHit: Boolean,
    ): Int {
        val stage = resolveBoost(unit, statKey)
        return if (criticalHit && stage > 0) 0 else stage
    }

    private fun resolveStat(
        unit: UnitState,
        statKey: String,
    ): Int? =
        when (statKey) {
            ATTACK_STAT -> unit.stats[ATTACK_STAT] ?: unit.stats["atk"]
            DEFENSE_STAT -> unit.stats[DEFENSE_STAT] ?: unit.stats["def"]
            SPECIAL_ATTACK_STAT -> unit.stats[SPECIAL_ATTACK_STAT] ?: unit.stats["spa"]
            SPECIAL_DEFENSE_STAT -> unit.stats[SPECIAL_DEFENSE_STAT] ?: unit.stats["spd"]
            else -> unit.stats[statKey]
        }

    private fun resolveBoost(
        unit: UnitState,
        statKey: String,
    ): Int =
        when (statKey) {
            ATTACK_STAT -> unit.boosts[ATTACK_STAT] ?: unit.boosts["atk"] ?: 0
            DEFENSE_STAT -> unit.boosts[DEFENSE_STAT] ?: unit.boosts["def"] ?: 0
            SPECIAL_ATTACK_STAT -> unit.boosts[SPECIAL_ATTACK_STAT] ?: unit.boosts["spa"] ?: 0
            SPECIAL_DEFENSE_STAT -> unit.boosts[SPECIAL_DEFENSE_STAT] ?: unit.boosts["spd"] ?: 0
            else -> unit.boosts[statKey] ?: 0
        }

    private fun stageMultiplier(stage: Int): Double {
        val normalized = stage.coerceIn(-6, 6)
        return if (normalized >= 0) {
            (2.0 + normalized) / 2.0
        } else {
            2.0 / (2.0 - normalized)
        }
    }

    private companion object {
        private const val DEFAULT_LEVEL: Int = 50
        private const val ATTACK_STAT: String = "attack"
        private const val DEFENSE_STAT: String = "defense"
        private const val SPECIAL_ATTACK_STAT: String = "special-attack"
        private const val SPECIAL_DEFENSE_STAT: String = "special-defense"
        private const val PHYSICAL_DAMAGE_CLASS: String = "physical"
        private const val SPECIAL_DAMAGE_CLASS: String = "special"
        private const val BURN_STATUS: String = "brn"
        private const val GUTS_ABILITY: String = "guts"
        private const val BURN_ATTACK_MULTIPLIER: Double = 0.5
        private const val CRITICAL_DAMAGE_MULTIPLIER: Double = 1.5
        private const val MIN_DAMAGE_ROLL: Int = 85
        private const val MAX_DAMAGE_ROLL: Int = 100
        private const val DAMAGE_ROLL_BOUND: Int = 16
        private const val DAMAGE_ROLL_DENOMINATOR: Double = 100.0
    }
}
