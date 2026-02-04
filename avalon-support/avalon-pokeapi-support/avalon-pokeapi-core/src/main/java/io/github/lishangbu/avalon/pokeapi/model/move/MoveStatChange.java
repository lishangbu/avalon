package io.github.lishangbu.avalon.pokeapi.model.move;

import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;

/// 招式对属性的影响模型
///
/// 表示招式对目标宝可梦某一属性造成的变化幅度
///
/// @param change 变化的数值
/// @param stat   受影响的属性引用
/// @author lishangbu
/// @since 2025/6/7
public record MoveStatChange(Integer change, NamedApiResource<?> stat) {}
