package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;

/// 宝可梦形态属性模型
///
/// 表示形态在属性列表中的位置与对应属性引用
///
/// @param slot 属性列表中的位置
/// @param type 形态拥有的属性引用
/// @author lishangbu
/// @see Type
/// @since 2025/6/8
public record PokemonFormType(Integer slot, NamedApiResource<Type> type) {}
