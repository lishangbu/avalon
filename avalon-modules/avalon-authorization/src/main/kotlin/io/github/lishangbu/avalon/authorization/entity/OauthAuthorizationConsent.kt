package io.github.lishangbu.avalon.authorization.entity

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.PropOverride
import org.babyfish.jimmer.sql.Table

@Entity
@Table(name = "oauth_authorization_consent")
interface OauthAuthorizationConsent {
    /** ID */
    @Id
    @PropOverride(prop = "registeredClientId", columnName = "registered_client_id")
    @PropOverride(prop = "principalName", columnName = "principal_name")
    val id: OauthAuthorizationConsentId

    /** 权限集合 */
    val authorities: String?
}
