package io.github.lishangbu.s3

/**
 * S3 object key 不满足 starter 统一约束时抛出的异常。
 */
class S3InvalidKeyException(
	val invalidKey: String,
	message: String,
) : S3StorageException(message)
