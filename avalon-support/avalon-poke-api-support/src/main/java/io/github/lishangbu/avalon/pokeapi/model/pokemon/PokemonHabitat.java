package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/// 栖息地模型
///
/// 表示宝可梦可能被发现的地形或区域
///
/// @param id             资源标识符
/// @param name           资源名称
/// @param names          多语言名称列表 {@link Name}
/// @param pokemonSpecies 可在此栖息地找到的宝可梦种类列表 {@link PokemonSpecies}
/// @author lishangbu
/// @see Name
/// @see PokemonSpecies
/// @since 2025/6/8
public record PokemonHabitat(
    Integer id,
    String name,
    List<Name> names,
    @JsonProperty("pokemon_species") List<NamedApiResource<PokemonSpecies>> pokemonSpecies) {}
