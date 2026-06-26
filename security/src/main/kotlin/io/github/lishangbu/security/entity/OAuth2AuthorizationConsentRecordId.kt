package io.github.lishangbu.security.entity

import org.babyfish.jimmer.sql.Column
import org.babyfish.jimmer.sql.Embeddable

/**
 * 授权同意记录的复合主键。
 */
@Embeddable
interface OAuth2AuthorizationConsentRecordId {
	@Column(name = "registered_client_id")
	val registeredClientId: String

	@Column(name = "principal_name")
	val principalName: String
}
