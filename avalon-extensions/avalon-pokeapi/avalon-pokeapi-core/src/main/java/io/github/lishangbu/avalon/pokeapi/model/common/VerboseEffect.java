package io.github.lishangbu.avalon.pokeapi.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.language.Language;

/// 详尽效果文本模型
///
/// @param effect      本地化的效果文本
/// @param shortEffect 简短的本地化效果文本
/// @param language    文本所使用的语言
/// @author lishangbu
/// @since 2025/5/20
public record VerboseEffect(
        String effect,
        @JsonProperty("short_effect") String shortEffect,
        NamedApiResource<Language> language) {}
