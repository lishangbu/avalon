package io.github.lishangbu.common.web

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 后台 API 的稳定错误响应。
 */
@Schema(
	name = "ApiErrorResponse",
	description = "后台 API 的稳定错误响应。前端应优先根据 code 和 field 做交互处理，message 用于展示给用户或排查问题。",
)
data class ApiErrorResponse(
	@field:Schema(description = "稳定错误码。用于前端分支处理和日志检索，不随展示文案变化。", example = "validation.invalid")
	val code: String,
	@field:Schema(description = "面向用户或管理员的中文错误说明。", example = "page 参数格式不正确")
	val message: String,
	@field:Schema(description = "出错字段名。字段缺失、格式错误或资源定位失败时返回；全局错误可为空。", example = "page", nullable = true)
	val field: String? = null,
)
