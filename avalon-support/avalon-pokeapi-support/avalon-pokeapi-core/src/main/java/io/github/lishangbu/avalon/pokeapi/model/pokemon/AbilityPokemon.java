package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;

/// 宝可梦与特性的关联信息
///
/// @param isHidden 是否为隐藏特性
/// @param slot     特性槽位编号
/// @param pokemon  可能拥有此特性的宝可梦引用 {@link Pokemon}
/// @author lishangbu
/// @see Pokemon
/// @since 2025/6/8
public record AbilityPokemon(
    @JsonProperty("is_hidden") Boolean isHidden, Integer slot, NamedApiResource<Pokemon> pokemon) {}
