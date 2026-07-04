package io.github.lishangbu.battlerules.controller

import io.github.lishangbu.battlerules.dto.BattleSandboxTurnRequest
import io.github.lishangbu.battlerules.dto.BattleSandboxTurnResponse
import io.github.lishangbu.battlerules.openapi.BATTLE_SANDBOX_API_BEARER_AUTH
import io.github.lishangbu.battlerules.service.BattleRuntimeSnapshotService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 战斗沙盒 API。
 *
 * 沙盒只提供“输入完整队伍和行动，立即在内存中结算一个回合”的调试入口。它刻意不保存对局、不分配房间，
 * 避免第一版把运行时验证和未来真人对战状态管理绑死在一起。
 */
@RestController
@RequestMapping("/api/battle-sandbox")
@Tag(name = "战斗沙盒")
@SecurityRequirement(name = BATTLE_SANDBOX_API_BEARER_AUTH)
class BattleSandboxController(
	private val service: BattleRuntimeSnapshotService,
) {
	@PostMapping("/turn")
	@Operation(summary = "结算沙盒单回合")
	fun resolveTurn(@RequestBody request: BattleSandboxTurnRequest): BattleSandboxTurnResponse =
		service.resolveSandboxTurn(request)
}
