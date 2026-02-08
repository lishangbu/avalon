package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/// 宝可梦颜色模型
///
/// 用于在图鉴中根据宝可梦的主色对其进行分类
///
/// @param id             资源标识符
/// @param name           资源名称
/// @param names          多语言名称列表
/// @param pokemonSpecies 拥有此颜色的宝可梦种类列表
/// @author lishangbu
/// @see Name
/// @see PokemonSpecies
/// @since 2025/6/8
public record PokemonColor(
    Integer id,
    String name,
    List<Name> names,
    @JsonProperty("pokemon_species") List<NamedApiResource<PokemonSpecies>> pokemonSpecies) {}
