package io.github.lishangbu.avalon.game.battle.engine.config

import io.github.lishangbu.avalon.game.battle.engine.capture.CaptureFormulaService
import io.github.lishangbu.avalon.game.battle.engine.capture.CaptureRandomSource
import io.github.lishangbu.avalon.game.battle.engine.capture.DefaultCaptureFormulaService
import io.github.lishangbu.avalon.game.battle.engine.capture.DefaultCaptureRandomSource
import io.github.lishangbu.avalon.game.battle.engine.registry.ActionExecutorRegistry
import io.github.lishangbu.avalon.game.battle.engine.registry.ConditionInterpreterRegistry
import io.github.lishangbu.avalon.game.battle.engine.registry.memory.StandardActionExecutorRegistryFactory
import io.github.lishangbu.avalon.game.battle.engine.registry.memory.StandardConditionInterpreterRegistryFactory
import io.github.lishangbu.avalon.game.battle.engine.repository.EffectDefinitionRepository
import io.github.lishangbu.avalon.game.battle.engine.runtime.DefaultHookRuleProcessor
import io.github.lishangbu.avalon.game.battle.engine.runtime.HookRuleProcessor
import io.github.lishangbu.avalon.game.battle.engine.runtime.apply.DefaultMutationApplier
import io.github.lishangbu.avalon.game.battle.engine.runtime.apply.MutationApplier
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.AddVolatileBattleMutationInterceptor
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleFlowEngine
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleFlowPhaseProcessor
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleHitResolutionPolicy
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleMoveAccuracyEvasionPhaseStep
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleMoveAfterMovePhaseStep
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleMoveCriticalHitPhaseStep
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleMoveHitHooksPhaseStep
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleMoveHitResolutionStep
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleMovePowerDamagePhaseStep
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleMovePreHitPhaseStep
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleMoveResolutionPipeline
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleMoveResolutionStep
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleMutationInterceptor
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleMutationInterceptorChain
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleTypeEffectivenessResolver
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.DefaultBattleFlowEngine
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.DefaultBattleFlowPhaseProcessor
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.DefaultBattleHitResolutionPolicy
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.DefaultBattleMoveResolutionPipeline
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.DefaultBattleMutationInterceptorChain
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.NoopBattleTypeEffectivenessResolver
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.SetStatusBattleMutationInterceptor
import io.github.lishangbu.avalon.game.battle.engine.service.BattleSessionFactory
import io.github.lishangbu.avalon.game.battle.engine.service.BattleSessionService
import io.github.lishangbu.avalon.game.battle.engine.service.BattleSessionStore
import io.github.lishangbu.avalon.game.battle.engine.service.CaptureActionResolver
import io.github.lishangbu.avalon.game.battle.engine.service.CaptureFormulaInputResolver
import io.github.lishangbu.avalon.game.battle.engine.service.DefaultBattleSessionFactory
import io.github.lishangbu.avalon.game.battle.engine.service.DefaultCaptureActionResolver
import io.github.lishangbu.avalon.game.battle.engine.service.memory.InMemoryBattleSessionService
import io.github.lishangbu.avalon.game.battle.engine.service.memory.InMemoryBattleSessionStore
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionActionExecutionSupport
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionActionHandler
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionActionHandlerRegistry
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionActionSortingStrategy
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionBattleLogProjector
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionCaptureActionHandler
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionCaptureChoiceHandler
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionChoiceHandler
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionChoiceHandlerRegistry
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionCommandFactory
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionEventPublisher
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionItemActionHandler
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionItemChoiceHandler
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionMoveActionHandler
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionMoveChoiceHandler
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionProjector
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionQueuedActionExecutionStep
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionResourceLedgerProjector
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionRunActionHandler
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionRunChoiceHandler
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionStructuredEventProjector
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionSwitchActionHandler
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionSwitchChoiceHandler
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionTurnAdvanceStep
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionTurnPipeline
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionTurnStep
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionTurnValidationStep
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionWaitActionHandler
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionWaitChoiceHandler
import io.github.lishangbu.avalon.game.battle.engine.session.DefaultBattleSessionActionSortingStrategy
import io.github.lishangbu.avalon.game.battle.engine.session.DefaultBattleSessionCommandFactory
import io.github.lishangbu.avalon.game.battle.engine.session.DefaultBattleSessionEventPublisher
import io.github.lishangbu.avalon.game.battle.engine.session.DefaultBattleSessionTurnPipeline
import io.github.lishangbu.avalon.game.battle.engine.session.DefaultReplacementStrategy
import io.github.lishangbu.avalon.game.battle.engine.session.InMemoryBattleSessionActionHandlerRegistry
import io.github.lishangbu.avalon.game.battle.engine.session.InMemoryBattleSessionChoiceHandlerRegistry
import io.github.lishangbu.avalon.game.battle.engine.session.ReplacementStrategy
import io.github.lishangbu.avalon.game.battle.engine.session.specification.BattleSessionCaptureChoiceSpecification
import io.github.lishangbu.avalon.game.battle.engine.session.specification.BattleSessionRunChoiceSpecification
import io.github.lishangbu.avalon.game.battle.engine.session.specification.BattleSessionTargetChoiceSpecification
import io.github.lishangbu.avalon.game.battle.engine.session.specification.BattleSessionTurnReadySpecification
import io.github.lishangbu.avalon.game.battle.engine.session.specification.BattleSessionUnitChoiceSpecification
import io.github.lishangbu.avalon.game.battle.engine.session.specification.DefaultBattleSessionCaptureChoiceSpecification
import io.github.lishangbu.avalon.game.battle.engine.session.specification.DefaultBattleSessionRunChoiceSpecification
import io.github.lishangbu.avalon.game.battle.engine.session.specification.DefaultBattleSessionTargetChoiceSpecification
import io.github.lishangbu.avalon.game.battle.engine.session.specification.DefaultBattleSessionTurnReadySpecification
import io.github.lishangbu.avalon.game.battle.engine.session.specification.DefaultBattleSessionUnitChoiceSpecification
import io.github.lishangbu.avalon.game.battle.engine.session.target.BattleSessionAvailableTargetResolver
import io.github.lishangbu.avalon.game.battle.engine.session.target.BattleSessionTargetModeResolver
import io.github.lishangbu.avalon.game.battle.engine.session.target.BattleSessionTargetQueryService
import io.github.lishangbu.avalon.game.battle.engine.session.target.DefaultBattleSessionAvailableTargetResolver
import io.github.lishangbu.avalon.game.battle.engine.session.target.DefaultBattleSessionTargetModeResolver
import io.github.lishangbu.avalon.game.battle.engine.session.target.DefaultBattleSessionTargetQueryService
import io.github.lishangbu.avalon.game.calculator.capture.CaptureRateCalculator
import io.github.lishangbu.avalon.game.calculator.capture.DefaultCaptureRateCalculator
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * `avalon-game-battle-engine` 默认装配。
 */
