package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import java.util.List;

/**
 * MoveStatAffectSets - 技能对状态影响的集合
 *
 * @param increase 增加引用状态的技能列表及其影响程度{@link MoveStatAffect}
 * @param decrease 减少引用状态的技能列表及其影响程度{@link MoveStatAffect}
 * @author lishangbu
 * @since 2025/6/8
 * @see MoveStatAffect
 */
public record MoveStatAffectSets(List<MoveStatAffect> increase, List<MoveStatAffect> decrease) {}
