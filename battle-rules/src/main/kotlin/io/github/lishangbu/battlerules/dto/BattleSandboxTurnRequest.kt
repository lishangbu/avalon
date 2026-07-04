package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗沙盒单回合结算请求。
 *
 * 请求沿用准备校验和行动校验的轻量队伍结构：调用方只提供赛制 code、双方成员、当前上场成员和本回合行动。
 * 服务端会在内存中装配战斗初始状态并立即结算一回合，不创建对局记录，也不把沙盒状态写回数据库。
 */
@Schema(description = "战斗沙盒单回合结算请求。")
data class BattleSandboxTurnRequest(
	@field:Schema(description = "赛制稳定 code。", example = "standard-single")
	var formatCode: String = "",
	@field:Schema(description = "双方队伍快照。")
	var sides: List<BattlePreparationSideRequest> = emptyList(),
	@field:Schema(description = "本回合提交行动。")
	var actions: List<BattleActionRequest> = emptyList(),
	@field:Schema(description = "本回合随机种子；相同输入和种子应得到相同结果。", example = "0")
	var randomSeed: Long = 0,
)
