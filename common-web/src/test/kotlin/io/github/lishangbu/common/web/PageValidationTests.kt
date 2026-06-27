package io.github.lishangbu.common.web

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PageValidationTests {
	@Test
	fun `page must not be negative`() {
		val exception = assertThrows<ApiException> {
			validatePage(page = -1, size = 20)
		}

		assertThat(exception.code).isEqualTo(ApiErrorCode.VALIDATION_INVALID)
		assertThat(exception.field).isEqualTo("page")
	}

	@Test
	fun `size must be within allowed range`() {
		val exception = assertThrows<ApiException> {
			validatePage(page = 0, size = 101)
		}

		assertThat(exception.code).isEqualTo(ApiErrorCode.VALIDATION_INVALID)
		assertThat(exception.field).isEqualTo("size")
	}

	@Test
	fun `search filter builds ilike pattern`() {
		assertThat(searchFilter(" Avalon ").pattern).isEqualTo("%avalon%")
	}
}
