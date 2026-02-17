package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.PokemonExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.Pokemon;
import io.github.lishangbu.avalon.pokeapi.util.NamedApiResourceUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/// 宝可梦数据提供者
///
/// @author lishangbu
/// @since 2026/2/10
@Service
@Slf4j
public class PokemonDataProvider extends AbstractPokeApiDataProvider<Pokemon, PokemonExcelDTO> {

  @Override
  public PokemonExcelDTO convert(Pokemon pokemon) {
    PokemonExcelDTO result = new PokemonExcelDTO();
    result.setId(pokemon.id());
    result.setInternalName(pokemon.name());
    result.setName(pokemon.name());
    result.setBaseExperience(pokemon.baseExperience());
    result.setHeight(pokemon.height());
    result.setIsDefault(pokemon.isDefault());
    result.setSortingOrder(pokemon.order());
    result.setWeight(pokemon.weight());
    result.setPokemonSpeciesId(NamedApiResourceUtils.getId(pokemon.species()));
    if (pokemon.forms() != null && !pokemon.forms().isEmpty()) {
      if (pokemon.forms().size() > 1) {
        log.warn("Pokemon [{}](id:[{}]) has multiple forms", pokemon.name(), pokemon.id());
      }
    }
    return result;
  }
}
