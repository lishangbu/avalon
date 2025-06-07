package io.github.lishangbu.avalon.pokeapi.model.machine;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.game.VersionGroup;
import io.github.lishangbu.avalon.pokeapi.model.item.Item;
import io.github.lishangbu.avalon.pokeapi.model.move.Move;

/**
 * 机器是教授宝可梦招式的道具的表示。它们在不同版本之间有所不同，因此不能确定一个特定的TM或HM对应单个机器。
 *
 * @param id 资源的标识符
 * @param item 对应此机器的道具{@link Item}
 * @param move 此机器教授的招式{@link Move}
 * @param versionGroup 此机器适用的版本组{@link VersionGroup}
 * @author lishangbu
 * @see Item
 * @see Move
 * @see VersionGroup
 * @since 2025/6/7
 */
public record Machine(
    Integer id,
    NamedApiResource<Item> item,
    NamedApiResource<Move> move,
    @JsonProperty("version_group") NamedApiResource<VersionGroup> versionGroup) {}
