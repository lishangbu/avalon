package io.github.lishangbu.avalon.pokeapi.model.pokemon.type;

import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;

/**
 * 参考<a href="https://pokeapi.co/docs/v2">官网Pokémon/Types/TypePokemon</a>
 *
 * @author lishangbu
 * @since 2025/5/20
 */
public record TypePokemon(Integer slot, NamedApiResource<?> pokemon) {
  /** 获取宝可梦属性的顺序 */
  public Integer slot() {
    return slot;
  }

  /** 获取拥有该属性的宝可梦 */
  public NamedApiResource<?> pokemon() {
    return pokemon;
  }
}
