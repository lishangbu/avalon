package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;

/**
 * 宝可梦种类的性别信息
 *
 * @param rate 此宝可梦为雌性的概率，以八分之几表示；或者-1表示无性别
 * @param pokemonSpecies 可以是所引用性别的宝可梦种类
 * @author lishangbu
 * @since 2025/6/8
 */
public record PokemonSpeciesGender(
    Integer rate,
    @JsonProperty("pokemon_species") NamedApiResource<PokemonSpecies> pokemonSpecies) {}
