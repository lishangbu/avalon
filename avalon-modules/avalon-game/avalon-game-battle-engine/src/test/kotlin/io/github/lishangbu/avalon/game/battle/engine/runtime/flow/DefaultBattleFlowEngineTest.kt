package io.github.lishangbu.avalon.game.battle.engine.runtime.flow

import io.github.lishangbu.avalon.game.battle.engine.loader.JsonEffectDefinitionBattleDataLoader
import io.github.lishangbu.avalon.game.battle.engine.model.BattleState
import io.github.lishangbu.avalon.game.battle.engine.model.FieldState
import io.github.lishangbu.avalon.game.battle.engine.model.UnitState
import io.github.lishangbu.avalon.game.battle.engine.registry.memory.StandardActionExecutorRegistryFactory
import io.github.lishangbu.avalon.game.battle.engine.registry.memory.StandardConditionInterpreterRegistryFactory
import io.github.lishangbu.avalon.game.battle.engine.repository.memory.InMemoryEffectDefinitionRepository
import io.github.lishangbu.avalon.game.battle.engine.runtime.DefaultHookRuleProcessor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 关注点：
 * - `DefaultBattleFlowEngine` 是否按 battle 阶段顺序消费 effect 定义。
 *
 * 这类测试是 battle 主流程层面的集成测试，覆盖阶段化结算、命中链、状态拦截和 residual。
 */
class DefaultBattleFlowEngineTest {
    private val effects =
        JsonEffectDefinitionBattleDataLoader(
            resourcePaths =
                listOf(
                    "data/battle/fixtures/move/thunderbolt.json",
                    "data/battle/fixtures/ability/static.json",
                    "data/battle/fixtures/ability/speed-boost.json",
                    "data/battle/fixtures/item/sitrus-berry.json",
                    "data/battle/fixtures/move/thunder-wave.json",
                    "data/battle/fixtures/ability/tangled-feet.json",
                    "data/battle/fixtures/move/electro-ball.json",
                    "data/battle/fixtures/move/will-o-wisp.json",
                    "data/battle/fixtures/move/teeter-dance.json",
                    "data/battle/fixtures/ability/limber.json",
                    "data/battle/fixtures/ability/water-veil.json",
                    "data/battle/fixtures/ability/own-tempo.json",
                    "data/battle/fixtures/ability/synchronize.json",
                    "data/battle/fixtures/move/supersonic.json",
                    "data/battle/fixtures/move/thunder-wave-immunity.json",
                    "data/battle/fixtures/ability/adaptability.json",
                    "data/battle/fixtures/ability/guts.json",
                    "data/battle/fixtures/move/slash.json",
                    "data/battle/fixtures/ability/super-luck.json",
                    "data/battle/fixtures/ability/battle-armor.json",
                ),
        ).loadEffects()

    private val repository = InMemoryEffectDefinitionRepository(effects.associateBy { it.id })

    private val flow = createFlow(repository)

    /**
     * 创建一个测试专用的 `DefaultBattleFlowEngine`。
     */
    private fun createFlow(repository: InMemoryEffectDefinitionRepository): DefaultBattleFlowEngine {
        val hookRuleProcessor =
            DefaultHookRuleProcessor(
                conditionInterpreterRegistry = StandardConditionInterpreterRegistryFactory.create(),
                actionExecutorRegistry = StandardActionExecutorRegistryFactory.create(),
            )
        val mutationInterceptorChain =
            DefaultBattleMutationInterceptorChain(
                listOf(
                    SetStatusBattleMutationInterceptor(),
                    AddVolatileBattleMutationInterceptor(),
                ),
            )
        val phaseProcessor =
            DefaultBattleFlowPhaseProcessor(
                effectRepository = repository,
                hookRuleProcessor = hookRuleProcessor,
                mutationApplier =
                    io.github.lishangbu.avalon.game.battle.engine.runtime.apply
                        .DefaultMutationApplier(),
                mutationInterceptorChain = mutationInterceptorChain,
            )
        val pipeline =
            DefaultBattleMoveResolutionPipeline(
                steps =
                    listOf(
                        BattleMovePreHitPhaseStep(phaseProcessor),
                        BattleMoveAccuracyEvasionPhaseStep(phaseProcessor),
                        BattleMoveHitResolutionStep(DefaultBattleHitResolutionPolicy()),
                        BattleMoveCriticalHitPhaseStep(phaseProcessor),
                        BattleMovePowerDamagePhaseStep(phaseProcessor),
                        BattleMoveHitHooksPhaseStep(phaseProcessor),
                        BattleMoveAfterMovePhaseStep(phaseProcessor),
                    ),
            )
        return DefaultBattleFlowEngine(
            effectRepository = repository,
            phaseProcessor = phaseProcessor,
            moveResolutionPipeline = pipeline,
        )
    }

