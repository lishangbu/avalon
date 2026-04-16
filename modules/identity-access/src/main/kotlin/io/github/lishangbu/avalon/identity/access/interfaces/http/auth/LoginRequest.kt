package io.github.lishangbu.avalon.identity.access.interfaces.http.auth

import io.github.lishangbu.avalon.identity.access.domain.authentication.ClientType
import io.github.lishangbu.avalon.identity.access.domain.authentication.IdentityType
import io.github.lishangbu.avalon.identity.access.domain.authentication.LoginCommand
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/**
 * 登录请求。
 *
 * 该请求承载认证所需的核心凭据与客户端元数据。服务端会在映射为
 * [LoginCommand] 时统一裁剪空白字符，并把空字符串折叠为 `null`，
 * 以避免接口层和应用层重复做输入标准化。
 *
 * @property identityType 登录标识类型，用于决定 `principal` 该按用户名、邮箱还是手机号解释。
 * @property principal 原始登录标识文本。
 * @property password 明文密码，由后续认证链路完成校验与散列比对。
 * @property clientType 本次登录所属的客户端类型，用于会话策略和审计。
 * @property deviceName 客户端上报的设备名称，可为空。
 * @property deviceFingerprint 客户端上报的设备指纹，可为空。
 */
data class LoginRequest(
    @field:NotNull
    val identityType: IdentityType,
    @field:NotBlank
    @field:Size(max = 255)
    val principal: String,
    @field:NotBlank
    @field:Size(max = 128)
    val password: String,
    @field:NotNull
    val clientType: ClientType,
    @field:Size(max = 128)
    val deviceName: String? = null,
    @field:Size(max = 128)
    val deviceFingerprint: String? = null,
)

/**
 * 把登录请求转换为应用层命令。
 *
 * 映射时会统一裁剪请求体与链路元数据中的空白字符，并把空字符串折叠为 `null`，
 * 避免应用层继续处理表现层的输入噪音。
 *
 * @param userAgent 请求头中的 User-Agent。
 * @param forwardedFor 请求头中的 `X-Forwarded-For`，会自动提取代理链路中的首个客户端地址。
 * @return 可直接交给认证应用服务的登录命令。
 */
fun LoginRequest.toCommand(
    userAgent: String?,
    forwardedFor: String?,
): LoginCommand =
    LoginCommand(
        identityType = identityType,
        principal = principal.trim(),
        password = password,
        clientType = clientType,
        deviceName = deviceName.normalizedOrNull(),
        deviceFingerprint = deviceFingerprint.normalizedOrNull(),
        userAgent = userAgent.normalizedOrNull(),
        ip = forwardedFor.toForwardedClientIp(),
    )

private fun String?.normalizedOrNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

private fun String?.toForwardedClientIp(): String? = this?.substringBefore(",").normalizedOrNull()