package io.github.lishangbu.avalon.dufs.autoconfiguration;

import io.github.lishangbu.avalon.dufs.component.DefaultDufsClient;
import io.github.lishangbu.avalon.dufs.component.DufsClient;
import io.github.lishangbu.avalon.dufs.properties.DufsProperties;
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

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * DUFS请求客户端配置
 *
 * @author lishangbu
 * @since 2025/8/11
 */
@EnableConfigurationProperties(DufsProperties.class)
@AutoConfiguration
public class DufsAutoConfiguration {
  public static final String DUFS_REST_CLIENT_BEAN_NAME = "dufsRestClient";

  /**
   * 用于请求的web客户端
   *
   * <ul>
   *   <li>处理重定向支持,参考<a
   *       href="https://github.com/spring-projects/spring-framework/issues/33734">github
   *       issues</a>的处理
   *   <li>添加默认访问地址
   * </ul>
   *
   * @return Rest 请求客户端
   */
  @Bean
  @ConditionalOnMissingBean(name = DUFS_REST_CLIENT_BEAN_NAME)
  @ConditionalOnProperty(prefix = DufsProperties.PROPERTIES_PREFIX, name = "url")
  public RestClient dufsRestClient(DufsProperties properties) {
    return RestClient.builder()
      .requestFactory(
        new JdkClientHttpRequestFactory(
          HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()))
      .baseUrl(properties.getUrl())
      .defaultHeaders(httpHeaders -> {
        String username = properties.getUsername();
        String password = properties.getPassword();
        if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
          String auth = username + ":" + password;
          String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
          httpHeaders.add(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth);
        }
      })
      .build();
  }

  @Bean
  @ConditionalOnMissingBean(DufsClient.class)
  public DufsClient dufsClient(@Qualifier(DUFS_REST_CLIENT_BEAN_NAME) RestClient restClient) {
    return new DefaultDufsClient(restClient);
  }
}
