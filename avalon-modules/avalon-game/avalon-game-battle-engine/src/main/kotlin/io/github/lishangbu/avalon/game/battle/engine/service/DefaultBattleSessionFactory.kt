package io.github.lishangbu.avalon.game.battle.engine.service

import io.github.lishangbu.avalon.game.battle.engine.repository.EffectDefinitionRepository
import io.github.lishangbu.avalon.game.battle.engine.runtime.apply.MutationApplier
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleFlowEngine
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSession
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionActionExecutionSupport
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionActionHandlerRegistry
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionActionSortingStrategy
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionChoiceHandlerRegistry
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionCommandFactory
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionEventPublisher
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionState
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionTurnPipeline
import io.github.lishangbu.avalon.game.battle.engine.session.ReplacementStrategy
import io.github.lishangbu.avalon.game.battle.engine.session.specification.BattleSessionCaptureChoiceSpecification
import io.github.lishangbu.avalon.game.battle.engine.session.specification.BattleSessionRunChoiceSpecification
import io.github.lishangbu.avalon.game.battle.engine.session.specification.BattleSessionTargetChoiceSpecification
import io.github.lishangbu.avalon.game.battle.engine.session.specification.BattleSessionTurnReadySpecification
import io.github.lishangbu.avalon.game.battle.engine.session.specification.BattleSessionUnitChoiceSpecification
import io.github.lishangbu.avalon.game.battle.engine.session.target.BattleSessionTargetQueryService

/**
 * 默认 BattleSession 工厂。
 *
 * 设计意图：
 * - 统一装配 battle flow、processor、mutation apply 和 replacement strategy。
 * - 为 service 层提供一个简单且稳定的 session 创建入口。
 */
class DefaultBattleSessionFactory(
    private val effectRepository: EffectDefinitionRepository,
    private val battleFlowEngine: BattleFlowEngine,
    private val captureActionResolver: CaptureActionResolver,
    private val mutationApplier: MutationApplier,
    private val replacementStrategy: ReplacementStrategy,
    private val actionExecutionSupport: BattleSessionActionExecutionSupport,
    private val choiceHandlerRegistry: BattleSessionChoiceHandlerRegistry,
    private val actionHandlerRegistry: BattleSessionActionHandlerRegistry,
    private val turnPipeline: BattleSessionTurnPipeline,
    private val turnReadySpecification: BattleSessionTurnReadySpecification,
    private val unitChoiceSpecification: BattleSessionUnitChoiceSpecification,
    private val runChoiceSpecification: BattleSessionRunChoiceSpecification,
    private val targetChoiceSpecification: BattleSessionTargetChoiceSpecification,
    private val captureChoiceSpecification: BattleSessionCaptureChoiceSpecification,
    private val targetQueryService: BattleSessionTargetQueryService,
    private val actionSortingStrategy: BattleSessionActionSortingStrategy,
    private val eventPublisher: BattleSessionEventPublisher,
    private val commandFactory: BattleSessionCommandFactory,
) : BattleSessionFactory {
    override fun create(
        sessionId: String,
        formatId: String,
    ): BattleSession =
        BattleSession(
            effectRepository = effectRepository,
            battleFlowEngine = battleFlowEngine,
            mutationApplier = mutationApplier,
            replacementStrategy = replacementStrategy,
            captureActionResolver = captureActionResolver,
            actionExecutionSupport = actionExecutionSupport,
            choiceHandlerRegistry = choiceHandlerRegistry,
            actionHandlerRegistry = actionHandlerRegistry,
            turnPipeline = turnPipeline,
            turnReadySpecification = turnReadySpecification,
            unitChoiceSpecification = unitChoiceSpecification,
            runChoiceSpecification = runChoiceSpecification,
            targetChoiceSpecification = targetChoiceSpecification,
            captureChoiceSpecification = captureChoiceSpecification,
            targetQueryService = targetQueryService,
            actionSortingStrategy = actionSortingStrategy,
            eventPublisher = eventPublisher,
            commandFactory = commandFactory,
            battleId = sessionId,
            formatId = formatId,
        )

    override fun restore(state: BattleSessionState): BattleSession =
        create(
            sessionId = state.snapshot.battle.id,
            formatId = state.snapshot.battle.formatId,
        ).also { session ->
            session.restoreState(state)
        }
}
