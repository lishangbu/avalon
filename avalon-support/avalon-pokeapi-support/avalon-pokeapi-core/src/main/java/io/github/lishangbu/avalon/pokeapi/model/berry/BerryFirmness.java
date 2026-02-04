package io.github.lishangbu.avalon.pokeapi.model.berry;

import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/// 树果硬度模型
///
/// 树果可以是柔软的，也可以是坚硬的，详情参考
// [Bulbapedia](https://bulbapedia.bulbagarden.net/wiki/Category:Berries_by_firmness)

/// @param id      资源标识符
/// @param name    资源名称
/// @param berries 具有该硬度的树果列表
/// @param names   多语言名称列表
/// @author lishangbu
/// @see Berry
/// @see Name
/// @since 2025/5/21
public record BerryFirmness(
    Integer id, String name, List<NamedApiResource> berries, List<Name> names) {}
