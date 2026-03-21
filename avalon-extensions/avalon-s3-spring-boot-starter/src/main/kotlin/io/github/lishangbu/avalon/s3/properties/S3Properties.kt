package io.github.lishangbu.avalon.s3.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * S3 属性配置 封装 S3（兼容 MinIO）相关的配置项，如 endpoint、凭证、bucket 等 配置前缀 是否启用 S3 对象存储，默认 true 对象存储服务的访问地址 是否启用
 * path-style 访问
 * - true 表示 pathStyle
 * - false 表示 virtual-hosted-style） 是否启用 chunked encoding（某些服务需关闭） 区域（region） Access Key Secret Key
 *   默认的 bucket 名称
 */

/**
 * S3 属性配置。
 *
 * 封装 S3（兼容 MinIO）相关的配置项，如 endpoint、凭证、bucket 等。
 */
@ConfigurationProperties(prefix = S3Properties.PREFIX)
class S3Properties {
    var enabled: Boolean = true

    var endpoint: String? = null

    var pathStyleAccess: Boolean = true

    var chunkedEncodingEnabled: Boolean = false

    var region: String? = null

    var accessKey: String? = null

    var secretKey: String? = null

    var bucketName: String? = null

    companion object {
        const val PREFIX: String = "s3"
    }
}
