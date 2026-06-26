package io.github.lishangbu.s3

/**
 * 下载对象后的内容和元数据。
 */
data class S3ObjectContent(
	val key: S3ObjectKey,
	val content: ByteArray,
	val contentType: String?,
	val metadata: Map<String, String>,
	val eTag: String?,
	val contentLength: Long,
) {
	override fun equals(other: Any?): Boolean {
		if (this === other) {
			return true
		}
		if (other !is S3ObjectContent) {
			return false
		}
		return key == other.key &&
			content.contentEquals(other.content) &&
			contentType == other.contentType &&
			metadata == other.metadata &&
			eTag == other.eTag &&
			contentLength == other.contentLength
	}

	override fun hashCode(): Int {
		var result = key.hashCode()
		result = 31 * result + content.contentHashCode()
		result = 31 * result + (contentType?.hashCode() ?: 0)
		result = 31 * result + metadata.hashCode()
		result = 31 * result + (eTag?.hashCode() ?: 0)
		result = 31 * result + contentLength.hashCode()
		return result
	}
}
