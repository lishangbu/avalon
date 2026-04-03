package io.github.lishangbu.avalon.game.battle.engine.session

import io.github.lishangbu.avalon.game.battle.engine.capture.CaptureContext
import io.github.lishangbu.avalon.game.battle.engine.capture.CaptureFormulaInput
import io.github.lishangbu.avalon.game.battle.engine.capture.DefaultCaptureFormulaService
import io.github.lishangbu.avalon.game.battle.engine.loader.JsonEffectDefinitionBattleDataLoader
import io.github.lishangbu.avalon.game.battle.engine.model.BattleType
import io.github.lishangbu.avalon.game.battle.engine.model.UnitState
import io.github.lishangbu.avalon.game.battle.engine.registry.memory.StandardActionExecutorRegistryFactory
import io.github.lishangbu.avalon.game.battle.engine.registry.memory.StandardConditionInterpreterRegistryFactory
import io.github.lishangbu.avalon.game.battle.engine.repository.EffectDefinitionRepository
import io.github.lishangbu.avalon.game.battle.engine.repository.memory.InMemoryEffectDefinitionRepository
import io.github.lishangbu.avalon.game.battle.engine.runtime.DefaultHookRuleProcessor
import io.github.lishangbu.avalon.game.battle.engine.runtime.apply.DefaultMutationApplier
import io.github.lishangbu.avalon.game.battle.engine.runtime.apply.MutationApplier
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.AddVolatileBattleMutationInterceptor
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleFlowEngine
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleFlowPhaseProcessor
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleMoveAccuracyEvasionPhaseStep
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleMoveAfterMovePhaseStep
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleMoveCriticalHitPhaseStep
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleMoveHitHooksPhaseStep
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleMoveHitResolutionStep
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleMovePowerDamagePhaseStep
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleMovePreHitPhaseStep
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.DefaultBattleFlowEngine
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.DefaultBattleFlowPhaseProcessor
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.DefaultBattleHitResolutionPolicy
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.DefaultBattleMoveResolutionPipeline
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.DefaultBattleMutationInterceptorChain
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.SetStatusBattleMutationInterceptor
import io.github.lishangbu.avalon.game.battle.engine.service.CaptureActionResolver
import io.github.lishangbu.avalon.game.battle.engine.service.CaptureFormulaInputResolver
import io.github.lishangbu.avalon.game.battle.engine.service.DefaultCaptureActionResolver
import io.github.lishangbu.avalon.game.battle.engine.service.UnsupportedCaptureActionResolver
import io.github.lishangbu.avalon.game.battle.engine.session.DefaultBattleSessionActionSortingStrategy
import io.github.lishangbu.avalon.game.battle.engine.session.specification.BattleSessionTargetChoiceSpecification
import io.github.lishangbu.avalon.game.battle.engine.session.specification.BattleSessionValidationResult
import io.github.lishangbu.avalon.game.battle.engine.session.specification.DefaultBattleSessionCaptureChoiceSpecification
import io.github.lishangbu.avalon.game.battle.engine.session.specification.DefaultBattleSessionRunChoiceSpecification
import io.github.lishangbu.avalon.game.battle.engine.session.specification.DefaultBattleSessionTargetChoiceSpecification
import io.github.lishangbu.avalon.game.battle.engine.session.specification.DefaultBattleSessionTurnReadySpecification
import io.github.lishangbu.avalon.game.battle.engine.session.specification.DefaultBattleSessionUnitChoiceSpecification
import io.github.lishangbu.avalon.game.battle.engine.session.target.BattleSessionTargetMode
import io.github.lishangbu.avalon.game.battle.engine.session.target.DefaultBattleSessionAvailableTargetResolver
import io.github.lishangbu.avalon.game.battle.engine.session.target.DefaultBattleSessionTargetModeResolver
import io.github.lishangbu.avalon.game.battle.engine.session.target.DefaultBattleSessionTargetQueryService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 关注点：
 * - `BattleSession` 是否能把当前 battle flow 包装成一个可持续推进的最小会话对象。
 */
class BattleSessionTest {
    private val effects =
        JsonEffectDefinitionBattleDataLoader(
            resourcePaths =
                listOf(
                    "data/battle/fixtures/move/thunderbolt.json",
                    "data/battle/fixtures/move/recover.json",
                    "data/battle/fixtures/move/teeter-dance.json",
                    "data/battle/fixtures/move/surf-rain.json",
                    "data/battle/fixtures/ability/static.json",
                    "data/battle/fixtures/ability/speed-boost.json",
                    "data/battle/fixtures/item/sitrus-berry.json",
                    "data/battle/fixtures/item/oran-berry-use.json",
                    "data/battle/fixtures/move/slash.json",
                    "data/battle/fixtures/ability/super-luck.json",
                    "data/battle/fixtures/ability/battle-armor.json",
                ),
        ).loadEffects()

    private val session =
        createSession(
            effectRepository = InMemoryEffectDefinitionRepository(effects.associateBy { it.id }),
            battleFlowEngine = createFlowEngine(InMemoryEffectDefinitionRepository(effects.associateBy { it.id })),
            battleId = "session-1",
            formatId = "singles",
        )

    /**
     * 创建一个测试专用的 `BattleSession`。
     *
     * 设计意图：
     * - 让测试可以显式装配 session 依赖，同时避免在每个用例里重复展开完整构造参数。
     */
    private fun createSession(
        effectRepository: EffectDefinitionRepository,
        battleFlowEngine: BattleFlowEngine,
        battleId: String,
        formatId: String,
        mutationApplier: MutationApplier = DefaultMutationApplier(),
        replacementStrategy: ReplacementStrategy = DefaultReplacementStrategy(),
        captureActionResolver: CaptureActionResolver = UnsupportedCaptureActionResolver(),
        targetChoiceSpecification: BattleSessionTargetChoiceSpecification? = null,
    ): BattleSession {
        val actionExecutionSupport = BattleSessionActionExecutionSupport()
        val commandFactory = DefaultBattleSessionCommandFactory()
        val choiceHandlerRegistry = InMemoryBattleSessionChoiceHandlerRegistry.createDefault(commandFactory)
        val actionHandlerRegistry = InMemoryBattleSessionActionHandlerRegistry.createDefault(actionExecutionSupport)
        val eventPublisher =
            DefaultBattleSessionEventPublisher(
                listOf(
                    BattleSessionBattleLogProjector(),
                    BattleSessionStructuredEventProjector(),
                    BattleSessionResourceLedgerProjector(),
                ),
            )
        val targetQueryService =
            DefaultBattleSessionTargetQueryService(
                targetModeResolver = DefaultBattleSessionTargetModeResolver(effectRepository),
                availableTargetResolver = DefaultBattleSessionAvailableTargetResolver(),
            )
        return BattleSession(
            effectRepository = effectRepository,
            battleFlowEngine = battleFlowEngine,
            mutationApplier = mutationApplier,
            replacementStrategy = replacementStrategy,
            captureActionResolver = captureActionResolver,
            actionExecutionSupport = actionExecutionSupport,
            choiceHandlerRegistry = choiceHandlerRegistry,
            actionHandlerRegistry = actionHandlerRegistry,
            turnPipeline = DefaultBattleSessionTurnPipeline.createDefault(),
            turnReadySpecification = DefaultBattleSessionTurnReadySpecification(),
            unitChoiceSpecification = DefaultBattleSessionUnitChoiceSpecification(),
            runChoiceSpecification = DefaultBattleSessionRunChoiceSpecification(),
            targetChoiceSpecification = targetChoiceSpecification ?: DefaultBattleSessionTargetChoiceSpecification(targetQueryService),
            captureChoiceSpecification = DefaultBattleSessionCaptureChoiceSpecification(),
            targetQueryService = targetQueryService,
            actionSortingStrategy = DefaultBattleSessionActionSortingStrategy(),
            eventPublisher = eventPublisher,
            commandFactory = commandFactory,
            battleId = battleId,
            formatId = formatId,
        )
    }

