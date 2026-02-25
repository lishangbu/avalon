package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;

/// 宝可梦种类变种，表示同一物种的不同形式
///
/// @param isDefault 该变种是否为默认
/// @param pokemon   宝可梦变种引用
/// @author lishangbu
/// @see Pokemon
/// @since 2025/6/8
public record PokemonSpeciesVariety(
        @JsonProperty("is_default") Boolean isDefault, NamedApiResource<Pokemon> pokemon) {}
