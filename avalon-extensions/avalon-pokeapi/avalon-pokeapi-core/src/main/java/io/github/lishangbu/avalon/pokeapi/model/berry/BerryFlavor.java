package io.github.lishangbu.avalon.pokeapi.model.berry;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.contest.ContestType;
import java.util.List;

/// 风味模型
///
/// 风味决定宝可梦根据性格食用树果时的效果，详情参考 [Bulbapedia](http://bulbapedia.bulbagarden.net/wiki/Flavor)
///
/// @param id          资源标识符
/// @param name        资源名称
/// @param berries     具有该风味的树果列表
/// @param contestType 与该风味相关的对战类型引用
/// @param names       多语言名称
/// @author lishangbu
/// @see FlavorBerryMap
/// @see ContestType
/// @see Name
/// @since 2025/5/21
public record BerryFlavor(
    Integer id,
    String name,
    List<FlavorBerryMap> berries,
    @JsonProperty("contest_type") NamedApiResource contestType,
    List<Name> names) {}
