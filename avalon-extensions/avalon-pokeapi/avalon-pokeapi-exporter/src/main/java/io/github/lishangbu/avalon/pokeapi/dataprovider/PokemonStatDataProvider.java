package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.component.PokeApiService;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.PokemonStatExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.Pokemon;
import io.github.lishangbu.avalon.pokeapi.model.resource.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.util.NamedApiResourceUtils;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/// 宝可梦能力值数据提供者
///
/// @author lishangbu
/// @since 2026/2/16
@Service
public class PokemonStatDataProvider implements PokeApiDataProvider<PokemonStatExcelDTO> {
  @Autowired protected PokeApiService pokeApiService;

  @Override
  public List<PokemonStatExcelDTO> fetch(
      PokeDataTypeEnum typeEnum, Class<PokemonStatExcelDTO> type) {
    NamedAPIResourceList namedAPIResourceList =
        pokeApiService.listNamedAPIResources(PokeDataTypeEnum.POKEMON);
    List<PokemonStatExcelDTO> result = new ArrayList<>();
    namedAPIResourceList
        .results()
        .forEach(
            namedApiResource -> {
              Pokemon pokemon =
                  (Pokemon)
                      pokeApiService.getEntityFromUri(
                          PokeDataTypeEnum.POKEMON, NamedApiResourceUtils.getId(namedApiResource));
              result.addAll(
                  pokemon.stats().stream()
                      .map(
                          pokemonStat -> {
                            PokemonStatExcelDTO tmp = new PokemonStatExcelDTO();
                            tmp.setStatId(NamedApiResourceUtils.getId(pokemonStat.stat()));
                            tmp.setPokemonId(pokemon.id());
                            tmp.setBaseStat(pokemonStat.baseStat());
                            tmp.setEffort(pokemonStat.effort());
                            return tmp;
                          })
                      .toList());
            });
    return result;
  }
}
