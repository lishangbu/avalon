package io.github.lishangbu.avalon.s3.properties

/** S3 兼容实现提供方。 */
enum class S3Provider {
    AWS,
    MINIO,
    RUSTFS,
    ALIYUN_OSS,
    QINIU_KODO,
    GENERIC_S3,
}
