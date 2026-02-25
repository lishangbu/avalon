package io.github.lishangbu.avalon.pokeapi;

import io.github.lishangbu.avalon.json.autoconfiguration.JacksonAutoConfiguration;
import io.github.lishangbu.avalon.pokeapi.autoconfiguration.PokeDataComponentAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/// 测试环境自动配置类
///
/// @author lishangbu
/// @since 2025/8/20
@Import(
        value = {
            PokeApiDataProviderAutoConfiguration.class,
            PokeDataComponentAutoConfiguration.class,
            JacksonAutoConfiguration.class
        })
@Configuration(proxyBeanMethods = false)
public class TestEnvironmentApplication {}
