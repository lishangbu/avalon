package io.github.lishangbu.avalon.pokeapi.model.location;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.game.Generation;
import io.github.lishangbu.avalon.pokeapi.model.game.Pokedex;
import io.github.lishangbu.avalon.pokeapi.model.game.VersionGroup;
import java.util.List;

/// 区域模型
///
/// 表示宝可梦世界中的地区，通常在不同区域可以遭遇不同的宝可梦种类
///
/// @param id             资源标识符
/// @param locations      此区域包含的位置列表
/// @param name           资源名称
/// @param names          不同语言下的名称列表
/// @param mainGeneration 引入此区域的世代引用
/// @param pokedexes      记录此区域宝可梦的图鉴列表
/// @param versionGroups  可以访问此区域的版本组列表
/// @author lishangbu
/// @see Location
/// @see Name
/// @see Pokedex
/// @see VersionGroup
/// @since 2025/5/26
public record Region(
    Integer id,
    List<NamedApiResource<Location>> locations,
    String name,
    List<Name> names,
    @JsonProperty("main_generation") NamedApiResource<Generation> mainGeneration,
    List<NamedApiResource<Pokedex>> pokedexes,
    @JsonProperty("version_groups") List<NamedApiResource<VersionGroup>> versionGroups) {}
