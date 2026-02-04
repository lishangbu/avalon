package io.github.lishangbu.avalon.pokeapi.model.common;

import io.github.lishangbu.avalon.pokeapi.model.language.Language;

/// PokeAPI Name 工具模型（本地化名称）
///
/// 表示某个资源在特定语言下的本地化名称
///
/// @param name     特定语言中 API 资源的本地化名称
/// @param language 该名称所在的语言
/// @author lishangbu
/// @since 2025/5/20
public record Name(String name, NamedApiResource<Language> language) {}
