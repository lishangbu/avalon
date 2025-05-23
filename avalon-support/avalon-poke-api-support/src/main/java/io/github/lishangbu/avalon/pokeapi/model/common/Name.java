package io.github.lishangbu.avalon.pokeapi.model.common;

import io.github.lishangbu.avalon.pokeapi.model.language.Language;

/**
 * 参考<a href="https://pokeapi.co/docs/v2">官网Utility/Common Models/Name</a>
 *
 * @param name 特定语言中API资源的本地化名称
 * @param language 该名称所在的语言
 * @author lishangbu
 * @since 2025/5/20
 */
public record Name(String name, NamedApiResource<Language> language) {}
