package io.github.lishangbu.system.session

import io.github.lishangbu.common.web.ApiErrorResponse
import io.github.lishangbu.system.openapi.SYSTEM_API_BEARER_AUTH
import io.github.lishangbu.system.openapi.SYSTEM_API_UNAUTHORIZED_DESCRIPTION
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 当前登录态 API。
 */
@RestController
@ConditionalOnProperty(prefix = "backend.security", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@RequestMapping("/api/session")
@Tag(
	name = "当前登录态",
	description = "面向管理端布局和权限初始化的当前用户会话接口。调用方通过该接口获得用户基础信息、角色快照、权限 code 和菜单树。",
)
class SessionController(
	private val service: SessionService,
) {
	/**
	 * 返回当前用户、访问节点快照和管理端可渲染的菜单树。
	 */
	@GetMapping
	@Operation(
		summary = "查询当前登录态",
		description = """
			返回当前 Bearer access token 对应的管理端登录态。

			响应中的 accessNodeCodes 是后端判定后的权限 code 快照，可用于前端按钮、菜单和路由显隐。
			menus 已按后端内置访问节点排序整理成树，前端不需要再根据原始权限节点自行拼装菜单层级。
		""",
		security = [SecurityRequirement(name = SYSTEM_API_BEARER_AUTH)],
	)
	@ApiResponses(
		value = [
			ApiResponse(
				responseCode = "200",
				description = "当前登录态读取成功。",
				content = [Content(schema = Schema(implementation = SessionResponse::class))],
			),
			ApiResponse(
				responseCode = "401",
				description = SYSTEM_API_UNAUTHORIZED_DESCRIPTION,
				content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
			),
		],
	)
	fun currentSession(authentication: Authentication): SessionResponse =
		service.currentSession(authentication)
}
