package io.github.lishangbu.avalon.pokeapi.model.berry;

import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;

/// 风味与树果映射模型
///
/// @param potency 该风味对树果的影响力
/// @param berry   具有该风味的树果引用
/// @author lishangbu
/// @see Berry
/// @since 2025/5/21
public record FlavorBerryMap(Integer potency, NamedApiResource berry) {}
