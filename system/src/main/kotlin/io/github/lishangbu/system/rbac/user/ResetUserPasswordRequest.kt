package io.github.lishangbu.system.rbac.user

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 重置 RBAC 用户密码的系统管理请求。
 */
@Schema(description = "重置用户密码请求。密码只用于写入，响应不会回显。")
data class ResetUserPasswordRequest(
	@field:Schema(description = "新密码。必须满足后端密码长度和格式校验。", example = "secret456", writeOnly = true)
	var password: String = "",
)
