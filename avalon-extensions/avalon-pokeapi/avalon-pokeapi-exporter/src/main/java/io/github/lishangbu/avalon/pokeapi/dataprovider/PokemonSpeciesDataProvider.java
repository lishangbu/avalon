package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.PokemonSpeciesExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.PokemonSpecies;
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
    result.setColorId(NamedApiResourceUtils.getId(pokemonSpecies.color()));
    result.setShapeId(NamedApiResourceUtils.getId(pokemonSpecies.shape()));
    result.setEvolvesFromSpeciesId(
        NamedApiResourceUtils.getId(pokemonSpecies.evolvesFromSpecies()));
    result.setEvolutionChainId(extractIdFromUrl(pokemonSpecies.evolutionChain().url()));
    result.setHabitatId(NamedApiResourceUtils.getId(pokemonSpecies.habitat()));
    result.setGenerationId(NamedApiResourceUtils.getId(pokemonSpecies.generation()));
    return result;
  }

  private Integer extractIdFromUrl(String url) {
    if (url == null) return null;
    String[] parts = url.split("/");
    for (int i = parts.length - 1; i >= 0; i--) {
      if (!parts[i].isEmpty()) {
        try {
          return Integer.parseInt(parts[i]);
        } catch (NumberFormatException e) {
          // continue
        }
      }
    }
    return null;
  }
}
