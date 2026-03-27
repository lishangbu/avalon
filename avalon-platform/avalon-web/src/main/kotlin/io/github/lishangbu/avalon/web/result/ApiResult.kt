package io.github.lishangbu.avalon.web.result

import java.io.Serializable

/**
 * 统一的 API 响应模型
 *
 * 封装接口响应的状态码、业务数据和错误信息
 *
 * @author lishangbu
 * @since 2025/4/16
 */
data class ApiResult<T>(
    /** 状态码 */
    val code: Int,
    /** 数据 */
    val data: T?,
    /** 错误信息 */
    val errorMessage: String?,
) : Serializable {
    /** 获取状态码 */
    fun code(): Int = code

    /** 获取数据 */
    fun data(): T? = data

    /** 获取错误信息 */
    fun errorMessage(): String? = errorMessage

    companion object {
        /** 成功状态码 */
        const val SUCCESS_CODE: Int = 200

        /** 序列化版本号 */
        private const val serialVersionUID: Long = 1L

        /** 构建成功响应 */
        @JvmStatic
        fun <T> ok(data: T?): ApiResult<T> = ApiResult(SUCCESS_CODE, data, null)

        /** 按状态码与消息构建失败响应 */
        @JvmStatic
        fun failed(
            code: Int,
            errorMessage: String,
        ): ApiResult<Void> = ApiResult(code, null, errorMessage)

        /** 按错误码枚举构建失败响应 */
        @JvmStatic
        fun failed(
            errorResultCode: ErrorResultCode,
            vararg errorMessages: String,
        ): ApiResult<Void> =
            if (errorMessages.isEmpty()) {
                failed(errorResultCode.code(), errorResultCode.errorMessage())
            } else {
                failed(errorResultCode.code(), errorMessages.joinToString(","))
            }
    }
}
