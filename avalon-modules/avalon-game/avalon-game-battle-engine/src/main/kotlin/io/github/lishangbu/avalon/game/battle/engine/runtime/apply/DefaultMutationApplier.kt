package io.github.lishangbu.avalon.game.battle.engine.runtime.apply

import io.github.lishangbu.avalon.game.battle.engine.model.FieldState
import io.github.lishangbu.avalon.game.battle.engine.model.UnitState
import io.github.lishangbu.avalon.game.battle.engine.mutation.AddVolatileMutation
import io.github.lishangbu.avalon.game.battle.engine.mutation.ApplyConditionMutation
import io.github.lishangbu.avalon.game.battle.engine.mutation.BattleMutation
import io.github.lishangbu.avalon.game.battle.engine.mutation.BoostMutation
import io.github.lishangbu.avalon.game.battle.engine.mutation.ChangeTypeMutation
import io.github.lishangbu.avalon.game.battle.engine.mutation.ClearBoostsMutation
import io.github.lishangbu.avalon.game.battle.engine.mutation.ClearFlagMutation
import io.github.lishangbu.avalon.game.battle.engine.mutation.ClearTerrainMutation
import io.github.lishangbu.avalon.game.battle.engine.mutation.ClearWeatherMutation
import io.github.lishangbu.avalon.game.battle.engine.mutation.ConsumeItemMutation
import io.github.lishangbu.avalon.game.battle.engine.mutation.DamageMutation
import io.github.lishangbu.avalon.game.battle.engine.mutation.ForceSwitchMutation
import io.github.lishangbu.avalon.game.battle.engine.mutation.HealMutation
import io.github.lishangbu.avalon.game.battle.engine.mutation.RemoveConditionMutation
import io.github.lishangbu.avalon.game.battle.engine.mutation.RemoveStatusMutation
import io.github.lishangbu.avalon.game.battle.engine.mutation.RemoveVolatileMutation
import io.github.lishangbu.avalon.game.battle.engine.mutation.RestorePpMutation
import io.github.lishangbu.avalon.game.battle.engine.mutation.SetFlagMutation
import io.github.lishangbu.avalon.game.battle.engine.mutation.SetStatusMutation
import io.github.lishangbu.avalon.game.battle.engine.mutation.SetTerrainMutation
import io.github.lishangbu.avalon.game.battle.engine.mutation.SetWeatherMutation
import io.github.lishangbu.avalon.game.battle.engine.mutation.TriggerEventMutation
import io.github.lishangbu.avalon.game.battle.engine.runtime.support.MutationTargetSelectorResolver

/**
 * 默认 mutation 应用器。
 *
 * 设计意图：
 * - 以不可变回写方式提交 mutation。
 * - 先覆盖当前骨架中已经存在的 mutation 类型。
 */
