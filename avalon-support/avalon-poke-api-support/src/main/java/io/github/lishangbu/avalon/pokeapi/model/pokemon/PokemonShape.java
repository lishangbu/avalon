package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/**
 * 用于在宝可梦图鉴中对宝可梦进行分类的形状。
 *
 * @param id 该资源的标识符
 * @param name 该资源的名称
 * @param awesomeNames 这种宝可梦形状的不同语言"科学"名称{@link AwesomeName}列表
 * @param names 该资源在不同语言中列出的名称{@link Name}
 * @param pokemonSpecies 具有这种形状的宝可梦种类{@link PokemonSpecies}列表
 * @author lishangbu
 * @see AwesomeName
 * @see Name
 * @see PokemonSpecies
 * @since 2025/6/8
 */
public record PokemonShape(
    Integer id,
    String name,
    @JsonProperty("awesome_names") List<AwesomeName> awesomeNames,
    List<Name> names,
    @JsonProperty("pokemon_species") List<NamedApiResource<PokemonSpecies>> pokemonSpecies) {}
