package io.github.lishangbu.avalon.pokeapi.autoconfiguration;

import io.github.lishangbu.avalon.pokeapi.service.PokeApiTemplate;
import io.github.lishangbu.avalon.pokeapi.service.impl.DefaultPokeApiTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

/**
 * POKE API请求模板配置
 *
 * @author lishangbu
 * @since 2025/5/21
 */
@AutoConfiguration
public class PokeApiTemplateAutoConfiguration {
  /**
   * 用于请求的web客户端
   *
   * @return
   */
  @Bean
  @ConditionalOnMissingBean
  public PokeApiTemplate pokeApiTemplate(RestClient pokeApiRestClient) {
    return new DefaultPokeApiTemplate(pokeApiRestClient);
  }
}
