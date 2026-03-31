package io.github.lishangbu.avalon.game.battle.engine.service

import io.github.lishangbu.avalon.game.battle.engine.loader.JsonEffectDefinitionBattleDataLoader
import io.github.lishangbu.avalon.game.battle.engine.model.UnitState
import io.github.lishangbu.avalon.game.battle.engine.registry.memory.StandardActionExecutorRegistryFactory
import io.github.lishangbu.avalon.game.battle.engine.registry.memory.StandardConditionInterpreterRegistryFactory
import io.github.lishangbu.avalon.game.battle.engine.repository.memory.InMemoryEffectDefinitionRepository
import io.github.lishangbu.avalon.game.battle.engine.runtime.DefaultHookRuleProcessor
import io.github.lishangbu.avalon.game.battle.engine.runtime.apply.DefaultMutationApplier
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.AddVolatileBattleMutationInterceptor
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleFlowEngine
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleMoveAccuracyEvasionPhaseStep
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleMoveAfterMovePhaseStep
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
import io.github.lishangbu.avalon.game.battle.engine.service.UnsupportedCaptureActionResolver
import io.github.lishangbu.avalon.game.battle.engine.service.memory.InMemoryBattleSessionService
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionActionExecutionSupport
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionBattleLogProjector
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionResourceLedgerProjector
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionStructuredEventProjector
import io.github.lishangbu.avalon.game.battle.engine.session.DefaultBattleSessionActionSortingStrategy
import io.github.lishangbu.avalon.game.battle.engine.session.DefaultBattleSessionCommandFactory
import io.github.lishangbu.avalon.game.battle.engine.session.DefaultBattleSessionEventPublisher
import io.github.lishangbu.avalon.game.battle.engine.session.DefaultBattleSessionTurnPipeline
import io.github.lishangbu.avalon.game.battle.engine.session.DefaultReplacementStrategy
import io.github.lishangbu.avalon.game.battle.engine.session.InMemoryBattleSessionActionHandlerRegistry
import io.github.lishangbu.avalon.game.battle.engine.session.InMemoryBattleSessionChoiceHandlerRegistry
import io.github.lishangbu.avalon.game.battle.engine.session.MoveChoice
import io.github.lishangbu.avalon.game.battle.engine.session.specification.DefaultBattleSessionCaptureChoiceSpecification
import io.github.lishangbu.avalon.game.battle.engine.session.specification.DefaultBattleSessionRunChoiceSpecification
import io.github.lishangbu.avalon.game.battle.engine.session.specification.DefaultBattleSessionTargetChoiceSpecification
import io.github.lishangbu.avalon.game.battle.engine.session.specification.DefaultBattleSessionTurnReadySpecification
import io.github.lishangbu.avalon.game.battle.engine.session.specification.DefaultBattleSessionUnitChoiceSpecification
import io.github.lishangbu.avalon.game.battle.engine.session.target.DefaultBattleSessionAvailableTargetResolver
import io.github.lishangbu.avalon.game.battle.engine.session.target.DefaultBattleSessionTargetModeResolver
import io.github.lishangbu.avalon.game.battle.engine.session.target.DefaultBattleSessionTargetQueryService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 关注点：
 * - BattleSessionService 是否已经能把 session 生命周期整理成稳定的对外服务 API。
 */
class InMemoryBattleSessionServiceTest {
    private val effects =
        JsonEffectDefinitionBattleDataLoader(
            resourcePaths =
                listOf(
                    "data/battle/fixtures/move/thunderbolt.json",
                    "data/battle/fixtures/move/recover.json",
                    "data/battle/fixtures/ability/static.json",
                ),
        ).loadEffects()

    private val service =
        InMemoryBattleSessionService(
            sessionFactory =
                createSessionFactory(captureActionResolver = UnsupportedCaptureActionResolver()),
        )

