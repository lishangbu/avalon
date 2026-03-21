package io.github.lishangbu.avalon.oauth2.common.properties

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.convert.DurationStyle
import java.time.Duration
import java.time.temporal.ChronoUnit

/**
 * OAuth2 安全配置属性类 封装 OAuth2 授权服务器的核心配置参数，支持通过 application.yml 或 application.properties 进行配置 前缀为
 * `oauth2`，包含认证路径控制、JWT 密钥管理、Token 签发等关键设置
 *
 * @author lishangbu
 * @since 2025/8/17 配置属性前缀，用于绑定 application.yml 中的 oauth2 配置段 免认证路径列表，匹配的请求路径将跳过 OAuth2 安全验证，支持 Ant
 *   模式 用户名参数名称，用于密码授权模式中的用户名字段 密码参数名称，用于密码授权模式中的密码字段 手机号参数名称，用于短信授权模式中的手机号字段
 *   短信验证码参数名称，用于短信授权模式中的验证码字段 邮箱参数名称，用于邮箱授权模式中的邮箱字段 邮箱验证码参数名称，用于邮箱授权模式中的验证码字段 短信验证码长度 短信验证码有效期
 *   短信验证码发送频率限制 邮箱验证码长度 邮箱验证码有效期 邮箱验证码发送频率限制 登录失败允许的最大尝试次数，<=0 则禁用 登录失败后的封禁时长，<=0 则禁用 JWT Token
 *   签发者地址，用于标识 Token 的颁发机构 JWT 公钥文件位置，用于 Token 签名验证 JWT 私钥文件位置，用于 Token 签名生成
 */
@AutoConfiguration
@ConfigurationProperties(prefix = "oauth2")
class Oauth2Properties {
    var ignoreUrls: MutableList<String> = mutableListOf()
    var usernameParameterName: String = "username"
    var passwordParameterName: String = "password"
    var phoneParameterName: String = "phone"
    var smsCodeParameterName: String = "sms_code"
    var emailParameterName: String = "email"
    var emailCodeParameterName: String = "email_code"
    var smsCodeLength: Int? = 6
    private var smsCodeTimeToLive: String? = "5m"
    private var smsCodeResendInterval: String? = "60s"
    var emailCodeLength: Int? = 6
    private var emailCodeTimeToLive: String? = "5m"
    private var emailCodeResendInterval: String? = "60s"
    var maxLoginFailures: Int? = 5
    private var loginLockDuration: String? = "15m"
    var issuerUrl: String? = null
    var jwtPublicKeyLocation: String? = null
    var jwtPrivateKeyLocation: String? = null

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

    fun setLoginLockDuration(loginLockDuration: String?) {
        this.loginLockDuration = loginLockDuration
    }

    fun getSmsCodeTimeToLiveDuration(): Duration = parseDuration(smsCodeTimeToLive, Duration.ofMinutes(5), ChronoUnit.MINUTES)

    fun getSmsCodeResendIntervalDuration(): Duration = parseDuration(smsCodeResendInterval, Duration.ofSeconds(60), ChronoUnit.SECONDS)

    fun getEmailCodeTimeToLiveDuration(): Duration = parseDuration(emailCodeTimeToLive, Duration.ofMinutes(5), ChronoUnit.MINUTES)

    fun getEmailCodeResendIntervalDuration(): Duration = parseDuration(emailCodeResendInterval, Duration.ofSeconds(60), ChronoUnit.SECONDS)

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
        const val PREFIX = "oauth2"
    }
}
