package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import java.util.List;

/// 性格对宝可梦竞技状态影响的集合
///
/// @param increase 增加强化竞技状态的性格列表及其影响程度
/// @param decrease 减少竞技状态的性格列表及其影响程度
/// @author lishangbu
/// @see NaturePokeathlonStatAffect
/// @since 2025/6/8
public record NaturePokeathlonStatAffectSets(
        List<NaturePokeathlonStatAffect> increase, List<NaturePokeathlonStatAffect> decrease) {}