    /**
     * 验证：
     * - battle flow 能同时消费招式自身的 `on_hit` 和目标 ability 的反应式 `on_hit`。
     *
     * 这里用 `thunderbolt + static` 验证：
     * - 目标先被十万伏特麻痹
     * - 来源再被静电反弹麻痹
     */
    @Test
    fun shouldResolveMoveHitWhenMoveAndReactiveAbilityBothApply() {
        val attacker = UnitState(id = "p1a", currentHp = 120, maxHp = 120)
        val target = UnitState(id = "p2a", currentHp = 100, maxHp = 100, abilityId = "static")
        val snapshot =
            BattleRuntimeSnapshot(
                battle = BattleState(id = "battle-flow-1", formatId = "singles", turn = 3),
                field = FieldState(),
                units =
                    mapOf(
                        attacker.id to attacker,
                        target.id to target,
                    ),
            )

        val resolved =
            flow.resolveMoveHit(
                snapshot = snapshot,
                moveId = "thunderbolt",
                attackerId = attacker.id,
                targetId = target.id,
                attributes =
                    mapOf(
                        "chanceRoll" to 7,
                        "targetRelation" to "foe",
                    ),
            )

        assertEquals("par", resolved.units.getValue("p2a").statusId)
        assertEquals("par", resolved.units.getValue("p1a").statusId)
    }

    /**
     * 验证：
     * - residual phase 会遍历单位上挂载的多个 effect，并把它们顺序应用到 battle snapshot。
     *
     * 这里用 `speed-boost + sitrus-berry` 验证：
     * - 同一 residual phase 中既能加速又能回血并消耗道具
     */
    @Test
    fun shouldResolveResidualPhaseWhenAbilityAndItemBothApply() {
        val unit =
            UnitState(
                id = "p1a",
                currentHp = 50,
                maxHp = 100,
                abilityId = "speed-boost",
                itemId = "sitrus-berry",
            )
        val snapshot =
            BattleRuntimeSnapshot(
                battle = BattleState(id = "battle-flow-2", formatId = "singles", turn = 4),
                field = FieldState(),
                units = mapOf(unit.id to unit),
            )

        val resolved = flow.resolveResidualPhase(snapshot)
        val updatedUnit = resolved.units.getValue("p1a")

        assertEquals(75, updatedUnit.currentHp)
        assertNull(updatedUnit.itemId)
        assertEquals(1, updatedUnit.boosts["spe"])
    }

    /**
     * 验证：
     * - battle flow 在 move action 中会保留 accuracy/evasion 的修正值，并完成 `thunder-wave` 的状态附加。
     *
     * 这条测试是主流程版的“命中后附加状态”基线。
     */
    @Test
    fun shouldResolveMoveActionWhenAccuracyAndStatusApplicationBothApply() {
        val attacker = UnitState(id = "p1a", currentHp = 120, maxHp = 120)
        val target = UnitState(id = "p2a", currentHp = 100, maxHp = 100, typeIds = setOf("water"))
        val snapshot =
            BattleRuntimeSnapshot(
                battle = BattleState(id = "battle-flow-3", formatId = "singles", turn = 2),
                field = FieldState(),
                units =
                    mapOf(
                        attacker.id to attacker,
                        target.id to target,
                    ),
            )

        val resolved =
            flow.resolveMoveAction(
                snapshot = snapshot,
                moveId = "thunder-wave",
                attackerId = attacker.id,
                targetId = target.id,
                accuracy = 90,
                evasion = 100,
                basePower = 0,
                damage = 0,
            )

        assertEquals(90, resolved.accuracy)
        assertEquals(100, resolved.evasion)
        assertEquals(
            "par",
            resolved.snapshot.units
                .getValue("p2a")
                .statusId,
        )
    }

