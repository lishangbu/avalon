package io.github.lishangbu.avalon.pokeapi.model.item;

import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/**
 * Pockets within the players bag used for storing items by category.
 *
 * @param id 该资源的标识符
 * @param name 该资源的名称
 * @param categories 与此物品口袋相关的物品分类列表{@link ItemCategory}
 * @param names 该物品口袋在不同语言中列出的名称{@link Name}
 * @author lishangbu
 * @see ItemCategory
 * @see Name
 * @since 2025/5/24
 */
public record ItemPocket(
    Integer id, String name, List<NamedApiResource<ItemCategory>> categories, List<Name> names) {}
