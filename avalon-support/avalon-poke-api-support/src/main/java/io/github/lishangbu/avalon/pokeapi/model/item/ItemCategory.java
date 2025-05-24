package io.github.lishangbu.avalon.pokeapi.model.item;

import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/**
 * Item categories determine where items will be placed in the players bag.
 *
 * @param id 该资源的标识符
 * @param name 该资源的名称
 * @param items 属于该类别的物品列表{@link Item}
 * @param names 该物品类别在不同语言下的名称{@link Name}
 * @param pocket 该类别物品所属的口袋{@link ItemPocket}
 * @see Item
 * @see Name
 * @see ItemPocket
 * @author lishangbu
 * @since 2025/5/23
 */
public record ItemCategory(
    Integer id,
    String name,
    List<NamedApiResource> items,
    List<Name> names,
    NamedApiResource pocket) {}
