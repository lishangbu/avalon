package io.github.lishangbu.avalon.dufs.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/// DUFS 客户端配置属性
///
/// 前缀为 {@code dufs}，包含连接 URL 及可选的认证信息
///
/// @author lishangbu
/// @since 2025/8/11
@ConfigurationProperties(prefix = DufsProperties.PROPERTIES_PREFIX)
public class DufsProperties {
  /// 配置属性前缀
  public static final String PROPERTIES_PREFIX = "dufs";

  private String url;

  private String username;

  private String password;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
