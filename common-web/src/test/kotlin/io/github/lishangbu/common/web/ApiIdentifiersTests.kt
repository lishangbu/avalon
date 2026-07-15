package io.github.lishangbu.common.web

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus

class ApiIdentifiersTests {
	@Test
	fun `malformed path identifiers are not found while malformed cursors are bad requests`() {
		assertThat(assertThrows<ApiException> { "bad".pathIdentifier("matchId") }.status)
			.isEqualTo(HttpStatus.NOT_FOUND)
		assertThat(assertThrows<ApiException> { "bad".queryCursorIdentifier("beforeMatchId") }.status)
			.isEqualTo(HttpStatus.BAD_REQUEST)
		assertThat(null.queryCursorIdentifier("beforeMatchId")).isEqualTo(Long.MAX_VALUE)
	}
}
