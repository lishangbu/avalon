package io.github.lishangbu.avalon.pokeapi.model.location;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/**
 * 位置区域是区域的分段，例如建筑物或洞穴中的楼层。每个区域都有自己的一组可能的宝可梦遭遇。
 *
 * @param id 资源的标识符
 * @param name 资源的名称
 * @param gameIndex 游戏数据内API资源的内部ID
 * @param encounterMethodRates 宝可梦在此区域可能遇到的方法列表{@link EncounterMethodRate}，以及该方法在游戏不同版本中出现的可能性
 * @param location 可以找到此位置区域的区域{@link Location}
 * @param names 此资源在不同语言中列出的名称{@link Name}
 * @param pokemonEncounters 可在此区域遇到的宝可梦列表及其版本特定的遭遇详情{@link PokemonEncounter}
 * @see EncounterMethodRate
 * @see Location
 * @see Name
 * @see PokemonEncounter
 * @author lishangbu
 * @since 2025/5/26
 */
public record LocationArea(
    Integer id,
    String name,
    @JsonProperty("game_index") Integer gameIndex,
    @JsonProperty("encounter_method_rates") List<EncounterMethodRate> encounterMethodRates,
    NamedApiResource<Location> location,
    List<Name> names,
    @JsonProperty("pokemon_encounters") List<PokemonEncounter> pokemonEncounters) {}
