package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.component.PokeApiService;
import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.PokemonAbilityExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.Pokemon;
import io.github.lishangbu.avalon.pokeapi.model.resource.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.util.NamedApiResourceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/// 属性相互克制关系数据提供者
///
/// @author lishangbu
/// @since 2026/2/10
@Service
public class PokemonAbilityDataProvider implements PokeApiDataProvider<PokemonAbilityExcelDTO> {
  @Autowired
  protected PokeApiService pokeApiService;

  @Override
  public List<PokemonAbilityExcelDTO> fetch(PokeDataTypeEnum typeEnum, Class<PokemonAbilityExcelDTO> type) {
    NamedAPIResourceList namedAPIResourceList = pokeApiService.listNamedAPIResources(PokeDataTypeEnum.POKEMON);
    List<PokemonAbilityExcelDTO> result = new ArrayList<>();
    namedAPIResourceList.results().forEach(namedApiResource -> {
      Pokemon pokemon = (Pokemon) pokeApiService.getEntityFromUri(PokeDataTypeEnum.POKEMON, NamedApiResourceUtils.getId(namedApiResource));
      result.addAll(
        pokemon.abilities().stream().map(pokemonAbility -> {
          PokemonAbilityExcelDTO tmp = new PokemonAbilityExcelDTO();
          tmp.setAbilityId(NamedApiResourceUtils.getId(pokemonAbility.ability()));
          tmp.setPokemonId(pokemon.id());
          tmp.setSlot(pokemonAbility.slot());
          tmp.setIsHidden(pokemonAbility.isHidden());
          return tmp;
        }).toList());
    });
    return result;
  }
}
