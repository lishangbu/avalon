package io.github.lishangbu.avalon.pokeapi.model.game;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;

/**
 * 图鉴中的宝可梦条目信息
 *
 * @param entryNumber 宝可梦物种在图鉴中的索引号
 * @param pokemonSpecies 被遇到的宝可梦物种
 * @author lishangbu
 * @since 2025/5/24
 */
public record PokemonEntry(
    @JsonProperty("entry_number") Integer entryNumber,
    @JsonProperty("pokemon_species") NamedApiResource<?> pokemonSpecies) {}
