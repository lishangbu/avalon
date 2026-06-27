package io.github.lishangbu.common.web

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ApiValidationTests {
	@Test
	fun `required text rejects blank values`() {
		val exception = assertThrows<ApiException> {
			"  ".requiredText("name", maxLength = 20)
		}

		assertThat(exception.code).isEqualTo(ApiErrorCode.VALIDATION_REQUIRED)
		assertThat(exception.field).isEqualTo("name")
	}

	@Test
	fun `slug code rejects invalid characters`() {
		val exception = assertThrows<ApiException> {
			"Bad Code".requiredSlugCode("code")
		}

		assertThat(exception.code).isEqualTo(ApiErrorCode.VALIDATION_INVALID)
		assertThat(exception.field).isEqualTo("code")
	}

	@Test
	fun `access node codes reject invalid values`() {
		val exception = assertThrows<ApiException> {
			listOf("security:admin", "bad access").normalizedAccessNodeCodes("accessNodeCodes")
		}

		assertThat(exception.code).isEqualTo(ApiErrorCode.VALIDATION_INVALID)
		assertThat(exception.field).isEqualTo("accessNodeCodes")
	}

	@Test
	fun `supported values reject unsupported entry`() {
		val exception = assertThrows<ApiException> {
			listOf("security:admin", "system:read").requireSupportedValues("scopes", setOf("security:admin"))
		}

		assertThat(exception.code).isEqualTo(ApiErrorCode.VALIDATION_UNSUPPORTED)
		assertThat(exception.field).isEqualTo("scopes")
	}
}
