package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;

/**
 * 宝可梦的属性
 *
 * @param slot 宝可梦属性列表中的顺序
 * @param type 宝可梦拥有的属性{@link Type}
 * @author lishangbu
 * @see Type
 * @since 2025/6/8
 */
public record PokemonType(Integer slot, NamedApiResource<Type> type) {}
