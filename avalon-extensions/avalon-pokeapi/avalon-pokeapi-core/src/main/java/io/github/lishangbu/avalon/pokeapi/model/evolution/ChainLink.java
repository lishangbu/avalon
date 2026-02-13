package io.github.lishangbu.avalon.pokeapi.model.evolution;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.PokemonSpecies;
import java.util.List;

/// 进化链中的一个环节，描述宝可梦的进化细节
///
/// @param isBaby           此链接是否适用于幼年宝可梦
/// @param species          该环节的宝可梦物种引用
/// @param evolutionDetails 有关进化的具体细节列表
/// @param evolvesTo        后续可能的进化链环节列表
/// @author lishangbu
/// @see EvolutionDetail
/// @see ChainLink
/// @since 2025/5/24
public record ChainLink(
    @JsonProperty("is_baby") Boolean isBaby,
    NamedApiResource<PokemonSpecies> species,
    @JsonProperty("evolution_details") List<EvolutionDetail> evolutionDetails,
    @JsonProperty("evolves_to") List<ChainLink> evolvesTo) {}
