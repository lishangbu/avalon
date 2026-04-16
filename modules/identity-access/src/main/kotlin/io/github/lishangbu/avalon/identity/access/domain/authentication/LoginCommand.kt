package io.github.lishangbu.avalon.identity.access.domain.authentication

/**
 * 登录命令。
 *
 * @property identityType 登录标识类型。
 * @property principal 已归一化的登录 principal。
 * @property password 明文密码。
 * @property clientType 客户端类型。
 * @property deviceName 设备名称，可为空。
 * @property deviceFingerprint 设备指纹，可为空。
 * @property userAgent 原始 User-Agent，可为空。
 * @property ip 客户端 IP，可为空。
 */
data class LoginCommand(
    val identityType: IdentityType,
    val principal: String,
    val password: String,
    val clientType: ClientType,
    val deviceName: String?,
    val deviceFingerprint: String?,
    val userAgent: String?,
    val ip: String?,
)