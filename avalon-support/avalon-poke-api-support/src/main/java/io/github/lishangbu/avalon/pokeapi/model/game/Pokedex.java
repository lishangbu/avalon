package io.github.lishangbu.avalon.pokeapi.model.game;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Description;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/**
 * 图鉴是一种便携式电子百科设备；能够记录和保存某个特定地区中各种宝可梦的信息，国家图鉴和一些与地区部分相关的较小图鉴除外。 更多详情请参见 <a
 * href="https://bulbapedia.bulbagarden.net/wiki/Pokedex">Bulbapedia</a>。
 *
 * @param id 该资源的标识符
 * @param name 该资源的名称
 * @param isMainSeries 这个图鉴是否起源于主系列游戏
 * @param descriptions 不同语言中列出的该资源描述{@link Description}
 * @param names 不同语言中列出的该资源名称{@link Name}
 * @param pokemonEntries 此图鉴中编入目录的宝可梦及其索引{@link PokemonEntry}列表
 * @param region 此图鉴为哪个区域的宝可梦编目
 * @param versionGroups 与此图鉴相关的版本组列表{@link VersionGroup}
 * @see Description
 * @see Name
 * @see PokemonEntry
 * @see VersionGroup
 * @author lishangbu
 * @since 2025/5/24
 */
public record Pokedex(
    Integer id,
    String name,
    @JsonProperty("is_main_series") Boolean isMainSeries,
    List<Description> descriptions,
    List<Name> names,
    @JsonProperty("pokemon_entries") List<PokemonEntry> pokemonEntries,
    NamedApiResource<?> region,
    @JsonProperty("version_groups") List<NamedApiResource<VersionGroup>> versionGroups) {}
