package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/**
 * 性别是在第二代游戏中引入的，主要用于宝可梦的繁殖，但也可能导致外观差异甚至不同的进化路线。 更多详情请查阅 <a
 * href="http://bulbapedia.bulbagarden.net/wiki/Gender">Bulbapedia</a>。
 *
 * @param id 该资源的标识符
 * @param name 该资源的名称
 * @param pokemonSpeciesDetails 可以是此性别的宝可梦种类{@link PokemonSpeciesGender}列表及其可能性
 * @param requiredForEvolution 需要此性别才能进化的宝可梦种类{@link PokemonSpecies}列表
 * @author lishangbu
 * @see PokemonSpeciesGender
 * @see PokemonSpecies
 * @since 2025/6/8
 */
public record Gender(
    Integer id,
    String name,
    @JsonProperty("pokemon_species_details") List<PokemonSpeciesGender> pokemonSpeciesDetails,
    @JsonProperty("required_for_evolution")
        List<NamedApiResource<PokemonSpecies>> requiredForEvolution) {}
