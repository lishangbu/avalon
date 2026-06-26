package io.github.lishangbu.s3

import java.io.Closeable
import java.io.InputStream
import java.time.Instant

/**
 * 流式下载对象时返回的内容流和对象元数据。
 *
 * 该类型持有底层 HTTP 响应流。调用方读取完成后必须调用 [close]，
 * 推荐使用 Kotlin `use` 语法释放资源。
 */
class S3ObjectStream(
	val key: S3ObjectKey,
	val content: InputStream,
	val contentType: String?,
	val metadata: Map<String, String>,
	val eTag: String?,
	val contentLength: Long,
	val lastModified: Instant?,
) : Closeable {
	override fun close() {
		content.close()
	}
}
