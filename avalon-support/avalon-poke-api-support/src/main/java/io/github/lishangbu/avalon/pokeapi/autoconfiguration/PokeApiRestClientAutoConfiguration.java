package io.github.lishangbu.avalon.pokeapi.autoconfiguration;

import io.github.lishangbu.avalon.pokeapi.properties.PokeApiProperties;
import java.net.http.HttpClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * RestClient自动配置
 *
 * @author lishangbu
 * @since 2025/5/20
 */
@EnableConfigurationProperties(PokeApiProperties.class)
@AutoConfiguration
public class PokeApiRestClientAutoConfiguration {
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
  @ConditionalOnMissingBean(name = "pokeApiRestClient")
  public RestClient pokeApiRestClient(PokeApiProperties properties) {
    return RestClient.builder()
        .requestFactory(
            new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()))
        .baseUrl(properties.getApiUrl())
        .build();
  }
}
