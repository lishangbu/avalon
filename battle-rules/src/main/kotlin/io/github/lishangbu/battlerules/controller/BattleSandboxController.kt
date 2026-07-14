package io.github.lishangbu.battlerules.controller

import io.github.lishangbu.common.web.security.RequireBattleSandboxRun

import io.github.lishangbu.battlerules.dto.BattleSandboxTurnRequest
import io.github.lishangbu.battlerules.dto.BattleSandboxTurnResponse
import io.github.lishangbu.battlerules.dto.BattleSandboxReplayRequest
import io.github.lishangbu.battlerules.dto.BattleSandboxReplayResponse
import io.github.lishangbu.battlerules.dto.BattleSandboxReplaySummaryResponse
import io.github.lishangbu.battlerules.dto.BattleSandboxReplayValidationResponse
import io.github.lishangbu.battlerules.openapi.BATTLE_SANDBOX_API_BEARER_AUTH
import io.github.lishangbu.battlerules.service.BattleSandboxReplayService
import io.github.lishangbu.battlerules.service.BattleRuntimeSnapshotService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.babyfish.jimmer.Page
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 战斗沙盒 API。
 *
 * 沙盒提供“输入完整队伍、行动和可选状态快照，立即在内存中结算一个回合”的调试入口。它刻意不保存对局、
 * 不分配房间，避免把运行时验证和未来真人对战状态管理绑死在一起；连续回合由客户端带回响应快照完成。
 */
@RequireBattleSandboxRun
@RestController
@RequestMapping("/api/battle-sandbox")
@Tag(name = "战斗沙盒")
@SecurityRequirement(name = BATTLE_SANDBOX_API_BEARER_AUTH)
class BattleSandboxController(
	private val service: BattleRuntimeSnapshotService,
	private val replayService: BattleSandboxReplayService,
) {
	@PostMapping("/turn")
	@Operation(summary = "结算沙盒回合")
	fun resolveTurn(@RequestBody request: BattleSandboxTurnRequest): BattleSandboxTurnResponse =
		service.resolveSandboxTurn(request)

	@GetMapping("/replays")
	@Operation(summary = "分页查询沙盒复盘")
	fun listReplays(
		@RequestParam(defaultValue = "0") page: Int,
		@RequestParam(defaultValue = "20") size: Int,
		@RequestParam(required = false) q: String?,
	): Page<BattleSandboxReplaySummaryResponse> =
		replayService.list(page, size, q)

	@GetMapping("/replays/{id}")
	@Operation(summary = "读取沙盒复盘")
	fun getReplay(@PathVariable id: Long): BattleSandboxReplayResponse =
		replayService.get(id)

	@PostMapping("/replays/{id}/validation")
	@Operation(summary = "校验沙盒复盘")
	fun validateReplay(@PathVariable id: Long): BattleSandboxReplayValidationResponse =
		replayService.validate(id)

	@PostMapping("/replays")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "保存沙盒复盘")
	fun createReplay(@RequestBody request: BattleSandboxReplayRequest): BattleSandboxReplayResponse =
		replayService.create(request)

	@DeleteMapping("/replays/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "删除沙盒复盘")
	fun deleteReplay(@PathVariable id: Long) {
		replayService.delete(id)
	}
}
