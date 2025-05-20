package io.github.lishangbu.avalon.pokeapi.autoconfiguration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

/**
 * RestClient自动配置
 *
 * @author lishangbu
 * @since 2025/5/20
 */
@AutoConfiguration
public class RestClientAutoConfiguration {
  /**
   * 用于请求的web客户端
   *
   * @return
   */
  @Bean
  @ConditionalOnMissingBean(name = "pokeApiRestClient")
  public RestClient pokeApiRestClient() {
    return RestClient.builder().baseUrl("https://pokeapi.co/api/v2/").build();
  }
}
