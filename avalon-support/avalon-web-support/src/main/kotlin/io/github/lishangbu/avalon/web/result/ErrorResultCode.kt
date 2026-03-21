package io.github.lishangbu.avalon.web.result

/**
 * 错误结果码接口 定义错误码与错误信息的标准 contract，便于统一错误处理
 *
 * @return 返回错误的状态码（Integer 类型） 获取与错误状态码关联的错误信息
 * @return 返回错误的详细信息（String 类型）
 * @author lishangbu
 * @since 2025/4/16 获取错误的状态码
 */

/** 错误结果码接口。 */
interface ErrorResultCode {
    fun code(): Int

    fun errorMessage(): String
}
