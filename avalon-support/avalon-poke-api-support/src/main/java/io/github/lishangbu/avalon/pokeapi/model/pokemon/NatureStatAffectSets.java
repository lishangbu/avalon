package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/**
 * NatureStatAffectSets - 性格对竞技状态影响的集合
 *
 * @param increase 增加引用竞技状态的性格{@link Nature}列表
 * @param decrease 减少引用竞技状态的性格{@link Nature}列表
 * @author lishangbu
 * @see Nature
 * @since 2025/6/8
 */
public record NatureStatAffectSets(
    List<NamedApiResource<Nature>> increase, List<NamedApiResource<Nature>> decrease) {}
