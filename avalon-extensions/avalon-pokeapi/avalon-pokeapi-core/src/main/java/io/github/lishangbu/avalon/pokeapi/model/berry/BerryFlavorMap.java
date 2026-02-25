package io.github.lishangbu.avalon.pokeapi.model.berry;

import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;

/// 风味与树果映射模型
///
/// @param potency 该风味对树果的影响力
/// @param flavor  具有该风味的风味引用
/// @author lishangbu
/// @see Berry
/// @since 2026/2/8
public record BerryFlavorMap(Integer potency, NamedApiResource flavor) {}
