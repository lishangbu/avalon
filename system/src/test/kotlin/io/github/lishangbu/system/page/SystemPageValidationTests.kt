package io.github.lishangbu.system.page

import io.github.lishangbu.system.error.SystemApiErrorCode
import io.github.lishangbu.system.error.SystemApiException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test

class SystemPageValidationTests {
	@Test
	fun `system page request accepts zero based page and bounded size`() {
		validateSystemPage(page = 0, size = 100)
	}

	@Test
	fun `system page request rejects negative page`() {
		val error = catchThrowable { validateSystemPage(page = -1, size = 50) }

		assertThat(error).isInstanceOf(SystemApiException::class.java)
		val exception = error as SystemApiException
		assertThat(exception.code).isEqualTo(SystemApiErrorCode.VALIDATION_INVALID)
		assertThat(exception.field).isEqualTo("page")
	}

	@Test
	fun `system page request rejects oversized page size`() {
		val error = catchThrowable { validateSystemPage(page = 0, size = 101) }

		assertThat(error).isInstanceOf(SystemApiException::class.java)
		val exception = error as SystemApiException
		assertThat(exception.code).isEqualTo(SystemApiErrorCode.VALIDATION_INVALID)
		assertThat(exception.field).isEqualTo("size")
	}

	@Test
	fun `system search filter normalizes blank query`() {
		val filter = systemSearchFilter("   ")

		assertThat(filter.pattern).isNull()
	}

	@Test
	fun `system search filter normalizes query pattern`() {
		val filter = systemSearchFilter(" Admin ")

		assertThat(filter.pattern).isEqualTo("%admin%")
	}

	@Test
	fun `system search filter rejects oversized query`() {
		val error = catchThrowable { systemSearchFilter("a".repeat(81)) }

		assertThat(error).isInstanceOf(SystemApiException::class.java)
		val exception = error as SystemApiException
		assertThat(exception.code).isEqualTo(SystemApiErrorCode.VALIDATION_INVALID)
		assertThat(exception.field).isEqualTo("q")
	}
}
