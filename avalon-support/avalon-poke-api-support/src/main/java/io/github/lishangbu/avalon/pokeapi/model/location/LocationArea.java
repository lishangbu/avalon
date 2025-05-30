package io.github.lishangbu.avalon.pokeapi.model.location;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/**
 * Location areas are sections of areas, such as floors in a building or cave. Each area has its own
 * set of possible Pokémon encounters.
 *
 * @param id 资源的标识符
 * @param name 资源的名称
 * @param gameIndex 游戏数据内API资源的内部ID
 * @param encounterMethodRates 宝可梦在此区域可能遇到的方法列表，以及该方法在游戏不同版本中出现的可能性
 * @param location 可以找到此位置区域的区域
 * @param names 此资源在不同语言中列出的名称
 * @param pokemonEncounters 可在此区域遇到的宝可梦列表及其版本特定的遭遇详情
 * @author lishangbu
 * @since 2025/5/26
 */
public record LocationArea(
    int id,
    String name,
    @JsonProperty("game_index") int gameIndex,
    @JsonProperty("encounter_method_rates") List<EncounterMethodRate> encounterMethodRates,
    NamedApiResource<Location> location,
    List<Name> names,
    @JsonProperty("pokemon_encounters") List<PokemonEncounter> pokemonEncounters) {}
