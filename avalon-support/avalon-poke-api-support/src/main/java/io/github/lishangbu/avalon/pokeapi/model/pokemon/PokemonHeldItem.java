package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.item.Item;
import java.util.List;

/// 宝可梦持有道具模型
///
/// 表示宝可梦可能持有的道具及其在不同版本中的详细信息
///
/// @param item           道具引用
/// @param versionDetails 在不同版本中持有该道具的详情列表
/// @author lishangbu
/// @see Item
/// @see PokemonHeldItemVersion
/// @since 2025/6/8
public record PokemonHeldItem(
    NamedApiResource<Item> item,
    @JsonProperty("version_details") List<PokemonHeldItemVersion> versionDetails) {}
