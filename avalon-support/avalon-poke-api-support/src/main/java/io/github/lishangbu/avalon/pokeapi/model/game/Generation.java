package io.github.lishangbu.avalon.pokeapi.model.game;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.Type;
import java.util.List;

/**
 * 世代是指在游戏系列历史上于特定时期推出的一组宝可梦游戏，通常以新版本、新地区和新宝可梦为标志。
 *
 * @param id 该资源的标识符
 * @param name 该资源的名称
 * @param abilities 在此世代中引入的特性列表
 * @param names 不同语言中列出的此资源名称{@link Name}
 * @param mainRegion 此世代中游历的主要区域
 * @param moves 在此世代中引入的招式列表
 * @param pokemonSpecies 在此世代中引入的宝可梦物种列表
 * @param types 在此世代中引入的属性{@link Type}列表
 * @param versionGroups 在此世代中引入的版本组{@link VersionGroup}列表
 * @see Name
 * @see Type
 * @see VersionGroup
 * @author lishangbu
 * @see Name
 * @see Type
 * @see VersionGroup
 * @since 2025/5/24
 */
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
