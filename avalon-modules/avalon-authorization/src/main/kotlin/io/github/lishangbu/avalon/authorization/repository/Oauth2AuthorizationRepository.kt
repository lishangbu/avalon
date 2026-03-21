package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.OauthAuthorization
import java.util.*

/**
 * 用户认证信息表(oauth_authorization)表数据库访问层
 *
 * 提供基础的 CRUD 操作
 *
 * @author lishangbu
 * @since 2025/9/14
 */
interface Oauth2AuthorizationRepository {
    fun findById(id: String): Optional<OauthAuthorization>

    fun save(authorization: OauthAuthorization): OauthAuthorization

    fun saveAndFlush(authorization: OauthAuthorization): OauthAuthorization

    fun deleteById(id: String)

    fun flush()

    /**
     * 根据 state 查询认证信息
     *
     * @param state 状态码
     * @return 匹配的认证信息
     */
    fun findByState(state: String): Optional<OauthAuthorization>

    /**
     * 根据授权码查询认证信息
     *
     * @param authorizationCode 授权码
     * @return 匹配的认证信息
     */
    fun findByAuthorizationCodeValue(authorizationCode: String): Optional<OauthAuthorization>

    /**
     * 根据访问令牌查询认证信息
     *
     * @param accessToken 访问令牌
     * @return 匹配的认证信息
     */
    fun findByAccessTokenValue(accessToken: String): Optional<OauthAuthorization>

    /**
     * 根据刷新令牌查询认证信息
     *
     * @param refreshToken 刷新令牌
     * @return 匹配的认证信息
     */
    fun findByRefreshTokenValue(refreshToken: String): Optional<OauthAuthorization>

    /**
     * 根据 OIDC ID Token 查询认证信息
     *
     * @param idToken OIDC ID Token
     * @return 匹配的认证信息
     */
    fun findByOidcIdTokenValue(idToken: String): Optional<OauthAuthorization>

    /**
     * 根据用户码查询认证信息
     *
     * @param userCode 用户码
     * @return 匹配的认证信息
     */
    fun findByUserCodeValue(userCode: String): Optional<OauthAuthorization>

    /**
     * 根据设备码查询认证信息
     *
     * @param deviceCode 设备码
     * @return 匹配的认证信息
     */
    fun findByDeviceCodeValue(deviceCode: String): Optional<OauthAuthorization>

    /**
     * 根据多种 token 字段联合查询认证信息，支持
     * state、authorizationCode、accessToken、refreshToken、idToken、userCode、deviceCode 任意一种 token。
     * 查询语句为多行文本块，便于维护和阅读。
     *
     * @param token token 值，可为上述任意一种 token
     * @return 匹配的认证信息
     */
    fun findByStateOrAuthorizationCodeValueOrAccessTokenValueOrRefreshTokenValueOrOidcIdTokenValueOrUserCodeValueOrDeviceCodeValue(
        token: String,
    ): Optional<OauthAuthorization>
}
