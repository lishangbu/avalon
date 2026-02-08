package io.github.lishangbu.avalon.pokeapi.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.language.Language;

/// PokeAPI 风味文本模型（本地化描述）
///
/// @param flavorText API 资源在特定语言中的本地化描述文本，可能包含特殊字符
/// @param language   该描述文本所使用的语言
/// @param version    该描述文本对应的游戏版本
/// @author lishangbu
/// @since 2025/5/20
public record FlavorText(
    @JsonProperty("flavor_text") String flavorText,
    NamedApiResource<Language> language,
    NamedApiResource<?> version) {}
