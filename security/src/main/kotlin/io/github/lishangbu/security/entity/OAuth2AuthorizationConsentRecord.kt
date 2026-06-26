package io.github.lishangbu.security.entity

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table

/**
 * Spring Authorization Server 用户授权同意表的 Jimmer 映射。
 */
@Entity
@Table(name = "oauth2_authorization_consent")
interface OAuth2AuthorizationConsentRecord {
	@Id
	val id: OAuth2AuthorizationConsentRecordId

	val authorities: String
}
