package io.github.lishangbu.avalon.pokeapi.model.common;

import io.github.lishangbu.avalon.pokeapi.model.language.Language;

/// PokeAPI 本地化描述模型
///
/// @param description 特定语言中 API 资源的本地化描述
/// @param language    此描述所使用的语言
/// @author lishangbu
/// @since 2025/5/20
public record Description(String description, NamedApiResource<Language> language) {}
