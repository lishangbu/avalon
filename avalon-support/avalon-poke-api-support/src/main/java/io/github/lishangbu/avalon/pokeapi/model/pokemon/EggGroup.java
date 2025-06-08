package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/**
 * 蛋组是决定哪些宝可梦能够互相繁殖的分类。宝可梦可能属于一个或两个蛋组。查看 <a
 * href="http://bulbapedia.bulbagarden.net/wiki/Egg_Group">Bulbapedia</a> 获取更多详情。
 *
 * @param id 该资源的标识符
 * @param name 该资源的名称
 * @param names 该资源在不同语言中列出的名称{@link Name}
 * @param pokemonSpecies 作为此蛋组成员的所有宝可梦种类{@link PokemonSpecies}列表
 * @author lishangbu
 * @see Name
 * @see PokemonSpecies
 * @since 2025/6/8
 */
public record EggGroup(
    Integer id,
    String name,
    List<Name> names,
    @JsonProperty("pokemon_species") List<NamedApiResource<PokemonSpecies>> pokemonSpecies) {}
