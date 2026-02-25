package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.game.VersionGroup;
import io.github.lishangbu.avalon.pokeapi.model.language.Language;

/// 特性的风味文本描述
///
/// @param flavorText   特定语言中 API 资源的本地化风味文本
/// @param language     此文本资源所使用的语言
/// @param versionGroup 使用此风味文本的版本组
/// @author lishangbu
/// @see Language
/// @see VersionGroup
/// @since 2025/6/8
public record AbilityFlavorText(
        @JsonProperty("flavor_text") String flavorText,
        NamedApiResource<Language> language,
        @JsonProperty("version_group") NamedApiResource<VersionGroup> versionGroup) {}
