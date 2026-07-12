package io.github.lishangbu.security.oauth

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import java.time.Duration
import java.time.Instant

/** 验证旋转 refresh token 的滑动期限不会越过 family 七天绝对边界。 */
class FamilyBoundRefreshTokenGeneratorTests {
	@Test
	fun `caps refresh token expiry at seven days after family creation`() {
		val familyStartedAt = Instant.parse("2026-07-01T00:00:00Z")
		val issuedAt = familyStartedAt.plus(Duration.ofDays(6)).plus(Duration.ofHours(20))
		val generated = OAuth2RefreshToken("token", issuedAt, issuedAt.plus(Duration.ofHours(8)))

		val bounded = boundRefreshToken(generated, familyStartedAt)

		assertThat(bounded.expiresAt).isEqualTo(Instant.parse("2026-07-08T00:00:00Z"))
		assertThat(bounded.tokenValue).isEqualTo("token")
	}
}
