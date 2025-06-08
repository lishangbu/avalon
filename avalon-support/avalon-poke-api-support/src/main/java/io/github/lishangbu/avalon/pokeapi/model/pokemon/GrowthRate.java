package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Description;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/**
 * 成长速率是宝可梦通过经验获得等级的速度。查看 <a href="http://bulbapedia.bulbagarden.net/wiki/Experience">Bulbapedia</a>
 * 获取更多详情。
 *
 * @param id 该资源的标识符
 * @param name 该资源的名称
 * @param formula 用于计算宝可梦种类获得等级的速率的公式
 * @param descriptions 不同语言中列出的此特性的描述{@link Description}
 * @param levels 基于此成长速率，达到各个等级所需经验值{@link GrowthRateExperienceLevel}的列表
 * @param pokemonSpecies 按照此成长速率获得等级的宝可梦种类{@link PokemonSpecies}列表
 * @author lishangbu
 * @see Description
 * @see GrowthRateExperienceLevel
 * @see PokemonSpecies
 * @since 2025/6/8
 */
public record GrowthRate(
    Integer id,
    String name,
    String formula,
    List<Description> descriptions,
    List<GrowthRateExperienceLevel> levels,
    @JsonProperty("pokemon_species") List<NamedApiResource<PokemonSpecies>> pokemonSpecies) {}
