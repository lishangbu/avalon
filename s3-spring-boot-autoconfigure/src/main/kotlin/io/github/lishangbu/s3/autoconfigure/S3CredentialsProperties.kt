package io.github.lishangbu.s3.autoconfigure

/**
 * 显式 S3 凭证配置。
 *
 * 为空时自动配置使用 AWS SDK 默认凭证链，便于生产环境接入 IAM role、
 * 环境变量或共享 credentials 文件。
 */
class S3CredentialsProperties {
	/**
	 * 显式配置的访问密钥 ID。
	 */
	var accessKey: String = ""

	/**
	 * 显式配置的访问密钥。
	 */
	var secretKey: String = ""

	/**
	 * 临时凭证的会话 token。
	 */
	var sessionToken: String = ""
}
