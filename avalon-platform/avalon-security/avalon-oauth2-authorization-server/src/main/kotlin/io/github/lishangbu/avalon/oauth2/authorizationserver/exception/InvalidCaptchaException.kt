package io.github.lishangbu.avalon.oauth2.authorizationserver.exception

import org.springframework.security.core.AuthenticationException

/**
 * 验证码异常 当验证码校验失败时抛出此异常
 *
 * @author lishangbu
 * @since 2025/8/21
 */
class InvalidCaptchaException(
    msg: String,
) : AuthenticationException(msg)
