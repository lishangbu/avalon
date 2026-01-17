package io.github.lishangbu.avalon.pokeapi.model.game;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/// 版本组模型
///
/// 将高度相似的游戏版本分组，包含引入的图鉴、可访问的区域与相关版本列表
///
/// @param id               资源标识符
/// @param name             资源名称
/// @param order            排序顺序
/// @param generation       引入该版本组的世代引用
/// @param moveLearnMethods 该组中学习招式的方法列表
/// @param pokedexes        引入的图鉴列表
/// @param regions          可访问的区域列表
/// @param versions         此版本组下的版本列表
/// @author lishangbu
/// @see Generation
/// @see Pokedex
/// @see Version
/// @since 2025/5/24
public record VersionGroup(
    Integer id,
    String name,
    Integer order,
    NamedApiResource<Generation> generation,
    @JsonProperty("move_learn_methods") List<NamedApiResource<?>> moveLearnMethods,
    List<NamedApiResource<Pokedex>> pokedexes,
    List<NamedApiResource<?>> regions,
    List<NamedApiResource<Version>> versions) {}
