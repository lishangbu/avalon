package io.github.lishangbu.system.error

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * 验证系统管理 API 字段校验会产生稳定错误 code 和字段名。
 */
class SystemApiValidationTests {
	@Test
	fun `required text rejects blank value`() {
		val exception = org.junit.jupiter.api.assertThrows<SystemApiException> {
			" ".requiredText("clientName", maxLength = 120)
		}

		assertThat(exception.code).isEqualTo(SystemApiErrorCode.VALIDATION_REQUIRED)
		assertThat(exception.field).isEqualTo("clientName")
		assertThat(exception.message).isEqualTo("clientName 不能为空")
	}

	@Test
	fun `slug code rejects uppercase value`() {
		val exception = org.junit.jupiter.api.assertThrows<SystemApiException> {
			"Admin".requiredSlugCode("code")
		}

		assertThat(exception.code).isEqualTo(SystemApiErrorCode.VALIDATION_INVALID)
		assertThat(exception.field).isEqualTo("code")
	}

	@Test
	fun `access node code list rejects invalid code`() {
		val exception = org.junit.jupiter.api.assertThrows<SystemApiException> {
			listOf("security:admin", "bad access").normalizedAccessNodeCodes("accessNodeCodes")
		}

		assertThat(exception.code).isEqualTo(SystemApiErrorCode.VALIDATION_INVALID)
		assertThat(exception.field).isEqualTo("accessNodeCodes")
	}

	@Test
	fun `supported values reject unsupported entry`() {
		val exception = org.junit.jupiter.api.assertThrows<SystemApiException> {
			listOf("security:admin", "system:read").requireSupportedValues("scopes", setOf("security:admin"))
		}

		assertThat(exception.code).isEqualTo(SystemApiErrorCode.VALIDATION_UNSUPPORTED)
		assertThat(exception.field).isEqualTo("scopes")
	}
}
