package io.github.lishangbu.security.entity

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table
import java.time.Instant

/** 已旋转 refresh token 的不可逆摘要，用于发现旧令牌重放并定位其 token family。 */
@Entity
@Table(name = "oauth_refresh_token_replay")
interface OAuthRefreshTokenReplay {
	@Id val tokenHash: String
	val authorizationId: String
	val recordedAt: Instant
}
