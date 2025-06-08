package io.github.lishangbu.avalon.pokeapi.model.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.language.Language;

/**
 * 参考<a href="https://pokeapi.co/docs/v2">官网Utility/Common Models/VerboseEffect</a>
 *
 * @param effect 该API资源在特定语言中的本地化效果文本
 * @param shortEffect 该API资源的简短本地化效果文本
 * @param language 该效果文本所使用的语言
 * @author lishangbu
 * @since 2025/5/20
 */
public record VerboseEffect(
    String effect,
    @JsonProperty("short_effect") String shortEffect,
    NamedApiResource<Language> language) {}
