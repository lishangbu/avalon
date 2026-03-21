package io.github.lishangbu.avalon.authorization.entity

import org.babyfish.jimmer.sql.Embeddable

@Embeddable
interface OauthAuthorizationConsentId {
    val registeredClientId: String

    val principalName: String
}
