package io.github.lishangbu.system.oauth.client

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 创建 OAuth client 的系统管理请求。
 *
 * 当前只开放密码 grant 管理客户端所需字段，其他 SAS settings 由服务层写入协议基线。
 */
@Schema(description = "创建 OAuth client 请求。clientSecret 只用于写入，不会在响应中返回。")
data class CreateOAuthClientRequest(
	@field:Schema(description = "OAuth clientId。必须唯一。", example = "system-tools-jwt")
	var clientId: String = "",
	@field:Schema(description = "OAuth client secret。写入后不能通过管理接口读回。", example = "{noop}tools-secret", writeOnly = true, nullable = true)
	var clientSecret: String? = null,
	@field:Schema(description = "客户端展示名称。", example = "系统工具 JWT Client")
	var clientName: String = "",
	@field:Schema(description = "允许该 client 请求的 scope 集合。当前 scope 应对应后端支持的权限 code。", example = "[\"security:admin\"]")
	var scopes: List<String> = emptyList(),
	@field:Schema(description = "access token 格式。self-contained 表示 JWT，reference 表示 opaque/reference token。", example = "self-contained")
	var accessTokenFormat: String = "",
)
