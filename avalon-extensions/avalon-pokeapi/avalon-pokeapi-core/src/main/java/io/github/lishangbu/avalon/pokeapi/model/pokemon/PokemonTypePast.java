package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.game.Generation;
import java.util.List;

/// 历史属性模型
///
/// 表示宝可梦在过去世代中拥有的属性列表
///
/// @param generation 该记录所述的最后世代引用
/// @param types      在该世代及之前拥有的属性列表
/// @author lishangbu
/// @since 2025/6/8
public record PokemonTypePast(NamedApiResource<Generation> generation, List<PokemonType> types) {}