class DefaultMutationApplier : MutationApplier {
    override fun apply(
        mutations: List<BattleMutation>,
        context: MutationApplicationContext,
    ): MutationApplicationResult {
        var currentUnits: Map<String, UnitState> = context.units
        var currentField: FieldState = context.field
        val triggeredHooks = mutableListOf<io.github.lishangbu.avalon.game.battle.engine.type.HookName>()

        for (mutation in mutations) {
            when (mutation) {
                is DamageMutation -> currentUnits = applyDamage(mutation, context, currentUnits)
                is HealMutation -> currentUnits = applyHeal(mutation, context, currentUnits)
                is SetStatusMutation -> currentUnits = applySetStatus(mutation, context, currentUnits)
                is RemoveStatusMutation -> currentUnits = applyRemoveStatus(mutation, context, currentUnits)
                is AddVolatileMutation -> currentUnits = applyAddVolatile(mutation, context, currentUnits)
                is RemoveVolatileMutation -> currentUnits = applyRemoveVolatile(mutation, context, currentUnits)
                is BoostMutation -> currentUnits = applyBoost(mutation, context, currentUnits)
                is ClearBoostsMutation -> currentUnits = applyClearBoosts(mutation, context, currentUnits)
                is SetWeatherMutation -> currentField = currentField.copy(weatherId = mutation.weatherId)
                is ClearWeatherMutation -> currentField = currentField.copy(weatherId = null)
                is SetTerrainMutation -> currentField = currentField.copy(terrainId = mutation.terrainId)
                is ClearTerrainMutation -> currentField = currentField.copy(terrainId = null)
                is ConsumeItemMutation -> currentUnits = applyConsumeItem(mutation, context, currentUnits)
                is RestorePpMutation -> currentUnits = applyRestorePp(mutation, context, currentUnits)
                is ChangeTypeMutation -> currentUnits = applyChangeType(mutation, context, currentUnits)
                is ForceSwitchMutation -> currentUnits = applyForceSwitch(mutation, context, currentUnits)
                is ApplyConditionMutation -> currentUnits = applyAddCondition(mutation, context, currentUnits)
                is RemoveConditionMutation -> currentUnits = applyRemoveCondition(mutation, context, currentUnits)
                is SetFlagMutation -> currentUnits = applySetFlag(mutation, context, currentUnits)
                is ClearFlagMutation -> currentUnits = applyClearFlag(mutation, context, currentUnits)
                is TriggerEventMutation -> triggeredHooks += mutation.hookName
            }
        }

        return MutationApplicationResult(
            battle = context.battle,
            field = currentField,
            units = currentUnits,
            side = context.side,
            foeSide = context.foeSide,
            triggeredHooks = triggeredHooks,
        )
    }

    private fun applyDamage(
        mutation: DamageMutation,
        context: MutationApplicationContext,
        units: Map<String, UnitState>,
    ): Map<String, UnitState> =
        updateTargets(mutation.target, context, units) { unit ->
            val damage = calculateAmount(unit.maxHp, mutation.mode, mutation.value)
            unit.copy(currentHp = (unit.currentHp - damage).coerceAtLeast(0))
        }

    private fun applyHeal(
        mutation: HealMutation,
        context: MutationApplicationContext,
        units: Map<String, UnitState>,
    ): Map<String, UnitState> =
        updateTargets(mutation.target, context, units) { unit ->
            val healing = calculateAmount(unit.maxHp, mutation.mode, mutation.value)
            unit.copy(currentHp = (unit.currentHp + healing).coerceAtMost(unit.maxHp))
        }

    private fun applySetStatus(
        mutation: SetStatusMutation,
        context: MutationApplicationContext,
        units: Map<String, UnitState>,
    ): Map<String, UnitState> =
        updateTargets(mutation.target, context, units) { unit ->
            unit.copy(statusId = mutation.statusId)
        }

    private fun applyRemoveStatus(
        mutation: RemoveStatusMutation,
        context: MutationApplicationContext,
        units: Map<String, UnitState>,
    ): Map<String, UnitState> =
        updateTargets(mutation.target, context, units) { unit ->
            unit.copy(statusId = null)
        }

    private fun applyAddVolatile(
        mutation: AddVolatileMutation,
        context: MutationApplicationContext,
        units: Map<String, UnitState>,
    ): Map<String, UnitState> =
        updateTargets(mutation.target, context, units) { unit ->
            unit.copy(volatileIds = unit.volatileIds + mutation.volatileId)
        }

    private fun applyRemoveVolatile(
        mutation: RemoveVolatileMutation,
        context: MutationApplicationContext,
        units: Map<String, UnitState>,
    ): Map<String, UnitState> =
        updateTargets(mutation.target, context, units) { unit ->
            unit.copy(volatileIds = unit.volatileIds - mutation.volatileId)
        }

    private fun applyBoost(
        mutation: BoostMutation,
        context: MutationApplicationContext,
        units: Map<String, UnitState>,
    ): Map<String, UnitState> =
        updateTargets(mutation.target, context, units) { unit ->
            val nextBoosts = unit.boosts.toMutableMap()
            mutation.boosts.forEach { (stat, delta) ->
                nextBoosts[stat] = (nextBoosts[stat] ?: 0) + delta
            }
            unit.copy(boosts = nextBoosts)
        }

