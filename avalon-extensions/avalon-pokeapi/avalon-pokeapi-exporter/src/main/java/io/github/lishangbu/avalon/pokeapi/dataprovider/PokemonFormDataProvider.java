package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.model.PokemonFormExcelDTO;
import io.github.lishangbu.avalon.pokeapi.model.pokemon.PokemonForm;
import io.github.lishangbu.avalon.pokeapi.util.NamedApiResourceUtils;
import org.springframework.stereotype.Service;

/// 宝可梦形态数据提供者
///
/// @author lishangbu
/// @since 2026/2/10
@Service
public class PokemonFormDataProvider
    extends AbstractPokeApiDataProvider<PokemonForm, PokemonFormExcelDTO> {

  @Override
  public PokemonFormExcelDTO convert(PokemonForm pokemonForm) {
    PokemonFormExcelDTO result = new PokemonFormExcelDTO();
    result.setId(pokemonForm.id());
    result.setInternalName(pokemonForm.name());
    result.setName(resolveLocalizedName(pokemonForm.names(), pokemonForm.name()));
    result.setFormName(resolveLocalizedName(pokemonForm.formNames(), pokemonForm.formName()));
    result.setIsDefault(pokemonForm.isDefault());
    result.setIsBattleOnly(pokemonForm.isBattleOnly());
    result.setIsMega(pokemonForm.isMega());
    result.setPokemonId(NamedApiResourceUtils.getId(pokemonForm.pokemon()));
    result.setVersionGroupId(NamedApiResourceUtils.getId(pokemonForm.versionGroup()));
    return result;
  }
}
