package io.github.lishangbu.avalon.oauth2.authorizationserver.introspection

import io.github.lishangbu.avalon.oauth2.common.userdetails.UserInfo
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector
import java.security.Principal

/**
 * 默认透明令牌处理器 用于资源服务器在收到不透明 access token 时执行 introspect，并根据授权与用户信息返回相应的 Principal
 *
 * @author lishangbu
 * @since 2025/8/22 OAuth2 授权服务 客户端模式默认返回 // 客户端模式默认返回
 */
class DefaultOpaqueTokenIntrospector(
    private val authorizationService: OAuth2AuthorizationService,
    private val userDetailsService: UserDetailsService,
) : OpaqueTokenIntrospector {
    override fun introspect(token: String): OAuth2AuthenticatedPrincipal? {
        val oldAuthorization: OAuth2Authorization? =
            authorizationService.findByToken(token, OAuth2TokenType.ACCESS_TOKEN)
        if (oldAuthorization == null) {
            throw InvalidBearerTokenException(token)
        }

        if (AuthorizationGrantType.CLIENT_CREDENTIALS == oldAuthorization.authorizationGrantType) {
            val accessTokenClaims =
                checkNotNull(oldAuthorization.accessToken?.claims) {
                    "Access token claims cannot be null for client credentials introspection"
                }
            return DefaultOAuth2AuthenticatedPrincipal(
                oldAuthorization.principalName,
                accessTokenClaims,
                AuthorityUtils.NO_AUTHORITIES,
            )
        }

        try {
            val principal =
                oldAuthorization.attributes[Principal::class.java.name]
                    as? UsernamePasswordAuthenticationToken
                    ?: return null
            val tokenPrincipal = principal.principal as? UserDetails ?: return null
            val userDetails = userDetailsService.loadUserByUsername(tokenPrincipal.username)
            if (userDetails is UserInfo) {
                userDetails.attributes.putAll(oldAuthorization.accessToken?.claims.orEmpty())
                return userDetails
            }
        } catch (notFoundException: UsernameNotFoundException) {
            log.warn("用户不不存在 {}", notFoundException.localizedMessage)
            throw notFoundException
        } catch (ex: Exception) {
            log.error("资源服务器 introspect Token error {}", ex.localizedMessage)
        }

        return null
    }

    companion object {
        private val log = LoggerFactory.getLogger(DefaultOpaqueTokenIntrospector::class.java)
    }
}
