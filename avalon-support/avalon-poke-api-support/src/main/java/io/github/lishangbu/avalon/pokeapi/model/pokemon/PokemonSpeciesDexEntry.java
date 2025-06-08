package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.game.Pokedex;

/**
 * 宝可梦种类在图鉴中的记录条目
 *
 * @param entryNumber 宝可梦图鉴中的索引编号
 * @param pokedex 所引用宝可梦种类可以在其中找到的宝可梦图鉴{@link Pokedex}
 * @author lishangbu
 * @see Pokedex
 * @since 2025/6/8
 */
public record PokemonSpeciesDexEntry(
    @JsonProperty("entry_number") Integer entryNumber, NamedApiResource<Pokedex> pokedex) {}
