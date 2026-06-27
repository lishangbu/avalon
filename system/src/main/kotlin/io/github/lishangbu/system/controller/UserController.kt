package io.github.lishangbu.system.controller

import io.github.lishangbu.system.dto.CreateUserRequest
import io.github.lishangbu.system.dto.ResetUserPasswordRequest
import io.github.lishangbu.system.dto.UpdateUserRolesRequest
import io.github.lishangbu.system.dto.UserResponse
import io.github.lishangbu.system.service.UserService
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
 * RBAC 用户系统管理 API。
 */
@RestController
@ConditionalOnProperty(prefix = "backend.security", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@RequestMapping("/api/system/rbac/users")
@Tag(
	name = "RBAC 用户",
	description = "管理后台用户账号、账号状态、密码和角色绑定。用户通过角色获得访问节点权限，接口响应永不返回密码摘要。",
)
class UserController(
	private val service: UserService,
) {
	/**
	 * 列出用户及其角色绑定。
	 */
	@GetMapping
	@Operation(
		summary = "查询用户列表",
		description = """
			分页查询用户账号和角色绑定快照。可按关键字、角色 code、启用状态和锁定状态过滤。

			q 会匹配用户名和显示名；roleCode 用于反查拥有指定角色的用户。响应中的 roleCodes 是当前完整角色绑定集合。
		""",
		security = [SecurityRequirement(name = SYSTEM_API_BEARER_AUTH)],
	)
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "200", description = "用户列表读取成功。"),
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
	fun listUsers(
		@Parameter(description = "模糊搜索关键字，匹配 username 或 displayName。", example = "audit")
		@RequestParam(required = false) q: String?,
		@Parameter(description = "按角色 code 过滤，只返回绑定了该角色的用户。", example = "system-admin")
		@RequestParam(required = false) roleCode: String?,
		@Parameter(description = "按启用状态过滤。true 表示只返回启用账号，false 表示只返回禁用账号。", example = "true")
		@RequestParam(required = false) enabled: Boolean?,
		@Parameter(description = "按锁定状态过滤。true 表示账号未锁定，false 表示账号已锁定。", example = "true")
		@RequestParam(required = false) accountNonLocked: Boolean?,
		@Parameter(description = "页码，从 0 开始。", example = "0")
		@RequestParam(defaultValue = "0") page: Int,
		@Parameter(description = "每页数量，最大 100。", example = "50")
		@RequestParam(defaultValue = "50") size: Int,
	): Page<UserResponse> =
		service.listUsers(page, size, q, roleCode, enabled, accountNonLocked)

	/**
	 * 查询单个用户及其角色绑定。
	 */
	@GetMapping("/{userId}")
	@Operation(
		summary = "查询用户详情",
		description = "按用户主键查询账号状态和完整角色绑定。响应不会包含 passwordHash 或任何可用于登录的凭据。",
		security = [SecurityRequirement(name = SYSTEM_API_BEARER_AUTH)],
	)
	@ApiResponses(
		value = [
			ApiResponse(
				responseCode = "200",
				description = "用户详情读取成功。",
				content = [Content(schema = Schema(implementation = UserResponse::class))],
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
	fun getUser(
		@Parameter(description = "用户主键 ID。", example = "40001")
		@PathVariable userId: Long,
	): UserResponse =
		service.getUser(userId)

	/**
	 * 创建用户并写入初始角色绑定。
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(
		summary = "创建用户",
		description = """
			创建管理端用户账号并写入初始角色绑定。

			password 只在请求中出现，服务端会写入编码后的密码摘要；响应永不返回明文密码或密码摘要。
			roleCodes 必须全部指向已存在角色，请求成功后响应会返回创建后的完整用户快照。
		""",
		security = [SecurityRequirement(name = SYSTEM_API_BEARER_AUTH)],
	)
	@ApiResponses(
		value = [
			ApiResponse(
				responseCode = "201",
				description = "用户创建成功。",
				content = [Content(schema = Schema(implementation = UserResponse::class))],
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
	fun createUser(
		@OpenApiRequestBody(
			description = "用户创建请求。roleCodes 表示创建后用户拥有的完整角色集合。",
			required = true,
			content = [Content(schema = Schema(implementation = CreateUserRequest::class))],
		)
		@RequestBody request: CreateUserRequest,
	): UserResponse =
		service.createUser(request)

	/**
	 * 启用用户账号。
	 */
	@PostMapping("/{userId}/enable")
	@Operation(
		summary = "启用用户",
		description = "将用户账号标记为启用。启用后账号仍需要处于未锁定状态并通过密码校验，才能成功换取 token。",
		security = [SecurityRequirement(name = SYSTEM_API_BEARER_AUTH)],
	)
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "200", description = "用户已启用。", content = [Content(schema = Schema(implementation = UserResponse::class))]),
			ApiResponse(responseCode = "401", description = SYSTEM_API_UNAUTHORIZED_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "403", description = SYSTEM_API_FORBIDDEN_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "404", description = SYSTEM_API_NOT_FOUND_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
		],
	)
	fun enableUser(
		@Parameter(description = "用户主键 ID。", example = "40001")
		@PathVariable userId: Long,
	): UserResponse =
		service.enableUser(userId)

	/**
	 * 禁用用户账号。
	 */
	@PostMapping("/{userId}/disable")
	@Operation(
		summary = "禁用用户",
		description = "将用户账号标记为禁用。禁用账号无法通过密码授权换取新的 access token。",
		security = [SecurityRequirement(name = SYSTEM_API_BEARER_AUTH)],
	)
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "200", description = "用户已禁用。", content = [Content(schema = Schema(implementation = UserResponse::class))]),
			ApiResponse(responseCode = "401", description = SYSTEM_API_UNAUTHORIZED_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "403", description = SYSTEM_API_FORBIDDEN_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "404", description = SYSTEM_API_NOT_FOUND_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
		],
	)
	fun disableUser(
		@Parameter(description = "用户主键 ID。", example = "40001")
		@PathVariable userId: Long,
	): UserResponse =
		service.disableUser(userId)

	/**
	 * 锁定用户账号。
	 */
	@PostMapping("/{userId}/lock")
	@Operation(
		summary = "锁定用户",
		description = "将用户账号标记为锁定。锁定账号保留角色绑定，但无法通过密码授权换取新的 access token。",
		security = [SecurityRequirement(name = SYSTEM_API_BEARER_AUTH)],
	)
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "200", description = "用户已锁定。", content = [Content(schema = Schema(implementation = UserResponse::class))]),
			ApiResponse(responseCode = "401", description = SYSTEM_API_UNAUTHORIZED_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "403", description = SYSTEM_API_FORBIDDEN_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "404", description = SYSTEM_API_NOT_FOUND_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
		],
	)
	fun lockUser(
		@Parameter(description = "用户主键 ID。", example = "40001")
		@PathVariable userId: Long,
	): UserResponse =
		service.lockUser(userId)

	/**
	 * 解锁用户账号。
	 */
	@PostMapping("/{userId}/unlock")
	@Operation(
		summary = "解锁用户",
		description = "将用户账号标记为未锁定。解锁只恢复锁定状态，不会自动启用已禁用账号。",
		security = [SecurityRequirement(name = SYSTEM_API_BEARER_AUTH)],
	)
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "200", description = "用户已解锁。", content = [Content(schema = Schema(implementation = UserResponse::class))]),
			ApiResponse(responseCode = "401", description = SYSTEM_API_UNAUTHORIZED_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "403", description = SYSTEM_API_FORBIDDEN_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "404", description = SYSTEM_API_NOT_FOUND_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
		],
	)
	fun unlockUser(
		@Parameter(description = "用户主键 ID。", example = "40001")
		@PathVariable userId: Long,
	): UserResponse =
		service.unlockUser(userId)

	/**
	 * 重置用户密码。
	 */
	@PutMapping("/{userId}/password")
	@Operation(
		summary = "重置用户密码",
		description = "重置用户密码。请求中的 password 会被服务端编码保存，响应只返回用户状态和角色绑定，不返回任何密码材料。",
		security = [SecurityRequirement(name = SYSTEM_API_BEARER_AUTH)],
	)
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "200", description = "用户密码已重置。", content = [Content(schema = Schema(implementation = UserResponse::class))]),
			ApiResponse(responseCode = "400", description = SYSTEM_API_BAD_REQUEST_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "401", description = SYSTEM_API_UNAUTHORIZED_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "403", description = SYSTEM_API_FORBIDDEN_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "404", description = SYSTEM_API_NOT_FOUND_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
		],
	)
	fun resetPassword(
		@Parameter(description = "用户主键 ID。", example = "40001")
		@PathVariable userId: Long,
		@OpenApiRequestBody(
			description = "新密码请求。必须满足后端密码强度和长度规则。",
			required = true,
			content = [Content(schema = Schema(implementation = ResetUserPasswordRequest::class))],
		)
		@RequestBody request: ResetUserPasswordRequest,
	): UserResponse =
		service.resetPassword(userId, request)

	/**
	 * 更新用户角色绑定。
	 */
	@PutMapping("/{userId}/roles")
	@Operation(
		summary = "更新用户角色",
		description = """
			整体替换用户角色绑定。

			roleCodes 表示更新后的完整角色集合，不是增量追加或删除列表。角色变更会影响后续签发 token 的权限快照。
		""",
		security = [SecurityRequirement(name = SYSTEM_API_BEARER_AUTH)],
	)
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "200", description = "用户角色已更新。", content = [Content(schema = Schema(implementation = UserResponse::class))]),
			ApiResponse(responseCode = "400", description = SYSTEM_API_BAD_REQUEST_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "401", description = SYSTEM_API_UNAUTHORIZED_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "403", description = SYSTEM_API_FORBIDDEN_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "404", description = SYSTEM_API_NOT_FOUND_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
		],
	)
	fun updateUserRoles(
		@Parameter(description = "用户主键 ID。", example = "40001")
		@PathVariable userId: Long,
		@OpenApiRequestBody(
			description = "用户角色绑定请求。roleCodes 会整体替换用户现有角色。",
			required = true,
			content = [Content(schema = Schema(implementation = UpdateUserRolesRequest::class))],
		)
		@RequestBody request: UpdateUserRolesRequest,
	): UserResponse =
		service.updateUserRoles(userId, request)
}
