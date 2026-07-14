package io.github.lishangbu.system.controller

import io.github.lishangbu.common.web.security.RequireSecurityAdmin

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
 * 权限目录系统管理 API。
 */
@RequireSecurityAdmin
@RestController
@ConditionalOnProperty(prefix = "backend.security", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@RequestMapping("/api/system/rbac/access-nodes")
@Tag(
	name = "RBAC 访问节点",
	description = "查询系统内置的权限目录。权限 code 是角色授权和前端功能准入判断的稳定契约。",
)
class AccessNodeController(
	private val service: AccessNodeService,
) {
	/**
	 * 分页列出权限目录。
	 */
	@GetMapping
	@Operation(
		summary = "查询访问节点列表",
		description = """
			分页查询系统内置权限目录。该接口主要用于角色授权界面和权限排查。

			权限数据由后端内置契约维护，不通过管理端临时创建；角色只保存权限 code 绑定关系。
			查询条件同时传入时，服务端按交集返回结果。
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
		@Parameter(description = "是否只返回启用权限。禁用权限不会进入新签发令牌或当前登录态权限集合。", example = "true")
		@RequestParam(required = false) enabled: Boolean?,
		@Parameter(description = "页码，从 0 开始。", example = "0")
		@RequestParam(defaultValue = "0") page: Int,
		@Parameter(description = "每页数量，最大 100。", example = "50")
		@RequestParam(defaultValue = "50") size: Int,
	): Page<AccessNodeResponse> =
		service.listAccessNodes(page, size, q, codePrefix, enabled)

	/**
	 * 查询单个访问节点 code。
	 */
	@GetMapping("/{accessNodeCode}")
	@Operation(
		summary = "查询访问节点详情",
		description = """
			按稳定 code 查询单个权限的名称和启用状态。

			该接口适合在角色授权页面点选权限后展示解释信息。
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
