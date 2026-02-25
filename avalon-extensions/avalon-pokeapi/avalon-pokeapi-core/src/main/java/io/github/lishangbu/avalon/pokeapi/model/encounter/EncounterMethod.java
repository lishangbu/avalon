package io.github.lishangbu.avalon.pokeapi.model.encounter;

import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import java.util.List;

/// 玩家在野外遇到宝可梦的方式，例如在高草中行走，
/// 详情参考[Bulbapedia](http://bulbapedia.bulbagarden.net/wiki/Category:Berries_by_firmness)
///
/// @param id    该资源的唯一标识符
/// @param name  该资源的名称
/// @param order 用于排序的推荐值
/// @param names 该资源在不同语言下的名称 {@link Name}
/// @author lishangbu
/// @see Name
/// @since 2025/5/23
public record EncounterMethod(Integer id, String name, Integer order, List<Name> names) {}
