package io.github.lishangbu.system.controller

import io.github.lishangbu.system.dto.CreateRoleRequest
import io.github.lishangbu.system.dto.RoleResponse
import io.github.lishangbu.system.dto.UpdateRoleRequest
import io.github.lishangbu.system.service.RoleService
import io.github.lishangbu.common.web.ApiErrorResponse
import io.github.lishangbu.system.openapi.SYSTEM_API_BAD_REQUEST_DESCRIPTION
import io.github.lishangbu.system.openapi.SYSTEM_API_BEARER_AUTH
import io.github.lishangbu.system.openapi.SYSTEM_API_CONFLICT_DESCRIPTION
import io.github.lishangbu.system.openapi.SYSTEM_API_FORBIDDEN_DESCRIPTION
import io.github.lishangbu.system.openapi.SYSTEM_API_NOT_FOUND_DESCRIPTION
import io.github.lishangbu.system.openapi.SYSTEM_API_UNAUTHORIZED_DESCRIPTION
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody as OpenApiRequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.babyfish.jimmer.Page
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * RBAC 角色系统管理 API。
 */
@RestController
@ConditionalOnProperty(prefix = "backend.security", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@RequestMapping("/api/system/rbac/roles")
@Tag(
	name = "RBAC 角色",
	description = "管理角色和访问节点绑定。角色是用户获得权限的中间层，角色 code 是前端展示、用户绑定和权限排查的稳定标识。",
)
class RoleController(
	private val service: RoleService,
) {
	/**
	 * 列出角色及其访问节点绑定。
	 */
	@GetMapping
	@Operation(
		summary = "查询角色列表",
		description = """
			分页查询角色及其访问节点绑定。可按关键字搜索角色 code 或名称，也可按访问节点 code 反查哪些角色持有该权限。

			返回结果中的 accessNodeCodes 是角色当前完整绑定快照，前端可直接用于授权表单回填。
		""",
		security = [SecurityRequirement(name = SYSTEM_API_BEARER_AUTH)],
	)
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "200", description = "角色列表读取成功。"),
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
	fun listRoles(
		@Parameter(description = "模糊搜索关键字，匹配角色 code 或名称。", example = "admin")
		@RequestParam(required = false) q: String?,
		@Parameter(description = "按访问节点 code 过滤，只返回绑定了该节点的角色。", example = "security:admin")
		@RequestParam(required = false) accessNodeCode: String?,
		@Parameter(description = "页码，从 0 开始。", example = "0")
		@RequestParam(defaultValue = "0") page: Int,
		@Parameter(description = "每页数量，最大 100。", example = "50")
		@RequestParam(defaultValue = "50") size: Int,
	): Page<RoleResponse> =
		service.listRoles(page, size, q, accessNodeCode)

	/**
	 * 查询单个角色及其访问节点绑定。
	 */
	@GetMapping("/{roleId}")
	@Operation(
		summary = "查询角色详情",
		description = "按角色主键查询角色详情和完整访问节点绑定。适用于编辑角色前的数据回填。",
		security = [SecurityRequirement(name = SYSTEM_API_BEARER_AUTH)],
	)
	@ApiResponses(
		value = [
			ApiResponse(
				responseCode = "200",
				description = "角色详情读取成功。",
				content = [Content(schema = Schema(implementation = RoleResponse::class))],
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
	fun getRole(
		@Parameter(description = "角色主键 ID。", example = "201")
		@PathVariable roleId: Long,
	): RoleResponse =
		service.getRole(roleId)

	/**
	 * 创建角色并绑定访问节点。
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(
		summary = "创建角色",
		description = """
			创建新角色并写入初始访问节点绑定。

			role code 创建后应视为稳定契约，不建议通过后续迁移随意改名；accessNodeCodes 必须全部指向已存在且启用的访问节点。
		""",
		security = [SecurityRequirement(name = SYSTEM_API_BEARER_AUTH)],
	)
	@ApiResponses(
		value = [
			ApiResponse(
				responseCode = "201",
				description = "角色创建成功。",
				content = [Content(schema = Schema(implementation = RoleResponse::class))],
			),
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
			ApiResponse(
				responseCode = "409",
				description = SYSTEM_API_CONFLICT_DESCRIPTION,
				content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
			),
		],
	)
	fun createRole(
		@OpenApiRequestBody(
			description = "角色创建请求。accessNodeCodes 表示创建后角色拥有的完整访问节点集合。",
			required = true,
			content = [Content(schema = Schema(implementation = CreateRoleRequest::class))],
		)
		@RequestBody request: CreateRoleRequest,
	): RoleResponse =
		service.createRole(request)

	/**
	 * 更新角色名称和访问节点绑定。
	 */
	@PutMapping("/{roleId}")
	@Operation(
		summary = "更新角色",
		description = """
			更新角色名称并整体替换访问节点绑定。

			该接口不会修改角色 code；请求体中的 accessNodeCodes 是更新后的完整集合，而不是增量追加或删除列表。
		""",
		security = [SecurityRequirement(name = SYSTEM_API_BEARER_AUTH)],
	)
	@ApiResponses(
		value = [
			ApiResponse(
				responseCode = "200",
				description = "角色更新成功。",
				content = [Content(schema = Schema(implementation = RoleResponse::class))],
			),
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
			ApiResponse(
				responseCode = "404",
				description = SYSTEM_API_NOT_FOUND_DESCRIPTION,
				content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
			),
		],
	)
	fun updateRole(
		@Parameter(description = "角色主键 ID。", example = "201")
		@PathVariable roleId: Long,
		@OpenApiRequestBody(
			description = "角色更新请求。accessNodeCodes 会整体替换现有绑定。",
			required = true,
			content = [Content(schema = Schema(implementation = UpdateRoleRequest::class))],
		)
		@RequestBody request: UpdateRoleRequest,
	): RoleResponse =
		service.updateRole(roleId, request)
}
