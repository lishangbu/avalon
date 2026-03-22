package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.*
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
class Oauth2AuthorizationRepositoryImpl(
    private val sql: KSqlClient,
) : Oauth2AuthorizationRepository {
    override fun findById(id: String): Optional<OauthAuthorization> = Optional.ofNullable(sql.findById(OauthAuthorization::class, id))

    override fun save(authorization: OauthAuthorization): OauthAuthorization = sql.save(authorization).modifiedEntity

    override fun saveAndFlush(authorization: OauthAuthorization): OauthAuthorization = save(authorization)

    override fun deleteById(id: String) {
        sql
            .createDelete(OauthAuthorization::class) {
                where(table.id eq id)
                disableDissociation()
            }.execute()
    }

    override fun flush() = Unit

    override fun findByState(state: String): Optional<OauthAuthorization> =
        Optional.ofNullable(
            sql
                .createQuery(OauthAuthorization::class) {
                    where(table.state eq state)
                    select(table)
                }.execute()
                .firstOrNull(),
        )

    override fun findByAuthorizationCodeValue(authorizationCode: String): Optional<OauthAuthorization> =
        Optional.ofNullable(
            sql
                .createQuery(OauthAuthorization::class) {
                    where(table.authorizationCodeValue eq authorizationCode)
                    select(table)
                }.execute()
                .firstOrNull(),
        )

    override fun findByAccessTokenValue(accessToken: String): Optional<OauthAuthorization> =
        Optional.ofNullable(
            sql
                .createQuery(OauthAuthorization::class) {
                    where(table.accessTokenValue eq accessToken)
                    select(table)
                }.execute()
                .firstOrNull(),
        )

    override fun findByRefreshTokenValue(refreshToken: String): Optional<OauthAuthorization> =
        Optional.ofNullable(
            sql
                .createQuery(OauthAuthorization::class) {
                    where(table.refreshTokenValue eq refreshToken)
                    select(table)
                }.execute()
                .firstOrNull(),
        )

    override fun findByOidcIdTokenValue(idToken: String): Optional<OauthAuthorization> =
        Optional.ofNullable(
            sql
                .createQuery(OauthAuthorization::class) {
                    where(table.oidcIdTokenValue eq idToken)
                    select(table)
                }.execute()
                .firstOrNull(),
        )

    override fun findByUserCodeValue(userCode: String): Optional<OauthAuthorization> =
        Optional.ofNullable(
            sql
                .createQuery(OauthAuthorization::class) {
                    where(table.userCodeValue eq userCode)
                    select(table)
                }.execute()
                .firstOrNull(),
        )

    override fun findByDeviceCodeValue(deviceCode: String): Optional<OauthAuthorization> =
        Optional.ofNullable(
            sql
                .createQuery(OauthAuthorization::class) {
                    where(table.deviceCodeValue eq deviceCode)
                    select(table)
                }.execute()
                .firstOrNull(),
        )

    override fun findByStateOrAuthorizationCodeValueOrAccessTokenValueOrRefreshTokenValueOrOidcIdTokenValueOrUserCodeValueOrDeviceCodeValue(
        token: String,
    ): Optional<OauthAuthorization> =
        findByState(token)
            .or { findByAuthorizationCodeValue(token) }
            .or { findByAccessTokenValue(token) }
            .or { findByRefreshTokenValue(token) }
            .or { findByOidcIdTokenValue(token) }
            .or { findByUserCodeValue(token) }
            .or { findByDeviceCodeValue(token) }
}
