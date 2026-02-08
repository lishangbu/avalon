package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/// 性别模型
///
/// 性别主要用于宝可梦繁殖，并可能影响外观或进化，详情参考 [Bulbapedia](http://bulbapedia.bulbagarden.net/wiki/Gender)
///
/// @param id                    资源标识符
/// @param name                  资源名称
/// @param pokemonSpeciesDetails 该性别下的宝可梦种类及其可能性 {@link PokemonSpeciesGender}
/// @param requiredForEvolution  需要此性别才能进化的宝可梦种类列表 {@link PokemonSpecies}
/// @author lishangbu
/// @see PokemonSpeciesGender
/// @see PokemonSpecies
/// @since 2025/6/8
public record Gender(
    Integer id,
    String name,
    @JsonProperty("pokemon_species_details") List<PokemonSpeciesGender> pokemonSpeciesDetails,
    @JsonProperty("required_for_evolution")
        List<NamedApiResource<PokemonSpecies>> requiredForEvolution) {}
