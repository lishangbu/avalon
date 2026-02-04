package io.github.lishangbu.avalon.pokeapi.model.location;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import java.util.List;

/// 伙伴公园区域模型
///
/// 用于分组在 Pal Park 中遭遇的宝可梦（参考 [Pal Park](https://bulbapedia.bulbagarden.net/wiki/Pal_Park)）
///
/// @param id                资源标识符
/// @param name              资源名称
/// @param names             不同语言下的名称列表
/// @param pokemonEncounters 在此区域遇到的宝可梦及其详情
/// @author lishangbu
/// @see Name
/// @see PalParkEncounterSpecies
/// @since 2025/5/26
public record PalParkArea(
    Integer id,
    String name,
    List<Name> names,
    @JsonProperty("pokemon_encounters") List<PalParkEncounterSpecies> pokemonEncounters) {}
