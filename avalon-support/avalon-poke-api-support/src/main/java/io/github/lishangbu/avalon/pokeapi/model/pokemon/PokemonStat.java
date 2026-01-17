package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;

/// 宝可梦属性统计模型
///
/// 表示宝可梦在特定属性上的基础值、努力值与属性引用
///
/// @param stat     宝可梦的属性引用 {@link Stat}
/// @param effort   努力值 (EV)
/// @param baseStat 属性的基础值
/// @author lishangbu
/// @see Stat
/// @since 2025/6/8
public record PokemonStat(
    NamedApiResource<Stat> stat, Integer effort, @JsonProperty("base_stat") Integer baseStat) {}
