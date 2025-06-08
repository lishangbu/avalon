package io.github.lishangbu.avalon.pokeapi.model.common;

import io.github.lishangbu.avalon.pokeapi.model.language.Language;

/**
 * 参考<a href="https://pokeapi.co/docs/v2">官网Utility/Common Models/Effect</a>
 *
 * @param effect 特定语言中API资源的本地化效果文本
 * @param language 该效果所在的语言
 * @author lishangbu
 * @since 2025/5/20
 */
public record Effect(String effect, NamedApiResource<Language> language) {}
