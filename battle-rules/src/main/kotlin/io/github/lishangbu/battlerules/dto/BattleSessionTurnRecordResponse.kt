package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

/** Session Runtime 成功结算后追加的回合事实。 */
@Schema(description = "Session Runtime 成功结算后追加的回合事实。")
data class BattleSessionTurnRecordResponse(
	@field:Schema(description = "产生本记录的 Turn Command 幂等标识。")
	val commandId: String,
	@field:Schema(description = "结算前 revision。")
	val revisionBefore: Long,
	@field:Schema(description = "结算后 revision。")
	val revisionAfter: Long,
	@field:Schema(description = "已结算回合序号。")
	val turnNumber: Int,
	@field:Schema(description = "按结算顺序固定的行动。")
	val submittedActions: List<BattleActionRequest>,
	@field:Schema(description = "服务端实际消费的随机轨迹。")
	val randomTrace: List<RandomTrace>,
	@field:Schema(description = "本回合新增的战斗事件。")
	val events: List<Event>,
	@field:Schema(description = "结算完成时间。")
	val resolvedAt: Instant,
) {
	@Schema(name = "BattleSessionRandomTrace", description = "单回合服务端随机数消费记录。")
	data class RandomTrace(
		val sequence: Int,
		val bound: Int,
		val reason: String,
		val value: Int,
	)

	@Schema(name = "BattleSessionEvent", description = "单回合新增的结构化战斗事件。")
	data class Event(
		@field:Schema(description = "稳定事件类型。")
		val type: String,
		@field:Schema(description = "事件所属回合。")
		val turnNumber: Int,
		@field:Schema(description = "事件自身字段，不重复包含 type 与 turnNumber。")
		val payload: Map<String, Any?>,
	)
}