    /**
     * 验证：
     * - battle flow 会在 move action 中处理 evasion 与 base power 的修正 phase。
     *
     * 这里用 `electro-ball + tangled-feet` 验证：
     * - 目标回避被提升
     * - 招式威力被提升
     * - 最终这些修正值被保留在 MoveResolutionResult 中
     */
    @Test
    fun shouldResolveMoveActionWhenEvasionAndBasePowerModifiersBothApply() {
        val attacker =
            UnitState(
                id = "p1a",
                currentHp = 120,
                maxHp = 120,
                stats = mapOf("spe" to 180),
            )
        val target =
            UnitState(
                id = "p2a",
                currentHp = 100,
                maxHp = 100,
                abilityId = "tangled-feet",
                volatileIds = setOf("confusion"),
            )
        val snapshot =
            BattleRuntimeSnapshot(
                battle = BattleState(id = "battle-flow-4", formatId = "singles", turn = 4),
                field = FieldState(),
                units =
                    mapOf(
                        attacker.id to attacker,
                        target.id to target,
                    ),
            )

        val resolved =
            flow.resolveMoveAction(
                snapshot = snapshot,
                moveId = "electro-ball",
                attackerId = attacker.id,
                targetId = target.id,
                accuracy = 100,
                evasion = 100,
                basePower = 100,
                damage = 100,
            )

        assertEquals(100, resolved.accuracy)
        assertEquals(200, resolved.evasion)
        assertEquals(150, resolved.basePower)
        assertEquals(100, resolved.damage)
        assertTrue(resolved.hitSuccessful)
    }

    /**
     * 验证：
     * - battle flow 会原生计算基础 STAB，而不是要求调用方预先把 STAB 乘进 `damage`。
     */
    @Test
    fun shouldApplyDefaultStabDuringMoveActionWhenMoveTypeMatchesAttackerType() {
        val attacker =
            UnitState(
                id = "p1a",
                currentHp = 120,
                maxHp = 120,
                typeIds = setOf("electric"),
            )
        val target =
            UnitState(
                id = "p2a",
                currentHp = 100,
                maxHp = 100,
                typeIds = setOf("water"),
            )
        val snapshot =
            BattleRuntimeSnapshot(
                battle = BattleState(id = "battle-flow-stab-1", formatId = "singles", turn = 4),
                field = FieldState(),
                units = mapOf(attacker.id to attacker, target.id to target),
            )

        val resolved =
            flow.resolveMoveAction(
                snapshot = snapshot,
                moveId = "thunderbolt",
                attackerId = attacker.id,
                targetId = target.id,
                accuracy = 100,
                evasion = 100,
                basePower = 90,
                damage = 100,
                attributes = mapOf("chanceRoll" to 80),
            )

        assertEquals(150, resolved.damage)
        assertTrue(resolved.hitSuccessful)
    }

    /**
     * 验证：
     * - 攻击方挂载的 `on_modify_stab` effect 可以在默认 STAB 之上继续改写倍率。
     */
    @Test
    fun shouldApplyAdaptabilityOnTopOfDefaultStabDuringMoveAction() {
        val attacker =
            UnitState(
                id = "p1a",
                currentHp = 120,
                maxHp = 120,
                abilityId = "adaptability",
                typeIds = setOf("electric"),
            )
        val target =
            UnitState(
                id = "p2a",
                currentHp = 100,
                maxHp = 100,
                typeIds = setOf("water"),
            )
        val snapshot =
            BattleRuntimeSnapshot(
                battle = BattleState(id = "battle-flow-stab-2", formatId = "singles", turn = 4),
                field = FieldState(),
                units = mapOf(attacker.id to attacker, target.id to target),
            )

        val resolved =
            flow.resolveMoveAction(
                snapshot = snapshot,
                moveId = "thunderbolt",
                attackerId = attacker.id,
                targetId = target.id,
                accuracy = 100,
                evasion = 100,
                basePower = 90,
                damage = 100,
                attributes = mapOf("chanceRoll" to 80),
            )

        assertEquals(200, resolved.damage)
        assertTrue(resolved.hitSuccessful)
    }

