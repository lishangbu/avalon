package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.language.Language;

/// 宝可梦种类的分类科属信息
///
/// @param genus    所引用宝可梦种类的本地化科属
/// @param language 此科属所使用的语言
/// @author lishangbu
/// @see Language
/// @since 2025/6/8
public record Genus(String genus, NamedApiResource<Language> language) {}
