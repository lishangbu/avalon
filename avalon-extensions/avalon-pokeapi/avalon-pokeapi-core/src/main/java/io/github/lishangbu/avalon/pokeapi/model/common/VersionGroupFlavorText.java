package io.github.lishangbu.avalon.pokeapi.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.game.VersionGroup;
import io.github.lishangbu.avalon.pokeapi.model.language.Language;

/// 版本组本地化文本模型
///
/// @param text         本地化文本
/// @param language     文本所使用的语言引用
/// @param versionGroup 使用此文本的版本组引用
/// @author lishangbu
/// @see Language
/// @see VersionGroup
/// @since 2025/5/20
public record VersionGroupFlavorText(
        String text,
        NamedApiResource<Language> language,
        @JsonProperty("version_group") NamedApiResource<VersionGroup> versionGroup) {}
