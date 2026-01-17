package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Description;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/// 成长速率模型
///
/// 表示宝可梦通过经验获得等级的速度，详情参考 [Bulbapedia](http://bulbapedia.bulbagarden.net/wiki/Experience)
///
/// @param id             资源标识符
/// @param name           资源名称
/// @param formula        用于计算经验到等级的公式
/// @param descriptions   多语言描述列表
/// @param levels         达到各等级所需经验的列表
/// @param pokemonSpecies 按此成长速率获得等级的宝可梦种类列表
/// @author lishangbu
/// @see Description
/// @see GrowthRateExperienceLevel
/// @see PokemonSpecies
/// @since 2025/6/8
public record GrowthRate(
    Integer id,
    String name,
    String formula,
    List<Description> descriptions,
    List<GrowthRateExperienceLevel> levels,
    @JsonProperty("pokemon_species") List<NamedApiResource<PokemonSpecies>> pokemonSpecies) {}
