package io.github.lishangbu.avalon.pokeapi.model.location;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;

/**
 * PalParkEncounterSpecies
 *
 * @param baseScore 当这个宝可梦在帕尔公园被捕获时给予玩家的基础分数
 * @param rate 在此帕尔公园区域遇到此宝可梦的基本概率
 * @param pokemonSpecies 被遇到的宝可梦物种
 * @author lishangbu
 * @since 2025/5/26
 */
public record PalParkEncounterSpecies(
    @JsonProperty("base_score") Integer baseScore,
    Integer rate,
    @JsonProperty("pokemon_species") NamedApiResource<?> pokemonSpecies) {}
