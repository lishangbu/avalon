package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * 默认 battle session 命令工厂。
 *
 * 设计意图：
 * - 集中承载 choice/action 两个命令族的数据对象创建逻辑。
 * - 保证 session 层各个入口对命令对象的创建语义保持一致。
 */
class DefaultBattleSessionCommandFactory : BattleSessionCommandFactory {
    /**
     * 创建一个 move choice。
     */
    override fun createMoveChoice(
        moveId: String,
        attackerId: String,
        targetId: String,
        priority: Int,
        speed: Int,
        accuracy: Int?,
        evasion: Int?,
        basePower: Int,
        damage: Int,
        attributes: Map<String, Any?>,
    ): MoveChoice =
        MoveChoice(
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
        )

    /**
     * 创建一个 item choice。
     */
    override fun createItemChoice(
        itemId: String,
        actorUnitId: String,
        targetId: String,
        priority: Int,
        speed: Int,
        attributes: Map<String, Any?>,
    ): ItemChoice =
        ItemChoice(
            itemId = itemId,
            actorUnitId = actorUnitId,
            targetId = targetId,
            priority = priority,
            speed = speed,
            attributes = attributes,
        )

    /**
     * 创建一个 capture choice。
     */
    override fun createCaptureChoice(
        playerId: String,
        ballItemId: String,
        sourceUnitId: String,
        targetId: String,
        priority: Int,
        speed: Int,
    ): CaptureChoice =
        CaptureChoice(
            playerId = playerId,
            ballItemId = ballItemId,
            sourceUnitId = sourceUnitId,
            targetId = targetId,
            priority = priority,
            speed = speed,
        )

    /**
     * 创建一个 switch choice。
     */
    override fun createSwitchChoice(
        sideId: String,
        outgoingUnitId: String,
        incomingUnitId: String,
        priority: Int,
        speed: Int,
    ): SwitchChoice =
        SwitchChoice(
            sideId = sideId,
            outgoingUnitId = outgoingUnitId,
            incomingUnitId = incomingUnitId,
            priority = priority,
            speed = speed,
        )

    /**
     * 创建一个 run choice。
     */
    override fun createRunChoice(
        sideId: String,
        priority: Int,
        speed: Int,
    ): RunChoice =
        RunChoice(
            sideId = sideId,
            priority = priority,
            speed = speed,
        )

    /**
     * 创建一个 wait choice。
     */
    override fun createWaitChoice(
        unitId: String,
        priority: Int,
        speed: Int,
    ): WaitChoice =
        WaitChoice(
            unitId = unitId,
            priority = priority,
            speed = speed,
        )

    /**
     * 创建一个 move action。
     */
    override fun createMoveAction(
        moveId: String,
        attackerId: String,
        targetId: String,
        priority: Int,
        speed: Int,
        accuracy: Int?,
        evasion: Int?,
        basePower: Int,
        damage: Int,
        attributes: Map<String, Any?>,
    ): BattleSessionMoveAction =
        BattleSessionMoveAction(
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
        )

    /**
     * 创建一个 item action。
     */
    override fun createItemAction(
        itemId: String,
        actorUnitId: String,
        targetId: String,
        priority: Int,
        speed: Int,
        attributes: Map<String, Any?>,
    ): BattleSessionItemAction =
        BattleSessionItemAction(
            itemId = itemId,
            actorUnitId = actorUnitId,
            targetId = targetId,
            priority = priority,
            speed = speed,
            attributes = attributes,
        )

    /**
     * 创建一个 capture action。
     */
    override fun createCaptureAction(
        playerId: String,
        ballItemId: String,
        sourceUnitId: String,
        targetId: String,
        priority: Int,
        speed: Int,
    ): BattleSessionCaptureAction =
        BattleSessionCaptureAction(
            playerId = playerId,
            ballItemId = ballItemId,
            sourceUnitId = sourceUnitId,
            targetId = targetId,
            priority = priority,
            speed = speed,
        )

    /**
     * 创建一个 switch action。
     */
    override fun createSwitchAction(
        sideId: String,
        outgoingUnitId: String,
        incomingUnitId: String,
        priority: Int,
        speed: Int,
    ): BattleSessionSwitchAction =
        BattleSessionSwitchAction(
            sideId = sideId,
            outgoingUnitId = outgoingUnitId,
            incomingUnitId = incomingUnitId,
            priority = priority,
            speed = speed,
        )

    /**
     * 创建一个 run action。
     */
    override fun createRunAction(
        sideId: String,
        priority: Int,
        speed: Int,
    ): BattleSessionRunAction =
        BattleSessionRunAction(
            sideId = sideId,
            priority = priority,
            speed = speed,
        )

    /**
     * 创建一个 wait action。
     */
    override fun createWaitAction(
        unitId: String,
        priority: Int,
        speed: Int,
    ): BattleSessionWaitAction =
        BattleSessionWaitAction(
            unitId = unitId,
            priority = priority,
            speed = speed,
        )

    /**
     * 由统一 choice 创建对应的 action。
     */
    override fun createAction(choice: BattleSessionChoice): BattleSessionAction =
        when (choice) {
            is MoveChoice -> {
                createMoveAction(
                    moveId = choice.moveId,
                    attackerId = choice.attackerId,
                    targetId = choice.targetId,
                    priority = choice.priority,
                    speed = choice.speed,
                    accuracy = choice.accuracy,
                    evasion = choice.evasion,
                    basePower = choice.basePower,
                    damage = choice.damage,
                    attributes = choice.attributes,
                )
            }

            is ItemChoice -> {
                createItemAction(
                    itemId = choice.itemId,
                    actorUnitId = choice.actorUnitId,
                    targetId = choice.targetId,
                    priority = choice.priority,
                    speed = choice.speed,
                    attributes = choice.attributes,
                )
            }

            is CaptureChoice -> {
                createCaptureAction(
                    playerId = choice.playerId,
                    ballItemId = choice.ballItemId,
                    sourceUnitId = choice.sourceUnitId,
                    targetId = choice.targetId,
                    priority = choice.priority,
                    speed = choice.speed,
                )
            }

            is SwitchChoice -> {
                createSwitchAction(
                    sideId = choice.sideId,
                    outgoingUnitId = choice.outgoingUnitId,
                    incomingUnitId = choice.incomingUnitId,
                    priority = choice.priority,
                    speed = choice.speed,
                )
            }

            is RunChoice -> {
                createRunAction(
                    sideId = choice.sideId,
                    priority = choice.priority,
                    speed = choice.speed,
                )
            }

            is WaitChoice -> {
                createWaitAction(
                    unitId = choice.unitId,
                    priority = choice.priority,
                    speed = choice.speed,
                )
            }

            else -> {
                error("Unsupported BattleSessionChoice type '${choice::class.qualifiedName}'.")
            }
        }
}
