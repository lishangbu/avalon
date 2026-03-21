package io.github.lishangbu.avalon.authorization.controller

import io.github.lishangbu.avalon.oauth2.common.userdetails.UserInfo
import org.springframework.security.core.Authentication
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 令牌接口
 *
 * 提供登出与当前用户令牌信息查询接口
 *
 * @author vains
 */
@RequestMapping("/token")
@RestController
class TokenController(
    private val oAuth2AuthorizationService: OAuth2AuthorizationService,
) {
    @DeleteMapping("/logout")
    fun logout() {
        val authentication: Authentication? = SecurityContextHolder.getContext().authentication
        if (authentication != null && authentication.credentials is OAuth2AccessToken) {
            val accessToken = authentication.credentials as OAuth2AccessToken
            val auth2Authorization: OAuth2Authorization? =
                oAuth2AuthorizationService.findByToken(
                    accessToken.tokenValue,
                    OAuth2TokenType.ACCESS_TOKEN,
                )
            if (auth2Authorization != null) {
                oAuth2AuthorizationService.remove(auth2Authorization)
                SecurityContextHolder.clearContext()
            }
        }
    }

    @GetMapping("/info")
    fun user(
        @AuthenticationPrincipal user: UserInfo,
    ): UserInfo = user
}
