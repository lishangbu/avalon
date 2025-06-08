package io.github.lishangbu.avalon.pokeapi.autoconfiguration;

import io.github.lishangbu.avalon.pokeapi.properties.PokeApiProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
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
   * @return
   */
  @Bean
  @ConditionalOnMissingBean(name = "pokeApiRestClient")
  public RestClient pokeApiRestClient(PokeApiProperties properties) {
    return RestClient.builder().baseUrl(properties.getApiUrl()).build();
  }
}