    /**
     * 验证：
     * - 高要害率招式会在 battle flow 中触发要害判定并乘上现代 1.5 倍倍率。
     */
    @Test
    fun shouldApplyCriticalHitDamageWhenHighCriticalMoveSucceeds() {
        val attacker = UnitState(id = "p1a", currentHp = 120, maxHp = 120)
        val target = UnitState(id = "p2a", currentHp = 100, maxHp = 100)
        val snapshot =
            BattleRuntimeSnapshot(
                battle = BattleState(id = "battle-flow-crit-1", formatId = "singles", turn = 4),
                field = FieldState(),
                units = mapOf(attacker.id to attacker, target.id to target),
            )

        val resolved =
            flow.resolveMoveAction(
                snapshot = snapshot,
                moveId = "slash",
                attackerId = attacker.id,
                targetId = target.id,
                accuracy = 100,
                evasion = 100,
                basePower = 70,
                damage = 100,
                attributes = mapOf("criticalRoll" to 0),
            )

        assertTrue(resolved.hitSuccessful)
        assertTrue(resolved.criticalHit)
        assertEquals(150, resolved.damage)
    }

    /**
     * 验证：
     * - 攻击方挂载的 `on_modify_crit_ratio` effect 可以把普通招式提升到会判定要害的等级。
     */
    @Test
    fun shouldApplyCriticalHitWhenAttackerHasSuperLuck() {
        val attacker =
            UnitState(
                id = "p1a",
                currentHp = 120,
                maxHp = 120,
                abilityId = "super-luck",
            )
        val target = UnitState(id = "p2a", currentHp = 100, maxHp = 100)
        val snapshot =
            BattleRuntimeSnapshot(
                battle = BattleState(id = "battle-flow-crit-2", formatId = "singles", turn = 4),
                field = FieldState(),
                units = mapOf(attacker.id to attacker, target.id to target),
            )

        val resolved =
            flow.resolveMoveAction(
                snapshot = snapshot,
                moveId = "thunderbolt",
                attackerId = attacker.id,
                targetId = target.id,
                accuracy = 100,
                evasion = 100,
                basePower = 90,
                damage = 100,
                attributes = mapOf("criticalRoll" to 0, "chanceRoll" to 80),
            )

        assertTrue(resolved.hitSuccessful)
        assertTrue(resolved.criticalHit)
        assertEquals(150, resolved.damage)
    }

    /**
     * 验证：
     * - 当调用方要求 engine 原生计算伤害时，会基于攻击/防御能力值计算基础伤害，
     *   而不是继续依赖外部预估的 `damage` 输入。
     */
    @Test
    fun shouldComputeDamageFromAttackAndDefenseWhenComputeDamageAttributeIsEnabled() {
        val attacker =
            UnitState(
                id = "p1a",
                currentHp = 120,
                maxHp = 120,
                typeIds = setOf("electric"),
                stats = mapOf("special-attack" to 120),
                flags = mapOf("level" to "50"),
            )
        val target =
            UnitState(
                id = "p2a",
                currentHp = 100,
                maxHp = 100,
                typeIds = setOf("water"),
                stats = mapOf("special-defense" to 100),
            )
        val snapshot =
            BattleRuntimeSnapshot(
                battle = BattleState(id = "battle-flow-damage-1", formatId = "singles", turn = 4),
                field = FieldState(),
                units = mapOf(attacker.id to attacker, target.id to target),
            )

        val resolved =
            flow.resolveMoveAction(
                snapshot = snapshot,
                moveId = "thunderbolt",
                attackerId = attacker.id,
                targetId = target.id,
                accuracy = 100,
                evasion = 100,
                basePower = 90,
                damage = 0,
                attributes = mapOf("computeDamage" to true, "chanceRoll" to 80, "criticalHit" to false, "damageRoll" to 100),
            )

        assertTrue(resolved.hitSuccessful)
        assertTrue(!resolved.criticalHit)
        assertEquals(100, resolved.damageRoll)
        assertEquals(73, resolved.damage)
    }

