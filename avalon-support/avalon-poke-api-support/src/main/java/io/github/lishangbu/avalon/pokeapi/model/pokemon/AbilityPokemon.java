package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;

/**
 * 宝可梦与特性的关联信息
 *
 * @param isHidden 这是否为所引用宝可梦的隐藏特性
 * @param slot 宝可梦有3个特性"槽位"，其中包含它们可能拥有的特性的引用。这是所引用宝可梦的此特性的槽位
 * @param pokemon 可能拥有此特性的宝可梦{@link Pokemon}
 * @author lishangbu
 * @see Pokemon
 * @since 2025/6/8
 */
public record AbilityPokemon(
    @JsonProperty("is_hidden") Boolean isHidden, Integer slot, NamedApiResource<Pokemon> pokemon) {}
