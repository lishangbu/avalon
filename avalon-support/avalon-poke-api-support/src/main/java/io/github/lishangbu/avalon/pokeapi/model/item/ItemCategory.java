package io.github.lishangbu.avalon.pokeapi.model.item;

import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/**
 * 道具类别决定了道具在玩家背包中的放置位置
 *
 * @param id 该资源的标识符
 * @param name 该资源的名称
 * @param items 属于该类别的道具列表{@link Item}
 * @param names 该道具类别在不同语言下的名称{@link Name}
 * @param pocket 该类别道具所属的口袋{@link ItemPocket}
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
