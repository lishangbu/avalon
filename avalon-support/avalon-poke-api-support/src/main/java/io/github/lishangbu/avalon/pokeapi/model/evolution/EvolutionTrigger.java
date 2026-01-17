package io.github.lishangbu.avalon.pokeapi.model.evolution;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/// 进化触发器模型
///
/// 进化触发器表示导致宝可梦进化的事件与条件（参考
// [Bulbapedia](http://bulbapedia.bulbagarden.net/wiki/Methods_of_evolution)）

///
/// @param id             资源标识符
/// @param name           资源名称
/// @param names          不同语言下的名称列表
/// @param pokemonSpecies 由此触发器产生的宝可梦物种列表
/// @author lishangbu
/// @see Name
/// @since 2025/5/24
public record EvolutionTrigger(
    Integer id,
    String name,
    List<Name> names,
    @JsonProperty("pokemon_species") List<NamedApiResource<?>> pokemonSpecies) {}
