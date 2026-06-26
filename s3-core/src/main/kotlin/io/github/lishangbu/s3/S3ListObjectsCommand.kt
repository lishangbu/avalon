package io.github.lishangbu.s3

/**
 * 按前缀分页列举对象的命令参数。
 */
data class S3ListObjectsCommand(
	val prefix: S3ObjectKey? = null,
	val maxKeys: Int = 1000,
	val continuationToken: String? = null,
) {
	init {
		require(maxKeys in 1..1000) {
			"S3 listObjects maxKeys 必须在 1 到 1000 之间"
		}
		require(continuationToken == null || continuationToken.isNotBlank()) {
			"S3 listObjects continuationToken 不能是空白字符串"
		}
	}
}
