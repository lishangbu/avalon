package io.github.lishangbu.avalon.oauth2.authorizationserver.keygen

import org.springframework.security.crypto.keygen.StringKeyGenerator
import java.util.*

/**
 * UUID Key 生成器 使用 UUID 生成一个小写的字符串键
 *
 * @author lishangbu
 * @since 2025/8/22
 */
class UuidKeyGenerator : StringKeyGenerator {
    /** 生成密钥 */
    override fun generateKey(): String = UUID.randomUUID().toString().lowercase(Locale.ROOT)
}