    private fun applyClearBoosts(
        mutation: ClearBoostsMutation,
        context: MutationApplicationContext,
        units: Map<String, UnitState>,
    ): Map<String, UnitState> =
        updateTargets(mutation.target, context, units) { unit ->
            unit.copy(boosts = emptyMap())
        }

    private fun applyConsumeItem(
        mutation: ConsumeItemMutation,
        context: MutationApplicationContext,
        units: Map<String, UnitState>,
    ): Map<String, UnitState> =
        updateTargets(mutation.target, context, units) { unit ->
            unit.copy(itemId = null)
        }

    private fun applyRestorePp(
        mutation: RestorePpMutation,
        context: MutationApplicationContext,
        units: Map<String, UnitState>,
    ): Map<String, UnitState> =
        updateTargets(mutation.target, context, units) { unit ->
            if (mutation.moveId == null) {
                unit
            } else {
                val nextPp = unit.movePp.toMutableMap()
                nextPp[mutation.moveId] = (nextPp[mutation.moveId] ?: 0) + mutation.value
                unit.copy(movePp = nextPp)
            }
        }

    private fun applyChangeType(
        mutation: ChangeTypeMutation,
        context: MutationApplicationContext,
        units: Map<String, UnitState>,
    ): Map<String, UnitState> =
        updateTargets(mutation.target, context, units) { unit ->
            unit.copy(typeIds = mutation.values.toSet())
        }

    private fun applyForceSwitch(
        mutation: ForceSwitchMutation,
        context: MutationApplicationContext,
        units: Map<String, UnitState>,
    ): Map<String, UnitState> =
        updateTargets(mutation.target, context, units) { unit ->
            unit.copy(forceSwitchRequested = true)
        }

    private fun applyAddCondition(
        mutation: ApplyConditionMutation,
        context: MutationApplicationContext,
        units: Map<String, UnitState>,
    ): Map<String, UnitState> =
        updateTargets(mutation.target, context, units) { unit ->
            unit.copy(conditionIds = unit.conditionIds + mutation.conditionId)
        }

    private fun applyRemoveCondition(
        mutation: RemoveConditionMutation,
        context: MutationApplicationContext,
        units: Map<String, UnitState>,
    ): Map<String, UnitState> =
        updateTargets(mutation.target, context, units) { unit ->
            unit.copy(conditionIds = unit.conditionIds - mutation.conditionId)
        }

    private fun applySetFlag(
        mutation: SetFlagMutation,
        context: MutationApplicationContext,
        units: Map<String, UnitState>,
    ): Map<String, UnitState> =
        updateTargets(mutation.target, context, units) { unit ->
            unit.copy(flags = unit.flags + (mutation.key to mutation.value))
        }

    private fun applyClearFlag(
        mutation: ClearFlagMutation,
        context: MutationApplicationContext,
        units: Map<String, UnitState>,
    ): Map<String, UnitState> =
        updateTargets(mutation.target, context, units) { unit ->
            unit.copy(flags = unit.flags - mutation.key)
        }

    private fun updateTargets(
        selector: io.github.lishangbu.avalon.game.battle.engine.type.TargetSelectorId,
        context: MutationApplicationContext,
        units: Map<String, UnitState>,
        transform: (UnitState) -> UnitState,
    ): Map<String, UnitState> {
        val targetIds = MutationTargetSelectorResolver.resolve(selector, context)
        if (targetIds.isEmpty()) {
            return units
        }
        val nextUnits = units.toMutableMap()
        targetIds.forEach { unitId ->
            val unit = requireNotNull(nextUnits[unitId]) { "Target unit '$unitId' was not found." }
            nextUnits[unitId] = transform(unit)
        }
        return nextUnits
    }

    private fun calculateAmount(
        maxHp: Int,
        mode: String?,
        value: Double,
    ): Int =
        when (mode) {
            "max_hp_ratio" -> (maxHp * value).toInt()
            else -> value.toInt()
        }
}
