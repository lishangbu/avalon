package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/**
 * 栖息地通常是指宝可梦可以被发现的不同地形，但也可以是指定给稀有或传说宝可梦的区域。
 *
 * @param id 该资源的标识符
 * @param name 该资源的名称
 * @param names 该资源在不同语言中列出的名称{@link Name}
 * @param pokemonSpecies 可以在此栖息地中找到的宝可梦种类列表{@link PokemonSpecies}
 * @author lishangbu
 * @see Name
 * @see PokemonSpecies
 * @since 2025/6/8
 */
public record PokemonHabitat(
    Integer id,
    String name,
    List<Name> names,
    @JsonProperty("pokemon_species") List<NamedApiResource<PokemonSpecies>> pokemonSpecies) {}
