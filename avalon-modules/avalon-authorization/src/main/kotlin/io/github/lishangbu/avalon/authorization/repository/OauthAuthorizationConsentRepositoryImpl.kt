package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.*
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
class OauthAuthorizationConsentRepositoryImpl(
    private val sql: KSqlClient,
) : OauthAuthorizationConsentRepository {
    override fun findById(id: OauthAuthorizationConsentId): Optional<OauthAuthorizationConsent> = Optional.ofNullable(sql.findById(OauthAuthorizationConsent::class, id))

    override fun save(consent: OauthAuthorizationConsent): OauthAuthorizationConsent = sql.save(consent).modifiedEntity

    override fun saveAndFlush(consent: OauthAuthorizationConsent): OauthAuthorizationConsent = save(consent)

    override fun flush() = Unit

    override fun findByRegisteredClientIdAndPrincipalName(
        registeredClientId: String,
        principalName: String,
    ): Optional<OauthAuthorizationConsent> =
        findById(
            OauthAuthorizationConsentId {
                this.registeredClientId = registeredClientId
                this.principalName = principalName
            },
        )

    override fun deleteByRegisteredClientIdAndPrincipalName(
        registeredClientId: String,
        principalName: String,
    ) {
        sql
            .createDelete(OauthAuthorizationConsent::class) {
                where(table.id.registeredClientId eq registeredClientId)
                where(table.id.principalName eq principalName)
                disableDissociation()
            }.execute()
    }
}
