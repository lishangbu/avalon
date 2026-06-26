package io.github.lishangbu.s3

/**
 * 经过校验的 S3 object key。
 *
 * Starter 在边界处统一校验 key，避免业务模块各自拼接路径时引入绝对路径、
 * 父级跳转片段或空白 key。
 */
@JvmInline
value class S3ObjectKey private constructor(val value: String) {
	companion object {
		private const val MAX_LENGTH = 1024

		fun of(value: String): S3ObjectKey {
			val normalizedValue = value.trim()
			if (normalizedValue.isBlank()) {
				throw S3InvalidKeyException(value, "S3 object key 不能为空")
			}
			if (normalizedValue.length > MAX_LENGTH) {
				throw S3InvalidKeyException(value, "S3 object key 不能超过 $MAX_LENGTH 个字符")
			}
			if (normalizedValue.startsWith("/") || normalizedValue.startsWith("\\")) {
				throw S3InvalidKeyException(value, "S3 object key 不能以路径分隔符开头")
			}
			if ('\\' in normalizedValue) {
				throw S3InvalidKeyException(value, "S3 object key 只能使用 / 作为路径分隔符")
			}
			if (normalizedValue.any(Char::isISOControl)) {
				throw S3InvalidKeyException(value, "S3 object key 不能包含控制字符")
			}
			if (normalizedValue.split('/').any { it == ".." }) {
				throw S3InvalidKeyException(value, "S3 object key 不能包含父级路径片段")
			}
			return S3ObjectKey(normalizedValue)
		}
	}
}
