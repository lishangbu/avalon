package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/// 蛋组模型
///
/// 决定哪些宝可梦能够互相繁殖，详情参考 [Bulbapedia](http://bulbapedia.bulbagarden.net/wiki/Egg_Group)
///
/// @param id             资源标识符
/// @param name           资源名称
/// @param names          多语言名称列表 {@link Name}
/// @param pokemonSpecies 作为此蛋组成员的宝可梦种类列表 {@link PokemonSpecies}
/// @author lishangbu
/// @see Name
/// @see PokemonSpecies
/// @since 2025/6/8
public record EggGroup(
        Integer id,
        String name,
        List<Name> names,
        @JsonProperty("pokemon_species") List<NamedApiResource<PokemonSpecies>> pokemonSpecies) {}
