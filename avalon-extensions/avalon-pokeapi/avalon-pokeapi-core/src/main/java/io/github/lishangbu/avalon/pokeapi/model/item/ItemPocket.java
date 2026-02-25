package io.github.lishangbu.avalon.pokeapi.model.item;

import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/// 玩家背包中用于按类别存储道具的口袋
///
/// @param id         该资源的标识符
/// @param name       该资源的名称
/// @param categories 与此道具口袋相关的道具分类列表
/// @param names      该道具口袋在不同语言中列出的名称
/// @see ItemCategory
/// @see Name
/// @since 2025/5/24
public record ItemPocket(
        Integer id,
        String name,
        List<NamedApiResource<ItemCategory>> categories,
        List<Name> names) {}
