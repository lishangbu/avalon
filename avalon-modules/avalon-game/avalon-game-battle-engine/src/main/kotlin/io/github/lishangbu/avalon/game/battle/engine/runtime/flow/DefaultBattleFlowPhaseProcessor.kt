package io.github.lishangbu.avalon.game.battle.engine.runtime.flow

import io.github.lishangbu.avalon.game.battle.engine.dsl.EffectDefinition
import io.github.lishangbu.avalon.game.battle.engine.event.EventContext
import io.github.lishangbu.avalon.game.battle.engine.event.StandardHookNames
import io.github.lishangbu.avalon.game.battle.engine.mutation.BattleMutation
import io.github.lishangbu.avalon.game.battle.engine.repository.EffectDefinitionRepository
import io.github.lishangbu.avalon.game.battle.engine.runtime.HookRuleProcessor
import io.github.lishangbu.avalon.game.battle.engine.runtime.apply.MutationApplicationContext
import io.github.lishangbu.avalon.game.battle.engine.runtime.apply.MutationApplier

/**
 * 默认 battle hook phase 处理器。
 *
 * @property effectRepository effect 定义查询入口。
 * @property hookRuleProcessor 单条 hook rule 处理器。
 * @property mutationApplier mutation 写回组件。
 * @property mutationInterceptorChain mutation 拦截链。
 */
