package io.github.lishangbu.avalon.pokeapi.model.item;

import io.github.lishangbu.avalon.pokeapi.model.common.Description;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/// 物品属性模型
///
/// 定义物品的特定属性，例如是否可在战斗中使用或可消耗
///
/// @param id           资源标识符
/// @param name         资源名称
/// @param items        拥有该属性的道具引用列表
/// @param names        该属性在不同语言下的名称列表
/// @param descriptions 该属性在不同语言下的描述列表
/// @author lishangbu
/// @see Item
/// @see Name
/// @see Description
/// @since 2025/5/24
public record ItemAttribute(
        Integer id,
        String name,
        List<NamedApiResource> items,
        List<Name> names,
        List<Description> descriptions) {}
