package io.github.lishangbu.avalon.game.battle.engine.runtime.action

import io.github.lishangbu.avalon.game.battle.engine.runtime.ActionExecutor

/**
 * 第一版标准动作执行器集合。
 */
object StandardActionExecutors {
    fun all(): List<ActionExecutor> =
        listOf(
            DamageActionExecutor(),
            HealActionExecutor(),
            AddStatusActionExecutor(),
            RemoveStatusActionExecutor(),
            AddVolatileActionExecutor(),
            RemoveVolatileActionExecutor(),
            BoostActionExecutor(),
            ClearBoostsActionExecutor(),
            SetWeatherActionExecutor(),
            ClearWeatherActionExecutor(),
            SetTerrainActionExecutor(),
            ClearTerrainActionExecutor(),
            ConsumeItemActionExecutor(),
            RestorePpActionExecutor(),
            ChangeTypeActionExecutor(),
            ForceSwitchActionExecutor(),
            FailMoveActionExecutor(),
            TriggerEventActionExecutor(),
            ApplyConditionActionExecutor(),
            RemoveConditionActionExecutor(),
            ModifyMultiplierActionExecutor(),
            SetFlagActionExecutor(),
            ClearFlagActionExecutor(),
        )
}
