package io.github.lishangbu.avalon.s3.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/// S3 属性配置
///
/// 封装 S3（兼容 MinIO）相关的配置项，如 endpoint、凭证、bucket 等
@Data
@ConfigurationProperties(prefix = S3Properties.PREFIX)
public class S3Properties {

  /// 配置前缀
  public static final String PREFIX = "s3";

  /// 是否启用 S3 对象存储，默认 true
  private boolean enabled = true;

  /// 对象存储服务的访问地址
  private String endpoint;

  /// 是否启用 path-style 访问
  /// - true 表示 pathStyle
  /// - false 表示 virtual-hosted-style）
  private Boolean pathStyleAccess = true;

  /// 是否启用 chunked encoding（某些服务需关闭）
  private Boolean chunkedEncodingEnabled = false;

  /// 区域（region）
  private String region;

  /// Access Key
  private String accessKey;

  /// Secret Key
  private String secretKey;

  /// 默认的 bucket 名称
  private String bucketName;
}
