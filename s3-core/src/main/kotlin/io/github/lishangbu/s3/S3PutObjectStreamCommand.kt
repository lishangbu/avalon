package io.github.lishangbu.s3

import java.io.InputStream

/**
 * 使用输入流上传对象的命令参数。
 *
 * AWS SDK 需要调用方提供准确的内容长度，因此 [contentLength] 必须与
 * [content] 可读取的字节数保持一致。该命令不拥有输入流生命周期，调用方
 * 仍然负责关闭 [content]。
 */
data class S3PutObjectStreamCommand(
	val key: S3ObjectKey,
	val content: InputStream,
	val contentLength: Long,
	val contentType: String? = null,
	val metadata: Map<String, String> = emptyMap(),
) {
	init {
		require(contentLength >= 0) {
			"S3 stream upload contentLength 不能小于 0"
		}
	}
}
