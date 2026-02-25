package io.github.lishangbu.avalon.pokeapi.model.machine;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.game.VersionGroup;
import io.github.lishangbu.avalon.pokeapi.model.item.Item;
import io.github.lishangbu.avalon.pokeapi.model.move.Move;

/// 机器（Machine）模型
///
/// 表示用于教授宝可梦招式的道具及其在特定版本组中的适用性
///
/// @param id           资源标识符
/// @param item         对应的道具引用
/// @param move         此机器教授的招式引用
/// @param versionGroup 适用的版本组引用
/// @author lishangbu
/// @see Item
/// @see Move
/// @see VersionGroup
/// @since 2025/6/7
public record Machine(
        Integer id,
        NamedApiResource<Item> item,
        NamedApiResource<Move> move,
        @JsonProperty("version_group") NamedApiResource<VersionGroup> versionGroup) {}
