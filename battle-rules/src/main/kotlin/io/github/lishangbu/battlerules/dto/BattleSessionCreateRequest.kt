package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 创建 Battle Session 的服务端权威请求。
 *
 * 调用方只描述赛制和阵容，不得提交场内标识、运行态或随机种子。
 */
@Schema(description = "创建 Battle Session 的阵容请求。")
data class BattleSessionCreateRequest(
	@field:Schema(
		description = "赛制稳定 code。",
		example = "official-double",
		requiredMode = Schema.RequiredMode.REQUIRED,
	)
	var formatCode: String = "",
	@field:Schema(description = "双方阵容配置。", requiredMode = Schema.RequiredMode.REQUIRED)
	var sides: List<BattleSessionRosterSideRequest> = emptyList(),
)
