package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * battle session 命令工厂。
 *
 * 设计意图：
 * - 统一负责 `BattleSessionChoice` 与 `BattleSessionAction` 两个命令族的创建。
 * - 让 `BattleSession` 包装方法和 choice handler 只保留校验、编排与日志职责。
 */
interface BattleSessionCommandFactory {
    /**
     * 创建一个 move choice。
     */
    fun createMoveChoice(
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
    ): MoveChoice

    /**
     * 创建一个 item choice。
     */
    fun createItemChoice(
        itemId: String,
        actorUnitId: String,
        targetId: String,
        priority: Int,
        speed: Int,
        attributes: Map<String, Any?>,
    ): ItemChoice

    /**
     * 创建一个 capture choice。
     */
    fun createCaptureChoice(
        playerId: String,
        ballItemId: String,
        sourceUnitId: String,
        targetId: String,
        priority: Int,
        speed: Int,
    ): CaptureChoice

    /**
     * 创建一个 switch choice。
     */
    fun createSwitchChoice(
        sideId: String,
        outgoingUnitId: String,
        incomingUnitId: String,
        priority: Int,
        speed: Int,
    ): SwitchChoice

    /**
     * 创建一个 run choice。
     */
    fun createRunChoice(
        sideId: String,
        priority: Int,
        speed: Int,
    ): RunChoice

    /**
     * 创建一个 wait choice。
     */
    fun createWaitChoice(
        unitId: String,
        priority: Int,
        speed: Int,
    ): WaitChoice

    /**
     * 创建一个 move action。
     */
    fun createMoveAction(
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
    ): BattleSessionMoveAction

    /**
     * 创建一个 item action。
     */
    fun createItemAction(
        itemId: String,
        actorUnitId: String,
        targetId: String,
        priority: Int,
        speed: Int,
        attributes: Map<String, Any?>,
    ): BattleSessionItemAction

    /**
     * 创建一个 capture action。
     */
    fun createCaptureAction(
        playerId: String,
        ballItemId: String,
        sourceUnitId: String,
        targetId: String,
        priority: Int,
        speed: Int,
    ): BattleSessionCaptureAction

    /**
     * 创建一个 switch action。
     */
    fun createSwitchAction(
        sideId: String,
        outgoingUnitId: String,
        incomingUnitId: String,
        priority: Int,
        speed: Int,
    ): BattleSessionSwitchAction

    /**
     * 创建一个 run action。
     */
    fun createRunAction(
        sideId: String,
        priority: Int,
        speed: Int,
    ): BattleSessionRunAction

    /**
     * 创建一个 wait action。
     */
    fun createWaitAction(
        unitId: String,
        priority: Int,
        speed: Int,
    ): BattleSessionWaitAction

    /**
     * 由统一 choice 创建对应的 action。
     *
     * @param choice 已完成校验与补全的 choice。
     * @return 与该 choice 对应的可执行 action。
     */
    fun createAction(choice: BattleSessionChoice): BattleSessionAction
}
