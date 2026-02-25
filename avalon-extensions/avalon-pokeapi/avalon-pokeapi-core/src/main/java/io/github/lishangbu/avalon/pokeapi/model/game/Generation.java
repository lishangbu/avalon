package io.github.lishangbu.avalon.pokeapi.model.game;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.Type;
import java.util.List;

/// 世代模型
///
/// 表示在游戏系列历史上推出的一组宝可梦游戏，通常包含新版本、新地区和新增宝可梦
///
/// @param id             资源标识符
/// @param name           资源名称
/// @param abilities      本世代引入的特性列表
/// @param names          多语言名称列表
/// @param mainRegion     本世代的主要区域引用
/// @param moves          本世代引入的招式列表
/// @param pokemonSpecies 本世代引入的宝可梦物种列表
/// @param types          本世代引入的属性列表
/// @param versionGroups  本世代引入的版本组列表
/// @author lishangbu
/// @see Name
/// @see Type
/// @see VersionGroup
/// @since 2025/5/24
public record Generation(
        Integer id,
        String name,
        List<NamedApiResource<?>> abilities,
        List<Name> names,
        @JsonProperty("main_region") NamedApiResource<?> mainRegion,
        List<NamedApiResource<?>> moves,
        @JsonProperty("pokemon_species") List<NamedApiResource<?>> pokemonSpecies,
        List<NamedApiResource<Type>> types,
        @JsonProperty("version_groups") List<NamedApiResource<VersionGroup>> versionGroups) {}
