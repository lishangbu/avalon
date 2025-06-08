package io.github.lishangbu.avalon.pokeapi.autoconfiguration;

import io.github.lishangbu.avalon.pokeapi.component.DefaultPokeApiService;
import io.github.lishangbu.avalon.pokeapi.component.PokeApiFactory;
import io.github.lishangbu.avalon.pokeapi.component.PokeApiService;
import io.github.lishangbu.avalon.pokeapi.properties.PokeApiProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

/**
 * POKE API相关组件自动装配
 *
 * @author lishangbu
 * @since 2025/5/21
 */
@AutoConfiguration
public class PokeApiComponentAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public PokeApiService pokeApiService(
      @Qualifier("pokeApiRestClient") RestClient restClient, PokeApiProperties properties) {
    return new DefaultPokeApiService(restClient, properties);
  }

  @Bean
  @ConditionalOnMissingBean
  public PokeApiFactory pokeApiFactory(PokeApiService pokeApiService) {
    return new PokeApiFactory(pokeApiService);
  }
}