    /**
     * 验证：
     * - 原生伤害计算会应用 85 到 100 的随机浮动区间。
     * - 显式传入 `damageRoll` 时，battle engine 会按该值稳定计算。
     */
    @Test
    fun shouldApplyDamageVarianceDuringNativeDamageCalculation() {
        val attacker =
            UnitState(
                id = "p1a",
                currentHp = 120,
                maxHp = 120,
                typeIds = setOf("electric"),
                stats = mapOf("special-attack" to 120),
                flags = mapOf("level" to "50"),
            )
        val target =
            UnitState(
                id = "p2a",
                currentHp = 100,
                maxHp = 100,
                stats = mapOf("special-defense" to 100),
            )
        val snapshot =
            BattleRuntimeSnapshot(
                battle = BattleState(id = "battle-flow-damage-variance-1", formatId = "singles", turn = 4),
                field = FieldState(),
                units = mapOf(attacker.id to attacker, target.id to target),
            )

        val lowRollResolved =
            flow.resolveMoveAction(
                snapshot = snapshot,
                moveId = "thunderbolt",
                attackerId = attacker.id,
                targetId = target.id,
                accuracy = 100,
                evasion = 100,
                basePower = 90,
                damage = 0,
                attributes = mapOf("computeDamage" to true, "chanceRoll" to 80, "criticalHit" to false, "damageRoll" to 85),
            )
        val highRollResolved =
            flow.resolveMoveAction(
                snapshot = snapshot,
                moveId = "thunderbolt",
                attackerId = attacker.id,
                targetId = target.id,
                accuracy = 100,
                evasion = 100,
                basePower = 90,
                damage = 0,
                attributes = mapOf("computeDamage" to true, "chanceRoll" to 80, "criticalHit" to false, "damageRoll" to 100),
            )

        assertEquals(85, lowRollResolved.damageRoll)
        assertEquals(61, lowRollResolved.damage)
        assertEquals(100, highRollResolved.damageRoll)
        assertEquals(73, highRollResolved.damage)
        assertTrue(highRollResolved.damage > lowRollResolved.damage)
    }

    /**
     * 验证：
     * - 未显式提供 `damageRoll` 时，原生伤害随机数会从 battle RNG 中消费。
     * - 同一初始随机状态重复回放时，伤害结果与推进后的随机状态保持一致。
     */
    @Test
    fun shouldReplayDamageVarianceRngWhenDamageRollIsNotProvided() {
        val attacker =
            UnitState(
                id = "p1a",
                currentHp = 120,
                maxHp = 120,
                typeIds = setOf("electric"),
                stats = mapOf("special-attack" to 120),
                flags = mapOf("level" to "50"),
            )
        val target =
            UnitState(
                id = "p2a",
                currentHp = 100,
                maxHp = 100,
                stats = mapOf("special-defense" to 100),
            )
        val snapshot =
            BattleRuntimeSnapshot(
                battle = BattleState(id = "battle-flow-damage-variance-2", formatId = "singles", turn = 4),
                field = FieldState(),
                units = mapOf(attacker.id to attacker, target.id to target),
            )

        val firstResolved =
            flow.resolveMoveAction(
                snapshot = snapshot,
                moveId = "thunderbolt",
                attackerId = attacker.id,
                targetId = target.id,
                accuracy = 100,
                evasion = 100,
                basePower = 90,
                damage = 0,
                attributes = mapOf("computeDamage" to true, "chanceRoll" to 80, "criticalHit" to false),
            )
        val replayResolved =
            flow.resolveMoveAction(
                snapshot = snapshot.copy(battle = snapshot.battle.copy()),
                moveId = "thunderbolt",
                attackerId = attacker.id,
                targetId = target.id,
                accuracy = 100,
                evasion = 100,
                basePower = 90,
                damage = 0,
                attributes = mapOf("computeDamage" to true, "chanceRoll" to 80, "criticalHit" to false),
            )

        assertEquals(firstResolved.damage, replayResolved.damage)
        assertEquals(firstResolved.damageRoll, replayResolved.damageRoll)
        assertEquals(firstResolved.snapshot.battle.randomState, replayResolved.snapshot.battle.randomState)
        assertTrue(firstResolved.damageRoll != null)
        assertTrue(firstResolved.snapshot.battle.randomState.generatedValueCount > snapshot.battle.randomState.generatedValueCount)
    }

