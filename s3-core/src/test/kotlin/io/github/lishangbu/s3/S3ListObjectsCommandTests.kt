package io.github.lishangbu.s3

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import kotlin.test.Test

/**
 * 验证列举对象命令的默认分页语义，以及 S3 页大小和分页 token 的边界约束。
 */
class S3ListObjectsCommandTests {
	@Test
	fun `default command requests the first page`() {
		val command = S3ListObjectsCommand()

		assertThat(command.prefix).isNull()
		assertThat(command.maxKeys).isEqualTo(1000)
		assertThat(command.continuationToken).isNull()
	}

	@Test
	fun `max keys must stay within S3 page size range`() {
		assertThatThrownBy { S3ListObjectsCommand(maxKeys = 0) }
			.isInstanceOf(IllegalArgumentException::class.java)
		assertThatThrownBy { S3ListObjectsCommand(maxKeys = 1001) }
			.isInstanceOf(IllegalArgumentException::class.java)
	}

	@Test
	fun `blank continuation token is rejected`() {
		assertThatThrownBy { S3ListObjectsCommand(continuationToken = " ") }
			.isInstanceOf(IllegalArgumentException::class.java)
	}
}
