package io.github.lishangbu.avalon.pokeapi.model.game;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/**
 * 版本组对高度相似的游戏版本进行分类。
 *
 * @param id 该资源的标识符
 * @param name 该资源的名称
 * @param order 用于排序的顺序。几乎按发行日期排序，但类似的版本会被归为一组
 * @param generation 引入该版本组的世代
 * @param moveLearnMethods 这个版本组中宝可梦可以学习招式的方法列表
 * @param pokedexes 在该版本组中引入的图鉴列表
 * @param regions 在该版本组中可以访问的区域列表
 * @param versions 该版本组拥有的版本列表
 * @author lishangbu
 * @since 2025/5/24
 */
public record VersionGroup(
    Integer id,
    String name,
    Integer order,
    NamedApiResource<Generation> generation,
    @JsonProperty("move_learn_methods") List<NamedApiResource<?>> moveLearnMethods,
    List<NamedApiResource<Pokedex>> pokedexes,
    List<NamedApiResource<?>> regions,
    List<NamedApiResource<Version>> versions) {}
