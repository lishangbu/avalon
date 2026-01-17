package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.game.Generation;
import java.util.List;

/// 历史特性模型
///
/// 表示宝可梦在过去世代中拥有的特性列表
///
/// @param generation 该记录所述的最后世代引用
/// @param abilities  在该世代及之前拥有的特性列表（若为 null 表示为空）
/// @author lishangbu
/// @see Generation
/// @see PokemonAbility
/// @since 2025/6/8
public record PokemonAbilityPast(
    NamedApiResource<Generation> generation, List<PokemonAbility> abilities) {}
