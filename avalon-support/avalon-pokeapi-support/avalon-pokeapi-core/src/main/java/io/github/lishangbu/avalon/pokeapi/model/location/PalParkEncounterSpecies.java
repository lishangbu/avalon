package io.github.lishangbu.avalon.pokeapi.model.location;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;

/// 帕尔公园遭遇物种模型
///
/// @param baseScore      捕获此宝可梦时给予的基础分数
/// @param rate           在该公园区域遭遇该宝可梦的基本概率
/// @param pokemonSpecies 被遇到的宝可梦物种引用
/// @author lishangbu
/// @since 2025/5/26
public record PalParkEncounterSpecies(
    @JsonProperty("base_score") Integer baseScore,
    Integer rate,
    @JsonProperty("pokemon_species") NamedApiResource<?> pokemonSpecies) {}
