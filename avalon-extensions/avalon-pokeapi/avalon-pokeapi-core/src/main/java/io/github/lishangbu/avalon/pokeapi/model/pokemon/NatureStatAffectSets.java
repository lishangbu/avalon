package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/// 性格对竞技状态影响的集合
///
/// @param increase 增加该竞技状态的性格列表 {@link Nature}
/// @param decrease 减少该竞技状态的性格列表 {@link Nature}
/// @author lishangbu
/// @see Nature
/// @since 2025/6/8
public record NatureStatAffectSets(
        List<NamedApiResource<Nature>> increase, List<NamedApiResource<Nature>> decrease) {}