@Configuration(proxyBeanMethods = false)
class BattleEngineConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun captureRandomSource(): CaptureRandomSource = DefaultCaptureRandomSource()

    @Bean
    @ConditionalOnMissingBean
    fun captureRateCalculator(): CaptureRateCalculator = DefaultCaptureRateCalculator()

    @Bean
    @ConditionalOnMissingBean
    fun captureFormulaService(
        captureRandomSource: CaptureRandomSource,
        captureRateCalculator: CaptureRateCalculator,
    ): CaptureFormulaService = DefaultCaptureFormulaService(captureRandomSource, captureRateCalculator)

    @Bean
    @ConditionalOnMissingBean(CaptureActionResolver::class)
    fun captureActionResolver(
        captureFormulaInputResolver: CaptureFormulaInputResolver,
        captureFormulaService: CaptureFormulaService,
    ): CaptureActionResolver = DefaultCaptureActionResolver(captureFormulaInputResolver, captureFormulaService)

    @Bean
    @ConditionalOnMissingBean
    fun conditionInterpreterRegistry(): ConditionInterpreterRegistry = StandardConditionInterpreterRegistryFactory.create()

    @Bean
    @ConditionalOnMissingBean
    fun actionExecutorRegistry(): ActionExecutorRegistry = StandardActionExecutorRegistryFactory.create()

    @Bean
    @ConditionalOnMissingBean
    fun hookRuleProcessor(
        conditionInterpreterRegistry: ConditionInterpreterRegistry,
        actionExecutorRegistry: ActionExecutorRegistry,
    ): HookRuleProcessor =
        DefaultHookRuleProcessor(
            conditionInterpreterRegistry = conditionInterpreterRegistry,
            actionExecutorRegistry = actionExecutorRegistry,
        )

    @Bean
    @ConditionalOnMissingBean
    fun mutationApplier(): MutationApplier = DefaultMutationApplier()

    @Bean
    @ConditionalOnMissingBean
    fun replacementStrategy(): ReplacementStrategy = DefaultReplacementStrategy()

    @Bean
    @ConditionalOnMissingBean(SetStatusBattleMutationInterceptor::class)
    fun setStatusBattleMutationInterceptor(): SetStatusBattleMutationInterceptor = SetStatusBattleMutationInterceptor()

    @Bean
    @ConditionalOnMissingBean(AddVolatileBattleMutationInterceptor::class)
    fun addVolatileBattleMutationInterceptor(): AddVolatileBattleMutationInterceptor = AddVolatileBattleMutationInterceptor()

    @Bean
    @ConditionalOnMissingBean(BattleMutationInterceptorChain::class)
    fun battleMutationInterceptorChain(
        interceptors: List<BattleMutationInterceptor>,
    ): BattleMutationInterceptorChain = DefaultBattleMutationInterceptorChain(interceptors)

    @Bean
    @ConditionalOnMissingBean(BattleFlowPhaseProcessor::class)
    fun battleFlowPhaseProcessor(
        effectDefinitionRepository: EffectDefinitionRepository,
        hookRuleProcessor: HookRuleProcessor,
        mutationApplier: MutationApplier,
        battleMutationInterceptorChain: BattleMutationInterceptorChain,
    ): BattleFlowPhaseProcessor =
        DefaultBattleFlowPhaseProcessor(
            effectRepository = effectDefinitionRepository,
            hookRuleProcessor = hookRuleProcessor,
            mutationApplier = mutationApplier,
            mutationInterceptorChain = battleMutationInterceptorChain,
        )

    @Bean
    @ConditionalOnMissingBean(BattleHitResolutionPolicy::class)
    fun battleHitResolutionPolicy(): BattleHitResolutionPolicy = DefaultBattleHitResolutionPolicy()

    @Bean
    @ConditionalOnMissingBean(BattleTypeEffectivenessResolver::class)
    fun battleTypeEffectivenessResolver(): BattleTypeEffectivenessResolver = NoopBattleTypeEffectivenessResolver

    @Bean
    @ConditionalOnMissingBean(BattleMovePreHitPhaseStep::class)
    fun battleMovePreHitPhaseStep(
        battleFlowPhaseProcessor: BattleFlowPhaseProcessor,
    ): BattleMovePreHitPhaseStep = BattleMovePreHitPhaseStep(battleFlowPhaseProcessor)

    @Bean
    @ConditionalOnMissingBean(BattleMoveAccuracyEvasionPhaseStep::class)
    fun battleMoveAccuracyEvasionPhaseStep(
        battleFlowPhaseProcessor: BattleFlowPhaseProcessor,
    ): BattleMoveAccuracyEvasionPhaseStep = BattleMoveAccuracyEvasionPhaseStep(battleFlowPhaseProcessor)

    @Bean
    @ConditionalOnMissingBean(BattleMoveHitResolutionStep::class)
    fun battleMoveHitResolutionStep(
        battleHitResolutionPolicy: BattleHitResolutionPolicy,
    ): BattleMoveHitResolutionStep = BattleMoveHitResolutionStep(battleHitResolutionPolicy)

    @Bean
    @ConditionalOnMissingBean(BattleMoveCriticalHitPhaseStep::class)
    fun battleMoveCriticalHitPhaseStep(
        battleFlowPhaseProcessor: BattleFlowPhaseProcessor,
    ): BattleMoveCriticalHitPhaseStep = BattleMoveCriticalHitPhaseStep(battleFlowPhaseProcessor)

    @Bean
    @ConditionalOnMissingBean(BattleMovePowerDamagePhaseStep::class)
    fun battleMovePowerDamagePhaseStep(
        battleFlowPhaseProcessor: BattleFlowPhaseProcessor,
        battleTypeEffectivenessResolver: BattleTypeEffectivenessResolver,
    ): BattleMovePowerDamagePhaseStep = BattleMovePowerDamagePhaseStep(battleFlowPhaseProcessor, battleTypeEffectivenessResolver)

    @Bean
    @ConditionalOnMissingBean(BattleMoveHitHooksPhaseStep::class)
    fun battleMoveHitHooksPhaseStep(
        battleFlowPhaseProcessor: BattleFlowPhaseProcessor,
    ): BattleMoveHitHooksPhaseStep = BattleMoveHitHooksPhaseStep(battleFlowPhaseProcessor)

    @Bean
    @ConditionalOnMissingBean(BattleMoveAfterMovePhaseStep::class)
    fun battleMoveAfterMovePhaseStep(
        battleFlowPhaseProcessor: BattleFlowPhaseProcessor,
    ): BattleMoveAfterMovePhaseStep = BattleMoveAfterMovePhaseStep(battleFlowPhaseProcessor)

    @Bean
    @ConditionalOnMissingBean(BattleMoveResolutionPipeline::class)
    fun battleMoveResolutionPipeline(
        steps: List<BattleMoveResolutionStep>,
    ): BattleMoveResolutionPipeline = DefaultBattleMoveResolutionPipeline(steps)

    @Bean
    @ConditionalOnMissingBean(BattleFlowEngine::class)
    fun battleFlowEngine(
        effectDefinitionRepository: EffectDefinitionRepository,
        battleFlowPhaseProcessor: BattleFlowPhaseProcessor,
        battleMoveResolutionPipeline: BattleMoveResolutionPipeline,
    ): BattleFlowEngine =
        DefaultBattleFlowEngine(
            effectRepository = effectDefinitionRepository,
            phaseProcessor = battleFlowPhaseProcessor,
            moveResolutionPipeline = battleMoveResolutionPipeline,
        )

    @Bean
    @ConditionalOnMissingBean(BattleSessionActionSortingStrategy::class)
    fun battleSessionActionSortingStrategy(): BattleSessionActionSortingStrategy = DefaultBattleSessionActionSortingStrategy()

    @Bean
    @ConditionalOnMissingBean(BattleSessionBattleLogProjector::class)
    fun battleSessionBattleLogProjector(): BattleSessionBattleLogProjector = BattleSessionBattleLogProjector()

    @Bean
    @ConditionalOnMissingBean(BattleSessionStructuredEventProjector::class)
    fun battleSessionStructuredEventProjector(): BattleSessionStructuredEventProjector = BattleSessionStructuredEventProjector()

    @Bean
    @ConditionalOnMissingBean(BattleSessionResourceLedgerProjector::class)
    fun battleSessionResourceLedgerProjector(): BattleSessionResourceLedgerProjector = BattleSessionResourceLedgerProjector()

    @Bean
    @ConditionalOnMissingBean(BattleSessionEventPublisher::class)
    fun battleSessionEventPublisher(
        projectors: List<BattleSessionProjector>,
    ): BattleSessionEventPublisher = DefaultBattleSessionEventPublisher(projectors)

    @Bean
    @ConditionalOnMissingBean(BattleSessionTargetModeResolver::class)
    fun battleSessionTargetModeResolver(
        effectDefinitionRepository: EffectDefinitionRepository,
    ): BattleSessionTargetModeResolver = DefaultBattleSessionTargetModeResolver(effectDefinitionRepository)

    @Bean
    @ConditionalOnMissingBean(BattleSessionAvailableTargetResolver::class)
    fun battleSessionAvailableTargetResolver(): BattleSessionAvailableTargetResolver = DefaultBattleSessionAvailableTargetResolver()

    @Bean
    @ConditionalOnMissingBean(BattleSessionTargetQueryService::class)
    fun battleSessionTargetQueryService(
        battleSessionTargetModeResolver: BattleSessionTargetModeResolver,
        battleSessionAvailableTargetResolver: BattleSessionAvailableTargetResolver,
    ): BattleSessionTargetQueryService =
        DefaultBattleSessionTargetQueryService(
            targetModeResolver = battleSessionTargetModeResolver,
            availableTargetResolver = battleSessionAvailableTargetResolver,
        )

    @Bean
    @ConditionalOnMissingBean(BattleSessionTurnReadySpecification::class)
    fun battleSessionTurnReadySpecification(): BattleSessionTurnReadySpecification = DefaultBattleSessionTurnReadySpecification()

    @Bean
    @ConditionalOnMissingBean(BattleSessionUnitChoiceSpecification::class)
    fun battleSessionUnitChoiceSpecification(): BattleSessionUnitChoiceSpecification = DefaultBattleSessionUnitChoiceSpecification()

    @Bean
    @ConditionalOnMissingBean(BattleSessionRunChoiceSpecification::class)
    fun battleSessionRunChoiceSpecification(): BattleSessionRunChoiceSpecification = DefaultBattleSessionRunChoiceSpecification()

    @Bean
    @ConditionalOnMissingBean(BattleSessionTargetChoiceSpecification::class)
    fun battleSessionTargetChoiceSpecification(
        battleSessionTargetQueryService: BattleSessionTargetQueryService,
    ): BattleSessionTargetChoiceSpecification = DefaultBattleSessionTargetChoiceSpecification(battleSessionTargetQueryService)

    @Bean
    @ConditionalOnMissingBean(BattleSessionCaptureChoiceSpecification::class)
    fun battleSessionCaptureChoiceSpecification(): BattleSessionCaptureChoiceSpecification = DefaultBattleSessionCaptureChoiceSpecification()

    @Bean
    @ConditionalOnMissingBean
    fun battleSessionActionExecutionSupport(): BattleSessionActionExecutionSupport = BattleSessionActionExecutionSupport()

    @Bean
    @ConditionalOnMissingBean(BattleSessionCommandFactory::class)
    fun battleSessionCommandFactory(): BattleSessionCommandFactory = DefaultBattleSessionCommandFactory()

    @Bean
    @ConditionalOnMissingBean(BattleSessionMoveChoiceHandler::class)
    fun battleSessionMoveChoiceHandler(
        battleSessionCommandFactory: BattleSessionCommandFactory,
    ): BattleSessionMoveChoiceHandler = BattleSessionMoveChoiceHandler(battleSessionCommandFactory)

    @Bean
    @ConditionalOnMissingBean(BattleSessionSwitchChoiceHandler::class)
    fun battleSessionSwitchChoiceHandler(
        battleSessionCommandFactory: BattleSessionCommandFactory,
    ): BattleSessionSwitchChoiceHandler = BattleSessionSwitchChoiceHandler(battleSessionCommandFactory)

    @Bean
    @ConditionalOnMissingBean(BattleSessionItemChoiceHandler::class)
    fun battleSessionItemChoiceHandler(
        battleSessionCommandFactory: BattleSessionCommandFactory,
    ): BattleSessionItemChoiceHandler = BattleSessionItemChoiceHandler(battleSessionCommandFactory)

    @Bean
    @ConditionalOnMissingBean(BattleSessionCaptureChoiceHandler::class)
    fun battleSessionCaptureChoiceHandler(
        battleSessionCommandFactory: BattleSessionCommandFactory,
    ): BattleSessionCaptureChoiceHandler = BattleSessionCaptureChoiceHandler(battleSessionCommandFactory)

    @Bean
    @ConditionalOnMissingBean(BattleSessionRunChoiceHandler::class)
    fun battleSessionRunChoiceHandler(
        battleSessionCommandFactory: BattleSessionCommandFactory,
    ): BattleSessionRunChoiceHandler = BattleSessionRunChoiceHandler(battleSessionCommandFactory)

    @Bean
    @ConditionalOnMissingBean(BattleSessionWaitChoiceHandler::class)
    fun battleSessionWaitChoiceHandler(
        battleSessionCommandFactory: BattleSessionCommandFactory,
    ): BattleSessionWaitChoiceHandler = BattleSessionWaitChoiceHandler(battleSessionCommandFactory)

    @Bean
    @ConditionalOnMissingBean(BattleSessionChoiceHandlerRegistry::class)
    fun battleSessionChoiceHandlerRegistry(
        battleSessionCommandFactory: BattleSessionCommandFactory,
        handlers: List<BattleSessionChoiceHandler>,
    ): BattleSessionChoiceHandlerRegistry = InMemoryBattleSessionChoiceHandlerRegistry(handlers)

    @Bean
    @ConditionalOnMissingBean(BattleSessionMoveActionHandler::class)
    fun battleSessionMoveActionHandler(
        battleSessionActionExecutionSupport: BattleSessionActionExecutionSupport,
    ): BattleSessionMoveActionHandler = BattleSessionMoveActionHandler(battleSessionActionExecutionSupport)

    @Bean
    @ConditionalOnMissingBean(BattleSessionSwitchActionHandler::class)
    fun battleSessionSwitchActionHandler(
        battleSessionActionExecutionSupport: BattleSessionActionExecutionSupport,
    ): BattleSessionSwitchActionHandler = BattleSessionSwitchActionHandler(battleSessionActionExecutionSupport)

    @Bean
    @ConditionalOnMissingBean(BattleSessionItemActionHandler::class)
    fun battleSessionItemActionHandler(
        battleSessionActionExecutionSupport: BattleSessionActionExecutionSupport,
    ): BattleSessionItemActionHandler = BattleSessionItemActionHandler(battleSessionActionExecutionSupport)

    @Bean
    @ConditionalOnMissingBean(BattleSessionCaptureActionHandler::class)
    fun battleSessionCaptureActionHandler(): BattleSessionCaptureActionHandler = BattleSessionCaptureActionHandler()

    @Bean
    @ConditionalOnMissingBean(BattleSessionRunActionHandler::class)
    fun battleSessionRunActionHandler(
        battleSessionActionExecutionSupport: BattleSessionActionExecutionSupport,
    ): BattleSessionRunActionHandler = BattleSessionRunActionHandler(battleSessionActionExecutionSupport)

    @Bean
    @ConditionalOnMissingBean(BattleSessionWaitActionHandler::class)
    fun battleSessionWaitActionHandler(): BattleSessionWaitActionHandler = BattleSessionWaitActionHandler()

    @Bean
    @ConditionalOnMissingBean(BattleSessionActionHandlerRegistry::class)
    fun battleSessionActionHandlerRegistry(
        handlers: List<BattleSessionActionHandler>,
    ): BattleSessionActionHandlerRegistry = InMemoryBattleSessionActionHandlerRegistry(handlers)

    @Bean
    @ConditionalOnMissingBean(BattleSessionTurnValidationStep::class)
    fun battleSessionTurnValidationStep(): BattleSessionTurnValidationStep = BattleSessionTurnValidationStep()

    @Bean
    @ConditionalOnMissingBean(BattleSessionQueuedActionExecutionStep::class)
    fun battleSessionQueuedActionExecutionStep(): BattleSessionQueuedActionExecutionStep = BattleSessionQueuedActionExecutionStep()

    @Bean
    @ConditionalOnMissingBean(BattleSessionTurnAdvanceStep::class)
    fun battleSessionTurnAdvanceStep(): BattleSessionTurnAdvanceStep = BattleSessionTurnAdvanceStep()

    @Bean
    @ConditionalOnMissingBean(BattleSessionTurnPipeline::class)
    fun battleSessionTurnPipeline(
        steps: List<BattleSessionTurnStep>,
    ): BattleSessionTurnPipeline = DefaultBattleSessionTurnPipeline(steps)

    @Bean
    @ConditionalOnMissingBean
    fun battleSessionFactory(
        effectDefinitionRepository: EffectDefinitionRepository,
        battleFlowEngine: BattleFlowEngine,
        captureActionResolver: CaptureActionResolver,
        mutationApplier: MutationApplier,
        replacementStrategy: ReplacementStrategy,
        battleSessionActionExecutionSupport: BattleSessionActionExecutionSupport,
        battleSessionChoiceHandlerRegistry: BattleSessionChoiceHandlerRegistry,
        battleSessionActionHandlerRegistry: BattleSessionActionHandlerRegistry,
        battleSessionTurnPipeline: BattleSessionTurnPipeline,
        battleSessionTurnReadySpecification: BattleSessionTurnReadySpecification,
        battleSessionUnitChoiceSpecification: BattleSessionUnitChoiceSpecification,
        battleSessionRunChoiceSpecification: BattleSessionRunChoiceSpecification,
        battleSessionTargetChoiceSpecification: BattleSessionTargetChoiceSpecification,
        battleSessionCaptureChoiceSpecification: BattleSessionCaptureChoiceSpecification,
        battleSessionTargetQueryService: BattleSessionTargetQueryService,
        battleSessionActionSortingStrategy: BattleSessionActionSortingStrategy,
        battleSessionEventPublisher: BattleSessionEventPublisher,
        battleSessionCommandFactory: BattleSessionCommandFactory,
    ): BattleSessionFactory =
        DefaultBattleSessionFactory(
            effectRepository = effectDefinitionRepository,
            battleFlowEngine = battleFlowEngine,
            captureActionResolver = captureActionResolver,
            mutationApplier = mutationApplier,
            replacementStrategy = replacementStrategy,
            actionExecutionSupport = battleSessionActionExecutionSupport,
            choiceHandlerRegistry = battleSessionChoiceHandlerRegistry,
            actionHandlerRegistry = battleSessionActionHandlerRegistry,
            turnPipeline = battleSessionTurnPipeline,
            turnReadySpecification = battleSessionTurnReadySpecification,
            unitChoiceSpecification = battleSessionUnitChoiceSpecification,
            runChoiceSpecification = battleSessionRunChoiceSpecification,
            targetChoiceSpecification = battleSessionTargetChoiceSpecification,
            captureChoiceSpecification = battleSessionCaptureChoiceSpecification,
            targetQueryService = battleSessionTargetQueryService,
            actionSortingStrategy = battleSessionActionSortingStrategy,
            eventPublisher = battleSessionEventPublisher,
            commandFactory = battleSessionCommandFactory,
        )

    @Bean
    @ConditionalOnMissingBean(BattleSessionStore::class)
    fun inMemoryBattleSessionStore(): BattleSessionStore = InMemoryBattleSessionStore()

    @Bean
    @ConditionalOnMissingBean
    fun battleSessionService(
        sessionFactory: BattleSessionFactory,
        battleSessionStore: BattleSessionStore,
    ): BattleSessionService = InMemoryBattleSessionService(sessionFactory, battleSessionStore)
}
