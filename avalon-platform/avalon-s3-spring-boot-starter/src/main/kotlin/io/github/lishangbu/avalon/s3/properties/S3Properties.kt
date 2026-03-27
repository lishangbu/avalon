package io.github.lishangbu.avalon.s3.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * S3 配置属性
 *
 * 封装 S3 或 MinIO 的连接配置
 */
@ConfigurationProperties(prefix = S3Properties.PREFIX)
class S3Properties {
    /** 启用状态 */
    var enabled: Boolean = true

    /** 端点 */
    var endpoint: String? = null

    /** 路径风格访问 */
    var pathStyleAccess: Boolean = true

    /** 分块编码启用状态 */
    var chunkedEncodingEnabled: Boolean = false

    /** 区域 */
    var region: String? = null

    /** 访问密钥 */
    var accessKey: String? = null

    /** 密钥 */
    var secretKey: String? = null

    /** 存储桶名称 */
    var bucketName: String? = null

    companion object {
        /** 配置前缀 */
        const val PREFIX: String = "s3"
    }
}
