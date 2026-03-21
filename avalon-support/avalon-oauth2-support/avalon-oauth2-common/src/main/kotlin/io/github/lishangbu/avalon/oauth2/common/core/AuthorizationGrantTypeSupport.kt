package io.github.lishangbu.avalon.oauth2.common.core

import org.springframework.security.oauth2.core.AuthorizationGrantType
import java.io.Serializable

/**
 * 一些过时但仍需支持的授权类型常量 提供对密码模式等旧授权类型的兼容支持
 *
 * @author lishangbu
 */
object AuthorizationGrantTypeSupport : Serializable {
    private const val serialVersionUID = 1L

    @JvmField
    val PASSWORD = AuthorizationGrantType("password")

    @JvmField
    val SMS = AuthorizationGrantType("sms")

    @JvmField
    val EMAIL = AuthorizationGrantType("email")
}