    /**
     * 创建测试专用的 `DefaultBattleSessionFactory`。
     */
    private fun createSessionFactory(captureActionResolver: CaptureActionResolver): DefaultBattleSessionFactory {
        val effectRepository = InMemoryEffectDefinitionRepository(effects.associateBy { it.id })
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
        return DefaultBattleSessionFactory(
            effectRepository = effectRepository,
            battleFlowEngine = createFlowEngine(effectRepository),
            captureActionResolver = captureActionResolver,
            mutationApplier = DefaultMutationApplier(),
            replacementStrategy = DefaultReplacementStrategy(),
            actionExecutionSupport = actionExecutionSupport,
            choiceHandlerRegistry = choiceHandlerRegistry,
            actionHandlerRegistry = actionHandlerRegistry,
            turnPipeline = DefaultBattleSessionTurnPipeline.createDefault(),
            turnReadySpecification = DefaultBattleSessionTurnReadySpecification(),
            unitChoiceSpecification = DefaultBattleSessionUnitChoiceSpecification(),
            runChoiceSpecification = DefaultBattleSessionRunChoiceSpecification(),
            targetChoiceSpecification = DefaultBattleSessionTargetChoiceSpecification(targetQueryService),
            captureChoiceSpecification = DefaultBattleSessionCaptureChoiceSpecification(),
            targetQueryService = targetQueryService,
            actionSortingStrategy = DefaultBattleSessionActionSortingStrategy(),
            eventPublisher = eventPublisher,
            commandFactory = commandFactory,
        )
    }

    /**
     * 创建测试专用的 `BattleFlowEngine`。
     */
    private fun createFlowEngine(effectRepository: InMemoryEffectDefinitionRepository): BattleFlowEngine {
        val hookRuleProcessor =
            DefaultHookRuleProcessor(
                conditionInterpreterRegistry = StandardConditionInterpreterRegistryFactory.create(),
                actionExecutorRegistry = StandardActionExecutorRegistryFactory.create(),
            )
        val phaseProcessor =
            DefaultBattleFlowPhaseProcessor(
                effectRepository = effectRepository,
                hookRuleProcessor = hookRuleProcessor,
                mutationApplier = DefaultMutationApplier(),
                mutationInterceptorChain =
                    DefaultBattleMutationInterceptorChain(
                        listOf(
                            SetStatusBattleMutationInterceptor(),
                            AddVolatileBattleMutationInterceptor(),
                        ),
                    ),
            )
        val pipeline =
            DefaultBattleMoveResolutionPipeline(
                steps =
                    listOf(
                        BattleMovePreHitPhaseStep(phaseProcessor),
                        BattleMoveAccuracyEvasionPhaseStep(phaseProcessor),
                        BattleMoveHitResolutionStep(DefaultBattleHitResolutionPolicy()),
                        BattleMovePowerDamagePhaseStep(phaseProcessor),
                        BattleMoveHitHooksPhaseStep(phaseProcessor),
                        BattleMoveAfterMovePhaseStep(phaseProcessor),
                    ),
            )
        return DefaultBattleFlowEngine(
            effectRepository = effectRepository,
            phaseProcessor = phaseProcessor,
            moveResolutionPipeline = pipeline,
        )
    }

    /**
     * 验证：
     * - service 可以创建 session、注册 side/unit、启动会话并返回统一 query DTO。
     */
    @Test
    fun shouldCreateAndStartSessionThroughServiceApi() {
        service.createSession("svc-1", "singles")
        service.registerSide("svc-1", "p1")
        service.registerSide("svc-1", "p2")
        service.registerUnit("svc-1", "p1", UnitState(id = "p1a", currentHp = 120, maxHp = 120))
        service.registerUnit("svc-1", "p2", UnitState(id = "p2a", currentHp = 100, maxHp = 100))

        val query = service.startSession("svc-1")

        assertTrue(query.snapshot.battle.started)
        assertEquals(1, query.snapshot.battle.turn)
        assertEquals(2, query.choiceStatuses.size)
    }

    /**
     * 验证：
     * - service 可以接收统一 choice 输入，并推动 session 完成一整回合结算。
     */
    @Test
    fun shouldSubmitChoicesAndResolveTurnThroughServiceApi() {
        service.createSession("svc-2", "singles")
        service.registerSide("svc-2", "p1")
        service.registerSide("svc-2", "p2")
        service.registerUnit("svc-2", "p1", UnitState(id = "p1a", currentHp = 120, maxHp = 120))
        service.registerUnit("svc-2", "p2", UnitState(id = "p2a", currentHp = 50, maxHp = 100))
        service.startSession("svc-2")

        val pending =
            service.submitChoices(
                "svc-2",
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
        assertEquals(2, pending.pendingActions.size)

        val turnResult = service.resolveTurn("svc-2")

        assertEquals(2, turnResult.actionResults.size)
        assertEquals(
            80,
            turnResult.snapshot.units
                .getValue("p2a")
                .currentHp,
        )
        assertEquals(
            "par",
            turnResult.snapshot.units
                .getValue("p2a")
                .statusId,
        )
    }
}