    /**
     * 验证：
     * - 要害命中时，目标的正向防御能力阶级不会继续压低原始伤害。
     */
    @Test
    fun shouldIgnorePositiveDefenseBoostOnCriticalHitWhenComputingDamage() {
        val attacker =
            UnitState(
                id = "p1a",
                currentHp = 120,
                maxHp = 120,
                stats = mapOf("attack" to 120),
                flags = mapOf("level" to "50"),
            )
        val target =
            UnitState(
                id = "p2a",
                currentHp = 100,
                maxHp = 100,
                stats = mapOf("defense" to 100),
                boosts = mapOf("defense" to 6),
            )
        val snapshot =
            BattleRuntimeSnapshot(
                battle = BattleState(id = "battle-flow-damage-2", formatId = "singles", turn = 4),
                field = FieldState(),
                units = mapOf(attacker.id to attacker, target.id to target),
            )

        val normalResolved =
            flow.resolveMoveAction(
                snapshot = snapshot,
                moveId = "slash",
                attackerId = attacker.id,
                targetId = target.id,
                accuracy = 100,
                evasion = 100,
                basePower = 70,
                damage = 0,
                attributes = mapOf("computeDamage" to true, "criticalHit" to false, "damageRoll" to 100),
            )
        val criticalResolved =
            flow.resolveMoveAction(
                snapshot = snapshot,
                moveId = "slash",
                attackerId = attacker.id,
                targetId = target.id,
                accuracy = 100,
                evasion = 100,
                basePower = 70,
                damage = 0,
                attributes = mapOf("computeDamage" to true, "criticalHit" to true, "damageRoll" to 100),
            )

        assertEquals(11, normalResolved.damage)
        assertEquals(57, criticalResolved.damage)
        assertTrue(criticalResolved.damage > normalResolved.damage)
    }

    /**
     * 验证：
     * - `on_modify_attack` 会真正进入原生伤害公式，而不是只停留在 relay 单测。
     */
    @Test
    fun shouldApplyAttackModifierHookInsideNativeDamageFormula() {
        val attacker =
            UnitState(
                id = "p1a",
                currentHp = 120,
                maxHp = 120,
                abilityId = "guts",
                statusId = "brn",
                stats = mapOf("attack" to 120),
                flags = mapOf("level" to "50"),
            )
        val target =
            UnitState(
                id = "p2a",
                currentHp = 100,
                maxHp = 100,
                stats = mapOf("defense" to 100),
            )
        val snapshot =
            BattleRuntimeSnapshot(
                battle = BattleState(id = "battle-flow-damage-3", formatId = "singles", turn = 4),
                field = FieldState(),
                units = mapOf(attacker.id to attacker, target.id to target),
            )

        val resolved =
            flow.resolveMoveAction(
                snapshot = snapshot,
                moveId = "slash",
                attackerId = attacker.id,
                targetId = target.id,
                accuracy = 100,
                evasion = 100,
                basePower = 70,
                damage = 0,
                attributes = mapOf("computeDamage" to true, "criticalHit" to false, "damageRoll" to 100),
            )

        assertEquals(57, resolved.damage)
    }

    /**
     * 验证：
     * - 目标挂载的 `on_modify_crit_ratio` effect 可以阻止本次要害。
     */
    @Test
    fun shouldBlockCriticalHitWhenTargetHasBattleArmor() {
        val attacker = UnitState(id = "p1a", currentHp = 120, maxHp = 120)
        val target =
            UnitState(
                id = "p2a",
                currentHp = 100,
                maxHp = 100,
                abilityId = "battle-armor",
            )
        val snapshot =
            BattleRuntimeSnapshot(
                battle = BattleState(id = "battle-flow-crit-3", formatId = "singles", turn = 4),
                field = FieldState(),
                units = mapOf(attacker.id to attacker, target.id to target),
            )

        val resolved =
            flow.resolveMoveAction(
                snapshot = snapshot,
                moveId = "slash",
                attackerId = attacker.id,
                targetId = target.id,
                accuracy = 100,
                evasion = 100,
                basePower = 70,
                damage = 100,
                attributes = mapOf("criticalRoll" to 0),
            )

        assertTrue(resolved.hitSuccessful)
        assertTrue(!resolved.criticalHit)
        assertEquals(100, resolved.damage)
    }

    /**
     * 验证：
     * - `on_set_status` 能在 battle flow 中拦截状态附加。
     *
     * 这里用 `thunder-wave + limber` 验证：
     * - 电磁波原本会附加麻痹
     * - 目标的柔软特性会阻止这次状态提交
     */
    @Test
    fun shouldBlockThunderWaveStatusApplicationWhenTargetHasLimber() {
        val attacker = UnitState(id = "p1a", currentHp = 120, maxHp = 120)
        val target = UnitState(id = "p2a", currentHp = 100, maxHp = 100, abilityId = "limber", typeIds = setOf("water"))
        val snapshot =
            BattleRuntimeSnapshot(
                battle = BattleState(id = "battle-flow-5", formatId = "singles", turn = 2),
                field = FieldState(),
                units = mapOf(attacker.id to attacker, target.id to target),
            )

        val resolved =
            flow.resolveMoveAction(
                snapshot = snapshot,
                moveId = "thunder-wave",
                attackerId = attacker.id,
                targetId = target.id,
                accuracy = 90,
                evasion = 100,
                basePower = 0,
                damage = 0,
            )

        assertNull(
            resolved.snapshot.units
                .getValue("p2a")
                .statusId,
        )
    }

