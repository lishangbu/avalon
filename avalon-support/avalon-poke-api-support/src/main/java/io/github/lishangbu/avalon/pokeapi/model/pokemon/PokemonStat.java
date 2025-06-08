package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;

/**
 * 宝可梦的属性统计
 *
 * @param stat 宝可梦拥有的属性{@link Stat}
 * @param effort 宝可梦在该属性上的努力值(EV)
 * @param baseStat 属性的基础值
 * @author lishangbu
 * @see Stat
 * @since 2025/6/8
 */
public record PokemonStat(
    NamedApiResource<Stat> stat, Integer effort, @JsonProperty("base_stat") Integer baseStat) {}
