package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗沙盒回合结算请求。
 *
 * 请求沿用准备校验和行动校验的轻量队伍结构：调用方只提供赛制 code、双方成员、当前上场成员和本回合行动。
 * 首回合不需要携带 [state]，服务端会在内存中装配并启动战斗；后续回合把上一次响应中的 [state] 原样带回，
 * 服务端会恢复 HP、PP、天气、场地和临时状态后继续结算下一回合。整个过程仍不创建对局记录，也不把沙盒状态
 * 写回数据库。
 */
@Schema(description = "战斗沙盒回合结算请求。")
data class BattleSandboxTurnRequest(
	@field:Schema(description = "赛制稳定 code。", example = "standard-single")
	var formatCode: String = "",
	@field:Schema(description = "双方队伍快照。")
	var sides: List<BattlePreparationSideRequest> = emptyList(),
	@field:Schema(description = "本回合提交行动。")
	var actions: List<BattleActionRequest> = emptyList(),
	@field:Schema(description = "本回合随机种子；相同输入和种子应得到相同结果。", example = "0")
	var randomSeed: Long = 0,
	@field:Schema(description = "上一次沙盒响应返回的状态快照；首回合为空。", nullable = true)
	var state: BattleSandboxStateSnapshot? = null,
)
