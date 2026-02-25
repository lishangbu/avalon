package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.PokemonSpeciesExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.PokemonSpecies;
import io.github.lishangbu.avalon.pokeapi.util.ApiResourceUtils;
import io.github.lishangbu.avalon.pokeapi.util.NamedApiResourceUtils;
import org.springframework.stereotype.Service;

/// 宝可梦种类数据提供者
///
/// @author lishangbu
/// @since 2026/2/10
@Service
public class PokemonSpeciesDataProvider
        extends AbstractPokeApiDataProvider<PokemonSpecies, PokemonSpeciesExcelDTO> {

    @Override
    public PokemonSpeciesExcelDTO convert(PokemonSpecies pokemonSpecies) {
        PokemonSpeciesExcelDTO result = new PokemonSpeciesExcelDTO();
        result.setId(pokemonSpecies.id());
        result.setInternalName(pokemonSpecies.name());
        result.setName(resolveLocalizedName(pokemonSpecies.names(), pokemonSpecies.name()));
        result.setSortingOrder(pokemonSpecies.order());
        result.setGenderRate(pokemonSpecies.genderRate());
        result.setCaptureRate(pokemonSpecies.captureRate());
        result.setBaseHappiness(pokemonSpecies.baseHappiness());
        result.setIsBaby(pokemonSpecies.isBaby());
        result.setIsLegendary(pokemonSpecies.isLegendary());
        result.setIsMythical(pokemonSpecies.isMythical());
        result.setHatchCounter(pokemonSpecies.hatchCounter());
        result.setHasGenderDifferences(pokemonSpecies.hasGenderDifferences());
        result.setFormsSwitchable(pokemonSpecies.formsSwitchable());
        result.setGrowthRateId(NamedApiResourceUtils.getId(pokemonSpecies.growthRate()));
        result.setPokemonColorId(NamedApiResourceUtils.getId(pokemonSpecies.color()));
        result.setPokemonShapeId(NamedApiResourceUtils.getId(pokemonSpecies.shape()));
        result.setEvolvesFromSpeciesId(
                NamedApiResourceUtils.getId(pokemonSpecies.evolvesFromSpecies()));
        result.setEvolutionChainId(ApiResourceUtils.getId(pokemonSpecies.evolutionChain()));
        result.setPokemonHabitatId(NamedApiResourceUtils.getId(pokemonSpecies.habitat()));
        return result;
    }
}
