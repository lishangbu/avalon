package io.github.lishangbu.avalon.pokeapi.model.item;

import io.github.lishangbu.avalon.pokeapi.model.common.Description;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/**
 * Item attributes define particular aspects of items, e.g. "usable in battle" or "consumable"
 *
 * @param id 该资源的标识符
 * @param name 该资源的名称
 * @param items 具有该属性的{@link Item}物品列表
 * @param names 该物品属性在不同语言中列出的名称{@link Name}
 * @param descriptions 该物品属性在不同语言中列出的描述{@link Description}
 * @see Item
 * @see Name
 * @see Description
 * @author lishangbu
 * @since 2025/5/24
 */
public record ItemAttribute(
    Integer id,
    String name,
    List<NamedApiResource> items,
    List<Name> names,
    List<Description> descriptions) {}
