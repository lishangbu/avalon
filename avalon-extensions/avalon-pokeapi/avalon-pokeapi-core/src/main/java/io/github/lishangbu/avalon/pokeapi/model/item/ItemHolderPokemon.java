package io.github.lishangbu.avalon.pokeapi.model.item;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/// 持有道具的宝可梦信息
///
/// @param pokemon        持有该道具的宝可梦引用
/// @param versionDetails 宝可梦持有该道具的版本详细信息列表
/// @author lishangbu
/// @see ItemHolderPokemonVersionDetail
/// @since 2025/5/24
public record ItemHolderPokemon(
        NamedApiResource<?> pokemon,
        @JsonProperty("version_details") List<ItemHolderPokemonVersionDetail> versionDetails) {}
