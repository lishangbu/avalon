package io.github.lishangbu.avalon.web.result

/**
 * 错误结果码接口
 *
 * 定义错误码与错误信息的统一契约
 *
 * @author lishangbu
 * @since 2025/4/16
 */
interface ErrorResultCode {
    /** 获取状态码 */
    fun code(): Int

    /** 获取错误信息 */
    fun errorMessage(): String
}
