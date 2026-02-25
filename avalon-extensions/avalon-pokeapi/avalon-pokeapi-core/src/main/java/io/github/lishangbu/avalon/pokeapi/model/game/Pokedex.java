package io.github.lishangbu.avalon.pokeapi.model.game;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Description;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/// 图鉴（Pokedex）模型
///
/// 表示某一地区或系列的宝可梦目录信息
///
/// @param id             资源标识符
/// @param name           资源名称
/// @param isMainSeries   是否为主系列图鉴
/// @param descriptions   不同语言的资源描述
/// @param names          不同语言的资源名称
/// @param pokemonEntries 收录的宝可梦及其索引列表
/// @param region         图鉴所属区域引用
/// @param versionGroups  与该图鉴相关的版本组列表
/// @author lishangbu
/// @see Description
/// @see Name
/// @see PokemonEntry
/// @see VersionGroup
/// @since 2025/5/24
public record Pokedex(
        Integer id,
        String name,
        @JsonProperty("is_main_series") Boolean isMainSeries,
        List<Description> descriptions,
        List<Name> names,
        @JsonProperty("pokemon_entries") List<PokemonEntry> pokemonEntries,
        NamedApiResource<?> region,
        @JsonProperty("version_groups") List<NamedApiResource<VersionGroup>> versionGroups) {}
