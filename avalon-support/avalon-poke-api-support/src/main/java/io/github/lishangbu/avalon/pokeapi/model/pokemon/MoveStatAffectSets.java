package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import java.util.List;

/// 技能对状态影响集合
///
/// @param increase 增加强化引用状态的技能列表及其影响程度
/// @param decrease 减少引用状态的技能列表及其影响程度
/// @author lishangbu
/// @see MoveStatAffect
/// @since 2025/6/8
public record MoveStatAffectSets(List<MoveStatAffect> increase, List<MoveStatAffect> decrease) {}