    /**
     * 验证：
     * - `on_set_status` 拦截不仅适用于麻痹，也适用于灼伤等其它主状态。
     *
     * 这里用 `will-o-wisp + water-veil` 验证：
     * - 鬼火的灼伤不会真正写回目标状态
     */
    @Test
    fun shouldBlockWillOWispStatusApplicationWhenTargetHasWaterVeil() {
        val attacker = UnitState(id = "p1a", currentHp = 120, maxHp = 120)
        val target = UnitState(id = "p2a", currentHp = 100, maxHp = 100, abilityId = "water-veil", typeIds = setOf("grass"))
        val snapshot =
            BattleRuntimeSnapshot(
                battle = BattleState(id = "battle-flow-6", formatId = "singles", turn = 2),
                field = FieldState(),
                units = mapOf(attacker.id to attacker, target.id to target),
            )

        val resolved =
            flow.resolveMoveAction(
                snapshot = snapshot,
                moveId = "will-o-wisp",
                attackerId = attacker.id,
                targetId = target.id,
                accuracy = 85,
                evasion = 100,
                basePower = 0,
                damage = 0,
            )

        assertNull(
            resolved.snapshot.units
                .getValue("p2a")
                .statusId,
        )
    }

    /**
     * 验证：
     * - `on_try_add_volatile` 能在 battle flow 中拦截挥发状态附加。
     *
     * 这里用 `teeter-dance + own-tempo` 验证：
     * - 摇晃舞原本要附加混乱
     * - 目标的我行我素会阻止这次挥发状态提交
     */
    @Test
    fun shouldBlockTeeterDanceConfusionWhenTargetHasOwnTempo() {
        val attacker = UnitState(id = "p1a", currentHp = 120, maxHp = 120)
        val target = UnitState(id = "p2a", currentHp = 100, maxHp = 100, abilityId = "own-tempo")
        val snapshot =
            BattleRuntimeSnapshot(
                battle = BattleState(id = "battle-flow-7", formatId = "doubles", turn = 2),
                field = FieldState(),
                units = mapOf(attacker.id to attacker, target.id to target),
            )

        val resolved =
            flow.resolveMoveAction(
                snapshot = snapshot,
                moveId = "teeter-dance",
                attackerId = attacker.id,
                targetId = target.id,
                accuracy = 100,
                evasion = 100,
                basePower = 0,
                damage = 0,
            )

        assertTrue(
            "confusion" !in
                resolved.snapshot.units
                    .getValue("p2a")
                    .volatileIds,
        )
    }

    /**
     * 验证：
     * - 命中后的后续事件链 `on_after_hit` 已经进入 battle flow，并能消费目标 effect。
     *
     * 这里用 `thunder-wave + synchronize` 验证：
     * - 目标先被麻痹
     * - 之后同步特性在 after-hit 阶段把麻痹返给来源
     */
    @Test
    fun shouldResolveAfterHitFollowUpWhenTargetHasSynchronize() {
        val attacker = UnitState(id = "p1a", currentHp = 120, maxHp = 120)
        val target = UnitState(id = "p2a", currentHp = 100, maxHp = 100, abilityId = "synchronize", typeIds = setOf("water"))
        val snapshot =
            BattleRuntimeSnapshot(
                battle = BattleState(id = "battle-flow-8", formatId = "singles", turn = 2),
                field = FieldState(),
                units = mapOf(attacker.id to attacker, target.id to target),
            )

        val resolved =
            flow.resolveMoveAction(
                snapshot = snapshot,
                moveId = "thunder-wave",
                attackerId = attacker.id,
                targetId = target.id,
                accuracy = 90,
                evasion = 100,
                basePower = 0,
                damage = 0,
            )

        assertEquals(
            "par",
            resolved.snapshot.units
                .getValue("p2a")
                .statusId,
        )
        assertEquals(
            "par",
            resolved.snapshot.units
                .getValue("p1a")
                .statusId,
        )
    }

