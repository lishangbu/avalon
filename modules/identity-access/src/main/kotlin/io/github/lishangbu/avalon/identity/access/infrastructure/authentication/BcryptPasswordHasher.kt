package io.github.lishangbu.avalon.identity.access.infrastructure.authentication

import io.quarkus.elytron.security.common.BcryptUtil
import jakarta.enterprise.context.ApplicationScoped

/**
 * Bcrypt 密码散列器。
 *
 * 当前认证链路把密码散列与比对集中在这里，避免应用服务和仓储层直接依赖
 * Quarkus/Elytron 的静态工具方法。
 */
@ApplicationScoped
class BcryptPasswordHasher {
    /**
     * 生成密码散列。
     *
     * @param password 明文密码。
     * @return Bcrypt 散列文本。
     */
    fun hash(password: String): String = BcryptUtil.bcryptHash(password)

    /**
     * 校验明文密码与散列文本是否匹配。
     *
     * @param rawPassword 明文密码。
     * @param hashedPassword 已保存的 Bcrypt 散列。
     * @return 若匹配返回 `true`。
     */
    fun matches(
        rawPassword: String,
        hashedPassword: String,
    ): Boolean = BcryptUtil.matches(rawPassword, hashedPassword)
}