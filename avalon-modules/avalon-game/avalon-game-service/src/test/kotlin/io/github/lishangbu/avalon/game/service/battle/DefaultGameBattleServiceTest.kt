package io.github.lishangbu.avalon.game.service.battle

import io.github.lishangbu.avalon.dataset.repository.CreatureSpeciesRepository
import io.github.lishangbu.avalon.dataset.repository.ItemRepository
import io.github.lishangbu.avalon.dataset.repository.MoveRepository
import io.github.lishangbu.avalon.dataset.repository.StatRepository
import io.github.lishangbu.avalon.game.battle.engine.capture.CaptureContext
import io.github.lishangbu.avalon.game.battle.engine.capture.CaptureFormulaResult
import io.github.lishangbu.avalon.game.battle.engine.model.BattleType
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
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleMoveCriticalHitPhaseStep
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleMoveHitHooksPhaseStep
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleMoveHitResolutionStep
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleMovePowerDamagePhaseStep
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleMovePreHitPhaseStep
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleTypeEffectivenessResolver
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.DefaultBattleFlowEngine
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.DefaultBattleFlowPhaseProcessor
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.DefaultBattleHitResolutionPolicy
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.DefaultBattleMoveResolutionPipeline
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.DefaultBattleMutationInterceptorChain
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.SetStatusBattleMutationInterceptor
import io.github.lishangbu.avalon.game.battle.engine.service.BattleAiChoiceProvider
import io.github.lishangbu.avalon.game.battle.engine.service.CaptureActionResolver
import io.github.lishangbu.avalon.game.battle.engine.service.DefaultBattleSessionFactory
import io.github.lishangbu.avalon.game.battle.engine.service.NoopBattleAiChoiceProvider
import io.github.lishangbu.avalon.game.battle.engine.service.WildBattleAiChoiceProvider
import io.github.lishangbu.avalon.game.battle.engine.service.memory.InMemoryBattleSessionService
import io.github.lishangbu.avalon.game.battle.engine.service.memory.InMemoryBattleSessionStore
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionActionExecutionSupport
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionActionKind
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionBattleLogProjector
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionCaptureAction
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionCaptureResourceUsage
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionMoveAction
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionResourceLedgerProjector
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionStructuredEventProjector
import io.github.lishangbu.avalon.game.battle.engine.session.DefaultBattleSessionActionSortingStrategy
import io.github.lishangbu.avalon.game.battle.engine.session.DefaultBattleSessionCommandFactory
import io.github.lishangbu.avalon.game.battle.engine.session.DefaultBattleSessionEventPublisher
import io.github.lishangbu.avalon.game.battle.engine.session.DefaultBattleSessionTurnPipeline
import io.github.lishangbu.avalon.game.battle.engine.session.DefaultReplacementStrategy
import io.github.lishangbu.avalon.game.battle.engine.session.InMemoryBattleSessionActionHandlerRegistry
import io.github.lishangbu.avalon.game.battle.engine.session.InMemoryBattleSessionChoiceHandlerRegistry
import io.github.lishangbu.avalon.game.battle.engine.session.specification.DefaultBattleSessionCaptureChoiceSpecification
import io.github.lishangbu.avalon.game.battle.engine.session.specification.DefaultBattleSessionRunChoiceSpecification
import io.github.lishangbu.avalon.game.battle.engine.session.specification.DefaultBattleSessionTargetChoiceSpecification
import io.github.lishangbu.avalon.game.battle.engine.session.specification.DefaultBattleSessionTurnReadySpecification
import io.github.lishangbu.avalon.game.battle.engine.session.specification.DefaultBattleSessionUnitChoiceSpecification
import io.github.lishangbu.avalon.game.battle.engine.session.target.DefaultBattleSessionAvailableTargetResolver
import io.github.lishangbu.avalon.game.battle.engine.session.target.DefaultBattleSessionTargetModeResolver
import io.github.lishangbu.avalon.game.battle.engine.session.target.DefaultBattleSessionTargetQueryService
import io.github.lishangbu.avalon.game.repository.BattleCaptureRecordRepository
import io.github.lishangbu.avalon.game.repository.CreatureStorageBoxRepository
import io.github.lishangbu.avalon.game.repository.OwnedCreatureMoveRepository
import io.github.lishangbu.avalon.game.repository.OwnedCreatureRepository
import io.github.lishangbu.avalon.game.repository.OwnedCreatureStatRepository
import io.github.lishangbu.avalon.game.repository.PlayerInventoryItemRepository
import io.github.lishangbu.avalon.game.repository.PlayerRepository
import io.github.lishangbu.avalon.game.service.battle.view.CreateImportedBattleSessionApiRequest
import io.github.lishangbu.avalon.game.service.battle.view.GameBattleCaptureResourceUsageView
import io.github.lishangbu.avalon.game.service.battle.view.GameBattleImportedMoveApiRequest
import io.github.lishangbu.avalon.game.service.battle.view.GameBattleImportedSideApiRequest
import io.github.lishangbu.avalon.game.service.battle.view.GameBattleImportedUnitApiRequest
import io.github.lishangbu.avalon.game.service.battle.view.GameBattleRequestAssembler
import io.github.lishangbu.avalon.game.service.battle.view.GameBattleViewAssembler
import io.github.lishangbu.avalon.game.service.capture.BattleUnitMetadata
import io.github.lishangbu.avalon.game.service.capture.CaptureCommand
import io.github.lishangbu.avalon.game.service.capture.CapturedCreatureSummary
import io.github.lishangbu.avalon.game.service.capture.DefaultBattleCaptureRecordService
import io.github.lishangbu.avalon.game.service.capture.DefaultCapturePreparationService
import io.github.lishangbu.avalon.game.service.capture.DefaultOwnedCreatureService
import io.github.lishangbu.avalon.game.service.capture.DefaultPlayerInventoryService
import io.github.lishangbu.avalon.game.service.effect.MoveImportRecord
import io.github.lishangbu.avalon.game.service.effect.SmartBattleEffectAssembler
import io.github.lishangbu.avalon.game.service.unit.BattleMoveImportRequest
import io.github.lishangbu.avalon.game.service.unit.BattleUnitImportRequest
import io.github.lishangbu.avalon.game.service.unit.BattleUnitImportResult
import io.github.lishangbu.avalon.game.service.unit.BattleUnitImporter
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultGameBattleServiceTest {
    private val thunderbolt =
        SmartBattleEffectAssembler.fromMove(
            MoveImportRecord(
                internalName = "thunderbolt",
                name = "十万伏特",
                typeInternalName = "electric",
                damageClassInternalName = "special",
                targetInternalName = "selected-pokemon",
                accuracy = 100,
                effectChance = 10,
                pp = 15,
                priority = 0,
                power = 90,
                shortEffect = "May paralyze the target.",
                effect = "Inflicts damage and may paralyze the target.",
                ailmentInternalName = "paralysis",
                ailmentChance = 10,
                healing = null,
                drain = null,
            ),
        )

    private val battleSessionService =
        InMemoryBattleSessionService(
            createSessionFactory(captureActionResolver = FakeCaptureActionResolver()),
            InMemoryBattleSessionStore(),
        )

    private val battleChoiceFactory =
        DefaultBattleChoiceFactory(
            effectDefinitionRepository = InMemoryEffectDefinitionRepository(mapOf(thunderbolt.id to thunderbolt)),
            targetQueryService =
                DefaultBattleSessionTargetQueryService(
                    targetModeResolver =
                        DefaultBattleSessionTargetModeResolver(
                            InMemoryEffectDefinitionRepository(mapOf(thunderbolt.id to thunderbolt)),
                        ),
                    availableTargetResolver = DefaultBattleSessionAvailableTargetResolver(),
                ),
        )

    private val service =
        DefaultGameBattleService(
            battleSessionService = battleSessionService,
            battleUnitImporter = FakeBattleUnitImporter(),
            battleChoiceFactory = battleChoiceFactory,
            battleAiChoiceProviders = listOf<BattleAiChoiceProvider>(WildBattleAiChoiceProvider(), NoopBattleAiChoiceProvider()),
            battleTypePolicies =
                listOf(
                    WildBattleTypePolicy(WildBattleSettlementHandler()),
                    TrainerBattleTypePolicy(SimpleBattleSettlementHandler()),
                    PvpBattleTypePolicy(SimpleBattleSettlementHandler()),
                    DungeonBattleTypePolicy(SimpleBattleSettlementHandler()),
                ),
            capturePreparationService = FakeCapturePreparationService(),
            playerInventoryService = FakePlayerInventoryService(),
            ownedCreatureService = FakeOwnedCreatureService(),
            battleCaptureRecordService = FakeBattleCaptureRecordService(),
            gameBattleRequestAssembler = GameBattleRequestAssembler(),
            gameBattleViewAssembler = GameBattleViewAssembler(),
        )

    /**
     * 创建测试专用的 `DefaultBattleSessionFactory`。
     */
    private fun createSessionFactory(captureActionResolver: CaptureActionResolver): DefaultBattleSessionFactory {
        val effectRepository = InMemoryEffectDefinitionRepository(mapOf(thunderbolt.id to thunderbolt))
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
                        BattleMoveCriticalHitPhaseStep(phaseProcessor),
                        BattleMovePowerDamagePhaseStep(phaseProcessor, FakeBattleTypeEffectivenessResolver()),
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

    @Test
    fun shouldCreateImportedSessionAndSubmitMoveChoice() {
        val created =
            service.createImportedSession(
                CreateImportedBattleSessionApiRequest(
                    sessionId = "session-import-1",
                    formatId = "single",
                    battleKind = BattleType.WILD,
                    capturableSideId = "side-a",
                    sides =
                        listOf(
                            GameBattleImportedSideApiRequest(
                                sideId = "side-a",
                                units =
                                    listOf(
                                        GameBattleImportedUnitApiRequest(
                                            unitId = "unit-a",
                                            level = 50,
                                            creatureInternalName = "pikachu",
                                            moves = listOf(GameBattleImportedMoveApiRequest("thunderbolt")),
                                        ),
                                    ),
                            ),
                            GameBattleImportedSideApiRequest(
                                sideId = "side-b",
                                units =
                                    listOf(
                                        GameBattleImportedUnitApiRequest(
                                            unitId = "unit-b",
                                            level = 50,
                                            creatureInternalName = "squirtle",
                                        ),
                                    ),
                            ),
                        ),
                ),
            )

        assertTrue(created.snapshot.battle.started)
        assertEquals(2, created.snapshot.units.size)

        val targets = service.queryTargets("session-import-1", "thunderbolt", "unit-a")
        assertEquals(listOf("unit-b"), targets.availableTargetUnitIds)

        val afterMove =
            service.submitMoveChoice(
                sessionId = "session-import-1",
                request =
                    SmartMoveChoiceRequest(
                        attackerId = "unit-a",
                        moveId = "thunderbolt",
                        accuracyRoll = 1,
                    ),
            )
        service.submitRunChoice("session-import-1", SubmitRunChoiceRequest(sideId = "side-b", priority = 1))
        val turnResult = service.resolveTurn("session-import-1")

        assertEquals(1, afterMove.pendingActions.size)
        assertEquals(BattleSessionActionKind.MOVE, afterMove.pendingActions.first().kind)
        assertTrue(turnResult.snapshot.battle.ended)
        assertEquals("run", turnResult.snapshot.battle.endedReason)
    }

    @Test
    fun shouldComputeNativeDamageThroughServiceWhenRequestDoesNotProvideDamage() {
        service.createImportedSession(
            CreateImportedBattleSessionApiRequest(
                sessionId = "session-native-damage-1",
                formatId = "single",
                battleKind = BattleType.TRAINER,
                sides =
                    listOf(
                        GameBattleImportedSideApiRequest(
                            sideId = "side-a",
                            units =
                                listOf(
                                    GameBattleImportedUnitApiRequest(
                                        unitId = "unit-a",
                                        level = 50,
                                        creatureInternalName = "pikachu",
                                        moves = listOf(GameBattleImportedMoveApiRequest("thunderbolt")),
                                    ),
                                ),
                        ),
                        GameBattleImportedSideApiRequest(
                            sideId = "side-b",
                            units =
                                listOf(
                                    GameBattleImportedUnitApiRequest(
                                        unitId = "unit-b",
                                        level = 50,
                                        creatureInternalName = "squirtle",
                                        moves = listOf(GameBattleImportedMoveApiRequest("thunderbolt")),
                                    ),
                                ),
                        ),
                    ),
            ),
        )

        service.submitMoveChoice(
            sessionId = "session-native-damage-1",
            request =
                SmartMoveChoiceRequest(
                    attackerId = "unit-a",
                    moveId = "thunderbolt",
                    targetId = "unit-b",
                    accuracyRoll = 1,
                    criticalHit = false,
                    chanceRoll = 99,
                    damageRoll = 100,
                ),
        )
        service.submitMoveChoice(
            sessionId = "session-native-damage-1",
            request =
                SmartMoveChoiceRequest(
                    attackerId = "unit-b",
                    moveId = "thunderbolt",
                    targetId = "unit-a",
                    accuracyRoll = 1,
                    criticalHit = false,
                    chanceRoll = 99,
                    damageRoll = 100,
                ),
        )

        val turnResult = service.resolveTurn("session-native-damage-1")
        val playerMoveResult =
            turnResult.actionResults.first { result ->
                result.action.submittingUnitId == "unit-a"
            }
        val defeatedTarget =
            turnResult.snapshot.units.first { unit ->
                unit.id == "unit-b"
            }

        assertEquals(146, playerMoveResult.moveResult?.damage)
        assertEquals(100, playerMoveResult.moveResult?.damageRoll)
        assertEquals(true, playerMoveResult.moveResult?.hitSuccessful)
        assertEquals(false, playerMoveResult.moveResult?.criticalHit)
        assertEquals(0, defeatedTarget.currentHp)
        assertTrue(turnResult.snapshot.battle.ended)
    }

    @Test
    fun shouldQueueCaptureChoiceResolveTurnAndSettleWildBattle() {
        service.createImportedSession(
            CreateImportedBattleSessionApiRequest(
                sessionId = "session-capture-1",
                formatId = "single",
                battleKind = BattleType.WILD,
                capturableSideId = "side-b",
                sides =
                    listOf(
                        GameBattleImportedSideApiRequest(
                            sideId = "side-a",
                            units = listOf(GameBattleImportedUnitApiRequest(unitId = "unit-a", level = 50, creatureInternalName = "pikachu")),
                        ),
                        GameBattleImportedSideApiRequest(
                            sideId = "side-b",
                            units = listOf(GameBattleImportedUnitApiRequest(unitId = "unit-b", level = 10, creatureInternalName = "squirtle")),
                        ),
                    ),
            ),
        )

        val submitted =
            service.submitCaptureChoice(
                sessionId = "session-capture-1",
                request =
                    SmartCaptureChoiceRequest(
                        playerId = "1",
                        ballItemId = "great-ball",
                        sourceUnitId = "unit-a",
                        targetId = "unit-b",
                    ),
            )
        val turnResult = service.resolveTurn("session-capture-1")
        val settled = service.settleSession("session-capture-1")
        val captureResult = requireNotNull(settled.captureResult)

        assertEquals(BattleSessionActionKind.CAPTURE, submitted.pendingActions.first().kind)
        assertEquals("capture", turnResult.snapshot.battle.endedReason)
        assertTrue(settled.settled)
        assertTrue(captureResult.success)
        assertEquals("great-ball", captureResult.ballItemId)
        assertEquals("unit-b", captureResult.targetUnitId)
        assertEquals("capture", settled.session.snapshot.battle.endedReason)
        assertEquals(
            "CAPTURE",
            settled.session.resourceLedger
                .single()
                .kind.name,
        )
        assertEquals(1, settled.session.captureResourceLedger.size)
    }

    private class FakeBattleUnitImporter : BattleUnitImporter {
        override fun importUnit(request: BattleUnitImportRequest): BattleUnitImportResult {
            val movePp =
                request.moves.associate { move ->
                    move.moveId to (move.currentPp ?: 15)
                }
            val unit =
                when (request.unitId) {
                    "unit-a" -> {
                        UnitState(
                            id = "unit-a",
                            currentHp = 140,
                            maxHp = 140,
                            abilityId = "static",
                            typeIds = setOf("electric"),
                            stats =
                                mapOf(
                                    "speed" to 120,
                                    "special-attack" to 120,
                                    "special-defense" to 90,
                                ),
                            movePp = movePp,
                            flags =
                                mapOf(
                                    "level" to request.level.toString(),
                                    "creatureId" to "25",
                                    "creatureSpeciesId" to "25",
                                    "creatureInternalName" to "pikachu",
                                    "creatureName" to "Pikachu",
                                    "captureRate" to "190",
                                ),
                        )
                    }

                    else -> {
                        UnitState(
                            id = "unit-b",
                            currentHp = 120,
                            maxHp = 120,
                            itemId = "smoke-ball",
                            typeIds = setOf("water"),
                            stats =
                                mapOf(
                                    "speed" to 80,
                                    "special-attack" to 60,
                                    "special-defense" to 100,
                                ),
                            movePp = movePp,
                            flags =
                                mapOf(
                                    "level" to request.level.toString(),
                                    "creatureId" to "7",
                                    "creatureSpeciesId" to "7",
                                    "creatureInternalName" to "squirtle",
                                    "creatureName" to "Squirtle",
                                    "captureRate" to "45",
                                ),
                        )
                    }
                }

            return BattleUnitImportResult(
                unit = unit,
                creatureId = if (request.unitId == "unit-a") 25L else 7L,
                creatureInternalName = request.creatureInternalName ?: request.unitId,
                creatureName = request.creatureInternalName ?: request.unitId,
                level = request.level,
                requiredExperience = 0,
                calculatedStats = unit.stats,
            )
        }
    }

    private class FakePlayerInventoryService :
        DefaultPlayerInventoryService(
            mock(PlayerInventoryItemRepository::class.java),
        ) {
        override fun ensureAvailable(
            playerId: Long,
            itemId: Long,
            count: Int,
        ) = Unit

        override fun consume(
            playerId: Long,
            itemId: Long,
            count: Int,
        ) = Unit
    }

    private class FakeOwnedCreatureService :
        DefaultOwnedCreatureService(
            mock(CreatureStorageBoxRepository::class.java),
            mock(OwnedCreatureRepository::class.java),
            mock(OwnedCreatureStatRepository::class.java),
            mock(OwnedCreatureMoveRepository::class.java),
            mock(StatRepository::class.java),
            mock(MoveRepository::class.java),
        ) {
        override fun capture(context: io.github.lishangbu.avalon.game.service.capture.PreparedCaptureContext): CapturedCreatureSummary =
            CapturedCreatureSummary(
                ownedCreatureId = "1000",
                creatureId = context.targetMetadata.creatureId.toString(),
                creatureSpeciesId = context.targetMetadata.creatureSpeciesId.toString(),
                creatureInternalName = context.targetMetadata.creatureInternalName,
                creatureName = context.targetMetadata.creatureName,
            )
    }

    private class FakeBattleCaptureRecordService :
        DefaultBattleCaptureRecordService(
            mock(BattleCaptureRecordRepository::class.java),
        ) {
        override fun record(
            context: io.github.lishangbu.avalon.game.service.capture.PreparedCaptureContext,
            result: CaptureFormulaResult,
            capturedCreature: CapturedCreatureSummary?,
        ) = Unit
    }

    private class FakeCapturePreparationService :
        DefaultCapturePreparationService(
            mock(ItemRepository::class.java),
            mock(CreatureSpeciesRepository::class.java),
            mock(PlayerRepository::class.java),
            mock(OwnedCreatureRepository::class.java),
        ) {
        override fun prepare(
            sessionId: String,
            snapshot: io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleRuntimeSnapshot,
            command: CaptureCommand,
        ) = io.github.lishangbu.avalon.game.service.capture.PreparedCaptureContext(
            sessionId = sessionId,
            playerId = command.playerId.toLong(),
            ballItemId = 2L,
            ballItemInternalName = command.ballItemId,
            targetUnitId = command.targetUnitId,
            sourceUnitId = command.sourceUnitId,
            snapshot = snapshot,
            targetUnit = snapshot.units.getValue(command.targetUnitId),
            sourceUnit = command.sourceUnitId?.let(snapshot.units::get),
            targetMetadata =
                BattleUnitMetadata(
                    creatureId = 7L,
                    creatureSpeciesId = 7L,
                    creatureInternalName = "squirtle",
                    creatureName = "Squirtle",
                    level = 10,
                    requiredExperience = 0,
                    natureId = null,
                    captureRate = 45,
                    ivs = emptyMap(),
                    evs = emptyMap(),
                    calculatedStats = snapshot.units.getValue(command.targetUnitId).stats,
                ),
            battleContext = CaptureContext(),
        )
    }

    private class FakeCaptureActionResolver : CaptureActionResolver {
        override fun resolve(
            session: io.github.lishangbu.avalon.game.battle.engine.session.BattleSession,
            action: io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionCaptureAction,
        ) = io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionCaptureResult(
            success = true,
            playerId = action.playerId,
            ballItemId = action.ballItemId,
            sourceUnitId = action.sourceUnitId,
            targetId = action.targetId,
            shakes = 4,
            reason = "all-shakes-passed",
            finalRate = 100.0,
        )
    }

    private class FakeBattleTypeEffectivenessResolver : BattleTypeEffectivenessResolver {
        override fun resolve(
            moveType: String?,
            attacker: UnitState?,
            target: UnitState?,
        ): Double = if (moveType == "electric" && target?.typeIds == setOf("water")) 2.0 else 1.0
    }
}
