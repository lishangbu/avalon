package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;

/**
 * 宝可梦的特性
 *
 * @param isHidden 这是否为隐藏特性
 * @param slot 此特性在宝可梦种类中占据的位置
 * @param ability 宝可梦可能拥有的特性{@link Ability}
 * @author lishangbu
 * @see Ability
 * @since 2025/6/8
 */
public record PokemonAbility(
    @JsonProperty("is_hidden") Boolean isHidden, Integer slot, NamedApiResource<Ability> ability) {}
