package io.github.lishangbu.avalon.game.battle.engine.session

import io.github.lishangbu.avalon.game.battle.engine.model.BattleState
import io.github.lishangbu.avalon.game.battle.engine.model.BattleType
import io.github.lishangbu.avalon.game.battle.engine.model.FieldState
import io.github.lishangbu.avalon.game.battle.engine.model.SideState
import io.github.lishangbu.avalon.game.battle.engine.model.UnitState
import io.github.lishangbu.avalon.game.battle.engine.repository.EffectDefinitionRepository
import io.github.lishangbu.avalon.game.battle.engine.runtime.apply.MutationApplier
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleFlowEngine
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleRuntimeSnapshot
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.MoveResolutionResult
import io.github.lishangbu.avalon.game.battle.engine.service.CaptureActionResolver
import io.github.lishangbu.avalon.game.battle.engine.session.specification.BattleSessionCaptureChoiceSpecification
import io.github.lishangbu.avalon.game.battle.engine.session.specification.BattleSessionRunChoiceSpecification
import io.github.lishangbu.avalon.game.battle.engine.session.specification.BattleSessionTargetChoiceSpecification
import io.github.lishangbu.avalon.game.battle.engine.session.specification.BattleSessionTurnReadySpecification
import io.github.lishangbu.avalon.game.battle.engine.session.specification.BattleSessionUnitChoiceSpecification
import io.github.lishangbu.avalon.game.battle.engine.session.target.BattleSessionTargetQuery
import io.github.lishangbu.avalon.game.battle.engine.session.target.BattleSessionTargetQueryService

/**
 * 最小可跑的 battle session。
 *
 * 设计意图：
 * - 把 BattleRuntimeSnapshot 和 BattleFlowEngine 包装成可持续推进的最小会话对象。
 * - 提供“开始、注册单位、收集动作、执行回合、结束回合、自动替补、最小胜负判定”能力。
 *
 * 当前阶段不负责：
 * - 玩家决策收集
 * - 完整行动优先级系统
 * - 玩家可选替补决策
 * - 复杂胜负规则
 *
 * @property effectRepository effect 定义查询入口。
 * @property battleFlowEngine battle runtime 主流程入口。
 * @property mutationApplier 结构化 mutation 写回组件。
 * @property replacementStrategy 自动替补选择策略。
 * @property captureActionResolver battle 内捕捉动作解析器。
 * @property actionExecutionSupport session action 共用执行辅助组件。
 * @property choiceHandlerRegistry choice 提交处理器注册中心。
 * @property actionHandlerRegistry action 执行处理器注册中心。
 * @property turnPipeline 回合推进 pipeline。
 * @property turnReadySpecification 回合结算前置规格。
 * @property unitChoiceSpecification active 单位行动提交规格。
 * @property runChoiceSpecification side 逃跑提交规格。
 * @property targetChoiceSpecification effect 目标合法性规格。
 * @property captureChoiceSpecification 捕捉动作合法性规格。
 * @property targetQueryService battle session 目标查询服务。
 * @property actionSortingStrategy battle session action 排序策略。
 * @property eventPublisher battle session 内部事件发布器。
 * @property commandFactory battle session 命令工厂。
 */
