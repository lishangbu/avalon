package io.github.lishangbu.avalon.pokeapi;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/// PokeAPI 数据提供接口
///
/// 统一定义从来源加载指定类型数据的契约，供导出流程复用
///
@AutoConfiguration
@ComponentScan("io.github.lishangbu.avalon.pokeapi.dataprovider")
public class PokeApiDataProviderAutoConfiguration {}