    /**
     * 验证：
     * - `on_prepare_hit` 会在真正命中前执行，并且其 mutation 会被提交。
     *
     * 这里用 `supersonic` 验证：
     * - prepare-hit 阶段先写入 `prepared_move` 标记
     * - 命中后再附加混乱
     */
    @Test
    fun shouldResolvePrepareHitWhenSupersonicAppliesConfusion() {
        val attacker = UnitState(id = "p1a", currentHp = 120, maxHp = 120)
        val target = UnitState(id = "p2a", currentHp = 100, maxHp = 100)
        val snapshot =
            BattleRuntimeSnapshot(
                battle = BattleState(id = "battle-flow-9", formatId = "singles", turn = 2),
                field = FieldState(),
                units = mapOf(attacker.id to attacker, target.id to target),
            )

        val resolved =
            flow.resolveMoveAction(
                snapshot = snapshot,
                moveId = "supersonic",
                attackerId = attacker.id,
                targetId = target.id,
                accuracy = 55,
                evasion = 100,
                basePower = 0,
                damage = 0,
            )

        assertEquals(
            "supersonic",
            resolved.snapshot.units
                .getValue("p1a")
                .flags["prepared_move"],
        )
        assertTrue(
            "confusion" in
                resolved.snapshot.units
                    .getValue("p2a")
                    .volatileIds,
        )
        assertTrue(resolved.hitSuccessful)
    }

    /**
     * 验证：
     * - `on_try_hit` 能在命中判定之前直接取消一次出招。
     *
     * 这里用 `thunder-wave-immunity` 验证：
     * - 目标是地面属性时，try-hit 阶段直接失败
     * - 不进入后续状态附加
     */
    @Test
    fun shouldCancelMoveAtTryHitPhaseWhenThunderWaveTargetsGroundType() {
        val attacker = UnitState(id = "p1a", currentHp = 120, maxHp = 120)
        val target = UnitState(id = "p2a", currentHp = 100, maxHp = 100, typeIds = setOf("ground"))
        val snapshot =
            BattleRuntimeSnapshot(
                battle = BattleState(id = "battle-flow-10", formatId = "singles", turn = 2),
                field = FieldState(),
                units = mapOf(attacker.id to attacker, target.id to target),
            )

        val resolved =
            flow.resolveMoveAction(
                snapshot = snapshot,
                moveId = "thunder-wave-immunity",
                attackerId = attacker.id,
                targetId = target.id,
                accuracy = 90,
                evasion = 100,
                basePower = 0,
                damage = 0,
            )

        assertTrue(resolved.cancelled)
        assertTrue(!resolved.hitSuccessful)
        assertNull(
            resolved.snapshot.units
                .getValue("p2a")
                .statusId,
        )
    }

    /**
     * 验证：
     * - accuracy/evasion 的修正最终会进入命中判定层，而不是只停留在数值输出上。
     *
     * 这里通过给定 `accuracyRoll` 验证：
     * - 目标回避被提升后，命中判定返回 miss
     * - miss 时不会错误进入状态附加链
     */
    @Test
    fun shouldReturnMissWhenAccuracyRollFailsAgainstModifiedEvasion() {
        val attacker =
            UnitState(
                id = "p1a",
                currentHp = 120,
                maxHp = 120,
                stats = mapOf("spe" to 180),
            )
        val target =
            UnitState(
                id = "p2a",
                currentHp = 100,
                maxHp = 100,
                abilityId = "tangled-feet",
                volatileIds = setOf("confusion"),
            )
        val snapshot =
            BattleRuntimeSnapshot(
                battle = BattleState(id = "battle-flow-11", formatId = "singles", turn = 4),
                field = FieldState(),
                units = mapOf(attacker.id to attacker, target.id to target),
            )

        val resolved =
            flow.resolveMoveAction(
                snapshot = snapshot,
                moveId = "electro-ball",
                attackerId = attacker.id,
                targetId = target.id,
                accuracy = 100,
                evasion = 100,
                basePower = 100,
                damage = 100,
                attributes = mapOf("accuracyRoll" to 75),
            )

        assertEquals(200, resolved.evasion)
        assertTrue(!resolved.hitSuccessful)
        assertNull(
            resolved.snapshot.units
                .getValue("p2a")
                .statusId,
        )
    }
}
