package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.AbilityExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.Ability;
import org.springframework.stereotype.Service;

/// 特性数据提供者
///
/// @author lishangbu
/// @since 2026/2/10
@Service
public class AbilityDataProvider extends AbstractPokeApiDataProvider<Ability, AbilityExcelDTO> {

  @Override
  public AbilityExcelDTO convert(Ability ability) {
    AbilityExcelDTO result = new AbilityExcelDTO();
    result.setId(ability.id());
    result.setInternalName(ability.name());
    result.setName(resolveLocalizedName(ability.names(), ability.name()));
    return result;
  }
}
