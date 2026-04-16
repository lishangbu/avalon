package io.github.lishangbu.avalon.identity.access.infrastructure.authentication

import jakarta.enterprise.context.ApplicationScoped
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*

/**
 * opaque token 与会话标识生成服务。
 *
 * refresh token、session key 和通用随机标识统一走这里生成，
 * 以便安全参数、随机源和散列策略集中维护。
 */
@ApplicationScoped
class OpaqueTokenService {
    private val secureRandom = SecureRandom()
    private val encoder = Base64.getUrlEncoder().withoutPadding()

    /**
     * 生成 refresh token 明文。
     *
     * refresh token 明文只返回给客户端，不会直接持久化到数据库。
     *
     * @return 具备足够熵值的 opaque token。
     */
    fun generateOpaqueToken(): String = generateIdentifier(48)

    /**
     * 生成会话稳定键。
     *
     * @return 带 `sess_` 前缀的会话标识。
     */
    fun generateSessionKey(): String = "sess_${generateIdentifier(24)}"

    /**
     * 生成指定位数的 URL-safe 随机标识。
     *
     * @param byteLength 随机字节长度。
     * @return Base64 URL-safe 编码后的随机标识。
     */
    fun generateIdentifier(byteLength: Int): String {
        val bytes = ByteArray(byteLength)
        secureRandom.nextBytes(bytes)
        return encoder.encodeToString(bytes)
    }

    /**
     * 对 token 明文做不可逆散列。
     *
     * @param token 原始 token 明文。
     * @return 适合持久化存储的 SHA-256 十六进制文本。
     */
    fun hashToken(token: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}