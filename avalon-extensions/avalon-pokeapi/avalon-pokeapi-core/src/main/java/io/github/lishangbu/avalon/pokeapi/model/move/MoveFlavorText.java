package io.github.lishangbu.avalon.pokeapi.model.move;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.game.VersionGroup;
import io.github.lishangbu.avalon.pokeapi.model.language.Language;

/// 招式风味文本模型
///
/// 招式在特定语言和版本组中的本地化描述
///
/// @param flavorText   本地化风味文本
/// @param language     文本所使用的语言引用
/// @param versionGroup 使用此风味文本的版本组引用
/// @author lishangbu
/// @see Language
/// @see VersionGroup
/// @since 2025/6/7
public record MoveFlavorText(
        @JsonProperty("flavor_text") String flavorText,
        NamedApiResource<Language> language,
        @JsonProperty("version_group") NamedApiResource<VersionGroup> versionGroup) {}
