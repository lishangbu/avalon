package io.github.lishangbu.system.controller

import io.github.lishangbu.common.web.security.RequireSecurityAdmin

import io.github.lishangbu.system.dto.CreateOAuthClientRequest
import io.github.lishangbu.system.dto.OAuthClientResponse
import io.github.lishangbu.system.dto.ResetOAuthClientSecretRequest
import io.github.lishangbu.system.dto.UpdateOAuthClientRequest
import io.github.lishangbu.system.service.OAuthClientService
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
 * OAuth client 系统管理 API。
 */
@RequireSecurityAdmin
@RestController
@ConditionalOnProperty(prefix = "backend.security", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@RequestMapping("/api/system/oauth/clients")
@Tag(
	name = "OAuth Client",
	description = "管理 Spring Authorization Server 注册客户端。接口只暴露客户端元数据，secret 仅允许创建或重置时写入，不会在响应中回显。",
)
class OAuthClientController(
	private val service: OAuthClientService,
) {
	/**
	 * 列出 OAuth client 元数据。
	 */
	@GetMapping
	@Operation(
		summary = "查询 OAuth client 列表",
		description = """
			分页查询 OAuth client 元数据。可按 clientId 或 clientName 模糊搜索。

			响应不包含 clientSecret；accessTokenFormat 表示 access token 采用 self-contained JWT 还是 reference token。
		""",
		security = [SecurityRequirement(name = SYSTEM_API_BEARER_AUTH)],
	)
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "200", description = "OAuth client 列表读取成功。"),
			ApiResponse(responseCode = "400", description = SYSTEM_API_BAD_REQUEST_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "401", description = SYSTEM_API_UNAUTHORIZED_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "403", description = SYSTEM_API_FORBIDDEN_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
		],
	)
	fun listClients(
		@Parameter(description = "模糊搜索关键字，匹配 clientId 或 clientName。", example = "system-admin")
		@RequestParam(required = false) q: String?,
		@Parameter(description = "页码，从 0 开始。", example = "0")
		@RequestParam(defaultValue = "0") page: Int,
		@Parameter(description = "每页数量，最大 100。", example = "50")
		@RequestParam(defaultValue = "50") size: Int,
	): Page<OAuthClientResponse> =
		service.listClients(page, size, q)

	/**
	 * 查询单个 OAuth client 元数据。
	 */
	@GetMapping("/{clientId}")
	@Operation(
		summary = "查询 OAuth client 详情",
		description = "按 clientId 查询注册客户端元数据。响应不会包含 clientSecret。",
		security = [SecurityRequirement(name = SYSTEM_API_BEARER_AUTH)],
	)
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "200", description = "OAuth client 详情读取成功。", content = [Content(schema = Schema(implementation = OAuthClientResponse::class))]),
			ApiResponse(responseCode = "401", description = SYSTEM_API_UNAUTHORIZED_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "403", description = SYSTEM_API_FORBIDDEN_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "404", description = SYSTEM_API_NOT_FOUND_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
		],
	)
	fun getClient(
		@Parameter(description = "OAuth clientId。", example = "system-admin-jwt")
		@PathVariable clientId: String,
	): OAuthClientResponse =
		service.getClient(clientId)

	/**
	 * 创建 OAuth client。
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(
		summary = "创建 OAuth client",
		description = """
			创建 OAuth client，并写入密码授权所需的 client secret 与 token 策略。

			clientSecret 只在请求中出现；如果传入明文 secret，服务端会按安全配置编码或校验格式。scopes 必须是后端支持的权限 code。
		""",
		security = [SecurityRequirement(name = SYSTEM_API_BEARER_AUTH)],
	)
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "201", description = "OAuth client 创建成功。", content = [Content(schema = Schema(implementation = OAuthClientResponse::class))]),
			ApiResponse(responseCode = "400", description = SYSTEM_API_BAD_REQUEST_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "401", description = SYSTEM_API_UNAUTHORIZED_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "403", description = SYSTEM_API_FORBIDDEN_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "409", description = SYSTEM_API_CONFLICT_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
		],
	)
	fun createClient(
		@OpenApiRequestBody(
			description = "OAuth client 创建请求。clientSecret 写入后不可读取，只能后续重置。",
			required = true,
			content = [Content(schema = Schema(implementation = CreateOAuthClientRequest::class))],
		)
		@RequestBody request: CreateOAuthClientRequest,
	): OAuthClientResponse =
		service.createClient(request)

	/**
	 * 更新 OAuth client 可管理字段。
	 */
	@PutMapping("/{clientId}")
	@Operation(
		summary = "更新 OAuth client",
		description = """
			更新 OAuth client 的可管理字段，包括名称、scope、access token 格式和 token TTL。

			该接口不会修改 clientId、认证方式或 clientSecret；如需变更 secret，请调用专用重置接口。
		""",
		security = [SecurityRequirement(name = SYSTEM_API_BEARER_AUTH)],
	)
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "200", description = "OAuth client 更新成功。", content = [Content(schema = Schema(implementation = OAuthClientResponse::class))]),
			ApiResponse(responseCode = "400", description = SYSTEM_API_BAD_REQUEST_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "401", description = SYSTEM_API_UNAUTHORIZED_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "403", description = SYSTEM_API_FORBIDDEN_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "404", description = SYSTEM_API_NOT_FOUND_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
		],
	)
	fun updateClient(
		@Parameter(description = "OAuth clientId。", example = "system-admin-jwt")
		@PathVariable clientId: String,
		@OpenApiRequestBody(
			description = "OAuth client 更新请求。scopes 会整体替换现有 scope 集合。",
			required = true,
			content = [Content(schema = Schema(implementation = UpdateOAuthClientRequest::class))],
		)
		@RequestBody request: UpdateOAuthClientRequest,
	): OAuthClientResponse =
		service.updateClient(clientId, request)

	/**
	 * 重置 OAuth client secret。
	 */
	@PutMapping("/{clientId}/secret")
	@Operation(
		summary = "重置 OAuth client secret",
		description = """
			重置指定 OAuth client 的 secret。新 secret 生效后，旧 secret 不能再用于 token 端点客户端认证。

			响应只返回 client 元数据，不会回显新的 secret；调用方需要在提交前自行保存 secret。
		""",
		security = [SecurityRequirement(name = SYSTEM_API_BEARER_AUTH)],
	)
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "200", description = "OAuth client secret 已重置。", content = [Content(schema = Schema(implementation = OAuthClientResponse::class))]),
			ApiResponse(responseCode = "400", description = SYSTEM_API_BAD_REQUEST_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "401", description = SYSTEM_API_UNAUTHORIZED_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "403", description = SYSTEM_API_FORBIDDEN_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
			ApiResponse(responseCode = "404", description = SYSTEM_API_NOT_FOUND_DESCRIPTION, content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
		],
	)
	fun resetClientSecret(
		@Parameter(description = "OAuth clientId。", example = "system-admin-jwt")
		@PathVariable clientId: String,
		@OpenApiRequestBody(
			description = "新 client secret 请求。提交后无法通过任何管理接口读回。",
			required = true,
			content = [Content(schema = Schema(implementation = ResetOAuthClientSecretRequest::class))],
		)
		@RequestBody request: ResetOAuthClientSecretRequest,
	): OAuthClientResponse =
		service.resetClientSecret(clientId, request)
}
