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
 * An implementation of an [AuthenticationSuccessHandler] used for handling an
 * [OAuth2AccessTokenAuthenticationToken] and returning the [OAuth2AccessTokenResponse]
 *
 * @see AuthenticationSuccessHandler
 * @see OAuth2AccessTokenResponseHttpMessageConverter
 * @author Dmitriy Dubson
 * @author lishangbu
 */
class AuthorizationEndpointResponseHandler : AuthenticationSuccessHandler {
    private val logger: Log = LogFactory.getLog(javaClass)

    private val accessTokenResponseConverter: HttpMessageConverter<OAuth2AccessTokenResponse> =
        OAuth2AccessTokenResponseHttpMessageConverter()

    private var accessTokenResponseCustomizer: Consumer<OAuth2AccessTokenAuthenticationContext>? =
        null

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

    /**
     * Sets the [Consumer] providing access to the[OAuth2AccessTokenAuthenticationContext]
     * containing an [OAuth2AccessTokenResponse.Builder] and additional context information.
     *
     * @param accessTokenResponseCustomizer the [Consumer] providing access to the
     */
    // [OAuth2AccessTokenAuthenticationContext] containing

    /**  */
    //                   an [OAuth2AccessTokenResponse.Builder]
    fun setAccessTokenResponseCustomizer(
        accessTokenResponseCustomizer: Consumer<OAuth2AccessTokenAuthenticationContext>,
    ) {
        this.accessTokenResponseCustomizer = accessTokenResponseCustomizer
    }
}
