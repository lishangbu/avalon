package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * 捕捉动作资源账本条目。
 *
 * @property playerId 发起捕捉的玩家标识。
 * @property sourceUnitId 扔球单位标识。
 * @property ballItemId 使用的球类道具标识。
 * @property targetUnitId 被捕捉的目标单位标识。
 * @property quantity 本次消耗的球数量。
 * @property success 本次捕捉是否成功。
 * @property shakes 本次捕捉的摇晃次数。
 * @property reason 本次捕捉结果原因。
 * @property finalRate 本次捕捉的最终概率。
 */
data class BattleSessionCaptureResourceUsage(
    val playerId: String,
    val sourceUnitId: String,
    val ballItemId: String,
    val targetUnitId: String,
    val quantity: Int = 1,
    val success: Boolean,
    val shakes: Int,
    val reason: String,
    val finalRate: Double,
) : BattleSessionResourceUsage {
    /**
     * 当前账本条目的业务种类。
     */
    override val kind: BattleSessionResourceUsageKind = BattleSessionResourceUsageKind.CAPTURE
}
