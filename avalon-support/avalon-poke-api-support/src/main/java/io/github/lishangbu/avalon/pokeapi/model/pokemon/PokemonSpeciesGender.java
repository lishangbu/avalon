package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;

/// 宝可梦种类性别信息模型
///
/// @param rate           成为雌性的概率（以 1/8 为单位），或 -1 表示无性别
/// @param pokemonSpecies 指定性别的宝可梦种类引用
/// @author lishangbu
/// @since 2025/6/8
public record PokemonSpeciesGender(
    Integer rate,
    @JsonProperty("pokemon_species") NamedApiResource<PokemonSpecies> pokemonSpecies) {}
