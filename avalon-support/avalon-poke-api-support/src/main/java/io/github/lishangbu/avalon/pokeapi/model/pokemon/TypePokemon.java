package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;

/**
 * 参考<a href="https://pokeapi.co/docs/v2">官网Pokémon/Types/TypePokemon</a>
 *
 * @param slot 宝可梦属性的顺序
 * @param pokemon 拥有该属性的宝可梦
 * @author lishangbu
 * @since 2025/5/20
 */
public record TypePokemon(Integer slot, NamedApiResource<?> pokemon) {}
