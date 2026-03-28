package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.*
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.stereotype.Repository

@Repository
class Oauth2AuthorizationRepositoryImpl(
    /** Jimmer SQL 客户端 */
    private val sql: KSqlClient,
) : Oauth2AuthorizationRepositoryExt {
    /** 按 ID 删除 OAuth2 授权 */
    override fun removeById(id: String) {
        sql
            .createDelete(OauthAuthorization::class) {
                where(table.id eq id)
                disableDissociation()
            }.execute()
    }

    /** 按状态查询 OAuth2 授权 */
    override fun findByState(state: String): OauthAuthorization? =
        sql
            .createQuery(OauthAuthorization::class) {
                where(table.state eq state)
                select(table)
            }.execute()
            .firstOrNull()

    /** 按授权码值查询 OAuth2 授权 */
    override fun findByAuthorizationCodeValue(authorizationCode: String): OauthAuthorization? =
        sql
            .createQuery(OauthAuthorization::class) {
                where(table.authorizationCodeValue eq authorizationCode)
                select(table)
            }.execute()
            .firstOrNull()

    /** 按访问令牌值查询 OAuth2 授权 */
    override fun findByAccessTokenValue(accessToken: String): OauthAuthorization? =
        sql
            .createQuery(OauthAuthorization::class) {
                where(table.accessTokenValue eq accessToken)
                select(table)
            }.execute()
            .firstOrNull()

    /** 按刷新令牌值查询 OAuth2 授权 */
    override fun findByRefreshTokenValue(refreshToken: String): OauthAuthorization? =
        sql
            .createQuery(OauthAuthorization::class) {
                where(table.refreshTokenValue eq refreshToken)
                select(table)
            }.execute()
            .firstOrNull()

    /** 按 OIDC ID 令牌值查询 OAuth2 授权 */
    override fun findByOidcIdTokenValue(idToken: String): OauthAuthorization? =
        sql
            .createQuery(OauthAuthorization::class) {
                where(table.oidcIdTokenValue eq idToken)
                select(table)
            }.execute()
            .firstOrNull()

    /** 按用户码值查询 OAuth2 授权 */
    override fun findByUserCodeValue(userCode: String): OauthAuthorization? =
        sql
            .createQuery(OauthAuthorization::class) {
                where(table.userCodeValue eq userCode)
                select(table)
            }.execute()
            .firstOrNull()

    /** 按设备码值查询 OAuth2 授权 */
    override fun findByDeviceCodeValue(deviceCode: String): OauthAuthorization? =
        sql
            .createQuery(OauthAuthorization::class) {
                where(table.deviceCodeValue eq deviceCode)
                select(table)
            }.execute()
            .firstOrNull()

    /** 按状态或各类令牌值查询 OAuth2 授权 */
    override fun findByStateOrAuthorizationCodeValueOrAccessTokenValueOrRefreshTokenValueOrOidcIdTokenValueOrUserCodeValueOrDeviceCodeValue(
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
