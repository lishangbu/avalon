package io.github.lishangbu.avalon.pokeapi.model.location;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.game.Generation;
import io.github.lishangbu.avalon.pokeapi.model.game.Pokedex;
import io.github.lishangbu.avalon.pokeapi.model.game.VersionGroup;
import java.util.List;

/**
 * 区域是宝可梦世界中的一个有组织的地区。最常见的是，区域之间的主要区别在于可以在其中遇到的宝可梦种类。
 *
 * @param id 资源的标识符
 * @param locations 可以在此区域找到的位置{@link Location}列表
 * @param name 资源的名称
 * @param names 此资源在不同语言中列出的名称{@link Name}
 * @param mainGeneration 引入此区域的世代
 * @param pokedexes 记录此区域宝可梦的图鉴列表{@link Pokedex}
 * @param versionGroups 可以访问此区域的版本组列表{@link VersionGroup}
 * @author lishangbu
 * @see Location
 * @see Name
 * @see Pokedex
 * @see VersionGroup
 * @since 2025/5/26
 */
public record Region(
    Integer id,
    List<NamedApiResource<Location>> locations,
    String name,
    List<Name> names,
    @JsonProperty("main_generation") NamedApiResource<Generation> mainGeneration,
    List<NamedApiResource<Pokedex>> pokedexes,
    @JsonProperty("version_groups") List<NamedApiResource<VersionGroup>> versionGroups) {}
