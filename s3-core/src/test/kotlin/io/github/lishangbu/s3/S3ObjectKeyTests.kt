package io.github.lishangbu.s3

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import kotlin.test.Test

/**
 * 验证对象 key 在进入 S3 操作前完成归一化和路径安全校验。
 */
class S3ObjectKeyTests {
	@Test
	fun `valid key is accepted and trimmed`() {
		val key = S3ObjectKey.of(" assets/avatar.png ")

		assertThat(key.value).isEqualTo("assets/avatar.png")
	}

	@Test
	fun `blank key is rejected`() {
		assertThatThrownBy { S3ObjectKey.of(" ") }
			.isInstanceOf(S3InvalidKeyException::class.java)
	}

	@Test
	fun `absolute path key is rejected`() {
		assertThatThrownBy { S3ObjectKey.of("/assets/avatar.png") }
			.isInstanceOf(S3InvalidKeyException::class.java)
	}

	@Test
	fun `parent path key is rejected`() {
		assertThatThrownBy { S3ObjectKey.of("assets/../avatar.png") }
			.isInstanceOf(S3InvalidKeyException::class.java)
	}
}
