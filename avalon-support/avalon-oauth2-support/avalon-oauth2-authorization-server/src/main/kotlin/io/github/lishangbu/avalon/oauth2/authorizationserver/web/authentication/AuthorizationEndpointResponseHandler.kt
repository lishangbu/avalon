package io.github.lishangbu.avalon.oauth2.authorizationserver.web.authentication

import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServletServerHttpResponse
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.*
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationContext
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import java.io.IOException
import java.time.temporal.ChronoUnit
import java.util.function.Consumer

/**
 * 授权端点成功响应处理器
 *
 * 将访问令牌认证结果写回标准 OAuth2 访问令牌响应
 */
class AuthorizationEndpointResponseHandler : AuthenticationSuccessHandler {
    /** 日志记录器 */
    private val logger: Log = LogFactory.getLog(javaClass)

    /** 访问令牌响应转换器 */
    private val accessTokenResponseConverter: HttpMessageConverter<OAuth2AccessTokenResponse> =
        OAuth2AccessTokenResponseHttpMessageConverter()

    /** 访问令牌响应定制器 */
    private var accessTokenResponseCustomizer: Consumer<OAuth2AccessTokenAuthenticationContext>? =
        null

    /** 处理认证成功 */
    @Throws(IOException::class, ServletException::class)
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        if (authentication !is OAuth2AccessTokenAuthenticationToken) {
            if (logger.isErrorEnabled) {
                logger.error(
                    Authentication::class.java.simpleName +
                        " must be of type " +
                        OAuth2AccessTokenAuthenticationToken::class.java.name +
                        " but was " +
                        authentication.javaClass.name,
                )
            }
            val error =
                OAuth2Error(
                    OAuth2ErrorCodes.SERVER_ERROR,
                    "Unable to process the access token response.",
                    null,
                )
            throw OAuth2AuthenticationException(error)
        }

        val accessToken: OAuth2AccessToken = authentication.accessToken
        val refreshToken: OAuth2RefreshToken? = authentication.refreshToken
        val additionalParameters = authentication.additionalParameters

        val builder =
            OAuth2AccessTokenResponse
                .withToken(accessToken.tokenValue)
                .tokenType(accessToken.tokenType)
                .scopes(accessToken.scopes)
        if (accessToken.issuedAt != null && accessToken.expiresAt != null) {
            builder.expiresIn(
                ChronoUnit.SECONDS.between(accessToken.issuedAt, accessToken.expiresAt),
            )
        }
        if (refreshToken != null) {
            builder.refreshToken(refreshToken.tokenValue)
        }
        if (!additionalParameters.isNullOrEmpty()) {
            builder.additionalParameters(additionalParameters)
        }

        accessTokenResponseCustomizer?.let { customizer ->
            // @formatter:off
            val accessTokenAuthenticationContext =
                OAuth2AccessTokenAuthenticationContext
                    .with(authentication)
                    .accessTokenResponse(builder)
                    .build()
            // @formatter:on
            customizer.accept(accessTokenAuthenticationContext)
            if (logger.isTraceEnabled) {
                logger.trace("Customized access token response")
            }
        }

        val accessTokenResponse = builder.build()
        val httpResponse = ServletServerHttpResponse(response)
        accessTokenResponseConverter.write(accessTokenResponse, null, httpResponse)
    }

    /** 设置访问令牌响应定制器 */
    fun setAccessTokenResponseCustomizer(
        accessTokenResponseCustomizer: Consumer<OAuth2AccessTokenAuthenticationContext>,
    ) {
        this.accessTokenResponseCustomizer = accessTokenResponseCustomizer
    }
}