class BattleSession(
    internal val effectRepository: EffectDefinitionRepository,
    internal val battleFlowEngine: BattleFlowEngine,
    internal val mutationApplier: MutationApplier,
    internal val replacementStrategy: ReplacementStrategy,
    internal val captureActionResolver: CaptureActionResolver,
    internal val actionExecutionSupport: BattleSessionActionExecutionSupport,
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
    battleId: String,
    formatId: String,
) {
    /**
     * 当前回合待执行动作队列。
     */
    internal val actionQueue: BattleSessionActionQueue = BattleSessionActionQueue(actionSortingStrategy)

    /**
     * 面向玩家可读的 battle log 缓存。
     */
    internal val battleLogs: MutableList<String> = mutableListOf()

    /**
     * 结构化事件日志缓存。
     */
    internal val eventLogs: MutableList<BattleSessionEvent> = mutableListOf()

    /**
     * 当前待处理的替补请求集合。
     */
    internal val replacementRequests: MutableList<BattleSessionReplacementRequest> = mutableListOf()

    /**
     * battle 内资源消耗账本。
     */
    internal val resourceLedger: MutableList<BattleSessionResourceUsage> = mutableListOf()

    /**
     * 当前会话持有的最新运行时快照。
     */
    internal var currentSnapshot: BattleRuntimeSnapshot =
        BattleRuntimeSnapshot(
            battle = BattleState(id = battleId, formatId = formatId),
            field = FieldState(),
            units = emptyMap(),
            sides = emptyMap(),
        )

    /**
     * choice 合法性校验器。
     */
    private val choiceValidator: BattleSessionChoiceValidator =
        BattleSessionChoiceValidator(
            session = this,
            turnReadySpecification = turnReadySpecification,
            unitChoiceSpecification = unitChoiceSpecification,
            runChoiceSpecification = runChoiceSpecification,
            targetChoiceSpecification = targetChoiceSpecification,
            captureChoiceSpecification = captureChoiceSpecification,
        )

    /**
     * 已入队 action 的执行协调器。
     */
    private val actionExecutor: BattleSessionActionExecutor = BattleSessionActionExecutor(this, actionHandlerRegistry)

    /**
     * 濒死、替补与胜负更新协调器。
     */
    private val replacementResolver: BattleSessionReplacementResolver = BattleSessionReplacementResolver(this)

    /**
     * 回合末推进与替补提交流程协调器。
     */
    private val lifecycleCoordinator: BattleSessionLifecycleCoordinator = BattleSessionLifecycleCoordinator(this)

    /**
     * 返回当前会话快照。
     */
    fun snapshot(): BattleRuntimeSnapshot = currentSnapshot

    /**
     * 返回当前会话的统一查询结果。
     */
    fun query(): BattleSessionQuery =
        BattleSessionQuery(
            snapshot = snapshot(),
            pendingActions = pendingActions(),
            choiceStatuses = choiceStatuses(),
            replacementRequests = pendingReplacementRequests(),
            resourceLedger = resourceLedger(),
            battleLogs = battleLogs(),
            eventLogs = eventLogs(),
        )

    /**
     * 查询某个 effect 在当前会话下的目标模式和可选目标。
     *
     * @param effectId 被查询的 effect 标识
     * @param actorUnitId 当前出手单位标识
     * @return 当前快照下的目标查询结果
     */
    fun queryTargets(
        effectId: String,
        actorUnitId: String,
    ): BattleSessionTargetQuery =
        targetQueryService.resolve(
            snapshot = currentSnapshot,
            effectId = effectId,
            actorUnitId = actorUnitId,
        )

    /**
     * 返回当前待执行动作队列快照。
     */
    fun pendingActions(): List<BattleSessionAction> = actionQueue.snapshot()

    /**
     * 返回每个 side 的当前回合输入状态。
     */
    fun choiceStatuses(): List<BattleSessionChoiceStatus> =
        currentSnapshot.sides.values.map { side ->
            val activeUnitIds = side.activeUnitIds
            val submittedUnitIds = submittedUnitIdsForSide(side)
            val missingUnitIds = activeUnitIds.filterNot { unitId -> unitId in submittedUnitIds }
            BattleSessionChoiceStatus(
                sideId = side.id,
                activeUnitIds = activeUnitIds,
                submittedUnitIds = submittedUnitIds,
                missingUnitIds = missingUnitIds,
                requiredActionCount = activeUnitIds.size,
                submittedActionCount = submittedUnitIds.size,
                ready = missingUnitIds.isEmpty(),
            )
        }

    /**
     * 返回当前回合尚未提交行动的 side 标识列表。
     *
     * 约定：
     * - 仅统计当前仍有 active 单位的 side。
     * - side 下只要还有任一 active 单位未提交行动，就视为该 side 仍缺少输入。
     */
    fun missingChoiceSideIds(): List<String> =
        choiceStatuses()
            .filterNot(BattleSessionChoiceStatus::ready)
            .map(BattleSessionChoiceStatus::sideId)

    /**
     * 返回本回合尚未提交行动的 active 单位列表。
     */
    fun pendingChoiceUnitIds(): List<String> =
        choiceStatuses()
            .flatMap(BattleSessionChoiceStatus::missingUnitIds)

    /**
     * 当前回合是否已经满足最小提交条件。
     */
    fun isTurnReady(): Boolean = missingChoiceSideIds().isEmpty()

    /**
     * 当前回合是否允许进入结算。
     *
     * 额外约束：
     * - 不能有未完成的 replacement request。
     */
    fun canResolveTurn(): Boolean = isTurnReady() && replacementRequests.isEmpty()

    /**
     * 返回人类可读 battle log 快照。
     */
    fun battleLogs(): List<String> = battleLogs.toList()

    /**
     * 返回结构化 event log 快照。
     */
    fun eventLogs(): List<BattleSessionEvent> = eventLogs.toList()

    /**
     * 返回当前待处理的替补请求。
     */
    fun pendingReplacementRequests(): List<BattleSessionReplacementRequest> = replacementRequests.toList()

    /**
     * 返回当前 battle 内部资源账本。
     */
    fun resourceLedger(): List<BattleSessionResourceUsage> = resourceLedger.toList()

    /**
     * 导出当前可持久化状态。
     */
    fun exportState(): BattleSessionState =
        BattleSessionState(
            snapshot = currentSnapshot,
            pendingActions = pendingActions(),
            replacementRequests = pendingReplacementRequests(),
            resourceLedger = resourceLedger(),
            battleLogs = battleLogs(),
            eventLogs = eventLogs(),
        )

    /**
     * 用已持久化状态恢复当前 session。
     */
    fun restoreState(state: BattleSessionState): BattleRuntimeSnapshot {
        currentSnapshot = state.snapshot
        actionQueue.replaceAll(state.pendingActions)
        replacementRequests.clear()
        replacementRequests += state.replacementRequests
        resourceLedger.clear()
        resourceLedger += state.resourceLedger
        battleLogs.clear()
        battleLogs += state.battleLogs
        eventLogs.clear()
        eventLogs += state.eventLogs
        return currentSnapshot
    }

    /**
     * 启动 battle session。
     *
     * 约定：
     * - 启动后 turn 初始化为 1。
     */
    fun start(): BattleRuntimeSnapshot {
        val battle = currentSnapshot.battle.copy(started = true, turn = 1)
        currentSnapshot = currentSnapshot.copy(battle = battle)
        recordLog("Battle ${battle.id} started at turn ${battle.turn}.")
        recordEvent(BattleSessionStartedPayload)
        return currentSnapshot
    }

    /**
     * 注册一个 side。
     *
     * @param sideId 待注册的 side 标识
     * @return 更新后的快照
     */
    fun registerSide(sideId: String): BattleRuntimeSnapshot {
        val nextSides = currentSnapshot.sides + (sideId to SideState(id = sideId))
        currentSnapshot = currentSnapshot.copy(sides = nextSides)
        recordLog("Registered side $sideId.")
        recordEvent(
            BattleSessionSideRegisteredPayload(sideId = sideId),
        )
        return currentSnapshot
    }

    /**
     * 配置当前 battle 的业务语义，例如 wild/trainer 和可捕捉 side。
     */
    fun configureBattle(
        battleKind: BattleType,
        capturableSideId: String? = null,
    ): BattleRuntimeSnapshot {
        currentSnapshot =
            currentSnapshot.copy(
                battle =
                    currentSnapshot.battle.copy(
                        battleKind = battleKind,
                        capturableSideId = capturableSideId,
                    ),
            )
        recordLog("Configured battle kind=${battleKind.name} capturableSideId=$capturableSideId.")
        return currentSnapshot
    }

    /**
     * 注册一个单位并挂到指定 side。
     *
     * @param sideId 目标 side 标识
     * @param unit 要注册的单位状态
     * @param active 当前是否直接加入 activeUnitIds
     * @return 更新后的快照
     */
    fun registerUnit(
        sideId: String,
        unit: UnitState,
        active: Boolean = true,
    ): BattleRuntimeSnapshot {
        val side =
            requireNotNull(currentSnapshot.sides[sideId]) {
                "Side '$sideId' must be registered before units can be added."
            }
        val nextUnits = currentSnapshot.units + (unit.id to unit)
        val nextSide =
            if (active) {
                side.copy(
                    unitIds = (side.unitIds + unit.id).distinct(),
                    activeUnitIds = (side.activeUnitIds + unit.id).distinct(),
                )
            } else {
                side.copy(unitIds = (side.unitIds + unit.id).distinct())
            }
        val nextSides = currentSnapshot.sides + (sideId to nextSide)
        currentSnapshot =
            currentSnapshot.copy(
                units = nextUnits,
                sides = nextSides,
            )
        recordLog("Registered unit ${unit.id} to side $sideId.")
        recordEvent(
            BattleSessionUnitRegisteredPayload(
                sideId = sideId,
                unitId = unit.id,
                active = active,
            ),
        )
        return currentSnapshot
    }

    /**
     * 执行一次出招。
     *
     * @param moveId 招式或 effect 标识
     * @param attackerId 出手单位标识
     * @param targetId 目标单位标识
     * @param accuracy 命中率输入；为空时交由底层默认规则处理
     * @param evasion 回避率输入；为空时交由底层默认规则处理
     * @param basePower 基础威力输入
     * @param damage 预期直接伤害
     * @param attributes 附加属性
     * @return 最终结算结果
     */
    fun useMove(
        moveId: String,
        attackerId: String,
        targetId: String,
        accuracy: Int? = null,
        evasion: Int? = null,
        basePower: Int,
        damage: Int,
        attributes: Map<String, Any?> = emptyMap(),
    ): MoveResolutionResult {
        ensureStarted()
        return executeResolvedEffect(
            effectId = moveId,
            actorUnitId = attackerId,
            targetUnitId = targetId,
            accuracy = accuracy,
            evasion = evasion,
            basePower = basePower,
            damage = damage,
            attributes = attributes,
        )
    }

    /**
     * 将一个 move action 加入当前回合待执行队列。
     *
     * @param moveId 招式或 effect 标识
     * @param attackerId 出手单位标识
     * @param targetId 目标单位标识
     * @param priority 行动优先级
     * @param speed 排序速度
     * @param accuracy 命中率输入
     * @param evasion 回避率输入
     * @param basePower 基础威力
     * @param damage 预期直接伤害
     * @param attributes 附加属性
     * @return 当前动作队列快照
     */
    fun queueMove(
        moveId: String,
        attackerId: String,
        targetId: String,
        priority: Int = 0,
        speed: Int = 0,
        accuracy: Int? = null,
        evasion: Int? = null,
        basePower: Int,
        damage: Int,
        attributes: Map<String, Any?> = emptyMap(),
    ): List<BattleSessionAction> {
        ensureStarted()
        ensureUnitCanSubmitChoice(attackerId)
        ensureTargetIsLegalForAction(effectId = moveId, actorUnitId = attackerId, targetUnitId = targetId)
        actionQueue.enqueue(
            commandFactory.createMoveAction(
                moveId = moveId,
                attackerId = attackerId,
                targetId = targetId,
                priority = priority,
                speed = speed,
                accuracy = accuracy,
                evasion = evasion,
                basePower = basePower,
                damage = damage,
                attributes = attributes,
            ),
        )
        recordLog("Queued move $moveId from $attackerId to $targetId.")
        recordEvent(
            BattleSessionMoveQueuedPayload(
                moveId = moveId,
                attackerId = attackerId,
                targetId = targetId,
                priority = priority,
                speed = speed,
            ),
        )
        return actionQueue.snapshot()
    }

    /**
     * 提交一个玩家出招选择。
     *
     * @param moveId 招式或 effect 标识
     * @param attackerId 出手单位标识
     * @param targetId 目标单位标识
     * @param priority 行动优先级
     * @param speed 排序速度
     * @param accuracy 命中率输入
     * @param evasion 回避率输入
     * @param basePower 基础威力
     * @param damage 预期直接伤害
     * @param attributes 附加属性
     * @return 当前动作队列快照
     */
    fun submitMoveChoice(
        moveId: String,
        attackerId: String,
        targetId: String,
        priority: Int = 0,
        speed: Int = 0,
        accuracy: Int? = null,
        evasion: Int? = null,
        basePower: Int,
        damage: Int,
        attributes: Map<String, Any?> = emptyMap(),
    ): List<BattleSessionAction> =
        submitChoice(
            commandFactory.createMoveChoice(
                moveId = moveId,
                attackerId = attackerId,
                targetId = targetId,
                priority = priority,
                speed = speed,
                accuracy = accuracy,
                evasion = evasion,
                basePower = basePower,
                damage = damage,
                attributes = attributes,
            ),
        )

    /**
     * 提交一个统一 choice 输入。
     *
     * 设计意图：
     * - 为调用方提供统一的 session 输入入口。
     * - 保留具体 `submitMoveChoice/submitSwitchChoice/...` 作为薄包装。
     *
     * @param choice 本次要提交的统一选择对象
     * @return 当前动作队列快照
     */
    fun submitChoice(choice: BattleSessionChoice): List<BattleSessionAction> = choiceHandlerRegistry.get(choice).submit(choice, this)

    /**
     * 批量提交多个统一 choice 输入。
     *
     * @param choices 本次要提交的选择列表
     * @return 当前动作队列快照
     */
    fun submitChoices(choices: List<BattleSessionChoice>): List<BattleSessionAction> {
        choices.forEach(::submitChoice)
        return actionQueue.snapshot()
    }

    /**
     * 提交一个捕捉选择。
     */
    fun submitCaptureChoice(
        playerId: String,
        ballItemId: String,
        sourceUnitId: String,
        targetId: String,
        priority: Int = 0,
        speed: Int = 0,
    ): List<BattleSessionAction> =
        submitChoice(
            commandFactory.createCaptureChoice(
                playerId = playerId,
                ballItemId = ballItemId,
                sourceUnitId = sourceUnitId,
                targetId = targetId,
                priority = priority,
                speed = speed,
            ),
        )

    /**
     * 提交一个等待动作。
     */
    fun submitWaitChoice(
        unitId: String,
        priority: Int = 0,
        speed: Int = 0,
    ): List<BattleSessionAction> =
        submitChoice(
            commandFactory.createWaitChoice(
                unitId = unitId,
                priority = priority,
                speed = speed,
            ),
        )

    /**
     * 提交一个玩家替换选择。
     *
     * @param sideId 发起替换的 side 标识
     * @param outgoingUnitId 当前下场单位
     * @param incomingUnitId 即将上场单位
     * @param priority 行动优先级
     * @param speed 排序速度
     * @return 当前动作队列快照
     */
    fun submitSwitchChoice(
        sideId: String,
        outgoingUnitId: String,
        incomingUnitId: String,
        priority: Int = 0,
        speed: Int = 0,
    ): List<BattleSessionAction> =
        submitChoice(
            commandFactory.createSwitchChoice(
                sideId = sideId,
                outgoingUnitId = outgoingUnitId,
                incomingUnitId = incomingUnitId,
                priority = priority,
                speed = speed,
            ),
        )

    /**
     * 提交一个玩家物品使用选择。
     *
     * @param itemId 物品或 effect 标识
     * @param actorUnitId 使用者单位标识
     * @param targetId 目标单位标识
     * @param priority 行动优先级
     * @param speed 排序速度
     * @param attributes 附加属性
     * @return 当前动作队列快照
     */
    fun submitItemChoice(
        itemId: String,
        actorUnitId: String,
        targetId: String,
        priority: Int = 0,
        speed: Int = 0,
        attributes: Map<String, Any?> = emptyMap(),
    ): List<BattleSessionAction> =
        submitChoice(
            commandFactory.createItemChoice(
                itemId = itemId,
                actorUnitId = actorUnitId,
                targetId = targetId,
                priority = priority,
                speed = speed,
                attributes = attributes,
            ),
        )

    /**
     * 提交一个玩家逃跑选择。
     *
     * @param sideId 发起逃跑的 side 标识
     * @param priority 行动优先级
     * @param speed 排序速度
     * @return 当前动作队列快照
     */
    fun submitRunChoice(
        sideId: String,
        priority: Int = 0,
        speed: Int = 0,
    ): List<BattleSessionAction> =
        submitChoice(
            commandFactory.createRunChoice(
                sideId = sideId,
                priority = priority,
                speed = speed,
            ),
        )

    /**
     * 按当前队列排序规则执行本回合已收集的全部 move action。
     *
     * @return 每个动作对应的执行结果列表
     */
    fun executeQueuedActions(): List<BattleSessionActionExecutionResult> = actionExecutor.executeQueuedActions()

    /**
     * 执行当前回合动作队列，然后推进到回合结束。
     *
     * @return 本回合汇总结果
     */
    fun resolveTurn(): BattleSessionTurnResult = turnPipeline.resolve(this)

    /**
     * 对一个待处理替补请求提交具体选择。
     *
     * @param sideId 需要替补的 side 标识
     * @param incomingUnitId 被选中的替补单位
     * @return 更新后的快照
     */
    fun submitReplacementChoice(
        sideId: String,
        incomingUnitId: String,
    ): BattleRuntimeSnapshot = lifecycleCoordinator.submitReplacementChoice(sideId, incomingUnitId)

    /**
     * 推进到回合结束并执行 residual。
     *
     * @return 回合结束后的快照
     */
    fun endTurn(): BattleRuntimeSnapshot = lifecycleCoordinator.endTurn()

    /**
     * 记录一次捕捉失败，不结束 battle。
     */
    fun recordCaptureFailure(
        ballItemId: String,
        targetUnitId: String,
        shakes: Int,
        reason: String,
        finalRate: Double,
    ): BattleRuntimeSnapshot {
        recordLog(
            "Capture failed: item=$ballItemId target=$targetUnitId shakes=$shakes reason=$reason finalRate=$finalRate.",
        )
        recordEvent(
            BattleSessionCaptureFailedPayload(
                ballItemId = ballItemId,
                targetUnitId = targetUnitId,
                shakes = shakes,
                reason = reason,
                finalRate = finalRate,
            ),
        )
        return currentSnapshot
    }

    /**
     * 以捕捉成功结束当前 battle。
     */
    fun finishByCapture(targetUnitId: String): BattleRuntimeSnapshot {
        currentSnapshot =
            currentSnapshot.copy(
                battle =
                    currentSnapshot.battle.copy(
                        ended = true,
                        winner = null,
                        endedReason = "capture",
                        capturedUnitId = targetUnitId,
                    ),
            )
        recordLog("Battle ended by capture. target=$targetUnitId.")
        recordEvent(
            BattleSessionCaptureSucceededPayload(targetUnitId = targetUnitId),
        )
        return currentSnapshot
    }

    /**
     * 从当前 session RNG 中消费一个 `[0, bound)` 范围内的整数。
     *
     * 每次调用都会推进 battle state 中持久化的随机游标，
     * 因而该结果可以随着 session state 一起被导出和恢复。
     */
    internal fun nextRandomInt(bound: Int): Int {
        val randomResult = currentSnapshot.battle.randomState.nextInt(bound)
        currentSnapshot =
            currentSnapshot.copy(
                battle =
                    currentSnapshot.battle.copy(
                        randomState = randomResult.nextState,
                    ),
            )
        return randomResult.value
    }

    /**
     * 生成一个 `[1, 100]` 的百分比判定值。
     */
    internal fun nextPercentageRoll(): Int = nextRandomInt(100) + 1

    /**
     * 生成一个 `[0, 65535]` 的捕捉摇晃判定值。
     */
    internal fun nextCaptureShakeRoll(): Int = nextRandomInt(65536)

    /**
     * 标记当前 battle 已完成结算。
     */
    fun markSettled(): BattleRuntimeSnapshot {
        currentSnapshot = currentSnapshot.copy(battle = currentSnapshot.battle.copy(settled = true))
        recordLog("Battle settlement completed.")
        return currentSnapshot
    }

    internal fun ensureStarted() = choiceValidator.ensureStarted()

    internal fun ensureTurnReady() = choiceValidator.ensureTurnReady()

    internal fun ensureUnitCanSubmitChoice(unitId: String) = choiceValidator.ensureUnitCanSubmitChoice(unitId)

    internal fun ensureSideCanSubmitRunChoice(sideId: String) = choiceValidator.ensureSideCanSubmitRunChoice(sideId)

    internal fun ensureTargetIsLegalForAction(
        effectId: String,
        actorUnitId: String,
        targetUnitId: String,
    ) = choiceValidator.ensureTargetIsLegalForAction(effectId, actorUnitId, targetUnitId)

    internal fun ensureCaptureIsLegal(
        playerId: String,
        sourceUnitId: String,
        targetUnitId: String,
    ) = choiceValidator.ensureCaptureIsLegal(playerId, sourceUnitId, targetUnitId)

    internal fun executeResolvedEffect(
        effectId: String,
        actorUnitId: String,
        targetUnitId: String,
        accuracy: Int?,
        evasion: Int?,
        basePower: Int,
        damage: Int,
        attributes: Map<String, Any?>,
    ): MoveResolutionResult =
        actionExecutionSupport.executeResolvedEffect(
            session = this,
            effectId = effectId,
            actorUnitId = actorUnitId,
            targetUnitId = targetUnitId,
            accuracy = accuracy,
            evasion = evasion,
            basePower = basePower,
            damage = damage,
            attributes = attributes,
        )

    internal fun applyDirectDamage(
        targetId: String,
        damage: Int,
    ): BattleRuntimeSnapshot = actionExecutionSupport.applyDirectDamage(this, targetId, damage)

    internal fun resolveFaintAndReplacement(snapshot: BattleRuntimeSnapshot = currentSnapshot): BattleRuntimeSnapshot = replacementResolver.resolveFaintAndReplacement(snapshot)

    internal fun updateWinnerIfNeeded(snapshot: BattleRuntimeSnapshot): BattleRuntimeSnapshot = replacementResolver.updateWinnerIfNeeded(snapshot)

    internal fun recordMoveExecution(
        moveId: String,
        attackerId: String,
        targetId: String,
        result: MoveResolutionResult,
    ) = actionExecutionSupport.recordMoveExecution(this, moveId, attackerId, targetId, result)

    internal fun applySwitchAction(action: BattleSessionSwitchAction): BattleRuntimeSnapshot = actionExecutionSupport.applySwitchAction(this, action)

    internal fun applyRunAction(action: BattleSessionRunAction): BattleRuntimeSnapshot = actionExecutionSupport.applyRunAction(this, action)

    internal fun recordResourceUsage(usage: BattleSessionResourceUsage) {
        publish(
            BattleSessionPublication(
                turn = currentSnapshot.battle.turn,
                resourceUsage = usage,
            ),
        )
    }

    internal fun recordLog(message: String) {
        publish(
            BattleSessionPublication(
                turn = currentSnapshot.battle.turn,
                battleLogMessage = message,
            ),
        )
    }

    internal fun recordEvent(payload: BattleSessionEventPayload) {
        publish(
            BattleSessionPublication(
                turn = currentSnapshot.battle.turn,
                eventPayload = payload,
            ),
        )
    }

    /**
     * 发布一条 session 内部事件。
     *
     * @param publication 本次需要投影到各个读模型的事件载体。
     */
    internal fun publish(publication: BattleSessionPublication) {
        eventPublisher.publish(this, publication)
    }

    /**
     * 供 projector 追加一条 battle log。
     *
     * @param message 已经完成格式化的 battle log 文本。
     */
    internal fun appendBattleLog(message: String) {
        battleLogs += message
    }

    /**
     * 供 projector 追加一条结构化事件。
     *
     * @param event 已经完成组装的结构化事件对象。
     */
    internal fun appendEventLog(event: BattleSessionEvent) {
        eventLogs += event
    }

    /**
     * 供 projector 追加一条资源账本记录。
     *
     * @param usage 已经完成组装的资源账本条目。
     */
    internal fun appendResourceUsage(usage: BattleSessionResourceUsage) {
        resourceLedger += usage
    }

    internal fun submittedSideId(action: BattleSessionAction): String? = choiceValidator.submittedSideId(action)

    internal fun sideIdOfUnit(unitId: String): String? = choiceValidator.sideIdOfUnit(unitId)

    internal fun submittedUnitIdsForSide(side: SideState): List<String> = choiceValidator.submittedUnitIdsForSide(side)
}
