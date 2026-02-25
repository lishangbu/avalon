package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;

/// 宝可梦的特性模型
///
/// 表示宝可梦拥有的特性及其在种类中的位置
///
/// @param isHidden 是否为隐藏特性
/// @param slot     特性的位置索引
/// @param ability  特性引用
/// @author lishangbu
/// @see Ability
/// @since 2025/6/8
public record PokemonAbility(
        @JsonProperty("is_hidden") Boolean isHidden,
        Integer slot,
        NamedApiResource<Ability> ability) {}
