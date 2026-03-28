package io.github.lishangbu.avalon.s3.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration
import java.util.LinkedHashMap

/**
 * Avalon S3 统一配置。
 *
 * 基于命名 client 暴露原生 AWS SDK client 与能力 facade。
 */
@ConfigurationProperties(prefix = AvalonS3Properties.PREFIX)
class AvalonS3Properties {
    /** 是否启用自动配置。 */
    var enabled: Boolean = false

    /** 默认 client 名称。 */
    var defaultClientName: String = DEFAULT_CLIENT_NAME

    /** 命名 client 配置。 */
    var clients: MutableMap<String, S3ClientProperties> =
        linkedMapOf(
            DEFAULT_CLIENT_NAME to S3ClientProperties(),
        )

    companion object {
        const val PREFIX: String = "avalon.s3"

        const val DEFAULT_CLIENT_NAME: String = "default"
    }
}

/** 单个命名 S3 client 的配置。 */
class S3ClientProperties {
    /** 是否启用当前 client。 */
    var enabled: Boolean = true

    /** 提供方类型。 */
    var provider: S3Provider = S3Provider.AWS

    /** AWS 区域。 */
    var region: String? = null

    /** 自定义 S3 端点。 */
    var endpoint: String? = null

    /** Access point / ARN 是否允许跨区域访问。 */
    var useArnRegionEnabled: Boolean = true

    /** 是否启用双栈。 */
    var dualstackEnabled: Boolean = false

    /** 是否启用 S3 accelerate。 */
    var accelerateModeEnabled: Boolean = false

    /** 是否启用路径风格访问。 */
    var pathStyleAccess: Boolean? = null

    /** 是否启用 chunked encoding。 */
    var chunkedEncodingEnabled: Boolean? = null

    /** 对象校验和验证。 */
    var checksumValidationEnabled: Boolean? = null

    /** 默认桶别名。 */
    var bucketAliases: MutableMap<String, String> = LinkedHashMap()

    /** 凭证配置。 */
    var credentials: S3CredentialsProperties = S3CredentialsProperties()

    /** 重试与超时配置。 */
    var overrides: S3ClientOverrideProperties = S3ClientOverrideProperties()

    /** HTTP 客户端配置。 */
    var http: S3HttpClientProperties = S3HttpClientProperties()

    /** 传输配置。 */
    var transfer: S3TransferProperties = S3TransferProperties()
}

/** 静态凭证配置。 */
class S3CredentialsProperties {
    /** Access key id。 */
    var accessKeyId: String? = null

    /** Secret access key。 */
    var secretAccessKey: String? = null

    /** Session token。 */
    var sessionToken: String? = null
}

/** 覆盖配置。 */
class S3ClientOverrideProperties {
    /** API 调用总超时。 */
    var apiCallTimeout: Duration? = null

    /** 单次重试超时。 */
    var apiCallAttemptTimeout: Duration? = null
}

/** HTTP 传输配置。 */
class S3HttpClientProperties {
    /** 连接超时。 */
    var connectionTimeout: Duration? = null

    /** 同步 socket 超时。 */
    var socketTimeout: Duration? = null

    /** 异步读取超时。 */
    var readTimeout: Duration? = null

    /** 异步写入超时。 */
    var writeTimeout: Duration? = null

    /** 同步客户端连接池大小。 */
    var maxConnections: Int? = null

    /** 异步客户端并发数。 */
    var maxConcurrency: Int? = null
}

/** Transfer Manager 配置。 */
class S3TransferProperties {
    /** 是否启用标准异步 multipart 支持。 */
    var multipartEnabled: Boolean = true
}
