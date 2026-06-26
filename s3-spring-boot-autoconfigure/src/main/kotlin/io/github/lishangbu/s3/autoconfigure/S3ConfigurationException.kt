package io.github.lishangbu.s3.autoconfigure

/**
 * S3 starter 配置不满足自动装配要求时抛出的异常。
 *
 * 该异常用于启动阶段的配置错误，帮助使用方在应用启动时直接定位
 * bucket、凭证或预签名过期时间等问题。
 */
class S3ConfigurationException(
	message: String,
	cause: Throwable? = null,
) : RuntimeException(message, cause)
