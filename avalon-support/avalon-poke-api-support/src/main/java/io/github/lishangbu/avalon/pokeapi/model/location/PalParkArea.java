package io.github.lishangbu.avalon.pokeapi.model.location;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import java.util.List;

/**
 * Areas used for grouping Pokémon encounters in Pal Park. They're like habitats that are specific
 * to <a href="https://bulbapedia.bulbagarden.net/wiki/Pal_Park" >Pal Park</a>.
 *
 * @param id 资源的标识符
 * @param name 资源的名称
 * @param names 该资源在不同语言中列出的名称
 * @param pokemonEncounters 在此帕尔公园区域遇到的宝可梦列表及详情
 * @author lishangbu
 * @since 2025/5/26
 */
public record PalParkArea(
    int id,
    String name,
    List<Name> names,
    @JsonProperty("pokemon_encounters") List<PalParkEncounterSpecies> pokemonEncounters) {}
