package io.github.lishangbu.avalon.pokeapi.model.location;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import java.util.List;

/**
 * 伙伴公园中用于分组宝可梦遭遇的区域。它们就像特定于 <a href="https://bulbapedia.bulbagarden.net/wiki/Pal_Park"
 * >伙伴公园</a>的栖息地。
 *
 * @param id 资源的标识符
 * @param name 资源的名称
 * @param names 该资源在不同语言中列出的名称{@link Name}
 * @param pokemonEncounters 在此伙伴公园区域遇到的宝可梦列表及详情{@link PalParkEncounterSpecies}
 * @see Name
 * @see PalParkEncounterSpecies
 * @author lishangbu
 * @since 2025/5/26
 */
public record PalParkArea(
    Integer id,
    String name,
    List<Name> names,
    @JsonProperty("pokemon_encounters") List<PalParkEncounterSpecies> pokemonEncounters) {}
