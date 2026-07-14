package io.github.lishangbu.system.controller

import io.github.lishangbu.common.web.security.RequireSecurityAdmin

import io.github.lishangbu.system.dto.OAuthTokenResponse
import io.github.lishangbu.system.service.OAuthTokenService
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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * OAuth 令牌系统管理 API。
 */
@RequireSecurityAdmin
@RestController
@ConditionalOnProperty(prefix = "backend.security", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@RequestMapping("/api/system/oauth/tokens")
@Tag(
	name = "OAuth Token",
	description = "管理授权服务器已签发的授权记录和令牌生命周期。接口不返回 token 明文，只允许查看元数据和撤销令牌。",
)
class OAuthTokenController(
	private val service: OAuthTokenService,
) {
	/**
	 * 列出授权记录和令牌状态。
	 */
	@GetMapping
	@Operation(
		summary = "查询 OAuth 令牌列表",
		description = """
			分页查询授权服务器已签发的授权记录和令牌元数据。可按用户名、授权 ID、scope 或授权类型模糊搜索，也可按 clientId 和用户名精确过滤。

			响应不会包含 access token、refresh token 或 authorization code 明文。
		""",
		security = [SecurityRequirement(name = SYSTEM_API_BEARER_AUTH)],
	)
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "200", description = "OAuth 令牌列表读取成功。"),
			ApiResponse(responseCode = "400", description = SYSTEM_API_BAD_REQUEST_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "401", description = SYSTEM_API_UNAUTHORIZED_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "403", description = SYSTEM_API_FORBIDDEN_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
		],
	)
	fun listTokens(
		@Parameter(description = "模糊搜索关键字，匹配授权 ID、用户名、授权类型或 scope。", example = "admin")
		@RequestParam(required = false) q: String?,
		@Parameter(description = "按 OAuth clientId 精确过滤。", example = "system-admin-opaque")
		@RequestParam(required = false) clientId: String?,
		@Parameter(description = "按授权主体用户名精确过滤。", example = "admin")
		@RequestParam(required = false) principalName: String?,
		@Parameter(description = "页码，从 0 开始。", example = "0")
		@RequestParam(defaultValue = "0") page: Int,
		@Parameter(description = "每页数量，最大 100。", example = "50")
		@RequestParam(defaultValue = "50") size: Int,
	): Page<OAuthTokenResponse> =
		service.listTokens(page, size, q, clientId, principalName)

	/**
	 * 查询授权记录和令牌状态详情。
	 */
	@GetMapping("/{authorizationId}")
	@Operation(
		summary = "查询 OAuth 令牌详情",
		description = "按授权记录 ID 查询令牌元数据。响应不会包含任何 token 明文。",
		security = [SecurityRequirement(name = SYSTEM_API_BEARER_AUTH)],
	)
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "200", description = "OAuth 令牌详情读取成功。", content = [Content(schema = Schema(implementation = OAuthTokenResponse::class))]),
			ApiResponse(responseCode = "401", description = SYSTEM_API_UNAUTHORIZED_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "403", description = SYSTEM_API_FORBIDDEN_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "404", description = SYSTEM_API_NOT_FOUND_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
		],
	)
	fun getToken(
		@Parameter(description = "Spring Authorization Server 授权记录 ID。")
		@PathVariable authorizationId: String,
	): OAuthTokenResponse =
		service.getToken(authorizationId)

	/**
	 * 撤销授权记录中的 access token 和 refresh token。
	 */
	@PostMapping("/{authorizationId}/revoke")
	@Operation(
		summary = "撤销 OAuth 令牌",
		description = """
			撤销指定授权记录中的 access token 和 refresh token。

			对 reference/opaque access token，撤销后下一次资源访问会立即失效；self-contained JWT 的即时失效取决于资源服务器是否回查授权状态。
		""",
		security = [SecurityRequirement(name = SYSTEM_API_BEARER_AUTH)],
	)
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "200", description = "OAuth 令牌已撤销。", content = [Content(schema = Schema(implementation = OAuthTokenResponse::class))]),
			ApiResponse(responseCode = "401", description = SYSTEM_API_UNAUTHORIZED_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "403", description = SYSTEM_API_FORBIDDEN_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "404", description = SYSTEM_API_NOT_FOUND_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
		],
	)
	fun revokeToken(
		@Parameter(description = "Spring Authorization Server 授权记录 ID。")
		@PathVariable authorizationId: String,
	): OAuthTokenResponse =
		service.revokeToken(authorizationId)
}
