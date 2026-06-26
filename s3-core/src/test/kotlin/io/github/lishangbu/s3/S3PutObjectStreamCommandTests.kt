package io.github.lishangbu.s3

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import java.io.ByteArrayInputStream
import kotlin.test.Test

/**
 * 验证流式上传命令的长度约束，确保空对象可上传而非法负长度会被拒绝。
 */
class S3PutObjectStreamCommandTests {
	@Test
	fun `zero length stream upload command is accepted`() {
		val command = S3PutObjectStreamCommand(
			key = S3ObjectKey.of("empty.txt"),
			content = ByteArrayInputStream(ByteArray(0)),
			contentLength = 0,
		)

		assertThat(command.contentLength).isZero()
	}

	@Test
	fun `negative content length is rejected`() {
		assertThatThrownBy {
			S3PutObjectStreamCommand(
				key = S3ObjectKey.of("broken.txt"),
				content = ByteArrayInputStream(ByteArray(0)),
				contentLength = -1,
			)
		}.isInstanceOf(IllegalArgumentException::class.java)
	}
}
