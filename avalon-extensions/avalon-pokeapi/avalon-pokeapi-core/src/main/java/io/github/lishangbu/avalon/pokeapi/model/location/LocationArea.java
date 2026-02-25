package io.github.lishangbu.avalon.pokeapi.model.location;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/// 位置区域模型
///
/// 表示区域内的子区域（如建筑物楼层或洞穴区域），每个区域都有特定的遭遇列表
///
/// @param id                   资源标识符
/// @param name                 资源名称
/// @param gameIndex            游戏数据内 API 资源的内部 ID
/// @param encounterMethodRates 遭遇方法及其在不同版本中的出现概率
/// @param location             所属区域引用
/// @param names                不同语言下的名称列表
/// @param pokemonEncounters    在此区域遇到的宝可梦及其版本性遭遇详情
/// @author lishangbu
/// @see EncounterMethodRate
/// @see Location
/// @see Name
/// @see PokemonEncounter
/// @since 2025/5/26
public record LocationArea(
        Integer id,
        String name,
        @JsonProperty("game_index") Integer gameIndex,
        @JsonProperty("encounter_method_rates") List<EncounterMethodRate> encounterMethodRates,
        NamedApiResource<Location> location,
        List<Name> names,
        @JsonProperty("pokemon_encounters") List<PokemonEncounter> pokemonEncounters) {}
