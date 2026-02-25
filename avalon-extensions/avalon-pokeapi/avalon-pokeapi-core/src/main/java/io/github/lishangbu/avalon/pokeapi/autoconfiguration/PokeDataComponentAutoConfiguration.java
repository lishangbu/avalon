package io.github.lishangbu.avalon.pokeapi.autoconfiguration;

import io.github.lishangbu.avalon.pokeapi.component.DefaultPokeApiService;
import io.github.lishangbu.avalon.pokeapi.component.PokeApiService;
import io.github.lishangbu.avalon.pokeapi.properties.PokeApiProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/// POKE API 相关组件自动装配
///
/// 提供 PokeApiService 的默认实现自动装配
///
/// @author lishangbu
/// @since 2025/5/21
@EnableConfigurationProperties(PokeApiProperties.class)
@AutoConfiguration
public class PokeDataComponentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PokeApiService pokeApiService(PokeApiProperties properties) {
        return new DefaultPokeApiService(properties);
    }
}
