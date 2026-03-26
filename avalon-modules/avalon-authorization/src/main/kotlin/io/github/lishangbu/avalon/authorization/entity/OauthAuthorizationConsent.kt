package io.github.lishangbu.avalon.authorization.entity

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.PropOverride

@Entity
interface OauthAuthorizationConsent {
    /** ID */
    @Id
    @PropOverride(prop = "registeredClientId", columnName = "registered_client_id")
    @PropOverride(prop = "principalName", columnName = "principal_name")
    val id: OauthAuthorizationConsentId

    /** 权限集合 */
    val authorities: String?
}