    /**
     * 创建一个测试专用的 `BattleFlowEngine`。
     *
     * 设计意图：
     * - 让测试可以显式复用 runtime flow 的标准装配。
     * - 在重构 `DefaultBattleFlowEngine` 后，避免在每个用例里重复展开 phase processor 与 pipeline。
     */
    private fun createFlowEngine(effectRepository: EffectDefinitionRepository): BattleFlowEngine {
        val hookRuleProcessor =
            DefaultHookRuleProcessor(
                conditionInterpreterRegistry = StandardConditionInterpreterRegistryFactory.create(),
                actionExecutorRegistry = StandardActionExecutorRegistryFactory.create(),
            )
        val mutationApplier = DefaultMutationApplier()
        val mutationInterceptorChain =
            DefaultBattleMutationInterceptorChain(
                listOf(
                    SetStatusBattleMutationInterceptor(),
                    AddVolatileBattleMutationInterceptor(),
                ),
            )
        val phaseProcessor: BattleFlowPhaseProcessor =
            DefaultBattleFlowPhaseProcessor(
                effectRepository = effectRepository,
                hookRuleProcessor = hookRuleProcessor,
                mutationApplier = mutationApplier,
                mutationInterceptorChain = mutationInterceptorChain,
            )
        val moveResolutionPipeline =
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
            effectRepository = effectRepository,
            phaseProcessor = phaseProcessor,
            moveResolutionPipeline = moveResolutionPipeline,
        )
    }

    /**
     * 验证：
     * - session 可以注册 side 与单位，启动后推进出招，再推进回合结束。
     * - 在这个过程中 battle flow 的招式结算和 residual 结算都能真正作用到会话快照。
     */
    @Test
    fun shouldAdvanceBattleSessionWhenUsingMoveAndEndingTurn() {
        session.registerSide("p1")
        session.registerSide("p2")
        session.registerUnit(
            sideId = "p1",
            unit = UnitState(id = "p1a", currentHp = 50, maxHp = 100, abilityId = "speed-boost", itemId = "sitrus-berry"),
        )
        session.registerUnit(
            sideId = "p2",
            unit = UnitState(id = "p2a", currentHp = 100, maxHp = 100, abilityId = "static"),
        )

        val startedSnapshot = session.start()
        assertTrue(startedSnapshot.battle.started)
        assertEquals(1, startedSnapshot.battle.turn)

        val moveResult =
            session.useMove(
                moveId = "thunderbolt",
                attackerId = "p1a",
                targetId = "p2a",
                basePower = 90,
                damage = 90,
                attributes =
                    mapOf(
                        "chanceRoll" to 7,
                        "targetRelation" to "foe",
                    ),
            )
        assertEquals(
            "par",
            moveResult.snapshot.units
                .getValue("p2a")
                .statusId,
        )
        assertEquals(
            "par",
            moveResult.snapshot.units
                .getValue("p1a")
                .statusId,
        )

        val endTurnSnapshot = session.endTurn()
        val selfAfterResidual = endTurnSnapshot.units.getValue("p1a")
        assertEquals(75, selfAfterResidual.currentHp)
        assertEquals(null, selfAfterResidual.itemId)
        assertEquals(1, selfAfterResidual.boosts["spe"])
        assertEquals(2, endTurnSnapshot.battle.turn)
    }

    /**
     * 验证：
     * - session 可以在同一回合内收集多个 move action，并按 FIFO 顺序依次结算。
     * - 队列在执行后会被清空。
     */
    @Test
    fun shouldExecuteQueuedMovesInFifoOrderWhenResolvingQueuedActions() {
        val queuedSession =
            createSession(
                effectRepository = InMemoryEffectDefinitionRepository(effects.associateBy { it.id }),
                battleFlowEngine = createFlowEngine(InMemoryEffectDefinitionRepository(effects.associateBy { it.id })),
                battleId = "session-queue-1",
                formatId = "singles",
            )
        queuedSession.registerSide("p1")
        queuedSession.registerSide("p2")
        queuedSession.registerUnit(
            sideId = "p1",
            unit = UnitState(id = "p1a", currentHp = 120, maxHp = 120),
        )
        queuedSession.registerUnit(
            sideId = "p2",
            unit = UnitState(id = "p2a", currentHp = 50, maxHp = 100),
        )
        queuedSession.start()

        queuedSession.queueMove(
            moveId = "thunderbolt",
            attackerId = "p1a",
            targetId = "p2a",
            basePower = 90,
            damage = 20,
            attributes = mapOf("chanceRoll" to 7, "targetRelation" to "foe"),
        )
        queuedSession.queueMove(
            moveId = "recover",
            attackerId = "p2a",
            targetId = "p2a",
            basePower = 0,
            damage = 0,
        )

        assertEquals(2, queuedSession.pendingActions().size)

        val actionResults = queuedSession.executeQueuedActions()

        assertEquals(2, actionResults.size)
        assertEquals(
            "par",
            actionResults
                .first()
                .snapshot.units
                .getValue("p2a")
                .statusId,
        )
        assertEquals(
            80,
            actionResults
                .last()
                .snapshot.units
                .getValue("p2a")
                .currentHp,
        )
        assertTrue(queuedSession.pendingActions().isEmpty())
    }

    /**
     * 验证：
     * - session 队列不再是单纯 FIFO，而是会先按 priority、再按 speed 排序。
     * - 当两个动作入队顺序与结算顺序不一致时，说明最小排序规则已经生效。
     */
    @Test
    fun shouldExecuteQueuedMovesByPriorityThenSpeedWhenResolvingQueuedActions() {
        val sortedSession =
            createSession(
                effectRepository = InMemoryEffectDefinitionRepository(effects.associateBy { it.id }),
                battleFlowEngine = createFlowEngine(InMemoryEffectDefinitionRepository(effects.associateBy { it.id })),
                battleId = "session-queue-2",
                formatId = "singles",
            )
        sortedSession.registerSide("p1")
        sortedSession.registerSide("p2")
        sortedSession.registerUnit(
            sideId = "p1",
            unit = UnitState(id = "slow", currentHp = 120, maxHp = 120),
        )
        sortedSession.registerUnit(
            sideId = "p2",
            unit = UnitState(id = "fast", currentHp = 50, maxHp = 100),
        )
        sortedSession.start()

        sortedSession.queueMove(
            moveId = "recover",
            attackerId = "fast",
            targetId = "fast",
            priority = 0,
            speed = 50,
            basePower = 0,
            damage = 0,
        )
        sortedSession.queueMove(
            moveId = "thunderbolt",
            attackerId = "slow",
            targetId = "fast",
            priority = 1,
            speed = 10,
            basePower = 90,
            damage = 20,
            attributes = mapOf("chanceRoll" to 7, "targetRelation" to "foe"),
        )

        val actionResults = sortedSession.executeQueuedActions()

        assertEquals(
            "par",
            actionResults
                .first()
                .snapshot.units
                .getValue("fast")
                .statusId,
        )
        assertEquals(
            80,
            actionResults
                .last()
                .snapshot.units
                .getValue("fast")
                .currentHp,
        )
    }

    /**
     * 验证：
     * - session 能识别当前还有哪些 side 没有为本回合提交行动。
     */
    @Test
    fun shouldReportMissingChoiceSidesWhenTurnIsNotReady() {
        val readySession =
            createSession(
                effectRepository = InMemoryEffectDefinitionRepository(effects.associateBy { it.id }),
                battleFlowEngine = createFlowEngine(InMemoryEffectDefinitionRepository(effects.associateBy { it.id })),
                battleId = "session-ready-1",
                formatId = "singles",
            )
        readySession.registerSide("p1")
        readySession.registerSide("p2")
        readySession.registerUnit("p1", UnitState(id = "p1a", currentHp = 120, maxHp = 120))
        readySession.registerUnit("p2", UnitState(id = "p2a", currentHp = 100, maxHp = 100))
        readySession.start()

        assertEquals(listOf("p1", "p2"), readySession.missingChoiceSideIds())
        assertFalse(readySession.isTurnReady())

        readySession.submitMoveChoice(
            moveId = "thunderbolt",
            attackerId = "p1a",
            targetId = "p2a",
            basePower = 90,
            damage = 20,
            attributes = mapOf("chanceRoll" to 7, "targetRelation" to "foe"),
        )

        assertEquals(listOf("p2"), readySession.missingChoiceSideIds())
        assertFalse(readySession.isTurnReady())

        readySession.submitMoveChoice(
            moveId = "recover",
            attackerId = "p2a",
            targetId = "p2a",
            basePower = 0,
            damage = 0,
        )

        assertTrue(readySession.isTurnReady())
        assertEquals(emptyList<String>(), readySession.missingChoiceSideIds())
    }

    /**
     * 验证：
     * - 当某个 side 有多个 active 单位时，session 会按 active 单位粒度报告缺少的行动输入。
     */
    @Test
    fun shouldReportMissingChoiceUnitsWhenNotAllActiveUnitsHaveSubmitted() {
        val multiActiveSession =
            createSession(
                effectRepository = InMemoryEffectDefinitionRepository(effects.associateBy { it.id }),
                battleFlowEngine = createFlowEngine(InMemoryEffectDefinitionRepository(effects.associateBy { it.id })),
                battleId = "session-ready-2",
                formatId = "doubles",
            )
        multiActiveSession.registerSide("p1")
        multiActiveSession.registerSide("p2")
        multiActiveSession.registerUnit("p1", UnitState(id = "p1a", currentHp = 120, maxHp = 120))
        multiActiveSession.registerUnit("p1", UnitState(id = "p1b", currentHp = 120, maxHp = 120))
        multiActiveSession.registerUnit("p2", UnitState(id = "p2a", currentHp = 100, maxHp = 100))
        multiActiveSession.start()

        multiActiveSession.submitMoveChoice(
            moveId = "thunderbolt",
            attackerId = "p1a",
            targetId = "p2a",
            basePower = 90,
            damage = 20,
            attributes = mapOf("chanceRoll" to 7, "targetRelation" to "foe"),
        )
        multiActiveSession.submitMoveChoice(
            moveId = "recover",
            attackerId = "p2a",
            targetId = "p2a",
            basePower = 0,
            damage = 0,
        )

        assertEquals(listOf("p1b"), multiActiveSession.pendingChoiceUnitIds())
        assertFalse(multiActiveSession.isTurnReady())
        assertFalse(multiActiveSession.canResolveTurn())
        assertEquals(listOf("p1"), multiActiveSession.missingChoiceSideIds())
        assertEquals(
            listOf("p1b"),
            multiActiveSession
                .choiceStatuses()
                .first { status -> status.sideId == "p1" }
                .missingUnitIds,
        )
    }

    /**
     * 验证：
     * - `resolveTurn()` 会执行当前回合队列中的全部动作，然后自动推进到回合末 residual。
     */
    @Test
    fun shouldResolveQueuedActionsAndResidualWhenResolvingTurn() {
        val turnSession =
            createSession(
                effectRepository = InMemoryEffectDefinitionRepository(effects.associateBy { it.id }),
                battleFlowEngine = createFlowEngine(InMemoryEffectDefinitionRepository(effects.associateBy { it.id })),
                battleId = "session-turn-1",
                formatId = "singles",
            )
        turnSession.registerSide("p1")
        turnSession.registerSide("p2")
        turnSession.registerUnit(
            sideId = "p1",
            unit = UnitState(id = "p1a", currentHp = 50, maxHp = 100, abilityId = "speed-boost", itemId = "sitrus-berry"),
        )
        turnSession.registerUnit(
            sideId = "p2",
            unit = UnitState(id = "p2a", currentHp = 100, maxHp = 100, abilityId = "static"),
        )
        turnSession.start()
        turnSession.queueMove(
            moveId = "thunderbolt",
            attackerId = "p1a",
            targetId = "p2a",
            basePower = 90,
            damage = 90,
            attributes = mapOf("chanceRoll" to 7, "targetRelation" to "foe"),
        )
        turnSession.queueMove(
            moveId = "recover",
            attackerId = "p2a",
            targetId = "p2a",
            basePower = 0,
            damage = 0,
            attributes =
                mapOf(
                    "chanceRoll" to 100,
                    "targetRelation" to "self",
                ),
        )

        val turnResult = turnSession.resolveTurn()
        val selfAfterTurn = turnResult.snapshot.units.getValue("p1a")
        val targetAfterTurn = turnResult.snapshot.units.getValue("p2a")

        assertEquals(2, turnResult.actionResults.size)
        assertEquals("par", targetAfterTurn.statusId)
        assertEquals("par", selfAfterTurn.statusId)
        assertEquals(60, targetAfterTurn.currentHp)
        assertEquals(75, selfAfterTurn.currentHp)
        assertEquals(1, selfAfterTurn.boosts["spe"])
        assertEquals(2, turnResult.snapshot.battle.turn)
    }

    /**
     * 验证：
     * - 如果当前回合并非所有 side 都提交了行动，`resolveTurn()` 会拒绝执行。
     */
    @Test
    fun shouldRejectResolveTurnWhenRequiredSidesHaveNotSubmittedChoices() {
        val gatedSession =
            createSession(
                effectRepository = InMemoryEffectDefinitionRepository(effects.associateBy { it.id }),
                battleFlowEngine = createFlowEngine(InMemoryEffectDefinitionRepository(effects.associateBy { it.id })),
                battleId = "session-gated-1",
                formatId = "singles",
            )
        gatedSession.registerSide("p1")
        gatedSession.registerSide("p2")
        gatedSession.registerUnit("p1", UnitState(id = "p1a", currentHp = 120, maxHp = 120))
        gatedSession.registerUnit("p2", UnitState(id = "p2a", currentHp = 100, maxHp = 100))
        gatedSession.start()
        gatedSession.submitMoveChoice(
            moveId = "thunderbolt",
            attackerId = "p1a",
            targetId = "p2a",
            basePower = 90,
            damage = 20,
            attributes = mapOf("chanceRoll" to 7, "targetRelation" to "foe"),
        )

        assertThrows(IllegalArgumentException::class.java) {
            gatedSession.resolveTurn()
        }
    }

    /**
     * 验证：
     * - 同一个 active 单位在同一回合内不能重复提交行动。
     */
    @Test
    fun shouldRejectDuplicateChoiceWhenSameUnitSubmitsTwiceInOneTurn() {
        val duplicateSession =
            createSession(
                effectRepository = InMemoryEffectDefinitionRepository(effects.associateBy { it.id }),
                battleFlowEngine = createFlowEngine(InMemoryEffectDefinitionRepository(effects.associateBy { it.id })),
                battleId = "session-duplicate-1",
                formatId = "singles",
            )
        duplicateSession.registerSide("p1")
        duplicateSession.registerSide("p2")
        duplicateSession.registerUnit("p1", UnitState(id = "p1a", currentHp = 120, maxHp = 120))
        duplicateSession.registerUnit("p2", UnitState(id = "p2a", currentHp = 100, maxHp = 100))
        duplicateSession.start()

        duplicateSession.submitMoveChoice(
            moveId = "thunderbolt",
            attackerId = "p1a",
            targetId = "p2a",
            basePower = 90,
            damage = 20,
            attributes = mapOf("chanceRoll" to 7, "targetRelation" to "foe"),
        )

        assertThrows(IllegalArgumentException::class.java) {
            duplicateSession.submitMoveChoice(
                moveId = "recover",
                attackerId = "p1a",
                targetId = "p1a",
                basePower = 0,
                damage = 0,
            )
        }
    }

    /**
     * 验证：
     * - 在多 active 场景下，定向行动不能把己方 active 当作目标。
     */
    @Test
    fun shouldRejectDirectedActionWhenTargetIsAlliedActiveUnit() {
        val doublesSession =
            createSession(
                effectRepository = InMemoryEffectDefinitionRepository(effects.associateBy { it.id }),
                battleFlowEngine = createFlowEngine(InMemoryEffectDefinitionRepository(effects.associateBy { it.id })),
                battleId = "session-target-1",
                formatId = "doubles",
            )
        doublesSession.registerSide("p1")
        doublesSession.registerSide("p2")
        doublesSession.registerUnit("p1", UnitState(id = "p1a", currentHp = 120, maxHp = 120))
        doublesSession.registerUnit("p1", UnitState(id = "p1b", currentHp = 120, maxHp = 120))
        doublesSession.registerUnit("p2", UnitState(id = "p2a", currentHp = 100, maxHp = 100))
        doublesSession.start()

        assertThrows(IllegalArgumentException::class.java) {
            doublesSession.submitMoveChoice(
                moveId = "thunderbolt",
                attackerId = "p1a",
                targetId = "p1b",
                basePower = 90,
                damage = 20,
            )
        }
    }

    /**
     * 验证：
     * - 定向行动的目标必须是当前 active 单位，不能指向 bench。
     */
    @Test
    fun shouldRejectDirectedActionWhenTargetUnitIsNotActive() {
        val targetSession =
            createSession(
                effectRepository = InMemoryEffectDefinitionRepository(effects.associateBy { it.id }),
                battleFlowEngine = createFlowEngine(InMemoryEffectDefinitionRepository(effects.associateBy { it.id })),
                battleId = "session-target-2",
                formatId = "singles",
            )
        targetSession.registerSide("p1")
        targetSession.registerSide("p2")
        targetSession.registerUnit("p1", UnitState(id = "p1a", currentHp = 120, maxHp = 120))
        targetSession.registerUnit("p2", UnitState(id = "p2a", currentHp = 100, maxHp = 100))
        targetSession.registerUnit("p2", UnitState(id = "p2b", currentHp = 100, maxHp = 100), active = false)
        targetSession.start()

        assertThrows(IllegalArgumentException::class.java) {
            targetSession.submitMoveChoice(
                moveId = "thunderbolt",
                attackerId = "p1a",
                targetId = "p2b",
                basePower = 90,
                damage = 20,
            )
        }
    }

    /**
     * 验证：
     * - session 可以替换目标合法性规格，而不必修改 `BattleSessionChoiceValidator` 主体。
     */
    @Test
    fun shouldUseCustomTargetChoiceSpecificationWhenProvided() {
        val targetSession =
            createSession(
                effectRepository = InMemoryEffectDefinitionRepository(effects.associateBy { it.id }),
                battleFlowEngine = createFlowEngine(InMemoryEffectDefinitionRepository(effects.associateBy { it.id })),
                targetChoiceSpecification =
                    object : BattleSessionTargetChoiceSpecification {
                        override fun validate(
                            session: BattleSession,
                            effectId: String,
                            actorUnitId: String,
                            targetUnitId: String,
                        ): BattleSessionValidationResult =
                            BattleSessionValidationResult.rejected(
                                "Custom target choice specification rejected effect '$effectId'.",
                            )
                    },
                battleId = "session-target-spec-1",
                formatId = "singles",
            )
        targetSession.registerSide("p1")
        targetSession.registerSide("p2")
        targetSession.registerUnit("p1", UnitState(id = "p1a", currentHp = 120, maxHp = 120))
        targetSession.registerUnit("p2", UnitState(id = "p2a", currentHp = 100, maxHp = 100))
        targetSession.start()

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                targetSession.submitMoveChoice(
                    moveId = "thunderbolt",
                    attackerId = "p1a",
                    targetId = "p2a",
                    basePower = 90,
                    damage = 20,
                )
            }

        assertEquals(
            "Custom target choice specification rejected effect 'thunderbolt'.",
            exception.message,
        )
    }

    /**
     * 验证：
     * - 当 active 单位在出招后被击倒时，session 会自动把同侧仍存活的后备单位顶上。
     */
    @Test
    fun shouldAutoReplaceFaintedActiveUnitWhenBenchUnitIsAvailable() {
        val replacementSession =
            createSession(
                effectRepository = InMemoryEffectDefinitionRepository(effects.associateBy { it.id }),
                battleFlowEngine = createFlowEngine(InMemoryEffectDefinitionRepository(effects.associateBy { it.id })),
                battleId = "session-replace-1",
                formatId = "singles",
            )
        replacementSession.registerSide("p1")
        replacementSession.registerSide("p2")
        replacementSession.registerUnit(
            sideId = "p1",
            unit = UnitState(id = "p1a", currentHp = 120, maxHp = 120),
        )
        replacementSession.registerUnit(
            sideId = "p2",
            unit = UnitState(id = "p2a", currentHp = 40, maxHp = 100),
        )
        replacementSession.registerUnit(
            sideId = "p2",
            unit = UnitState(id = "p2b", currentHp = 100, maxHp = 100),
            active = false,
        )
        replacementSession.start()

        val result =
            replacementSession.useMove(
                moveId = "thunderbolt",
                attackerId = "p1a",
                targetId = "p2a",
                basePower = 90,
                damage = 50,
                attributes = mapOf("chanceRoll" to 80, "targetRelation" to "foe"),
            )

        assertEquals(
            0,
            result.snapshot.units
                .getValue("p2a")
                .currentHp,
        )
        assertEquals(
            listOf("p2b"),
            result.snapshot.sides
                .getValue("p2")
                .activeUnitIds,
        )
        assertTrue(!result.snapshot.battle.ended)
    }

    /**
     * 验证：
     * - 当某个 side 的最后一个可战单位被击倒时，session 会结束战斗并写入 winner。
     */
    @Test
    fun shouldEndBattleWhenSideHasNoRemainingUnits() {
        val winnerSession =
            createSession(
                effectRepository = InMemoryEffectDefinitionRepository(effects.associateBy { it.id }),
                battleFlowEngine = createFlowEngine(InMemoryEffectDefinitionRepository(effects.associateBy { it.id })),
                battleId = "session-win-1",
                formatId = "singles",
            )
        winnerSession.registerSide("p1")
        winnerSession.registerSide("p2")
        winnerSession.registerUnit(
            sideId = "p1",
            unit = UnitState(id = "p1a", currentHp = 120, maxHp = 120),
        )
        winnerSession.registerUnit(
            sideId = "p2",
            unit = UnitState(id = "p2a", currentHp = 40, maxHp = 100),
        )
        winnerSession.start()

        val result =
            winnerSession.useMove(
                moveId = "thunderbolt",
                attackerId = "p1a",
                targetId = "p2a",
                basePower = 90,
                damage = 50,
                attributes = mapOf("chanceRoll" to 80, "targetRelation" to "foe"),
            )

        assertTrue(result.snapshot.battle.ended)
        assertEquals("p1", result.snapshot.battle.winner)
    }

    /**
     * 验证：
     * - `BattleSession` 会使用注入的替补策略，而不是把“谁顶上”写死在会话主体里。
     */
    @Test
    fun shouldUseInjectedReplacementStrategyWhenSelectingBenchReplacement() {
        val customSession =
            createSession(
                effectRepository = InMemoryEffectDefinitionRepository(effects.associateBy { it.id }),
                battleFlowEngine = createFlowEngine(InMemoryEffectDefinitionRepository(effects.associateBy { it.id })),
                replacementStrategy =
                    object : ReplacementStrategy {
                        override fun selectActiveUnitIds(
                            side: io.github.lishangbu.avalon.game.battle.engine.model.SideState,
                            units: Map<String, UnitState>,
                        ): List<String> {
                            val availableBench =
                                side.unitIds
                                    .filterNot { unitId -> unitId in side.activeUnitIds }
                                    .filter { unitId -> (units[unitId]?.currentHp ?: 0) > 0 }
                            return if (availableBench.isEmpty()) emptyList() else listOf(availableBench.last())
                        }
                    },
                battleId = "session-replace-2",
                formatId = "singles",
            )
        customSession.registerSide("p1")
        customSession.registerSide("p2")
        customSession.registerUnit(
            sideId = "p1",
            unit = UnitState(id = "p1a", currentHp = 120, maxHp = 120),
        )
        customSession.registerUnit(
            sideId = "p2",
            unit = UnitState(id = "p2a", currentHp = 40, maxHp = 100),
        )
        customSession.registerUnit(
            sideId = "p2",
            unit = UnitState(id = "p2b", currentHp = 100, maxHp = 100),
            active = false,
        )
        customSession.registerUnit(
            sideId = "p2",
            unit = UnitState(id = "p2c", currentHp = 100, maxHp = 100),
            active = false,
        )
        customSession.start()

        val result =
            customSession.useMove(
                moveId = "thunderbolt",
                attackerId = "p1a",
                targetId = "p2a",
                basePower = 90,
                damage = 50,
                attributes = mapOf("chanceRoll" to 80, "targetRelation" to "foe"),
            )

        assertEquals(
            listOf("p2c"),
            result.snapshot.sides
                .getValue("p2")
                .activeUnitIds,
        )
    }

    /**
     * 验证：
     * - session 现在可以接收 switch 行动输入并在队列中执行。
     */
    @Test
    fun shouldExecuteQueuedSwitchActionWhenSubmittedByPlayer() {
        val switchSession =
            createSession(
                effectRepository = InMemoryEffectDefinitionRepository(effects.associateBy { it.id }),
                battleFlowEngine = createFlowEngine(InMemoryEffectDefinitionRepository(effects.associateBy { it.id })),
                battleId = "session-switch-1",
                formatId = "singles",
            )
        switchSession.registerSide("p1")
        switchSession.registerUnit("p1", UnitState(id = "p1a", currentHp = 120, maxHp = 120))
        switchSession.registerUnit("p1", UnitState(id = "p1b", currentHp = 100, maxHp = 100), active = false)
        switchSession.start()

        switchSession.submitSwitchChoice(
            sideId = "p1",
            outgoingUnitId = "p1a",
            incomingUnitId = "p1b",
            priority = 6,
            speed = 30,
        )

        val results = switchSession.executeQueuedActions()

        assertEquals(1, results.size)
        assertEquals(
            listOf("p1b"),
            switchSession
                .snapshot()
                .sides
                .getValue("p1")
                .activeUnitIds,
        )
    }

    /**
     * 验证：
     * - session 可以通过统一的 `submitChoice` API 接收 move 输入。
     */
    @Test
    fun shouldAcceptMoveChoiceWhenSubmittingUnifiedChoiceApi() {
        val choiceSession =
            createSession(
                effectRepository = InMemoryEffectDefinitionRepository(effects.associateBy { it.id }),
                battleFlowEngine = createFlowEngine(InMemoryEffectDefinitionRepository(effects.associateBy { it.id })),
                battleId = "session-choice-1",
                formatId = "singles",
            )
        choiceSession.registerSide("p1")
        choiceSession.registerSide("p2")
        choiceSession.registerUnit("p1", UnitState(id = "p1a", currentHp = 120, maxHp = 120))
        choiceSession.registerUnit("p2", UnitState(id = "p2a", currentHp = 100, maxHp = 100))
        choiceSession.start()

        choiceSession.submitChoice(
            MoveChoice(
                moveId = "thunderbolt",
                attackerId = "p1a",
                targetId = "p2a",
                basePower = 90,
                damage = 20,
                attributes = mapOf("chanceRoll" to 7, "targetRelation" to "foe"),
            ),
        )

        val results = choiceSession.executeQueuedActions()

        assertEquals(1, results.size)
        assertEquals(
            "par",
            choiceSession
                .snapshot()
                .units
                .getValue("p2a")
                .statusId,
        )
    }

    /**
     * 验证：
     * - session 可以通过统一的 `submitChoices` API 批量接收多个输入。
     */
    @Test
    fun shouldAcceptMultipleChoicesWhenSubmittingUnifiedChoiceBatch() {
        val choiceSession =
            createSession(
                effectRepository = InMemoryEffectDefinitionRepository(effects.associateBy { it.id }),
                battleFlowEngine = createFlowEngine(InMemoryEffectDefinitionRepository(effects.associateBy { it.id })),
                battleId = "session-choice-2",
                formatId = "singles",
            )
        choiceSession.registerSide("p1")
        choiceSession.registerSide("p2")
        choiceSession.registerUnit("p1", UnitState(id = "p1a", currentHp = 120, maxHp = 120))
        choiceSession.registerUnit("p2", UnitState(id = "p2a", currentHp = 50, maxHp = 100))
        choiceSession.start()

        val pending =
            choiceSession.submitChoices(
                listOf(
                    MoveChoice(
                        moveId = "thunderbolt",
                        attackerId = "p1a",
                        targetId = "p2a",
                        basePower = 90,
                        damage = 20,
                        attributes = mapOf("chanceRoll" to 7, "targetRelation" to "foe"),
                    ),
                    MoveChoice(
                        moveId = "recover",
                        attackerId = "p2a",
                        targetId = "p2a",
                        basePower = 0,
                        damage = 0,
                    ),
                ),
            )

        assertEquals(2, pending.size)
    }

    /**
     * 验证：
     * - session 可以通过统一查询 DTO 一次性暴露当前对外状态。
     */
    @Test
    fun shouldExposeSessionStateThroughUnifiedQueryDto() {
        val querySession =
            createSession(
                effectRepository = InMemoryEffectDefinitionRepository(effects.associateBy { it.id }),
                battleFlowEngine = createFlowEngine(InMemoryEffectDefinitionRepository(effects.associateBy { it.id })),
                battleId = "session-query-1",
                formatId = "singles",
            )
        querySession.registerSide("p1")
        querySession.registerSide("p2")
        querySession.registerUnit("p1", UnitState(id = "p1a", currentHp = 120, maxHp = 120))
        querySession.registerUnit("p2", UnitState(id = "p2a", currentHp = 100, maxHp = 100))
        querySession.start()
        querySession.submitMoveChoice(
            moveId = "thunderbolt",
            attackerId = "p1a",
            targetId = "p2a",
            basePower = 90,
            damage = 20,
            attributes = mapOf("chanceRoll" to 7, "targetRelation" to "foe"),
        )

        val query = querySession.query()

        assertEquals("session-query-1", query.snapshot.battle.id)
        assertEquals(1, query.pendingActions.size)
        assertEquals(2, query.choiceStatuses.size)
        assertTrue(query.battleLogs.isNotEmpty())
        assertTrue(query.eventLogs.isNotEmpty())
        assertTrue(query.eventPayloads.isNotEmpty())
        assertTrue(query.queuedActionEvents.isNotEmpty())
    }

    /**
     * 验证：
     * - session 能根据 effect `data.target` 给出当前可选目标集合。
     */
    @Test
    fun shouldQueryAvailableTargetsByEffectTargetMode() {
        val targetQuerySession =
            createSession(
                effectRepository = InMemoryEffectDefinitionRepository(effects.associateBy { it.id }),
                battleFlowEngine = createFlowEngine(InMemoryEffectDefinitionRepository(effects.associateBy { it.id })),
                battleId = "session-query-targets-1",
                formatId = "doubles",
            )
        targetQuerySession.registerSide("p1")
        targetQuerySession.registerSide("p2")
        targetQuerySession.registerUnit("p1", UnitState(id = "p1a", currentHp = 120, maxHp = 120))
        targetQuerySession.registerUnit("p1", UnitState(id = "p1b", currentHp = 120, maxHp = 120))
        targetQuerySession.registerUnit("p2", UnitState(id = "p2a", currentHp = 100, maxHp = 100))
        targetQuerySession.registerUnit("p2", UnitState(id = "p2b", currentHp = 100, maxHp = 100))
        targetQuerySession.start()

        val recoverTargets = targetQuerySession.queryTargets("recover", "p1a")
        val thunderboltTargets = targetQuerySession.queryTargets("thunderbolt", "p1a")
        val teeterDanceTargets = targetQuerySession.queryTargets("teeter-dance", "p1a")

        assertEquals(BattleSessionTargetMode.SELF, recoverTargets.mode)
        assertEquals(listOf("p1a"), recoverTargets.availableTargetUnitIds)
        assertFalse(recoverTargets.requiresExplicitTarget)

        assertEquals(BattleSessionTargetMode.FOE, thunderboltTargets.mode)
        assertEquals(listOf("p2a", "p2b"), thunderboltTargets.availableTargetUnitIds)
        assertTrue(thunderboltTargets.requiresExplicitTarget)

        assertEquals(BattleSessionTargetMode.ALL_OTHER_POKEMON, teeterDanceTargets.mode)
        assertEquals(listOf("p1b", "p2a", "p2b"), teeterDanceTargets.availableTargetUnitIds)
        assertFalse(teeterDanceTargets.requiresExplicitTarget)
    }

    /**
     * 验证：
     * - 当替补策略不自动给出结果时，session 会生成 replacement request。
     */
    @Test
    fun shouldCreateReplacementRequestWhenStrategyDoesNotAutoReplace() {
        val requestSession =
            createSession(
                effectRepository = InMemoryEffectDefinitionRepository(effects.associateBy { it.id }),
                battleFlowEngine = createFlowEngine(InMemoryEffectDefinitionRepository(effects.associateBy { it.id })),
                replacementStrategy =
                    object : ReplacementStrategy {
                        override fun selectActiveUnitIds(
                            side: io.github.lishangbu.avalon.game.battle.engine.model.SideState,
                            units: Map<String, UnitState>,
                        ): List<String> = side.activeUnitIds
                    },
                battleId = "session-request-1",
                formatId = "singles",
            )
        requestSession.registerSide("p1")
        requestSession.registerSide("p2")
        requestSession.registerUnit("p1", UnitState(id = "p1a", currentHp = 120, maxHp = 120))
        requestSession.registerUnit("p2", UnitState(id = "p2a", currentHp = 40, maxHp = 100))
        requestSession.registerUnit("p2", UnitState(id = "p2b", currentHp = 100, maxHp = 100), active = false)
        requestSession.start()

        requestSession.useMove(
            moveId = "thunderbolt",
            attackerId = "p1a",
            targetId = "p2a",
            basePower = 90,
            damage = 50,
            attributes = mapOf("chanceRoll" to 80, "targetRelation" to "foe"),
        )

        val requests = requestSession.pendingReplacementRequests()

        assertEquals(1, requests.size)
        assertEquals("p2", requests.single().sideId)
        assertEquals(listOf("p2b"), requests.single().candidateUnitIds)
    }

    /**
     * 验证：
     * - session 可以接收玩家对 replacement request 的明确替补选择。
     */
    @Test
    fun shouldApplyManualReplacementChoiceWhenSubmitted() {
        val manualSession =
            createSession(
                effectRepository = InMemoryEffectDefinitionRepository(effects.associateBy { it.id }),
                battleFlowEngine = createFlowEngine(InMemoryEffectDefinitionRepository(effects.associateBy { it.id })),
                replacementStrategy =
                    object : ReplacementStrategy {
                        override fun selectActiveUnitIds(
                            side: io.github.lishangbu.avalon.game.battle.engine.model.SideState,
                            units: Map<String, UnitState>,
                        ): List<String> = side.activeUnitIds
                    },
                battleId = "session-request-2",
                formatId = "singles",
            )
        manualSession.registerSide("p1")
        manualSession.registerSide("p2")
        manualSession.registerUnit("p1", UnitState(id = "p1a", currentHp = 120, maxHp = 120))
        manualSession.registerUnit("p2", UnitState(id = "p2a", currentHp = 40, maxHp = 100))
        manualSession.registerUnit("p2", UnitState(id = "p2b", currentHp = 100, maxHp = 100), active = false)
        manualSession.registerUnit("p2", UnitState(id = "p2c", currentHp = 100, maxHp = 100), active = false)
        manualSession.start()

        manualSession.useMove(
            moveId = "thunderbolt",
            attackerId = "p1a",
            targetId = "p2a",
            basePower = 90,
            damage = 50,
            attributes = mapOf("chanceRoll" to 80, "targetRelation" to "foe"),
        )

        val updatedSnapshot =
            manualSession.submitReplacementChoice(
                sideId = "p2",
                incomingUnitId = "p2c",
            )

        assertEquals(listOf("p2c"), updatedSnapshot.sides.getValue("p2").activeUnitIds)
        assertTrue(manualSession.pendingReplacementRequests().isEmpty())
    }

    /**
     * 验证：
     * - session 可以通过统一输入层接收 item 行动，并让具有效果定义的物品生效。
     */
    @Test
    fun shouldExecuteItemChoiceWhenSubmittedThroughUnifiedApi() {
        val itemSession =
            createSession(
                effectRepository = InMemoryEffectDefinitionRepository(effects.associateBy { it.id }),
                battleFlowEngine = createFlowEngine(InMemoryEffectDefinitionRepository(effects.associateBy { it.id })),
                battleId = "session-item-1",
                formatId = "singles",
            )
        itemSession.registerSide("p1")
        itemSession.registerUnit("p1", UnitState(id = "p1a", currentHp = 40, maxHp = 100))
        itemSession.start()

        itemSession.submitChoice(
            ItemChoice(
                itemId = "oran-berry-use",
                actorUnitId = "p1a",
                targetId = "p1a",
            ),
        )

        itemSession.executeQueuedActions()

        assertEquals(
            50,
            itemSession
                .snapshot()
                .units
                .getValue("p1a")
                .currentHp,
        )
    }

    /**
     * 验证：
     * - session 可以通过统一输入层接收 run 行动，并在执行后结束战斗。
     */
    @Test
    fun shouldExecuteRunChoiceWhenSubmittedThroughUnifiedApi() {
        val runSession =
            createSession(
                effectRepository = InMemoryEffectDefinitionRepository(effects.associateBy { it.id }),
                battleFlowEngine = createFlowEngine(InMemoryEffectDefinitionRepository(effects.associateBy { it.id })),
                battleId = "session-run-1",
                formatId = "singles",
            )
        runSession.registerSide("p1")
        runSession.registerSide("p2")
        runSession.registerUnit("p1", UnitState(id = "p1a", currentHp = 120, maxHp = 120, stats = mapOf("speed" to 200)))
        runSession.registerUnit("p2", UnitState(id = "p2a", currentHp = 120, maxHp = 120, stats = mapOf("speed" to 4)))
        runSession.configureBattle(BattleType.WILD)
        runSession.start()

        runSession.submitChoice(RunChoice(sideId = "p1"))
        runSession.executeQueuedActions()

        assertTrue(runSession.snapshot().battle.ended)
        assertEquals("p2", runSession.snapshot().battle.winner)
        assertEquals("run", runSession.snapshot().battle.endedReason)
    }

    @Test
    fun shouldRejectRunChoiceWhenBattleIsNotWild() {
        val trainerSession =
            createSession(
                effectRepository = InMemoryEffectDefinitionRepository(effects.associateBy { it.id }),
                battleFlowEngine = createFlowEngine(InMemoryEffectDefinitionRepository(effects.associateBy { it.id })),
                battleId = "session-run-trainer-1",
                formatId = "singles",
            )
        trainerSession.registerSide("p1")
        trainerSession.registerSide("p2")
        trainerSession.registerUnit("p1", UnitState(id = "p1a", currentHp = 120, maxHp = 120))
        trainerSession.registerUnit("p2", UnitState(id = "p2a", currentHp = 120, maxHp = 120))
        trainerSession.start()

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                trainerSession.submitChoice(RunChoice(sideId = "p1"))
            }

        assertTrue(exception.message!!.contains("Run is only allowed in wild battles."))
    }

    @Test
    fun shouldFailRunChoiceWhenRunnerIsTrappedAndContinueBattle() {
        val trappedSession =
            createSession(
                effectRepository = InMemoryEffectDefinitionRepository(effects.associateBy { it.id }),
                battleFlowEngine = createFlowEngine(InMemoryEffectDefinitionRepository(effects.associateBy { it.id })),
                battleId = "session-run-trapped-1",
                formatId = "singles",
            )
        trappedSession.registerSide("p1")
        trappedSession.registerSide("p2")
        trappedSession.registerUnit(
            "p1",
            UnitState(
                id = "p1a",
                currentHp = 120,
                maxHp = 120,
                stats = mapOf("speed" to 200),
                conditionIds = setOf("cannot-escape"),
            ),
        )
        trappedSession.registerUnit("p2", UnitState(id = "p2a", currentHp = 120, maxHp = 120, stats = mapOf("speed" to 4)))
        trappedSession.configureBattle(BattleType.WILD)
        trappedSession.start()

        trappedSession.submitChoice(RunChoice(sideId = "p1"))
        trappedSession.submitChoice(WaitChoice(unitId = "p2a"))

        val result = trappedSession.resolveTurn()

        assertTrue(!result.snapshot.battle.ended)
        assertEquals(2, result.snapshot.battle.turn)
        assertEquals(1, result.snapshot.battle.failedRunAttempts["p1"])
        assertTrue(trappedSession.eventLogs().any { event -> event.type == BattleSessionEventType.RUN_FAILED })
    }

    /**
     * 验证：
     * - session 会为关键生命周期节点写入人类可读 battle log。
     */
    @Test
    fun shouldRecordBattleLogsWhenSessionAdvances() {
        val logSession =
            createSession(
                effectRepository = InMemoryEffectDefinitionRepository(effects.associateBy { it.id }),
                battleFlowEngine = createFlowEngine(InMemoryEffectDefinitionRepository(effects.associateBy { it.id })),
                battleId = "session-log-1",
                formatId = "singles",
            )
        logSession.registerSide("p1")
        logSession.registerSide("p2")
        logSession.registerUnit(sideId = "p1", unit = UnitState(id = "p1a", currentHp = 120, maxHp = 120))
        logSession.registerUnit(sideId = "p2", unit = UnitState(id = "p2a", currentHp = 100, maxHp = 100))
        logSession.start()
        logSession.queueMove(
            moveId = "thunderbolt",
            attackerId = "p1a",
            targetId = "p2a",
            basePower = 90,
            damage = 20,
            attributes = mapOf("chanceRoll" to 7, "targetRelation" to "foe"),
        )
        logSession.executeQueuedActions()

        val logs = logSession.battleLogs()

        assertTrue(logs.isNotEmpty())
        assertTrue(logs.any { line -> "Registered side p1" in line })
        assertTrue(logs.any { line -> "Registered unit p1a" in line })
        assertTrue(logs.any { line -> "Queued move thunderbolt" in line })
        assertTrue(logs.any { line -> "Executed move thunderbolt" in line })
    }

    /**
     * 验证：
     * - session 会为关键生命周期节点写入结构化 event log。
     */
    @Test
    fun shouldRecordStructuredEventsWhenSessionAdvances() {
        val eventSession =
            createSession(
                effectRepository = InMemoryEffectDefinitionRepository(effects.associateBy { it.id }),
                battleFlowEngine = createFlowEngine(InMemoryEffectDefinitionRepository(effects.associateBy { it.id })),
                battleId = "session-log-2",
                formatId = "singles",
            )
        eventSession.registerSide("p1")
        eventSession.registerSide("p2")
        eventSession.registerUnit(sideId = "p1", unit = UnitState(id = "p1a", currentHp = 120, maxHp = 120))
        eventSession.registerUnit(sideId = "p2", unit = UnitState(id = "p2a", currentHp = 100, maxHp = 100))
        eventSession.start()
        eventSession.queueMove(
            moveId = "thunderbolt",
            attackerId = "p1a",
            targetId = "p2a",
            basePower = 90,
            damage = 20,
            attributes = mapOf("chanceRoll" to 80, "damageRoll" to 100, "targetRelation" to "foe"),
        )
        eventSession.executeQueuedActions()

        val events = eventSession.eventLogs()
        val moveExecutedPayload =
            events
                .mapNotNull { event -> event.payload as? BattleSessionMoveExecutedPayload }
                .single()

        assertTrue(events.any { event -> event.type == BattleSessionEventType.SESSION_STARTED })
        assertTrue(events.any { event -> event.type == BattleSessionEventType.SIDE_REGISTERED })
        assertTrue(events.any { event -> event.type == BattleSessionEventType.UNIT_REGISTERED })
        assertTrue(events.any { event -> event.type == BattleSessionEventType.MOVE_QUEUED })
        assertTrue(events.any { event -> event.type == BattleSessionEventType.MOVE_EXECUTED })
        assertTrue(events.any { event -> event.payload is BattleSessionMoveQueuedPayload })
        assertTrue(events.any { event -> event.payload is BattleSessionMoveExecutedPayload })
        assertEquals(100, moveExecutedPayload.damageRoll)
        assertEquals(100, events.first { event -> event.payload === moveExecutedPayload }.attributes["damageRoll"])
        assertTrue(eventSession.query().eventPayloads.any { payload -> payload is BattleSessionMoveQueuedPayload })
        assertTrue(eventSession.query().executedActionEvents.any { payload -> payload is BattleSessionMoveExecutedPayload })
    }

    /**
     * 验证：
     * - session 能根据 effect `data.target` 给出当前可选目标集合与目标模式。
     */
    @Test
    fun shouldQueryAvailableTargetsWhenEffectUsesDifferentTargetModes() {
        val targetQuerySession =
            createSession(
                effectRepository = InMemoryEffectDefinitionRepository(effects.associateBy { it.id }),
                battleFlowEngine = createFlowEngine(InMemoryEffectDefinitionRepository(effects.associateBy { it.id })),
                battleId = "session-target-query-1",
                formatId = "doubles",
            )
        targetQuerySession.registerSide("p1")
        targetQuerySession.registerSide("p2")
        targetQuerySession.registerUnit("p1", UnitState(id = "p1a", currentHp = 120, maxHp = 120))
        targetQuerySession.registerUnit("p1", UnitState(id = "p1b", currentHp = 120, maxHp = 120))
        targetQuerySession.registerUnit("p2", UnitState(id = "p2a", currentHp = 100, maxHp = 100))
        targetQuerySession.registerUnit("p2", UnitState(id = "p2b", currentHp = 100, maxHp = 100))
        targetQuerySession.start()

        val recoverTargets = targetQuerySession.queryTargets("recover", "p1a")
        val thunderboltTargets = targetQuerySession.queryTargets("thunderbolt", "p1a")
        val teeterDanceTargets = targetQuerySession.queryTargets("teeter-dance", "p1a")

        assertEquals(BattleSessionTargetMode.SELF, recoverTargets.mode)
        assertEquals(listOf("p1a"), recoverTargets.availableTargetUnitIds)
        assertFalse(recoverTargets.requiresExplicitTarget)

        assertEquals(BattleSessionTargetMode.FOE, thunderboltTargets.mode)
        assertEquals(listOf("p2a", "p2b"), thunderboltTargets.availableTargetUnitIds)
        assertTrue(thunderboltTargets.requiresExplicitTarget)

        assertEquals(BattleSessionTargetMode.ALL_OTHER_POKEMON, teeterDanceTargets.mode)
        assertEquals(listOf("p1b", "p2a", "p2b"), teeterDanceTargets.availableTargetUnitIds)
        assertFalse(teeterDanceTargets.requiresExplicitTarget)
    }

    /**
     * 验证：
     * - 对 `all-opponents` 这类多目标模式，session 会把一次行动展开成多个目标结算。
     */
    @Test
    fun shouldApplyMoveToAllOpponentTargetsWhenEffectTargetsAllFoes() {
        val multiTargetSession =
            createSession(
                effectRepository = InMemoryEffectDefinitionRepository(effects.associateBy { it.id }),
                battleFlowEngine = createFlowEngine(InMemoryEffectDefinitionRepository(effects.associateBy { it.id })),
                battleId = "session-multi-target-1",
                formatId = "doubles",
            )
        multiTargetSession.registerSide("p1")
        multiTargetSession.registerSide("p2")
        multiTargetSession.registerUnit("p1", UnitState(id = "p1a", currentHp = 120, maxHp = 120))
        multiTargetSession.registerUnit("p2", UnitState(id = "p2a", currentHp = 100, maxHp = 100))
        multiTargetSession.registerUnit("p2", UnitState(id = "p2b", currentHp = 100, maxHp = 100))
        multiTargetSession.start()

        multiTargetSession.submitMoveChoice(
            moveId = "surf",
            attackerId = "p1a",
            targetId = "p2a",
            basePower = 90,
            damage = 20,
            attributes = mapOf("targetRelation" to "foe", "criticalHit" to false),
        )
        multiTargetSession.submitMoveChoice(
            moveId = "recover",
            attackerId = "p2a",
            targetId = "p2a",
            basePower = 0,
            damage = 0,
        )
        multiTargetSession.submitMoveChoice(
            moveId = "recover",
            attackerId = "p2b",
            targetId = "p2b",
            basePower = 0,
            damage = 0,
        )

        val results = multiTargetSession.executeQueuedActions()

        assertEquals(
            80,
            results
                .first()
                .snapshot.units
                .getValue("p2a")
                .currentHp,
        )
        assertEquals(
            80,
            results
                .first()
                .snapshot.units
                .getValue("p2b")
                .currentHp,
        )
        assertEquals(
            100,
            multiTargetSession
                .snapshot()
                .units
                .getValue("p2a")
                .currentHp,
        )
        assertEquals(
            100,
            multiTargetSession
                .snapshot()
                .units
                .getValue("p2b")
                .currentHp,
        )
    }

    @Test
    fun shouldReplayGeneratedMoveRandomnessAfterRestoringState() {
        val replaySession =
            createSession(
                effectRepository = InMemoryEffectDefinitionRepository(effects.associateBy { it.id }),
                battleFlowEngine = createFlowEngine(InMemoryEffectDefinitionRepository(effects.associateBy { it.id })),
                battleId = "session-replay-move-1",
                formatId = "singles",
            )
        replaySession.registerSide("p1")
        replaySession.registerSide("p2")
        replaySession.registerUnit("p1", UnitState(id = "p1a", currentHp = 120, maxHp = 120))
        replaySession.registerUnit("p2", UnitState(id = "p2a", currentHp = 100, maxHp = 100))
        replaySession.start()

        val restoredSession =
            createSession(
                effectRepository = InMemoryEffectDefinitionRepository(effects.associateBy { it.id }),
                battleFlowEngine = createFlowEngine(InMemoryEffectDefinitionRepository(effects.associateBy { it.id })),
                battleId = "session-replay-move-restored",
                formatId = "singles",
            )
        restoredSession.restoreState(replaySession.exportState())

        val originalResult =
            replaySession.useMove(
                moveId = "thunderbolt",
                attackerId = "p1a",
                targetId = "p2a",
                basePower = 90,
                damage = 20,
                attributes = mapOf("targetRelation" to "foe"),
            )
        val replayedResult =
            restoredSession.useMove(
                moveId = "thunderbolt",
                attackerId = "p1a",
                targetId = "p2a",
                basePower = 90,
                damage = 20,
                attributes = mapOf("targetRelation" to "foe"),
            )

        assertEquals(originalResult.hitSuccessful, replayedResult.hitSuccessful)
        assertEquals(
            originalResult.snapshot.units
                .getValue("p2a")
                .statusId,
            replayedResult.snapshot.units
                .getValue("p2a")
                .statusId,
        )
        assertEquals(originalResult.snapshot.battle.randomState, replayedResult.snapshot.battle.randomState)
    }

    @Test
    fun shouldReplayCriticalHitRandomnessAfterRestoringState() {
        val replaySession =
            createSession(
                effectRepository = InMemoryEffectDefinitionRepository(effects.associateBy { it.id }),
                battleFlowEngine = createFlowEngine(InMemoryEffectDefinitionRepository(effects.associateBy { it.id })),
                battleId = "session-replay-crit-1",
                formatId = "singles",
            )
        replaySession.registerSide("p1")
        replaySession.registerSide("p2")
        replaySession.registerUnit("p1", UnitState(id = "p1a", currentHp = 120, maxHp = 120))
        replaySession.registerUnit("p2", UnitState(id = "p2a", currentHp = 100, maxHp = 100))
        replaySession.start()

        val restoredSession =
            createSession(
                effectRepository = InMemoryEffectDefinitionRepository(effects.associateBy { it.id }),
                battleFlowEngine = createFlowEngine(InMemoryEffectDefinitionRepository(effects.associateBy { it.id })),
                battleId = "session-replay-crit-restored",
                formatId = "singles",
            )
        restoredSession.restoreState(replaySession.exportState())

        val originalResult =
            replaySession.useMove(
                moveId = "slash",
                attackerId = "p1a",
                targetId = "p2a",
                basePower = 70,
                damage = 100,
            )
        val replayedResult =
            restoredSession.useMove(
                moveId = "slash",
                attackerId = "p1a",
                targetId = "p2a",
                basePower = 70,
                damage = 100,
            )

        assertEquals(originalResult.criticalHit, replayedResult.criticalHit)
        assertEquals(originalResult.damage, replayedResult.damage)
        assertEquals(originalResult.snapshot.battle.randomState, replayedResult.snapshot.battle.randomState)
    }

    @Test
    fun shouldReplayCaptureRandomnessAfterRestoringState() {
        val captureResolver =
            DefaultCaptureActionResolver(
                captureFormulaInputResolver =
                    object : CaptureFormulaInputResolver {
                        override fun resolve(
                            sessionId: String,
                            snapshot: io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleRuntimeSnapshot,
                            action: BattleSessionCaptureAction,
                        ): CaptureFormulaInput =
                            CaptureFormulaInput(
                                currentHp = 30,
                                maxHp = 120,
                                captureRate = 45,
                                statusId = "par",
                                ballItemInternalName = action.ballItemId,
                                turn = 1,
                                battleContext = CaptureContext(),
                            )
                    },
                captureFormulaService = DefaultCaptureFormulaService(),
            )

        val replaySession =
            createSession(
                effectRepository = InMemoryEffectDefinitionRepository(effects.associateBy { it.id }),
                battleFlowEngine = createFlowEngine(InMemoryEffectDefinitionRepository(effects.associateBy { it.id })),
                battleId = "session-replay-capture-1",
                formatId = "singles",
                captureActionResolver = captureResolver,
            )
        replaySession.start()

        val restoredSession =
            createSession(
                effectRepository = InMemoryEffectDefinitionRepository(effects.associateBy { it.id }),
                battleFlowEngine = createFlowEngine(InMemoryEffectDefinitionRepository(effects.associateBy { it.id })),
                battleId = "session-replay-capture-restored",
                formatId = "singles",
                captureActionResolver = captureResolver,
            )
        restoredSession.restoreState(replaySession.exportState())

        val action =
            BattleSessionCaptureAction(
                playerId = "player-1",
                ballItemId = "poke-ball",
                sourceUnitId = "p1a",
                targetId = "wild-1",
            )

        val originalResult = replaySession.captureActionResolver.resolve(replaySession, action)
        val replayedResult = restoredSession.captureActionResolver.resolve(restoredSession, action)

        assertEquals(originalResult, replayedResult)
        assertEquals(replaySession.snapshot().battle.randomState, restoredSession.snapshot().battle.randomState)
    }
}
