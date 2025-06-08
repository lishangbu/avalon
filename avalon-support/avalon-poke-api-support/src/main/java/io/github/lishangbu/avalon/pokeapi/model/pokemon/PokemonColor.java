package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/**
 * 用于在宝可梦图鉴中对宝可梦进行分类的颜色。宝可梦图鉴中列出的颜色通常是每个宝可梦身体上最明显或覆盖最多的颜色。没有橙色类别；主要为橙色的宝可梦被归类为红色或棕色。
 *
 * @param id 该资源的标识符
 * @param name 该资源的名称
 * @param names 该资源在不同语言中列出的名称{@link Name}
 * @param pokemonSpecies 具有这种颜色的宝可梦种类{@link PokemonSpecies}列表
 * @author lishangbu
 * @see Name
 * @see PokemonSpecies
 * @since 2025/6/8
 */
public record PokemonColor(
    Integer id,
    String name,
    List<Name> names,
    @JsonProperty("pokemon_species") List<NamedApiResource<PokemonSpecies>> pokemonSpecies) {}
