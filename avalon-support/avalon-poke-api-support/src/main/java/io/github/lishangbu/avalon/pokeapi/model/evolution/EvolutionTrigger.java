package io.github.lishangbu.avalon.pokeapi.model.evolution;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/**
 * Evolution triggers are the events and conditions that cause a Pokémon to evolve. Check out <a
 * href="http://bulbapedia.bulbagarden.net/wiki/Methods_of_evolution">Bulbapedia</a> for greater
 * detail.
 *
 * @param id 该资源的标识符
 * @param name 该资源的名称
 * @param names 不同语言中列出的该资源名称{@link Name}
 * @param pokemonSpecies 由此进化触发器产生的宝可梦物种列表
 * @author lishangbu
 * @see Name
 * @since 2025/5/24
 */
public record EvolutionTrigger(
    Integer id,
    String name,
    List<Name> names,
    @JsonProperty("pokemon_species") List<NamedApiResource<?>> pokemonSpecies) {}
