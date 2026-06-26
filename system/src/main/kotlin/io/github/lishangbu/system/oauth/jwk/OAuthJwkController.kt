package io.github.lishangbu.system.oauth.jwk

import io.github.lishangbu.system.error.SystemApiErrorResponse
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
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * JWK 系统管理 API。
 */
@RestController
@ConditionalOnProperty(prefix = "backend.security", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@RequestMapping("/api/system/oauth/jwks")
@Tag(
	name = "OAuth JWK",
	description = "管理授权服务器用于 JWT 签名的 JWK 元数据。管理接口不返回私钥材料，只返回 keyId 和 active 状态。",
)
class OAuthJwkController(
	private val service: OAuthJwkService,
) {
	/**
	 * 列出 JWK 元数据。
	 */
	@GetMapping
	@Operation(
		summary = "查询 JWK 列表",
		description = """
			分页查询授权服务器 JWK 元数据。active=true 的 key 是当前用于签发 self-contained JWT 的签名 key。

			接口不会返回 jwkJson 或任何私钥字段；需要公开验证 key 时应使用授权服务器标准 JWKS 端点。
		""",
		security = [SecurityRequirement(name = SYSTEM_API_BEARER_AUTH)],
	)
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "200", description = "JWK 列表读取成功。"),
			ApiResponse(responseCode = "400", description = SYSTEM_API_BAD_REQUEST_DESCRIPTION, content = [Content(schema = Schema(implementation = SystemApiErrorResponse::class))]),
			ApiResponse(responseCode = "401", description = SYSTEM_API_UNAUTHORIZED_DESCRIPTION, content = [Content(schema = Schema(implementation = SystemApiErrorResponse::class))]),
			ApiResponse(responseCode = "403", description = SYSTEM_API_FORBIDDEN_DESCRIPTION, content = [Content(schema = Schema(implementation = SystemApiErrorResponse::class))]),
		],
	)
	fun listJwks(
		@Parameter(description = "模糊搜索关键字，匹配 keyId。", example = "system")
		@RequestParam(required = false) q: String?,
		@Parameter(description = "页码，从 0 开始。", example = "0")
		@RequestParam(defaultValue = "0") page: Int,
		@Parameter(description = "每页数量，最大 100。", example = "50")
		@RequestParam(defaultValue = "50") size: Int,
	): Page<OAuthJwkResponse> =
		service.listJwks(page, size, q)

	/**
	 * 查询单个 JWK 元数据。
	 */
	@GetMapping("/{keyId}")
	@Operation(
		summary = "查询 JWK 详情",
		description = "按 keyId 查询 JWK 元数据。响应只包含 keyId 和 active 状态，不包含私钥材料。",
		security = [SecurityRequirement(name = SYSTEM_API_BEARER_AUTH)],
	)
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "200", description = "JWK 详情读取成功。", content = [Content(schema = Schema(implementation = OAuthJwkResponse::class))]),
			ApiResponse(responseCode = "401", description = SYSTEM_API_UNAUTHORIZED_DESCRIPTION, content = [Content(schema = Schema(implementation = SystemApiErrorResponse::class))]),
			ApiResponse(responseCode = "403", description = SYSTEM_API_FORBIDDEN_DESCRIPTION, content = [Content(schema = Schema(implementation = SystemApiErrorResponse::class))]),
			ApiResponse(responseCode = "404", description = SYSTEM_API_NOT_FOUND_DESCRIPTION, content = [Content(schema = Schema(implementation = SystemApiErrorResponse::class))]),
		],
	)
	fun getJwk(
		@Parameter(description = "JWK keyId。", example = "system-jwt-key-20260625")
		@PathVariable keyId: String,
	): OAuthJwkResponse =
		service.getJwk(keyId)

	/**
	 * 轮换 JWK 并返回新的活跃 key 元数据。
	 */
	@PostMapping("/rotation")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(
		summary = "轮换 JWK",
		description = """
			生成新的 JWT 签名 key，并将其设置为唯一 active key。

			轮换后新签发的 self-contained JWT 使用新 key；历史 token 的验证窗口取决于授权服务器保留和发布旧公钥的策略。
			该接口返回新 active key 的元数据，不返回私钥材料。
		""",
		security = [SecurityRequirement(name = SYSTEM_API_BEARER_AUTH)],
	)
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "201", description = "JWK 已轮换，新 key 已激活。", content = [Content(schema = Schema(implementation = OAuthJwkResponse::class))]),
			ApiResponse(responseCode = "401", description = SYSTEM_API_UNAUTHORIZED_DESCRIPTION, content = [Content(schema = Schema(implementation = SystemApiErrorResponse::class))]),
			ApiResponse(responseCode = "403", description = SYSTEM_API_FORBIDDEN_DESCRIPTION, content = [Content(schema = Schema(implementation = SystemApiErrorResponse::class))]),
		],
	)
	fun rotateJwk(): OAuthJwkResponse =
		service.rotateJwk()
}
