package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import java.util.List;

/// 宝可梦竞技状态
///
/// 表示宝可梦在竞技场中表现的不同属性（参考 [Bulbapedia](http://bulbapedia.bulbagarden.net/wiki/Pok%C3%A9athlon)）
///
/// @param id               资源标识符
/// @param name             资源名称
/// @param names            多语言名称列表
/// @param affectingNatures 正面或负面影响该竞技状态的性格集合
/// @author lishangbu
/// @see Name
/// @see NaturePokeathlonStatAffectSets
/// @since 2025/6/8
public record PokeathlonStat(
        Integer id,
        String name,
        List<Name> names,
        @JsonProperty("affecting_natures") NaturePokeathlonStatAffectSets affectingNatures) {}
