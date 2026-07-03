package io.github.lishangbu.system.controller

import io.github.lishangbu.system.dto.AccessNodeResponse
import io.github.lishangbu.system.service.AccessNodeService
import io.github.lishangbu.common.web.ApiErrorResponse
import io.github.lishangbu.system.openapi.SYSTEM_API_BAD_REQUEST_DESCRIPTION
import io.github.lishangbu.system.openapi.SYSTEM_API_BEARER_AUTH
import io.github.lishangbu.system.openapi.SYSTEM_API_FORBIDDEN_DESCRIPTION
import io.github.lishangbu.system.openapi.SYSTEM_API_NOT_FOUND_DESCRIPTION
import io.github.lishangbu.system.openapi.SYSTEM_API_UNAUTHORIZED_DESCRIPTION
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.babyfish.jimmer.Page
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 访问节点系统管理 API。
 */
@RestController
@ConditionalOnProperty(prefix = "backend.security", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@RequestMapping("/api/system/rbac/access-nodes")
@Tag(
	name = "RBAC 访问节点",
	description = "查询系统内置的菜单目录、路由页面和 API 权限节点。访问节点是角色授权和管理端菜单渲染的共同来源。",
)
class AccessNodeController(
	private val service: AccessNodeService,
) {
	/**
	 * 列出菜单目录、路由页面和 API 访问节点。
	 */
	@GetMapping
	@Operation(
		summary = "查询访问节点列表",
		description = """
			分页查询系统内置访问节点。该接口主要用于角色授权界面、菜单配置预览和权限排查。

			访问节点数据由后端内置契约维护，不通过管理端临时创建；角色只保存访问节点 code 绑定关系。
			当 type、visible、enabled 等过滤条件同时传入时，服务端会按交集返回结果。
		""",
		security = [SecurityRequirement(name = SYSTEM_API_BEARER_AUTH)],
	)
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "200", description = "访问节点列表读取成功。"),
			ApiResponse(
				responseCode = "400",
				description = SYSTEM_API_BAD_REQUEST_DESCRIPTION,
				content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
			),
			ApiResponse(
				responseCode = "401",
				description = SYSTEM_API_UNAUTHORIZED_DESCRIPTION,
				content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
			),
			ApiResponse(
				responseCode = "403",
				description = SYSTEM_API_FORBIDDEN_DESCRIPTION,
				content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
			),
		],
	)
	fun listAccessNodes(
		@Parameter(description = "模糊搜索关键字，匹配访问节点 code 或名称。")
		@RequestParam(required = false) q: String?,
		@Parameter(description = "按权限 code 前缀过滤，常用于按模块或权限域缩小结果，例如 security 或 system.rbac。", example = "system.rbac")
		@RequestParam(required = false) codePrefix: String?,
		@Parameter(description = "访问节点类型。DIRECTORY 表示菜单目录，ROUTE 表示前端页面，API 表示后端接口权限。", example = "ROUTE")
		@RequestParam(required = false) type: String?,
		@Parameter(description = "是否只返回管理端可见节点。API 权限通常不可见，但仍可绑定角色。", example = "true")
		@RequestParam(required = false) visible: Boolean?,
		@Parameter(description = "是否只返回启用节点。禁用节点不会参与菜单和权限判定。", example = "true")
		@RequestParam(required = false) enabled: Boolean?,
		@Parameter(description = "页码，从 0 开始。", example = "0")
		@RequestParam(defaultValue = "0") page: Int,
		@Parameter(description = "每页数量，最大 100。", example = "50")
		@RequestParam(defaultValue = "50") size: Int,
	): Page<AccessNodeResponse> =
		service.listAccessNodes(page, size, q, codePrefix, type, visible, enabled)

	/**
	 * 查询单个访问节点 code。
	 */
	@GetMapping("/{accessNodeCode}")
	@Operation(
		summary = "查询访问节点详情",
		description = """
			按稳定 code 查询单个访问节点的完整元数据，包括节点类型、父节点、前端路由信息和 API 匹配规则。

			该接口适合在角色授权页面点选节点后展示解释信息，也适合排查某个权限 code 为什么能控制指定接口或菜单。
		""",
		security = [SecurityRequirement(name = SYSTEM_API_BEARER_AUTH)],
	)
	@ApiResponses(
		value = [
			ApiResponse(
				responseCode = "200",
				description = "访问节点详情读取成功。",
				content = [Content(schema = Schema(implementation = AccessNodeResponse::class))],
			),
			ApiResponse(
				responseCode = "401",
				description = SYSTEM_API_UNAUTHORIZED_DESCRIPTION,
				content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
			),
			ApiResponse(
				responseCode = "403",
				description = SYSTEM_API_FORBIDDEN_DESCRIPTION,
				content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
			),
			ApiResponse(
				responseCode = "404",
				description = SYSTEM_API_NOT_FOUND_DESCRIPTION,
				content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
			),
		],
	)
	fun getAccessNode(
		@Parameter(description = "访问节点稳定 code。code 是角色绑定和前端权限判断的长期契约。", example = "security:admin")
		@PathVariable accessNodeCode: String,
	): AccessNodeResponse =
		service.getAccessNode(accessNodeCode)
}
