package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.PokemonHabitatExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.PokemonHabitat;
import org.springframework.stereotype.Service;

/// 宝可梦栖息地数据提供者
///
/// @author lishangbu
/// @since 2026/2/10
@Service
public class PokemonHabitatDataProvider
    extends AbstractPokeApiDataProvider<PokemonHabitat, PokemonHabitatExcelDTO> {

  @Override
  public PokemonHabitatExcelDTO convert(PokemonHabitat pokemonHabitat) {
    PokemonHabitatExcelDTO result = new PokemonHabitatExcelDTO();
    result.setId(pokemonHabitat.id());
    result.setInternalName(pokemonHabitat.name());
    result.setName(resolveLocalizedName(pokemonHabitat.names(), pokemonHabitat.name()));
    return result;
  }
}
