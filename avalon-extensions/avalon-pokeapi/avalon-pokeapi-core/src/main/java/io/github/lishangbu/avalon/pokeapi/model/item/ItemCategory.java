package io.github.lishangbu.avalon.pokeapi.model.item;

import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/// 道具类别模型
///
/// 决定道具在玩家背包中的放置口袋
///
/// @param id     资源标识符
/// @param name   资源名称
/// @param items  属于该类别的道具引用列表
/// @param names  此类别在不同语言下的名称列表
/// @param pocket 所属口袋引用
/// @author lishangbu
/// @see Item
/// @see Name
/// @see ItemPocket
/// @since 2025/5/23
public record ItemCategory(
    Integer id,
    String name,
    List<NamedApiResource> items,
    List<Name> names,
    NamedApiResource pocket) {}
