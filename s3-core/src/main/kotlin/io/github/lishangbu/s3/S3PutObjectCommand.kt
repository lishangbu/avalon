package io.github.lishangbu.s3

/**
 * 上传对象的命令参数。
 */
data class S3PutObjectCommand(
	val key: S3ObjectKey,
	val content: ByteArray,
	val contentType: String? = null,
	val metadata: Map<String, String> = emptyMap(),
) {
	override fun equals(other: Any?): Boolean {
		if (this === other) {
			return true
		}
		if (other !is S3PutObjectCommand) {
			return false
		}
		return key == other.key &&
			content.contentEquals(other.content) &&
			contentType == other.contentType &&
			metadata == other.metadata
	}

	override fun hashCode(): Int {
		var result = key.hashCode()
		result = 31 * result + content.contentHashCode()
		result = 31 * result + (contentType?.hashCode() ?: 0)
		result = 31 * result + metadata.hashCode()
		return result
	}
}
