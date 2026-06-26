package io.github.lishangbu.s3.autoconfigure

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty
import java.net.URI

/**
 * S3 starter 的配置属性。
 *
 * 这些属性只描述自动装配所需的最小 S3 连接信息。具体校验在
 * [S3AutoConfiguration] 中执行，以便启动失败时统一抛出 [S3ConfigurationException]。
 */
@ConfigurationProperties(prefix = "s3")
class S3Properties {
	/**
	 * 是否启用 S3 自动配置。
	 */
	var enabled: Boolean = false

	/**
	 * 对象存储 bucket 名称。
	 */
	var bucket: String = ""

	/**
	 * AWS SDK 使用的 region；为空白时自动配置回退到默认 region。
	 */
	var region: String = "us-east-1"

	/**
	 * 私有 S3 或 S3 兼容服务的 endpoint。
	 */
	var endpoint: URI? = null

	/**
	 * 是否启用 path-style 访问，MinIO 等兼容服务通常需要打开。
	 */
	var pathStyleAccessEnabled: Boolean = false

	/**
	 * starter 统一附加到 object key 前的逻辑前缀。
	 */
	var keyPrefix: String = ""

	@NestedConfigurationProperty
	var credentials: S3CredentialsProperties = S3CredentialsProperties()

	@NestedConfigurationProperty
	var presign: S3PresignProperties = S3PresignProperties()
}
