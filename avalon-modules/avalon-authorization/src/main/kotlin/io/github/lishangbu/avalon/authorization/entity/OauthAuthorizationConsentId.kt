package io.github.lishangbu.avalon.authorization.entity

import org.babyfish.jimmer.sql.Embeddable

@Embeddable
interface OauthAuthorizationConsentId {
    /** 注册客户端 ID */
    val registeredClientId: String

    /** 主体名称 */
    val principalName: String
}