class DefaultBattleFlowPhaseProcessor(
    private val effectRepository: EffectDefinitionRepository,
    private val hookRuleProcessor: HookRuleProcessor,
    private val mutationApplier: MutationApplier,
    private val mutationInterceptorChain: BattleMutationInterceptorChain,
) : BattleFlowPhaseProcessor {
    /**
     * 处理一次完整的 hook phase。
     */
    override fun processPhase(
        snapshot: BattleRuntimeSnapshot,
        hookName: String,
        moveEffect: EffectDefinition,
        selfId: String,
        targetId: String,
        sourceId: String,
        relay: Any?,
        attributes: Map<String, Any?>,
    ): HookPhaseResult {
        var currentResult =
            processEffectHook(
                snapshot = snapshot,
                hookName = hookName,
                effect = moveEffect,
                selfId = selfId,
                targetId = targetId,
                sourceId = sourceId,
                relay = relay,
                attributes = attributes,
            )
        if (currentResult.cancelled) {
            return currentResult
        }

        val attachmentOrder =
            attachmentOrderForPhase(
                hookName = hookName,
                selfId = selfId,
                targetId = targetId,
            )
        attachmentOrder.forEach { unitId ->
            currentResult =
                processAttachedEffects(
                    snapshot = currentResult.snapshot,
                    unitId = unitId,
                    hookName = hookName,
                    targetId = targetId,
                    sourceId = sourceId,
                    relay = currentResult.relay,
                    attributes = attributes,
                )
            if (currentResult.cancelled) {
                return currentResult
            }
        }
        return currentResult
    }

    /**
     * 处理某个单位上挂载 effect 的指定 hook。
     */
    override fun processAttachedEffects(
        snapshot: BattleRuntimeSnapshot,
        unitId: String,
        hookName: String,
        targetId: String?,
        sourceId: String?,
        relay: Any?,
        attributes: Map<String, Any?>,
    ): HookPhaseResult {
        var currentResult = HookPhaseResult(snapshot = snapshot, cancelled = false, relay = relay)
        val unit = requireUnit(currentResult.snapshot, unitId)
        val effectIds =
            buildList {
                unit.abilityId?.let(::add)
                unit.itemId?.let(::add)
                unit.statusId?.let(::add)
                addAll(unit.conditionIds)
                addAll(unit.volatileIds)
            }

        effectIds.forEach { effectId ->
            if (!effectRepository.contains(effectId)) {
                return@forEach
            }
            currentResult =
                processEffectHook(
                    snapshot = currentResult.snapshot,
                    hookName = hookName,
                    effect = effectRepository.get(effectId),
                    selfId = unitId,
                    targetId = targetId,
                    sourceId = sourceId,
                    relay = currentResult.relay,
                    attributes = attributes,
                )
            if (currentResult.cancelled) {
                return currentResult
            }
        }
        return currentResult
    }

    /**
     * 处理某个 effect 自身的指定 hook。
     */
    private fun processEffectHook(
        snapshot: BattleRuntimeSnapshot,
        hookName: String,
        effect: EffectDefinition,
        selfId: String?,
        targetId: String?,
        sourceId: String?,
        relay: Any?,
        attributes: Map<String, Any?>,
    ): HookPhaseResult {
        val rules =
            effect.hooks.entries
                .firstOrNull { entry -> entry.key.value == hookName }
                ?.value
                .orEmpty()
        if (rules.isEmpty()) {
            return HookPhaseResult(snapshot = snapshot, cancelled = false, relay = relay)
        }

        var currentSnapshot = snapshot
        var currentRelay: Any? = relay
        var cancelled = false

        val sortedRules =
            rules.sortedWith(
                compareByDescending<io.github.lishangbu.avalon.game.battle.engine.dsl.HookRule> { rule -> rule.priority }
                    .thenByDescending { rule -> rule.subOrder },
            )
        for (rule in sortedRules) {
            val context =
                EventContext(
                    hookName =
                        io.github.lishangbu.avalon.game.battle.engine.type
                            .HookName(hookName),
                    battle = currentSnapshot.battle,
                    self = selfId?.let { id -> currentSnapshot.units[id] },
                    target = targetId?.let { id -> currentSnapshot.units[id] },
                    source = sourceId?.let { id -> currentSnapshot.units[id] },
                    field = currentSnapshot.field,
                    effect = effect,
                    relay = currentRelay,
                    attributes = attributes,
                )
            val ruleResult = hookRuleProcessor.process(rule, context)
            currentRelay = ruleResult.relay
            currentSnapshot = applyMutations(currentSnapshot, selfId, targetId, sourceId, ruleResult.mutations)
            if (ruleResult.cancelled) {
                cancelled = true
                break
            }
        }

        return HookPhaseResult(
            snapshot = currentSnapshot,
            cancelled = cancelled,
            relay = currentRelay,
        )
    }

    /**
     * 把一批 mutation 写回当前 battle 快照。
     */
    private fun applyMutations(
        snapshot: BattleRuntimeSnapshot,
        selfId: String?,
        targetId: String?,
        sourceId: String?,
        mutations: List<BattleMutation>,
    ): BattleRuntimeSnapshot {
        if (mutations.isEmpty()) {
            return snapshot
        }
        val filteredResult =
            mutationInterceptorChain.filter(
                snapshot = snapshot,
                selfId = selfId,
                targetId = targetId,
                sourceId = sourceId,
                mutations = mutations,
                attachedEffectProcessor =
                    BattleAttachedEffectProcessor { attachedSnapshot, unitId, hookName, attachedTargetId, attachedSourceId, relay, attributes ->
                        processAttachedEffects(
                            snapshot = attachedSnapshot,
                            unitId = unitId,
                            hookName = hookName,
                            targetId = attachedTargetId,
                            sourceId = attachedSourceId,
                            relay = relay,
                            attributes = attributes,
                        )
                    },
            )
        val applyResult =
            mutationApplier.apply(
                mutations = filteredResult.mutations,
                context =
                    MutationApplicationContext(
                        battle = filteredResult.snapshot.battle,
                        field = filteredResult.snapshot.field,
                        units = filteredResult.snapshot.units,
                        selfId = selfId,
                        targetId = targetId,
                        sourceId = sourceId,
                    ),
            )
        return filteredResult.snapshot.copy(
            battle = applyResult.battle,
            field = applyResult.field,
            units = applyResult.units,
        )
    }

    /**
     * 返回指定 phase 下挂载 effect 的处理顺序。
     */
    private fun attachmentOrderForPhase(
        hookName: String,
        selfId: String,
        targetId: String,
    ): List<String> =
        when (hookName) {
            StandardHookNames.ON_MODIFY_EVASION.value -> listOf(targetId)
            StandardHookNames.ON_MODIFY_ACCURACY.value -> listOf(selfId)
            StandardHookNames.ON_MODIFY_BASE_POWER.value -> listOf(selfId)
            StandardHookNames.ON_MODIFY_DAMAGE.value -> listOf(selfId, targetId)
            StandardHookNames.ON_BEFORE_MOVE.value -> listOf(selfId)
            StandardHookNames.ON_TRY_MOVE.value -> listOf(selfId)
            StandardHookNames.ON_AFTER_MOVE.value -> listOf(selfId)
            else -> listOf(selfId, targetId)
        }

    /**
     * 从当前快照中读取指定单位状态。
     */
    private fun requireUnit(
        snapshot: BattleRuntimeSnapshot,
        unitId: String,
    ) = requireNotNull(snapshot.units[unitId]) { "Unit '$unitId' was not found in snapshot." }
}
