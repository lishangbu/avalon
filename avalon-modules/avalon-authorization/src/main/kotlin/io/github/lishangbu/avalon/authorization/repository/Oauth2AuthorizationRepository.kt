package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.OauthAuthorization
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * OAuth2 授权仓储接口
 *
 * 定义OAuth2 授权数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2025/9/14
 */
interface Oauth2AuthorizationRepository : KRepository<OauthAuthorization, String> {
    /**
     * 根据 state 查询认证信息
     *
     * @param state 状态码
     * @return 匹配的认证信息
     */
    fun findByState(state: String): OauthAuthorization?

    /**
     * 根据授权码查询认证信息
     *
     * @param authorizationCode 授权码
     * @return 匹配的认证信息
     */
    fun findByAuthorizationCodeValue(authorizationCode: String): OauthAuthorization?

    /**
     * 根据访问令牌查询认证信息
     *
     * @param accessToken 访问令牌
     * @return 匹配的认证信息
     */
    fun findByAccessTokenValue(accessToken: String): OauthAuthorization?

    /**
     * 根据刷新令牌查询认证信息
     *
     * @param refreshToken 刷新令牌
     * @return 匹配的认证信息
     */
    fun findByRefreshTokenValue(refreshToken: String): OauthAuthorization?

    /**
     * 根据 OIDC ID Token 查询认证信息
     *
     * @param idToken OIDC ID Token
     * @return 匹配的认证信息
     */
    fun findByOidcIdTokenValue(idToken: String): OauthAuthorization?

    /**
     * 根据用户码查询认证信息
     *
     * @param userCode 用户码
     * @return 匹配的认证信息
     */
    fun findByUserCodeValue(userCode: String): OauthAuthorization?

    /**
     * 根据设备码查询认证信息
     *
     * @param deviceCode 设备码
     * @return 匹配的认证信息
     */
    fun findByDeviceCodeValue(deviceCode: String): OauthAuthorization?

    /**
     * 根据多种 token 字段联合查询认证信息，支持
     * state、authorizationCode、accessToken、refreshToken、idToken、userCode、deviceCode 任意一种 token。
     * 查询语句为多行文本块，便于维护和阅读。
     *
     * @param token token 值，可为上述任意一种 token
     * @return 匹配的认证信息
     */
    fun loadByTokenValue(
        token: String,
    ): OauthAuthorization? =
        findByState(token)
            ?: findByAuthorizationCodeValue(token)
            ?: findByAccessTokenValue(token)
            ?: findByRefreshTokenValue(token)
            ?: findByOidcIdTokenValue(token)
            ?: findByUserCodeValue(token)
            ?: findByDeviceCodeValue(token)
}
