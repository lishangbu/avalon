package io.github.lishangbu.avalon.pokeapi.model.move;

import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;

/**
 * 招式对宝可梦属性造成的变化。
 *
 * @param change 变化的数值
 * @param stat 受影响的属性
 * @author lishangbu
 * @since 2025/6/7
 */
public record MoveStatChange(Integer change, NamedApiResource<?> stat) {}
