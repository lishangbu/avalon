package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.move.Move;

/**
 * 表示一个技能对特定状态产生的影响
 *
 * @param change 对引用状态的最大变化量
 * @param move 导致变化的技能{@link Move}
 * @author lishangbu
 * @see Move
 * @since 2025/6/8
 */
public record MoveStatAffect(Integer change, NamedApiResource<Move> move) {}
