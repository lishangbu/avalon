package io.github.lishangbu.avalon.pokeapi.autoconfiguration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * POKE API请求模板配置
 *
 * @author lishangbu
 * @since 2025/5/21
 */
@AutoConfiguration
@ComponentScan("io.github.lishangbu.avalon.pokeapi.service")
public class PokeApiServiceAutoConfiguration {}
