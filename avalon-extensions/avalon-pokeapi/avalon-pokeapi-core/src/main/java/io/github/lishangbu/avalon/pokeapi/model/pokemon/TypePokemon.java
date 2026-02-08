package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;

/// 类型-宝可梦关系模型
///
/// 参考 PokeAPI 文档: [Pokémon/Types/TypePokemon](https://pokeapi.co/docs/v2)
///
/// @param slot    宝可梦属性的顺序
/// @param pokemon 拥有该属性的宝可梦引用
/// @author lishangbu
/// @since 2025/5/20
public record TypePokemon(Integer slot, NamedApiResource<?> pokemon) {}
