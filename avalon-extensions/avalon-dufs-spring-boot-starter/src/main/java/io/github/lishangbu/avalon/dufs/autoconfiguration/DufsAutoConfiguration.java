package io.github.lishangbu.avalon.dufs.autoconfiguration;

import io.github.lishangbu.avalon.dufs.component.DefaultDufsClient;
import io.github.lishangbu.avalon.dufs.component.DufsClient;
import io.github.lishangbu.avalon.dufs.properties.DufsProperties;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/// DUFS 请求客户端配置
///
/// 提供用于和 DUFS 服务通信的 RestClient 与 DufsClient Bean
/// - dufsRestClient: 基于 JDK HttpClient 的 RestClient，支持重定向与基础认证头
/// - dufsClient: 使用 DefaultDufsClient 封装 RestClient
///
/// @author lishangbu
/// @since 2025/8/11
@EnableConfigurationProperties(DufsProperties.class)
@AutoConfiguration
public class DufsAutoConfiguration {
  public static final String DUFS_REST_CLIENT_BEAN_NAME = "dufsRestClient";

  /// 提供用于发送 HTTP 请求的 RestClient
  /// - 当配置中存在 `dufs.url` 时才创建该 Bean
  /// - 自动添加 Basic Auth 头（如配置了 username/password）
  ///
  /// @param properties DUFS 配置属性
  /// @return RestClient 请求客户端
  @Bean
  @ConditionalOnMissingBean(name = DUFS_REST_CLIENT_BEAN_NAME)
  @ConditionalOnProperty(prefix = DufsProperties.PROPERTIES_PREFIX, name = "url")
  public RestClient dufsRestClient(DufsProperties properties) {
    return RestClient.builder()
        .requestFactory(
            new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()))
        .baseUrl(properties.getUrl())
        .defaultHeaders(
            httpHeaders -> {
              String username = properties.getUsername();
              String password = properties.getPassword();
              if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
                String auth = username + ":" + password;
                String encodedAuth =
                    Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                httpHeaders.add(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth);
              }
            })
        .build();
  }

  /// DUFS 客户端封装，用于提供高层 API
  ///
  /// @param restClient 注入的 RestClient
  /// @return DufsClient 默认实现
  @Bean
  @ConditionalOnMissingBean(DufsClient.class)
  public DufsClient dufsClient(@Qualifier(DUFS_REST_CLIENT_BEAN_NAME) RestClient restClient) {
    return new DefaultDufsClient(restClient);
  }
}
