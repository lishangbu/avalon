package io.github.lishangbu.avalon.oauth2.common.properties

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.convert.DurationStyle
import java.time.Duration
import java.time.temporal.ChronoUnit

/**
 * OAuth2 配置属性
 *
 * 定义认证参数名、验证码策略、登录失败限制和 JWT 密钥位置等配置
 */
@AutoConfiguration
@ConfigurationProperties(prefix = "oauth2")
class Oauth2Properties {
    /** 忽略认证的 URL 列表 */
    var ignoreUrls: MutableList<String> = mutableListOf()

    /** 用户名参数名 */
    var usernameParameterName: String = "username"

    /** 密码参数名 */
    var passwordParameterName: String = "password"

    /** 手机号参数名 */
    var phoneParameterName: String = "phone"

    /** 短信验证码参数名 */
    var smsCodeParameterName: String = "sms_code"

    /** 邮箱参数名 */
    var emailParameterName: String = "email"

    /** 邮箱验证码参数名 */
    var emailCodeParameterName: String = "email_code"

    /** 短信验证码长度 */
    var smsCodeLength: Int? = 6

    /** 短信验证码有效期 */
    private var smsCodeTimeToLive: String? = "5m"

    /** 短信验证码重发间隔 */
    private var smsCodeResendInterval: String? = "60s"

    /** 邮箱验证码长度 */
    var emailCodeLength: Int? = 6

    /** 邮箱验证码有效期 */
    private var emailCodeTimeToLive: String? = "5m"

    /** 邮箱验证码重发间隔 */
    private var emailCodeResendInterval: String? = "60s"

    /** 最大登录失败次数 */
    var maxLoginFailures: Int? = 5

    /** 登录锁定时长 */
    private var loginLockDuration: String? = "15m"

    /** 登录失败跟踪存储类型 */
    var loginFailureTrackerStoreType: LoginFailureTrackerStoreType = LoginFailureTrackerStoreType.MEMORY

    /** 登录失败跟踪 Redis Key 前缀 */
    var loginFailureTrackerKeyPrefix: String = "oauth2:login-failure"

    /** 登录失败跟踪 JDBC 表名 */
    var loginFailureTrackerJdbcTableName: String = "oauth2_login_failure"

    /** 签发者 URL */
    var issuerUrl: String? = null

    /** JWT 公钥文件位置 */
    var jwtPublicKeyLocation: String? = null

    /** JWT 私钥文件位置 */
    var jwtPrivateKeyLocation: String? = null

    /** 获取登录锁定时长 */
    fun getLoginLockDuration(): Duration? {
        val value = loginLockDuration
        if (value.isNullOrBlank()) {
            return null
        }
        return try {
            DurationStyle.detectAndParse(value.trim(), ChronoUnit.MINUTES)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /** 设置登录锁定时长 */
    fun setLoginLockDuration(loginLockDuration: String?) {
        this.loginLockDuration = loginLockDuration
    }

    /** 获取短信验证码有效期 */
    fun getSmsCodeTimeToLiveDuration(): Duration = parseDuration(smsCodeTimeToLive, Duration.ofMinutes(5), ChronoUnit.MINUTES)

    /** 获取短信验证码重发间隔 */
    fun getSmsCodeResendIntervalDuration(): Duration = parseDuration(smsCodeResendInterval, Duration.ofSeconds(60), ChronoUnit.SECONDS)

    /** 获取邮箱验证码有效期 */
    fun getEmailCodeTimeToLiveDuration(): Duration = parseDuration(emailCodeTimeToLive, Duration.ofMinutes(5), ChronoUnit.MINUTES)

    /** 获取邮箱验证码重发间隔 */
    fun getEmailCodeResendIntervalDuration(): Duration = parseDuration(emailCodeResendInterval, Duration.ofSeconds(60), ChronoUnit.SECONDS)

    /** 解析时长配置 */
    private fun parseDuration(
        value: String?,
        fallback: Duration,
        defaultUnit: ChronoUnit,
    ): Duration {
        if (value.isNullOrBlank()) {
            return fallback
        }
        return try {
            DurationStyle.detectAndParse(value.trim(), defaultUnit)
        } catch (_: IllegalArgumentException) {
            fallback
        }
    }

    companion object {
        /** 配置前缀 */
        const val PREFIX = "oauth2"
    }
}
